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
                                "promote-manager",
                                request -> {
                                        try {
                                                request.andExpect(content()
                                                                .string("User promoted to manager successfully"));

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
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user into a manager with missing authorization
         * header.
         */
        @Test
        @Transactional
        public void promoteToManager_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                performRequest(
                                "PUT",
                                "/users/" + userDto.getId() + "/promote-manager",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "promote-manager-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user into a manager with a malformed token.
         */
        @Test
        @Transactional
        public void promoteToManager_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";

                UserDto userDto = userService.findByLogin("test.user@test.com");

                performRequest(
                                "PUT",
                                "/users/" + userDto.getId() + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "promote-manager-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user into a manager as a non-admin user.
         */
        @Test
        @Transactional
        public void promoteToManager_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/users/" + userDto.getId() + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                403,
                                "promote-manager-non-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user into a managerwith a non-existing user.
         */
        @Test
        @Transactional
        public void promoteToManager_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                performRequest(
                                "PUT",
                                "/users/" + fakeUserId + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "promote-manager-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user who is already a manager.
         */
        @Test
        @Transactional
        public void promoteToManager_userAlreadyManager_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);
                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                409,
                                "promote-manager-user-already-manager",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manage.
         *
         * Mock a request to promote a user who is already an admin.
         */
        @Test
        @Transactional
        public void promoteToManager_userAlreadyAdmin_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);
                performRequest(
                                "PUT",
                                "/users/" + adminDto.getId() + "/promote-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                409,
                                "promote-manager-user-already-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager
         *
         * Mock a request to revoke a manager into a user
         */
        @Test
        @Transactional
        public void revokeManagerRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/revoke-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "revoke-manager",
                                request -> {
                                        try {
                                                request.andExpect(
                                                                content().string("Manager role revoked successfully"));

                                                // Assert: fetch manager again and verify role changed to USER
                                                UserDto updatedManager = userService
                                                                .findByLogin("test.manager@test.com");
                                                assertNotNull(updatedManager.getMainRole(),
                                                                "Manager role should not be null after promotion");
                                                assertEquals("USER", updatedManager.getMainRole(),
                                                                "Manager role should be USER after promotion");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager
         *
         * Mock a request to revoke a manager into a user with missing header
         */
        @Test
        @Transactional
        public void revokeManagerRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/revoke-manager",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "revoke-manager-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager
         *
         * Mock a request to revoke a manager into a user with malformed token
         */
        @Test
        @Transactional
        public void revokeManagerRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/revoke-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "revoke-manager-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager
         *
         * Mock a request to revoke a manager into a user as a non-admin user
         */
        @Test
        @Transactional
        public void revokeManagerRole_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/revoke-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                403,
                                "revoke-manager-non-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager
         *
         * Mock a request to revoke a manager into a user with a non-existing user
         */
        @Test
        @Transactional
        public void revokeManagerRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                performRequest(
                                "PUT",
                                "/users/" + fakeUserId + "/revoke-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "revoke-manager-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin
         */
        @Test
        @Transactional
        public void promoteToAdmin_withRealData_shouldReturnSuccess() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/promote-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "promote-admin",
                                request -> {
                                        try {
                                                request.andExpect(content().string("Admin role assigned successfully"));

                                                // Assert: fetch manager again and verify role changed to ADMIN
                                                UserDto updatedManager = userService
                                                                .findByLogin("test.manager@test.com");
                                                assertNotNull(updatedManager.getMainRole(),
                                                                "Manager role should not be null after promotion");
                                                assertEquals("ADMIN", updatedManager.getMainRole(),
                                                                "Manager role should be ADMIN after promotion");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin with missing
         * authorization header
         */
        @Test
        @Transactional
        public void promoteToAdmin_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/promote-admin",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "promote-admin-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin with a malformed
         * token
         */
        @Test
        @Transactional
        public void promoteToAdmin_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/promote-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "promote-admin-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin as a non-admin
         */
        @Test
        @Transactional
        public void promoteToAdmin_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto managerDto = userService.findByLogin("test.manager@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/users/" + managerDto.getId() + "/promote-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                403,
                                "promote-admin-non-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin with a non-existing
         * user
         */
        @Test
        @Transactional
        public void promoteToAdmin_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                performRequest(
                                "PUT",
                                "/users/" + fakeUserId + "/promote-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "promote-admin-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin
         *
         * Mock a request to Promote a user or manager into an admin with a user
         * who is already an admin.
         */
        @Test
        @Transactional
        public void promoteToAdmin_userAlreadyAdmin_shouldReturnConflict() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + adminDto.getId() + "/promote-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                409,
                                "promote-admin-user-already-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to revoke an admin into a user.
         */
        @Test
        @Transactional
        public void revokeAdminRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + adminToRevokeDto.getId() + "/revoke-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "revoke-admin",
                                request -> {
                                        try {
                                                request.andExpect(content().string("Admin role revoked successfully"));

                                                // Assert: fetch admin again and verify role changed to USER
                                                UserDto updatedAdmin = userService.findByLogin("test.admin2@test.com");
                                                assertNotNull(updatedAdmin.getMainRole(),
                                                                "Admin role should not be null after promotion");
                                                assertEquals("USER", updatedAdmin.getMainRole(),
                                                                "Admin role should be USER after promotion");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to revoke an admin into a user with missing
         * authorization header.
         */
        @Test
        @Transactional
        public void revokeAdminRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                performRequest(
                                "PUT",
                                "/users/" + adminToRevokeDto.getId() + "/revoke-admin",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "revoke-admin-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to revoke an admin into a user with malformed token.
         */
        @Test
        @Transactional
        public void revokeAdminRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                performRequest(
                                "PUT",
                                "/users/" + adminToRevokeDto.getId() + "/revoke-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "revoke-admin-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to revoke an admin into a user as a non-admin user.
         */
        @Test
        @Transactional
        public void revokeAdminRole_asNonAdmin_shouldReturnForbidden() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                UserDto adminToRevokeDto = userService.findByLogin("test.admin2@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/users/" + adminToRevokeDto.getId() + "/revoke-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                403,
                                "revoke-admin-non-admin",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to revoke an admin into a user with a non-existing user.
         */
        @Test
        @Transactional
        public void revokeAdminRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                String fakeUserId = "9999";

                performRequest(
                                "PUT",
                                "/users/" + fakeUserId + "/revoke-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "revoke-admin-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to downgrade an admin into a manager.
         */
        @Test
        @Transactional
        public void downgradeAdminRole_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/" + adminToDowngradeDto.getId() + "/downgrade-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "downgrade-admin",
                                request -> {
                                        try {
                                                request.andExpect(
                                                                content().string("Admin role downgraded successfully"));

                                                // Assert: fetch admin again and verify role changed to MANAGER
                                                UserDto updatedAdmin = userService.findByLogin("test.admin2@test.com");
                                                assertNotNull(updatedAdmin.getMainRole(),
                                                                "Admin role should not be null after promotion");
                                                assertEquals("MANAGER", updatedAdmin.getMainRole(),
                                                                "Admin role should be MANAGER after promotion");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to downgrade an admin into a manager with missing
         * authorization header.
         */
        @Test
        @Transactional
        public void downgradeAdminRole_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                performRequest(
                                "PUT",
                                "/users/" + adminToDowngradeDto.getId() + "/downgrade-admin",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "downgrade-admin-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin
         *
         * Mock a request to downgrade an admin into a manager with a malformed token
         */
        @Test
        @Transactional
        public void downgradeAdminRole_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto adminToDowngradeDto = userService.findByLogin("test.admin2@test.com");

                performRequest(
                                "PUT",
                                "/users/" + adminToDowngradeDto.getId() + "/downgrade-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "downgrade-admin-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message").exists());

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
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
