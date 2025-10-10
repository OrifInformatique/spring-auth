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

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

                MvcResult result = mockMvc.perform(put("/users/" + adminToRevokeDto.getId().toString() + "/revoke-admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Admin role revoked successfully"))
                                .andReturn();

                String responseBody = result.getResponse().getContentAsString();
                // Save response to file for later tests
                Path path = Paths.get("target/test-data/users-revokeAdminRole-response.txt");
                Files.createDirectories(path.getParent());
                Files.writeString(path, responseBody);

                // Save token to file for later tests
                Path pathToken = Paths.get("target/test-data/users-revokeAdminRole-token.txt");
                Files.createDirectories(pathToken.getParent());
                Files.writeString(pathToken, token);
        }
}
