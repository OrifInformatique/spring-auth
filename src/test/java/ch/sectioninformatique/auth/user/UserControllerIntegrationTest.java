package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Integration tests for the UserController.
 * 
 * These tests cover various scenarios for the user management endpoints,
 * including authentication, authorization, and role management.
 * 
 * The tests use MockMvc to perform HTTP requests and verify responses.
 */
@Tag("integration")
@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class UserControllerIntegrationTest {

        /**
         * Helper method for performing and documenting HTTP requests in tests.
         * This reduces repetition by centralizing the request execution and REST Docs
         * generation.
         *
         * @param requestTypeString HTTP method (GET, POST, PUT, etc.)
         * @param endpoint          API endpoint to call
         * @param token             Optional JWT token for authentication
         * @param contentType       Content type for the request
         * @param expectedStatus    Expected HTTP status code (e.g. 200)
         * @param docsFileName      Name for the generated REST Docs snippet
         * @param script            Optional lambda to perform additional assertions
         * 
         * @throws Exception
         */
        private void performRequest(
                        String requestTypeString,
                        String endpoint,
                        String content,
                        String token,
                        MediaType contentType,
                        int expectedStatus,
                        String docsFileName,
                        Consumer<ResultActions> script) throws Exception {

                var requestType = get(endpoint);

                if (requestTypeString.equals("GET")) {
                        requestType = get(endpoint);
                } else if (requestTypeString.equals("POST")) {
                        requestType = post(endpoint);
                } else if (requestTypeString.equals("PUT")) {
                        requestType = put(endpoint);
                } else if (requestTypeString.equals("DELETE")) {
                        requestType = delete(endpoint);
                } else {
                        throw new IllegalArgumentException("Unsupported request type: " + requestTypeString);
                }

                // Set content only if it's not null
                if (content != null) {
                        requestType.content(content);
                }

                // Set Authorization header only if token is provided
                if (token != null) {
                        requestType.header("Authorization", "Bearer " + token);
                }

                // Set content type
                requestType.contentType(contentType);

                // Perform request
                var request = mockMvc.perform(requestType)
                                .andExpect(status().is(expectedStatus));

                // Execute any additional assertions provided in the lambda
                if (script != null) {
                        script.accept(request);
                }

                // Generate a REST Docs snippet for the request/response pair
                request.andDo(document("users/" + docsFileName, preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));

        }

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** UserAuthenticationProvider instance for managing user authentication. */
        @Autowired
        private UserAuthenticationProvider userAuthenticationProvider;

        /** UserService instance for user-related operations. */
        @Autowired
        private UserService userService;

        /**
         * Test: GET /users/me
         *
         * Mock a user request for it's own informations.
         */
        @Test
        @Transactional
        public void me_withRealData_shouldReturnSuccess() throws Exception {

                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "GET",
                                "/users/me",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "me",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.id").isNotEmpty())
                                                                .andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("User"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.user@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                                                .andExpect(jsonPath("$.token").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me
         *
         * Mock a user request for it's own informations without authorisation header.
         */
        @Test
        @Transactional
        public void me_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/users/me",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "me-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me
         *
         * Mock a user request for it's own informations with a malformed token.
         */
        @Test
        @Transactional
        public void me_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";

                performRequest(
                                "GET",
                                "/users/me",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "me-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me
         *
         * Mock a user request for it's own informations with a expired token.
         */
        @Test
        @Transactional
        public void me_withExpiredToken_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto, Date.from(
                                Instant.now().minus(2, ChronoUnit.HOURS)));

                performRequest(
                                "GET",
                                "/users/me",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "me-expired-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all
         *
         * Mock a user request for all users.
         */
        @Test
        @Transactional
        public void all_withRealData_shouldReturnSuccess() throws Exception {

                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "GET",
                                "/users/all",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "all",
                                request -> {
                                        try {
                                                MvcResult result = request.andReturn();
                                                String responseBody = result.getResponse().getContentAsString();

                                                // Parse the JSON response
                                                ObjectMapper mapper = new ObjectMapper();
                                                List<Map<String, Object>> users = mapper.readValue(responseBody,
                                                                new TypeReference<List<Map<String, Object>>>() {
                                                                });

                                                // Assert the number of users is 4 (from the seeder)
                                                assertEquals(4, users.size(), "Should return 4 users");

                                                // Assert specific users are present
                                                List<String> expectedLogins = List.of(
                                                                "test.user@test.com",
                                                                "test.manager@test.com",
                                                                "test.admin@test.com",
                                                                "test.admin2@test.com");

                                                List<String> returnedLogins = users.stream()
                                                                .map(user -> (String) user.get("login"))
                                                                .collect(Collectors.toList());

                                                assertTrue(returnedLogins.containsAll(expectedLogins),
                                                                "Returned users should include all seeded logins");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all
         *
         * Mock a user request for all users without header.
         */
        @Test
        @Transactional
        public void all_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/users/all",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "all-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all
         *
         * Mock a user request for all users with a malformed token.
         */
        @Test
        @Transactional
        public void all_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";

                performRequest(
                                "GET",
                                "/users/all",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "all-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all
         *
         * Mock a user request for all users with a expired token.
         */
        @Test
        @Transactional
        public void all_withExpiredToken_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto, Date.from(
                                Instant.now().minus(2, ChronoUnit.HOURS)));

                performRequest(
                                "GET",
                                "/users/all",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "all-expired-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager
         *
         * Mock a request to promote a user into a manager
         */
        @Test
        @Transactional
        public void promoteToManager_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + userDto.getId() + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "all",
                                request -> {
                                        try {
                                                request.andExpect(content()
                                                                .string("User promoted to manager successfully"));
                                                MvcResult result = request.andReturn();

                                                // Assert: fetch user again and verify role changed to MANAGER
                                                UserDto updatedUser = userService.findByLogin("test.user@test.com");
                                                assertNotNull(updatedUser.getMainRole(),
                                                                "User role should not be null after promotion");
                                                assertEquals("MANAGER", updatedUser.getMainRole(),
                                                                "User role should be MANAGER after promotion");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with missing
         * authorization header.
         * This test retrieves a known user and performs a PUT request to
         * promote the user to manager
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + userDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with a malformed token.
         * This test retrieves a known user and performs a PUT request to
         * promote the user to manager
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto userDto = userService.findByLogin("test.user@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + userDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with real data
         * as a non-admin user.
         * This test retrieves a known user, generates an authentication token
         * for a non-admin user,
         * and performs a PUT request to promote the user to manager.
         * It verifies that the response status is Forbidden and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(put("/users/" + userDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-non-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token-non-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with a non-existing user.
         * This test retrieves a known admin user, generates an authentication token
         * for the admin,
         * and performs a PUT request to promote a non-existing user to manager.
         * It verifies that the response status is Not Found and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                MvcResult result = mockMvc.perform(put("/users/" + fakeUserId + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-not-found.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token-user-not-found.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with a user
         * who is already a manager.
         * This test retrieves a known manager user and an admin user,
         * generates an authentication token for the admin,
         * and performs a PUT request to promote the manager to manager again.
         * It verifies that the response status is Conflict and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_userAlreadyManager_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-already-manager.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token-user-already-manager.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with a user
         * who is already an admin.
         * This test retrieves a known admin user,
         * generates an authentication token for the admin,
         * and performs a PUT request to promote the admin to manager.
         * It verifies that the response status is Conflict and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_userAlreadyAdmin_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + adminDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response-user-already-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token-user-already-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint with real data.
         * This test retrieves a known manager user and an admin user,
         * generates an authentication token for the admin,
         * and performs a PUT request to revoke the manager role from the user.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeManagerRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Manager role revoked successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: fetch manager again and verify role changed to USER
                UserDto updatedManager = userService.findByLogin("test.manager@test.com");
                assertNotNull(updatedManager.getMainRole(), "Manager role should not be null after promotion");
                assertEquals("USER", updatedManager.getMainRole(),
                                "Manager role should be USER after promotion");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeManagerRole-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeManagerRole-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint with missing
         * authorization header.
         * This test retrieves a known manager user and performs a PUT request to
         * revoke the manager role
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeManagerRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint with a malformed token.
         * This test retrieves a known manager user and performs a PUT request to
         * revoke the manager role
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeManagerRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeManagerRole-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint with real data
         * as a non-admin user.
         * This test retrieves a known manager user, generates an authentication
         * token for a non-admin user,
         * and performs a PUT request to revoke the manager role from the user.
         * It verifies that the response status is Forbidden and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeManagerRole_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-non-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeManagerRole-token-non-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-manager endpoint with a non-existing user.
         * This test retrieves a known admin user, generates an authentication token
         * for the admin,
         * and performs a PUT request to revoke the manager role from a non-existing
         * user.
         * It verifies that the response status is Not Found and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeManagerRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                MvcResult result = mockMvc.perform(put("/users/" + fakeUserId + "/revoke-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeManagerRole-response-user-not-found.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeManagerRole-token-user-not-found.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with real data.
         * This test retrieves a known manager user and an admin user,
         * generates an authentication token for the admin,
         * and performs a PUT request to promote the manager to admin.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_withRealData_shouldReturnSuccess() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role assigned successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: fetch manager again and verify role changed to ADMIN
                UserDto updatedManager = userService.findByLogin("test.manager@test.com");
                assertNotNull(updatedManager.getMainRole(), "Manager role should not be null after promotion");
                assertEquals("ADMIN", updatedManager.getMainRole(),
                                "Manager role should be ADMIN after promotion");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToAdmin-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with missing
         * authorization header.
         * This test retrieves a known manager user and performs a PUT request to
         * promote the manager to admin
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with a malformed token.
         * This test retrieves a known manager user and performs a PUT request to
         * promote the manager to admin
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                Path pathToken = Paths.get("target/test-data/users-promoteToAdmin-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with real data
         * as a non-admin user.
         * This test retrieves a known manager user, generates an authentication
         * token for a non-admin user,
         * and performs a PUT request to promote the manager to admin.
         * It verifies that the response status is Forbidden and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(put("/users/" + managerDto.getId().toString() + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-non-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                Path pathToken = Paths.get("target/test-data/users-promoteToAdmin-token-non-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with a non-existing user.
         * This test retrieves a known admin user, generates an authentication token
         * for the admin,
         * and performs a PUT request to promote a non-existing user to admin.
         * It verifies that the response status is Not Found and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                MvcResult result = mockMvc.perform(put("/users/" + fakeUserId + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-user-not-found.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                Path pathToken = Paths.get("target/test-data/users-promoteToAdmin-token-user-not-found.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-admin endpoint with a user
         * who is already an admin.
         * This test retrieves a known admin user,
         * generates an authentication token for the admin,
         * and performs a PUT request to promote the admin to admin.
         * It verifies that the response status is Conflict and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToAdmin_userAlreadyAdmin_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + adminDto.getId().toString() + "/promote-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                Path path = Paths.get("target/test-data/users-promoteToAdmin-response-user-already-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                Path pathToken = Paths.get("target/test-data/users-promoteToAdmin-token-user-already-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint with real data.
         * This test retrieves a known admin user and another admin user,
         * generates an authentication token for the first admin,
         * and performs a PUT request to revoke the admin role from the second admin.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeAdminRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToRevokeDto.getId().toString() + "/revoke-admin")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role revoked successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: fetch admin again and verify role changed to USER
                UserDto updatedAdmin = userService.findByLogin("test.admin2@test.com");
                assertNotNull(updatedAdmin.getMainRole(), "Admin role should not be null after promotion");
                assertEquals("USER", updatedAdmin.getMainRole(),
                                "Admin role should be USER after promotion");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeAdminRole-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint with missing
         * authorization header.
         * This test retrieves a known admin user and performs a PUT request to
         * revoke the admin role
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeAdminRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToRevokeDto.getId().toString() + "/revoke-admin")
                                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint with a malformed token.
         * This test retrieves a known admin user and performs a PUT request to
         * revoke the admin role
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeAdminRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToRevokeDto.getId().toString() + "/revoke-admin")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeAdminRole-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint with real data
         * as a non-admin user.
         * This test retrieves a known admin user and generates an authentication
         * token for a non-admin user,
         * and performs a PUT request to revoke the admin role from the user.
         * It verifies that the response status is Forbidden and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeAdminRole_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToRevokeDto.getId().toString() + "/revoke-admin")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-non-admin.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeAdminRole-token-non-admin.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/revoke-admin endpoint with a non-existing user.
         * This test retrieves a known admin user, generates an authentication token
         * for the admin,
         * and performs a PUT request to revoke the admin role from a non-existing user.
         * It verifies that the response status is Not Found and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void revokeAdminRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                MvcResult result = mockMvc.perform(put("/users/" + fakeUserId + "/revoke-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response-user-not-found.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeAdminRole-token-user-not-found.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint with real data.
         * This test retrieves a known admin user and another admin user,
         * generates an authentication token for the first admin,
         * and performs a PUT request to downgrade the second admin to manager.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void downgradeAdminRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToDowngradeDto.getId().toString() + "/downgrade-admin")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role downgraded successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-downgradeAdminRole-response.txt");

                // Assert: fetch admin again and verify role changed to MANAGER
                UserDto updatedAdmin = userService.findByLogin("test.admin2@test.com");
                assertNotNull(updatedAdmin.getMainRole(), "Admin role should not be null after promotion");
                assertEquals("MANAGER", updatedAdmin.getMainRole(),
                                "Admin role should be MANAGER after promotion");

                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-downgradeAdminRole-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint with missing
         * authorization header.
         * This test retrieves a known admin user and performs a PUT request to
         * downgrade the admin role
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void downgradeAdminRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToDowngradeDto.getId().toString() + "/downgrade-admin")
                                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-downgradeAdminRole-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId}/downgrade-admin endpoint with a malformed token.
         * This test retrieves a known admin user and performs a PUT request to
         * downgrade the admin role
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void downgradeAdminRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                MvcResult result = mockMvc
                                .perform(put("/users/" + adminToDowngradeDto.getId().toString() + "/downgrade-admin")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-downgradeAdminRole-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-downgradeAdminRole-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId} DELETE endpoint with real data.
         * This test retrieves a known user and an admin user,
         * generates an authentication token for the admin,
         * and performs a DELETE request to delete the user.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void deleteUser_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(delete("/users/" + userDto.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                                .andExpect(jsonPath("$.deletedUserLogin").value("test.user@test.com"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                UserDto deletedUser = userService.findByLogin("test.user@test.com");
                assertTrue(deletedUser.isDeleted(), "User should be marked as deleted");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-deleteUser-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-deleteUser-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId} DELETE endpoint with missing authorization
         * header.
         * This test retrieves a known user and performs a DELETE request to delete
         * the user
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void deleteUser_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                MvcResult result = mockMvc.perform(delete("/users/" + userDto.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-deleteUser-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/{userId} DELETE endpoint with a malformed token.
         * This test retrieves a known user and performs a DELETE request to delete
         * the user
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void deleteUser_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto userDto = userService.findByLogin("test.user@test.com");

                MvcResult result = mockMvc.perform(delete("/users/" + userDto.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-deleteUser-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-deleteUser-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }
}
