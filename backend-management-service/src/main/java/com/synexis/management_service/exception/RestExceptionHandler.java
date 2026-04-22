package com.synexis.management_service.exception;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<Map<String, String>> handleWrongPassword(WrongPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

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

    /**
     * Business rule violations (invalid state transitions, unavailable partner,
     * etc.) → {@code 409 CONFLICT}.
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Concurrent update (e.g. two partners racing to accept) → retry or refresh.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Resource was modified by another request; please retry"));
    }

    /**
     * Catch-all for any unexpected exception so we return a JSON body instead of
     * the default Spring HTML error page. This makes debugging much easier from
     * mobile clients that expect JSON.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected server error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", message));
    }
}
