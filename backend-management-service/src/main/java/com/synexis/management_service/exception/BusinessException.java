package com.synexis.management_service.exception;

/**
 * Thrown when a business-related error occurs (for example, invalid input data
 * or missing required fields).
 *
 * <p>
 * Mapped to {@code 422 UNPROCESSABLE ENTITY} by {@link RestExceptionHandler}.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}