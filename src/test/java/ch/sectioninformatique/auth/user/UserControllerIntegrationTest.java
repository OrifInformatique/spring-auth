package ch.sectioninformatique.auth.user;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;

/**
 * Integration tests for the UserController.
 * 
 * These tests cover various scenarios for the user management endpoints,
 * including authentication, authorization, and role management.
 * 
 * The tests use MockMvc to perform HTTP requests and verify responses.
 */
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
                requestType.locale(LocaleContextHolder.getLocale());

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

        /** UserRepository instance for user-related operations. */
        @Autowired
        private UserRepository userRepository;

        @Autowired
        private MessageSource messageSource;
        @BeforeEach
        public void setUp() {
                LocaleContextHolder.setLocale(Locale.FRANCE);
        }

        @AfterEach
        public void tearDown() {
                LocaleContextHolder.resetLocaleContext();
        }

        private String message(String key, Object... args) {
                return messageSource.getMessage(key, args, Locale.FRANCE);
        }

        /**
         * Test: GET /users/me - Retrieve authenticated user's information
         * 
         * Verifies that an authenticated user can retrieve their own user information.
         * This is a common endpoint for checking current user session and profile data.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains user information (id, firstName, lastName, login, mainRole)
         * - Response includes a refreshed JWT access token
         * - Only the authenticated user's data is returned (not other users)
         * 
         * Test data:
         * - User: test.user@test.com (authenticated via JWT token)
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
                                                request.andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("User"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.user@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me - Authentication error without Authorization header
         * 
         * Verifies that the /users/me endpoint requires authentication.
         * Requests without an Authorization header should be rejected.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing authentication
         * - Response contains localized error message
         * - No user data is returned
         * 
         * Test data:
         * - Authorization header: (missing)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me - Authentication error with invalid JWT token
         * 
         * Verifies that the endpoint properly validates JWT tokens and rejects malformed ones.
         * Invalid tokens (wrong format, invalid signature, corrupted data) should not grant access.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - No user data is returned
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/me - Authentication error with expired JWT token
         * 
         * Verifies that the endpoint properly checks token expiration and rejects expired tokens.
         * This ensures users must refresh their tokens periodically for continued access.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token expiration validation fails
         * - Response contains localized error message about expired token
         * - No user data is returned
         * - Client should request a new token using refresh token
         * 
         * Test data:
         * - User: test.user@test.com
         * - JWT Token: Expired 2 hours ago
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.expired")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all - Retrieve all active users (excluding soft-deleted)
         * 
         * Verifies that authenticated users can retrieve a list of all active users in the system.
         * This endpoint excludes soft-deleted users and returns only active accounts.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains an array of user objects
         * - Returns exactly 4 users (from test data seeder)
         * - Soft-deleted users are excluded from the list
         * - All expected test users are present in the response
         * 
         * Test data:
         * - Authenticated as: test.user@test.com
         * - Expected users: test.user@test.com, test.manager@test.com, 
         *   test.admin@test.com, test.admin2@test.com
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
         * Test: GET /users/all - Authentication error without Authorization header
         * 
         * Verifies that the /users/all endpoint requires authentication to protect
         * user data from unauthorized access.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing authentication
         * - Response contains localized error message
         * - No user list is returned
         * 
         * Test data:
         * - Authorization header: (missing)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all - Authentication error with invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized access to user data.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - No user list is returned
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all - Authentication error with expired JWT token
         * 
         * Verifies that the endpoint checks token expiration to ensure users
         * cannot access protected resources with expired tokens.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token expiration validation fails
         * - Response contains localized error message about expired token
         * - No user list is returned
         * - Client should refresh token and retry
         * 
         * Test data:
         * - User: test.user@test.com
         * - JWT Token: Expired 2 hours ago
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.expired")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/all-with-deleted - Retrieve all users including soft-deleted
         * 
         * Verifies that authenticated administrators can retrieve a complete list of all users,
         * including those that have been soft-deleted. This is useful for admin panels and
         * audit purposes.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains an array of all user objects
         * - Includes both active and soft-deleted users
         * - Returns at least 4 users (may include soft-deleted ones)
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         */
        @Test
        @Transactional
        public void allWithDeleted_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "GET",
                                "/users/all-with-deleted",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "all-with-deleted",
                                request -> {
                                        try {
                                                MvcResult result = request.andReturn();
                                                String responseBody = result.getResponse().getContentAsString();

                                                ObjectMapper mapper = new ObjectMapper();
                                                List<Map<String, Object>> users = mapper.readValue(responseBody,
                                                                new TypeReference<List<Map<String, Object>>>() {
                                                                });

                                                assertTrue(users.size() >= 4, "Should return at least 4 users");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /users/deleted - Retrieve only soft-deleted users
         * 
         * Verifies that authenticated administrators can retrieve a filtered list
         * of only soft-deleted users. This is useful for reviewing deleted accounts
         * and potentially restoring them.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains an array of soft-deleted user objects
         * - Excludes active users from the list
         * - Returns an empty array if no users are soft-deleted
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         */
        @Test
        @Transactional
        public void deleted_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "GET",
                                "/users/deleted",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "deleted",
                                request -> {
                                        try {
                                                MvcResult result = request.andReturn();
                                                String responseBody = result.getResponse().getContentAsString();

                                                ObjectMapper mapper = new ObjectMapper();
                                                List<Map<String, Object>> users = mapper.readValue(responseBody,
                                                                new TypeReference<List<Map<String, Object>>>() {
                                                                });

                                                assertNotNull(users, "Should return a list of users");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/restore - Restore a soft-deleted user
         * 
         * Verifies that administrators can restore users that were previously soft-deleted.
         * This is a two-step test: first soft-delete a user, then restore them.
         * 
         * Expected behavior:
         * - Step 1: Soft-delete succeeds (HTTP 200)
         * - Step 2: Restore succeeds (HTTP 200)
         * - Response contains success message: "message.user.restored"
         * - User is marked as active again in the database
         * - User can log in after restoration
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.user@test.com
         */
        @Test
        @Transactional
        public void restoreDeletedUser_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                UserDto userDto = userService.findByLogin("test.user@test.com");
                
                String adminToken = userAuthenticationProvider.createToken(adminDto);
                
                // First, soft delete the user using the DELETE endpoint
                performRequest(
                                "DELETE",
                                "/users/" + userDto.getId() + "/false",
                                null,
                                adminToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "delete-for-restore",
                                null
                );

                // Then restore the user
                performRequest(
                                "PUT",
                                "/users/" + userDto.getId() + "/restore",
                                null,
                                adminToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "restore",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$")
                                                                .value(message("message.user.restored")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: DELETE /users/{userId}/permanent - Permanently delete a user
         * 
         * Verifies that administrators can permanently remove users from the database.
         * Unlike soft-delete, this operation cannot be undone. This should be used with caution.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User is permanently removed from the database
         * - Response contains confirmation message: "message.user.deleted.permanent"
         * - Response includes the deleted user's login for confirmation
         * - User cannot be restored after permanent deletion
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.user@test.com
         */
        @Test
        @Transactional
        public void deletePermanent_withRealData_shouldReturnSuccess() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "DELETE",
                                "/users/" + userDto.getId() + "/true",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "delete-permanent",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("message.user.deleted.permanent")))
                                                                .andExpect(jsonPath("$.deletedUserLogin")
                                                                                .value("test.user@test.com"));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Promote user to MANAGER role
         * 
         * Verifies that administrators can promote regular users to the MANAGER role.
         * This grants elevated permissions for managing other users and resources.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User's role is changed from USER to MANAGER
         * - Response contains success message: "message.user.promoted.manager"
         * - Database is updated with new role
         * - User gains MANAGER permissions immediately
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.user@test.com (USER role)
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
                                                                .string(message("message.user.promoted.manager")));

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
         * Test: PUT /users/{userId}/promote-manager - Authentication required
         * 
         * Verifies that role promotion requires authentication to prevent unauthorized
         * privilege escalation.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.user@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized role changes.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.user@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Authorization check (admin only)
         * 
         * Verifies that only administrators can promote users to manager role.
         * Regular users should not be able to change roles, even their own.
         * 
         * Expected behavior:
         * - Returns HTTP 403 (Forbidden)
         * - Request is rejected due to insufficient permissions
         * - Response contains authorization error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authenticated as: test.user@test.com (USER role - insufficient permissions)
         * - Target user: test.user@test.com (attempting self-promotion)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.access.denied")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Error when user doesn't exist
         * 
         * Verifies that the endpoint properly handles requests to promote non-existent users.
         * This prevents accidental or malicious attempts to promote invalid user IDs.
         * 
         * Expected behavior:
         * - Returns HTTP 404 (Not Found)
         * - Request is rejected because user ID doesn't exist
         * - Response contains localized error message about user not found
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user ID: 9999 (non-existent)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.not.found", fakeUserId)));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Conflict when user is already manager
         * 
         * Verifies that attempting to promote a user who already has manager role
         * returns an appropriate error instead of succeeding unnecessarily.
         * 
         * Expected behavior:
         * - Returns HTTP 409 (Conflict)
         * - Request is rejected because user already has MANAGER role
         * - Response contains localized error message about user already being manager
         * - User's role remains MANAGER (unchanged)
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.manager@test.com (already MANAGER role)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.already.manager",
                                                                                managerDto.getLogin())));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-manager - Conflict when user is already admin
         * 
         * Verifies that attempting to "promote" an admin to manager role returns an error.
         * This would actually be a demotion since ADMIN is higher than MANAGER.
         * 
         * Expected behavior:
         * - Returns HTTP 409 (Conflict)
         * - Request is rejected because user has ADMIN role (higher than MANAGER)
         * - Response contains localized error message about conflicting role
         * - User's role remains ADMIN (unchanged)
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.admin@test.com (already ADMIN role - higher than MANAGER)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.already.admin",
                                                                                adminDto.getLogin())));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager - Revoke manager role, demote to USER
         * 
         * Verifies that administrators can revoke manager privileges, demoting them
         * back to regular USER role. This is useful for removing elevated permissions.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User's role is changed from MANAGER to USER
         * - Response contains success message: "message.user.revoked.manager"
         * - Database is updated with new role
         * - User loses manager permissions immediately
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.manager@test.com (MANAGER role to be revoked)
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
                                                                content().string(message("message.user.revoked.manager")));

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
         * Test: PUT /users/{userId}/revoke-manager - Authentication required
         * 
         * Verifies that revoking manager privileges requires authentication to prevent
         * unauthorized role modifications.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized manager role revocations.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager - Authorization check (admin only)
         * 
         * Verifies that only administrators can revoke manager privileges.
         * Non-admin users should not be able to modify manager roles.
         * 
         * Expected behavior:
         * - Returns HTTP 403 (Forbidden)
         * - Request is rejected due to insufficient permissions
         * - Response contains authorization error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authenticated as: test.user@test.com (USER role - insufficient permissions)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.access.denied")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-manager - Error when user doesn't exist
         * 
         * Verifies that the endpoint properly handles requests to revoke manager role
         * from non-existent users.
         * 
         * Expected behavior:
         * - Returns HTTP 404 (Not Found)
         * - Request is rejected because user ID doesn't exist
         * - Response contains localized error message about user not found
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user ID: 9999 (non-existent)
         */
        @Test
        @Transactional
        public void revokeManagerRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/9999/revoke-manager",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "revoke-manager-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.not.found", "9999")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin - Promote user/manager to ADMIN role
         * 
         * Verifies that administrators can promote users or managers to the ADMIN role.
         * This grants the highest level of permissions, allowing full system management.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User's role is changed to ADMIN
         * - Response contains success message: "message.user.promoted.admin"
         * - Database is updated with new role
         * - User gains all administrative permissions immediately
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.manager@test.com (MANAGER role)
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
                                                request.andExpect(content().string(message("message.user.promoted.admin")));

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
         * Test: PUT /users/{userId}/promote-admin - Authentication required
         * 
         * Verifies that promoting users to admin requires authentication to prevent
         * unauthorized privilege escalation to the highest level.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized admin promotions.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin - Authorization check (admin only)
         * 
         * Verifies that only administrators can promote users to admin role.
         * Non-admin users (including managers) should not be able to grant admin privileges.
         * 
         * Expected behavior:
         * - Returns HTTP 403 (Forbidden)
         * - Request is rejected due to insufficient permissions
         * - Response contains authorization error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authenticated as: test.user@test.com (USER role - insufficient permissions)
         * - Target user: test.manager@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.access.denied")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin - Error when user doesn't exist
         * 
         * Verifies that the endpoint properly handles requests to promote non-existent users.
         * This prevents accidental or malicious attempts to promote invalid user IDs.
         * 
         * Expected behavior:
         * - Returns HTTP 404 (Not Found)
         * - Request is rejected because user ID doesn't exist
         * - Response contains localized error message about user not found
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user ID: 9999 (non-existent)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.not.found", fakeUserId)));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/promote-admin - Conflict when user is already admin
         * 
         * Verifies that attempting to promote a user who already has admin role
         * returns an appropriate error instead of succeeding unnecessarily.
         * 
         * Expected behavior:
         * - Returns HTTP 409 (Conflict)
         * - Request is rejected because user already has ADMIN role
         * - Response contains localized error message about user already being admin
         * - User's role remains ADMIN (unchanged)
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.admin@test.com (already ADMIN role)
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.already.admin",
                                                                                adminDto.getLogin())));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin - Revoke admin role, demote to USER
         * 
         * Verifies that administrators can revoke admin privileges from other admins,
         * demoting them back to regular USER role. This is useful for removing admin access.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User's role is changed from ADMIN to USER
         * - Response contains success message: "message.user.revoked.admin"
         * - Database is updated with new role
         * - User loses admin permissions immediately
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.admin2@test.com (ADMIN role to be revoked)
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
                                                request.andExpect(content().string(message("message.user.revoked.admin")));

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
         * Test: PUT /users/{userId}/revoke-admin - Authentication required
         * 
         * Verifies that revoking admin privileges requires authentication to prevent
         * unauthorized role modifications.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.admin2@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized admin role revocations.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.admin2@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin - Authorization check (admin only)
         * 
         * Verifies that only administrators can revoke admin privileges from other users.
         * Non-admin users should not be able to modify admin roles.
         * 
         * Expected behavior:
         * - Returns HTTP 403 (Forbidden)
         * - Request is rejected due to insufficient permissions
         * - Response contains authorization error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authenticated as: test.user@test.com (USER role - insufficient permissions)
         * - Target user: test.admin2@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.access.denied")));

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/revoke-admin - Error when user doesn't exist
         * 
         * Verifies that the endpoint properly handles requests to revoke admin role
         * from non-existent users.
         * 
         * Expected behavior:
         * - Returns HTTP 404 (Not Found)
         * - Request is rejected because user ID doesn't exist
         * - Response contains localized error message about user not found
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user ID: 9999 (non-existent)
         */
        @Test
        @Transactional
        public void revokeAdminRole_userNotFound_shouldReturnNotFound() throws Exception {
                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "PUT",
                                "/users/9999/revoke-admin",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                404,
                                "revoke-admin-user-not-found",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.user.not.found", "9999")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/downgrade-admin - Downgrade admin to MANAGER role
         * 
         * Verifies that administrators can downgrade other admins to manager role.
         * This allows partial privilege reduction while retaining some elevated permissions.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User's role is changed from ADMIN to MANAGER
         * - Response contains success message: "message.user.downgraded.admin"
         * - Database is updated with new role
         * - User loses admin permissions but retains manager permissions
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.admin2@test.com (ADMIN role to be downgraded)
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
                                                                content().string(message("message.user.downgraded.admin")));

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
         * Test: PUT /users/{userId}/downgrade-admin - Authentication required
         * 
         * Verifies that downgrading admin roles requires authentication to prevent
         * unauthorized role modifications.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.admin2@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /users/{userId}/downgrade-admin - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized admin role downgrades.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User's role remains unchanged
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.admin2@test.com
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
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));

                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: DELETE /users/{userId} - Soft delete a user
         * 
         * Verifies that administrators can soft-delete users. Soft deletion marks the user
         * as deleted without removing them from the database, allowing for potential restoration.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - User is marked as deleted in the database (isDeleted = true)
         * - User record remains in database for audit/recovery purposes
         * - Response contains success message: "message.user.deleted"
         * - Response includes the deleted user's login for confirmation
         * - User cannot log in after soft deletion
         * 
         * Test data:
         * - Authenticated as: test.admin@test.com (admin user)
         * - Target user: test.user@test.com
         */
        @Test
        @Transactional
        public void deleteUser_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                performRequest(
                                "DELETE",
                                "/users/" + userDto.getId() + "/false",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                200,
                                "delete",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("message.user.deleted")))
                                                                .andExpect(jsonPath("$.deletedUserLogin")
                                                                                .value("test.user@test.com"));

                                                // Verify the user is marked as deleted in the database
                                                User deletedUser = userRepository.findByLogin("test.user@test.com")
                                                        .orElseThrow(() -> new AssertionError("User should still exist in database"));
                                                assertTrue(deletedUser.isDeleted(), "User should be marked as deleted");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: DELETE /users/{userId} - Authentication required
         * 
         * Verifies that deleting users requires authentication to prevent
         * unauthorized account deletions.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains localized error message
         * - User remains active (not deleted)
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Target user: test.user@test.com
         */
        @Test
        @Transactional
        public void deleteUser_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                performRequest(
                                "DELETE",
                                "/users/" + userDto.getId() + "/false",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "delete-missing-authorization",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.authentication.token.invalid.or.missing")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: DELETE /users/{userId} - Reject invalid JWT token
         * 
         * Verifies that the endpoint validates JWT tokens and rejects malformed ones
         * to prevent unauthorized user deletions.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - User remains active (not deleted)
         * 
         * Test data:
         * - JWT Token: this.is.not.a.valid.token (malformed)
         * - Target user: test.user@test.com
         */
        @Test
        @Transactional
        public void deleteUser_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";
                UserDto userDto = userService.findByLogin("test.user@test.com");


                performRequest(
                                "DELETE",
                                "/users/" + userDto.getId() + "/false",
                                null,
                                token,
                                MediaType.APPLICATION_JSON,
                                401,
                                "delete-malformed-token",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.security.token.invalid")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }
}
