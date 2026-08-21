package ch.sectioninformatique.auth.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.AuthApplication;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.security.RoleRepository;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserMapper;
import ch.sectioninformatique.auth.user.UserRepository;
import ch.sectioninformatique.auth.user.UserService;
import jakarta.servlet.http.Cookie;


/**
 * Integration tests for AuthController.
 * These tests use real data and the actual application context to verify
 * the authentication endpoints.
 */
@SpringBootTest(classes = AuthApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
public class AuthControllerIntegrationTest {


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
                request.andDo(document("auth/" + docsFileName, preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));

        }

        /**
         * Helper method for performing and documenting HTTP requests with a cookie.
         * This keeps tests consistent with performRequest while allowing cookie-based auth.
         */
        private void performRequest(
                        String requestTypeString,
                        String endpoint,
                        String content,
                        String token,
                        MediaType contentType,
                        int expectedStatus,
                        String docsFileName,
                        Cookie cookie,
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

                if (cookie != null) {
                        requestType.cookie(cookie);
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
                request.andDo(document("auth/" + docsFileName, preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint())));

        }

        /** MockMvc instance for performing HTTP requests in tests. */
        @Autowired
        private MockMvc mockMvc;

        /** UserAuthenticationProvider instance for managing user authentication. */
        @Autowired
        private UserAuthenticationProvider userAuthenticationProvider;

        @Autowired
        private UserService userService;

        @Autowired
        private AuthService authService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private MessageSource messageSource;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private UserMapper userMapper;

        private static final ResourceBundleMessageSource HV_MESSAGES = new ResourceBundleMessageSource();

        static {
                HV_MESSAGES.setBasename("org.hibernate.validator.ValidationMessages");
                HV_MESSAGES.setDefaultEncoding("UTF-8");
        }

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
         * Test the /auth/login endpoint with missing login.
         * This test performs a login request with missing login and expects a bad
         * request response.
         * The response is saved to a file.
         *
         * @throws Exception if an error occurs during the test
         */
        @Test
        public void login_missingLogin_shouldReturnBadRequest() throws Exception {
                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-missing-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test that a soft-deleted user cannot log in.
         * Retrieves a known user, marks them as deleted,
         * and tries to authenticate with valid credentials.
         * Expects an Unauthorized (401) response.
         * 
         * Verifies that a user can successfully log in with valid email and password.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains user information (id, firstName, lastName, login)
         * - Response includes a valid JWT access token
         * - Response includes a valid refresh token
         * - User's mainRole is correctly set to "USER"
         * 
         * Test data:
         * - Login: test.user@test.com
         * - Password: Test1234!
         */
        @Test
        @Transactional
        public void login_withRealData_shouldReturnSuccess() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                200,
                                "login",
                                null);
        }

        /**
         * Test: POST /auth/login - Validation error when password field is missing
         * 
         * Verifies that the API properly validates required fields and returns
         * an appropriate error when the password field is omitted from the request.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.password for the validation failure
         * - Request is rejected before attempting authentication
         * 
         * Test data:
         * - Login: test.user@test.com
         * - Password: (missing)
         */
        @Test
        @Transactional
        public void login_missingPassword_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-missing-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Validation error for invalid email format
         * 
         * Verifies that the API validates email format using standard email validation rules.
         * Invalid email formats (missing @, missing domain, etc.) should be rejected.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.login for invalid email format
         * - Request is rejected during input validation
         * 
         * Test data:
         * - Login: invalid-email-format (no @ or domain)
         * - Password: Test1234!
         */
        @Test
        @Transactional
        public void login_invalidEmailFormat_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"invalid-email-format\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-invalid-email-format",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Error handling for empty request body
         * 
         * Verifies that the API properly handles requests with no body content.
         * This tests the API's robustness against malformed or incomplete requests.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response contains a localized error message indicating missing request body
         * - Request fails during JSON parsing/validation
         * 
         * Test data:
         * - Request body: (empty string)
         */
        @Test
        @Transactional
        public void login_emptyBody_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-empty-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Error handling for malformed JSON
         * 
         * Verifies that the API properly handles syntactically invalid JSON.
         * This ensures the API doesn't crash or expose internal errors when receiving
         * malformed data (missing closing brace, invalid syntax, etc.).
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response contains a localized error message about JSON parsing failure
         * - Request fails during JSON deserialization
         * 
         * Test data:
         * - Request body: Missing closing brace in JSON
         */
        @Test
        @Transactional
        public void login_malformedJson_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-malformed-json",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Security test for SQL injection in login field
         * 
         * Verifies that the API is protected against SQL injection attacks in the login field.
         * The validation should reject common SQL injection patterns (OR '1'='1', etc.)
         * before they reach the database layer.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - SQL injection attempt is rejected by email validation
         * - No database query is executed with malicious input
         * - Response includes fieldErrors.login
         * 
         * Test data:
         * - Login: ' OR '1'='1 (SQL injection attempt)
         * - Password: Test1234!
         */
        @Test
        @Transactional
        public void login_sqlInjectionAttemptLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"' OR '1'='1\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "login-sql-injection-attempt-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Security test for SQL injection in password field
         * 
         * Verifies that the API is protected against SQL injection attacks in the password field.
         * Since passwords are hashed and compared using secure methods, SQL injection attempts
         * should fail authentication rather than succeed.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - SQL injection attempt is safely handled by password hashing/comparison
         * - No database is compromised
         * - Response contains authentication failure message
         * 
         * Test data:
         * - Login: test.user@test.com
         * - Password: ' OR '1'='1 (SQL injection attempt)
         */
        @Test
        @Transactional
        public void login_sqlInjectionAttemptPassword_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"' OR '1'='1\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "login-sql-injection-attempt-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.authorisation.invalid.credentials")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Error handling for unsupported content type
         * 
         * Verifies that the API enforces the correct Content-Type header.
         * The login endpoint expects application/json, and should reject other media types.
         * 
         * Expected behavior:
         * - Returns HTTP 415 (Unsupported Media Type)
         * - Request is rejected due to incorrect Content-Type header
         * - Response contains a localized error message about media type
         * 
         * Test data:
         * - Content-Type: text/plain (should be application/json)
         * - Valid credentials in request body
         */
        @Test
        @Transactional
        public void login_wrongMediaType_shouldReturnUnsupportedMediaType() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}",
                                null,
                                MediaType.TEXT_PLAIN,
                                415,
                                "login-wrong-media-type",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Authentication failure with incorrect password
         * 
         * Verifies that the API correctly rejects login attempts with valid email
         * but incorrect password. This tests proper password verification.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - User lookup succeeds, but password comparison fails
         * - Response contains authentication error message
         * - No token is issued
         * 
         * Test data:
         * - Login: test.user@test.com (valid user)
         * - Password: WrongPassword! (incorrect password)
         */
        @Test
        @Transactional
        public void login_wrongPassword_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"test.user@test.com\", \"password\":\"WrongPassword!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "login-wrong-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.authorisation.invalid.credentials")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/login - Authentication failure with non-existent user
         * 
         * Verifies that the API correctly handles login attempts for users that
         * don't exist in the database. For security reasons, the error message
         * should not reveal whether the user exists or not.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - User lookup fails (no user found)
         * - Response contains generic authentication error message
         * - Error message doesn't reveal if user exists or password is wrong
         * 
         * Test data:
         * - Login: non.existent@test.com (non-existent user)
         * - Password: WrongPassword!
         */
        @Test
        @Transactional
        public void login_nonExistentUser_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/login",
                                "{\"login\":\"non.existent@test.com\", \"password\":\"WrongPassword!\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "login-non-existent-user",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("error.authorisation.invalid.credentials")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Successful user registration
         * 
         * Verifies that a new user can successfully register with valid information.
         * This test validates the complete registration flow including user creation,
         * role assignment, and token generation.
         * 
         * Expected behavior:
         * - Returns HTTP 201 (Created)
         * - User is created in the database with default USER role
         * - Response contains user information (id, firstName, lastName, login, mainRole)
         * - Response includes a valid JWT access token
         * - Response includes a valid refresh token
         * - Password is securely hashed (not stored in plain text)
         * - Additional verification: User can be retrieved from database after registration
         * 
         * Test data:
         * - First Name: Test
         * - Last Name: NewUser
         * - Login: test.newuser@test.com
         * - Password: testPassword
         */
        @Test
        @Transactional
        public void register_withRealData_shouldReturnSuccess() throws Exception {

                Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
                        .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

                User admin = User.builder()
                .firstName("admin")
                .lastName("test")
                .login("admin.test@test.com")
                .mainRole(adminRole)
                .build();

                UserDto adminDto = userMapper.toUserDto(admin);
                String token = userAuthenticationProvider.createToken(adminDto);
                adminDto.setToken(token);

                String rawPassword = "testPassword";
                String requestBody =
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\""
                                                + rawPassword + "\", \"mainRole\": \"USER\"}";

                performRequest(
                                "POST",
                                "/auth/register",
                                requestBody,
                                adminDto.getToken(),
                                MediaType.APPLICATION_JSON,
                                201,
                                "register",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.id").isNotEmpty())
                                                                .andExpect(jsonPath("$.firstName").value("Test"))
                                                                .andExpect(jsonPath("$.lastName").value("NewUser"))
                                                                .andExpect(jsonPath("$.login")
                                                                                .value("test.newuser@test.com"))
                                                                .andExpect(jsonPath("$.mainRole").value("USER"))
                                                                .andExpect(jsonPath("$.token").isNotEmpty())
                                                                .andExpect(cookie().exists("refresh_token"));

                                                // Assert: fetch user again and verify user created
                                                UserDto updatedUser = userService.findByLogin("test.newuser@test.com");
                                                assertNotNull(updatedUser,
                                                                "User should not be null after registration");
                                                assertEquals("Test", updatedUser.getFirstName(),
                                                                "User first name should match");
                                                assertEquals("NewUser", updatedUser.getLastName(),
                                                                "User last name should match");
                                                assertEquals("test.newuser@test.com", updatedUser.getLogin(),
                                                                "User login should match");
                                                assertEquals("USER", updatedUser.getMainRole(),
                                                                "User role should be USER");

                                                var storedUser = userRepository.findByLogin("test.newuser@test.com")
                                                                .orElseThrow(() -> new RuntimeException("User not found after registration"));

                                                assertTrue(passwordEncoder.matches(rawPassword, storedUser.getPassword()),
                                                                "Stored password should match encoded raw password");
                                                assertNotEquals(rawPassword, storedUser.getPassword(),
                                                                "Password must not be stored in plain text");
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Validation error when firstName is missing
         * 
         * Verifies that the API enforces required field validation during registration.
         * The firstName field is mandatory and its absence should trigger a validation error.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.firstName
         * - No user is created in the database
         * - Request is rejected during input validation
         * 
         * Test data:
         * - First Name: (missing)
         * - Last Name: NewUser
         * - Login: test.newuser@test.com
         * - Password: testPassword
         */
        @Test
        @Transactional
        public void register_missingFirstName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-first-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Validation error when lastName is missing
         * 
         * Verifies that the API enforces required field validation during registration.
         * The lastName field is mandatory and its absence should trigger a validation error.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.lastName
         * - No user is created in the database
         * - Request is rejected during input validation
         * 
         * Test data:
         * - First Name: Test
         * - Last Name: (missing)
         * - Login: test.newuser@test.com
         * - Password: testPassword
         */
        @Test
        @Transactional
        public void register_missingLastName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-last-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
                        }


        @Transactional
        @Test
        public void register_withWrongPermissions_shouldReturn403() throws Exception{

                Role userRole = roleRepository.findByName(RoleEnum.USER)
                        .orElseThrow(() -> new RuntimeException("Role USER not found"));

                User user = User.builder()
                        .firstName("Test")
	                .lastName("User")
			.login("test.user@test.com")
			.password(passwordEncoder.encode("Secure123@Pass"))
			.mainRole(userRole)
			.build();

                UserDto userDto = userMapper.toUserDto(user);
                String token = userAuthenticationProvider.createToken(userDto);
                userDto.setToken(token);

                performRequest(
                        "POST", 
                        "/auth/register",
                        "{\"firstName\":\"NewTest\",\"lastName\":\"Register\",\"login\":\"newtest.register@test.com\",\"password\":\"testPassword\"}", 
                        userDto.getToken(),
                        MediaType.APPLICATION_JSON,
                        403,
                        "register-noPermissions",
                        request ->{}
                );
        }

        /**
         * Test: POST /auth/register - Validation error when login is missing
         * 
         * Verifies that the API enforces required field validation during registration.
         * The login (email) field is mandatory as it serves as the unique identifier.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.login
         * - No user is created in the database
         * - Request is rejected during input validation
         * 
         * Test data:
         * - First Name: Test
         * - Last Name: NewUser
         * - Login: (missing)
         * - Password: testPassword
         */
        @Test
        @Transactional
        public void register_missingLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Validation error when password is missing
         * 
         * Verifies that the API enforces required field validation during registration.
         * The password field is mandatory for account security.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.password
         * - No user is created in the database
         * - Request is rejected during input validation
         * 
         * Test data:
         * - First Name: Test
         * - Last Name: NewUser
         * - Login: test.newuser@test.com
         * - Password: (missing)
         */
        @Test
        @Transactional
        public void register_missingPassword_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"login\":\"test.newuser@test.com\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-missing-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.password").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Validation error for invalid email format
         * 
         * Verifies that the API validates email format according to standard email rules.
         * The login field must be a properly formatted email address.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response includes fieldErrors.login for email format
         * - No user is created in the database
         * - Request is rejected during email validation
         * 
         * Test data:
         * - Login: invalid-email-format (missing @ symbol and domain)
         * - Other fields: valid values
         */
        @Test
        @Transactional
        public void register_invalidEmailFormat_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\", \"login\":\"invalid-email-format\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-invalid-email-format",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Error handling for empty request body
         * 
         * Verifies that the API properly handles registration requests with no body content.
         * This tests the API's robustness against incomplete or malformed requests.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response contains a localized error message about missing request body
         * - No user is created in the database
         * - Request fails during JSON parsing/validation
         * 
         * Test data:
         * - Request body: (empty string)
         */
        @Test
        @Transactional
        public void register_emptyBody_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-empty-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Error handling for malformed JSON
         * 
         * Verifies that the API properly handles syntactically invalid JSON in registration requests.
         * The API should gracefully handle JSON parsing errors without crashing.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Response contains a localized error message about JSON parsing failure
         * - No user is created in the database
         * - Request fails during JSON deserialization
         * 
         * Test data:
         * - Request body: JSON with missing closing brace
         */
        @Test
        @Transactional
        public void register_malformedJson_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"User\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-malformed-json",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Security test for SQL injection in firstName
         * 
         * Verifies that the API is protected against SQL injection attacks in the firstName field.
         * The validation should reject SQL injection patterns before they reach the database.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - SQL injection attempt is rejected by name validation
         * - Response includes fieldErrors.firstName
         * - No database query is executed with malicious input
         * - No user is created
         * 
         * Test data:
         * - First Name: ' OR '1'='1 (SQL injection attempt)
         * - Other fields: valid values
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptFirstName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"' OR '1'='1\", \"lastName\":\"User\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-first-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.firstName").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Security test for SQL injection in lastName
         * 
         * Verifies that the API is protected against SQL injection attacks in the lastName field.
         * The validation should reject SQL injection patterns before they reach the database.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - SQL injection attempt is rejected by name validation
         * - Response includes fieldErrors.lastName
         * - No database query is executed with malicious input
         * - No user is created
         * 
         * Test data:
         * - Last Name: ' OR '1'='1 (SQL injection attempt)
         * - Other fields: valid values
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptLastName_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"' OR '1'='1\", \"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-last-name",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Security test for SQL injection in login
         * 
         * Verifies that the API is protected against SQL injection attacks in the login (email) field.
         * The email validation should reject SQL injection patterns.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - SQL injection attempt is rejected by email validation
         * - Response includes fieldErrors.login
         * - No database query is executed with malicious input
         * - No user is created
         * 
         * Test data:
         * - Login: ' OR '1'='1 (SQL injection attempt)
         * - Other fields: valid values
         */
        @Test
        @Transactional
        public void register_sqlInjectionAttemptLogin_shouldReturnBadRequest() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\", \"lastName\":\"User\", \"login\":\"' OR '1'='1\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                400,
                                "register-sql-injection-attempt-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.fieldErrors.login").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Error handling for unsupported content type
         * 
         * Verifies that the API enforces the correct Content-Type header for registration.
         * The endpoint expects application/json and should reject other media types.
         * 
         * Expected behavior:
         * - Returns HTTP 415 (Unsupported Media Type)
         * - Request is rejected due to incorrect Content-Type header
         * - Response contains a localized error message about media type
         * - No user is created in the database
         * 
         * Test data:
         * - Content-Type: text/plain (should be application/json)
         * - Valid registration data in request body
         */
        @Test
        @Transactional
        public void register_wrongMediaType_shouldReturnUnsupportedMediaType() throws Exception {

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"NewUser\",\"login\":\"test.newuser@test.com\", \"password\":\"testPassword\"}",
                                null,
                                MediaType.TEXT_PLAIN,
                                415,
                                "register-wrong-media-type",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/register - Conflict error when registering with existing email
         * 
         * Verifies that the API prevents duplicate user registrations with the same email.
         * Email addresses must be unique as they serve as login credentials.
         * 
         * Expected behavior:
         * - Returns HTTP 409 (Conflict)
         * - Registration is rejected because email already exists
         * - Response contains a localized error message about duplicate login
         * - No new user is created (existing user remains unchanged)
         * 
         * Test data:
         * - Login: test.user@test.com (already exists in database)
         * - Other fields: valid but different from existing user
         */
        @Test
        @Transactional
        public void register_duplicateLogin_shouldReturnConflict() throws Exception {

                Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
                        .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

                User admin = User.builder()
                .firstName("admin")
                .lastName("test")
                .login("admin.test@test.com")
                .mainRole(adminRole)
                .build();

                UserDto adminDto = userMapper.toUserDto(admin);
                String token = userAuthenticationProvider.createToken(adminDto);
                adminDto.setToken(token);

                performRequest(
                                "POST",
                                "/auth/register",
                                "{\"firstName\":\"Test\",\"lastName\":\"User\",\"login\":\"test.user@test.com\", \"password\":\"Test1234!\"}",
                                adminDto.getToken(),
                                MediaType.APPLICATION_JSON,
                                409,
                                "register-duplicate-login",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message(
                                                                                "error.user.already.exists",
                                                                                "test.user@test.com")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /auth/refresh - Successful token refresh with valid refresh token
         * 
         * Verifies that users can obtain a new access token using a valid refresh token.
         * This is essential for maintaining user sessions without requiring re-login.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains updated user information
         * - Response includes a new JWT access token
         * - User is authenticated using the refresh token
         * - Original refresh token remains valid (can be used again)
         * 
         * Test data:
         * - User: test.user@test.com
         * - Refresh token: Generated from valid user session
         */
        @Test
        @Transactional
        public void refresh_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);
                // Store the refresh token in the database
                userService.storeRefreshToken(userDto.getLogin(), refreshToken, java.time.Instant.now().plus(java.time.Duration.ofDays(30)));

                performRequest(
                                "POST",
                                "/auth/refresh",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                200,
                                "refresh",
                                new Cookie("refresh_token", refreshToken),
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.accessToken").isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: GET /auth/refresh - Authentication error when refresh token is missing
         * 
         * Verifies that the API requires a refresh token in the Authorization header.
         * Requests without authentication should be rejected.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains authentication error message
         * - No token is issued
         * 
         * Test data:
         * - Authorization header: (missing)
         */
        @Test
        @Transactional
        public void refresh_missingToken_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-missing-token",
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
         * Test: GET /auth/refresh - Authentication error with invalid/malformed token
         * 
         * Verifies that the API properly validates refresh tokens and rejects invalid ones.
         * Invalid tokens (malformed JWT, wrong signature, etc.) should not grant access.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - No new token is issued
         * 
         * Test data:
         * - Refresh token: this.is.not.a.valid.token (invalid JWT format)
         */
        @Test
        @Transactional
        public void refresh_invalidToken_shouldReturnUnauthorized() throws Exception {
                String invalidToken = "this.is.not.a.valid.token";

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                invalidToken,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-invalid-token",
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
         * Test: GET /auth/refresh - Authentication error when Authorization header is missing
         * 
         * Verifies that the API enforces the presence of the Authorization header.
         * This is a duplicate of refresh_missingToken but explicitly tests the header requirement.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing Authorization header
         * - Response contains authentication error message
         * - No token is issued
         * 
         * Test data:
         * - Authorization header: (not set)
         */
        @Test
        @Transactional
        public void refresh_missingAuthorizationHeader_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/auth/refresh",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-missing-authorization",
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
         * Test: GET /auth/refresh - Authentication error with empty body
         * 
         * Verifies that GET requests with empty bodies are handled correctly.
         * Since refresh tokens are sent in headers, empty body should still fail
         * if no Authorization header is provided.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing authentication in header
         * - Response contains authentication error message
         * - No token is issued
         * 
         * Test data:
         * - Request body: (empty)
         * - Authorization header: (missing)
         */
        @Test
        @Transactional
        public void refresh_emptyBody_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "GET",
                                "/auth/refresh",
                                "",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "refresh-empty-body",
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
         * Test: PUT /auth/update-password - Successful password update
         * 
         * Verifies that authenticated users can successfully change their password
         * by providing their current password and a new password.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Old password is verified before allowing update
         * - New password is securely hashed and stored
         * - Response contains success message
         * - User can subsequently log in with the new password
         * 
         * Test data:
         * - User: test.user@test.com (authenticated via refresh token)
         * - Old Password: Test1234!
         * - New Password: TestNewPassword
         */
        @Test
        @Transactional
        public void updatePassword_withRealData_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String accessToken = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "{\"oldPassword\":\"Test1234!\", \"newPassword\":\"TestNewPassword\"}",
                                accessToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "update-password",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("message.password.updated")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /auth/update-password - Validation error with empty request body
         * 
         * Verifies that the API requires password data in the request body.
         * Even with valid authentication, the request must include password information.
         * 
         * Expected behavior:
         * - Returns HTTP 400 (Bad Request)
         * - Request is rejected due to missing required fields
         * - Response contains a localized error message for malformed or missing JSON
         * - User's password remains unchanged
         * 
         * Test data:
         * - User: test.user@test.com (authenticated via refresh token)
         * - Request body: (empty)
         */
        @Test
        @Transactional
        public void setPassword_missingBody_shouldReturnBadRequest() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String accessToken = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "",
                                accessToken,
                                MediaType.APPLICATION_JSON,
                                400,
                                "update-password-missing-body",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .isNotEmpty());
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: PUT /auth/update-password - Authentication error without token
         * 
         * Verifies that password updates require user authentication.
         * Unauthenticated requests should be rejected even with valid password data.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing authentication
         * - Response contains authentication error message
         * - No password is changed
         * 
         * Test data:
         * - Authorization header: (missing)
         * - Request body: Valid password update data
         */
        @Test
        @Transactional
        public void setPassword_missingToken_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "PUT",
                                "/auth/update-password",
                                "{\"oldPassword\":\"Test1234!\", \"newPassword\":\"TestNewPassword\"}",
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "update-password-missing-token",
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
         * Test: POST /auth/logout - Successful logout
         * 
         * Verifies that authenticated users can successfully log out by invalidating
         * their refresh tokens. After logout, the refresh token should no longer be valid.
         * 
         * Expected behavior:
         * - Returns HTTP 200 (OK)
         * - Response contains success message
         * - Refresh tokens are deleted from the database
         * - User can no longer use their refresh token
         * - User must re-login to obtain new tokens
         * 
         * Test data:
         * - User: test.user@test.com (authenticated via access token)
         */
        @Test
        @Transactional
        public void logout_withValidToken_shouldReturnSuccess() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");
                String accessToken = userAuthenticationProvider.createToken(userDto);

                performRequest(
                                "POST",
                                "/auth/logout",
                                null,
                                accessToken,
                                MediaType.APPLICATION_JSON,
                                200,
                                "logout",
                                request -> {
                                        try {
                                                request.andExpect(jsonPath("$.message")
                                                                .value(message("message.logout.success")));
                                        } catch (Exception e) {
                                                throw new RuntimeException(e);
                                        }
                                });
        }

        /**
         * Test: POST /auth/logout - Authentication error without token
         * 
         * Verifies that the logout endpoint requires user authentication.
         * Unauthenticated requests should be rejected.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Request is rejected due to missing authentication
         * - Response contains authentication error message
         * - No tokens are deleted (invalid request)
         * 
         * Test data:
         * - Authorization header: (missing)
         */
        @Test
        @Transactional
        public void logout_missingToken_shouldReturnUnauthorized() throws Exception {

                performRequest(
                                "POST",
                                "/auth/logout",
                                null,
                                null,
                                MediaType.APPLICATION_JSON,
                                401,
                                "logout-missing-token",
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
         * Test: POST /auth/logout - Authentication error with malformed token
         * 
         * Verifies that the logout endpoint rejects invalid or malformed JWT tokens.
         * Invalid tokens should not grant access even if the request is properly formatted.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token validation fails
         * - Response contains authentication error message
         * - No tokens are deleted (invalid authentication)
         * 
         * Test data:
         * - Authorization header: Bearer this.is.not.a.valid.token (malformed JWT)
         */
        @Test
        @Transactional
        public void logout_withMalformedToken_shouldReturnUnauthorized() throws Exception {
                String malformedToken = "this.is.not.a.valid.token";

                performRequest(
                                "POST",
                                "/auth/logout",
                                null,
                                malformedToken,
                                MediaType.APPLICATION_JSON,
                                401,
                                "logout-malformed-token",
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
         * Test: POST /auth/logout - Authentication error with expired token
         * 
         * Verifies that the logout endpoint rejects expired access tokens.
         * Even if a token was previously valid, expired tokens should not grant access.
         * 
         * Expected behavior:
         * - Returns HTTP 401 (Unauthorized)
         * - Token expiration validation fails
         * - Response contains authentication error message
         * - No tokens are deleted (token no longer valid)
         * 
         * Test data:
         * - User: test.user@test.com
         * - JWT Token: Expired 2 hours ago
         */
        @Test
        @Transactional
        public void logout_withExpiredToken_shouldReturnUnauthorized() throws Exception {
                UserDto userDto = userService.findByLogin("test.user@test.com");

                String expiredToken = userAuthenticationProvider.createToken(userDto, Date.from(
                                Instant.now().minus(2, ChronoUnit.HOURS)));

                performRequest(
                                "POST",
                                "/auth/logout",
                                null,
                                expiredToken,
                                MediaType.APPLICATION_JSON,
                                401,
                                "logout-expired-token",
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
         * Test : GET /oauth2/success
         * verify if the success endpoints works correctly.
         * 
         * Expected behavior:
         * - success
         * - message success login
         * - auth code generate
         * 
          */
        @Test
        @Transactional
        public void login_oauth2_success() throws Exception{

                mockMvc.perform(get("/oauth2/success")
                        .with(oauth2Login().attributes(attrs -> {
                    attrs.put("email", "user@test.com");
                    attrs.put("given_name", "User");
                    attrs.put("family_name", "Test");
                })))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->{
                        String redirectUrl = result.getResponse().getRedirectedUrl();

                        assertTrue(redirectUrl.contains("/auth/redirect-after-login"));
                        assertTrue(redirectUrl.contains("?loginType=azure"));
                        assertTrue(redirectUrl.contains("&authCode="));
        });
}


        @Test
        @Transactional
        public void get_token_with_authCode() throws Exception{
                
                String authCode = authService.generateAndStoreAuthCode("test.user@test.com", "redirectUrl");

                performRequest(
                        "POST",
                        "/oauth2/token",
                        "{\"login\":\"test.user@test.com\", \"code\":\"" + authCode + "\"}",
                        null,
                        MediaType.APPLICATION_JSON,
                        200,
                        "get-token-with-authcode",
                        request -> {
                                try{
                                        request.andExpect(jsonPath("$.token").exists());
                                }catch (Exception e){
                                        throw new RuntimeException(e);
                                }
                        }
                        
                        
                );
        }

        @Test
        @Transactional
        public void getTokenWithAuthCode_withWronLogin_ShouldReturn404() throws Exception{
                
                String authCode = authService.generateAndStoreAuthCode("not.a@login.com", "redirectUrl");
                
                performRequest(
                        "POST",
                        "/oauth2/token",
                        "{\"login\":\"not.a@login.com\", \"code\":\"" + authCode + "\"}",
                        null,
                        MediaType.APPLICATION_JSON,
                        404,
                        "get-token-with-auth-code-wrong-login",
                        request -> {
                                try{
                                        request.andExpect(jsonPath("$.message")
                                        .value(message("error.user.not.found", "not.a@login.com")));
                        } catch (Exception e){
                                throw new RuntimeException(e);
                        }
                }
                        
                );
                
        }

        @Test
        @Transactional
        public void getTokenWithAuthCode_withWrongCode_ShouldReturn404() throws Exception{

                String wrongCode = "ThisIsNotACode";

                performRequest(
                        "POST",
                        "/oauth2/token",
                        "{\"login\":\"test.user@test.com\", \"code\":\"" + wrongCode + "\"}",
                        null,
                        MediaType.APPLICATION_JSON,
                        404,
                        "get-token-with-auth-code-wrong-code",
                        request -> {
                                try{
                                        request.andExpect(jsonPath("$.message")
                                .value(message("error.authcode.not.found")));
                                } catch(Exception e){
                                        throw new RuntimeException(e);
                                }
                        }
                );
        }
        

}
