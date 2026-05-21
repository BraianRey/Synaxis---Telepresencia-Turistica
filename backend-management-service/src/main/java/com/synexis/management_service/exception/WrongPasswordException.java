package com.synexis.management_service.exception;

/** Thrown when email exists but supplied password is incorrect. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("The password is incorrect. Please try again.");
    }
}

