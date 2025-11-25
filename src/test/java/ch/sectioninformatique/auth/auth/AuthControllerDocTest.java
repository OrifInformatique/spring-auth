package ch.sectioninformatique.auth.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.sectioninformatique.auth.app.exceptions.CustomException;
import ch.sectioninformatique.auth.app.exceptions.InvalidCredentialsException;
import ch.sectioninformatique.auth.app.exceptions.UserAlreadyExistsException;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

/**
 * Documentation tests for AuthController.
 * These tests generate API documentation snippets using Spring REST Docs.
 * 
 * NOTE: These tests depend on test output files (JSON response files) generated
 * by a separate integration test (AuthControllerIntegrationTest). If those
 * files
 * are missing, the tests will fail early with a clear message.
 */
@Tag("restdocs")
@ExtendWith(RestDocumentationExtension.class)
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerDocTest {

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** Static variable to hold the login response JSON for use in tests. */
        private static String loginResponseJson;

        /** Static variable to hold the register response JSON for use in tests. */
        private static String registerResponseJson;

        /** Static variable to hold the refresh response JSON for use in tests. */
        private static String refreshResponseJson;

        /** Static variable to hold the refresh token for use in tests. */
        private static String refreshToken;

        /** Mocked UserService for simulating user-related operations. */
        @MockBean
        private UserService userService;

        /**
         * Mocked UserAuthenticationProvider for simulating authentication operations.
         */
        @MockBean
        private UserAuthenticationProvider userAuthenticationProvider;

        /** Mocked Authentication for simulating security context. */
        @MockBean
        private Authentication authentication;

        /** Mocked SecurityContext for simulating security context. */
        @MockBean
        private SecurityContext securityContext;


        /**
         * Test the /auth/update-password endpoint with missing password to generate
         * documentation.
         * This test performs a set password request with missing password and
         * expects a bad request response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void setPassword_withMockedService_generatesDoc_missingBody() throws Exception {
                UserDto mockedUserDto = UserDto.builder()
                                .id(1L)
                                .login("test.user@test.com")
                                .firstName("Test")
                                .lastName("User")
                                .mainRole("USER")
                                .permissions(new ArrayList<>())
                                .build();

                when(authentication.getPrincipal()).thenReturn(mockedUserDto);
                when(authentication.isAuthenticated()).thenReturn(true);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                // Perform the /auth/update-password request with expected input and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(put("/auth/update-password")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andDo(document("auth/update-password-missing-body", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }

        /**
         * Test the /auth/update-password endpoint with missing token to generate
         * documentation.
         * This test performs a set password request with missing token and
         * expects a unauthorized response.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void setPassword_withMockedService_generatesDoc_missingToken() throws Exception {

                // Mock Spring Security context and authentication
                when(authentication.getPrincipal()).thenReturn(null);
                when(securityContext.getAuthentication()).thenReturn(authentication);
                SecurityContextHolder.setContext(securityContext);

                when(securityContext.getAuthentication()).thenThrow(
                                new CustomException("Full authentication is required to access this resource",
                                                HttpStatus.UNAUTHORIZED));

                // Perform the /auth/update-password request with expected input and validate
                // response
                // Spring REST Docs will capture the interaction and generate documentation
                mockMvc.perform(put("/auth/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"oldPassword\":\"Test1234!\", \"newPassword\":\"TestNewPassword\"}"))
                                .andExpect(status().isUnauthorized())
                                .andDo(document("auth/update-password-missing-token", preprocessRequest(prettyPrint()),
                                                preprocessResponse(prettyPrint())));
        }
}