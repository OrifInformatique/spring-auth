package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for user credentials.
 * This record holds the login and password used for authentication.
 *
 * Validation Rules:
 * - Login must be a valid email format
 * - Password must be 8-72 characters long
 *
 * @param login    The user's login identifier (email)
 * @param password The user's password as a character array
 */
public record CredentialsDto(
    @NotBlank(message = "{validation.credentials.login.required}")
    @Email(message = "{validation.credentials.login.email}")
    String login,

    @NotNull(message = "{validation.credentials.password.required}")
    @Size(min = 8, max = 72, message = "{validation.credentials.password.size}")
    char[] password
) {}