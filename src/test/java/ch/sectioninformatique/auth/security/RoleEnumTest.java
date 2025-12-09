package ch.sectioninformatique.auth.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RoleEnum} enumeration.
 * 
 * This test class validates the RoleEnum functionality, including:
 * - All enum constants exist with correct values
 * - Enum name retrieval
 * - Enum ordering and count
 * - Values array contains all expected roles
 * 
 * RoleEnum defines the three hierarchical roles in the system:
 * - USER: Basic access level
 * - MANAGER: Intermediate access level with user management permissions
 * - ADMIN: Full system access with all permissions
 */
public class RoleEnumTest {

    /**
     * Test: USER role constant exists
     * 
     * Verifies that the RoleEnum enumeration contains the USER constant,
     * which represents the base-level user role with standard permissions.
     * 
     * Expected: RoleEnum.USER is not null and exists
     */
    @Test
    public void roleEnum_shouldHaveUserRole() {
        // Assert
        assertNotNull(RoleEnum.USER);
        assertEquals("USER", RoleEnum.USER.name());
    }

    /**
     * Test: MANAGER role constant exists
     * 
     * Verifies that the RoleEnum enumeration contains the MANAGER constant,
     * which represents the intermediate role with user management permissions.
     * 
     * Expected: RoleEnum.MANAGER is not null and exists
     */
    @Test
    public void roleEnum_shouldHaveManagerRole() {
        // Assert
        assertNotNull(RoleEnum.MANAGER);
        assertEquals("MANAGER", RoleEnum.MANAGER.name());
    }

    /**
     * Test: ADMIN role constant exists
     * 
     * Verifies that the RoleEnum enumeration contains the ADMIN constant,
     * which represents the highest privilege level with full system access.
     * 
     * Expected: RoleEnum.ADMIN is not null and exists
     */
    @Test
    public void roleEnum_shouldHaveAdminRole() {
        // Assert
        assertNotNull(RoleEnum.ADMIN);
        assertEquals("ADMIN", RoleEnum.ADMIN.name());
    }

    /**
     * Test: Enum values() returns all expected roles
     * 
     * Verifies that RoleEnum.values() returns an array containing exactly three
     * roles (USER, MANAGER, ADMIN) with no missing or extra values.
     * 
     * Expected:
     * - Array length is 3
     * - Array contains USER
     * - Array contains MANAGER
     * - Array contains ADMIN
     */
    @Test
    public void roleEnum_shouldHaveAllExpectedValues() {
        // Arrange
        RoleEnum[] roles = RoleEnum.values();

        // Assert
        assertEquals(3, roles.length, "Should have exactly 3 roles");
        assertTrue(containsRole(roles, RoleEnum.USER));
        assertTrue(containsRole(roles, RoleEnum.MANAGER));
        assertTrue(containsRole(roles, RoleEnum.ADMIN));
    }

    /**
     * Test: valueOf() returns correct enum constant for valid string
     * 
     * Verifies that RoleEnum.valueOf(String) correctly converts string names
     * to their corresponding enum constants.
     * 
     * Test data:
     * - "USER" should return RoleEnum.USER
     * - "MANAGER" should return RoleEnum.MANAGER
     * - "ADMIN" should return RoleEnum.ADMIN
     * 
     * Expected: Each string is correctly mapped to its enum constant
     */
    @Test
    public void roleEnum_valueOf_shouldReturnCorrectEnum() {
        // Act & Assert
        assertEquals(RoleEnum.USER, RoleEnum.valueOf("USER"));
        assertEquals(RoleEnum.MANAGER, RoleEnum.valueOf("MANAGER"));
        assertEquals(RoleEnum.ADMIN, RoleEnum.valueOf("ADMIN"));
    }

    /**
     * Test: valueOf() throws exception for invalid role name
     * 
     * Verifies that RoleEnum.valueOf(String) throws IllegalArgumentException
     * when given a string that doesn't match any defined enum constant.
     * 
     * Test data: "INVALID_ROLE" (non-existent role)
     * Expected: IllegalArgumentException is thrown
     */
    @Test
    public void roleEnum_valueOf_withInvalidValue_shouldThrowException() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            RoleEnum.valueOf("INVALID_ROLE");
        });
    }

    /**
     * Test: toString() returns enum constant name
     * 
     * Verifies that the toString() method returns the string representation
     * of each enum constant, which should match the constant's name.
     * 
     * Expected:
     * - USER.toString() returns "USER"
     * - MANAGER.toString() returns "MANAGER"
     * - ADMIN.toString() returns "ADMIN"
     */
    @Test
    public void roleEnum_toString_shouldReturnEnumName() {
        // Assert
        assertEquals("USER", RoleEnum.USER.toString());
        assertEquals("MANAGER", RoleEnum.MANAGER.toString());
        assertEquals("ADMIN", RoleEnum.ADMIN.toString());
    }

    /**
     * Test: Enum constants maintain correct ordinal values
     * 
     * Verifies that each enum constant has the expected ordinal value based
     * on its declaration order. This order represents the role hierarchy.
     * 
     * Expected ordinals:
     * - USER = 0 (lowest privilege)
     * - MANAGER = 1 (intermediate privilege)
     * - ADMIN = 2 (highest privilege)
     */
    @Test
    public void roleEnum_shouldBeOrdered() {
        // Assert - Check ordinal values (order matters for hierarchy)
        assertEquals(0, RoleEnum.USER.ordinal());
        assertEquals(1, RoleEnum.MANAGER.ordinal());
        assertEquals(2, RoleEnum.ADMIN.ordinal());
    }

    /**
     * Test: Enum constants are comparable by ordinal
     * 
     * Verifies that RoleEnum constants can be compared using compareTo(),
     * which compares their ordinal values. This enables hierarchy comparisons.
     * 
     * Test cases:
     * - USER < MANAGER (returns negative)
     * - ADMIN > USER (returns positive)
     * - USER == USER (returns 0)
     * 
     * Expected: Comparisons follow ordinal-based ordering
     */
    @Test
    public void roleEnum_shouldBeComparable() {
        // Assert - Enums are comparable by their ordinal
        assertTrue(RoleEnum.USER.compareTo(RoleEnum.MANAGER) < 0);
        assertTrue(RoleEnum.ADMIN.compareTo(RoleEnum.USER) > 0);
        assertEquals(0, RoleEnum.USER.compareTo(RoleEnum.USER));
    }

    /**
     * Test: All enum constants are non-null
     * 
     * Verifies that each RoleEnum constant is properly initialized and
     * not null. Enum constants should never be null in Java.
     * 
     * Expected: USER, MANAGER, and ADMIN are all non-null
     */
    @Test
    public void roleEnum_shouldNotBeNull() {
        // Assert
        assertNotNull(RoleEnum.USER);
        assertNotNull(RoleEnum.MANAGER);
        assertNotNull(RoleEnum.ADMIN);
    }

    /**
     * Test: values() returns a new array copy each time
     * 
     * Verifies that calling RoleEnum.values() multiple times returns
     * different array instances (defensive copying) while maintaining
     * the same content. This prevents external modification of the enum values.
     * 
     * Expected:
     * - Two calls to values() return different array references
     * - Array contents are identical
     */
    @Test
    public void roleEnum_values_shouldReturnArrayCopy() {
        // Arrange
        RoleEnum[] values1 = RoleEnum.values();
        RoleEnum[] values2 = RoleEnum.values();

        // Assert - values() should return a new array each time
        assertNotSame(values1, values2, "values() should return a new array");
        assertArrayEquals(values1, values2, "Arrays should have the same content");
    }

    /**
     * Helper method to check if an array contains a specific role.
     */
    private boolean containsRole(RoleEnum[] roles, RoleEnum target) {
        for (RoleEnum role : roles) {
            if (role == target) {
                return true;
            }
        }
        return false;
    }
}
