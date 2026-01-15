package ch.sectioninformatique.auth.user;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.app.exceptions.AppException;

/**
 * User-related exceptions for the user package.
 */
public class UserExceptions {

    /**
     * Thrown when a user with the given login already exists.
     */
    public static class UserAlreadyExistsException extends AppException {
        public UserAlreadyExistsException(String login) {
            super("User already exists: " + login, HttpStatus.CONFLICT);
        }
    }

    /**
     * Thrown when a user with the given login or ID is not found.
     */
    public static class UserNotFoundException extends AppException {
        public UserNotFoundException(String loginOrId) {
            super("User not found: " + loginOrId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Thrown when attempting to promote a user to admin when they are already admin.
     */
    public static class UserAlreadyAdminException extends AppException {
        public UserAlreadyAdminException(String login) {
            super("User already admin: " + login, HttpStatus.CONFLICT);
        }
    }

    /**
     * Thrown when attempting to promote a user to manager when they are already manager.
     */
    public static class UserAlreadyManagerException extends AppException {
        public UserAlreadyManagerException(String login) {
            super("User already manager: " + login, HttpStatus.CONFLICT);
        }
    }

    /**
     * Thrown when attempting to demote a user to regular when they are already regular.
     */
    public static class UserAlreadyRegularException extends AppException {
        public UserAlreadyRegularException(String login) {
            super("User already regular: " + login, HttpStatus.CONFLICT);
        }
    }
}
