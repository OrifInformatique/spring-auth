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

@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerDocTest {

    @Autowired
    private MockMvc mockMvc;

    private static String loginResponseJson;

    @MockBean
    private UserService userService;

    @MockBean
    private UserAuthenticationProvider userAuthenticationProvider;

    @BeforeAll
    static void loadResponse() throws IOException {
        Path path = Paths.get("target/test-data/auth-login-response.json");
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                    "Missing required login response data. Make sure AuthControllerIntegrationTest ran first.");
        }
        loginResponseJson = Files.readString(path);
    }

    @Test
    public void login_withMockedService_generatesDoc() throws Exception {
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

        // You might stub the token provider to avoid NPE if it’s called:
        // (optional if your controller does not call it in the test path)
        when(userAuthenticationProvider.createToken(any())).thenReturn(userDto.getToken());
        when(userAuthenticationProvider.createRefreshToken(any())).thenReturn(userDto.getRefreshToken());

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"login\":\"super.admin@test.com\", \"password\":\"ReallySecure123@PassWordBecauseIWantToBeSuperSafe\"}"))
                .andExpect(status().isOk())
                .andDo(document("auth/login", preprocessResponse(prettyPrint())));
    }
}