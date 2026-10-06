package ch.sectioninformatique.auth.auth.oauth2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ch.sectioninformatique.auth.security.CorsProperties;
import lombok.RequiredArgsConstructor;

/**
 * Decides whether a URL may receive the authentication code at the end of the OAuth2 flow.
 *
 * Without this check, a crafted link such as
 * {@code /oauth2/login/azure?redirectUrl=https://attacker.example} would send a victim's
 * authentication code to a third-party site (open redirect), which could then exchange
 * it for tokens. Only relative paths of this server and the configured CORS origins
 * (the known client applications) are accepted.
 */
@Component
@RequiredArgsConstructor
public class RedirectUrlPolicy {

    private final CorsProperties corsProperties;

    /**
     * @param url candidate redirect URL (query parameter or Referer header)
     * @return true if the URL is a local path or belongs to an allowed origin
     */
    public boolean isAllowed(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        // Local path, but not a protocol-relative URL ("//host") or a backslash trick ("/\host")
        if (url.startsWith("/")) {
            return !url.startsWith("//") && !url.contains("\\");
        }
        return isAllowedOrigin(url);
    }

    private boolean isAllowedOrigin(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }

        String origin = (scheme + "://" + uri.getHost() + (uri.getPort() != -1 ? ":" + uri.getPort() : ""))
                .toLowerCase(Locale.ROOT);
        List<String> allowedOrigins = corsProperties.allowedOrigins();
        // The "*" origin is only accepted by CorsConfigurationValidator in dev and test profiles
        return allowedOrigins.contains("*") || allowedOrigins.stream()
                .map(allowed -> allowed.trim().toLowerCase(Locale.ROOT))
                .anyMatch(origin::equals);
    }
}
