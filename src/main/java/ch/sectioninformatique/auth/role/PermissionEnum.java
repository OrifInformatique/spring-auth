package ch.sectioninformatique.auth.role;

/**
 * Fine-grained permissions granted by roles.
 *
 * The string value is the Spring Security authority checked by
 * {@code @PreAuthorize("hasAuthority('user:read')")}.
 */
public enum PermissionEnum {
    USER_READ("user:read"),
    USER_WRITE("user:write"),
    USER_UPDATE("user:update"),
    USER_DELETE("user:delete");

    private final String permission;

    PermissionEnum(String permission) {
        this.permission = permission;
    }

    /**
     * @return the authority string, e.g. "user:read"
     */
    public String getPermission() {
        return permission;
    }
}
