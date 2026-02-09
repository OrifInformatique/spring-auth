package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) used to update a user's password.
 * 
 * Security Considerations:
 * - Using `char[]` instead of `String` reduces the risk of passwords lingering
 *   in immutable memory (String pool), which can be accessed in memory dumps.
 * - Old password must be provided for verification
 * - New password must meet complexity requirements
 * - New password cannot be the same as the old password (validated via @PasswordNotReused)
 * 
 */
@PasswordNotReused
public record PasswordUpdateDto(
        @NotNull()
        char[] oldPassword,

        @NotNull()
        @Size(min = 8, max = 72)
        char[] newPassword
) {}