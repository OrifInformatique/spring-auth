package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class for application-specific errors.
 * This exception can optionally carry an HTTP status code for REST API responses.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs a new AppException with a specific HTTP status.
     *
     * @param status     The HTTP status code to associate with this exception
     */
    public AppException(HttpStatus status) {
        super();
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
