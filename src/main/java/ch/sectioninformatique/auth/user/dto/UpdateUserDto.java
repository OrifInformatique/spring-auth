package ch.sectioninformatique.auth.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body of {@code PUT /users/{login}}.
 *
 * Only identity fields can be updated here: the role has its own endpoint
 * ({@code PUT /users/{login}/role}) with stricter authorization rules, and the
 * password can only be changed by its owner.
 *
 * @param firstName new first name
 * @param lastName  new last name, may be null (Azure accounts without last name)
 * @param login     new email used as login
 */
public record UpdateUserDto(
        @NotBlank @Pattern(regexp = CreateUserDto.NAME_PATTERN) String firstName,

        @Pattern(regexp = CreateUserDto.NAME_PATTERN) String lastName,

        @NotBlank @Email String login) {
}
