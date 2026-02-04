package ch.sectioninformatique.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.app.errors.ErrorDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.context.NoSuchMessageException;

import java.io.IOException;

/**
 * Entry point for handling unauthenticated requests.
 * This class implements Spring Security's AuthenticationEntryPoint to provide
 * a custom response when an unauthenticated user attempts to access a protected resource.
 * It returns a JSON response with an appropriate error message and HTTP 401 status code.
 * The response includes:
 * - HTTP 401 Unauthorized status code
 * - Content-Type: application/json header
 * - JSON body containing an error message
 */
@Component
public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Object mapper for JSON serialization of error responses */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MessageSource messageSource;

    public UserAuthenticationEntryPoint(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handles unauthenticated requests by sending a JSON response with an error message.
     * This method is called when an unauthenticated user attempts to access a protected resource.
     * The response includes:
     * - HTTP 401 Unauthorized status code
     * - Content-Type: application/json header
     * - JSON body containing either:
     *   - The specific authentication exception message if available
     *   - A default "Invalid or missing authentication token" message if no specific message is available
     *
     * @param request The HTTP request that triggered the authentication failure
     * @param response The HTTP response to be sent back to the client
     * @param authException The authentication exception that occurred, containing details about the failure
     * @throws IOException if an I/O error occurs while writing the response
     * @throws ServletException if a servlet error occurs during request processing
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        String errorMessage = messageSource.getMessage(
            "error.security.authentication.failed",
            null,
            LocaleContextHolder.getLocale()
        );
        if (authException != null) {
            String exceptionMessage = authException.getMessage();
            if (exceptionMessage != null && !exceptionMessage.isEmpty()) {
                try {
                    errorMessage = messageSource.getMessage(
                            exceptionMessage,
                            null,
                            LocaleContextHolder.getLocale()
                    );
                } catch (NoSuchMessageException ignored) {
                    errorMessage = messageSource.getMessage(
                            "error.security.authentication.token.invalid.or.missing",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                }
            } else {
                errorMessage = messageSource.getMessage(
                        "error.security.authentication.token.invalid.or.missing",
                        null,
                        LocaleContextHolder.getLocale()
                );
            }
        }
        
        OBJECT_MAPPER.writeValue(response.getOutputStream(), new ErrorDto(errorMessage));
    }
}
