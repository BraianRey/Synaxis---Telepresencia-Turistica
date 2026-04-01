package com.synexis.management_service.exception;

/** Thrown when email exists but supplied password is incorrect. */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException() {
        super("La contraseña es incorrecta. Vuelve a intentarlo.");
    }
}

