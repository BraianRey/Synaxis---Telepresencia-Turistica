package com.synexis.streaming_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket RAW handler for WebRTC signaling.
 *
 * Protocol:
 * - Client connects with ?peerId=XXX in the URL.
 * - Messages are JSON with a "targetId" field for routing.
 * - Examples:
 * Offer: {"type":"offer", "sdp":"...",
 * "senderId":"PARTNER_01","targetId":"CLIENT_01"}
 * Answer: {"type":"answer", "sdp":"...", "senderId":"CLIENT_01",
 * "targetId":"PARTNER_01"}
 * Candidate: {"type":"candidate","candidate":{...},"senderId":"CLIENT_01",
 * "targetId":"PARTNER_01"}
 *
 * FEATURE: Pending message queue
 * - If target peer is not connected, message is queued
 * - When peer connects, all pending messages are delivered
 */
@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketHandler.class);

    /** Regex to extract the value of "targetId" from a JSON string */
    private static final Pattern TARGET_ID_PATTERN = Pattern.compile("\"targetId\"\\s*:\\s*\"([^\"]+)\"");

    /** Thread-safe map of peerId → active WebSocket session */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Pending messages queue: peerId → List of messages to deliver when connected
     */
    private final ConcurrentHashMap<String, Queue<String>> pendingMessages = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String peerId = extractPeerId(session);
        if (peerId == null || peerId.isBlank()) {
            log.warn("Rejected connection without ?peerId= param. sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        sessions.put(peerId, session);
        log.info("✅ Connected — peerId='{}' sessionId={} | Active peers: {}",
                peerId, session.getId(), sessions.keySet());

        // Deliver any pending messages queued while disconnected
        Queue<String> pending = pendingMessages.remove(peerId);
        if (pending != null && !pending.isEmpty()) {
            log.info("📬 Delivering {} pending messages to '{}'", pending.size(), peerId);
            synchronized (session) {
                while (!pending.isEmpty()) {
                    String pendingMessage = pending.poll();
                    try {
                        session.sendMessage(new TextMessage(pendingMessage));
                        log.debug("✅ Delivered pending message to '{}'", peerId);
                    } catch (IOException e) {
                        log.error("❌ Failed to deliver pending message to '{}': {}", peerId, e.getMessage());
                        pending.offer(pendingMessage); // Re-queue if failed
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession senderSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String senderPeerId = extractPeerId(senderSession);
        log.debug("📨 Message from '{}': {}", senderPeerId, payload);

        String targetId = extractTargetId(payload);
        if (targetId == null) {
            log.warn("⚠️  Message from '{}' missing 'targetId' — dropped. Payload={}", senderPeerId, payload);
            sendError(senderSession, "Message must include 'targetId' field.");
            return;
        }

        WebSocketSession targetSession = sessions.get(targetId);

        if (targetSession == null || !targetSession.isOpen()) {
            // TARGET NOT CONNECTED: Queue the message
            log.warn("⚠️  Target peer '{}' not connected. Queueing message for later delivery. Available: {}",
                    targetId, sessions.keySet());

            Queue<String> queue = pendingMessages.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>());
            queue.offer(payload);

            log.info("📥 Message queued for '{}' — Queue size: {}", targetId, queue.size());
            sendInfo(senderSession, "Message queued. Peer '" + targetId + "' will receive it when connected.");
            return;
        }

        // TARGET IS CONNECTED: Relay immediately
        log.info("🔀 Relaying message from '{}' → '{}'", senderPeerId, targetId);
        synchronized (targetSession) {
            try {
                targetSession.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("❌ Failed to relay to '{}': {}", targetId, e.getMessage());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String peerId = extractPeerId(session);
        if (peerId != null) {
            sessions.remove(peerId);
            log.info("🔌 Disconnected — peerId='{}' status={} | Remaining peers: {}",
                    peerId, status, sessions.keySet());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String peerId = extractPeerId(session);
        log.error("❌ Transport error for peerId='{}': {}", peerId, exception.getMessage());
        if (peerId != null) {
            sessions.remove(peerId);
        }
    }

    /**
     * Extracts peerId from the WebSocket URL query string.
     * Example: ws://host:8081/ws-signaling?peerId=PARTNER_01 → "PARTNER_01"
     */
    private String extractPeerId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null)
            return null;
        String query = uri.getQuery();
        if (query == null)
            return null;
        for (String param : query.split("&")) {
            if (param.startsWith("peerId=")) {
                String value = param.substring("peerId=".length());
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * Extracts "targetId" value from a JSON string using regex.
     * Avoids adding a JSON library dependency.
     */
    private String extractTargetId(String json) {
        Matcher matcher = TARGET_ID_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        sendJsonMessage(session, "error", errorMessage);
    }

    private void sendInfo(WebSocketSession session, String infoMessage) {
        sendJsonMessage(session, "info", infoMessage);
    }

    private void sendJsonMessage(WebSocketSession session, String type, String message) {
        if (session == null || !session.isOpen())
            return;
        try {
            String json = "{\"type\":\"" + type + "\",\"message\":\"" + message.replace("\"", "'") + "\"}";
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send {} message to session: {}", type, e.getMessage());
        }
    }
}
