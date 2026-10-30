package ch.sectioninformatique.auth.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS settings bound from the {@code cors.*} properties (comma-separated lists).
 *
 * The allowed origins are also the only external origins the OAuth2 flow may
 * redirect to after login.
 *
 * @param allowedOrigins origins allowed to call the API with credentials
 * @param allowedMethods HTTP methods allowed for cross-origin requests
 * @param allowedHeaders request headers allowed for cross-origin requests
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders) {
}
