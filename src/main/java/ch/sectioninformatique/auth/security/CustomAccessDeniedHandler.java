package ch.sectioninformatique.auth.security;

import java.io.IOException;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import ch.sectioninformatique.auth.app.errors.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Handles requests that are authenticated but not authorized (403 Forbidden).
 * Returns a JSON response with the error.security.access.denied message.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final MessageSource messageSource;

    public CustomAccessDeniedHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Content-Type", "application/json");

        String errorMessage = messageSource.getMessage(
                "error.security.access.denied",
                null,
                LocaleContextHolder.getLocale()
        );
        if (accessDeniedException != null && accessDeniedException.getMessage() != null) {
            String exceptionMessage = accessDeniedException.getMessage();
            if (!exceptionMessage.isEmpty()) {
                try {
                    errorMessage = messageSource.getMessage(
                            exceptionMessage,
                            null,
                            LocaleContextHolder.getLocale()
                    );
                } catch (NoSuchMessageException ignored) {
                    errorMessage = messageSource.getMessage(
                            "error.security.access.denied",
                            null,
                            LocaleContextHolder.getLocale()
                    );
                }
            }
        }

        ErrorDto errorDto = new ErrorDto(errorMessage);
        OBJECT_MAPPER.writeValue(response.getOutputStream(), errorDto);
    }
}