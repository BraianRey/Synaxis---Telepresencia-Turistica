package com.synexis.management_service.dto;

import com.synexis.management_service.models.Role;

/**
 * HTTP response body returned after a successful registration ({@code 201 Created}). Exposes safe fields only —
 * no password or hash.
 *
 * <p>How it works: Built in {@link com.synexis.management_service.service.AuthService#register(RegisterRequest)
 * AuthService.register} from the persisted {@link com.synexis.management_service.models.User User} entity.
 */
public record RegisterResponse(Long id, String email, Role role, String profilePicturePath) {}
