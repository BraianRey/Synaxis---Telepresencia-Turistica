package com.synexis.management_service.service.impl;

import com.synexis.management_service.client.NominatimClient;
import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.dto.response.usersProfile.PartnerPublicProfileResponse;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.exception.ResourceNotFoundException;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.KeycloakService;
import com.synexis.management_service.service.PartnerService;
import com.synexis.management_service.utils.GeoUtils;


import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles partner registration and related business operations.
 */
@Service
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;
    private final KeycloakService keycloakService;
    private final NominatimClient nominatimClient;

    public PartnerServiceImpl(PartnerRepository partnerRepository, KeycloakService keycloakService, NominatimClient nominatimClient) {
        this.partnerRepository = partnerRepository;
        this.keycloakService = keycloakService;
        this.nominatimClient = nominatimClient;
    }

    @Override
    @Transactional
    public RegisterPartnerResponse registerPartner(RegisterPartnerRequest request) {

        String normalizedEmail = request.email().trim().toLowerCase();
        String trimmedName     = request.name().trim();

        if (partnerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String userId = keycloakService.resolveUserForRole(
                normalizedEmail, request.password(), trimmedName, "PARTNER");

        if (partnerRepository.existsByKeycloakId(userId)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Partner partner = new Partner();
        partner.setKeycloakId(userId);
        partner.setEmail(normalizedEmail);
        partner.setName(trimmedName);
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
        partner.setTermsAccepted(request.termsAccepted());
        partner.setLanguage(request.language() != null ? request.language() : UserLanguage.es);
        partner.setPicDirectory(normalizePicDirectory(request.picDirectory()));
        partner.setRole(UserRole.PARTNER);
        partner.setCreatedAt(Instant.now());
        partner.setLocation(GeoUtils.createPoint(request.longitude(), request.latitude()));
        partner.setAverageRating(0.0);
        partner.setRatingCount(0);

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
                saved.getAvailabilityStatus());
    }

    public PartnerPublicProfileResponse getPublicProfile(Long partnerId) {

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Partner not found with id: " + partnerId
                        ));

        return new PartnerPublicProfileResponse(
                partner.getName(),
                partner.getPicDirectory(),
                partner.getAverageRating(),
                partner.getRatingCount(),
                partner.getLanguage().name(),
                partner.getAvailabilityStatus().name(),
                partner.getCreatedAt(),
                nominatimClient.getCityFromCoordinates(
                        partner.getLocation().getX(),
                        partner.getLocation().getY()
                )
        );
    }

    
    private String normalizePicDirectory(String path) {
        if (path == null)
            return null;
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}