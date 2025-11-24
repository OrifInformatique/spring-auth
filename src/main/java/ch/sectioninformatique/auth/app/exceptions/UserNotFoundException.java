package ch.sectioninformatique.auth.app.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String loginOrId) {
        super("User not found: " + loginOrId);
    }
}