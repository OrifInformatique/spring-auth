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
        @NotNull(message = "Current password is required for verification")
        char[] oldPassword,

        @NotNull(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        char[] newPassword
) {}