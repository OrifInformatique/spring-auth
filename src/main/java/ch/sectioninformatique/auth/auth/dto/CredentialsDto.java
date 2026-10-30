package ch.sectioninformatique.auth.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Credentials sent to {@code POST /auth/login}.
 *
 * The password is a {@code char[]} so that it can be wiped from memory after use,
 * which is not possible with an immutable String.
 *
 * @param login    user's email
 * @param password user's password, 8 to 72 characters (BCrypt limit)
 */
public record CredentialsDto(
        @NotBlank @Email String login,
        @NotNull @Size(min = 8, max = 72) char[] password) {
}
