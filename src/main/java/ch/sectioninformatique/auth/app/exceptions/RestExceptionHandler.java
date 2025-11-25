package ch.sectioninformatique.auth.app.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.security.access.AccessDeniedException;

import ch.sectioninformatique.auth.app.errors.ErrorDto;

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
     * @return ResponseEntity containing the error details and appropriate status
     *         code
     */
    @ExceptionHandler(value = { AppException.class })
    @ResponseBody
    public ResponseEntity<ErrorDto> handleException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorDto(ex.getMessage()));
    }

    /**
     * Handles validation exceptions and converts them to error responses.
     * This method:
     * - Is annotated with @ExceptionHandler to catch
     * MethodArgumentNotValidException instances
     * - Returns a ResponseEntity with HTTP 400 Bad Request status and validation
     * error message
     * - Wraps the error message in an ErrorDto object
     * - Is used for all REST endpoints in the application
     *
     * @param ex The MethodArgumentNotValidException that was thrown
     * @return ResponseEntity containing the validation error details and HTTP 400
     *         status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorDto> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDto(errorMessage));
    }

    /**
     * Handles HTTP message not readable exceptions and converts them to error
     * responses.
     * This method:
     * - Is annotated with @ExceptionHandler to catch
     * HttpMessageNotReadableException instances
     * - Returns a ResponseEntity with HTTP 400 Bad Request status and error message
     * - Wraps the error message in an ErrorDto object
     * - Is used for all REST endpoints in the application
     *
     * @param ex The HttpMessageNotReadableException that was thrown
     * @return ResponseEntity containing the error details and HTTP 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public ResponseEntity<ErrorDto> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Request body is missing or unreadable";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorDto(message));
    }

    /**
     * Handles HTTP media type not supported exceptions and converts them to error
     * responses.
     * This method:
     * - Is annotated with @ExceptionHandler to catch
     * HttpMediaTypeNotSupportedException instances
     * - Returns a ResponseEntity with HTTP 415 Unsupported Media Type status and
     * error message
     * - Wraps the error message in an ErrorDto object
     * - Is used for all REST endpoints in the application
     *
     * @param ex The HttpMediaTypeNotSupportedException that was thrown
     * @return ResponseEntity containing the error details and HTTP 415 status
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseBody
    public ResponseEntity<ErrorDto> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String message = "Unsupported content type: " + ex.getContentType();
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorDto(message));
    }

    /**
     * Handles access denied exceptions and converts them to error responses.
     * This method:
     * - Is annotated with @ExceptionHandler to catch AccessDeniedException
     * instances
     * - Returns a ResponseEntity with HTTP 403 Forbidden status and error message
     * - Wraps the error message in an ErrorDto object
     * - Is used for all REST endpoints in the application
     *
     * @param ex The AccessDeniedException that was thrown
     * @return ResponseEntity containing the error details and HTTP 403 status
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public ResponseEntity<ErrorDto> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto("Access is denied"));
    }
}
