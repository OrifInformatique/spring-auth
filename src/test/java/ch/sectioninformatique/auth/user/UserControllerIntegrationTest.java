package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@Tag("integration")
@SpringBootTest(classes = AuthApplication.class)
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

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
         * Test the /users/me endpoint with real data.
         * This test retrieves a known user, generates an authentication token,
         * and performs a GET request to the /users/me endpoint.
         * It verifies that the response contains the expected user information
         * and saves the response and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void me_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Test"))
                                .andExpect(jsonPath("$.lastName").value("User"))
                                .andExpect(jsonPath("$.login").value("test.user@test.com"))
                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-me-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-me-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/me endpoint without Authorization header.
         * This test performs a GET request to the /users/me endpoint
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void me_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {

                MvcResult result = mockMvc.perform(get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-me-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);
        }

        /**
         * Test the /users/me endpoint with a malformed token.
         * This test performs a GET request to the /users/me endpoint
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void me_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";

                MvcResult result = mockMvc.perform(get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-me-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-me-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/me endpoint with an expired token.
         * This test retrieves a known user, generates an expired
         * authentication token,
         * and performs a GET request to the /users/me endpoint.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void me_withExpiredToken_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto, Date.from(
                                Instant.now().minus(2, ChronoUnit.HOURS)));

                MvcResult result = mockMvc.perform(get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-me-response-expired-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-me-token-expired-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/all endpoint with real data.
         * This test retrieves a known user, generates an authentication token,
         * and performs a GET request to the /users/all endpoint.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void all_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(get("/users/all")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andReturn();

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

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-all-response.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-all-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/all endpoint with missing authorization header.
         * This test performs a GET request to the /users/all endpoint
         * without providing an Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response to a file.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void all_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {
                MvcResult result = mockMvc.perform(get("/users/all")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-all-response-missing-authorization.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

        }

        /**
         * Test the /users/all endpoint with a malformed token.
         * This test performs a GET request to the /users/all endpoint
         * with an invalid JWT token in the Authorization header.
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void all_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String token = "this.is.not.a.valid.token";

                MvcResult result = mockMvc.perform(get("/users/all")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-all-response-malformed-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-all-token-malformed-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/all endpoint with an expired token.
         * This test performs a GET request to the /users/all endpoint
         * with an expired token
         * It verifies that the response status is Unauthorized and
         * saves the response and token to files.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void all_withExpiredToken_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String token = userAuthenticationProvider.createToken(userDto, Date.from(
                                Instant.now().minus(2, ChronoUnit.HOURS)));

                MvcResult result = mockMvc.perform(get("/users/all")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").exists())
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-all-response-expired-token.json");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-all-token-expired-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }

        /**
         * Test the /users/{userId}/promote-manager endpoint with real data.
         * This test retrieves a known user and an admin user, generates an
         * authentication token for the admin,
         * and performs a PUT request to promote the user to manager.
         * It verifies that the response status is OK and saves the response
         * and token to files for later use.
         * 
         * @throws Exception if an error occurs during the test
         */
        @Test
        @Transactional
        public void promoteToManager_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                UserDto adminDto = userService.findByLogin("test.admin@test.com");

                String token = userAuthenticationProvider.createToken(adminDto);

                MvcResult result = mockMvc.perform(put("/users/" + userDto.getId().toString() + "/promote-manager")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("User promoted to manager successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: fetch user again and verify role changed to MANAGER
                UserDto updatedUser = userService.findByLogin("test.user@test.com");
                assertNotNull(updatedUser.getMainRole(), "User role should not be null after promotion");
                assertEquals("MANAGER", updatedUser.getMainRole(),
                                "User role should be MANAGER after promotion");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-promoteToManager-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-promoteToManager-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
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
                                .andExpect(content().string("User deleted successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();

                // Assert: verify user no longer exists
                assertThrows(AppException.class, () -> {
                        userService.findByLogin("test.user@test.com");
                }, "Expected AppException when fetching deleted user");

                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-deleteUser-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-deleteUser-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }
}
