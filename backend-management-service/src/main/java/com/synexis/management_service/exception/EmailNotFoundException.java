package com.synexis.management_service.exception;

public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(String email) {
        super("Email not found: " + email);
    }
}
