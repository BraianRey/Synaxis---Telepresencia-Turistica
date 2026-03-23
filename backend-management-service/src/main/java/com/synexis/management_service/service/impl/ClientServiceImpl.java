package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterClientRequest;
import com.synexis.management_service.dto.response.RegisterClientResponse;
import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.repository.ClientRepository;
import com.synexis.management_service.service.ClientService;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and future client-specific business rules for
 * {@link com.synexis.management_service.entity.Client}.
 */
@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AuthServiceImpl authService;

    public ClientServiceImpl(ClientRepository clientRepository, AuthServiceImpl authService) {
        this.clientRepository = clientRepository;
        this.authService = authService;
    }

    @Override
    @Transactional
    public RegisterClientResponse registerClient(RegisterClientRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (clientRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Client client = new Client();
        client.setEmail(normalizedEmail);
        client.setPasswordHash(authService.encodePassword(request.password()));
        client.setName(request.name().trim());
        client.setTermsAccepted(request.termsAccepted());
        client.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        client.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        client.setRole(UserRole.client);
        client.setCreatedAt(Instant.now());

        Client saved = clientRepository.save(client);
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
        if (path == null) {
            return null;
        }
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}
