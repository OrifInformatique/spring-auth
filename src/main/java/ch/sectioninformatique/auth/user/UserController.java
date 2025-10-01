package ch.sectioninformatique.auth.user;

import java.util.List;

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
 * All endpoints are secured with appropriate authorization checks using Spring Security's
 * @PreAuthorize annotations. The controller follows RESTful conventions and returns
 * appropriate HTTP responses with success/error messages.
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
    public UserController(UserService userService)
    {
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
     * Retrieves all users in the system.
     * This endpoint:
     * - Requires the 'user:read' authority
     * - Returns a list of all users
     * - Is typically used by administrators
     *
     * @return ResponseEntity containing a list of all users
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> allUsers() {
        List <User> users = userService.allUsers();
        return ResponseEntity.ok(users);
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
        try {
            userService.promoteToManager(userId);
            return ResponseEntity.ok().body("User promoted to manager successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        try {
            userService.revokeManagerRole(userId);
            return ResponseEntity.ok().body("Manager role revoked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        try {
            userService.promoteToAdmin(userId);
            return ResponseEntity.ok().body("Admin role assigned successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        try {
            userService.revokeAdminRole(userId);
            return ResponseEntity.ok().body("Admin role revoked successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
        try {
            userService.downgradeAdminRole(userId);
            return ResponseEntity.ok().body("Admin role downgraded successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a user from the system.
     * This endpoint:
     * - Requires the 'user:delete' authority
     * - Validates the authenticated user has sufficient permissions
     * - Returns success/error message
     *
     * @param userId The ID of the user to delete
     * @return ResponseEntity with success message or error details
     */
    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.ok().body("User deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
