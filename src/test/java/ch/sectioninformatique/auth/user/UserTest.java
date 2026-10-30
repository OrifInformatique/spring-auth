package ch.sectioninformatique.auth.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;

/**
 * Tests of the Spring Security {@code UserDetails} contract of {@link User}.
 */
class UserTest {

    @Test
    void activeUser_isEnabledAndUsesLoginAsUsername() {
        User user = User.builder().login("john.doe@test.com").password("hash").build();

        assertThat(user.getUsername()).isEqualTo("john.doe@test.com");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void softDeletedUser_isDisabled() {
        User user = User.builder().login("john.doe@test.com").deleted(true).build();

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountNonExpired()).isFalse();
        assertThat(user.isAccountNonLocked()).isFalse();
        assertThat(user.isCredentialsNonExpired()).isFalse();
    }

    @Test
    void authorities_comeFromTheRole() {
        Role role = new Role();
        role.setName(RoleEnum.MANAGER);
        User user = User.builder().mainRole(role).build();

        assertThat(user.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_MANAGER", "user:read", "user:write", "user:update");
    }

    @Test
    void userWithoutRole_hasNoAuthorities() {
        assertThat(User.builder().build().getAuthorities()).isEmpty();
    }

    @Test
    void toString_neverContainsThePassword() {
        User user = User.builder().login("john.doe@test.com").password("secret-hash").build();

        assertThat(user.toString()).doesNotContain("secret-hash");
    }
}
