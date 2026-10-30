package ch.sectioninformatique.auth.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ch.sectioninformatique.auth.support.AbstractIntegrationTest;
import ch.sectioninformatique.auth.support.RestDocsSnippets;
import ch.sectioninformatique.auth.support.TestUsers;
import jakarta.servlet.http.Cookie;

/**
 * HTTP tests of the Azure login flow under {@code /oauth2}. Azure itself is simulated
 * with Spring Security's {@code oauth2Login()} request post-processor.
 */
class OAuth2ControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String SESSION_KEY = "OAUTH2_RETURN_URL";
    private static final String CLIENT_CALLBACK = "http://localhost:3000/callback";
    private static final String DEFAULT_REDIRECT = "/auth/redirect-after-login";

    @Autowired
    private AuthCodeService authCodeService;

    private static Map<String, Object> azureAttributes(String email) {
        return Map.of("email", email, "given_name", "Azure", "family_name", "User");
    }

    @Nested
    class InitiateLogin {

        @Test
        void allowedRedirectUrl_isStoredAndBrowserSentToAzure() throws Exception {
            MvcResult result = mockMvc.perform(get("/oauth2/login/azure").param("redirectUrl", CLIENT_CALLBACK))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("/oauth2/authorization/azure"))
                    .andDo(document("oauth2/login-azure"))
                    .andReturn();

            assertThat(result.getRequest().getSession().getAttribute(SESSION_KEY)).isEqualTo(CLIENT_CALLBACK);
        }

        @Test
        void refererHeader_isUsedWhenNoRedirectUrlIsGiven() throws Exception {
            MvcResult result = mockMvc.perform(get("/oauth2/login/azure")
                    .header(HttpHeaders.REFERER, "http://localhost:4000/login"))
                    .andExpect(status().isFound())
                    .andReturn();

            assertThat(result.getRequest().getSession().getAttribute(SESSION_KEY))
                    .isEqualTo("http://localhost:4000/login");
        }

        @Test
        void foreignRedirectUrl_isReplacedByDefault() throws Exception {
            MvcResult result = mockMvc.perform(get("/oauth2/login/azure")
                    .param("redirectUrl", "https://attacker.example/steal"))
                    .andExpect(status().isFound())
                    .andReturn();

            assertThat(result.getRequest().getSession().getAttribute(SESSION_KEY)).isEqualTo(DEFAULT_REDIRECT);
        }
    }

    @Nested
    class Success {

        @Test
        void redirectsToClientWithOneTimeCode() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute(SESSION_KEY, CLIENT_CALLBACK);

            MvcResult result = mockMvc.perform(get("/oauth2/success")
                    .session(session)
                    .with(oauth2Login().attributes(attributes -> attributes.putAll(azureAttributes(TestUsers.USER)))))
                    .andExpect(status().isFound())
                    .andReturn();

            UriComponents redirect = UriComponentsBuilder.fromUriString(result.getResponse().getRedirectedUrl()).build();
            assertThat(redirect.getHost()).isEqualTo("localhost");
            assertThat(redirect.getPath()).isEqualTo("/callback");
            assertThat(redirect.getQueryParams().getFirst("loginType")).isEqualTo("azure");
            assertThat(redirect.getQueryParams().getFirst("authCode")).isNotBlank();
            assertThat(redirect.getQueryParams().getFirst("userId"))
                    .isEqualTo(userService.findByLogin(TestUsers.USER).getId().toString());
            assertThat(session.isInvalid()).isTrue();
        }

        @Test
        void withoutStoredUrl_redirectsToDefaultLandingPage() throws Exception {
            MvcResult result = mockMvc.perform(get("/oauth2/success")
                    .with(oauth2Login().attributes(attributes -> attributes.putAll(azureAttributes(TestUsers.USER)))))
                    .andExpect(status().isFound())
                    .andReturn();

            assertThat(result.getResponse().getRedirectedUrl())
                    .startsWith(DEFAULT_REDIRECT + "?loginType=azure&authCode=");
        }

        @Test
        void firstLogin_createsLocalUserWithUserRole() throws Exception {
            String email = "new.azure.user@test.com";

            mockMvc.perform(get("/oauth2/success")
                    .with(oauth2Login().attributes(attributes -> attributes.putAll(azureAttributes(email)))))
                    .andExpect(status().isFound());

            assertThat(userService.findByLogin(email))
                    .satisfies(user -> {
                        assertThat(user.getFirstName()).isEqualTo("Azure");
                        assertThat(user.getLastName()).isEqualTo("User");
                        assertThat(user.getMainRole()).isEqualTo("USER");
                    });
        }

        @Test
        void accountWithoutLastNameOrGivenName_usesDisplayName() throws Exception {
            String email = "no.last.name@test.com";

            mockMvc.perform(get("/oauth2/success")
                    .with(oauth2Login().attributes(attributes -> {
                        attributes.put("email", email);
                        attributes.put("name", "Display Name");
                    })))
                    .andExpect(status().isFound());

            assertThat(userService.findByLogin(email).getFirstName()).isEqualTo("Display Name");
            assertThat(userService.findByLogin(email).getLastName()).isNull();
        }

        @Test
        void missingEmail_returns401() throws Exception {
            mockMvc.perform(get("/oauth2/success")
                    .with(oauth2Login().attributes(attributes -> attributes.put("name", "No Email"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(message("error.oauth2.missing.user.attribute", "email")));
        }

        @Test
        void softDeletedAccount_returns403() throws Exception {
            mockMvc.perform(get("/oauth2/success")
                    .with(oauth2Login().attributes(attributes -> attributes.putAll(azureAttributes(TestUsers.DELETED)))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(message("error.user.account.deleted", TestUsers.DELETED)));
        }

        @Test
        void withoutAzureAuthentication_returns401() throws Exception {
            mockMvc.perform(get("/oauth2/success"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class TokenExchange {

        private String exchangeBody(Object userId, String code) {
            return """
                    {"userId": %s, "code": "%s"}""".formatted(userId, code);
        }

        @Test
        void validCode_returnsUserWithAccessTokenAndUsableRefreshCookie() throws Exception {
            Long userId = userService.findByLogin(TestUsers.USER).getId();
            String code = authCodeService.generate(TestUsers.USER, CLIENT_CALLBACK);

            MvcResult result = mockMvc.perform(post("/oauth2/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(exchangeBody(userId, code)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.login").value(TestUsers.USER))
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andDo(document("oauth2/token",
                            RestDocsSnippets.authCodeExchangeRequest(),
                            RestDocsSnippets.userResponse(),
                            RestDocsSnippets.refreshCookieSet()))
                    .andReturn();

            // The refresh token must have been stored, otherwise the session could not be extended
            Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
            mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                    .andExpect(status().isOk());
        }

        @Test
        void codeCanOnlyBeUsedOnce() throws Exception {
            Long userId = userService.findByLogin(TestUsers.USER).getId();
            String code = authCodeService.generate(TestUsers.USER, CLIENT_CALLBACK);
            String body = exchangeBody(userId, code);

            mockMvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.authcode.invalid")));
        }

        @Test
        void codeOfAnotherUser_returns400() throws Exception {
            Long otherUserId = userService.findByLogin(TestUsers.MANAGER).getId();
            String code = authCodeService.generate(TestUsers.USER, CLIENT_CALLBACK);

            mockMvc.perform(post("/oauth2/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(exchangeBody(otherUserId, code)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.authcode.invalid")))
                    .andDo(document("oauth2/token-invalid-code", RestDocsSnippets.errorResponse()));
        }

        @Test
        void unknownUserId_returnsSame400AsWrongCode() throws Exception {
            mockMvc.perform(post("/oauth2/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(exchangeBody(999999, "whatever")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(message("error.authcode.invalid")));
        }

        @Test
        void missingFields_return400WithFieldErrors() throws Exception {
            mockMvc.perform(post("/oauth2/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.userId").isNotEmpty())
                    .andExpect(jsonPath("$.fieldErrors.code").isNotEmpty());
        }
    }

    @Test
    void loginFailure_returns401() throws Exception {
        mockMvc.perform(get("/oauth2/error"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(message("error.oauth2.login.failed")))
                .andDo(document("oauth2/error", RestDocsSnippets.errorResponse()));
    }
}
