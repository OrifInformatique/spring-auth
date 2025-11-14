package ch.sectioninformatique.auth.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for user credentials.
 * This record holds the login and password used for authentication.
 *
 * @param login    The user's login identifier (email)
 * @param password The user's password as a character array
 */
public record CredentialsDto(
    @NotBlank(message = "Login is required")
    @Email(message = "Invalid email format")
    String login,

    @NotNull(message = "Password is required")
    char[] password
) {}