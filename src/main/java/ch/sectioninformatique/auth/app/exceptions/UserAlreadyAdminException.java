package ch.sectioninformatique.auth.app.exceptions;


public class UserAlreadyAdminException extends AppException {
    public UserAlreadyAdminException(String login) {
        super("The user is already an admin: " + login);
    }
}