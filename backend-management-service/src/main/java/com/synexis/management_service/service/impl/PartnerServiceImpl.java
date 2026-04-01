package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.entity.Area;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.KeycloakUserCreationException;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.AreaService;
import com.synexis.management_service.service.PartnerService;

import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
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

    private String keycloakRealm = "synexis";

    private final PartnerRepository partnerRepository;
    private final Keycloak keycloak;
    private final AreaService areaService;

    public PartnerServiceImpl(PartnerRepository partnerRepository, Keycloak keycloak, AreaService areaService) {
        this.partnerRepository = partnerRepository;
        this.keycloak = keycloak;
        this.areaService = areaService;
    }

    @Override
    @Transactional
    public RegisterPartnerResponse registerPartner(RegisterPartnerRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();

        if (partnerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        // 1. CREATE USER IN KEYCLOAK
        UserRepresentation user = new UserRepresentation();
        user.setUsername(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setEnabled(true);
        user.setFirstName(request.name().trim());
        user.setLastName(request.name().trim());
        user.setEmailVerified(true);

        Response response = keycloak.realm(keycloakRealm).users().create(user);

        if (response.getStatus() != 201) {
            throw new KeycloakUserCreationException(response.getStatus());
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // 2. SET PASSWORD
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        keycloak.realm(keycloakRealm).users().get(userId).resetPassword(credential);

        // 3. ASSIGN 'PARTNER' ROLE AT 'telepresence' CLIENT LEVEL
        // We search for the internal UUID of the Keycloak client
        String clientUuid = keycloak.realm(keycloakRealm)
                .clients()
                .findByClientId("telepresence")
                .get(0)
                .getId();

        // We get the PARTNER role defined within that client
        RoleRepresentation partnerRole = keycloak.realm(keycloakRealm)
                .clients()
                .get(clientUuid)
                .roles()
                .get("PARTNER")
                .toRepresentation();

        // We assign the role to the user
        keycloak.realm(keycloakRealm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(clientUuid)
                .add(List.of(partnerRole));

        // 4. PERSISTENCE IN LOCAL DATABASE
        Partner partner = new Partner();
        partner.setKeycloakId(userId);
        partner.setEmail(normalizedEmail);
        partner.setName(request.name().trim());
        Area area = areaService.findById(request.areaId().longValue());
        partner.setArea(area);
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
        partner.setTermsAccepted(request.termsAccepted());
        partner.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        partner.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        partner.setRole(UserRole.PARTNER);
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
                saved.getArea().getId().intValue(),
                saved.getAvailabilityStatus());
    }

    private String normalizePicDirectory(String path) {
        if (path == null)
            return null;
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}