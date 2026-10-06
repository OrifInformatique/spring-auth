package ch.sectioninformatique.auth.support;

import static ch.sectioninformatique.auth.support.RestDocsSensitiveDataMasking.maskSensitiveData;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.snippet.Snippet;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.security.JwtService;
import ch.sectioninformatique.auth.user.UserService;

/**
 * Base class of the HTTP integration tests.
 *
 * Tests run against the full application (test profile, MariaDB test schema) through
 * MockMvc. Each test runs in a transaction rolled back at the end, so tests are
 * independent and always start from the data of {@link TestUserSeeder}.
 *
 * Successful and representative error exchanges are recorded with {@link #document}
 * as Spring REST Docs snippets, which are assembled into the API documentation by
 * {@code src/asciidoc/index.adoc}.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
@Transactional
public abstract class AbstractIntegrationTest {

    /** Locale used by the API when the request does not specify one. */
    protected static final Locale DEFAULT_LOCALE = Locale.FRANCE;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected UserService userService;

    @Autowired
    private MessageSource messageSource;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * @param login login of an active user
     * @return a valid "Bearer ..." Authorization header value for this user
     */
    protected String bearer(String login) {
        return "Bearer " + jwtService.createAccessToken(userService.findByLogin(login));
    }

    /**
     * @param login login of an active user
     * @return an Authorization header value carrying an already expired token
     */
    protected String expiredBearer(String login) {
        Instant issuedLongAgo = Instant.now().minus(Duration.ofDays(1));
        return "Bearer " + jwtService.createAccessToken(userService.findByLogin(login), issuedLongAgo);
    }

    /**
     * @return the message the API returns by default for this key
     */
    protected String message(String key, Object... args) {
        return messageSource.getMessage(key, args, DEFAULT_LOCALE);
    }

    /**
     * Records the exchange as REST Docs snippets, with tokens masked and JSON pretty-printed.
     *
     * @param identifier snippet folder, e.g. "users/get-user"
     * @param snippets   additional snippets (field descriptions, parameters...)
     */
    protected static RestDocumentationResultHandler document(String identifier, Snippet... snippets) {
        return MockMvcRestDocumentation.document(identifier,
                preprocessRequest(maskSensitiveData(), prettyPrint()),
                preprocessResponse(maskSensitiveData(), prettyPrint()),
                snippets);
    }
}
