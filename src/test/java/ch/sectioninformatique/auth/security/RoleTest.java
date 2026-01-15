package ch.sectioninformatique.auth.security;

import org.junit.jupiter.api.Test;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Role} entity.
 * 
 * This test class validates the basic functionality of the Role entity, including:
 * - Constructor and initialization
 * - Getter and setter methods for all fields
 * - Field data types and values
 * - Support for all RoleEnum values
 * 
 * The Role entity represents user roles in the authentication system with
 * hierarchical permissions (USER < MANAGER < ADMIN).
 */
public class RoleTest {

    /**
     * Test: Role creation with default constructor
     * 
     * Verifies that a Role instance can be created using the default constructor
     * without throwing any exceptions.
     * 
     * Expected: Non-null Role object
     */
    @Test
    public void role_shouldCreateWithDefaultConstructor() {
        // Act
        Role role = new Role();

        // Assert
        assertNotNull(role);
    }

    /**
     * Test: ID field getter and setter
     * 
     * Verifies that the Role ID can be set and retrieved correctly.
     * The ID is the primary key used for database identification.
     * 
     * Test data: ID = 1L
     * Expected: getId() returns the same value as setId()
     */
    @Test
    public void role_shouldSetAndGetId() {
        // Arrange
        Role role = new Role();
        long expectedId = 1L;

        // Act
        role.setId(expectedId);

        // Assert
        assertEquals(expectedId, role.getId());
    }

    /**
     * Test: Name field getter and setter
     * 
     * Verifies that the role name (as RoleEnum) can be set and retrieved correctly.
     * The name defines the role type and determines permission levels.
     * 
     * Test data: Name = RoleEnum.ADMIN
     * Expected: getName() returns the same RoleEnum value as setName()
     */
    @Test
    public void role_shouldSetAndGetName() {
        // Arrange
        Role role = new Role();
        RoleEnum expectedName = RoleEnum.ADMIN;

        // Act
        role.setName(expectedName);

        // Assert
        assertEquals(expectedName, role.getName());
    }

    /**
     * Test: Description field getter and setter
     * 
     * Verifies that the role description can be set and retrieved correctly.
     * The description provides human-readable information about the role's purpose.
     * 
     * Test data: Description = "Administrator role with full permissions"
     * Expected: getDescription() returns the same string as setDescription()
     */
    @Test
    public void role_shouldSetAndGetDescription() {
        // Arrange
        Role role = new Role();
        String expectedDescription = "Administrator role with full permissions";

        // Act
        role.setDescription(expectedDescription);

        // Assert
        assertEquals(expectedDescription, role.getDescription());
    }

    /**
     * Test: CreatedAt timestamp field getter and setter
     * 
     * Verifies that the creation timestamp can be set and retrieved correctly.
     * This field tracks when the role was first created in the system.
     * 
     * Test data: Current date/time
     * Expected: getCreatedAt() returns the same Date object as setCreatedAt()
     */
    @Test
    public void role_shouldSetAndGetCreatedAt() {
        // Arrange
        Role role = new Role();
        Date expectedDate = new Date();

        // Act
        role.setCreatedAt(expectedDate);

        // Assert
        assertEquals(expectedDate, role.getCreatedAt());
    }

    /**
     * Test: UpdatedAt timestamp field getter and setter
     * 
     * Verifies that the last update timestamp can be set and retrieved correctly.
     * This field tracks the most recent modification to the role.
     * 
     * Test data: Current date/time
     * Expected: getUpdatedAt() returns the same Date object as setUpdatedAt()
     */
    @Test
    public void role_shouldSetAndGetUpdatedAt() {
        // Arrange
        Role role = new Role();
        Date expectedDate = new Date();

        // Act
        role.setUpdatedAt(expectedDate);

        // Assert
        assertEquals(expectedDate, role.getUpdatedAt());
    }

    /**
     * Test: Support for all RoleEnum values
     * 
     * Verifies that the Role entity can be assigned any of the three
     * defined RoleEnum values (USER, MANAGER, ADMIN) without errors.
     * 
     * Test data:
     * - RoleEnum.USER
     * - RoleEnum.MANAGER  
     * - RoleEnum.ADMIN
     * 
     * Expected: All enum values can be set and retrieved correctly
     */
    @Test
    public void role_shouldAllowAllRoleEnumValues() {
        // Arrange
        Role userRole = new Role();
        Role managerRole = new Role();
        Role adminRole = new Role();

        // Act
        userRole.setName(RoleEnum.USER);
        managerRole.setName(RoleEnum.MANAGER);
        adminRole.setName(RoleEnum.ADMIN);

        // Assert
        assertEquals(RoleEnum.USER, userRole.getName());
        assertEquals(RoleEnum.MANAGER, managerRole.getName());
        assertEquals(RoleEnum.ADMIN, adminRole.getName());
    }

    @Test
    public void role_shouldHandleMultipleProperties() {
        // Arrange
        Role role = new Role();
        long id = 5L;
        RoleEnum name = RoleEnum.MANAGER;
        String description = "Manager role";
        Date createdAt = new Date();
        Date updatedAt = new Date();

        // Act
        role.setId(id);
        role.setName(name);
        role.setDescription(description);
        role.setCreatedAt(createdAt);
        role.setUpdatedAt(updatedAt);

        // Assert
        assertEquals(id, role.getId());
        assertEquals(name, role.getName());
        assertEquals(description, role.getDescription());
        assertEquals(createdAt, role.getCreatedAt());
        assertEquals(updatedAt, role.getUpdatedAt());
    }

    @Test
    public void role_shouldAllowNullDescription() {
        // Arrange
        Role role = new Role();

        // Act
        role.setDescription(null);

        // Assert
        assertNull(role.getDescription());
    }

    @Test
    public void role_shouldAllowEmptyDescription() {
        // Arrange
        Role role = new Role();
        String emptyDescription = "";

        // Act
        role.setDescription(emptyDescription);

        // Assert
        assertEquals(emptyDescription, role.getDescription());
    }

    @Test
    public void role_shouldHandleLongDescription() {
        // Arrange
        Role role = new Role();
        String longDescription = "A".repeat(500);

        // Act
        role.setDescription(longDescription);

        // Assert
        assertEquals(longDescription, role.getDescription());
        assertEquals(500, role.getDescription().length());
    }
}
