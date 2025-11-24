package ch.sectioninformatique.auth.app.exceptions;

import ch.sectioninformatique.auth.security.RoleEnum;

public class RoleNotFoundException extends AppException {
    public RoleNotFoundException(RoleEnum role) {
        super("Role not found: " + role.name());
    }
}
