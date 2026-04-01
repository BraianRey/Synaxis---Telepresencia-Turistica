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
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles partner registration and related business operations.
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

        // Validate referenced area before creating external user in Keycloak.
        Area area = areaService.findById(request.areaId().longValue());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setEnabled(true);
        user.setFirstName(request.name().trim());
        user.setLastName(request.name().trim());
        user.setEmailVerified(true);

        Response response = keycloak.realm(keycloakRealm).users().create(user);

        if (response.getStatus() == 409) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        if (response.getStatus() != 201) {
            throw new KeycloakUserCreationException(response.getStatus());
        }

        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        keycloak.realm(keycloakRealm).users().get(userId).resetPassword(credential);

        String clientUuid;
        try {
            List<ClientRepresentation> clients = keycloak.realm(keycloakRealm)
                    .clients()
                    .findByClientId("telepresence");

            if (clients == null || clients.isEmpty()) {
                throw new KeycloakUserCreationException("Keycloak client 'telepresence' not found in realm 'synexis'.");
            }

            clientUuid = clients.get(0).getId();

            RoleRepresentation partnerRole = keycloak.realm(keycloakRealm)
                    .clients()
                    .get(clientUuid)
                    .roles()
                    .get("PARTNER")
                    .toRepresentation();

            keycloak.realm(keycloakRealm)
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientUuid)
                    .add(List.of(partnerRole));
        } catch (KeycloakUserCreationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KeycloakUserCreationException("Failed to assign PARTNER role in Keycloak: " + ex.getMessage());
        }

        Partner partner = new Partner();
        partner.setKeycloakId(userId);
        partner.setEmail(normalizedEmail);
        partner.setName(request.name().trim());
        partner.setArea(area);
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