package ch.sectioninformatique.auth.security;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.common.exception.AppException;

/**
 * Security-related exceptions.
 */
public final class SecurityExceptions {

    private SecurityExceptions() {
    }

    /**
     * Thrown when the authenticated user tries to act on an account or grant a role
     * that requires higher rights (e.g. a manager acting on an admin account).
     */
    public static class InsufficientRightsException extends AppException {
        public InsufficientRightsException(String actorLogin) {
            super(HttpStatus.FORBIDDEN, "error.security.insufficient.rights", actorLogin);
        }
    }

    /**
     * Thrown at startup when the CORS configuration is invalid or insecure.
     */
    public static class CorsConfigurationException extends AppException {
        public CorsConfigurationException(String messageKey, Object... args) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, messageKey, args);
        }
    }
}
