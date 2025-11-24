package ch.sectioninformatique.auth.app.exceptions;

public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}