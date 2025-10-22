package ch.sectioninformatique.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.io.IOException;

/**
 * JWT Authentication Filter for processing JWT tokens in incoming requests.
 * This filter:
 * - Extends OncePerRequestFilter to ensure it's only executed once per request
 * - Intercepts all incoming HTTP requests
 * - Validates JWT tokens in the Authorization header
 * - Sets up Spring Security context with authenticated user information
 * 
 * The filter implements different validation strategies based on the HTTP method:
 * - GET requests use standard token validation
 * - Other methods use strong token validation
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
     * Processes each incoming request to validate JWT tokens.
     * This method:
     * - Extracts the Authorization header from the request
     * - Validates the JWT token if present
     * - Sets up the security context with authenticated user information
     * - Applies different validation strategies based on HTTP method
     * - Clears security context if validation fails
     *
     * @param request The incoming HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain for request processing
     * @throws ServletException If there's a servlet-related error
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
                
        if (request == null || response == null || filterChain == null) {
            throw new IllegalArgumentException("Request, response and filterChain cannot be null");
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null) {
            String[] authElements = header.split(" ");

            if (authElements.length == 2
                    && "Bearer".equals(authElements[0])) {
                try {
                    if ("GET".equals(request.getMethod())) {
                        SecurityContextHolder.getContext().setAuthentication(
                                userAuthenticationProvider.validateToken(authElements[1]));
                    } else {
                        SecurityContextHolder.getContext().setAuthentication(
                                userAuthenticationProvider.validateTokenStrongly(authElements[1]));
                    }
                } catch (JWTVerificationException e) {
                    SecurityContextHolder.clearContext();
                    log.debug("Invalid JWT token: {}", e.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"message\":\"Invalid token\"}");
                    response.getWriter().flush();
                    return;
                } catch (RuntimeException e) {
                    // Preserve behavior for other runtime exceptions
                    SecurityContextHolder.clearContext();
                    throw e;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

