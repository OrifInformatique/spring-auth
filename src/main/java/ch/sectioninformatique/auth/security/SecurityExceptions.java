package ch.sectioninformatique.auth.security;

import org.springframework.http.HttpStatus;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.app.exceptions.MessageKeyProvider;

/**
 * Security and authorization-related exceptions for the security package.
 */
public class SecurityExceptions {

    /**
     * Thrown when a role with the given name is not found.
     */
    public static class RoleNotFoundException extends AppException implements MessageKeyProvider {
        private final RoleEnum role;

        public RoleNotFoundException(RoleEnum role) {
            super(HttpStatus.NOT_FOUND);
            this.role = role;
        }

        @Override
        public String getMessageKey() {
            return "error.security.role.not.found";
        }

        @Override
        public Object[] getMessageArgs() {
            return new Object[] { role.name() };
        }
    }

    /**
     * Thrown when an Azure token is not from a trusted tenant.
     */
    public static class TokenNotFromTrustedTenantException extends AppException implements MessageKeyProvider {
        public TokenNotFromTrustedTenantException() {
            super(HttpStatus.FORBIDDEN);
        }

        @Override
        public String getMessageKey() {
            return "error.security.token.untrusted.tenant";
        }
    }

    /**
     * Thrown when a required claim is missing from the JWT token.
     */
    public static class MissingJwtClaimException extends AppException implements MessageKeyProvider {
        private final String claimName;

        public MissingJwtClaimException(String claimName) {
            super(HttpStatus.FORBIDDEN);
            this.claimName = claimName;
        }

        @Override
        public String getMessageKey() {
            return "error.security.jwt.missing.claim";
        }

        @Override
        public Object[] getMessageArgs() {
            return new Object[] { claimName };
        }
    }

    /**
     * Thrown when a user attempts an action they are not authorized to perform.
     */
    public static class UnauthorizedActionException extends AppException implements MessageKeyProvider {
        public UnauthorizedActionException() {
            super(HttpStatus.FORBIDDEN);
        }

        @Override
        public String getMessageKey() {
            return "error.security.access.denied";
        }
    }

    /**
     * Thrown when a user attempts an action they don't have permissions for due to insufficient rights.
     */
    public static class UserHasLowerRightsException extends AppException implements MessageKeyProvider {
        private final String login;

        public UserHasLowerRightsException(String login) {
            super(HttpStatus.FORBIDDEN);
            this.login = login;
        }

        public String getLogin() {
            return login;
        }

        @Override
        public String getMessageKey() {
            return "error.security.insufficient.rights";
        }

        @Override
        public Object[] getMessageArgs() {
            return new Object[] { login };
        }
    }

    /**
     * Thrown when the server's hash algorithm is unavailable.
     */
    public static class HashAlgorithmUnavailableException extends AppException implements MessageKeyProvider {
        public HashAlgorithmUnavailableException() {
            super(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Override
        public String getMessageKey() {
            return "error.security.hash.algorithm.unavailable";
        }
    }

    /**
     * Thrown when CORS configuration is invalid.
     */
    public static class CorsConfigurationException extends AppException implements MessageKeyProvider {
        private final String messageKey;
        private final Object[] args;

        public CorsConfigurationException(String messageKey, Object... args) {
            super(HttpStatus.INTERNAL_SERVER_ERROR);
            this.messageKey = messageKey;
            this.args = args == null ? MessageKeyProvider.NO_ARGS : args;
        }

        @Override
        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public Object[] getMessageArgs() {
            return args;
        }
    }
}
