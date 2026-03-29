package com.synexis.management_service.exception;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global REST error mapping: converts selected exceptions into JSON bodies with
 * appropriate HTTP status codes.
 *
 * <p>
 * How it works: Spring MVC invokes these methods when a controller throws
 * {@link EmailAlreadyExistsException} or
 * when validation fails on {@code @Valid} request bodies, avoiding duplicated
 * try/catch in every controller.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    /** Duplicate email on register → {@code 409} with {@code {"error":"..."}}. */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Keycloak user creation failures → {@code 500} with {@code {"error":"..."}}.
     */
    @ExceptionHandler(KeycloakUserCreationException.class)
    public ResponseEntity<Map<String, String>> handleKeycloakUserCreation(KeycloakUserCreationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Bean Validation failures (bad email, short password, etc.) → {@code 400} with
     * a joined field error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /** Resource not found → {@code 404} with {@code {"error":"..."}}. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
