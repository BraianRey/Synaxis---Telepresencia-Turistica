package com.synexis.management_service.dto.response.usersProfile;

import java.time.Instant;

public record PartnerPublicProfileResponse(
        String name,
        String picDirectory,
        Double averageRating,
        Integer ratingCount,
        String language,
        String availabilityStatus,
        Instant createdAt,
        String city
) {
}