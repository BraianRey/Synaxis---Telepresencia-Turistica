package com.synexis.management_service.dto.response;

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
        Long areaId) {
}


