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
        private final String login;

        public UserAlreadyExistsException(String login) {
            super(HttpStatus.CONFLICT);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }
    }

    /**
     * Thrown when a user with the given login or ID is not found.
     */
    public static class UserNotFoundException extends AppException {
        private final String loginOrId;

        public UserNotFoundException(String loginOrId) {
            super(HttpStatus.NOT_FOUND);
            this.loginOrId = loginOrId;
        }

        public String getLoginOrId() {
            return loginOrId;
        }
    }

    /**
     * Thrown when attempting to promote a user to admin when they are already admin.
     */
    public static class UserAlreadyAdminException extends AppException {
        private final String login;

        public UserAlreadyAdminException(String login) {
            super(HttpStatus.CONFLICT);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }
    }

    /**
     * Thrown when attempting to promote a user to manager when they are already manager.
     */
    public static class UserAlreadyManagerException extends AppException {
        private final String login;

        public UserAlreadyManagerException(String login) {
            super(HttpStatus.CONFLICT);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }
    }

    /**
     * Thrown when attempting to demote a user to regular when they are already regular.
     */
    public static class UserAlreadyRegularException extends AppException {
        private final String login;

        public UserAlreadyRegularException(String login) {
            super(HttpStatus.CONFLICT);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }
    }
}
