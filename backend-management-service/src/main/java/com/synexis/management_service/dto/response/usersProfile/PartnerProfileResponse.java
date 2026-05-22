package com.synexis.management_service.dto.response.usersProfile;

import java.time.Instant;

public record PartnerProfileResponse(
        String name,
        String email,
        String language,
        String picDirectory,
        Double averageRating,
        Integer ratingCount,
        String availabilityStatus,
        Instant createdAt,
        String city
) implements ProfileData {}
