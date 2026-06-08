package com.synexis.management_service.controller;

import com.synexis.management_service.dto.response.IceServersResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/api/streaming")
public class StreamingController {

    @Value("${turn.secret}")
    private String turnSecret;

    @Value("${turn.host}")
    private String turnHost;

    @Value("${turn.udp-port}")
    private int turnUdpPort;

    @Value("${turn.tcp-port}")
    private int turnTcpPort;

    @Value("${turn.tls-port}")
    private int turnTlsPort;

    @GetMapping("/ice-servers")
    public ResponseEntity<IceServersResponse> getIceServers() {
        // Generate dynamic credentials valid for 24 hours (86400 seconds)
        long timestamp = (System.currentTimeMillis() / 1000) + 86400;
        String username = timestamp + ":synexis-user";
        String password = generateHmacSHA1(username, turnSecret);

        List<IceServersResponse.IceServerInfo> servers = new ArrayList<>();

        // 1. Public fallback Google STUN
        servers.add(IceServersResponse.IceServerInfo.builder()
                .urls(List.of("stun:stun.l.google.com:19302"))
                .build());

        // 2. Custom STUN
        servers.add(IceServersResponse.IceServerInfo.builder()
                .urls(List.of("stun:" + turnHost + ":" + turnUdpPort))
                .build());

        // 3. TURN over UDP
        servers.add(IceServersResponse.IceServerInfo.builder()
                .urls(List.of("turn:" + turnHost + ":" + turnUdpPort + "?transport=udp"))
                .username(username)
                .credential(password)
                .build());

        // 4. TURN over TCP
        servers.add(IceServersResponse.IceServerInfo.builder()
                .urls(List.of("turn:" + turnHost + ":" + turnTcpPort + "?transport=tcp"))
                .username(username)
                .credential(password)
                .build());

        // 5. TURNS over TLS/TCP
        servers.add(IceServersResponse.IceServerInfo.builder()
                .urls(List.of("turns:" + turnHost + ":" + turnTlsPort + "?transport=tcp"))
                .username(username)
                .credential(password)
                .build());

        return ResponseEntity.ok(new IceServersResponse(servers));
    }

    private String generateHmacSHA1(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC-SHA1 signature for TURN credentials", e);
        }
    }
}
