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
 * Validation annotations ensure that the provided data meets
 * necessary constraints.
 * 
 * | Part             | What it does                                                            |
 * | ---------------- | ----------------------------------------------------------------------- |
 * | `^[\\p{L}]`      | Starts with a **letter** (no punctuation at start)                      |
 * | `[\\p{L} '\\-]*` | Allows any number of letters, spaces, `'` or `-` in between             |
 * | `[\\p{L}]$`      | Must end with a **letter** (no punctuation at end)                      |
 * | `\\p{L}`         | Matches any **Unicode letter** (accents, international names supported) |
 * 
 * @param firstName The user's first name
 * @param lastName  The user's last name
 * @param login     The user's login email
 * @param password  The user's password as a character array
 */
public record SignUpDto(
        @NotBlank(message = "First name is required") 
        @Pattern(
            regexp = "^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$", 
            message = "First name contains invalid characters"
        ) 
        String firstName,

        @NotBlank(message = "Last name is required") 
        @Pattern(
            regexp = "^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$", 
            message = "Last name contains invalid characters"
        ) 
        String lastName,

        @NotBlank(message = "Login is required") 
        @Email(message = "Login must be a valid email") 
        String login,

        @NotNull(message = "Password is required") 
        @Size(min = 8, max = 72, message = "Password must be between 8 and 20 characters")
        char[] password) {}
