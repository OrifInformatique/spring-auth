package ch.sectioninformatique.auth.user;

import org.mockito.Mockito;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.sectioninformatique.auth.security.RoleRepository;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Test class for {@link UserController}.
 * This class uses MockMvc to perform HTTP requests and validate responses.
 * It includes tests for:
 * - Retrieving the authenticated user's information
 * - Listing all users
 * - Promoting a user to manager role
 * - Revoking a user's manager role
 */
@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class })
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
class UserControllerDocTest {

        /** MockMvc for performing HTTP requests in tests */
        @Autowired
        private MockMvc mockMvc;

        /** Mocked UserService for simulating user-related operations */
        @MockBean
        private UserService userService;

        /** Mocked UserMapper for simulating user-related mapping operations */
        @MockBean
        private UserMapper userMapper;

        /** Mocked RoleRepository for simulating role-related database operations */
        @MockBean
        private RoleRepository roleRepository;

        /** Mocked UserRepository for simulating user-related database operations */
        @MockBean
        private UserRepository userRepository;

        /**
         * Mocked UserAuthenticationProvider for simulating authentication operations
         */
        @MockBean
        private UserAuthenticationProvider userAuthenticationProvider;

        @MockBean
        private Authentication authentication;

        @MockBean
        private SecurityContext securityContext;

        private static String meResponseJson;
        private static String meToken;

        /**
         * Test for retrieving the authenticated user's information.
         * This test verifies that the correct user information is returned
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void me_withMockedService_generatesDoc() throws Exception {

                Path path = Paths.get("target/test-data/users-me-response.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required me response data. Make sure UserControllerIntegrationTest ran first.");
                }
                meResponseJson = Files.readString(path);

                Path pathToken = Paths.get("target/test-data/users-me-token.txt");
                if (!Files.exists(pathToken)) {
                        throw new IllegalStateException(
                                        "Missing required token data. Make sure UserControllerIntegrationTest ran first.");
                }
                meToken = Files.readString(pathToken);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(meResponseJson);

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

                when(authentication.getPrincipal()).thenReturn(userDto);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                this.mockMvc.perform(get("/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + meToken))
                                .andExpect(status().isOk())
                                .andDo(document("users/me", preprocessResponse(prettyPrint())));
        }

        /*
         * @Test
         * void authenticatedUser_Unauthenticated_ReturnsUnauthorized() throws Exception
         * {
         * this.mockMvc.perform(get("/users/me")
         * .accept(MediaType.APPLICATION_JSON))
         * .andExpect(status().isUnauthorized())
         * .andDo(document("users/me/401", preprocessResponse(prettyPrint())));
         * }
         */

        /**
         * Test for retrieving all users.
         * This test verifies that a list of users is returned
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void allUsers_ReturnsListOfUsers_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);
                // Arrange
                List<User> users = Arrays.asList(
                                new User(1L, "John", "Doe", "john@test.com", "pass", null, null, null),
                                new User(2L, "Jane", "Smith", "jane@test.com", "pass", null, null, null));
                Mockito.when(userService.allUsers()).thenReturn(users);

                // Act & Assert
                this.mockMvc.perform(get("/users/all")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].firstName").value("John"))
                                .andExpect(jsonPath("$[0].lastName").value("Doe"))
                                .andExpect(jsonPath("$[0].login").value("john@test.com"))
                                .andExpect(jsonPath("$[1].id").value(2))
                                .andExpect(jsonPath("$[1].firstName").value("Jane"))
                                .andExpect(jsonPath("$[1].lastName").value("Smith"))
                                .andExpect(jsonPath("$[1].login").value("jane@test.com"))
                                .andDo(document("users/all", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for promoting a user to manager role.
         * This test verifies that the user is successfully promoted
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void promoteToManager_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "MANAGER", null);
                when(userService.promoteToManager(userId)).thenReturn(expectedDto);

                // Act & Assert
                this.mockMvc.perform(put("/users/{userId}/promote-manager", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("User promoted to manager successfully"))
                                .andDo(document("users/promote-manager", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for revoking a user's manager role.
         * This test verifies that the user is successfully revoked
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void revokeManagerRole_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "USER", null);

                when(userService.revokeManagerRole(userId)).thenReturn(expectedDto);

                this.mockMvc.perform(put("/users/{userId}/revoke-manager", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Manager role revoked successfully"))
                                .andDo(document("users/revoke-manager", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for promoting a user or manager to admin role.
         * This test verifies that the user is successfully promoted
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void promoteToAdmin_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "ADMIN", null);

                when(userService.promoteToAdmin(userId)).thenReturn(expectedDto);

                this.mockMvc.perform(put("/users/{userId}/promote-admin", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role assigned successfully"))
                                .andDo(document("users/promote-admin", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for revoking a user's admin role.
         * This test verifies that the user is successfully revoked
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void revokeAdminRole_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "USER", null);

                when(userService.revokeAdminRole(userId)).thenReturn(expectedDto);

                this.mockMvc.perform(put("/users/{userId}/revoke-admin", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role revoked successfully"))
                                .andDo(document("users/revoke-admin", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for downgrading an admin to manager role.
         * This test verifies that the admin is successfully downgraded
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void downgradeAdminRole_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "MANAGER", null);

                when(userService.downgradeAdminRole(userId)).thenReturn(expectedDto);

                this.mockMvc.perform(put("/users/{userId}/downgrade-admin", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role downgraded successfully"))
                                .andDo(document("users/downgrade-admin", preprocessResponse(prettyPrint())));
        }

        /**
         * Test for deleting a user.
         * This test verifies that the user is successfully deleted
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void deleteUser_Successful_Doc() throws Exception {
                UserDto mockUser = new UserDto(1L, "John", "Doe", "john@test.com", null, null, "ADMIN", null);

                Authentication authentication = Mockito.mock(Authentication.class);
                Mockito.when(authentication.getPrincipal()).thenReturn(mockUser);

                Mockito.when(authentication.isAuthenticated()).thenReturn(true);

                SecurityContext securityContext = Mockito.mock(SecurityContext.class);
                Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);

                SecurityContextHolder.setContext(securityContext);

                // Arrange
                Long userId = 2L;
                UserDto expectedDto = new UserDto(2L, "Jane", "Smith", "jane@test.com", null, null, "MANAGER", null);

                when(userService.deleteUser(userId)).thenReturn(expectedDto);

                this.mockMvc.perform(delete("/users/{userId}", userId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().string("User deleted successfully"))
                                .andDo(document("users/delete", preprocessResponse(prettyPrint())));
        }

}
