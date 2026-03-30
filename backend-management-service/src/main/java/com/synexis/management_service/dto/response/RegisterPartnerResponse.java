package com.synexis.management_service.dto.response;

import java.time.Instant;

import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.entity.UserStatus;

/** Response after successful partner registration. */
public record RegisterPartnerResponse(
        Long id,
        String email,
        String name,
        UserStatus status,
        UserLanguage language,
        Instant createdAt,
        Boolean termsAccepted,
        String picDirectory,
        UserRole role,
        Integer areaId,
        PartnerAvailabilityStatus availabilityStatus) {
}
