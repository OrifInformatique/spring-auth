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

/**
 * Centralized resolver for error messages based on exceptions and validation
 * errors.
 * This class maps specific exceptions to message keys and arguments, and
 * retrieves localized messages from the Message
 * source. It also provides methods to resolve messages for validation errors
 * and common Spring exceptions.
 * 
 * The main purpose of this class is to keep error message resolution logic in
 * one place, making it easier to maintain and
 * localize error messages across the application. It is used by the
 * GlobalExceptionHandler to generate user-friendly error
 * messages for API responses based on the exceptions thrown in the application.
 */
@Component
public class ErrorMessageResolver {

    // Predefined empty arguments array to avoid creating new arrays for exceptions
    // without parameters
    private static final Object[] NO_ARGS = new Object[0];

    // Internal class to hold message key and arguments for an exception
    private static final class MessageSpec {
        private final String key;
        private final Object[] args;

        // Private constructor to enforce the use of the spec() factory method
        private MessageSpec(String key, Object[] args) {
            this.key = key;
            this.args = args;
        }
    }

    // Factory method to create a MessageSpec instance
    private static MessageSpec spec(String key, Object... args) {
        return new MessageSpec(key, args == null ? NO_ARGS : args);
    }

    /**
     * Mapping of specific AppException subclasses to functions that generate
     * MessageSpec instances.
     * Each entry maps an exception class to a function that takes an AppException
     * instance and returns
     * a MessageSpec containing the message key and arguments for that exception.
     * This allows for dynamic
     * message generation based on the properties of the exception (e.g., user
     * login, role name).
     */
    private static final Map<Class<? extends AppException>, Function<AppException, MessageSpec>> APP_EXCEPTION_MAPPINGS = Map
            .ofEntries(
                    Map.entry(
                            UserExceptions.UserAlreadyExistsException.class,
                            ex -> spec("error.user.already.exists",
                                    ((UserExceptions.UserAlreadyExistsException) ex).getLogin())),
                    Map.entry(
                            UserExceptions.UserNotFoundException.class,
                            ex -> spec("error.user.not.found",
                                    ((UserExceptions.UserNotFoundException) ex).getLoginOrId())),
                    Map.entry(
                            UserExceptions.UserAlreadyAdminException.class,
                            ex -> spec("error.user.already.admin",
                                    ((UserExceptions.UserAlreadyAdminException) ex).getLogin())),
                    Map.entry(
                            UserExceptions.UserAlreadyManagerException.class,
                            ex -> spec("error.user.already.manager",
                                    ((UserExceptions.UserAlreadyManagerException) ex).getLogin())),
                    Map.entry(
                            UserExceptions.UserAlreadyRegularException.class,
                            ex -> spec("error.user.already.regular",
                                    ((UserExceptions.UserAlreadyRegularException) ex).getLogin())),
                    Map.entry(
                            SecurityExceptions.RoleNotFoundException.class,
                            ex -> spec("error.security.role.not.found",
                                    ((SecurityExceptions.RoleNotFoundException) ex).getRole().name())),
                    Map.entry(
                            SecurityExceptions.TokenNotFromTrustedTenantException.class,
                            ex -> spec("error.security.token.untrusted.tenant")),
                    Map.entry(
                            SecurityExceptions.MissingJwtClaimException.class,
                            ex -> spec("error.security.jwt.missing.claim",
                                    ((SecurityExceptions.MissingJwtClaimException) ex).getClaimName())),
                    Map.entry(
                            SecurityExceptions.UnauthorizedActionException.class,
                            ex -> spec("error.security.access.denied")),
                    Map.entry(
                            SecurityExceptions.UserHasLowerRightsException.class,
                            ex -> spec("error.security.insufficient.rights",
                                    ((SecurityExceptions.UserHasLowerRightsException) ex).getLogin())),
                    Map.entry(
                            AuthExceptions.InvalidCredentialsException.class,
                            ex -> spec("error.authorisation.invalid.credentials")),
                    Map.entry(
                            AuthExceptions.InvalidRefreshTokenException.class,
                            ex -> spec("error.security.refresh.token.invalid")));

    // The MessageSource is injected to allow retrieval of localized messages based
    // on keys and arguments.
    private final MessageSource messageSource;

    // Constructor for dependency injection of the MessageSource
    public ErrorMessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Helper method to retrieve a message from the MessageSource using a key and
    // arguments, based on the current locale.
    private String msg(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

    // Helper method to retrieve a message with a fallback in case the key is not
    // found or arguments are missing.
    private String msgWithFallback(String key, Object[] args, String fallback) {
        return messageSource.getMessage(key, args, fallback, LocaleContextHolder.getLocale());
    }

    /**
     * Resolve the message for a given AppException by looking it up in the
     * APP_EXCEPTION_MAPPINGS. If a mapping is found, it generates the message using
     * the specified key and arguments. If no mapping is found for the exception
     * type, it returns a generic unexpected error message.
     * 
     * @param ex the AppException instance for which to resolve the message
     * @return the resolved message string
     */
    public String resolveAppExceptionMessage(AppException ex) {

        // Look for a mapping for the specific exception type and generate the message
        // using the corresponding function
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

    /**
     * Resolve the message for a validation field error by using the default message
     * as the key and providing a fallback if the default message is not set. This
     * allows for dynamic resolution of validation error messages based on the field
     * and error type.
     * 
     * @param error the FieldError instance containing details about the validation
     *              error
     * @return the resolved message string for the validation error
     */
    public String resolveValidationFieldError(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        String messageKey = defaultMessage != null ? defaultMessage : "error.validation.failed";
        String fallback = defaultMessage != null
                ? defaultMessage
                : msg("error.validation.failed");

        return msgWithFallback(messageKey, null, fallback);
    }

    /**
     * Resolve a combined message for multiple validation field errors by
     * concatenating individual field error messages. This method takes a map of
     * field names to error messages and creates a single string that lists all
     * validation errors in a user-friendly format.
     * 
     * @param fieldErrors a map where the key is the field name and the value is the
     *                    error message for that field
     * @return a combined message string that includes all validation errors
     */
    public String resolveValidationCombinedMessage(Map<String, String> fieldErrors) {
        return fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((e1, e2) -> e1 + "; " + e2)
                .orElse(msg("error.validation.failed"));
    }

    /**
     * Resolve the title message for validation errors, which can be used as a
     * general heading for validation error responses. This allows for a consistent
     * title to be displayed in API responses when validation fails.
     * 
     * @return the resolved title message string for validation errors
     */
    public String resolveValidationTitle() {
        return msg("error.validation.failed.title");
    }

    /**
     * Resolve the message for an unsupported media type error by using the content
     * type and supported media types as arguments. This provides a user-friendly
     * message indicating which media type was not supported and what media types
     * are acceptable.
     * 
     * @param ex the HttpMediaTypeNotSupportedException instance containing details
     *           about the unsupported media type error
     * @return the resolved message string for the unsupported media type error
     */
    public String resolveUnsupportedMediaTypeMessage(HttpMediaTypeNotSupportedException ex) {
        return msgWithFallback(
                "error.media.type.unsupported",
                new Object[] { ex.getContentType(), ex.getSupportedMediaTypes() },
                ex.getMessage());
    }

    /**
     * Resolve the message for a missing request parameter error by using the
     * parameter name as an argument. This provides a clear message indicating which
     * required parameter is missing from the request.
     * 
     * @param ex the MissingServletRequestParameterException instance containing
     *           details about the missing parameter error
     * @return the resolved message string for the missing request parameter error
     */
    public String resolveMissingParamMessage(MissingServletRequestParameterException ex) {
        return msg("error.request.parameter.missing", ex.getParameterName());
    }

    /**
     * Resolve the message for a malformed JSON error by analyzing the cause of the
     * HttpMessageNotReadableException. This method checks the cause message for specific patterns to determine the most appropriate error message to return, providing more specific feedback about what is wrong with
     * the JSON input (e.g., incomplete JSON, invalid characters, type mismatches).
     * 
     * @param ex the HttpMessageNotReadableException instance containing details about the malformed JSON error
     * @return the resolved message string for the malformed JSON error
     */
    public String resolveMalformedJsonMessage(HttpMessageNotReadableException ex) {
        String message = msg("error.request.json.malformed.or.missing");

        // Analyze the cause message to provide more specific feedback about the JSON error
        Throwable cause = ex.getCause();
        if (cause == null) {
            return message;
        }

        // Check for specific patterns in the cause message to determine the most appropriate error message
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
