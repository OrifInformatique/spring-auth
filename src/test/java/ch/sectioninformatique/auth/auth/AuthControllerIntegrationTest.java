package ch.sectioninformatique.auth.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

import java.util.function.Consumer;

import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for AuthController.
 * These tests use real data and the actual application context to verify
 * the authentication endpoints.
 */
@Tag("integration")
@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerIntegrationTest {

        /**
         * Helper method for performing and documenting HTTP requests in tests.
         * This reduces repetition by centralizing the request execution and REST Docs
         * generation.
         *
         * @param requestTypeString HTTP method (GET, POST, PUT, etc.)
         * @param endpoint          API endpoint to call
         * @param token             Optional JWT token for authentication
         * @param contentType       Content type for the request
         * @param expectedStatus    Expected HTTP status code (e.g. 200)
         * @param docsFileName      Name for the generated REST Docs snippet
         * @param script            Optional lambda to perform additional assertions
         * 
         * @throws Exception
         */
        private void performRequest(
                        String requestTypeString,
                        String endpoint,
                        String content,
                        String token,
                        MediaType contentType,
                        int expectedStatus,
                        String docsFileName,
                        Consumer<ResultActions> script) throws Exception {

                var requestType = get(endpoint);

                if (requestTypeString.equals("GET")) {
                        requestType = get(endpoint);
                } else if (requestTypeString.equals("POST")) {
                        requestType = post(endpoint);
                } else if (requestTypeString.equals("PUT")) {
                        requestType = put(endpoint);
                } else if (requestTypeString.equals("DELETE")) {
                        requestType = delete(endpoint);
                } else {
                        throw new IllegalArgumentException("Unsupported request type: " + requestTypeString);
                }

                // Set content only if it's not null
                if (content != null) {
                        requestType.content(content);
                }

                // Set Authorization header only if token is provided
                if (token != null) {
                        requestType.header("Authorization", "Bearer " + token);
                }

                // Set content type
                requestType.contentType(contentType);

                // Perform request
                var request = mockMvc.perform(requestType)
                                .andExpect(status().is(expectedStatus));

                // Execute any additional assertions provided in the lambda
                if (script != null) {
                        script.accept(request);
                }

                // Generate a REST Docs snippet for the request/response pair
                request.andDo(document("auth/" + docsFileName, preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));

        }

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** UserAuthenticationProvider instance for managing user authentication. */
        @Autowired
        private UserAuthenticationProvider userAuthenticationProvider;

        @Autowired
        private UserService userService;

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in successfull with valid credentials.
         */
        @Test
        @Transactional
        public void login_withRealData_shouldReturnSuccess() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                200,
                                "login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.id").isNotEmpty())
                                                                .andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("User"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.user@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                                                .andExpect(jsonPath("$.token").isNotEmpty())
                                                                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in without login and test the excetpion.
         */
        @Test
        @Transactional
        public void login_missingLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-missing-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in without password and test the excetpion.
         */
        @Test
        @Transactional
        public void login_missingPassword_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-missing-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with invalid email format and test the excetpion.
         */
        @Test
        @Transactional
        public void login_invalidEmailFormat_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"invalid-email-format\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-invalid-email-format",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with empty body and test the excetpion.
         */
        @Test
        @Transactional
        public void login_emptyBody_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-empty-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with malformed JSON and test the excetpion.
         */
        @Test
        @Transactional
        public void login_malformedJson_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-malformed-json",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with SQL injection attempt in login and test the
         * excetpion.
         */
        @Test
        @Transactional
        public void login_sqlInjectionAttemptLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"' OR '1'='1\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-sql-injection-attempt-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with wrong password and test the excetpion.
         */
        @Test
        @Transactional
        public void login_wrongPassword_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"WrongPassword!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "login-wrong-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login
         *
         * Mock a user log in with non-existent user and test the excetpion.
         */
        @Test
        @Transactional
        public void login_nonExistentUser_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"non.existent@test.com\", \"password\":\"WrongPassword!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "login-non-existent-user",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a successfull user register with valid credentials.
         */
        @Test
        @Transactional
        public void register_withRealData_shouldReturnSuccess() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                201,
                                "register",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.id").isNotEmpty())
                                                                .andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("NewUser"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.newuser@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                                                .andExpect(jsonPath("$.token").isNotEmpty())
                                                                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

                                                // Assert: fetch user again and verify user created
                                                UserDto updatedUser = userService.findByLogin("test.newuser@test.com");
                                                assertNotNull(updatedUser,
                                                                "User should not be null after registration");
                                                assertEquals("Test", updatedUser.getFirstName(),
                                                                "User first name should match");
                                                assertEquals("NewUser", updatedUser.getLastName(),
                                                                "User last name should match");
                                                assertEquals("test.newuser@test.com", updatedUser.getLogin(),
                                                                "User login should match");
                                                assertEquals("USER", updatedUser.getMainRole(),
                                                                "User role should be USER");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with missing firstname.
         */
        @Test
        @Transactional
        public void register_missingFirstName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-first-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with missing lastname.
         */
        @Test
        @Transactional
        public void register_missingLastName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-last-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with missing login.
         */
        @Test
        @Transactional
        public void register_missingLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with missing password.
         */
        @Test
        @Transactional
        public void register_missingPassword_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"login\":\"test.newuser@test.com\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with invalid email format.
         */
        @Test
        @Transactional
        public void register_invalidEmailFormat_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"login\":\"invalid-email-format\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-invalid-email-format",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with empty body.
         */
        @Test
        @Transactional
        public void register_emptyBody_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-empty-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register with malformed JSON.
         */
        @Test
        @Transactional
        public void register_malformedJson_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"User\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-malformed-json",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register SQL with injection attempt in first name.
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptFirstName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"' OR '1'='1\", \"lastName\":\"User\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-first-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register SQL with injection attempt in last name.
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptLastName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"' OR '1'='1\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-last-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register SQL with injection attempt in login.
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"User\", \"login\":\"' OR '1'='1\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register
         *
         * Mock a failed user register SQL with duplicate login.
         */
        @Test
        @Transactional
        public void register_duplicateLogin_shouldReturnConflict() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"User\",\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                409,
                                "register-duplicate-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /auth/refresh
         *
         * Mock a user successfull refresh token with valid credentials.
         */
        @Test
        @Transactional
        public void refresh_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                refreshToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "refresh",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("User"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.user@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                                                .andExpect(jsonPath("$.token").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /auth/refresh
         *
         * Mock a user failed user refresh token with missing token.
         */
        @Test
        @Transactional
        public void refresh_missingToken_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-missing-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /auth/refresh
         *
         * Mock a user failed user refresh token with invalid token.
         */
        @Test
        @Transactional
        public void refresh_invalidToken_shouldReturnUnauthorized() throws Exception {
                String invalidToken = "this.is.not.a.valid.token";

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                invalidToken,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-invalid-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /auth/update-password
         *
         * Mock a user successfull update his password.
         */
        @Test
        @Transactional
        public void setPassword_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "{\"oldPassword\":\"Test1234!\", \"newPassword\":\"TestNewPassword\"}",
                                refreshToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "update-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /auth/update-password
         *
         * Mock a user failing at updating his password without a body in his request.
         */
        @Test
        @Transactional
        public void setPassword_missingBody_shouldReturnBadRequest() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "",
                                refreshToken,
                                MediaType.APPLICATION_JSON,
                                400,
                                "update-password-missing-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /auth/update-password
         *
         * Mock a user failing at updating his password with missing token.
         */
        @Test
        @Transactional
        public void setPassword_missingToken_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "{\"oldPassword\":\"Test1234!\", \"newPassword\":\"TestNewPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "update-password-missing-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }
}
