package com.synexis.management_service.dto.response.usersProfile;

import java.time.Instant;

public record ClientPublicProfileResponse(
        String name,
        String language,
        Instant createdAt
) {
}
