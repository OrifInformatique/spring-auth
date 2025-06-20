package ch.sectioninformatique.template.user;

import org.springframework.security.core.GrantedAuthority;

import ch.sectioninformatique.template.auth.SignUpDto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting between User entities and DTOs.
 * This interface uses MapStruct to generate implementation classes for mapping
 * between different user-related objects, including:
 * - User entity to UserDto conversion
 * - SignUpDto to User entity conversion
 * - Authorities to permissions conversion
 */
@Mapper(componentModel = "spring")
public interface UserMapper { 

    /**
     * Converts a User entity to a UserDto.
     * This method:
     * - Maps basic user properties (id, firstName, lastName, login)
     * - Converts the role to a "ROLE_" prefixed string
     * - Converts authorities to a list of permission strings
     * - Ignores the token field (handled separately)
     *
     * @param user The User entity to convert
     * @return A UserDto containing the user's information
     */
    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "permissions", source = "authorities", qualifiedByName = "authoritiesToPermissions")
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "login", source = "login")
    UserDto toUserDto(User user);

    /**
     * Converts a SignUpDto to a User entity.
     * This method:
     * - Maps basic user properties from the signup data
     * - Ignores password and roles (these are handled separately)
     * - Ignores ID and timestamps (these are set by the system)
     *
     * @param signUpDto The SignUpDto containing user registration data
     * @return A new User entity with the signup information
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User signUpToUser(SignUpDto signUpDto);

    /**
     * Converts a collection of GrantedAuthority objects to a list of permission strings.
     * This method is used to transform Spring Security authorities into a format
     * suitable for the UserDto.
     *
     * @param authorities Collection of GrantedAuthority objects to convert
     * @return List of permission strings, or null if authorities is null
     */
    @Named("authoritiesToPermissions")
    default List<String> authoritiesToPermissions(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) return null;
        return authorities.stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.toList());
    }
}
