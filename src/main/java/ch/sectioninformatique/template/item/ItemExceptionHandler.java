package ch.sectioninformatique.template.item;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

/**
 * Global exception handler for item-related exceptions.
 * This class provides centralized exception handling for the item module,
 * converting exceptions into appropriate HTTP responses.
 */
@ControllerAdvice
public class ItemExceptionHandler {

    /**
     * Handles ItemNotFoundException by returning a 404 Not Found response
     * with the exception message as the response body.
     *
     * @param e The ItemNotFoundException that was thrown
     * @return The error message from the exception
     */
    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseBody 
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String itemNotFoundHandler(ItemNotFoundException e) {
        return e.getMessage();
    }
}
