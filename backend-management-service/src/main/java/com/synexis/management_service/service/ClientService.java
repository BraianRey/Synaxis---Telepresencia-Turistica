package com.synexis.management_service.service;

import com.synexis.management_service.dto.RegisterClientRequest;
import com.synexis.management_service.dto.RegisterClientResponse;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.models.Client;
import com.synexis.management_service.models.UserLanguage;
import com.synexis.management_service.models.UserRole;
import com.synexis.management_service.repository.ClientRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and future client-specific business rules for {@link com.synexis.management_service.models.Client}. */
@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final AuthService authService;

    public ClientService(ClientRepository clientRepository, AuthService authService) {
        this.clientRepository = clientRepository;
        this.authService = authService;
    }

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

    private static String normalizePicDirectory(String path) {
        if (path == null) {
            return null;
        }
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}
