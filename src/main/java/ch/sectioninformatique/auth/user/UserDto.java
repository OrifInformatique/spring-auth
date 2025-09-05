package ch.sectioninformatique.auth.user;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for user information.
 * This class is used to transfer user data between the client and server,
 * excluding sensitive information like passwords. It includes basic user details,
 * authentication token, role, and permissions.
 */
@Data
@Builder(toBuilder = true)
public class UserDto {

    /**
     * Unique identifier for the user.
     */
    private Long id;

    /**
     * User's first name.
     */
    private String firstName;

    /**
     * User's last name.
     */
    private String lastName;

    /**
     * User's unique login identifier (email).
     */
    private String login;

    /**
     * JWT token for user authentication.
     */
    private String token;
    
    /**
     * User's role in the system.
     * Defaults to "ROLE_USER" if not specified.
     */
    @Builder.Default
    private String mainRole = "ROLE_USER";

    /**
     * List of additional app specific roles.
     * Defaults to an empty list if not specified.
     */
    @Builder.Default
    private List<String> appSpecificRoles = new ArrayList<>();
    
    /**
     * List of permissions granted to the user.
     * Defaults to an empty list if not specified.
     */
    @Builder.Default
    private List<String> permissions = new ArrayList<>();
}


/*
 * @JsonIgnore explique que le champ password ne doit pas être inclus dans la réponse JSON
 * @JsonIgnoreProperties(ignoreUnknown = true) explique que les champs non définis dans la classe UserDto ne doivent pas provoquer d'erreur
 */