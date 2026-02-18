package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import ch.sectioninformatique.auth.web.ErrorMessageResolver;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private ErrorMessageResolver errorMessageResolver;

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
        return buildResponse(ex.getStatus(), errorMessageResolver.resolveAppExceptionMessage(ex));
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
                fieldErrors.put(
                    error.getField(),
                    errorMessageResolver.resolveValidationFieldError(error)
                );
            });

        // Create a single message combining all field errors for backward compatibility
        String combinedMessage = errorMessageResolver.resolveValidationCombinedMessage(fieldErrors);

        // Build response with both message and detailed fieldErrors
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", errorMessageResolver.resolveValidationTitle());
        response.put("message", combinedMessage);
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        String message = errorMessageResolver.resolveUnsupportedMediaTypeMessage(ex);
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            errorMessageResolver.resolveMissingParamMessage(ex));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessageResolver.resolveMalformedJsonMessage(ex));
    }
}
