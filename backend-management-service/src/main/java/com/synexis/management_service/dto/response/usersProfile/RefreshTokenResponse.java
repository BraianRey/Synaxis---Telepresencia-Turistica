package com.synexis.management_service.dto.response.usersProfile;

/**
 * Response payload containing new token details after refresh.
 */
public record RefreshTokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
