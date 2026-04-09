package com.synexis.streaming_service.presentation.controllers;

import com.synexis.streaming_service.business.services.SessionManager;
import com.synexis.streaming_service.model.Peer;
import com.synexis.streaming_service.presentation.dto.request.JoinRoomRequest;
import com.synexis.streaming_service.presentation.dto.request.SignalRequest;
import org.springframework.stereotype.Controller;

/**
 * Controlador de señalización WebRTC.
 *
 * NOTA: Este controlador estaba originalmente basado en STOMP (@MessageMapping).
 * La señalización ahora se maneja directamente en RawWebSocketHandler (WebSocket RAW).
 * Esta clase se conserva para futuras integraciones con salas (rooms).
 */
@Controller
public class SignalingController {

    private final SessionManager sessionManager;

    public SignalingController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Unused: join room logic is now handled via RawWebSocketHandler.
     * Preserved for future room-based signaling implementation.
     */
    public void joinRoom(JoinRoomRequest request, String sessionId) {
        Peer peer = new Peer(request.getPeerId(), sessionId, request.getRole());
        sessionManager.joinRoom(request.getRoomId(), peer);
    }

    /**
     * Unused: signal relay is now handled via RawWebSocketHandler.
     */
    public void relaySignal(SignalRequest request) {
        // Relay is now done in RawWebSocketHandler
    }
}
