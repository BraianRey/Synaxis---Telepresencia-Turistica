package com.synexis.management_service.service.impl;

import com.synexis.management_service.dto.request.RegisterPartnerRequest;
import com.synexis.management_service.dto.response.RegisterPartnerResponse;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.exception.EmailAlreadyExistsException;
import com.synexis.management_service.repository.PartnerRepository;
import com.synexis.management_service.service.PartnerService;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and future partner-specific business rules for
 * {@link com.synexis.management_service.entity.Partner}.
 */
@Service
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;
    private final AuthServiceImpl authService;

    public PartnerServiceImpl(PartnerRepository partnerRepository, AuthServiceImpl authService) {
        this.partnerRepository = partnerRepository;
        this.authService = authService;
    }

    @Transactional
    public RegisterPartnerResponse registerPartner(RegisterPartnerRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (partnerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        Partner partner = new Partner();
        partner.setEmail(normalizedEmail);
        partner.setPasswordHash(authService.encodePassword(request.password()));
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
        if (path == null) {
            return null;
        }
        String t = path.trim();
        return t.isEmpty() ? null : t;
    }
}
