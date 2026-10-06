package ch.sectioninformatique.auth.common.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes an {@link ErrorResponse} directly to the servlet response.
 *
 * Used by components running outside Spring MVC (security filter, entry point,
 * access denied handler), where {@code @ExceptionHandler} methods do not apply.
 * Those components run before Spring MVC has resolved the request locale, so the
 * locale is resolved here with the application's {@link LocaleResolver}.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    /**
     * @param request    current request, used to resolve the message language
     * @param response   response to write to
     * @param status     HTTP status to set
     * @param messageKey i18n key of the error message, resolved with the current locale
     * @throws IOException if the response body cannot be written
     */
    public void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String messageKey) throws IOException {
        String message = messageSource.getMessage(messageKey, null, localeResolver.resolveLocale(request));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status, message));
    }
}
