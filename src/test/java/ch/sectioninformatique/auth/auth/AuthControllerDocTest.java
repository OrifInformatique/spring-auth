package ch.sectioninformatique.auth.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Documentation tests for AuthController.
 * These tests generate API documentation snippets using Spring REST Docs.
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

        /** Mocked UserService for simulating user-related operations. */
        @MockBean
        private UserService userService;

        /** Mocked UserAuthenticationProvider for simulating authentication operations. */
        @MockBean
        private UserAuthenticationProvider userAuthenticationProvider;

        /**
         * Test the /auth/login endpoint with mocked services to generate documentation.
         * This test stubs the UserService and UserAuthenticationProvider to return
         * predefined data, performs a login request, and generates API documentation.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_withMockedService_generatesDoc() throws Exception {
                Path path = Paths.get("target/test-data/auth-login-response.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required login response data. Make sure AuthControllerIntegrationTest ran first.");
                }
                loginResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(loginResponseJson);

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
                when(userService.login(any())).thenReturn(userDto);

                when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());
                when(userAuthenticationProvider.createRefreshToken(any())).thenReturn(userDto.getRefreshToken());

                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"login\":\"super.admin@test.com\", \"password\":\"ReallySecure123@PassWordBecauseIWantToBeSuperSafe\"}"))
                                .andExpect(status().isOk())
                                .andDo(document("auth/login", preprocessResponse(prettyPrint())));
        }

        @Test
        public void register_withMockedService_generatesDoc() throws Exception {
                Path path = Paths.get("target/test-data/auth-register-response.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required register response data. Make sure AuthControllerIntegrationTest ran first.");
                }
                registerResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(registerResponseJson);

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
                when(userService.register(any())).thenReturn(userDto);

                when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());
                when(userAuthenticationProvider.createRefreshToken(any())).thenReturn(userDto.getRefreshToken());

                mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"Test\",\"lastName\":\"Test\",\"login\":\"test.login@test.com\", \"password\":\"testPassword\"}"))
                                .andExpect(status().isCreated())
                                .andDo(document("auth/register", preprocessResponse(prettyPrint())));
        }
}