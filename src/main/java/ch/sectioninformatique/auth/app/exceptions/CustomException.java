package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.http.HttpStatus;

public class CustomException extends AppException {
    public CustomException(String messsage, HttpStatus status) {
        super(messsage, status);
    }
}
