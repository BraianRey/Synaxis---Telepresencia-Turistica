package com.synexis.management_service.exception;

/**
 * Exception thrown when a user cannot be created in Keycloak.
 */
public class KeycloakUserCreationException extends RuntimeException {

    public KeycloakUserCreationException(int statusCode) {
        super("Error creating user in Keycloak with status code: " + statusCode);
    }

    public KeycloakUserCreationException(String message) {
        super(message);
    }
}
