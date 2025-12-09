package ch.sectioninformatique.auth.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SignUpDto} validation.
 * 
 * This test class validates the SignUpDto validation constraints, including:
 * - First name and last name requirements and format validation
 * - Email format validation (must be valid email address)
 * - Password length requirements (8-72 characters)
 * - Support for international characters (Unicode) in names
 * - Special character handling in names (hyphens, apostrophes)
 * - Null and blank field handling
 * 
 * SignUpDto is used for user registration requests.
 */
public class SignUpDtoTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Test: Valid sign-up data passes validation
     * 
     * Verifies that a SignUpDto with all valid fields passes validation
     * without any constraint violations.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Password123!" (12 chars)
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withValidData_shouldPassValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "Valid sign-up data should have no violations");
    }

    /**
     * Test: Names with hyphens and apostrophes pass validation
     * 
     * Verifies that first and last names containing special characters
     * commonly used in names (hyphens, apostrophes) are accepted as valid.
     * 
     * Test data:
     * - firstName: "Jean-Pierre" (contains hyphen)
     * - lastName: "O'Connor" (contains apostrophe)
     * - login: "jp.oconnor@example.com"
     * - password: "Password123!"
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withValidComplexNames_shouldPassValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "Jean-Pierre",
            "O'Connor",
            "jp.oconnor@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "Names with hyphens and apostrophes should be valid");
    }

    /**
     * Test: Names with Unicode characters pass validation
     * 
     * Verifies that first and last names containing international/accented
     * characters are accepted as valid, supporting non-ASCII names.
     * 
     * Test data:
     * - firstName: "François" (contains ç)
     * - lastName: "Müller" (contains ü)
     * - login: "francois.muller@example.com"
     * - password: "Password123!"
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withUnicodeNames_shouldPassValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "François",
            "Müller",
            "francois.muller@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "Unicode characters in names should be valid");
    }

    /**
     * Test: Blank first name fails validation
     * 
     * Verifies that a SignUpDto with a blank/empty first name is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "" (empty string)
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "First name is required"
     */
    @Test
    public void signUpDto_withBlankFirstName_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "",
            "Doe",
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("First name is required")));
    }

    /**
     * Test: Blank last name fails validation
     * 
     * Verifies that a SignUpDto with a blank/empty last name is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "" (empty string)
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "Last name is required"
     */
    @Test
    public void signUpDto_withBlankLastName_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "",
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Last name is required")));
    }

    /**
     * Test: First name with invalid characters fails validation
     * 
     * Verifies that a first name containing invalid characters (numbers)
     * is rejected with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John123" (contains numbers)
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "invalid characters"
     */
    @Test
    public void signUpDto_withInvalidFirstNameCharacters_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John123",  // Contains numbers
            "Doe",
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("invalid characters")));
    }

    /**
     * Test: Last name with invalid characters fails validation
     * 
     * Verifies that a last name containing invalid characters (@ symbol)
     * is rejected with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe@" (contains @)
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "invalid characters"
     */
    @Test
    public void signUpDto_withInvalidLastNameCharacters_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe@",  // Contains @
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("invalid characters")));
    }

    /**
     * Test: Invalid email format fails validation
     * 
     * Verifies that a login field with invalid email format is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "not-an-email" (invalid email format)
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "valid email"
     */
    @Test
    public void signUpDto_withInvalidEmail_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "not-an-email",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("valid email")));
    }

    /**
     * Test: Blank login/email fails validation
     * 
     * Verifies that a SignUpDto with a blank/empty login field is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "" (empty string)
     * - password: "Password123!"
     * 
     * Expected: Validation error containing "Login is required"
     */
    @Test
    public void signUpDto_withBlankLogin_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Login is required")));
    }

    /**
     * Test: Null password fails validation
     * 
     * Verifies that a SignUpDto with null password is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: null
     * 
     * Expected: Validation error containing "Password is required"
     */
    @Test
    public void signUpDto_withNullPassword_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            null
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password is required")));
    }

    /**
     * Test: Password shorter than 8 characters fails validation
     * 
     * Verifies that a password with less than 8 characters is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Pass1!" (only 6 characters)
     * 
     * Expected: Validation error containing "between 8 and 72"
     */
    @Test
    public void signUpDto_withPasswordTooShort_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            "Pass1!".toCharArray()  // Only 6 characters
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("between 8 and 72")));
    }

    /**
     * Test: Password longer than 72 characters fails validation
     * 
     * Verifies that a password exceeding 72 characters is rejected
     * with appropriate validation error message.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: 73-character string
     * 
     * Expected: Validation error containing "between 8 and 72"
     */
    @Test
    public void signUpDto_withPasswordTooLong_shouldFailValidation() {
        // Arrange
        String longPassword = "A1!".concat("a".repeat(70));  // 73 characters
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            longPassword.toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertEquals(1, violations.size());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("between 8 and 72")));
    }

    /**
     * Test: Password at minimum length (8 chars) passes validation
     * 
     * Verifies that a password with exactly 8 characters (the minimum)
     * is accepted as valid.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Pass123!" (exactly 8 characters)
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withMinimumPasswordLength_shouldPassValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            "Pass123!".toCharArray()  // Exactly 8 characters
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "8-character password should be valid");
    }

    /**
     * Test: Password at maximum length (72 chars) passes validation
     * 
     * Verifies that a password with exactly 72 characters (the maximum)
     * is accepted as valid.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: 72-character string
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withMaximumPasswordLength_shouldPassValidation() {
        // Arrange
        String maxPassword = "A1!".concat("a".repeat(69));  // Exactly 72 characters
        SignUpDto signUp = new SignUpDto(
            "John",
            "Doe",
            "john.doe@example.com",
            maxPassword.toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "72-character password should be valid");
    }

    /**
     * Test: Multiple validation errors are all reported
     * 
     * Verifies that when a SignUpDto has multiple invalid fields,
     * all constraint violations are detected and returned.
     * 
     * Test data (all invalid):
     * - firstName: "" (blank)
     * - lastName: "Doe123" (contains numbers)
     * - login: "not-email" (invalid email format)
     * - password: "short" (only 5 characters)
     * 
     * Expected: At least 3 validation violations
     */
    @Test
    public void signUpDto_withMultipleViolations_shouldReturnAllViolations() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "",           // Blank first name
            "Doe123",     // Invalid last name
            "not-email",  // Invalid email
            "short".toCharArray()  // Too short password
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.size() >= 3, "Should have at least 3 violations");
    }

    /**
     * Test: Record accessor methods return correct values
     * 
     * Verifies that SignUpDto (as a Java record) correctly implements
     * accessor methods that return the values passed to the constructor.
     * 
     * Test data:
     * - firstName: "John"
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: All accessor methods return their corresponding constructor arguments
     */
    @Test
    public void signUpDto_recordMethods_shouldWorkCorrectly() {
        // Arrange
        String firstName = "John";
        String lastName = "Doe";
        String email = "john.doe@example.com";
        char[] password = "Password123!".toCharArray();
        SignUpDto signUp = new SignUpDto(firstName, lastName, email, password);

        // Assert
        assertEquals(firstName, signUp.firstName());
        assertEquals(lastName, signUp.lastName());
        assertEquals(email, signUp.login());
        assertArrayEquals(password, signUp.password());
    }

    /**
     * Test: Name containing only spaces fails validation
     * 
     * Verifies that a first name consisting only of whitespace characters
     * is treated as blank and rejected by validation.
     * 
     * Test data:
     * - firstName: "   " (only spaces)
     * - lastName: "Doe"
     * - login: "john.doe@example.com"
     * - password: "Password123!"
     * 
     * Expected: Validation violation (first name required)
     */
    @Test
    public void signUpDto_withNameContainingOnlySpaces_shouldFailValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "   ",
            "Doe",
            "john.doe@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertFalse(violations.isEmpty());
    }

    /**
     * Test: Names with internal spaces pass validation
     * 
     * Verifies that first and last names containing spaces (multi-word names)
     * are accepted as valid.
     * 
     * Test data:
     * - firstName: "Mary Anne" (contains space)
     * - lastName: "De La Cruz" (contains spaces)
     * - login: "mary.delacruz@example.com"
     * - password: "Password123!"
     * 
     * Expected: No validation violations
     */
    @Test
    public void signUpDto_withNameWithSpaces_shouldPassValidation() {
        // Arrange
        SignUpDto signUp = new SignUpDto(
            "Mary Anne",
            "De La Cruz",
            "mary.delacruz@example.com",
            "Password123!".toCharArray()
        );

        // Act
        Set<ConstraintViolation<SignUpDto>> violations = validator.validate(signUp);

        // Assert
        assertTrue(violations.isEmpty(), "Names with spaces should be valid");
    }
}
