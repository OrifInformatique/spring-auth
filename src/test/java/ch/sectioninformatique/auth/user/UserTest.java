package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.user.User;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link User}.
 * This class tests the functionality of the User entity, including:
 * - UserDetails interface implementation
 * - Role and authority management
 * - Account status methods
 * - Entity field access and modification
 */
class UserTest {

    private static final Long TEST_ID = 1L;
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String TEST_LOGIN = "john.doe@example.com";
    private static final String TEST_PASSWORD = "hashedPassword";
    private static final Date TEST_CREATED_AT = new Date();
    private static final Date TEST_UPDATED_AT = new Date();

    /**
     * Tests the UserDetails interface implementation.
     * Verifies that:
     * - Username returns login
     * - Password is correctly returned
     * - Account status methods return true by default
     */
    @Test
    void testUserDetailsImplementation() {
        // Given
        User user = User.builder()
                .login(TEST_LOGIN)
                .password(TEST_PASSWORD)
                .build();

        // Then
        assertEquals(TEST_LOGIN, user.getUsername());
        assertEquals(TEST_PASSWORD, user.getPassword());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    /**
     * Tests authority management with roles.
     * Verifies that:
     * - Authorities are correctly created from roles
     * - Role names are properly prefixed with "ROLE_"
     */
    @Test
    void testAuthoritiesWithRoles() {
        // Given
        Role userRole = new Role();
        userRole.setName(RoleEnum.USER);
        Role adminRole = new Role();
        adminRole.setName(RoleEnum.ADMIN);

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);

        User user = User.builder()
                .roles(roles)
                .build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        System.out.println("Authorities found: " + authorities);
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    /**
     * Tests the builder pattern with all fields.
     * Verifies that:
     * - All fields are correctly set
     * - Values can be retrieved
     */
    @Test
    void testBuilderWithAllFields() {
        // Given
        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setName(RoleEnum.USER);
        roles.add(role);

        // When
        User user = User.builder()
                .id(TEST_ID)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .login(TEST_LOGIN)
                .password(TEST_PASSWORD)
                .createdAt(TEST_CREATED_AT)
                .updatedAt(TEST_UPDATED_AT)
                .roles(roles)
                .build();

        // Then
        assertEquals(TEST_ID, user.getId());
        assertEquals(TEST_FIRST_NAME, user.getFirstName());
        assertEquals(TEST_LAST_NAME, user.getLastName());
        assertEquals(TEST_LOGIN, user.getLogin());
        assertEquals(TEST_PASSWORD, user.getPassword());
        assertEquals(TEST_CREATED_AT, user.getCreatedAt());
        assertEquals(TEST_UPDATED_AT, user.getUpdatedAt());
        assertEquals(roles, user.getRoles());
    }

    /**
     * Tests role management.
     * Verifies that:
     * - Roles can be added
     * - First role can be retrieved
     */
    @Test
    void testRoleManagement() {
        // Given
        User user = User.builder().build();
        Role role = new Role();
        role.setName(RoleEnum.USER);

        // When
        user.addRole(role);

        // Then
        assertEquals(role, user.getRole());
        assertTrue(user.getRoles().contains(role));
    }
} 