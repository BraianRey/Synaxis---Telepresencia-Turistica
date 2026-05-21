package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.dto.response.usersProfile.ClientPublicProfileResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.service.ClientService;
import com.synexis.management_service.service.KeycloakService;

import java.time.Instant;

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

    private final ClientRepository clientRepository;
    private final KeycloakService keycloakService;

    public ClientServiceImpl(ClientRepository clientRepository, KeycloakService keycloakService) {
        this.clientRepository = clientRepository;
        this.keycloakService = keycloakService;
    }

    @Override
    @Transactional
    public RegisterClientResponse registerClient(RegisterClientRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();
        String trimmedName     = request.name().trim();

        if (clientRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String userId = keycloakService.resolveUserForRole(
                normalizedEmail, request.password(), trimmedName, "CLIENT");

        if (clientRepository.existsByKeycloakId(userId)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Client client = new Client();
        client.setKeycloakId(userId);
        client.setEmail(normalizedEmail);
        client.setName(trimmedName);
        client.setTermsAccepted(request.termsAccepted());
        client.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        client.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        client.setRole(UserRole.CLIENT);
        client.setCreatedAt(Instant.now());

        Client saved = clientRepository.save(client);

        return new RegisterClientResponse(
                saved.getId(), saved.getEmail(), saved.getName(), saved.getStatus(),
                saved.getLanguage(), saved.getCreatedAt(), saved.getTermsAccepted(),
                saved.getPicDirectory(), saved.getRole());
    }

    public ClientPublicProfileResponse getPublicProfile(Long clientId) {

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Client not found with id: " + clientId
                        ));

        return new ClientPublicProfileResponse(
                client.getName(),
                client.getPicDirectory(),
                client.getLanguage().name(),
                client.getCreatedAt()
        );
    }


    private String normalizePicDirectory(String path) {
        if (path == null)
            return null;
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}