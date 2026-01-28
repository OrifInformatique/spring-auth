package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Custom exception class for application-specific errors.
 * This exception can optionally carry an HTTP status code for REST API responses.
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final Object[] messageArgs;

    /**
     * Constructs a new AppException with a message
     *
     * @param message The error message
     */
    public AppException(String message) {
        super(message);
        this.status = null; // default
        this.messageKey = null;
        this.messageArgs = null;
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
        this.messageKey = null;
        this.messageArgs = null;
    }

    /**
     * Constructs a new AppException with a message key for i18n and specific HTTP status.
     *
     * @param messageKey The message key for internationalization
     * @param status     The HTTP status code to associate with this exception
     * @param args       Arguments to be used in the message template
     */
    public AppException(String messageKey, HttpStatus status, Object... args) {
        super(messageKey); // fallback if message resolution fails
        this.status = status;
        this.messageKey = messageKey;
        this.messageArgs = args;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return HttpStatus
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Returns the message key for internationalization.
     *
     * @return The message key, or null if not using i18n
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * Returns the message arguments for interpolation.
     *
     * @return The message arguments, or null if none
     */
    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
