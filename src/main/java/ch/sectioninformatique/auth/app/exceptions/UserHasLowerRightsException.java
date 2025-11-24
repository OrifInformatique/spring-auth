package ch.sectioninformatique.auth.app.exceptions;

public class UserHasLowerRightsException extends RuntimeException {
    public UserHasLowerRightsException(String login) {
        super("The user has lower rights than desired: " + login);
    }
}