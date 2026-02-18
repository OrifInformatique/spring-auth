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
        private final RoleEnum role;

        public RoleNotFoundException(RoleEnum role) {
            super(HttpStatus.NOT_FOUND);
            this.role = role;
        }

        public RoleEnum getRole() {
            return role;
        }
    }

    /**
     * Thrown when an Azure token is not from a trusted tenant.
     */
    public static class TokenNotFromTrustedTenantException extends AppException {
        public TokenNotFromTrustedTenantException() {
            super(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Thrown when a required claim is missing from the JWT token.
     */
    public static class MissingJwtClaimException extends AppException {
        private final String claimName;

        public MissingJwtClaimException(String claimName) {
            super(HttpStatus.FORBIDDEN);
            this.claimName = claimName;
        }

        public String getClaimName() {
            return claimName;
        }
    }

    /**
     * Thrown when a user attempts an action they are not authorized to perform.
     */
    public static class UnauthorizedActionException extends AppException {
        public UnauthorizedActionException() {
            super(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Thrown when a user attempts an action they don't have permissions for due to insufficient rights.
     */
    public static class UserHasLowerRightsException extends AppException {
        private final String login;

        public UserHasLowerRightsException(String login) {
            super(HttpStatus.FORBIDDEN);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }
    }
}
