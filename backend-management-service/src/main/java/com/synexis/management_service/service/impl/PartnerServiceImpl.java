package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.KeycloakUserCreationException;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.PartnerService;

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
 * Handles partner registration and related business operations.
 *
 * This implementation persists partner data to the local database and
 * integrates with Keycloak for identity management, password setup, and role
 * assignment.
 *
 * It also enforces uniqueness checks and normalizes user input.
 */
@Service
public class PartnerServiceImpl implements PartnerService {

    // @Value("${KEYCLOAK_REALM}")
    private String keycloakRealm = "synexis"; // TODO - FIX - HARDCODED FOR TESTING - REVERT TO @Value IN PRODUCTION

    private final PartnerRepository partnerRepository;
    private final Keycloak keycloak;

    public PartnerServiceImpl(PartnerRepository partnerRepository, Keycloak keycloak) {
        this.partnerRepository = partnerRepository;
        this.keycloak = keycloak;
    }

    @Override
    @Transactional
    public RegisterPartnerResponse registerPartner(RegisterPartnerRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        if (partnerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
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

        // =========================
        // ASSIGN REALM ROLE IN KEYCLOAK
        // =========================
        // Add the CLIENT role to the newly created user in Keycloak realm roles.
        RoleRepresentation role = keycloak.realm(keycloakRealm)
                .roles()
                .get("PARTNER")
                .toRepresentation();

        keycloak.realm(keycloakRealm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));

        // =========================
        // PERSIST CLIENT ENTITY TO DATABASE (PASSWORD IS NOT STORED LOCALLY)
        // =========================
        // Save the client profile in local database with Keycloak id and normalized
        // data.
        Partner partner = new Partner();
        partner.setKeycloakId(userId); // IMPORTANT - LINK TO KEYCLOAK USER
        partner.setEmail(normalizedEmail);
        partner.setName(request.name().trim());
        partner.setAreaId(request.areaId());
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
        partner.setTermsAccepted(request.termsAccepted());
        partner.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        partner.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        partner.setRole(UserRole.partner);
        partner.setCreatedAt(Instant.now());

        Partner saved = partnerRepository.save(partner);

        return new RegisterPartnerResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                saved.getStatus(),
                saved.getLanguage(),
                saved.getCreatedAt(),
                saved.getTermsAccepted(),
                saved.getPicDirectory(),
                saved.getRole(),
                saved.getAreaId(),
                saved.getAvailabilityStatus());
    }

    private String normalizePicDirectory(String path) {
        if (path == null)
            return null;
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}