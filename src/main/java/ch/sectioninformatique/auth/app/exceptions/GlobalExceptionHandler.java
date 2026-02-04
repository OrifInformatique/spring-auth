package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private MessageSource messageSource;

    // Helper method to format responses
    private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", status.value(),
                        "error", status.getReasonPhrase(),
                        "message", message));
    }

    // -------------------------------
    // App exceptions
    // -------------------------------
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleAppException(AppException ex) {
        String message;
        
        // If the exception has a message key, resolve it from messages.properties
        if (ex.getMessageKey() != null) {
            message = messageSource.getMessage(
                ex.getMessageKey(), 
                ex.getMessageArgs(), 
                ex.getMessage(), // fallback to default message
                LocaleContextHolder.getLocale()
            );
        } else {
            // Legacy behavior: use the message directly
            message = ex.getMessage();
        }
        
        return buildResponse(ex.getStatus(), message);
    }

    // -------------------------------
    // Spring built-in Errors
    // -------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect ALL field errors, not just the first one
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(error -> {
                String defaultMessage = error.getDefaultMessage();
                String messageKey = defaultMessage != null ? defaultMessage : "error.validation.failed";
                String resolved = messageSource.getMessage(
                    messageKey,
                    null,
                    defaultMessage != null
                        ? defaultMessage
                        : messageSource.getMessage(
                            "error.validation.failed",
                            null,
                            LocaleContextHolder.getLocale()
                        ),
                    LocaleContextHolder.getLocale()
                );
                fieldErrors.put(error.getField(), resolved);
            });

        // Create a single message combining all field errors for backward compatibility
        String combinedMessage = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((e1, e2) -> e1 + "; " + e2)
            .orElse(messageSource.getMessage(
                "error.validation.failed",
                null,
                LocaleContextHolder.getLocale()
            ));

        // Build response with both message and detailed fieldErrors
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", messageSource.getMessage(
            "error.validation.failed.title",
            null,
            LocaleContextHolder.getLocale()
        ));
        response.put("message", combinedMessage);
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        String message = messageSource.getMessage(
                "error.media.type.unsupported",
                new Object[] {ex.getContentType(), ex.getSupportedMediaTypes()},
                ex.getMessage(),
                LocaleContextHolder.getLocale()
        );
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            messageSource.getMessage(
                "error.request.parameter.missing",
                new Object[] {ex.getParameterName()},
                LocaleContextHolder.getLocale()
            ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMalformedJson(HttpMessageNotReadableException ex) {
        String message = messageSource.getMessage(
            "error.request.json.malformed.or.missing",
            null,
            LocaleContextHolder.getLocale()
        );
        
        // Extract more specific error information if available
        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            // Provide more specific guidance based on the parsing error
            if (causeMessage != null) {
                if (causeMessage.contains("Unexpected end-of-input")) {
                    message = messageSource.getMessage(
                            "error.request.json.incomplete",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                } else if (causeMessage.contains("Unexpected character")) {
                    message = messageSource.getMessage(
                            "error.request.json.invalid.character",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                } else if (causeMessage.contains("cannot deserialize")) {
                    message = messageSource.getMessage(
                            "error.request.json.invalid.value.type",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                } else if (causeMessage.contains("No content to map")) {
                    message = messageSource.getMessage(
                            "error.request.json.empty.or.missing",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                }
            }
        }
        
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }
}
