package ch.sectioninformatique.auth.user.dto;

import ch.sectioninformatique.auth.user.validation.PasswordNotReused;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code PUT /users/me/password}.
 *
 * Passwords are {@code char[]} so that they can be wiped from memory after use.
 *
 * @param oldPassword current password, checked before the update
 * @param newPassword new password, 8 to 72 characters, different from the current one
 */
@PasswordNotReused
public record PasswordUpdateDto(
        @NotNull char[] oldPassword,
        @NotNull @Size(min = 8, max = 72) char[] newPassword) {
}
