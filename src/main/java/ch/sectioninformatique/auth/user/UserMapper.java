package ch.sectioninformatique.auth.user;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.security.core.GrantedAuthority;

import ch.sectioninformatique.auth.user.dto.CreateUserDto;
import ch.sectioninformatique.auth.user.dto.UserDto;

/**
 * Conversions between {@link User} entities and DTOs, implemented by MapStruct at build
 * time and exposed as a Spring bean ({@code componentModel = "spring"}).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a user entity to its API representation. The role becomes its name,
     * the authorities become permission strings, and the token is left empty.
     *
     * @param user entity to convert
     * @return the corresponding DTO
     */
    @Mapping(target = "mainRole", expression = "java(user.getMainRole() != null ? user.getMainRole().getName().name() : null)")
    @Mapping(target = "permissions", source = "authorities", qualifiedByName = "authoritiesToPermissions")
    @Mapping(target = "token", ignore = true)
    UserDto toUserDto(User user);

    /**
     * Creates a user entity from a creation request. The password, role, id and
     * timestamps are set by the service or the database, not copied from the request.
     *
     * @param createUserDto creation request
     * @return a new, unsaved user
     */
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "mainRole", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toUser(CreateUserDto createUserDto);

    /**
     * @param authorities Spring Security authorities
     * @return authority names, e.g. ["user:read", "ROLE_USER"]
     */
    @Named("authoritiesToPermissions")
    default List<String> authoritiesToPermissions(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return List.of();
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
    }
}
