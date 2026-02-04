package ch.sectioninformatique.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validates CORS configuration at application startup.
 * 
 * This validator ensures that CORS settings loaded from properties are secure:
 * - No wildcard origins in production (allowed only in dev/test)
 * - All origins are valid URLs with proper protocols (http/https)
 * - Only expected HTTP methods are configured
 * - Only necessary headers are allowed
 * - Environment-specific restrictions are enforced
 * 
 * Throws IllegalArgumentException if configuration is invalid.
 */
@Configuration
@Slf4j
public class CorsConfigurationValidator {

    private final Environment environment;
    private final MessageSource messageSource;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders;

    /**
     * List of allowed HTTP methods for CORS
     */
    private static final Set<String> ALLOWED_HTTP_METHODS = new HashSet<>(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    ));

    /**
     * List of allowed headers for CORS (whitelist of common, safe headers)
     */
    private static final Set<String> ALLOWED_HEADER_NAMES = new HashSet<>(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Accept-Language",
            "Cache-Control",
            "If-Match",
            "If-Modified-Since",
            "If-None-Match",
            "If-Unmodified-Since",
            "X-Requested-With",
            "X-CSRF-Token",
            "X-API-Key"
    ));

    public CorsConfigurationValidator(Environment environment, MessageSource messageSource) {
        this.environment = environment;
        this.messageSource = messageSource;
    }

    /**
     * Validates CORS configuration at application startup.
     * Called automatically by Spring after bean construction.
     * 
     * @throws IllegalArgumentException if any CORS configuration is invalid
     */
    @PostConstruct
    public void validateCorsConfiguration() {
        log.info("Validating CORS configuration...");

        boolean isDevOrTest = isDevelopmentOrTest();

        try {
            validateOrigins(isDevOrTest);
            validateMethods();
            validateHeaders();
            log.info("✓ CORS configuration is valid and secure");
        } catch (IllegalArgumentException e) {
            log.error("✗ CORS configuration validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Validates origin URLs for security and format.
     * 
     * Rules:
     * - Origins must not be empty
     * - Wildcard "*" is only allowed in dev/test environments
     * - Each origin must be a valid URL with http or https protocol
     * - Origins must not contain duplicate entries
     * 
     * @param isDevOrTest true if running in dev or test profile
     * @throws IllegalArgumentException if origins are invalid
     */
    private void validateOrigins(boolean isDevOrTest) {
        if (allowedOrigins == null || allowedOrigins.length == 0) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "error.cors.allowed.origins.empty",
                    null,
                    LocaleContextHolder.getLocale()
                )
            );
        }

        Set<String> uniqueOrigins = new HashSet<>();

        for (String origin : allowedOrigins) {
            origin = origin.trim();

            // Check for wildcard
            if ("*".equals(origin)) {
                if (!isDevOrTest) {
                    throw new IllegalArgumentException(
                        messageSource.getMessage(
                            "error.cors.origin.wildcard.production",
                            null,
                            LocaleContextHolder.getLocale()
                        )
                    );
                }
                log.warn("⚠ Wildcard CORS origin '*' configured (allowed only in dev/test)");
                uniqueOrigins.add(origin);
                continue;
            }

            // Validate URL format
            try {
                URI uri = new URI(origin);
                String protocol = uri.getScheme();

                // Check protocol is http or https
                if (!("http".equals(protocol) || "https".equals(protocol))) {
                    throw new IllegalArgumentException(
                        messageSource.getMessage(
                            "error.cors.origin.invalid.protocol",
                            new Object[] {origin, protocol},
                            LocaleContextHolder.getLocale()
                        )
                    );
                }

                // Check for localhost in production
                String host = uri.getHost();
                if (!isDevOrTest && ("localhost".equals(host) || "127.0.0.1".equals(host))) {
                    throw new IllegalArgumentException(
                        messageSource.getMessage(
                            "error.cors.origin.localhost.production",
                            new Object[] {origin},
                            LocaleContextHolder.getLocale()
                        )
                    );
                }

            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.origin.invalid.url",
                        new Object[] {origin, e.getMessage()},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            // Check for duplicates
            if (!uniqueOrigins.add(origin)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.origin.duplicate",
                        new Object[] {origin},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            log.debug("✓ Origin validated: {}", origin);
        }
    }

    /**
     * Validates HTTP methods for security.
     * 
     * Rules:
     * - Methods cannot be empty
     * - Only standard HTTP methods are allowed (GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD)
     * - No dangerous methods like TRACE, CONNECT
     * - No duplicate methods
     * 
     * @throws IllegalArgumentException if methods are invalid
     */
    private void validateMethods() {
        if (allowedMethods == null || allowedMethods.length == 0) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "error.cors.allowed.methods.empty",
                    null,
                    LocaleContextHolder.getLocale()
                )
            );
        }

        Set<String> uniqueMethods = new HashSet<>();

        for (String method : allowedMethods) {
            method = method.trim().toUpperCase();

            // Check if method is in whitelist
            if (!ALLOWED_HTTP_METHODS.contains(method)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.method.not.allowed",
                        new Object[] {method, ALLOWED_HTTP_METHODS},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            // Check for duplicates
            if (!uniqueMethods.add(method)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.method.duplicate",
                        new Object[] {method},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            log.debug("✓ Method validated: {}", method);
        }
    }

    /**
     * Validates headers for security.
     * 
     * Rules:
     * - Headers cannot be empty
     * - Only whitelisted header names are allowed
     * - No wildcard "*" for headers (too permissive)
     * - No duplicate headers
     * 
     * Note: Custom application headers (e.g., X-API-Key) can be added to the whitelist
     * 
     * @throws IllegalArgumentException if headers are invalid
     */
    private void validateHeaders() {
        if (allowedHeaders == null || allowedHeaders.length == 0) {
            throw new IllegalArgumentException(
                messageSource.getMessage(
                    "error.cors.allowed.headers.empty",
                    null,
                    LocaleContextHolder.getLocale()
                )
            );
        }

        Set<String> uniqueHeaders = new HashSet<>();

        for (String header : allowedHeaders) {
            header = header.trim();

            // Check for wildcard
            if ("*".equals(header)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.header.wildcard.not.allowed",
                        new Object[] {ALLOWED_HEADER_NAMES},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            // Check if header is in whitelist
            if (!ALLOWED_HEADER_NAMES.contains(header)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.header.not.allowed",
                        new Object[] {header, ALLOWED_HEADER_NAMES},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            // Check for duplicates
            if (!uniqueHeaders.add(header)) {
                throw new IllegalArgumentException(
                    messageSource.getMessage(
                        "error.cors.header.duplicate",
                        new Object[] {header},
                        LocaleContextHolder.getLocale()
                    )
                );
            }

            log.debug("Header validated: {}", header);
        }
    }

    /**
     * Checks if the application is running in development or test mode.
     * Validation is more permissive in these modes.
     *
     * @return true if running in dev or test profile, false if in production
     */
    private boolean isDevelopmentOrTest() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("dev") || profile.equalsIgnoreCase("test")) {
                return true;
            }
        }
        return false;
    }
}
