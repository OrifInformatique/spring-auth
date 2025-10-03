package ch.sectioninformatique.auth.auth;

import org.mockito.Mockito;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Test class for {@link AuthController}.
 * This class uses MockMvc to perform HTTP requests and validate responses.
 * It includes tests for:
 * - User login
 * - User registration
 * - Token refresh
 */
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class })
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
public class AuthControllerTest {

    /** MockMvc for performing HTTP requests in tests */
    @Autowired
    private MockMvc mockMvc;

    /** Mocked UserService for simulating user-related operations */
    @MockBean
    private UserService userService;

    /**
     * Mocked UserAuthenticationProvider for simulating authentication operations
     */
    @MockBean
    private UserAuthenticationProvider userAuthenticationProvider;

    /** Mocked UserDto for simulating user data */
    @MockBean
    private UserDto userDto;

    /**
     * Test for the login endpoint.
     * This test verifies that a user can successfully log in with valid
     * credentials.
     * It checks that the response status is OK and documents the API using Spring
     * REST Docs.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    void login_Success_Doc() throws Exception {
        String json = """
                {
                  "login": "john@test.com",
                  "password": "secret"
                }
                """;

        UserDto mockUser = UserDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .login("john@test.com")
                .mainRole("USER")
                .build();

        // Arrange
        Mockito.when(userService.login(Mockito.any())).thenReturn(mockUser);
        Mockito.when(userAuthenticationProvider.createToken(Mockito.any())).thenReturn("mocked-jwt-token");
        Mockito.when(userAuthenticationProvider.createRefreshToken(Mockito.any())).thenReturn("mocked-refresh-token");

        // Act & Assert
        this.mockMvc.perform(post("/auth/login")
                .content(json) // ✅ Add the request body
                .contentType(MediaType.APPLICATION_JSON) // ✅ Set correct content type
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("auth/login", preprocessResponse(prettyPrint())));

    }

    /**
     * Test for the register endpoint.
     * This test verifies that a user can successfully register with valid
     * information.
     * It checks that the response status is CREATED and documents the API using Spring
     * REST Docs.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    void register_Success_Doc() throws Exception {
        String json = """
                {
                  "firstName": "John",
                  "lastName": "Doe",
                  "login": "john@test.com",
                  "password": "secret"
                }
                """;

        UserDto mockUser = UserDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .login("john@test.com")
                .mainRole("USER")
                .build();

        // Arrange
        Mockito.when(userService.register(Mockito.any())).thenReturn(mockUser);
        Mockito.when(userAuthenticationProvider.createToken(Mockito.any())).thenReturn("mocked-jwt-token");
        Mockito.when(userAuthenticationProvider.createRefreshToken(Mockito.any())).thenReturn("mocked-refresh-token");

        // Act & Assert
        this.mockMvc.perform(post("/auth/register")
                .content(json) // ✅ Add the request body
                .contentType(MediaType.APPLICATION_JSON) // ✅ Set correct content type
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(document("auth/register", preprocessResponse(prettyPrint())));

    }

    /**
     * Test for the refresh endpoint.
     * This test verifies that a user can successfully refresh their JWT token.
     * It checks that the response status is OK and documents the API using Spring
     * REST Docs.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    void refresh_Success_Doc() throws Exception {
        UserDto mockUser = UserDto.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .login("john@test.com")
                .refreshToken("mocked-refresh-token")
                .mainRole("USER")
                .build();

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

        // Arrange
        Mockito.when(userService.refreshLogin(Mockito.any())).thenReturn(mockUser);
        Mockito.when(userAuthenticationProvider.createToken(Mockito.any())).thenReturn("mocked-new-jwt-token");

        // Act & Assert
        this.mockMvc.perform(get("/auth/refresh")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("auth/refresh", preprocessResponse(prettyPrint())));

    }
}
