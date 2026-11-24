package ch.sectioninformatique.auth.auth.token;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import ch.sectioninformatique.auth.security.JwtService;

/**
 * Builds the cookie carrying the refresh token.
 *
 * The cookie is HTTP-only (not readable by JavaScript, which mitigates XSS), Secure
 * (HTTPS only) and scoped to {@code /auth/refresh}, the only endpoint that reads it.
 */
@Component
public class RefreshTokenCookieFactory {

    /** Name of the cookie holding the refresh token. */
    public static final String COOKIE_NAME = "refresh_token";

    private static final String COOKIE_PATH = "/auth/refresh";

    private final Duration lifetime;
    private final String sameSite;

    /**
     * @param jwtService provides the refresh token lifetime, used as cookie max age
     * @param sameSite   SameSite attribute: "Strict" when the client application is served
     *                   from the same site as the API, "None" for a cross-site client
     */
    public RefreshTokenCookieFactory(JwtService jwtService,
            @Value("${SECURITY_REFRESH_COOKIE_SAME_SITE:Strict}") String sameSite) {
        this.lifetime = jwtService.getRefreshTokenLifetime();
        this.sameSite = sameSite;
    }

    /**
     * @param refreshToken raw refresh token
     * @return the cookie to send in a Set-Cookie header
     */
    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, lifetime);
    }

    /**
     * @return an empty, already expired cookie that makes the browser delete the refresh token
     */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }
}
