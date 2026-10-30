package ch.sectioninformatique.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ch.sectioninformatique.auth.auth.token.RefreshToken;
import ch.sectioninformatique.auth.auth.token.RefreshTokenRepository;
import ch.sectioninformatique.auth.auth.token.RefreshTokenService;
import ch.sectioninformatique.auth.security.TokenHasher;
import ch.sectioninformatique.auth.support.AbstractIntegrationTest;
import ch.sectioninformatique.auth.support.RestDocsSnippets;
import ch.sectioninformatique.auth.support.TestUsers;
import jakarta.servlet.http.Cookie;

/**
 * HTTP tests of the session endpoints under {@code /auth}.
 */
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String REFRESH_COOKIE = "refresh_token";

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private static MockHttpServletRequestBuilder jsonPost(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String credentials(String login, String password) {
        return """
                {"login": "%s", "password": "%s"}""".formatted(login, password);
    }

    /** Logs in and returns the refresh token set in the cookie. */
    private String loginAndGetRefreshToken(String login, String password) throws Exception {
        MvcResult result = mockMvc.perform(jsonPost("/auth/login", credentials(login, password)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(REFRESH_COOKIE).getValue();
    }

    @Nested
    class Login {

        @Test
        void validCredentials_returnUserWithAccessTokenAndRefreshCookie() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials(TestUsers.USER, TestUsers.USER_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.USER))
                    .andExpect(jsonPath("$.mainRole").value("USER"))
                    .andExpect(jsonPath("$.permissions").isArray())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(cookie().exists(REFRESH_COOKIE))
                    .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                    .andExpect(cookie().secure(REFRESH_COOKIE, true))
                    .andExpect(cookie().path(REFRESH_COOKIE, "/auth/refresh"))
                    .andExpect(cookie().maxAge(REFRESH_COOKIE, greaterThan(0)))
                    .andDo(document("auth/login",
                            RestDocsSnippets.loginRequest(),
                            RestDocsSnippets.userResponse(),
                            RestDocsSnippets.refreshCookieSet()));
        }

        @Test
        void wrongPassword_returns401() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials(TestUsers.USER, "WrongPassword1!")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.authorisation.invalid.credentials")))
                    .andExpect(cookie().doesNotExist(REFRESH_COOKIE))
                    .andDo(document("auth/login-invalid-credentials", RestDocsSnippets.errorResponse()));
        }

        @Test
        void unknownUser_returnsSame401AsWrongPassword() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials("nobody@test.com", TestUsers.USER_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.authorisation.invalid.credentials")));
        }

        @Test
        void softDeletedUser_cannotLogIn() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials(TestUsers.DELETED, TestUsers.USER_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.authorisation.invalid.credentials")));
        }

        @Test
        void missingFields_return400WithFieldErrors() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", "{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.validation.failed")))
                    .andExpect(jsonPath("$.fieldErrors.login").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty())
                    .andDo(document("auth/login-validation-error", RestDocsSnippets.errorResponse()));
        }

        @Test
        void invalidEmailOrInjectionAttempt_returns400() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials("' OR '1'='1", TestUsers.USER_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
        }

        @Test
        void passwordTooShort_returns400() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials(TestUsers.USER, "short")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
        }

        @Test
        void malformedJson_returns400WithoutParserDetails() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", "{\"login\": \"test.user@test.com\", "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.request.body.unreadable")))
                    .andDo(document("auth/login-malformed-json", RestDocsSnippets.errorResponse()));
        }

        @Test
        void unsupportedMediaType_returns415() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(credentials(TestUsers.USER, TestUsers.USER_PASSWORD)))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value(415))
                    .andDo(document("auth/login-unsupported-media-type", RestDocsSnippets.errorResponse()));
        }

        @Test
        void acceptLanguageHeader_selectsMessageLanguage() throws Exception {
            mockMvc.perform(jsonPost("/auth/login", credentials(TestUsers.USER, "WrongPassword1!"))
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials"));
        }

        @Test
        void langParameter_takesPrecedenceOverAcceptLanguage() throws Exception {
            mockMvc.perform(jsonPost("/auth/login?lang=fr", credentials(TestUsers.USER, "WrongPassword1!"))
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Identifiants invalides"));
        }

        @Test
        void unsupportedLanguage_fallsBackToFrench() throws Exception {
            mockMvc.perform(jsonPost("/auth/login?lang=de", credentials(TestUsers.USER, "WrongPassword1!")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Identifiants invalides"));
        }
    }

    @Nested
    class Refresh {

        @Test
        void validCookie_returnsNewAccessTokenAndRotatesRefreshToken() throws Exception {
            String refreshToken = loginAndGetRefreshToken(TestUsers.USER, TestUsers.USER_PASSWORD);

            MvcResult result = mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(cookie().exists(REFRESH_COOKIE))
                    .andDo(document("auth/refresh",
                            RestDocsSnippets.refreshCookieSent(),
                            RestDocsSnippets.tokenResponse(),
                            RestDocsSnippets.refreshCookieSet()))
                    .andReturn();

            String newAccessToken = JsonPath.read(
                    result.getResponse().getContentAsString(), "$.accessToken");
            mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.USER));
        }

        @Test
        void previousRefreshToken_isRejectedAfterRotation() throws Exception {
            String refreshToken = loginAndGetRefreshToken(TestUsers.USER, TestUsers.USER_PASSWORD);
            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.refresh.token.invalid")));
        }

        @Test
        void missingCookie_returns401() throws Exception {
            mockMvc.perform(post("/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.refresh.token.missing")))
                    .andDo(document("auth/refresh-missing-cookie", RestDocsSnippets.errorResponse()));
        }

        @Test
        void malformedToken_returns401() throws Exception {
            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "not.a.jwt")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.refresh.token.invalid")))
                    .andDo(document("auth/refresh-invalid-token", RestDocsSnippets.errorResponse()));
        }

        @Test
        void accessTokenUsedAsRefreshToken_returns401() throws Exception {
            String accessToken = bearer(TestUsers.USER).substring("Bearer ".length());

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, accessToken)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void validlySignedTokenThatWasNeverIssued_returns401() throws Exception {
            String unknownToken = jwtService.createRefreshToken(userService.findByLogin(TestUsers.USER));

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, unknownToken)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void tokenOfUserDeletedSinceLogin_returns401() throws Exception {
            String refreshToken = loginAndGetRefreshToken(TestUsers.USER, TestUsers.USER_PASSWORD);
            userService.delete(TestUsers.USER, false, TestUsers.ADMIN);

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Logout {

        @Test
        void revokesRefreshTokenAndClearsCookie() throws Exception {
            String refreshToken = loginAndGetRefreshToken(TestUsers.USER, TestUsers.USER_PASSWORD);

            mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(TestUsers.USER)))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().value(REFRESH_COOKIE, ""))
                    .andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
                    .andDo(document("auth/logout",
                            RestDocsSnippets.authorizationHeader(),
                            RestDocsSnippets.refreshCookieCleared()));

            mockMvc.perform(post("/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, refreshToken)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void withoutAccessToken_returns401() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message")
                            .value(message("error.security.authentication.token.invalid.or.missing")))
                    .andDo(document("auth/logout-unauthenticated", RestDocsSnippets.errorResponse()));
        }

        @Test
        void withExpiredAccessToken_returns401TokenExpired() throws Exception {
            mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, expiredBearer(TestUsers.USER)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.token.expired")))
                    .andDo(document("auth/expired-access-token", RestDocsSnippets.errorResponse()));
        }

        @Test
        void securityErrors_followTheRequestLanguage() throws Exception {
            mockMvc.perform(post("/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, expiredBearer(TestUsers.USER))
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Token has expired"));

            mockMvc.perform(post("/auth/logout").param("lang", "en"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid or missing authentication token"));
        }

        @Test
        void withMalformedAccessToken_returns401TokenInvalid() throws Exception {
            mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not.a.valid.token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.token.invalid")));
        }

        @Test
        void withTamperedAccessToken_returns401InvalidSignature() throws Exception {
            String token = bearer(TestUsers.USER);
            String tampered = token.substring(0, token.length() - 4) + "AAAA";

            mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, tampered))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.security.token.invalid.signature")));
        }

        @Test
        void withRefreshTokenAsAccessToken_returns401() throws Exception {
            String refreshToken = refreshTokenService.issue(userService.findByLogin(TestUsers.USER));

            mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void redirectAfterLogin_isPublicAndReturnsMessage() throws Exception {
        mockMvc.perform(get("/auth/redirect-after-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(message("message.login.success")))
                .andDo(document("auth/redirect-after-login", RestDocsSnippets.messageResponse()));
    }

    @Test
    void refreshTokens_areStoredHashed() {
        String refreshToken = refreshTokenService.issue(userService.findByLogin(TestUsers.USER));

        RefreshToken stored = refreshTokenRepository.findByUserLoginAndRevokedFalse(TestUsers.USER).orElseThrow();
        assertThat(stored.getTokenHash())
                .isNotEqualTo(refreshToken)
                .isEqualTo(TokenHasher.sha256(refreshToken));
    }
}
