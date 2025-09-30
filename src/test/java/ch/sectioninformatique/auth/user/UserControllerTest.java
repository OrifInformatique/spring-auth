package ch.sectioninformatique.auth.user;

import org.mockito.Mockito;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.sectioninformatique.auth.security.RoleRepository;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

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



@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = UserController.class,
            excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserAuthenticationProvider userAuthenticationProvider;

    @Test
    void authenticatedUser_ReturnsCurrentUser_Doc() throws Exception {

        UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "USER", null);

        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        this.mockMvc.perform(get("/users/me")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("users/me", preprocessResponse(prettyPrint())));
    }

    @Test
    void allUsers_ReturnsListOfUsers_Doc() throws Exception {
        // Arrange
        List<User> users = Arrays.asList(
            new User(1L, "John", "Doe", "john@test.com", "pass", null, null, null),
            new User(2L, "Jane", "Smith", "jane@test.com", "pass", null, null, null)
        );
        Mockito.when(userService.allUsers()).thenReturn(users);

        this.mockMvc.perform(get("/users/all")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("users/all", preprocessResponse(prettyPrint())));
    }
}
