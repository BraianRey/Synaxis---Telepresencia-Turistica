package com.synexis.management_service.dto.response;

import java.time.Instant;

import com.synexis.management_service.entity.UserLanguage;
import com.synexis.management_service.entity.UserRole;
import com.synexis.management_service.entity.UserStatus;

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
                UserRole role) {
}
