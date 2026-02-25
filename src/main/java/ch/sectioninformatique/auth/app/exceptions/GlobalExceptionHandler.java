package ch.sectioninformatique.auth.app.exceptions;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the application.
 * Catches both custom AppExceptions and common Spring exceptions to return
 * consistent error responses.
 * 
 * For AppExceptions, it uses the provided HTTP status and resolves a localized
 * message if available.
 * For validation errors, it returns a generic localized message and exposes
 * per-field details in a structured fieldErrors object.
 * For other exceptions, it returns a generic error message with the appropriate
 * HTTP status.
 * 
 * This ensures that clients receive clear and consistent error information for
 * all types of exceptions.
 */
@ControllerAdvice
// Handlers first, helpers last for a top-down read.
public class GlobalExceptionHandler {

    private static final String VALIDATION_FAILED_MESSAGE_KEY = "error.validation.failed";

    // MessageSource is used to resolve localized messages for exceptions that
    // implement MessageKeyProvider
    private final MessageSource messageSource;

    // Constructor injection of MessageSource allows for better testability and
    // decoupling from the Spring context
    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handles custom application exceptions (AppException and its subclasses).
     * Returns a structured error response with the HTTP status and a localized
     * message if available.
     * 
     * @param ex The AppException to handle
     * @return ResponseEntity containing the error details and appropriate HTTP
     *         status
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppException(AppException ex) {
        return buildResponse(ex.getStatus(), resolveAppExceptionMessage(ex));
    }

    /**
     * Handles validation errors that occur when @Valid annotated request bodies
     * fail validation.
        * Uses a generic top-level message and returns detailed validation
        * information in fieldErrors for client-side field mapping.
     * 
     * @param ex The MethodArgumentNotValidException to handle
     * @return ResponseEntity containing the error details and appropriate HTTP
     *         status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex) {

        // Collect detailed validation errors keyed by field name.
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::resolveValidationFieldError,
                        (existing, replacement) -> replacement));

        // Keep the top-level message generic and localized.
        String genericMessage = msg(VALIDATION_FAILED_MESSAGE_KEY);

        // Include fieldErrors as the source of truth for frontend rendering.
        Map<String, Object> response = new java.util.LinkedHashMap<>(
            errorResponse(HttpStatus.BAD_REQUEST, genericMessage));
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handles cases where the client sends a request with an unsupported media
     * type.
     * Returns a structured error response indicating the unsupported media type and
     * supported types.
     * 
     * @param ex The HttpMediaTypeNotSupportedException to handle
     * @return ResponseEntity containing the error details and appropriate HTTP
     *         status
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    /**
     * Handles cases where a required request parameter is missing.
     * Returns a structured error response indicating the missing parameter.
     * 
     * @param ex The MissingServletRequestParameterException to handle
     * @return ResponseEntity containing the error details and appropriate HTTP
     *         status
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles cases where the request body is not readable, typically due to
     * malformed JSON.
     * Returns a structured error response indicating the issue with the request
     * body.
     * 
     * @param ex The HttpMessageNotReadableException to handle
     * @return ResponseEntity containing the error details and appropriate HTTP
     *         status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Helper method to construct a structured error response with a timestamp,
     * status code, error reason, and message.
     * This method centralizes the error response format for consistency across all
     * exception handlers.
     * 
     * @param status  The HTTP status to include in the response
     * @param message The error message to include in the response
     * @return A map containing the structured error response
     */
    private Map<String, Object> errorResponse(HttpStatus status, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message);
    }

    /**
     * Helper method to build a ResponseEntity with a structured error response
     * body.
     * This method uses the errorResponse helper to create a consistent response
     * format and sets the appropriate
     * HTTP status code for the response.
     * 
     * @param status  The HTTP status to set for the response
     * @param message The error message to include in the response body
     * @return ResponseEntity containing the structured error response and
     *         appropriate HTTP status
     */
    private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(errorResponse(status, message));
    }

    /**
     * Helper method to resolve a localized message for an AppException that
     * implements MessageKeyProvider.
     * If the exception does not implement MessageKeyProvider, it returns a generic
     * unexpected error message
     * 
     * @param key  The message key to resolve from the MessageSource
     * @param args Optional arguments to include in the message formatting
     * @return The resolved localized message based on the current locale, or a
     *         generic error message if the key is not found
     */
    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    /**
     * Helper method to resolve a localized message for an AppException that
     * implements MessageKeyProvider.
     * If the exception does not implement MessageKeyProvider, it returns a generic
     * unexpected error message.
     * 
     * @param ex The AppException for which to resolve the message
     * @return The resolved localized message based on the exception's message key
     *         and arguments, or a generic error message if the exception does not
     *         provide a message key
     */
    private String resolveAppExceptionMessage(AppException ex) {
        if (!(ex instanceof MessageKeyProvider)) {
            return msg("error.unexpected");
        }

        MessageKeyProvider provider = (MessageKeyProvider) ex;
        return msg(provider.getMessageKey(), provider.getMessageArgs());
    }

    /**
     * Helper method to resolve a validation field error message.
     * If the field error has a default message, it returns that message; otherwise,
     * it returns the field name.
     * 
     * @param error The FieldError to resolve
     * @return The resolved validation error message
     */
    private String resolveValidationFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        return defaultMessage != null ? defaultMessage : error.getField();
    }
}
