package com.synexis.management_service.exception;

/**
 * Thrown when a domain/business rule is violated (for example, invalid state
 * transition or unavailable partner).
 *
 * <p>
 * Mapped to {@code 409 CONFLICT} by {@link RestExceptionHandler}.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}

