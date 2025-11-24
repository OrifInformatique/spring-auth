package ch.sectioninformatique.auth.app.exceptions;

public class UserAlreadyExistsException extends AppException {
    public UserAlreadyExistsException(String login) {
        super("User already exists: " + login);
    }
}