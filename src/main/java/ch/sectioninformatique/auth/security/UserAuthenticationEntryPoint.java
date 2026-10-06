package ch.sectioninformatique.auth.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import ch.sectioninformatique.auth.common.web.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Answers 401 Unauthorized, with the standard JSON error body, when an
 * unauthenticated request reaches an endpoint that requires authentication.
 */
@Component
@RequiredArgsConstructor
public class UserAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "error.security.authentication.token.invalid.or.missing");
    }
}
