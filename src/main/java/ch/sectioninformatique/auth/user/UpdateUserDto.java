package ch.sectioninformatique.auth.user;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class UpdateUserDto {
    /**
     * The user first name
     */
    private String firstName;

    /**
     * The user last name
     */
    private String lastName;

    /**
     * The user id
     */
    private Long id;

    /**
     * The user login
     */
    private String login;

    /**
     * The user main role
     */
    @Builder.Default
    private String mainRole = "USER";

    /**
     *  The user permissions
     */
    @Builder.Default
    private List<String> permissions = new ArrayList<>();

    /**
     * The user password
     */
    private char[] password;

}
