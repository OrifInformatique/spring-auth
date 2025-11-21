package ch.sectioninformatique.auth.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing user operations.
 * This controller provides endpoints for:
 * - User authentication and profile management
 * - User role management (promotion, revocation)
 * - User deletion
 * - User listing and retrieval
 * 
 * All endpoints are secured with appropriate authorization checks using Spring
 * Security's
 * 
 * @PreAuthorize annotations. The controller follows RESTful conventions and
 *               returns
 *               appropriate HTTP responses with success/error messages.
 */
@RequestMapping("/users")
@RestController
public class UserController {
    /** Service for handling user-related operations */
    private final UserService userService;

    /**
     * Constructs a new UserController with the required service.
     *
     * @param userService Service for handling user-related operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the currently authenticated user's information.
     * This endpoint:
     * - Requires user authentication
     * - Returns the user's profile information
     * - Is accessible to all authenticated users
     *
     * @return ResponseEntity containing the current user's DTO
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> authenticatedUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserDto currentUser = (UserDto) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    /**
     * Retrieves all users in the system excluding soft-deleted ones.
     * This endpoint:
     * - Requires the 'user:read' authority
     * - Returns a list of all users
     * - Is typically used by administrators
     *
     * @return ResponseEntity containing a list of all users, without soft-deleted ones
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> allUsers() {
        List<User> users = userService.allUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves all users in the system including soft-deleted ones.
     * This endpoint:
     * - Requires the 'user:read' authority
     * - Returns a list of all users
     * - Is typically used by administrators
     *
     * @return ResponseEntity containing a list of all users, including soft-deleted ones
     */
    @GetMapping("/all-with-deleted")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> allWithDeletedUsers() {
        List<User> users = userService.allWithDeletedUsers();
        return ResponseEntity.ok(users); 
    }

    /**
     * Retrieves all soft-deleted users in the system.
     * This endpoint:
     * - Requires the 'user:read' authority
     * - Returns a list of all soft-deleted users
     * - Is typically used by administrators
     *
     * @return ResponseEntity containing a list of all soft-deleted users
     */
    @GetMapping("/deleted")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> deletedUsers() {
        List<User> users = userService.deletedUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Restore a user that was soft deleted
     * 
     * @param userId The ID of the user to restore
     * @return ResponseEntity with success message or error details
     */
    @PutMapping("/{userId}/restore")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<?> restoreDeletedUser(@PathVariable Long userId) {
        userService.restoreDeletedUser(userId);
        return ResponseEntity.ok().body("User restored successfully");
    }

    /**
     * Promotes a user to the manager role.
     * This endpoint:
     * - Requires the 'user:update' authority
     * - Validates the user exists and isn't already a manager
     * - Returns success/error message
     *
     * @param userId The ID of the user to promote
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{userId}/promote-manager")
    public ResponseEntity<?> promoteToManager(@PathVariable Long userId) {

        userService.promoteToManager(userId);
        return ResponseEntity.ok().body("User promoted to manager successfully");

    }

    /**
     * Revokes the manager role from a user.
     * This endpoint:
     * - Requires the 'user:update' authority
     * - Validates the user exists and isn't an admin
     * - Returns success/error message
     *
     * @param userId The ID of the user to revoke manager role from
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasAuthority('user:update')")
    @PutMapping("/{userId}/revoke-manager")
    public ResponseEntity<?> revokeManagerRole(@PathVariable Long userId) {

        userService.revokeManagerRole(userId);
        return ResponseEntity.ok().body("Manager role revoked successfully");
    }

    /**
     * Promotes a user to the admin role.
     * This endpoint:
     * - Requires 'ADMIN' role
     * - Validates the user exists and isn't already a admin
     * - Returns success/error message
     *
     * @param userId The ID of the user to promote to admin
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/promote-admin")
    public ResponseEntity<?> promoteToAdmin(@PathVariable Long userId) {
        userService.promoteToAdmin(userId);
        return ResponseEntity.ok().body("Admin role assigned successfully");
    }

    /**
     * Revokes the admin role from a user.
     * This endpoint:
     * - Requires 'ADMIN' role
     * - Validates the user exists and isn't already a regular user
     * - Returns success/error message
     *
     * @param userId The ID of the user to revoke admin role from
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/revoke-admin")
    public ResponseEntity<?> revokeAdminRole(@PathVariable Long userId) {
        userService.revokeAdminRole(userId);
        return ResponseEntity.ok().body("Admin role revoked successfully");
    }

    /**
     * Downgrades a admin to a regular manager role.
     * This endpoint:
     * - Requires 'ADMIN' role
     * - Validates the user exists and is currently a admin
     * - Returns success/error message
     *
     * @param userId The ID of the admin to downgrade
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/downgrade-admin")
    public ResponseEntity<?> downgradeAdminRole(@PathVariable Long userId) {
        userService.downgradeAdminRole(userId);
        return ResponseEntity.ok().body("Admin role downgraded successfully");
    }

    /**
     * Soft-deletes a user from the system.
     * This endpoint:
     * - Requires the 'user:delete' authority
     * - Validates the authenticated user has sufficient permissions
     * - Returns success/error message
     *
     * @param userId The ID of the user to soft-delete
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> delete(@PathVariable Long userId) {
        UserDto deletedUser = userService.deleteUser(userId);
        return ResponseEntity
                .ok(Map.of("message", "User deleted successfully", "deletedUserLogin", deletedUser.getLogin()));
    }

    /**
     * Permanently deletes a user from the system.
     * This endpoint:
     * - Requires the 'user:delete' authority
     * - Validates the authenticated user has sufficient permissions
     * - Returns success/error message
     *
     * @param userId The ID of the user to permanently delete
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}/delete-permanent")
    public ResponseEntity<?> deletePermanent(@PathVariable Long userId) {
        UserDto deletedUser = userService.deletePermanentUser(userId);
        return ResponseEntity
                .ok(Map.of("message", "User deleted permanently", "deletedUserLogin", deletedUser.getLogin()));
    }
}
