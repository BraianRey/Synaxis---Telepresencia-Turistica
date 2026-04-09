package com.synexis.streaming_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSocket RAW para señalización WebRTC.
 *
 * Expone el endpoint /ws-signaling con soporte CORS abierto (* para desarrollo).
 * Los clientes se conectan pasando su ID como query param: /ws-signaling?peerId=XXX
 *
 * Nota: Se reemplazó @EnableWebSocketMessageBroker (STOMP) por @EnableWebSocket (RAW)
 * porque los clientes móviles (OkHttp WebSocketListener) usan WebSocket puro sin STOMP.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RawWebSocketHandler rawWebSocketHandler;

    public WebSocketConfig(RawWebSocketHandler rawWebSocketHandler) {
        this.rawWebSocketHandler = rawWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(rawWebSocketHandler, "/ws-signaling")
                .setAllowedOrigins("*");
    }
}
