package ch.sectioninformatique.auth.app.exceptions;

public class UserAlreadyManagerException extends AppException {
    public UserAlreadyManagerException(String login) {
        super("The user is already a manager: " + login);
    }
}