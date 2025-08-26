package ch.sectioninformatique.auth.security;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static ch.sectioninformatique.auth.security.PermissionEnum.USER_DELETE;
import static ch.sectioninformatique.auth.security.PermissionEnum.USER_READ;
import static ch.sectioninformatique.auth.security.PermissionEnum.USER_UPDATE;
import static ch.sectioninformatique.auth.security.PermissionEnum.USER_WRITE;

/**
 * Enumeration defining the available roles in the application.
 * Each role has a predefined set of permissions that determine what actions
 * a user with that role can perform. The roles are hierarchical:
 * - USER: Basic access to resources
 * - MANAGER: Extended access to user management
 * - SUPER_ADMIN: Full access to all system features
 */
public enum RoleEnum {
    /**
     * Basic user role with limited permissions.
     * Can only read user information.
     */
    USER(EnumSet.of(
        USER_READ
    )),

    /**
     * Administrator role with extended permissions.
     * Can manage users, but cannot delete them.
     */
    MANAGER(EnumSet.of(
        USER_READ, 
        USER_WRITE, 
        USER_UPDATE
    )),

    /**
     * Super administrator role with full system access.
     * Has all permissions including deletion of users.
     */
    SUPER_ADMIN(EnumSet.of(
        USER_READ, 
        USER_WRITE, 
        USER_UPDATE, 
        USER_DELETE
    ));

    /** Set of permissions associated with this role */
    private final Set<PermissionEnum> permissions;

    /**
     * Constructs a new RoleEnum with the specified permissions.
     *
     * @param permissions The set of permissions to be associated with this role
     */
    RoleEnum(Set<PermissionEnum> permissions) {
        this.permissions = permissions;
    }

    /**
     * Returns the set of permissions associated with this role.
     *
     * @return The set of permissions for this role
     */
    public Set<PermissionEnum> getPermissions() {
        return permissions;
    }

    /**
     * Converts the role's permissions into Spring Security GrantedAuthority objects.
     * This method creates SimpleGrantedAuthority objects for each permission and
     * adds a role-based authority (e.g., "ROLE_USER").
     *
     * @return Set of SimpleGrantedAuthority objects representing the role's permissions
     */
    public Set<SimpleGrantedAuthority> getGrantedAuthorities() {
        Set<SimpleGrantedAuthority> permissions = getPermissions().stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toSet());
        permissions.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return permissions;
    }
}
