package ch.sectioninformatique.auth.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Integration tests for AuthController.
 * These tests use real data and the actual application context to verify
 * the authentication endpoints.
 */
@Tag("integration")
@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** UserAuthenticationProvider instance for managing user authentication. */
        @Autowired
        private UserAuthenticationProvider userAuthenticationProvider;

        @Autowired
        private UserService userService;

        /**
         * Test the /auth/login endpoint with real data.
         * This test performs a login request and expects a successful response.
         * The response is saved to a file for use in other tests.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withRealData_shouldReturnSuccess() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.firstName").value("Test"))
                                .andExpect(jsonPath("$.lastName").value("User"))
                                .andExpect(jsonPath("$.login").value("test.user@test.com"))
                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                .andExpect(jsonPath("$.token").isNotEmpty()) // Verify token present and not empty
                                .andExpect(jsonPath("$.refreshToken").isNotEmpty()) // Verify refreshToken present and
                                                                                    // not empty
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/auth-login-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /auth/login endpoint with missing login.
         * This test performs a login request with missing login and expects a bad
         * request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_missingLogin_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-missing-login.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with missing password.
         * This test performs a login request with missing password and expects a bad
         * request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_missingPassword_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-missing-password.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with invalid email format.
         * This test performs a login request with invalid email format and expects a
         * bad request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_invalidEmailFormat_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"invalid-email-format\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-invalid-email-format.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with empty body.
         * This test performs a login request with empty body and expects a bad request
         * response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_emptyBody_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-empty-body.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with malformed JSON.
         * This test performs a login request with malformed JSON and expects a bad
         * request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_malformedJson_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\""))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-malformed-json.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with SQL injection attempt in login.
         * This test performs a login request with SQL injection attempt in login and
         * expects a bad request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_sqlInjectionAttemptLogin_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"' OR '1'='1\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-sql-injection-attempt-login.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with SQL injection attempt in password.
         * This test performs a login request with SQL injection attempt in password and
         * expects an unauthorized response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_sqlInjectionAttemptPassword_shouldReturnUnauthorized() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"' OR '1'='1\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-sql-injection-attempt-password.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with wrong password.
         * This test performs a login request with wrong password and expects an
         * unauthorized response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_wrongPassword_shouldReturnUnauthorized() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"WrongPassword!\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-wrong-password.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with non-existent user.
         * This test performs a login request with non-existent user and expects an
         * unauthorized response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_nonExistentUser_shouldReturnUnauthorized() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"non.existent@test.com\", \"password\":\"WrongPassword!\"}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-response-non-existent-user.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/login endpoint with wrong content type.
         * This test performs a login request with wrong content type and expects an
         * unsupported media type response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_wrongContentType_shouldReturnUnsupportedMediaType() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/login")
                                .content("{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-login-wrong-content-type.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/register endpoint with real data.
         * This test performs a registration request and expects a successful response.
         * The response is saved to a file for use in other tests.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_withRealData_shouldReturnSuccess() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.firstName").value("Test"))
                                .andExpect(jsonPath("$.lastName").value("NewUser"))
                                .andExpect(jsonPath("$.login").value("test.newuser@test.com"))
                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                .andExpect(jsonPath("$.token").isNotEmpty()) // Verify token present and not empty
                                .andExpect(jsonPath("$.refreshToken").isNotEmpty()) // Verify refreshToken present and
                                                                                    // not empty
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: fetch user again and verify user created
                UserDto updatedUser = userService.findByLogin("test.newuser@test.com");
                assertNotNull(updatedUser, "User should not be null after registration");
                assertEquals("Test", updatedUser.getFirstName(), "User first name should match");
                assertEquals("NewUser", updatedUser.getLastName(), "User last name should match");
                assertEquals("test.newuser@test.com", updatedUser.getLogin(), "User login should match");
                assertEquals("USER", updatedUser.getMainRole(), "User role should be USER");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/auth-register-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /auth/register endpoint with missing first name.
         * This test performs a registration request with missing first name and
         * expects a bad request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_missingFirstName_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-register-response-missing-first-name.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/register endpoint with missing last name.
         * This test performs a registration request with missing last name and
         * expects a bad request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void register_missingLastName_shouldReturnBadRequest() throws Exception {
                MvcResult result = mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                int status = result.getResponse().getStatus();

                // Parse original response body
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
                });

                // Add status code
                responseMap.put("status", status);

                // Serialize updated map to JSON
                String wrappedResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap);

                // Save response to file
                Path path = Paths.get("target/test-data/auth-register-response-missing-last-name.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, wrappedResponse);
        }

        /**
         * Test the /auth/refresh endpoint with real data.
         * This test performs a token refresh request and expects a successful response.
         * The response is saved to a file for use in other tests.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void refresh_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

                MvcResult result = mockMvc.perform(get("/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.firstName").value("Test"))
                                .andExpect(jsonPath("$.lastName").value("User"))
                                .andExpect(jsonPath("$.login").value("test.user@test.com"))
                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                .andExpect(jsonPath("$.token").isNotEmpty()) // Verify token present and not empty
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/auth-refresh-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/auth-refresh-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, refreshToken);
        }
}
