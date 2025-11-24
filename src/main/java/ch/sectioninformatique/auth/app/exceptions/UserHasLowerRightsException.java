package ch.sectioninformatique.auth.app.exceptions;

public class UserHasLowerRightsException extends AppException {
    public UserHasLowerRightsException(String login) {
        super("The user has lower rights than desired: " + login);
    }
}