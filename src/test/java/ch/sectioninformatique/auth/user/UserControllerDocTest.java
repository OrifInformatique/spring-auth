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
         * Test for retrieving all users.
         * This test verifies that the list of all users is returned
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void all_withMockedService_generatesDoc() throws Exception {
                // Read saved user list response JSON
                Path responsePath = Paths.get("target/test-data/users-all-response.json");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-all-response.json. Run all_withRealData_shouldReturnSuccess first.");
                }
                String allUsersJson = Files.readString(responsePath);

                // Read saved token
                Path tokenPath = Paths.get("target/test-data/users-all-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-all-token.txt. Run all_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                // Parse user list from JSON
                ObjectMapper objectMapper = new ObjectMapper()
                                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                List<User> users = objectMapper.readValue(allUsersJson, new TypeReference<>() {
                });

                // Mock the userService to return the loaded users
                when(userService.allUsers()).thenReturn(users);

                // Mock authenticated principal
                UserDto userDto = UserDto.builder()
                                .id(100L)
                                .login("test.user@test.com")
                                .firstName("Test")
                                .lastName("User")
                                .mainRole("USER")
                                .permissions(new ArrayList<String>())
                                .build();

                when(authentication.getPrincipal()).thenReturn(userDto);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                // Perform request and generate REST Docs
                this.mockMvc.perform(get("/users/all")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andDo(document("users/all", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test for retrieving all users that is missing it's
         * Authorization header.
         * This test verifies that an unauthorized response is returned
         * when the Authorization header is missing,
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void all_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-all-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required all response data. Make sure UserControllerIntegrationTest ran first.");
                }

                meResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(meResponseJson);

                when(userService.allUsers()).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                this.mockMvc.perform(get("/users/all")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/all-missing-authorization", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test for retrieving all users with malformed
         * token.
         * This test verifies that an unauthorized response is returned
         * when the token is malformed,
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void all_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-all-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required all response data. Make sure UserControllerIntegrationTest ran first.");
                }

                meResponseJson = Files.readString(path);

                Path pathToken = Paths.get("target/test-data/users-all-token-malformed-token.txt");
                if (!Files.exists(pathToken)) {
                        throw new IllegalStateException(
                                        "Missing required token data. Make sure UserControllerIntegrationTest ran first.");
                }
                meToken = Files.readString(pathToken);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(meResponseJson);

                when(userService.allUsers()).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                this.mockMvc.perform(get("/users/all")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + meToken))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/all-malformed-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test for retrieving all users with expired
         * token.
         * This test verifies that an unauthorized response is returned
         * when the token is expired,
         * and generates API documentation using Spring REST Docs.
         * 
         * @throws Exception
         */
        @Test
        void all_withMockedService_generatesDoc_withExpiredToken() throws Exception {

                Path path = Paths.get("target/test-data/users-all-response-expired-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required all response data. Make sure UserControllerIntegrationTest ran first.");
                }

                meResponseJson = Files.readString(path);

                Path pathToken = Paths.get("target/test-data/users-all-token-expired-token.txt");
                if (!Files.exists(pathToken)) {
                        throw new IllegalStateException(
                                        "Missing required token data. Make sure UserControllerIntegrationTest ran first.");
                }
                meToken = Files.readString(pathToken);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(meResponseJson);

                when(userService.allUsers()).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                this.mockMvc.perform(get("/users/all")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + meToken))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/all-expired-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call,
         * mocks authenticated admin user, performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-promoteToManager-response.txt");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-promoteToManager-response.txt. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String promoteResponse = Files.readString(responsePath);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-promoteToManager-token.txt. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedUserDto = UserDto.builder()
                                .id(1L)
                                .login("test.user@test.com")
                                .firstName("Test")
                                .lastName("User")
                                .mainRole("MANAGER")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.promoteToManager(anyLong())).thenReturn(mockedUserDto);

                // Mock authenticated admin user with authority user:update
                UserDto adminDto = UserDto.builder()
                                .id(200L)
                                .login("test.admin@test.com")
                                .firstName("Admin")
                                .lastName("User")
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

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string(promoteResponse))
                                .andDo(document("users/promote-manager", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response file, mocks the
         * userService.promoteToManager call to throw unauthorized exception,
         * performs PUT request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/promote-manager-missing-authorization",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call to throw unauthorized exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager token data. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/promote-manager-malformed-token",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call to throw forbidden exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_asNonAdmin() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-non-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token-non-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager token data. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.FORBIDDEN));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andDo(document("users/promote-manager-non-admin",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call to throw not found exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_userNotFound() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-not-found.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }

                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token-user-not-found.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager token data. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new UserNotFoundException(exampleUserId.toString()));

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andDo(document("users/promote-manager-user-not-found",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call to throw conflict exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_userAlreadyManager() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-already-manager.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }

                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token-user-already-manager.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager token data. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new UserAlreadyManagerException(exampleUserId.toString()));

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andDo(document("users/promote-manager-user-already-manager",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToManager call to throw conflict exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToManager_withMockedService_generatesDoc_userAlreadyAdmin() throws Exception {

                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-already-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager response data. Make sure UserControllerIntegrationTest ran first.");
                }

                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToManager-token-user-already-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToManager token data. Run promoteToManager_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                when(userService.promoteToManager(anyLong())).thenThrow(
                                new UserAlreadyAdminException(exampleUserId.toString()));

                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andDo(document("users/promote-manager-user-already-admin",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeManagerRole call,
         * mocks authenticated admin user, performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeManagerRole_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-revokeManagerRole-response.txt");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-revokeManagerRole-response.txt. Run revokeManagerRole_withRealData_shouldReturnSuccess first.");
                }
                String revokeResponse = Files.readString(responsePath);

                Path tokenPath = Paths.get("target/test-data/users-revokeManagerRole-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-revokeManagerRole-token.txt. Run revokeManagerRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedManagerDto = UserDto.builder()
                                .id(1L)
                                .login("test.manager@test.com")
                                .firstName("Test")
                                .lastName("Manager")
                                .mainRole("USER")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.revokeManagerRole(anyLong())).thenReturn(mockedManagerDto);

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

                // Perform the PUT request to revoke the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string(revokeResponse))
                                .andDo(document("users/revoke-manager", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint using mocked service and
         * security.
         * This test loads saved response file, mocks the
         * userService.revokeManagerRole call to throw unauthorized exception,
         * performs PUT request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeManagerRole_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeManagerRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to revoke the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-manager")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/revoke-manager-missing-authorization",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeManagerRole call to throw unauthorized exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeManagerRole_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeManagerRole-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole token data. Run revokeManagerRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeManagerRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to revoke the user manager role
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/revoke-manager-malformed-token",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeManagerRole call to throw forbidden exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeManagerRole_withMockedService_generatesDoc_asNonAdmin() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-non-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeManagerRole-token-non-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole token data. Run revokeManagerRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeManagerRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.FORBIDDEN));

                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                // Perform the PUT request to revoke the user manager role
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andDo(document("users/revoke-manager-non-admin",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeManagerRole call to throw not found exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeManagerRole_withMockedService_generatesDoc_userNotFound() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-user-not-found.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole response data. Make sure UserControllerIntegrationTest ran first.");
                }

                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeManagerRole-token-user-not-found.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeManagerRole token data. Run revokeManagerRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                
                // Use an example userId - ideally read from your saved data or hardcoded if
                // stable
                Long exampleUserId = 100L;

                when(userService.revokeManagerRole(anyLong())).thenThrow(
                                new UserNotFoundException(exampleUserId.toString()));


                // Perform the PUT request to promote the user to manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-manager")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andDo(document("users/revoke-manager-user-not-found",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToAdmin call,
         * mocks authenticated admin user, performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-promoteToAdmin-response.txt");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-promoteToAdmin-response.txt. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String promoteResponse = Files.readString(responsePath);

                Path tokenPath = Paths.get("target/test-data/users-promoteToAdmin-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-promoteToAdmin-token.txt. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedManagerDto = UserDto.builder()
                                .id(1L)
                                .login("test.manager@test.com")
                                .firstName("Test")
                                .lastName("Manager")
                                .mainRole("ADMIN")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.promoteToAdmin(anyLong())).thenReturn(mockedManagerDto);

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

                // Perform the PUT request to promote the user to admin
                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string(promoteResponse))
                                .andDo(document("users/promote-admin", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response file, mocks the
         * userService.promoteToAdmin call to throw unauthorized exception,
         * performs PUT request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToAdmin(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/promote-admin-missing-authorization",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToAdmin call to throw unauthorized exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc_withMalformedToken() throws Exception {
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToAdmin-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin token data. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToAdmin(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/promote-admin-malformed-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToAdmin call to throw forbidden exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc_asNonAdmin() throws Exception {
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-non-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToAdmin-token-non-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin token data. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToAdmin(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.FORBIDDEN));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andDo(document("users/promote-admin-non-admin", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToAdmin call to throw not found exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc_userNotFound() throws Exception {
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-user-not-found.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToAdmin-token-user-not-found.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin token data. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToAdmin(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.NOT_FOUND));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andDo(document("users/promote-admin-user-not-found", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.promoteToAdmin call to throw conflict exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void promoteToAdmin_withMockedService_generatesDoc_userAlreadyAdmin() throws Exception {
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-user-already-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String promoteResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-promoteToAdmin-token-user-already-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required promoteToAdmin token data. Run promoteToAdmin_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(promoteResponseJson);

                when(userService.promoteToAdmin(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.CONFLICT));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/promote-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andDo(document("users/promote-admin-user-already-admin",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeAdminRole call,
         * mocks authenticated admin user, performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeAdminRole_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-revokeAdminRole-response.txt");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-revokeAdminRole-response.txt. Run revokeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String revokeResponse = Files.readString(responsePath);

                Path tokenPath = Paths.get("target/test-data/users-revokeAdminRole-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-revokeAdminRole-token.txt. Run revokeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedAdminDto = UserDto.builder()
                                .id(1L)
                                .login("test.admin2@test.com")
                                .firstName("Test2")
                                .lastName("Admin2")
                                .mainRole("USER")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.revokeAdminRole(anyLong())).thenReturn(mockedAdminDto);

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

                // Perform the PUT request to revoke the admin role into an user
                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string(revokeResponse))
                                .andDo(document("users/revoke-admin", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint using mocked service and
         * security.
         * This test loads saved response file, mocks the
         * userService.revokeAdminRole call to throw unauthorized exception,
         * performs PUT request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeAdminRole_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-admin")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/revoke-admin-missing-authorization",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeAdminRole call to throw unauthorized exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeAdminRole_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeAdminRole-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole token data. Run revokeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/revoke-admin-malformed-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeAdminRole call to throw forbidden exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeAdminRole_withMockedService_generatesDoc_asNonAdmin() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-non-admin.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeAdminRole-token-non-admin.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole token data. Run revokeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.FORBIDDEN));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andDo(document("users/revoke-admin-non-admin", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.revokeAdminRole call to throw not found exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void revokeAdminRole_withMockedService_generatesDoc_userNotFound() throws Exception {

                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-user-not-found.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String revokeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-revokeAdminRole-token-user-not-found.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required revokeAdminRole token data. Run revokeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(revokeResponseJson);

                when(userService.revokeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.NOT_FOUND));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/revoke-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andDo(document("users/revoke-admin-user-not-found", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.downgradeAdminRole call,
         * mocks authenticated admin user, performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void downgradeAdminRole_withMockedService_generatesDoc() throws Exception {
                // Paths to saved response and token files
                Path responsePath = Paths.get("target/test-data/users-downgradeAdminRole-response.txt");
                if (!Files.exists(responsePath)) {
                        throw new IllegalStateException(
                                        "Missing users-downgradeAdminRole-response.txt. Run downgradeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String downgradeResponse = Files.readString(responsePath);

                Path tokenPath = Paths.get("target/test-data/users-downgradeAdminRole-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing users-downgradeAdminRole-token.txt. Run downgradeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                UserDto mockedAdminDto = UserDto.builder()
                                .id(1L)
                                .login("test.admin2@test.com")
                                .firstName("Test2")
                                .lastName("Admin2")
                                .mainRole("MANAGER")
                                .permissions(new ArrayList<>())
                                .build();

                when(userService.downgradeAdminRole(anyLong())).thenReturn(mockedAdminDto);

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

                // Perform the PUT request to downgrade the admin role into an Manager
                this.mockMvc.perform(put("/users/" + exampleUserId + "/downgrade-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string(downgradeResponse))
                                .andDo(document("users/downgrade-admin", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint using mocked service and
         * security.
         * This test loads saved response file, mocks the
         * userService.downgradeAdminRole call to throw unauthorized exception,
         * performs PUT request without Authorization header,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void downgradeAdminRole_withMockedService_generatesDoc_missingAuthorizationHeader() throws Exception {

                Path path = Paths.get("target/test-data/users-downgradeAdminRole-response-missing-authorization.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required downgradeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String downgradeResponseJson = Files.readString(path);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(downgradeResponseJson);

                when(userService.downgradeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/downgrade-admin")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/downgrade-admin-missing-authorization",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint using mocked service and
         * security.
         * This test loads saved response and token files, mocks the
         * userService.downgradeAdminRole call to throw unauthorized exception,
         * performs PUT request,
         * verifies response, and generates API documentation using Spring REST Docs.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        void downgradeAdminRole_withMockedService_generatesDoc_withMalformedToken() throws Exception {

                Path path = Paths.get("target/test-data/users-downgradeAdminRole-response-malformed-token.json");
                if (!Files.exists(path)) {
                        throw new IllegalStateException(
                                        "Missing required downgradeAdminRole response data. Make sure UserControllerIntegrationTest ran first.");
                }
                String downgradeResponseJson = Files.readString(path);

                Path tokenPath = Paths.get("target/test-data/users-downgradeAdminRole-token-malformed-token.txt");
                if (!Files.exists(tokenPath)) {
                        throw new IllegalStateException(
                                        "Missing required downgradeAdminRole token data. Run downgradeAdminRole_withRealData_shouldReturnSuccess first.");
                }
                String token = Files.readString(tokenPath);

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(downgradeResponseJson);

                when(userService.downgradeAdminRole(anyLong())).thenThrow(
                                new CustomException(jsonNode.get("message").asText(), HttpStatus.UNAUTHORIZED));

                Long exampleUserId = 100L;

                this.mockMvc.perform(put("/users/" + exampleUserId + "/downgrade-admin")
                                .accept(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("users/downgrade-admin-malformed-token",
                                                preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

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
