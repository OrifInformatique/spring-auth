package ch.sectioninformatique.template.app.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class for application-specific errors.
 * This exception:
 * - Extends RuntimeException for unchecked exception behavior
 * - Includes an HTTP status code for REST API responses
 * - Provides a standardized way to handle application errors
 * - Can be used to wrap business logic exceptions
 */
public class AppException extends RuntimeException {

    /**
     * The HTTP status code associated with this exception.
     * This field:
     * - Is final to ensure immutability
     * - Represents the appropriate HTTP response status
     * - Is used to generate proper REST API responses
     */
    private final HttpStatus status;

    /**
     * Constructs a new AppException with the specified message and status.
     * This constructor:
     * - Initializes the exception with a descriptive message
     * - Sets the HTTP status code for the response
     * - Calls the parent RuntimeException constructor
     *
     * @param message The error message describing the exception
     * @param status The HTTP status code to be returned in the response
     */
    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     * This method:
     * - Provides access to the status code for response generation
     * - Is used by exception handlers to set the response status
     *
     * @return The HTTP status code for this exception
     */
    public HttpStatus getStatus() {
        return status;
    }
}

