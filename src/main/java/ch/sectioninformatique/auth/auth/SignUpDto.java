package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for user sign-up information.
 * This record encapsulates the data required for user registration,
 * including first name, last name, login (email), and password.
 * Validation annotations ensure that the provided data meets
 * necessary constraints.
 */
public record SignUpDto(
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    @NotBlank(message = "Login is required")
    @Email(message = "Login must be a valid email")
    String login,

    @NotNull(message = "Password is required")
    char[] password
) {}
