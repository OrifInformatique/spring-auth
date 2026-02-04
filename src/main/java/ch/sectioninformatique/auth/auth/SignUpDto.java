package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user sign-up information.
 * This record encapsulates the data required for user registration,
 * including first name, last name, login (email), and password.
 * 
 * Validation Rules:
 * - First/Last Name: 2-50 chars, letters/spaces/hyphens/apostrophes only
 * - Login: Valid email format
 * - Password: 8-72 chars, must contain uppercase, lowercase, digit, and special character
 * 
 * @param firstName The user's first name
 * @param lastName  The user's last name
 * @param login     The user's login email
 * @param password  The user's password as a character array
 */
public record SignUpDto(
        @NotBlank(message = "{validation.signup.firstName.required}") 
        @Pattern(
            regexp = "^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$", 
            message = "{validation.signup.firstName.pattern}"
        ) 
        String firstName,

        @NotBlank(message = "{validation.signup.lastName.required}") 
        @Pattern(
            regexp = "^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$", 
            message = "{validation.signup.lastName.pattern}"
        ) 
        String lastName,

        @NotBlank(message = "{validation.signup.login.required}") 
        @Email(message = "{validation.signup.login.email}") 
        String login,

        @NotNull(message = "{validation.signup.password.required}") 
        @Size(min = 8, max = 72, message = "{validation.signup.password.size}")
        char[] password
) {}
