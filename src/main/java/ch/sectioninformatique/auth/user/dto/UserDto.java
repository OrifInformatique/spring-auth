package ch.sectioninformatique.auth.user.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User representation exposed by the API. Never contains the password.
 *
 * It is also the principal of authenticated requests, rebuilt from the access token claims.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;

    private String firstName;

    private String lastName;

    /** User's email, used as login. */
    private String login;

    /** JWT access token, only present in login and token exchange responses. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    /** True if the user is soft-deleted. */
    @Builder.Default
    private boolean deleted = false;

    /** Role name: USER, MANAGER or ADMIN. */
    @Builder.Default
    private String mainRole = "USER";

    /** Authorities granted by the role, e.g. "user:read". */
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
}
