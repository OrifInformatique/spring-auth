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
            super("Role not found: " + role.name(), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Thrown when a security validation fails or unauthorized access is attempted.
     */
    public static class SecurityException extends AppException {
        public SecurityException(String message) {
            super(message, HttpStatus.FORBIDDEN);
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
            super("User has insufficient rights: " + login, HttpStatus.FORBIDDEN);
        }
    }
}
