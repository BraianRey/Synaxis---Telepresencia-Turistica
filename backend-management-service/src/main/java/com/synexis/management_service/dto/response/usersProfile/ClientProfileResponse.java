package com.synexis.management_service.dto.response.usersProfile;

import java.time.Instant;

public record ClientProfileResponse(
        String name,
        String email,
        String language,
        String picDirectory,
        Instant createdAt
) implements ProfileData {}
