package ch.sectioninformatique.auth.user.dto;

import ch.sectioninformatique.auth.role.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body of {@code POST /users}.
 *
 * Names must start and end with a letter and may contain letters, spaces, hyphens
 * and apostrophes (e.g. "Jean-Pierre", "O'Connor", "Ana Lucía").
 *
 * @param firstName first name
 * @param lastName  last name
 * @param login     email used as login
 * @param password  password, 8 to 72 characters; wiped from memory once hashed
 * @param mainRole  role of the new user, USER when omitted
 */
public record CreateUserDto(
        @NotBlank @Pattern(regexp = NAME_PATTERN) String firstName,

        @NotBlank(message = "{validation.signup.lastName.required}") @Pattern(regexp = NAME_PATTERN) String lastName,

        @NotBlank @Email String login,

        @NotNull @Size(min = 8, max = 72) char[] password,

        RoleEnum mainRole) {

    /** Letters, inner spaces, hyphens and apostrophes; must start and end with a letter. */
    public static final String NAME_PATTERN = "^[\\p{L}][\\p{L} '\\-]*[\\p{L}]$";

    /**
     * @return the requested role, or USER if none was given
     */
    public RoleEnum mainRoleOrDefault() {
        return mainRole != null ? mainRole : RoleEnum.USER;
    }
}
