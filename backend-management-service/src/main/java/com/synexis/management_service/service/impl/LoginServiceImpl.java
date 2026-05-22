package com.synexis.management_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synexis.management_service.dto.response.usersProfile.LoginResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.exception.InvalidCredentialsException;
import com.synexis.management_service.exception.KeycloakUserCreationException;
import com.synexis.management_service.exception.WrongPasswordException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.LoginService;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class LoginServiceImpl implements LoginService {

    private static final String KEYCLOAK_URL = "http://localhost:8085";
    private static final String REALM = "synexis";
    private static final String CLIENT_ID = "telepresence";
    private static final String CLIENT_SECRET = "wO8qciPyigPvlYPQAX3wzft18wa1lqv6";

    private final ClientRepository clientRepository;
    private final PartnerRepository partnerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public LoginServiceImpl(
            ClientRepository clientRepository,
            PartnerRepository partnerRepository) {
        this.clientRepository = clientRepository;
        this.partnerRepository = partnerRepository;
    }

    @Override
    public LoginResponse loginClient(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Client client = clientRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        TokenResult token = requestToken(normalizedEmail, password);
        String picDir = client.getPicDirectory();
        System.out.println("[LoginDebug] Client login: email=" + normalizedEmail + ", picDirectory=" + picDir);
        return new LoginResponse(
                token.accessToken,
                token.refreshToken,
                token.tokenType,
                token.expiresIn,
                client.getId(),
                client.getEmail(),
                client.getName(),
                client.getRole().name(),
                client.getLanguage().name(),
                picDir);
    }

    @Override
    public LoginResponse loginPartner(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Partner partner = partnerRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        TokenResult token = requestToken(normalizedEmail, password);
        String picDir = partner.getPicDirectory();
        System.out.println("[LoginDebug] Partner login: email=" + normalizedEmail + ", picDirectory=" + picDir);
        return new LoginResponse(
                token.accessToken,
                token.refreshToken,
                token.tokenType,
                token.expiresIn,
                partner.getId(),
                partner.getEmail(),
                partner.getName(),
                partner.getRole().name(),
                partner.getLanguage().name(),
                picDir);
    }

    private TokenResult requestToken(String email, String password) {
        String formBody = "client_id=" + encode(CLIENT_ID)
                + "&client_secret=" + encode(CLIENT_SECRET)
                + "&grant_type=password"
                + "&username=" + encode(email)
                + "&password=" + encode(password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                return new TokenResult(
                        node.path("access_token").asText(),
                        node.path("refresh_token").asText(),
                        node.path("token_type").asText(),
                        node.path("expires_in").asLong());
            }
            if (response.statusCode() == 400 || response.statusCode() == 401) {
                throw new WrongPasswordException();
            }
            throw new KeycloakUserCreationException(
                    "Keycloak authentication error. Status: " + response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KeycloakUserCreationException("Unable to validate login with Keycloak: " + ex.getMessage());
        } catch (IOException ex) {
            throw new KeycloakUserCreationException("Unable to validate login with Keycloak: " + ex.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class TokenResult {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final long expiresIn;

        private TokenResult(String accessToken, String refreshToken, String tokenType, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
        }
    }
}
