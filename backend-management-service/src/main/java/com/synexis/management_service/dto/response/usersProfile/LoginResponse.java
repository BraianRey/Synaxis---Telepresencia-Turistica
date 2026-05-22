package com.synexis.management_service.dto.response.usersProfile;

/** Successful login response with Keycloak tokens and local profile data. */
public record LoginResponse(
                String accessToken,
                String refreshToken,
                String tokenType,
                long expiresIn,
                Long id,
                String email,
                String name,
                String role,
                String language,
                String picDirectory) {
}
