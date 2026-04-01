package ch.sectioninformatique.auth.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CredentialsDto} validation.
 * 
 * This test class validates the Bean Validation constraints on CredentialsDto,
 * which is used for user authentication. Tests ensure that:
 * - Email addresses are properly formatted
 * - Passwords meet length requirements (8-72 characters)
 * - Required fields are not blank or null
 * 
 * The tests use Jakarta Bean Validation API to verify constraint violations.
 */
@SpringBootTest(classes = ch.sectioninformatique.auth.AuthApplication.class)
public class CredentialsDtoTest {

    private static final ResourceBundleMessageSource HV_MESSAGES = new ResourceBundleMessageSource();

    static {
        HV_MESSAGES.setBasename("org.hibernate.validator.ValidationMessages");
        HV_MESSAGES.setDefaultEncoding("UTF-8");
    }

    @Autowired
    private Validator validator;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.FRANCE);
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    private boolean hasViolation(Set<ConstraintViolation<CredentialsDto>> violations,
                                 String field,
                                 Class<? extends Annotation> annotationType) {
        return violations.stream().anyMatch(v ->
            v.getPropertyPath().toString().equals(field)
                && v.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationType));
    }

    /**
     * Test: Valid credentials pass all validation
     * 
     * Verifies that a CredentialsDto with valid email and password
     * passes all validation constraints without any violations.
     * 
     * Test data:
     * - Login: test@example.com (valid email format)
     * - Password: password123 (within 8-72 character range)
     */
    @Test
    public void credentialsDto_withValidData_shouldPassValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            "password123".toCharArray()
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertTrue(violations.isEmpty(), "Valid credentials should have no violations");
    }

    /**
     * Test: Invalid email format fails validation
     * 
     * Verifies that the @Email constraint rejects strings that don't
     * match standard email format (missing @, missing domain, etc.).
     * 
     * Test data:
     * - Login: not-an-email (no @ symbol or domain)
     * 
        * Expected: 1 @Email constraint violation on login
     */
    @Test
    public void credentialsDto_withInvalidEmail_shouldFailValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "not-an-email",
            "password123".toCharArray()
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(hasViolation(violations, "login", Email.class));
    }

    /**
     * Test: Blank login fails validation
     * 
     * Verifies that the @NotBlank constraint rejects empty strings
     * for the login field.
     * 
     * Test data:
     * - Login: "" (empty string)
     * - Password: password123 (valid)
     * 
        * Expected: At least 1 @NotBlank constraint violation on login
     */
    @Test
    public void credentialsDto_withBlankLogin_shouldFailValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "",
            "password123".toCharArray()
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(hasViolation(violations, "login", NotBlank.class));
    }

    /**
     * Test: Null password fails validation
     * 
     * Verifies that the @NotNull constraint rejects null password values.
     * Passwords are required for authentication.
     * 
     * Test data:
     * - Login: test@example.com (valid)
     * - Password: null
     * 
        * Expected: 1 @NotNull constraint violation on password
     */
    @Test
    public void credentialsDto_withNullPassword_shouldFailValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            null
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(hasViolation(violations, "password", NotNull.class));
    }

    /**
     * Test: Password shorter than minimum length fails validation
     * 
     * Verifies that the @Size constraint rejects passwords with fewer than
     * 8 characters. This enforces minimum password security requirements.
     * 
     * Test data:
     * - Login: test@example.com (valid)
     * - Password: "short" (5 characters - below minimum)
     * 
        * Expected: 1 @Size constraint violation on password
     */
    @Test
    public void credentialsDto_withPasswordTooShort_shouldFailValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            "short".toCharArray()  // Only 5 characters
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(hasViolation(violations, "password", Size.class));
    }

    /**
     * Test: Password longer than maximum length fails validation
     * 
     * Verifies that the @Size constraint rejects passwords with more than
     * 72 characters. This limit is based on BCrypt's maximum input length.
     * 
     * Test data:
     * - Login: test@example.com (valid)
     * - Password: 73-character string (above maximum)
     * 
        * Expected: 1 @Size constraint violation on password
     */
    @Test
    public void credentialsDto_withPasswordTooLong_shouldFailValidation() {
        // Arrange
        String longPassword = "a".repeat(73);  // 73 characters
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            longPassword.toCharArray()
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(hasViolation(violations, "password", Size.class));
    }

    /**
     * Test: Password with exactly 8 characters passes validation
     * 
     * Verifies that the minimum password length boundary (8 characters)
     * is accepted as valid. This tests the lower bound of the @Size constraint.
     * 
     * Test data:
     * - Login: test@example.com (valid)
     * - Password: "12345678" (exactly 8 characters - minimum)
     * 
     * Expected: No violations
     */
    @Test
    public void credentialsDto_withMinimumPasswordLength_shouldPassValidation() {
        // Arrange
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            "12345678".toCharArray()  // Exactly 8 characters
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertTrue(violations.isEmpty(), "8-character password should be valid");
    }

    /**
     * Test: Password with exactly 72 characters passes validation
     * 
     * Verifies that the maximum password length boundary (72 characters)
     * is accepted as valid. This tests the upper bound of the @Size constraint.
     * 
     * Test data:
     * - Login: test@example.com (valid)
     * - Password: 72-character string (exactly maximum)
     * 
     * Expected: No violations
     */
    @Test
    public void credentialsDto_withMaximumPasswordLength_shouldPassValidation() {
        // Arrange
        String maxPassword = "a".repeat(72);  // Exactly 72 characters
        CredentialsDto credentials = new CredentialsDto(
            "test@example.com",
            maxPassword.toCharArray()
        );

        // Act
        Set<ConstraintViolation<CredentialsDto>> violations = validator.validate(credentials);

        // Assert
        assertTrue(violations.isEmpty(), "72-character password should be valid");
    }

    /**
     * Test: Record accessor methods work correctly
     * 
     * Verifies that CredentialsDto's record accessors (login() and password())
     * correctly return the values provided during construction. This validates
     * the basic record functionality.
     * 
     * Expected behavior:
     * - login() returns the email address provided in constructor
     * - password() returns the exact char array provided in constructor
     * - Values are not modified or transformed
     * 
     * Test data:
     * - Login: test@example.com
     * - Password: password123
     */
    @Test
    public void credentialsDto_recordMethods_shouldWorkCorrectly() {
        // Arrange
        String email = "test@example.com";
        char[] password = "password123".toCharArray();
        CredentialsDto credentials = new CredentialsDto(email, password);

        // Assert
        assertEquals(email, credentials.login());
        assertArrayEquals(password, credentials.password());
    }
}
