package ch.sectioninformatique.auth.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import ch.sectioninformatique.auth.security.SecurityExceptions.CorsConfigurationException;

/**
 * Unit tests of the startup validation of the CORS settings.
 */
class CorsConfigurationValidatorTest {

    private static final List<String> METHODS = List.of("GET", "POST");
    private static final List<String> HEADERS = List.of("Authorization", "Content-Type");

    private static void validate(String profile, CorsProperties properties) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        new CorsConfigurationValidator(environment, properties).validateCorsConfiguration();
    }

    private static CorsProperties origins(String... origins) {
        return new CorsProperties(List.of(origins), METHODS, HEADERS);
    }

    @Test
    void validProductionConfiguration_passes() {
        assertThatCode(() -> validate("prod", origins("https://app.example.com")))
                .doesNotThrowAnyException();
    }

    @Test
    void wildcardOrigin_isOnlyAllowedInDevAndTest() {
        assertThatCode(() -> validate("dev", origins("*"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> validate("prod", origins("*"))).isInstanceOf(CorsConfigurationException.class);
    }

    @Test
    void localhostOrigin_isRejectedInProduction() {
        assertThatCode(() -> validate("test", origins("http://localhost:3000"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> validate("prod", origins("http://localhost:3000")))
                .isInstanceOf(CorsConfigurationException.class);
    }

    @Test
    void invalidOrDuplicateOrigins_areRejected() {
        assertThatThrownBy(() -> validate("dev", origins("ftp://files.example.com")))
                .isInstanceOf(CorsConfigurationException.class);
        assertThatThrownBy(() -> validate("dev", origins("https://a.example.com", "https://a.example.com")))
                .isInstanceOf(CorsConfigurationException.class);
        assertThatThrownBy(() -> validate("dev", new CorsProperties(List.of(), METHODS, HEADERS)))
                .isInstanceOf(CorsConfigurationException.class);
    }

    @Test
    void unsafeMethodsAndHeaders_areRejected() {
        assertThatThrownBy(() -> validate("dev",
                new CorsProperties(List.of("https://a.example.com"), List.of("TRACE"), HEADERS)))
                .isInstanceOf(CorsConfigurationException.class);
        assertThatThrownBy(() -> validate("dev",
                new CorsProperties(List.of("https://a.example.com"), METHODS, List.of("*"))))
                .isInstanceOf(CorsConfigurationException.class);
        assertThatThrownBy(() -> validate("dev",
                new CorsProperties(List.of("https://a.example.com"), METHODS, List.of("X-Custom"))))
                .isInstanceOf(CorsConfigurationException.class);
    }
}
