package com.synexis.streaming_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket RAW handler for WebRTC signaling.
 * Features:
 * - Stale Message Cleaning: If a new offer arrives, old pending messages are
 * purged
 * - Queue Management: Ensures latest negotiation cycle delivered upon
 * reconnection
 * - Active Keep-Alive (Ping): Server sends pings every 10s to detect zombie
 * sessions
 * - Queue Timeout: Inactive queues cleaned every 5 minutes
 * - Queue Size Limit: Maximum 100 messages per peer to prevent memory
 * exhaustion
 */
@Component
public class RawWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocketHandler.class);

    private static final Pattern TARGET_ID_PATTERN = Pattern.compile("\"targetId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");
    private static final int MAX_QUEUE_MESSAGES = 100;
    private static final long QUEUE_TIMEOUT_MILLIS = 300000; // 5 minutes

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<String>> pendingMessages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> queueAccessTime = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String peerId = extractPeerId(session);
        if (peerId == null || peerId.isBlank()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // If there was an old session for the same peerId, close it
        WebSocketSession oldSession = sessions.remove(peerId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
            }
        }

        sessions.put(peerId, session);
        log.info("Peer connected: '{}'", peerId);

        // Deliver pending messages
        Queue<String> pending = pendingMessages.remove(peerId);
        if (pending != null && !pending.isEmpty()) {
            log.info("Delivering {} messages to '{}' after reconnection", pending.size(), peerId);
            synchronized (session) {
                while (!pending.isEmpty()) {
                    String msg = pending.poll();
                    try {
                        session.sendMessage(new TextMessage(msg));
                    } catch (IOException e) {
                        pending.offer(msg);
                        break;
                    }
                }
            }
        }
        queueAccessTime.remove(peerId);
    }

    /**
     * Active Keep-Alive: Sends a WebSocket PING to all sessions every 10 seconds.
     * If a session is a zombie (dead connection that looks open), sending a ping
     * will eventually trigger an IOException, allowing us to clean up the session.
     */
    @Scheduled(fixedRate = 10000)
    public void sendPings() {
        if (sessions.isEmpty())
            return;

        log.debug("Sending Pings to {} active peers...", sessions.size());
        sessions.forEach((peerId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("keep-alive".getBytes())));
                } catch (IOException e) {
                    log.warn("Detected dead session for '{}' during ping. Closing.", peerId);
                    sessions.remove(peerId);
                    try {
                        session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    } catch (IOException ignore) {
                    }
                }
            } else {
                sessions.remove(peerId);
            }
        });
    }

    /**
     * Cleanup inactive queues: Removes pending message queues that haven't been
     * accessed for 5 minutes.
     * Prevents long-term memory accumulation from offline peers.
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void cleanupInactiveQueues() {
        if (queueAccessTime.isEmpty())
            return;

        long currentTime = System.currentTimeMillis();
        List<String> peersToClean = new ArrayList<>();

        queueAccessTime.forEach((peerId, lastAccess) -> {
            if (currentTime - lastAccess > QUEUE_TIMEOUT_MILLIS) {
                peersToClean.add(peerId);
            }
        });

        for (String peerId : peersToClean) {
            Queue<String> queue = pendingMessages.remove(peerId);
            queueAccessTime.remove(peerId);

            if (queue != null) {
                int queueSize = queue.size();
                log.info("Cleaned inactive queue for peer '{}' (was {} messages, inactive >5min)", peerId, queueSize);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession senderSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String senderId = extractPeerId(senderSession);
        String targetId = extractTargetId(payload);
        String type = extractType(payload);

        if (targetId == null)
            return;

        // ROBUSTNESS: Clean stale messages.
        if ("offer".equals(type)) {
            log.info("New offer detected from '{}' to '{}'. Purging old pending messages.", senderId, targetId);
            pendingMessages.remove(targetId);
        }

        WebSocketSession targetSession = sessions.get(targetId);

        if (targetSession == null || !targetSession.isOpen()) {
            log.warn("Target '{}' offline. Queueing {}...", targetId, type);
            Queue<String> queue = pendingMessages.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>());

            // Update access time for timestamp-based cleanup
            queueAccessTime.put(targetId, System.currentTimeMillis());

            // Check queue size limit to prevent memory exhaustion
            if (queue.size() >= MAX_QUEUE_MESSAGES) {
                String dropped = queue.poll();
                log.warn("Queue for '{}' reached max size ({}). Dropping oldest message to maintain limit.", targetId,
                        MAX_QUEUE_MESSAGES);
            }

            if ("offer".equals(type) || "answer".equals(type)) {
                queue.removeIf(m -> m.contains("\"type\":\"" + type + "\""));
            }

            queue.offer(payload);
            return;
        }

        // Relay immediately
        synchronized (targetSession) {
            try {
                targetSession.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("Relay failed to '{}'", targetId);
                pendingMessages.computeIfAbsent(targetId, k -> new ConcurrentLinkedQueue<>()).offer(payload);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String peerId = extractPeerId(session);
        if (peerId != null) {
            sessions.remove(peerId);
            // Mark access time so queue cleanup will handle it if offline for 5+ minutes
            queueAccessTime.put(peerId, System.currentTimeMillis());
            log.info("Peer disconnected: '{}' (status={}). Pending messages will be kept for 5 minutes.", peerId,
                    status);
        }
    }

    private String extractPeerId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null)
            return null;
        for (String param : uri.getQuery().split("&")) {
            if (param.startsWith("peerId="))
                return param.substring(7);
        }
        return null;
    }

    private String extractTargetId(String json) {
        Matcher matcher = TARGET_ID_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractType(String json) {
        Matcher matcher = TYPE_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}