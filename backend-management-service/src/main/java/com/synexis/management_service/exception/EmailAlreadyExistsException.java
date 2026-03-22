package com.synexis.management_service.exception;

/**
 * Business rule violation: registration attempted with an email that already exists (after normalization).
 *
 * <p>How it works: Thrown from {@link com.synexis.management_service.service.AuthService#register(
 * com.synexis.management_service.dto.RegisterRequest) AuthService.register}; mapped to HTTP {@code 409 Conflict} by
 * {@link RestExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
