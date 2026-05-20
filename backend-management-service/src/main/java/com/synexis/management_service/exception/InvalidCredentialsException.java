package com.synexis.management_service.exception;

/** Thrown when email does not exist for the expected role/app. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("The credentials are not correct.");
    }
}

