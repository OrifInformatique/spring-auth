package ch.sectioninformatique.auth.user;

import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.sectioninformatique.auth.app.exceptions.CustomException;
import ch.sectioninformatique.auth.app.exceptions.UserAlreadyAdminException;
import ch.sectioninformatique.auth.app.exceptions.UserAlreadyManagerException;
import ch.sectioninformatique.auth.app.exceptions.UserNotFoundException;
import ch.sectioninformatique.auth.security.RoleRepository;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyLong;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;





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
         * Test the /users/{userId} DELETE endpoint using mocked service and security.
         * This test loads saved response and token files, mocks the
         * userService.deleteUser
         * call,
         * mocks authenticated admin user, performs DELETE request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void deleteUser_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-deleteUser-response.json");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-deleteUser-response.txt. Run deleteUser_withRealData_shouldReturnSuccess first.");
                }
                String deleteResponse = Files.readString(responsePath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(deleteResponse);

                Path tokenPath = Paths.get("target/test-data/users-deleteUser-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-deleteUser-token.txt. Run deleteUser_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedUserDto = UserDto.builder()
                                .id(1L)
                                .login("test.user@test.com")
                                .firstName("Test")
                                .lastName("User")
                                .mainRole("USER")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.deleteUser(anyLong())).thenReturn(mockedUserDto);

                // Mock authenticated admin user with authority user:update
                UserDto adminDto = UserDto.builder()
                                .id(200L)
                                .login("test.admin@test.com")
                                .firstName("Admin")
                                .lastName("Test")
                                .mainRole("ADMIN")
                                .permissions(new ArrayList<>())
                                .build();

                when(authentication.getPrincipal()).thenReturn(adminDto);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the DELETE request to delete a user
                this.mockMvc.perform(delete("/users/" + exampleUserId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value(jsonNode.get("message").asText()))
                                .andDo(document("users/delete", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId} DELETE endpoint using mocked service and security.
         * This test loads saved response file, mocks the
         * userService.deleteUser call to throw unauthorized exception,
         * performs DELETE request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void deleteUser_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-deleteUser-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required deleteUser response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String deleteResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(deleteResponseJson);

                when(userService.deleteUser(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(delete("/users/" + exampleUserId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/delete-missing-authorization", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId} DELETE endpoint using mocked service and security.
         * This test loads saved response and token files, mocks the
         * userService.deleteUser call to throw unauthorized exception,
         * performs DELETE request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void deleteUser_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-deleteUser-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required deleteUser response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String deleteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-deleteUser-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required deleteUser token data. Run deleteUser_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(deleteResponseJson);

                when(userService.deleteUser(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(delete("/users/" + exampleUserId)
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/delete-malformed-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

}
