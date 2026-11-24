package ch.sectioninformatique.auth.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Internationalization and localization configuration.
 *
 * This configuration provides a message source with automatic bundle discovery
 * under messages, a stateless locale resolution (lang request parameter, then
 * Accept-Language header, then French by default), and validation message
 * resolution through the same message source.
 */
@Configuration
public class LocaleConfig {

    /** Locales for which message bundles exist. Any other requested locale falls back to the default. */
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.FRENCH, Locale.ENGLISH);
    private static final Locale DEFAULT_LOCALE = Locale.FRANCE;
    private static final String LANG_PARAMETER = "lang";

    // Constants for message resource discovery and basename resolution
    private static final String MESSAGE_RESOURCES_PATTERN = "classpath*:messages/**/*.properties";
    private static final String MESSAGE_SEGMENT = "/messages/";
    private static final String PROPERTIES_EXTENSION = ".properties";
    private static final String CLASSPATH_MESSAGES_PREFIX = "classpath:messages/";
    private static final String DEFAULT_MESSAGE_BASENAME = "classpath:messages/messages";

    // Pattern to identify and remove locale suffixes from message basenames (e.g., _en, _en_US)
    private static final Pattern LOCALE_SUFFIX_PATTERN = Pattern.compile("_[a-z]{2}(?:_[A-Z]{2})?$");

    /**
     * Configures the application message source used for internationalization.
     *
     * Message files matching MESSAGE_RESOURCES_PATTERN are discovered
     * automatically and converted to Spring basenames.
     *
     * @return configured message source
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(resolveMessageBasenames());
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        // Bundles only exist for supported locales: never depend on the server's system locale
        messageSource.setFallbackToSystemLocale(false);
        // Apply MessageFormat to every message, so that the escaped apostrophes ('')
        // of the bundles are rendered the same way with or without arguments
        messageSource.setAlwaysUseMessageFormat(true);
        return messageSource;
    }

    /**
     * Resolves all message basenames by scanning message property files from the
     * classpath.
     *
     * @return discovered message basenames
     */
    private String[] resolveMessageBasenames() {
        try {
            // Scan for all message property files matching the defined pattern
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(MESSAGE_RESOURCES_PATTERN);
            Set<String> basenames = new TreeSet<>();

            // Convert each resource to a Spring message basename by extracting the path relative to the messages segment and removing locale suffixes and file extension
            for (Resource resource : resources) {
                String basename = resolveResourceBasename(resource);
                if (basename != null) {
                    basenames.add(basename);
                }
            }

            // Ensure at least the default basename is included if no resources were found
            if (basenames.isEmpty()) {
                basenames.add(DEFAULT_MESSAGE_BASENAME);
            }

            return basenames.toArray(new String[0]);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to resolve i18n message bundles from classpath", exception);
        }
    }

    /**
     * Resolves a message basename from a given resource by extracting the path
     * relative to the messages segment and removing locale suffixes and file extension.
     * 
     * @param resource message property file resource
     * @return message basename corresponding to the resource, or null if the resource does not match expected patterns
     * @throws IOException if the resource URL cannot be accessed
     */
    private String resolveResourceBasename(Resource resource) throws IOException {

        // Get the resource URL and convert it to a consistent format for processing
        String resourceUrl = resource.getURL().toString().replace('\\', '/');
        int messagesIndex = resourceUrl.lastIndexOf(MESSAGE_SEGMENT);
        if (messagesIndex < 0) {
            return null;
        }

        // Extract the path relative to the messages segment and remove locale suffixes and file extension to get the Spring message basename
        String relativePath = resourceUrl.substring(messagesIndex + MESSAGE_SEGMENT.length());
        if (!relativePath.endsWith(PROPERTIES_EXTENSION)) {
            return null;
        }

        // Remove the .properties extension
        String withoutExtension = relativePath.substring(0, relativePath.length() - PROPERTIES_EXTENSION.length());
        String basenameWithoutLocale = LOCALE_SUFFIX_PATTERN.matcher(withoutExtension).replaceFirst("");
        return CLASSPATH_MESSAGES_PREFIX + basenameWithoutLocale;
    }

    /**
     * Resolves the locale of each request without using the HTTP session, as the API is stateless.
     *
     * Resolution order: the lang query parameter (e.g. /users/me?lang=en), then the
     * Accept-Language header, then French. Only supported locales are accepted.
     *
     * @return the request locale resolver
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new LangParameterLocaleResolver();
    }

    /**
     * Configures Bean Validation to use the same internationalized message source.
     *
     * @param messageSource application message source
     * @return validator factory bean configured with the application message source
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }

    /**
     * Accept-Language resolver that lets the lang query parameter take precedence.
     */
    static class LangParameterLocaleResolver extends AcceptHeaderLocaleResolver {

        LangParameterLocaleResolver() {
            setSupportedLocales(SUPPORTED_LOCALES);
            setDefaultLocale(DEFAULT_LOCALE);
        }

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            String lang = request.getParameter(LANG_PARAMETER);
            if (StringUtils.hasText(lang)) {
                Locale requested = StringUtils.parseLocale(lang);
                if (requested != null && SUPPORTED_LOCALES.stream()
                        .anyMatch(supported -> supported.getLanguage().equals(requested.getLanguage()))) {
                    return requested;
                }
            }
            return super.resolveLocale(request);
        }
    }
}
