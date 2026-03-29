package com.synexis.management_service.dto.response;

/**
 * Response DTO for the user profile endpoint.
 */
public record UserProfileResponse(
        String name,
        String email,
        String status,
        String language,
        String role,
        String picDirectory) {
}