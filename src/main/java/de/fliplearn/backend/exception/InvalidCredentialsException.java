package de.fliplearn.backend.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-Mail-Adresse oder Passwort ist falsch.");
    }
}
