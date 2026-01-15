package ch.sectioninformatique.auth.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PermissionEnum} enumeration.
 * 
 * This test class validates the PermissionEnum functionality, including:
 * - All permission constants exist with correct string values
 * - Permission string format follows "resource:action" convention
 * - Enum methods (valueOf, toString, getPermission) work correctly
 * - All four CRUD permissions are defined (READ, WRITE, UPDATE, DELETE)
 * 
 * PermissionEnum defines granular user management permissions:
 * - USER_READ: "user:read" - View user information
 * - USER_WRITE: "user:write" - Create new users
 * - USER_UPDATE: "user:update" - Modify existing users
 * - USER_DELETE: "user:delete" - Remove users
 */
public class PermissionEnumTest {

    /**
     * Test: USER_READ permission has correct string value
     * 
     * Verifies that the USER_READ enum constant's getPermission() method
     * returns the expected permission string "user:read".
     * 
     * Expected: getPermission() returns "user:read"
     */
    @Test
    public void permissionEnum_shouldHaveCorrectUserReadPermission() {
        // Assert
        assertEquals("user:read", PermissionEnum.USER_READ.getPermission());
    }

    /**
     * Test: USER_WRITE permission has correct string value
     * 
     * Verifies that the USER_WRITE enum constant's getPermission() method
     * returns the expected permission string "user:write".
     * 
     * Expected: getPermission() returns "user:write"
     */
    @Test
    public void permissionEnum_shouldHaveCorrectUserWritePermission() {
        // Assert
        assertEquals("user:write", PermissionEnum.USER_WRITE.getPermission());
    }

    /**
     * Test: USER_UPDATE permission has correct string value
     * 
     * Verifies that the USER_UPDATE enum constant's getPermission() method
     * returns the expected permission string "user:update".
     * 
     * Expected: getPermission() returns "user:update"
     */
    @Test
    public void permissionEnum_shouldHaveCorrectUserUpdatePermission() {
        // Assert
        assertEquals("user:update", PermissionEnum.USER_UPDATE.getPermission());
    }

    /**
     * Test: USER_DELETE permission has correct string value
     * 
     * Verifies that the USER_DELETE enum constant's getPermission() method
     * returns the expected permission string "user:delete".
     * 
     * Expected: getPermission() returns "user:delete"
     */
    @Test
    public void permissionEnum_shouldHaveCorrectUserDeletePermission() {
        // Assert
        assertEquals("user:delete", PermissionEnum.USER_DELETE.getPermission());
    }

    /**
     * Test: Enum values() returns all expected permissions
     * 
     * Verifies that PermissionEnum.values() returns an array containing exactly
     * four CRUD permission constants with no missing or extra values.
     * 
     * Expected:
     * - Array length is 4
     * - Array contains USER_READ
     * - Array contains USER_WRITE
     * - Array contains USER_UPDATE
     * - Array contains USER_DELETE
     */
    @Test
    public void permissionEnum_shouldHaveAllExpectedValues() {
        // Arrange
        PermissionEnum[] permissions = PermissionEnum.values();

        // Assert
        assertEquals(4, permissions.length, "Should have exactly 4 permissions");
        assertTrue(containsPermission(permissions, PermissionEnum.USER_READ));
        assertTrue(containsPermission(permissions, PermissionEnum.USER_WRITE));
        assertTrue(containsPermission(permissions, PermissionEnum.USER_UPDATE));
        assertTrue(containsPermission(permissions, PermissionEnum.USER_DELETE));
    }

    /**
     * Test: valueOf() returns correct enum constant for valid string
     * 
     * Verifies that PermissionEnum.valueOf(String) correctly converts
     * enum constant names to their corresponding enum instances.
     * 
     * Test data:
     * - "USER_READ" should return PermissionEnum.USER_READ
     * - "USER_WRITE" should return PermissionEnum.USER_WRITE
     * - "USER_UPDATE" should return PermissionEnum.USER_UPDATE
     * - "USER_DELETE" should return PermissionEnum.USER_DELETE
     * 
     * Expected: Each string maps to its corresponding enum constant
     */
    @Test
    public void permissionEnum_valueOf_shouldReturnCorrectEnum() {
        // Act & Assert
        assertEquals(PermissionEnum.USER_READ, PermissionEnum.valueOf("USER_READ"));
        assertEquals(PermissionEnum.USER_WRITE, PermissionEnum.valueOf("USER_WRITE"));
        assertEquals(PermissionEnum.USER_UPDATE, PermissionEnum.valueOf("USER_UPDATE"));
        assertEquals(PermissionEnum.USER_DELETE, PermissionEnum.valueOf("USER_DELETE"));
    }

    /**
     * Test: valueOf() throws exception for invalid permission name
     * 
     * Verifies that PermissionEnum.valueOf(String) throws IllegalArgumentException
     * when given a string that doesn't match any defined enum constant.
     * 
     * Test data: "INVALID_PERMISSION" (non-existent permission)
     * Expected: IllegalArgumentException is thrown
     */
    @Test
    public void permissionEnum_valueOf_withInvalidValue_shouldThrowException() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            PermissionEnum.valueOf("INVALID_PERMISSION");
        });
    }

    /**
     * Test: toString() returns enum constant name
     * 
     * Verifies that the toString() method returns the string representation
     * of each enum constant (not the permission string value).
     * 
     * Expected:
     * - USER_READ.toString() returns "USER_READ"
     * - USER_WRITE.toString() returns "USER_WRITE"
     * - USER_UPDATE.toString() returns "USER_UPDATE"
     * - USER_DELETE.toString() returns "USER_DELETE"
     */
    @Test
    public void permissionEnum_toString_shouldReturnEnumName() {
        // Assert
        assertEquals("USER_READ", PermissionEnum.USER_READ.toString());
        assertEquals("USER_WRITE", PermissionEnum.USER_WRITE.toString());
        assertEquals("USER_UPDATE", PermissionEnum.USER_UPDATE.toString());
        assertEquals("USER_DELETE", PermissionEnum.USER_DELETE.toString());
    }

    /**
     * Test: All enum constants are non-null
     * 
     * Verifies that each PermissionEnum constant is properly initialized
     * and not null. Enum constants should never be null in Java.
     * 
     * Expected: USER_READ, USER_WRITE, USER_UPDATE, USER_DELETE are all non-null
     */
    @Test
    public void permissionEnum_shouldNotBeNull() {
        // Assert
        assertNotNull(PermissionEnum.USER_READ);
        assertNotNull(PermissionEnum.USER_WRITE);
        assertNotNull(PermissionEnum.USER_UPDATE);
        assertNotNull(PermissionEnum.USER_DELETE);
    }

    /**
     * Test: getPermission() returns non-null for all permissions
     * 
     * Verifies that the getPermission() method returns a non-null string
     * value for each enum constant, ensuring all permissions have valid
     * string representations.
     * 
     * Expected: Each getPermission() call returns a non-null string
     */
    @Test
    public void permissionEnum_getPermission_shouldReturnNonNullValue() {
        // Assert
        assertNotNull(PermissionEnum.USER_READ.getPermission());
        assertNotNull(PermissionEnum.USER_WRITE.getPermission());
        assertNotNull(PermissionEnum.USER_UPDATE.getPermission());
        assertNotNull(PermissionEnum.USER_DELETE.getPermission());
    }

    /**
     * Test: All permissions follow "resource:action" naming convention
     * 
     * Verifies that all permission strings adhere to the standard format
     * of "resource:action" where resource is "user" and action is a CRUD operation.
     * 
     * Expected:
     * - All permission strings contain colon separator
     * - All permission strings start with "user:"
     */
    @Test
    public void permissionEnum_permissions_shouldFollowNamingConvention() {
        // Assert - All permissions should follow "resource:action" format
        for (PermissionEnum permission : PermissionEnum.values()) {
            String permStr = permission.getPermission();
            assertTrue(permStr.contains(":"), 
                "Permission " + permStr + " should follow 'resource:action' format");
            assertTrue(permStr.startsWith("user:"), 
                "Permission " + permStr + " should start with 'user:'");
        }
    }

    /**
     * Test: Enum constants are comparable by ordinal
     * 
     * Verifies that PermissionEnum constants can be compared using compareTo(),
     * which compares their ordinal values based on declaration order.
     * 
     * Test cases:
     * - USER_READ < USER_WRITE (returns negative)
     * - USER_DELETE > USER_READ (returns positive)
     * - USER_READ == USER_READ (returns 0)
     * 
     * Expected: Comparisons follow ordinal-based ordering
     */
    @Test
    public void permissionEnum_shouldBeComparable() {
        // Assert - Enums are comparable by their ordinal
        assertTrue(PermissionEnum.USER_READ.compareTo(PermissionEnum.USER_WRITE) < 0);
        assertTrue(PermissionEnum.USER_DELETE.compareTo(PermissionEnum.USER_READ) > 0);
        assertEquals(0, PermissionEnum.USER_READ.compareTo(PermissionEnum.USER_READ));
    }

    /**
     * Helper method to check if an array contains a specific permission.
     */
    private boolean containsPermission(PermissionEnum[] permissions, PermissionEnum target) {
        for (PermissionEnum permission : permissions) {
            if (permission == target) {
                return true;
            }
        }
        return false;
    }
}
