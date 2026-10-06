package ch.sectioninformatique.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;

import ch.sectioninformatique.auth.common.web.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticates requests carrying an "Authorization: Bearer &lt;access token&gt;" header.
 *
 * - No bearer header: the request continues unauthenticated; endpoints that require
 *   authentication are then rejected by {@link UserAuthenticationEntryPoint}.
 * - Valid token: the security context receives the authenticated user.
 * - Invalid token: the request is stopped with a 401 response explaining why
 *   (expired, bad signature, invalid claims), so that clients know when to refresh.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "bearer ";

    private final JwtService jwtService;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.toLowerCase().startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            SecurityContextHolder.getContext().setAuthentication(jwtService.validateAccessToken(token));
        } catch (JWTVerificationException e) {
            SecurityContextHolder.clearContext();
            log.debug("JWT validation failed: {}", e.getMessage());
            errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, messageKeyFor(e));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String messageKeyFor(JWTVerificationException e) {
        if (e instanceof TokenExpiredException) {
            return "error.security.token.expired";
        }
        if (e instanceof SignatureVerificationException) {
            return "error.security.token.invalid.signature";
        }
        if (e instanceof InvalidClaimException) {
            return "error.security.token.invalid.claims";
        }
        return "error.security.token.invalid";
    }
}
