package ch.sectioninformatique.auth.app.exceptions;

public class UserAlreadyRegularException extends RuntimeException {
    public UserAlreadyRegularException(String login) {
        super("The user is already a regular user: " + login);
    }
}