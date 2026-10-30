package ch.sectioninformatique.auth.role;

import static ch.sectioninformatique.auth.role.PermissionEnum.USER_DELETE;
import static ch.sectioninformatique.auth.role.PermissionEnum.USER_READ;
import static ch.sectioninformatique.auth.role.PermissionEnum.USER_UPDATE;
import static ch.sectioninformatique.auth.role.PermissionEnum.USER_WRITE;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Roles of the application and the permissions they grant.
 *
 * Roles are hierarchical: each role has every permission of the previous one.
 * - USER: can read users
 * - MANAGER: can also create and update users
 * - ADMIN: can also delete users, and is the only role allowed to act on admin accounts
 */
public enum RoleEnum {

    USER("Default user role", EnumSet.of(USER_READ)),

    MANAGER("Manager role", EnumSet.of(USER_READ, USER_WRITE, USER_UPDATE)),

    ADMIN("Administrator role", EnumSet.of(USER_READ, USER_WRITE, USER_UPDATE, USER_DELETE));

    private final String description;
    private final Set<PermissionEnum> permissions;

    RoleEnum(String description, Set<PermissionEnum> permissions) {
        this.description = description;
        this.permissions = permissions;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return an unmodifiable view of the permissions granted by this role
     */
    public Set<PermissionEnum> getPermissions() {
        return Set.copyOf(permissions);
    }

    /**
     * Converts the role into Spring Security authorities: one authority per
     * permission (e.g. "user:read") plus the role itself (e.g. "ROLE_ADMIN").
     *
     * @return the authorities granted by this role
     */
    public Set<SimpleGrantedAuthority> getGrantedAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getPermission())));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + name()));
        return authorities;
    }
}
