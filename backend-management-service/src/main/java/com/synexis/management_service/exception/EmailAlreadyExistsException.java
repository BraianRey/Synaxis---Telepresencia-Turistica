package com.synexis.management_service.exception;

/**
 * Business rule violation: registration attempted with an email that already
 * exists (after normalization).
 *
 * <p>
 * Thrown when registering a client if that email already exists in
 * {@code clients}, or when registering a partner
 * if it already exists in {@code partners}. The same email may exist in both
 * tables (same person as client and
 * partner). Mapped to HTTP {@code 409} by {@link RestExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
