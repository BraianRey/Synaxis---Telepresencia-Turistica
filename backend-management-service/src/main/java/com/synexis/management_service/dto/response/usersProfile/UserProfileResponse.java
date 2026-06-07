package com.synexis.management_service.dto.response.usersProfile;

/**
 * Response DTO for the user profile endpoint.
 */
public record UserProfileResponse(
        String role,
        Object profile
) {}