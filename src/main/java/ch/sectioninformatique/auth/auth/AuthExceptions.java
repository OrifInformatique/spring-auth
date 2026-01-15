package ch.sectioninformatique.auth.auth;

import ch.sectioninformatique.auth.app.exceptions.AppException;

/**
 * Authentication-related exceptions for the auth package.
 */
public class AuthExceptions {

    /**
     * Thrown when provided credentials are invalid.
     */
    public static class InvalidCredentialsException extends AppException {
        public InvalidCredentialsException() {
            super("Invalid credentials");
        }
    }
}
