package ch.sectioninformatique.auth.web;

import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.auth.AuthExceptions;
import ch.sectioninformatique.auth.security.SecurityExceptions;
import ch.sectioninformatique.auth.user.UserExceptions;

@Component
public class ErrorMessageService {

    private final MessageSource messageSource;

    public ErrorMessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private String msgWithFallback(String key, Object[] args, String fallback) {
        return messageSource.getMessage(key, args, fallback, LocaleContextHolder.getLocale());
    }

    public String resolveAppExceptionMessage(AppException ex) {
        if (ex instanceof UserExceptions.UserAlreadyExistsException typed) {
            return msg("error.user.already.exists", typed.getLogin());
        }

        if (ex instanceof UserExceptions.UserNotFoundException typed) {
            return msg("error.user.not.found", typed.getLoginOrId());
        }

        if (ex instanceof UserExceptions.UserAlreadyAdminException typed) {
            return msg("error.user.already.admin", typed.getLogin());
        }

        if (ex instanceof UserExceptions.UserAlreadyManagerException typed) {
            return msg("error.user.already.manager", typed.getLogin());
        }

        if (ex instanceof UserExceptions.UserAlreadyRegularException typed) {
            return msg("error.user.already.regular", typed.getLogin());
        }

        if (ex instanceof SecurityExceptions.RoleNotFoundException typed) {
            return msg("error.security.role.not.found", typed.getRole().name());
        }

        if (ex instanceof SecurityExceptions.TokenNotFromTrustedTenantException) {
            return msg("error.security.token.untrusted.tenant");
        }

        if (ex instanceof SecurityExceptions.MissingJwtClaimException typed) {
            return msg("error.security.jwt.missing.claim", typed.getClaimName());
        }

        if (ex instanceof SecurityExceptions.UnauthorizedActionException) {
            return msg("error.security.access.denied");
        }

        if (ex instanceof SecurityExceptions.UserHasLowerRightsException typed) {
            return msg("error.security.insufficient.rights", typed.getLogin());
        }

        if (ex instanceof AuthExceptions.InvalidCredentialsException) {
            return msg("error.authorisation.invalid.credentials");
        }

        if (ex instanceof AuthExceptions.InvalidRefreshTokenException) {
            return msg("error.security.refresh.token.invalid");
        }

        return msg("error.unexpected");
    }

    public String resolveValidationFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        String messageKey = defaultMessage != null ? defaultMessage : "error.validation.failed";
        String fallback = defaultMessage != null
            ? defaultMessage
            : msg("error.validation.failed");

        return msgWithFallback(messageKey, null, fallback);
    }

    public String resolveValidationCombinedMessage(Map<String, String> fieldErrors) {
        return fieldErrors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .reduce((e1, e2) -> e1 + "; " + e2)
            .orElse(msg("error.validation.failed"));
    }

    public String resolveValidationTitle() {
        return msg("error.validation.failed.title");
    }

    public String resolveUnsupportedMediaTypeMessage(HttpMediaTypeNotSupportedException ex) {
        return msgWithFallback(
            "error.media.type.unsupported",
            new Object[] {ex.getContentType(), ex.getSupportedMediaTypes()},
            ex.getMessage()
        );
    }

    public String resolveMissingParamMessage(MissingServletRequestParameterException ex) {
        return msg("error.request.parameter.missing", ex.getParameterName());
    }

    public String resolveMalformedJsonMessage(HttpMessageNotReadableException ex) {
        String message = msg("error.request.json.malformed.or.missing");

        Throwable cause = ex.getCause();
        if (cause == null) {
            return message;
        }

        String causeMessage = cause.getMessage();
        if (causeMessage == null) {
            return message;
        }

        if (causeMessage.contains("Unexpected end-of-input")) {
            return msg("error.request.json.incomplete");
        }

        if (causeMessage.contains("Unexpected character")) {
            return msg("error.request.json.invalid.character");
        }

        if (causeMessage.contains("cannot deserialize")) {
            return msg("error.request.json.invalid.value.type");
        }

        if (causeMessage.contains("No content to map")) {
            return msg("error.request.json.empty.or.missing");
        }

        return message;
    }
}
