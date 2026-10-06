package ch.sectioninformatique.auth.user;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.common.exception.AppException;
import ch.sectioninformatique.auth.role.RoleEnum;

/**
 * User management exceptions.
 */
public final class UserExceptions {

    private UserExceptions() {
    }

    /** Thrown when no user matches the given login or id. */
    public static class UserNotFoundException extends AppException {
        public UserNotFoundException(String loginOrId) {
            super(HttpStatus.NOT_FOUND, "error.user.not.found", loginOrId);
        }
    }

    /** Thrown when creating or renaming a user with a login that is already used. */
    public static class UserAlreadyExistsException extends AppException {
        public UserAlreadyExistsException(String login) {
            super(HttpStatus.CONFLICT, "error.user.already.exists", login);
        }
    }

    /** Thrown when assigning a role the user already has. */
    public static class UserAlreadyHasRoleException extends AppException {
        public UserAlreadyHasRoleException(String login, RoleEnum role) {
            super(HttpStatus.CONFLICT, "error.user.already.has.role", login, role.name());
        }
    }

    /** Thrown when a role is missing from the database (roles are seeded at startup). */
    public static class RoleNotFoundException extends AppException {
        public RoleNotFoundException(RoleEnum role) {
            super(HttpStatus.INTERNAL_SERVER_ERROR, "error.role.not.found", role.name());
        }
    }

    /**
     * Thrown when users try to change their own role or delete their own account,
     * which could leave the application without any administrator.
     */
    public static class SelfModificationForbiddenException extends AppException {
        public SelfModificationForbiddenException() {
            super(HttpStatus.FORBIDDEN, "error.user.self.modification.forbidden");
        }
    }

    /** Thrown when the current password given to change a password is wrong. */
    public static class InvalidCurrentPasswordException extends AppException {
        public InvalidCurrentPasswordException() {
            super(HttpStatus.BAD_REQUEST, "error.user.current.password.invalid");
        }
    }

    /** Thrown when a soft-deleted user tries to log in through Azure. */
    public static class DeletedAccountException extends AppException {
        public DeletedAccountException(String login) {
            super(HttpStatus.FORBIDDEN, "error.user.account.deleted", login);
        }
    }
}
