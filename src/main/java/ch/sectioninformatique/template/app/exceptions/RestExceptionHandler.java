package ch.sectioninformatique.template.app.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.sectioninformatique.template.app.errors.ErrorDto;

/**
 * Global exception handler for REST API endpoints.
 * This class:
 * - Is annotated with @ControllerAdvice for global exception handling
 * - Provides centralized exception handling for the application
 * - Converts exceptions into standardized error responses
 * - Ensures consistent error handling across all endpoints
 */
@ControllerAdvice
public class RestExceptionHandler {

    /**
     * Handles AppException instances and converts them to error responses.
     * This method:
     * - Is annotated with @ExceptionHandler to catch AppException instances
     * - Returns a ResponseEntity with appropriate HTTP status and error message
     * - Wraps the error message in an ErrorDto object
     * - Is used for all REST endpoints in the application
     *
     * @param ex The AppException that was thrown
     * @return ResponseEntity containing the error details and appropriate status code
     */
    @ExceptionHandler(value = { AppException.class })
    @ResponseBody
    public ResponseEntity<ErrorDto> handleException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorDto(ex.getMessage()));
    }
}



