package ch.sectioninformatique.auth.auth;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.common.exception.AppException;

/**
 * Authentication-related exceptions.
 */
public final class AuthExceptions {

    private AuthExceptions() {
    }

    /**
     * Thrown when a login or password is wrong. The message is intentionally the same
     * in both cases, so that the API does not reveal which logins exist.
     */
    public static class InvalidCredentialsException extends AppException {
        public InvalidCredentialsException() {
            super(HttpStatus.UNAUTHORIZED, "error.authorisation.invalid.credentials");
        }
    }

    /**
     * Thrown when a refresh token is invalid, expired, revoked or replaced by a newer one.
     */
    public static class InvalidRefreshTokenException extends AppException {
        public InvalidRefreshTokenException() {
            super(HttpStatus.UNAUTHORIZED, "error.security.refresh.token.invalid");
        }
    }

    /**
     * Thrown when an OAuth2 authentication code is unknown, expired, already used,
     * or does not belong to the given user.
     */
    public static class InvalidAuthCodeException extends AppException {
        public InvalidAuthCodeException() {
            super(HttpStatus.BAD_REQUEST, "error.authcode.invalid");
        }
    }

    /**
     * Thrown when the identity provider does not return an attribute required to
     * create or identify the user (e.g. the email).
     */
    public static class MissingOAuth2AttributeException extends AppException {
        public MissingOAuth2AttributeException(String attribute) {
            super(HttpStatus.UNAUTHORIZED, "error.oauth2.missing.user.attribute", attribute);
        }
    }

    /**
     * Thrown when the Azure login could not be completed (refused consent, invalid state, ...).
     */
    public static class OAuth2LoginFailedException extends AppException {
        public OAuth2LoginFailedException() {
            super(HttpStatus.UNAUTHORIZED, "error.oauth2.login.failed");
        }
    }
}
