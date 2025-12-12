package ch.sectioninformatique.auth.user;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import java.util.Arrays;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.GrantedAuthority;

import ch.sectioninformatique.auth.auth.SignUpDto;

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
     * By convention for MapStruct, the interface declares a member INSTANCE,
     * providing clients access to the mapper implementation.
     */
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

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
    @Mapping(target = "mainRole", expression = "java(user.getMainRole().getName().name())")
    @Mapping(target = "permissions", source = "authorities", qualifiedByName = "authoritiesToPermissions")
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "login", source = "login")
    @Mapping(target = "deleted", source = "deleted")
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
    @Mapping(target = "mainRole", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
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

    /**
     * After mapping from SignUpDto to User, clear sensitive data held in the
     * source object (the password char[]). This reduces the window of time the
     * raw password is present in memory and follows the good practice of
     * explicitly zeroing-out sensitive arrays after use.
     *
     * Note: SignUpDto stores the password as a mutable `char[]` to allow this
     * clearing; we mutate the array in-place to avoid creating additional
     * String objects containing the password.
     */
    @AfterMapping
    default void clearPasswordAfterMapping(SignUpDto source, @MappingTarget User target) {
        if (source == null) return;
        char[] pwd = source.password();
        if (pwd != null) {
            Arrays.fill(pwd, '\0');
        }
    }
}
