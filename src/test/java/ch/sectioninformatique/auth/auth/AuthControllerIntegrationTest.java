package ch.sectioninformatique.auth.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
