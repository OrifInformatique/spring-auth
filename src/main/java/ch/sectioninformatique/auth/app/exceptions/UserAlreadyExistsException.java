package ch.sectioninformatique.auth.app.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String login) {
        super("User already exists: " + login);
    }
}