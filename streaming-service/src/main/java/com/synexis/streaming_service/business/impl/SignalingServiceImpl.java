package com.synexis.streaming_service.business.impl;

import com.synexis.streaming_service.business.services.SignalingService;
import com.synexis.streaming_service.business.services.SessionManager;
import com.synexis.streaming_service.presentation.dto.request.SignalRequest;
import org.springframework.stereotype.Service;

/**
 * Implementación de SignalingService.
 *
 * NOTA: El relay de señales WebRTC ahora se maneja directamente en RawWebSocketHandler.
 * Esta clase se conserva para uso futuro (lógica de rooms, persistencia de sesiones, etc.).
 */
@Service
public class SignalingServiceImpl implements SignalingService {

    private final SessionManager sessionManager;

    public SignalingServiceImpl(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void relaySignal(SignalRequest request) {
        // Relay is now handled in RawWebSocketHandler.
        // This method is preserved for future room-based logic.
    }
}
