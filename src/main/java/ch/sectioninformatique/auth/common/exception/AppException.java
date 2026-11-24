package ch.sectioninformatique.auth.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for business exceptions that map to an HTTP error response.
 *
 * Each exception carries the HTTP status to return and an i18n message key
 * (with optional arguments). {@link GlobalExceptionHandler} resolves the key
 * against the current locale, so the exception itself never holds user-facing text.
 */
public abstract class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] messageArgs;

    /**
     * @param status      HTTP status returned to the client
     * @param messageKey  i18n key of the error message
     * @param messageArgs arguments injected in the localized message
     */
    protected AppException(HttpStatus status, String messageKey, Object... messageArgs) {
        // The key is used as technical message so that logs stay readable
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }
}
