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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .content("{\"login\":\"super.admin@test.com\", \"password\":\"ReallySecure123@PassWordBecauseIWantToBeSuperSafe\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Save response to file for later tests
        Path path = Paths.get("target/test-data/auth-login-response.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, responseBody);
    }

    @Test
    public void register_withRealData_shouldReturnSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Test\",\"lastName\":\"Test\",\"login\":\"test.login@test.com\", \"password\":\"testPassword\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Save response to file for later tests
        Path path = Paths.get("target/test-data/auth-register-response.json");
        Files.createDirectories(path.getParent());
        Files.writeString(path, responseBody);
    }
}
