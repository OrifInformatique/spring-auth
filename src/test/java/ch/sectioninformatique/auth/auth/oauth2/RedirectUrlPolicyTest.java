package ch.sectioninformatique.auth.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.sectioninformatique.auth.security.CorsProperties;

/**
 * Unit tests of the open-redirect protection of the OAuth2 flow.
 */
class RedirectUrlPolicyTest {

    private final RedirectUrlPolicy policy = new RedirectUrlPolicy(new CorsProperties(
            List.of("http://localhost:3000", "https://app.example.com"), List.of("GET"), List.of("Accept")));

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:3000",
            "http://localhost:3000/callback?state=1",
            "https://app.example.com/login",
            "HTTPS://APP.EXAMPLE.COM/login",
            "/auth/redirect-after-login" })
    void allowedOriginsAndLocalPaths_areAccepted(String url) {
        assertThat(policy.isAllowed(url)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "https://attacker.example",
            "http://localhost:3001/callback",
            "https://app.example.com.attacker.example/",
            "https://app.example.com@attacker.example/",
            "//attacker.example/path",
            "/\\attacker.example",
            "javascript:alert(1)",
            "ftp://app.example.com",
            "not a url" })
    void foreignOrDangerousUrls_areRejected(String url) {
        assertThat(policy.isAllowed(url)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://anything.example", "http://localhost:9999" })
    void wildcardOrigin_acceptsAnyHttpUrl(String url) {
        RedirectUrlPolicy permissive = new RedirectUrlPolicy(
                new CorsProperties(List.of("*"), List.of("GET"), List.of("Accept")));

        assertThat(permissive.isAllowed(url)).isTrue();
    }
}
