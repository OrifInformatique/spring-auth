package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) used to update a user's password.
 * 
 * This DTO encapsulates the new password as a char array for security reasons:
 * - Using `char[]` instead of `String` reduces the risk of passwords lingering
 *   in immutable memory (String pool), which can be accessed in memory dumps.
 * 
 * Validation:
 * - `@NotNull` ensures that a password must be provided in the request body.
 */
public record PasswordUpdateDto (@NotNull char[] oldPassword, @NotNull char[] newPassword) {}