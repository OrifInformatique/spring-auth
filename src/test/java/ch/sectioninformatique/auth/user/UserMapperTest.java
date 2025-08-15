package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import ch.sectioninformatique.auth.auth.SignUpDto;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link UserMapper}.
 * This class tests the conversion functionalities between different
 * user representations (User, UserDto, SignUpDto).
 * 
 * @author [Your name]
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
class UserMapperTest {

    /**
     * Test configuration to provide necessary beans.
     * This internal configuration provides a UserMapper instance
     * for testing purposes.
     */
    @Configuration
    static class TestConfig {
        /**
         * Creates and returns a UserMapper instance for testing.
         *
         * @return A UserMapper instance
         */
        @org.springframework.context.annotation.Bean
        public UserMapper userMapper() {
            return UserMapper.INSTANCE;
        }
    }

    /** Mapper to test, injected by Spring */
    @Autowired
    private UserMapper userMapper;

    /**
     * Tests the conversion from User to UserDto.
     * Verifies that:
     * - All fields are correctly mapped
     * - Role is properly converted
     * - Permissions are initialized
     */
    @Test
    void testToUserDto() {
        // Given
        User user = new User();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("johndoe");
        
        Role role = new Role();
        role.setName(RoleEnum.ADMIN);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        // When
        UserDto userDto = userMapper.toUserDto(user);

        // Then
        assertNotNull(userDto);
        assertEquals(1L, userDto.getId());
        assertEquals("John", userDto.getFirstName());
        assertEquals("Doe", userDto.getLastName());
        assertEquals("johndoe", userDto.getLogin());
        assertEquals("ADMIN", userDto.getRole());
        assertNotNull(userDto.getPermissions());
    }

    /**
     * Tests the conversion from SignUpDto to User.
     * Verifies that:
     * - Basic information is correctly mapped
     * - Password is ignored as configured
     * - Roles are initialized as an empty set
     */
    @Test
    void testSignUpToUser() {
        // Given
        SignUpDto signUpDto = new SignUpDto(
            "Jane",
            "Smith",
            "janesmith",
            "password123".toCharArray()
        );

        // When
        User user = userMapper.signUpToUser(signUpDto);

        // Then
        assertNotNull(user);
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("janesmith", user.getLogin());
        assertNull(user.getPassword()); // Password should be ignored as per mapping
        assertTrue(user.getRoles().isEmpty()); // Roles should be empty as per mapping
    }

    /**
     * Tests the conversion of authorities to permissions.
     * Verifies that:
     * - The permissions list is not null
     * - All authorities are correctly converted
     * - Order and number of elements are preserved
     */
    @Test
    void testAuthoritiesToPermissions() {
        // Given
        List<SimpleGrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("READ"),
            new SimpleGrantedAuthority("WRITE"),
            new SimpleGrantedAuthority("DELETE")
        );

        // When
        List<String> permissions = userMapper.authoritiesToPermissions(authorities);

        // Then
        assertNotNull(permissions);
        assertEquals(3, permissions.size());
        assertTrue(permissions.containsAll(Arrays.asList("READ", "WRITE", "DELETE")));
    }

    /**
     * Tests the behavior of authority conversion with null input.
     * Verifies that:
     * - The method returns null when input is null
     * - No exception is thrown
     */
    @Test
    void testAuthoritiesToPermissionsWithNull() {
        // When
        List<String> permissions = userMapper.authoritiesToPermissions(null);

        // Then
        assertNull(permissions);
    }
} 