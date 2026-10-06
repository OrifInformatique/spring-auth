package ch.sectioninformatique.auth.user.dto;

import ch.sectioninformatique.auth.role.RoleEnum;
import jakarta.validation.constraints.NotNull;

/**
 * Request body of {@code PUT /users/{login}/role}.
 *
 * @param role new role: USER, MANAGER or ADMIN
 */
public record RoleUpdateDto(@NotNull RoleEnum role) {
}
