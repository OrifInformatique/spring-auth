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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private UserAuthenticationProvider userAuthenticationProvider;

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
                        "{\"login\":\"john.doe@test.com\", \"password\":\"Secure123@Pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("DOE"))
                .andExpect(jsonPath("$.login").value("john.doe@test.com"))
                .andExpect(jsonPath("$.mainRole").value("USER"))
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
                        "{\"firstName\":\"Test\",\"lastName\":\"Test\",\"login\":\"test.login@test.com\", \"password\":\"testPassword\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
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
        UserDto userDto = UserDto.builder()
                .id(8L)
                .firstName("Super")
                .lastName("Admin")
                .login("super.admin@test.com")
                .token(null)
                .refreshToken(null)
                .mainRole("ADMIN")
                .permissions(new ArrayList<String>())
                .build();

        String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

        MvcResult result = mockMvc.perform(get("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Save response to file for later tests
        Path path = Paths.get("target/test-data/auth-refresh-response.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, responseBody);
    }
}
