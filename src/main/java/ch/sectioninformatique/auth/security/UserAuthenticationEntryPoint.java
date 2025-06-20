package ch.sectioninformatique.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.app.errors.ErrorDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

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
        
        String errorMessage = "Authentication failed";
        if (authException != null) {
            errorMessage = authException.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Invalid or missing authentication token";
            }
        }
        
        OBJECT_MAPPER.writeValue(response.getOutputStream(), new ErrorDto(errorMessage));
    }
}
