package ch.sectioninformatique.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import ch.sectioninformatique.auth.support.AbstractIntegrationTest;

/**
 * Fails when an endpoint exists in the code but is not described in the API documentation.
 *
 * Each endpoint must appear in {@code src/asciidoc/index.adoc} as {@code `METHOD /path`}
 * (e.g. {@code `PUT /users/{login}/role`}). Together with the REST Docs field contracts
 * and the Asciidoctor check on missing snippets, this keeps the documentation in sync
 * with the API.
 */
class ApiDocumentationCoverageTest extends AbstractIntegrationTest {

    private static final Path API_DOCUMENTATION = Path.of("src/asciidoc/index.adoc");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyEndpointIsDocumented() throws IOException {
        String documentation = Files.readString(API_DOCUMENTATION);

        List<String> undocumented = new ArrayList<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().startsWith("ch.sectioninformatique.auth")) {
                return;
            }
            mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods()
                    .forEach(method -> {
                        String endpoint = "`" + method + " " + path + "`";
                        if (!documentation.contains(endpoint)) {
                            undocumented.add(endpoint);
                        }
                    }));
        });

        assertThat(undocumented).as("Endpoints missing from %s", API_DOCUMENTATION).isEmpty();
    }
}
