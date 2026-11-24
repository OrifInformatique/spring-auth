package ch.sectioninformatique.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import ch.sectioninformatique.auth.common.web.ErrorResponseWriter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests of the 401 entry point and the 403 handler, which must answer with the
 * standard JSON error body.
 */
class SecurityErrorHandlersTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private ErrorResponseWriter writer;

    @BeforeEach
    void setUp() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("error.security.authentication.token.invalid.or.missing", Locale.ENGLISH, "Missing token");
        messages.addMessage("error.security.access.denied", Locale.ENGLISH, "Access denied");
        writer = new ErrorResponseWriter(objectMapper, messages, new FixedLocaleResolver(Locale.ENGLISH));
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsString());
    }

    @Test
    void entryPoint_answers401WithJsonError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new UserAuthenticationEntryPoint(writer).commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = body(response);
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("message").asString()).isEqualTo("Missing token");
        assertThat(body.has("timestamp")).isTrue();
        assertThat(body.has("fieldErrors")).isFalse();
    }

    @Test
    void accessDeniedHandler_answers403WithJsonError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAccessDeniedHandler(writer).handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = body(response);
        assertThat(body.get("error").asString()).isEqualTo("Forbidden");
        assertThat(body.get("message").asString()).isEqualTo("Access denied");
    }
}
