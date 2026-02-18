package ch.sectioninformatique.auth.web;

import java.util.Map;
import java.util.function.Function;

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
public class ErrorMessageResolver {

    private static final Object[] NO_ARGS = new Object[0];

    private static final class MessageSpec {
        private final String key;
        private final Object[] args;

        private MessageSpec(String key, Object[] args) {
            this.key = key;
            this.args = args;
        }
    }

    private static MessageSpec spec(String key, Object... args) {
        return new MessageSpec(key, args == null ? NO_ARGS : args);
    }

    private static final Map<Class<? extends AppException>, Function<AppException, MessageSpec>> APP_EXCEPTION_MAPPINGS =
        Map.ofEntries(
            Map.entry(
                UserExceptions.UserAlreadyExistsException.class,
                ex -> spec("error.user.already.exists", ((UserExceptions.UserAlreadyExistsException) ex).getLogin())
            ),
            Map.entry(
                UserExceptions.UserNotFoundException.class,
                ex -> spec("error.user.not.found", ((UserExceptions.UserNotFoundException) ex).getLoginOrId())
            ),
            Map.entry(
                UserExceptions.UserAlreadyAdminException.class,
                ex -> spec("error.user.already.admin", ((UserExceptions.UserAlreadyAdminException) ex).getLogin())
            ),
            Map.entry(
                UserExceptions.UserAlreadyManagerException.class,
                ex -> spec("error.user.already.manager", ((UserExceptions.UserAlreadyManagerException) ex).getLogin())
            ),
            Map.entry(
                UserExceptions.UserAlreadyRegularException.class,
                ex -> spec("error.user.already.regular", ((UserExceptions.UserAlreadyRegularException) ex).getLogin())
            ),
            Map.entry(
                SecurityExceptions.RoleNotFoundException.class,
                ex -> spec("error.security.role.not.found", ((SecurityExceptions.RoleNotFoundException) ex).getRole().name())
            ),
            Map.entry(
                SecurityExceptions.TokenNotFromTrustedTenantException.class,
                ex -> spec("error.security.token.untrusted.tenant")
            ),
            Map.entry(
                SecurityExceptions.MissingJwtClaimException.class,
                ex -> spec("error.security.jwt.missing.claim", ((SecurityExceptions.MissingJwtClaimException) ex).getClaimName())
            ),
            Map.entry(
                SecurityExceptions.UnauthorizedActionException.class,
                ex -> spec("error.security.access.denied")
            ),
            Map.entry(
                SecurityExceptions.UserHasLowerRightsException.class,
                ex -> spec("error.security.insufficient.rights", ((SecurityExceptions.UserHasLowerRightsException) ex).getLogin())
            ),
            Map.entry(
                AuthExceptions.InvalidCredentialsException.class,
                ex -> spec("error.authorisation.invalid.credentials")
            ),
            Map.entry(
                AuthExceptions.InvalidRefreshTokenException.class,
                ex -> spec("error.security.refresh.token.invalid")
            )
        );

    private final MessageSource messageSource;

    public ErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    private String msgWithFallback(String key, Object[] args, String fallback) {
        return messageSource.getMessage(key, args, fallback, LocaleContextHolder.getLocale());
    }

    public String resolveAppExceptionMessage(AppException ex) {
        MessageSpec spec = APP_EXCEPTION_MAPPINGS.entrySet().stream()
            .filter(entry -> entry.getKey().isInstance(ex))
            .map(entry -> entry.getValue().apply(ex))
            .findFirst()
            .orElse(null);

        if (spec == null) {
            return msg("error.unexpected");
        }

        return msg(spec.key, spec.args);
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
