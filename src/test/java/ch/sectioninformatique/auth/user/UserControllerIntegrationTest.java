package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        @Autowired
        private UserService userService;

        @Test
        public void me_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("john.doe@test.com");

                String token = userAuthenticationProvider.createToken(userDto);

                MvcResult result = mockMvc.perform(get("/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("John"))
                                .andExpect(jsonPath("$.lastName").value("DOE"))
                                .andExpect(jsonPath("$.login").value("john.doe@test.com"))
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

        @Test
        public void all_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("john.doe@test.com");

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
}
