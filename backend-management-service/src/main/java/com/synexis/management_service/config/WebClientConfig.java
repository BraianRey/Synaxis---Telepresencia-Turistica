package com.synexis.management_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient nominatimWebClient() {
        return WebClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "telepresencia/0.1")
                .build();
    }

    @Bean
    public WebClient wikimediaWebClient() {
        return WebClient.builder()
                .baseUrl("https://commons.wikimedia.org")
                .defaultHeader("User-Agent", "TourPresence/1.0 (academic project)")
                .build();
    }
}
