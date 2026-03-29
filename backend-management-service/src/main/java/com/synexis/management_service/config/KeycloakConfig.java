package com.synexis.management_service.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    // @Value("${KEYCLOAK_URL}")
    // private String serverUrl;

    // @Value("${KEYCLOAK_REALM}")
    // private String realm;

    // @Value("${KEYCLOAK_CLIENT_ID}")
    // private String clientId;

    // @Value("${KEYCLOAK_CLIENT_SECRET}")
    // private String clientSecret;

    // @Value("${KEYCLOAK_ADMIN_USERNAME}")
    // private String adminUsername;

    // @Value("${KEYCLOAK_ADMIN_PASSWORD}")
    // private String adminPassword;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl("http://localhost:8085")
                .realm("synexis")
                .clientId("telepresence")
                .clientSecret("wO8qciPyigPvlYPQAX3wzft18wa1lqv6")
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }
}