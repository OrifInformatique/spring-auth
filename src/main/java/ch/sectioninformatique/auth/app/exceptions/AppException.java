package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class for application-specific errors.
 * This exception can optionally carry an HTTP status code for REST API responses.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs a new AppException with a message
     *
     * @param message The error message
     */
    public AppException(String message) {
        super(message);
        this.status = null; // default
    }

    /**
     * Constructs a new AppException with a message and specific HTTP status.
     *
     * @param message The error message
     * @param status  The HTTP status code to associate with this exception
     */
    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return HttpStatus
     */
    public HttpStatus getStatus() {
        return status;
    }
}
