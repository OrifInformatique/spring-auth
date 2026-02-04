package ch.sectioninformatique.auth.security;

import ch.sectioninformatique.auth.app.errors.ErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CustomAccessDeniedHandler}.
 * 
 * This test class validates the CustomAccessDeniedHandler functionality, including:
 * - HTTP 403 Forbidden response for access denied scenarios
 * - JSON error response format
 * - Custom exception message handling
 * - Default error message when exception is null or has null message
 * - Content-Type header set to application/json
 * 
 * CustomAccessDeniedHandler is invoked when an authenticated user attempts
 * to access a resource they don't have permission for.
 */
@SpringBootTest
public class CustomAccessDeniedHandlerTest {

    private CustomAccessDeniedHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @Autowired
    private MessageSource messageSource;

    @BeforeEach
    public void setUp() {
        handler = new CustomAccessDeniedHandler(messageSource);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
        LocaleContextHolder.setLocale(java.util.Locale.ENGLISH);
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private String message(String key, Object... args) {
        return messageSource.getMessage(key, args, java.util.Locale.ENGLISH);
    }

    /**
    * Test: AccessDeniedException returns 403 with error.security.access.denied message
    * 
    * Verifies that when an AccessDeniedException is handled, the response contains
    * HTTP 403 status, JSON content type, and the error.security.access.denied message.
    * 
    * Test data: AccessDeniedException with a non-localized message
    * 
    * Expected:
    * - HTTP status: 403 Forbidden
    * - Content-Type: application/json
    * - Response body: ErrorDto with the error.security.access.denied message
     */
    @Test
    public void handle_withAccessDeniedException_shouldReturn403WithMessage() throws Exception {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Custom access denied message");

        // Act
        handler.handle(request, response, exception);

        // Assert
        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(message("error.security.access.denied"), errorDto.message());
    }

    /**
     * Test: Null exception returns 403 with default error message
     * 
     * Verifies that when no exception is provided (null), the handler returns
     * a default error message instead of failing.
     * 
     * Test data: null exception
     * 
     * Expected:
     * - HTTP status: 403 Forbidden
     * - Content-Type: application/json
    * - Response body: ErrorDto with the error.security.access.denied message
     */
    @Test
    public void handle_withNullException_shouldReturn403WithDefaultMessage() throws Exception {
        // Act
        handler.handle(request, response, null);

        // Assert
        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(message("error.security.access.denied"), errorDto.message());
    }

    /**
     * Test: Exception with null message returns 403 with default error message
     * 
     * Verifies that when an AccessDeniedException has a null message, the handler
     * returns a default error message instead of propagating the null.
     * 
     * Test data: AccessDeniedException with null message
     * 
     * Expected:
     * - HTTP status: 403 Forbidden
     * - Content-Type: application/json
    * - Response body: ErrorDto with the error.security.access.denied message
     */
    @Test
    public void handle_withExceptionWithNullMessage_shouldReturn403WithDefaultMessage() throws Exception {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException(null);

        // Act
        handler.handle(request, response, exception);

        // Assert
        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getHeader("Content-Type"));

        ErrorDto errorDto = objectMapper.readValue(response.getContentAsString(), ErrorDto.class);
        assertEquals(message("error.security.access.denied"), errorDto.message());
    }

    /**
    * Test: Response body contains valid JSON structure
    * 
    * Verifies that the response body is valid JSON with the expected structure,
    * containing a "message" field with the error.security.access.denied message.
    * 
    * Test data: AccessDeniedException with a non-localized message
    * 
    * Expected:
    * - Response body is valid JSON
    * - JSON contains "message" field
    * - Message value is the error.security.access.denied message
     */
    @Test
    public void handle_shouldReturnValidJsonStructure() throws Exception {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Insufficient permissions");

        // Act
        handler.handle(request, response, exception);

        // Assert
        String responseBody = response.getContentAsString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("message"));
        assertTrue(responseBody.contains(message("error.security.access.denied")));
    }

    /**
     * Test: Content-Type header is set to application/json
     * 
     * Verifies that the handler sets the correct Content-Type header
     * to indicate JSON response format.
     * 
     * Test data: AccessDeniedException with "Test"
     * 
     * Expected: Content-Type header is "application/json"
     */
    @Test
    public void handle_shouldSetCorrectContentType() throws Exception {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Test");

        // Act
        handler.handle(request, response, exception);

        // Assert
        assertEquals("application/json", response.getHeader("Content-Type"));
    }

    /**
     * Test: HTTP status code is set to 403 Forbidden
     * 
     * Verifies that the handler sets the correct HTTP status code (403)
     * for access denied scenarios.
     * 
     * Test data: AccessDeniedException with "Test"
     * 
     * Expected: HTTP status is 403
     */
    @Test
    public void handle_shouldSetCorrectStatusCode() throws Exception {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("Test");

        // Act
        handler.handle(request, response, exception);

        // Assert
        assertEquals(403, response.getStatus());
    }
}
