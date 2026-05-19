package ch.sectioninformatique.auth.auth;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.app.exceptions.MessageKeyProvider;

/**
 * Authentication-related exceptions for the auth package.
 */
public class AuthExceptions {

    /**
     * Thrown when provided credentials are invalid.
     */
    public static class InvalidCredentialsException extends AppException implements MessageKeyProvider {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED);
        }

        @Override
        public String getMessageKey() {
            return "error.authorisation.invalid.credentials";
        }
    }

    /**
     * Thrown when the refresh token is invalid.
     */
    public static class InvalidRefreshTokenException extends AppException implements MessageKeyProvider {
        public InvalidRefreshTokenException() {
            super(HttpStatus.UNAUTHORIZED);
        }

        @Override
        public String getMessageKey() {
            return "error.security.refresh.token.invalid";
        }
    }


    public static class AuthCodeNotFoundException extends AppException implements MessageKeyProvider{

        public AuthCodeNotFoundException(String login){
            super(HttpStatus.NOT_FOUND);
        }

        @Override
        public String getMessageKey(){
            return "error.authcode.not.found";
        }

    }
}
