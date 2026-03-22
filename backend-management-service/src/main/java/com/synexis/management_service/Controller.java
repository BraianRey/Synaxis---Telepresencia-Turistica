package com.synexis.management_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        String message = "ping detected " + LocalDateTime.now();
        System.out.println(message);
        Map<String, String> response = new HashMap<>();
        response.put("status", "pong");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}