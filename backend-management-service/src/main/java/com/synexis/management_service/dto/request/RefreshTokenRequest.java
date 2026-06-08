package com.synexis.management_service.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for refreshing access tokens.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token must not be blank")
    String refreshToken
) {}
