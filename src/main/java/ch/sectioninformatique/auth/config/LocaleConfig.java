package ch.sectioninformatique.auth.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Internationalization and localization configuration.
 *
 * This configuration provides a message source with automatic bundle discovery
 * under messages, a default French locale with locale switching through the
 * lang request parameter, and validation message resolution through the same
 * message source.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

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
     * Defines the default locale used for message resolution.
     *
     * Clients can override it per request with the lang query parameter,
     * for example /api/some-endpoint?lang=en.
     *
     * @return locale resolver configured with French as default locale
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.FRANCE);
        return localeResolver;
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
     * Creates an interceptor that switches locale based on the lang request
     * parameter.
     *
     * @return locale change interceptor using the lang parameter
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    /**
     * Registers MVC interceptors related to localization.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
