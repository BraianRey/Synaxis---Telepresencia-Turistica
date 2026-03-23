package com.synexis.management_service.dto;

import com.synexis.management_service.models.UserLanguage;
import com.synexis.management_service.models.UserStatus;
import com.synexis.management_service.models.UserRole;
import java.time.Instant;

/** Response after successful client registration. */
public record RegisterClientResponse(
        Long id,
        String email,
        String name,
        UserStatus status,
        UserLanguage language,
        Instant createdAt,
        Boolean termsAccepted,
        String picDirectory,
        UserRole role) {}
