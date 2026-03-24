package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.KeycloakUserCreationException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.service.ClientService;

import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles client registration and related business operations.
 *
 * This implementation persists client data to the local database and
 * integrates with Keycloak for identity management, password setup, and role
 * assignment.
 *
 * It also enforces uniqueness checks and normalizes user input.
 */
@Service
public class ClientServiceImpl implements ClientService {

    // @Value("${KEYCLOAK_REALM}")
    private String keycloakRealm = "synexis"; // TODO - FIX - HARDCODED FOR TESTING - REVERT TO @Value IN PRODUCTION

    private final ClientRepository clientRepository;
    private final Keycloak keycloak;

    public ClientServiceImpl(ClientRepository clientRepository, Keycloak keycloak) {
        this.clientRepository = clientRepository;
        this.keycloak = keycloak;
    }

    @Override
    @Transactional
    public RegisterClientResponse registerClient(RegisterClientRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        if (clientRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // =========================
        // CREATE USER IN KEYCLOAK
        // =========================
        // Create a Keycloak user record with username/email and enable status.
        UserRepresentation user = new UserRepresentation();
        user.setUsername(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setEnabled(true);

        Response response = keycloak.realm(keycloakRealm)
                .users()
                .create(user);

        if (response.getStatus() != 201) {
            throw new KeycloakUserCreationException(response.getStatus());
        }

        System.out.println("Keycloak user created with email: " + normalizedEmail);

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // =========================
        // SET PASSWORD IN KEYCLOAK
        // =========================
        // Configure the new Keycloak user credentials (non-temporary password).
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password()); // Use raw password here; Keycloak will hash it internally.
        credential.setTemporary(false);

        keycloak.realm(keycloakRealm)
                .users()
                .get(userId)
                .resetPassword(credential);

        System.out.println("Password set for Keycloak user: " + normalizedEmail);

        // =========================
        // ASSIGN REALM ROLE IN KEYCLOAK
        // =========================
        // Add the CLIENT role to the newly created user in Keycloak realm roles.
        RoleRepresentation role = keycloak.realm(keycloakRealm)
                .roles()
                .get("CLIENT")
                .toRepresentation();

        keycloak.realm(keycloakRealm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        System.out.println("CLIENT role assigned to Keycloak user: " + normalizedEmail);

        // =========================
        // PERSIST CLIENT ENTITY TO DATABASE (PASSWORD IS NOT STORED LOCALLY)
        // =========================
        // Save the client profile in local database with Keycloak id and normalized
        // data.
        Client client = new Client();
        client.setKeycloakId(userId); // IMPORTANT - LINK TO KEYCLOAK USER
        client.setEmail(normalizedEmail);
        client.setName(request.name().trim());
        client.setTermsAccepted(request.termsAccepted());
        client.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        client.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        client.setRole(UserRole.client);
        client.setCreatedAt(Instant.now());

        Client saved = clientRepository.save(client);

        System.out.println("Client entity saved to database with email: " + normalizedEmail);

        return new RegisterClientResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getStatus(),
                saved.getLanguage(),
                saved.getCreatedAt(),
                saved.getTermsAccepted(),
                saved.getPicDirectory(),
                saved.getRole());
    }

    private String normalizePicDirectory(String path) {
        if (path == null)
            return null;
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}