package ch.sectioninformatique.auth.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pins the role/permission matrix: any change to it is a security decision and must
 * be reflected here on purpose.
 */
class RoleEnumTest {

    static Stream<Arguments> permissionMatrix() {
        return Stream.of(
                Arguments.of(RoleEnum.USER, Set.of("user:read")),
                Arguments.of(RoleEnum.MANAGER, Set.of("user:read", "user:write", "user:update")),
                Arguments.of(RoleEnum.ADMIN, Set.of("user:read", "user:write", "user:update", "user:delete")));
    }

    @ParameterizedTest
    @MethodSource("permissionMatrix")
    void grantedAuthorities_arePermissionsPlusRole(RoleEnum role, Set<String> permissions) {
        assertThat(role.getGrantedAuthorities()).extracting("authority")
                .containsExactlyInAnyOrderElementsOf(
                        Stream.concat(permissions.stream(), Stream.of("ROLE_" + role.name())).toList());
    }

    @Test
    void rolesAreHierarchical() {
        assertThat(RoleEnum.MANAGER.getPermissions()).containsAll(RoleEnum.USER.getPermissions());
        assertThat(RoleEnum.ADMIN.getPermissions()).containsAll(RoleEnum.MANAGER.getPermissions());
    }

    @Test
    void permissionsCannotBeModifiedFromOutside() {
        Set<PermissionEnum> permissions = RoleEnum.USER.getPermissions();

        assertThatThrownBy(() -> permissions.add(PermissionEnum.USER_DELETE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void everyRoleHasADescription() {
        for (RoleEnum role : RoleEnum.values()) {
            assertThat(role.getDescription()).isNotBlank();
        }
    }
}
