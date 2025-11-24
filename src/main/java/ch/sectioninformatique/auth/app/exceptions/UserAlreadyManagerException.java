package ch.sectioninformatique.auth.app.exceptions;

public class UserAlreadyManagerException extends RuntimeException {
    public UserAlreadyManagerException(String login) {
        super("The user is already a manager: " + login);
    }
}