package com.synexis.management_service.exception;

/**
 * Thrown when an authenticated user tries to access a resource they are not
 * allowed to see or modify.
 *
 * <p>
 * Mapped to {@code 403 FORBIDDEN} by {@link RestExceptionHandler}.
 * </p>
 */
public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
