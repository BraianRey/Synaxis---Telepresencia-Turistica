package com.synexis.management_service.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(Long clientId) {
        super("Client not found with ID: " + clientId);
    }
}
