package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;

import java.util.Date;
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
     * Tests authority management with mainRole.
     * Verifies that:
     * - Authorities are correctly created from mainRole
     * - Role names are properly prefixed with "ROLE_"
     */
    @Test
    void testAuthoritiesWithRoles() {
        // Given
        Role managerRole = new Role();
        managerRole.setName(RoleEnum.MANAGER);

        User user = User.builder()
                .mainRole(managerRole)
                .build();

        // When
        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        // Then
        System.out.println("Authorities found: " + authorities);
        assertTrue(authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_MANAGER")));
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
        Role role = new Role();
        role.setName(RoleEnum.USER);

        // When
        User user = User.builder()
                .id(TEST_ID)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .login(TEST_LOGIN)
                .password(TEST_PASSWORD)
                .createdAt(TEST_CREATED_AT)
                .updatedAt(TEST_UPDATED_AT)
                .mainRole(role)
                .build();

        // Then
        assertEquals(TEST_ID, user.getId());
        assertEquals(TEST_FIRST_NAME, user.getFirstName());
        assertEquals(TEST_LAST_NAME, user.getLastName());
        assertEquals(TEST_LOGIN, user.getLogin());
        assertEquals(TEST_PASSWORD, user.getPassword());
        assertEquals(TEST_CREATED_AT, user.getCreatedAt());
        assertEquals(TEST_UPDATED_AT, user.getUpdatedAt());
        assertEquals(role, user.getMainRole());
    }

    /**
     * Tests role management.
     * Verifies that:
     * - mainRole can be set
     * - mainRole can be retrieved
     */
    @Test
    void testRoleManagement() {
        // Given
        User user = User.builder().build();
        Role role = new Role();
        role.setName(RoleEnum.USER);

        // When
        user.setMainRole(role);

        // Then
        assertEquals(role, user.getMainRole());
    }
} 