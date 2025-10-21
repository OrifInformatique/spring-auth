package ch.sectioninformatique.auth.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Documentation tests for AuthController.
 * These tests generate API documentation snippets using Spring REST Docs.
 * 
 * NOTE: These tests depend on test output files (JSON response files) generated
 * by a separate integration test (AuthControllerIntegrationTest). If those
 * files
 * are missing, the tests will fail early with a clear message.
 */
@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerDocTest {

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** Static variable to hold the login response JSON for use in tests. */
        private static String loginResponseJson;

        /** Static variable to hold the register response JSON for use in tests. */
        private static String registerResponseJson;

        /** Static variable to hold the refresh response JSON for use in tests. */
        private static String refreshResponseJson;

        /** Static variable to hold the refresh token for use in tests. */
        private static String refreshToken;

        /** Mocked UserService for simulating user-related operations. */
        @MockBean
        private UserService userService;

        /**
         * Mocked UserAuthenticationProvider for simulating authentication operations.
         */
        @MockBean
        private UserAuthenticationProvider userAuthenticationProvider;

        /** Mocked Authentication for simulating security context. */
        @MockBean
        private Authentication authentication;

        /** Mocked SecurityContext for simulating security context. */
        @MockBean
        private SecurityContext securityContext;

        /**
         * Test the /auth/login endpoint with mocked services to generate documentation.
         * This test stubs the UserService and UserAuthenticationProvider to return
         * predefined data, performs a login request, and generates API documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc() throws Exception {
                // Load login response from integration test output to ensure consistency
                // between doc and actual behavior
                Path path = Paths.get("target/test-data/auth-login-response.json");

                // Early fail if the required file is missing
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required login response data. Make sure AuthControllerIntegrationTest ran first.");
                }

                // Read the JSON response from file
                loginResponseJson = Files.readString(path);

                // Parse JSON to extract fields for mocking
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(loginResponseJson);

                // Build a UserDto from the parsed JSON to simulate a real login response
                UserDto userDto = UserDto.builder()
                                .id(jsonNode.get("id").asLong())
                                .firstName(jsonNode.get("firstName").asText())
                                .lastName(jsonNode.get("lastName").asText())
                                .login(jsonNode.get("login").asText())
                                .token(jsonNode.get("token").asText(null))
                                .refreshToken(jsonNode.get("refreshToken").asText(null))
                                .mainRole(jsonNode.get("mainRole").asText("USER"))
                                .permissions(new ArrayList<String>())
                                .build();

                // Mock userService.login(...) and token creation methods to return expected
                // data
                when(userService.login(any())).thenReturn(userDto);
                when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());
                when(userAuthenticationProvider.createRefreshToken(any())).thenReturn(userDto.getRefreshToken());

                // Perform the /auth/login request with expected input and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isOk())
                                .andDo(document("auth/login", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with missing login to generate documentation.
         * This test performs a login request with missing login and expects a bad
         * request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_missingLogin() throws Exception {
                // Perform the /auth/login request with missing login and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-missing-login", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with missing password to generate
         * documentation.
         * This test performs a login request with missing password and expects a bad
         * request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_missingPassword() throws Exception {
                // Perform the /auth/login request with missing password and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-missing-password", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with invalid email format to generate
         * documentation.
         * This test performs a login request with invalid email format and expects a
         * bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_invalidEmailFormat() throws Exception {
                // Perform the /auth/login request with invalid email format and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"invalid-email-format\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-invalid-email-format", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with empty body to generate documentation.
         * This test performs a login request with empty body and expects a bad
         * request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_emptyBody() throws Exception {
                // Perform the /auth/login request with empty body and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-empty-body", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with malformed JSON to generate documentation.
         * This test performs a login request with malformed JSON and expects a bad
         * request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_malformedJson() throws Exception {
                // Perform the /auth/login request with malformed JSON and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\""))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-malformed-json", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with SQL injection attempt in login to
         * generate documentation.
         * This test performs a login request with SQL injection attempt in login and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_sqlInjectionAttemptLogin() throws Exception {
                // Perform the /auth/login request with SQL injection attempt and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"' OR '1'='1\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/login-sql-injection-attempt-login",
                                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with SQL injection attempt in password to
         * generate documentation.
         * This test stubs the UserService to throw an AppException for SQL injection
         * attempt,
         * performs a login request, and generates API documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_sqlInjectionAttemptPassword() throws Exception {

                when(userService.login(any()))
                                .thenThrow(new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

                // Perform the /auth/login request with SQL injection attempt in password and
                // validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"' OR '1'='1\"}"))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("auth/login-sql-injection-attempt-password",
                                                preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with wrong password to generate documentation.
         * This test stubs the UserService to throw an AppException for invalid
         * password,
         * performs a login request, and generates API documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_wrongPassword() throws Exception {

                when(userService.login(any()))
                                .thenThrow(new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

                // Perform the /auth/login request with wrong password and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"WrongPassword!\"}"))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("auth/login-wrong-password", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with non-existent user to generate
         * documentation.
         * This test stubs the UserService to throw an AppException for non-existent
         * user,
         * performs a login request, and generates API documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_nonExistentUser() throws Exception {
                when(userService.login(any()))
                                .thenThrow(new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

                // Perform the /auth/login request with non-existent user and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"WrongPassword!\"}"))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("auth/login-non-existent-user", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/login endpoint with wrong media type to generate
         * documentation.
         * This test performs a login request with wrong media type and expects an
         * unsupported media type response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc_wrongMediaType() throws Exception {
                // Perform the /auth/login request with non-existent MediaType and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/login")
                                .content("{\"login\":\"non.existent@test.com\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andDo(document("auth/login-wrong-media-type", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/register endpoint with mocked services to generate
         * documentation.
         * This test stubs the UserService and UserAuthenticationProvider to return
         * predefined data, performs a registration request, and generates API
         * documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withMockedService_generatesDoc() throws Exception {
                /*
                 * Load register response from integration test output to ensure consistency
                 * between doc and actual behavior
                 */
                Path path = Paths.get("target/test-data/auth-register-response.json");

                // Early fail if the required file is missing
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required register response data. Make sure AuthControllerIntegrationTest ran first.");
                }

                // Read the JSON response from file
                registerResponseJson = Files.readString(path);

                // Parse JSON to extract fields for mocking
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(registerResponseJson);

                // Build a UserDto from the parsed JSON to simulate a real register response
                UserDto userDto = UserDto.builder()
                                .id(jsonNode.get("id").asLong())
                                .firstName(jsonNode.get("firstName").asText())
                                .lastName(jsonNode.get("lastName").asText())
                                .login(jsonNode.get("login").asText())
                                .token(jsonNode.get("token").asText(null))
                                .refreshToken(jsonNode.get("refreshToken").asText(null))
                                .mainRole(jsonNode.get("mainRole").asText("USER"))
                                .permissions(new ArrayList<String>())
                                .build();

                // Mock userService.register(...) and token creation methods to return expected
                // data
                when(userService.register(any())).thenReturn(userDto);
                when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());
                when(userAuthenticationProvider.createRefreshToken(any())).thenReturn(userDto.getRefreshToken());

                // Perform the /auth/register request with expected input and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isCreated())
                                .andDo(document("auth/register", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/register endpoint with missing first name to generate
         * documentation.
         * This test performs a registration request with missing first name and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withMockedService_generatesDoc_missingFirstName() throws Exception {
                // Perform the /auth/register request with missing first name and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/register-missing-first-name", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/register endpoint with missing last name to generate
         * documentation.
         * This test performs a registration request with missing last name and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withMockedService_generatesDoc_missingLastName() throws Exception {
                // Perform the /auth/register request with missing last name and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/register-missing-last-name", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/register endpoint with missing login to generate
         * documentation.
         * This test performs a registration request with missing login and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withMockedService_generatesDoc_missingLogin() throws Exception {
                // Perform the /auth/register request with missing login and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/register-missing-login", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/register endpoint with missing password to generate
         * documentation.
         * This test performs a registration request with missing password and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withMockedService_generatesDoc_missingPassword() throws Exception {
                // Perform the /auth/register request with missing password and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"login\":\"test.user@test.com\"}"))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/register-missing-password", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/refresh endpoint with mocked services to generate
         * documentation.
         * This test stubs the UserService and UserAuthenticationProvider to return
         * predefined data, performs a token refresh request, and generates API
         * documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void refresh_withMockedService_generatesDoc() throws Exception {
                // Load refresh response and token from integration test output to ensure
                // consistency between doc and actual behavior
                Path path = Paths.get("target/test-data/auth-refresh-response.json");
                Path pathToken = Paths.get("target/test-data/auth-refresh-token.txt");

                // Early fails if the required files are missing
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required refresh response data. Make sure AuthControllerIntegrationTest ran first.");
                }
                if (!Files.exists(pathToken)) {
                        throw new IllegalStateException(
                                        "Missing required token data. Make sure AuthControllerIntegrationTest ran first.");
                }

                // Read the JSON response and token from files
                refreshResponseJson = Files.readString(path);
                refreshToken = Files.readString(pathToken);

                // Parse JSON to extract fields for mocking
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(refreshResponseJson);

                // Build a UserDto from the parsed JSON to simulate a real refresh response
                UserDto userDto = UserDto.builder()
                                .id(jsonNode.get("id").asLong())
                                .firstName(jsonNode.get("firstName").asText())
                                .lastName(jsonNode.get("lastName").asText())
                                .login(jsonNode.get("login").asText())
                                .token(jsonNode.get("token").asText(null))
                                .refreshToken(jsonNode.get("refreshToken").asText(null))
                                .mainRole(jsonNode.get("mainRole").asText("USER"))
                                .permissions(new ArrayList<String>())
                                .build();

                // Mock Spring Security context and authentication to simulate a valid
                // authenticated user
                when(authentication.getPrincipal()).thenReturn(userDto);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                // Mock userService.refreshLogin(...) and token creation method to return
                // expected data
                when(userService.refreshLogin(any())).thenReturn(userDto);
                when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());

                // Perform the /auth/refresh request with expected input and validate response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(get("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isOk())
                                .andDo(document("auth/refresh", preprocessResponse(prettyPrint())));
        }
}