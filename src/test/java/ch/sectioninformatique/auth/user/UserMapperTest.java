package ch.sectioninformatique.auth.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.user.dto.CreateUserDto;
import ch.sectioninformatique.auth.user.dto.UserDto;

/**
 * Tests of the MapStruct-generated {@link UserMapper}.
 */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    void toUserDto_mapsIdentityRoleAndPermissionsButNeverThePassword() {
        Role role = new Role();
        role.setName(RoleEnum.MANAGER);
        User user = User.builder().id(1L).firstName("John").lastName("Doe").login("john.doe@test.com")
                .password("hash").mainRole(role).build();

        UserDto dto = userMapper.toUserDto(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFirstName()).isEqualTo("John");
        assertThat(dto.getLastName()).isEqualTo("Doe");
        assertThat(dto.getLogin()).isEqualTo("john.doe@test.com");
        assertThat(dto.getMainRole()).isEqualTo("MANAGER");
        assertThat(dto.getPermissions())
                .containsExactly("ROLE_MANAGER", "user:read", "user:update", "user:write");
        assertThat(dto.getToken()).isNull();
        assertThat(dto.isDeleted()).isFalse();
    }

    @Test
    void toUserDto_userWithoutRole_hasNoRoleAndNoPermissions() {
        UserDto dto = userMapper.toUserDto(User.builder().login("john.doe@test.com").build());

        assertThat(dto.getMainRole()).isNull();
        assertThat(dto.getPermissions()).isEmpty();
    }

    @Test
    void toUser_copiesIdentityOnly() {
        CreateUserDto request = new CreateUserDto("Jane", "Smith", "jane@test.com",
                "Password123!".toCharArray(), RoleEnum.ADMIN);

        User user = userMapper.toUser(request);

        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getLogin()).isEqualTo("jane@test.com");
        assertThat(user.getPassword()).isNull();
        assertThat(user.getMainRole()).isNull();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void authoritiesToPermissions_isSortedAndNullSafe() {
        assertThat(userMapper.authoritiesToPermissions(List.of(
                new SimpleGrantedAuthority("user:write"), new SimpleGrantedAuthority("user:read"))))
                .containsExactly("user:read", "user:write");
        assertThat(userMapper.authoritiesToPermissions(null)).isEmpty();
    }
}
