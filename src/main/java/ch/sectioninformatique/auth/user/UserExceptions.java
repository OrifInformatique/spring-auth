package ch.sectioninformatique.auth.user;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.app.exceptions.MessageKeyProvider;

/**
 * User-related exceptions for the user package.
 */
public class UserExceptions {

    private abstract static class LoginBasedException extends AppException implements MessageKeyProvider {
        private final String login;

        protected LoginBasedException(HttpStatus status, String login) {
            super(status);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }

        @Override
        public Object[] getMessageArgs() {
            return new Object[] { login };
        }
    }

    /**
     * Thrown when a user with the given login already exists.
     */
    public static class UserAlreadyExistsException extends LoginBasedException {
        public UserAlreadyExistsException(String login) {
            super(HttpStatus.CONFLICT, login);
        }

        @Override
        public String getMessageKey() {
            return "error.user.already.exists";
        }

        @Override
        public Object[] getMessageArgs() {
            return super.getMessageArgs();
        }
    }

    /**
     * Thrown when a user with the given login or ID is not found.
     */
    public static class UserNotFoundException extends AppException implements MessageKeyProvider {
        private final String loginOrId;

        public UserNotFoundException(String loginOrId) {
            super(HttpStatus.NOT_FOUND);
            this.loginOrId = loginOrId;
        }

        public String getLoginOrId() {
            return loginOrId;
        }

        @Override
        public String getMessageKey() {
            return "error.user.not.found";
        }

        @Override
        public Object[] getMessageArgs() {
            return new Object[] { loginOrId };
        }
    }

    /**
     * Thrown when attempting to promote a user to admin when they are already admin.
     */
    public static class UserAlreadyAdminException extends LoginBasedException {
        public UserAlreadyAdminException(String login) {
            super(HttpStatus.CONFLICT, login);
        }

        @Override
        public String getMessageKey() {
            return "error.user.already.admin";
        }

        @Override
        public Object[] getMessageArgs() {
            return super.getMessageArgs();
        }
    }

    /**
     * Thrown when attempting to promote a user to manager when they are already manager.
     */
    public static class UserAlreadyManagerException extends LoginBasedException {
        public UserAlreadyManagerException(String login) {
            super(HttpStatus.CONFLICT, login);
        }

        @Override
        public String getMessageKey() {
            return "error.user.already.manager";
        }

        @Override
        public Object[] getMessageArgs() {
            return super.getMessageArgs();
        }
    }

    /**
     * Thrown when attempting to demote a user to regular when they are already regular.
     */
    public static class UserAlreadyRegularException extends LoginBasedException {
        public UserAlreadyRegularException(String login) {
            super(HttpStatus.CONFLICT, login);
        }

        @Override
        public String getMessageKey() {
            return "error.user.already.regular";
        }

        @Override
        public Object[] getMessageArgs() {
            return super.getMessageArgs();
        }
    }

}
