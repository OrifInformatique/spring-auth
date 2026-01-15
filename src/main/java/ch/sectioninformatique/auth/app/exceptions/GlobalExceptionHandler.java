package ch.sectioninformatique.auth.app.exceptions;

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
        return buildResponse(ex.getStatus(), ex.getMessage());
    }

    // -------------------------------
    // Spring built-in Errors
    // -------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect ALL field errors, not just the first one
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        // Create a single message combining all field errors for backward compatibility
        String combinedMessage = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((e1, e2) -> e1 + "; " + e2)
                .orElse("Validation failed");

        // Build response with both message and detailed fieldErrors
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", combinedMessage);
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParams(MissingServletRequestParameterException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getParameterName() + " parameter is missing");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMalformedJson(HttpMessageNotReadableException ex) {
        String message = "Malformed or missing JSON request body";
        
        // Extract more specific error information if available
        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            // Provide more specific guidance based on the parsing error
            if (causeMessage != null) {
                if (causeMessage.contains("Unexpected end-of-input")) {
                    message = "JSON is incomplete - missing closing bracket or quote";
                } else if (causeMessage.contains("Unexpected character")) {
                    message = "JSON contains invalid character - check for unescaped quotes or missing commas";
                } else if (causeMessage.contains("cannot deserialize")) {
                    message = "Invalid value type for a field - check your data types match the schema";
                } else if (causeMessage.contains("No content to map")) {
                    message = "Empty or missing request body";
                }
            }
        }
        
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }
}
