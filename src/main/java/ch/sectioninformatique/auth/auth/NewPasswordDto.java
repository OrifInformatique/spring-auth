package ch.sectioninformatique.auth.auth;

import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) representing a new password input from the client.
 * 
 * This DTO encapsulates the new password as a char array for security reasons:
 * - Using `char[]` instead of `String` reduces the risk of passwords lingering
 *   in immutable memory (String pool), which can be accessed in memory dumps.
 * 
 * Validation:
 * - `@NotNull` ensures that a password must be provided in the request body.
 */
public record NewPasswordDto (@NotNull char[] newPassword) {}