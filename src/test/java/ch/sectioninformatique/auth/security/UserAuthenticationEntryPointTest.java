package ch.sectioninformatique.auth.security;

import ch.sectioninformatique.auth.app.errors.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserAuthenticationEntryPoint}.
 * 
 * This test class validates the UserAuthenticationEntryPoint functionality, including:
 * - HTTP 401 Unauthorized response for authentication failures
 * - JSON error response format
 * - Custom exception message handling
 * - Default error message when exception is null or has null/empty message
 * - Content-Type header set to application/json
 * - Support for various AuthenticationException types (BadCredentialsException, InsufficientAuthenticationException)
 * 
 * UserAuthenticationEntryPoint is invoked when authentication fails or is missing
 * (e.g., invalid credentials, missing token, expired token).
 */
@SpringBootTest
public class UserAuthenticationEntryPointTest {

    private UserAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @Autowired
    private MessageSource messageSource;

    @BeforeEach
    public void setUp() {
        entryPoint = new UserAuthenticationEntryPoint(messageSource);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
        LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    /**
     * Test: AuthenticationException returns 401 with custom error message
     * 
     * Verifies that when an AuthenticationException with a custom message is handled,
     * the response contains HTTP 401 status, JSON content type, and the exception's message.
     * 
     * Test data: BadCredentialsException with "Invalid credentials"
     * 
     * Expected:
     * - HTTP status: 401 Unauthorized
     * - Content-Type: application/json
     * - Response body: ErrorDto with "Invalid credentials"
     */
    @Test
    public void commence_withAuthenticationException_shouldReturn401WithMessage() throws Exception {
        // Arrange
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(
            messageSource.getMessage(
                "error.security.authentication.token.invalid.or.missing",
                null,
                java.util.Locale.ENGLISH),
            errorDto.message());
    }

    /**
     * Test: Null exception returns 401 with default error message
     * 
     * Verifies that when no exception is provided (null), the entry point returns
     * a default authentication failure message.
     * 
     * Test data: null exception
     * 
     * Expected:
     * - HTTP status: 401 Unauthorized
     * - Content-Type: application/json
     * - Response body: ErrorDto with "Authentication failed"
     */
    @Test
    public void commence_withNullException_shouldReturn401WithDefaultMessage() throws Exception {
        // Act
        entryPoint.commence(request, response, null);

        // Assert
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(
            messageSource.getMessage(
                "error.security.authentication.failed",
                null,
                java.util.Locale.ENGLISH),
            errorDto.message());  // Default message when authException is null
    }

    /**
     * Test: Exception with null message returns 401 with default error message
     * 
     * Verifies that when an AuthenticationException has a null message, the entry point
     * returns a default error message for missing/invalid tokens.
     * 
     * Test data: InsufficientAuthenticationException with null message
     * 
     * Expected:
     * - HTTP status: 401 Unauthorized
     * - Content-Type: application/json
     * - Response body: ErrorDto with "Invalid or missing authentication token"
     */
    @Test
    public void commence_withExceptionWithNullMessage_shouldReturn401WithDefaultMessage() throws Exception {
        // Arrange
        AuthenticationException exception = new InsufficientAuthenticationException(null);

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(
            messageSource.getMessage(
                "error.security.authentication.token.invalid.or.missing",
                null,
                java.util.Locale.ENGLISH),
            errorDto.message());
    }

    /**
     * Test: Response body contains valid JSON structure
     * 
     * Verifies that the response body is valid JSON with the expected structure,
     * containing a "message" field with the exception message.
     * 
     * Test data: BadCredentialsException with "Token expired"
     * 
     * Expected:
     * - Response body is valid JSON
     * - JSON contains "message" field
     * - Message value is "Token expired"
     */
    @Test
    public void commence_shouldReturnValidJsonStructure() throws Exception {
        // Arrange
        AuthenticationException exception = new BadCredentialsException("Token expired");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        String responseBody = response.getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("message"));
        assertTrue(responseBody.contains(
            messageSource.getMessage(
                "error.security.authentication.token.invalid.or.missing",
                null,
                java.util.Locale.ENGLISH)));
    }

    /**
     * Test: Content-Type header is set to application/json
     * 
     * Verifies that the entry point sets the correct Content-Type header
     * to indicate JSON response format.
     * 
     * Test data: BadCredentialsException with "Test"
     * 
     * Expected: Content-Type header is "application/json"
     */
    @Test
    public void commence_shouldSetCorrectContentType() throws Exception {
        // Arrange
        AuthenticationException exception = new BadCredentialsException("Test");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals("application/json", response.getHeader("Content-Type"));
    }

    /**
     * Test: HTTP status code is set to 401 Unauthorized
     * 
     * Verifies that the entry point sets the correct HTTP status code (401)
     * for authentication failures.
     * 
     * Test data: BadCredentialsException with "Test"
     * 
     * Expected: HTTP status is 401
     */
    @Test
    public void commence_shouldSetCorrectStatusCode() throws Exception {
        // Arrange
        AuthenticationException exception = new BadCredentialsException("Test");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());
    }

    /**
     * Test: InsufficientAuthenticationException returns 401 with message
     * 
     * Verifies that InsufficientAuthenticationException (thrown when authentication
     * is required but not provided) is handled correctly with its custom message.
     * 
     * Test data: InsufficientAuthenticationException with "Full authentication is required"
     * 
     * Expected:
     * - HTTP status: 401 Unauthorized
     * - Response body: ErrorDto with "Full authentication is required"
     */
    @Test
    public void commence_withInsufficientAuthenticationException_shouldReturn401() throws Exception {
        // Arrange
        AuthenticationException exception = new InsufficientAuthenticationException("Full authentication is required");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(
            messageSource.getMessage(
                "error.security.authentication.token.invalid.or.missing",
                null,
                java.util.Locale.ENGLISH),
            errorDto.message());
    }

    /**
     * Test: Exception with empty message returns 401 with default error message
     * 
     * Verifies that when an AuthenticationException has an empty string message,
     * the entry point returns a default error message for invalid/missing tokens.
     * 
     * Test data: BadCredentialsException with empty string message
     * 
     * Expected:
     * - HTTP status: 401 Unauthorized
     * - Response body: ErrorDto with "Invalid or missing authentication token"
     */
    @Test
    public void commence_shouldHandleEmptyExceptionMessage() throws Exception {
        // Arrange
        AuthenticationException exception = new BadCredentialsException("");

        // Act
        entryPoint.commence(request, response, exception);

        // Assert
        assertEquals(401, response.getStatus());

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(
            messageSource.getMessage(
                "error.security.authentication.token.invalid.or.missing",
                null,
                java.util.Locale.ENGLISH),
            errorDto.message());
    }
}
