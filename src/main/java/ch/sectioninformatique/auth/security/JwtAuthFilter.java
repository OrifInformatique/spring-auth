package ch.sectioninformatique.auth.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter for processing JWT tokens in incoming requests.
 * This filter:
 * - Extends OncePerRequestFilter to ensure it's only executed once per request
 * - Intercepts all incoming HTTP requests
 * - Validates JWT tokens in the Authorization header
 * - Sets up Spring Security context with authenticated user information
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * Provider for user authentication and token validation.
     * This field:
     * - Is automatically injected via constructor
     * - Handles token validation logic
     * - Manages user authentication state
     */
    private final UserAuthenticationProvider userAuthenticationProvider;

    /**
     * Used for writing JSON error responses when token verification fails.
     */
    private final ObjectMapper mapper;

    /**
     * Message source for localized error messages.
     */
    private final MessageSource messageSource;

    /**
     * Processes each incoming request to validate JWT tokens.
     * This method:
     * - Extracts the Authorization header from the request
     * - Validates the JWT token if present
     * - Sets up the security context with authenticated user information
     * - Clears security context if validation fails
     *
     * @param request     The incoming HTTP request
     * @param response    The HTTP response
     * @param filterChain The filter chain for request processing
     * @throws ServletException If there's a servlet-related error
     * @throws IOException      If there's an I/O error
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Retrieve Authorization header ("Authorization: Bearer <token>")
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // If the Authorization header is missing OR doesn't start with "Bearer ",
        // simply continue the filter chain without authentication.
        if (header == null || !header.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the JWT token (everything after "Bearer ")
        String token = header.substring(7).trim();

        log.debug("Token reçus : {}", token);

        try {
            // Validate the token and set Authentication object in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(
                    userAuthenticationProvider.validateToken(token));
        } catch (JWTVerificationException e) {
            // Specific exception thrown when JWT is invalid or expired.

            // Clear previous authentication just in case
            SecurityContextHolder.clearContext();

            // Return a 401 Unauthorized with a JSON error message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            String message = switch (e.getClass().getSimpleName()) {
                case "TokenExpiredException" -> messageSource.getMessage(
                    "error.security.token.expired",
                    null,
                    LocaleContextHolder.getLocale()
                );
                case "InvalidClaimException" -> messageSource.getMessage(
                    "error.security.token.invalid.claims",
                    null,
                    LocaleContextHolder.getLocale()
                );
                case "SignatureVerificationException" -> messageSource.getMessage(
                    "error.security.token.invalid.signature",
                    null,
                    LocaleContextHolder.getLocale()
                );
                default -> messageSource.getMessage(
                    "error.security.token.invalid",
                    null,
                    LocaleContextHolder.getLocale()
                );
            };

            log.debug("JWT validation failed: {}", message, e);
            Map<String, String> errorBody = Map.of(
                    "message", message,
                    "error", "INVALID_TOKEN");

            var writer = response.getWriter();
            mapper.writeValue(writer, errorBody);
            writer.flush();
            return;

        } catch (RuntimeException e) {
            // Catch any other unexpected error during token validation

            SecurityContextHolder.clearContext();
            log.error("Unexpected error during JWT validation", e);

            // Re-throw to let Spring handle global exception handling
            throw e;
        }

        // If authentication succeeded or token not required,
        // continue normal request processing.
        filterChain.doFilter(request, response);
    }
}
