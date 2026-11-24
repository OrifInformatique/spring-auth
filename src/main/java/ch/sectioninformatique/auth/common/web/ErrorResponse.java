package ch.sectioninformatique.auth.common.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body of every error response returned by the API.
 *
 * The same structure is produced by the global exception handler, the JWT filter
 * and the security entry points, so clients only have one format to handle.
 *
 * @param timestamp   when the error occurred
 * @param status      HTTP status code
 * @param error       HTTP reason phrase (e.g. "Unauthorized")
 * @param message     localized, human-readable error message
 * @param fieldErrors validation messages keyed by field name, only present on validation errors
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return of(status, message, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
    }
}
