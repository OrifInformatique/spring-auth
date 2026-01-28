package ch.sectioninformatique.auth.security;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.app.exceptions.AppException;

/**
 * Security and authorization-related exceptions for the security package.
 */
public class SecurityExceptions {

    /**
     * Thrown when a role with the given name is not found.
     */
    public static class RoleNotFoundException extends AppException {
        public RoleNotFoundException(RoleEnum role) {
            super("error.security.role.not.found", HttpStatus.NOT_FOUND, role.name());
        }
    }

    /**
     * Thrown when an Azure token is not from a trusted tenant.
     */
    public static class TokenNotFromTrustedTenantException extends AppException {
        public TokenNotFromTrustedTenantException() {
            super("error.security.token.untrusted.tenant", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Thrown when a required claim is missing from the JWT token.
     */
    public static class MissingJwtClaimException extends AppException {
        public MissingJwtClaimException(String claimName) {
            super("error.security.jwt.missing.claim", HttpStatus.FORBIDDEN, claimName);
        }
    }

    /**
     * Thrown when a user attempts an action they are not authorized to perform.
     */
    public static class UnauthorizedActionException extends AppException {
        public UnauthorizedActionException(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Thrown when a user attempts an action they don't have permissions for due to insufficient rights.
     */
    public static class UserHasLowerRightsException extends AppException {
        public UserHasLowerRightsException(String login) {
            super("error.security.insufficient.rights", HttpStatus.FORBIDDEN, login);
        }
    }
}
