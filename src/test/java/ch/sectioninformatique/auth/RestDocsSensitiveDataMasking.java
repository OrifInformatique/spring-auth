package ch.sectioninformatique.auth;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.OperationResponseFactory;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;

/**
 * Masks JWT access tokens and refresh-token cookies in REST Docs snippets so that
 * generated documentation never commits real credentials to the repository.
 */
public final class RestDocsSensitiveDataMasking {

    private static final OperationRequestFactory REQUEST_FACTORY = new OperationRequestFactory();
    private static final OperationResponseFactory RESPONSE_FACTORY = new OperationResponseFactory();

    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile(
            "Bearer\\s+eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private static final Pattern REFRESH_TOKEN_COOKIE = Pattern.compile(
            "refresh_token=eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private static final Pattern JSON_TOKEN_FIELD = Pattern.compile(
            "(\"token\"\\s*:\\s*\")(eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)(\")");

    private static final Pattern JSON_ACCESS_TOKEN_FIELD = Pattern.compile(
            "(\"accessToken\"\\s*:\\s*\")(eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+)(\")");

    private RestDocsSensitiveDataMasking() {
    }

    public static OperationPreprocessor maskSensitiveData() {
        return new OperationPreprocessor() {
            @Override
            public OperationRequest preprocess(OperationRequest request) {
                byte[] content = maskText(request.getContentAsString()).getBytes(StandardCharsets.UTF_8);
                OperationRequest withContent = REQUEST_FACTORY.createFrom(request, content);
                return REQUEST_FACTORY.createFrom(withContent, maskHeaders(request.getHeaders()));
            }

            @Override
            public OperationResponse preprocess(OperationResponse response) {
                byte[] content = maskText(response.getContentAsString()).getBytes(StandardCharsets.UTF_8);
                OperationResponse withContent = RESPONSE_FACTORY.createFrom(response, content);
                return RESPONSE_FACTORY.createFrom(withContent, maskHeaders(response.getHeaders()));
            }
        };
    }

    static String maskText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = AUTHORIZATION_BEARER.matcher(text).replaceAll("Bearer <access-token>");
        masked = REFRESH_TOKEN_COOKIE.matcher(masked).replaceAll("refresh_token=<refresh-token>");
        masked = JSON_TOKEN_FIELD.matcher(masked).replaceAll("$1<jwt-access-token>$3");
        masked = JSON_ACCESS_TOKEN_FIELD.matcher(masked).replaceAll("$1<jwt-access-token>$3");
        return JWT.matcher(masked).replaceAll("<jwt>");
    }

    private static HttpHeaders maskHeaders(HttpHeaders headers) {
        HttpHeaders masked = new HttpHeaders();
        headers.forEach((name, values) -> values.forEach(value -> masked.add(name, maskText(value))));
        return masked;
    }
}
