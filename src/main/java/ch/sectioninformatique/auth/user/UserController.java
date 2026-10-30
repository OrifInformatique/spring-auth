package ch.sectioninformatique.auth.user;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import ch.sectioninformatique.auth.user.dto.CreateUserDto;
import ch.sectioninformatique.auth.user.dto.PasswordUpdateDto;
import ch.sectioninformatique.auth.user.dto.RoleUpdateDto;
import ch.sectioninformatique.auth.user.dto.UpdateUserDto;
import ch.sectioninformatique.auth.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * The users resource. Users are identified by their login (email) in URLs.
 *
 * Every endpoint requires an access token; the permission required by each one is
 * declared with {@code @PreAuthorize}. Rules depending on the target user (admin
 * accounts, own account) are enforced by {@link UserService}.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * @param currentUser authenticated user
     * @return 200 with the authenticated user, as currently stored (the access token
     *         may carry outdated data, e.g. a role changed since login)
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserDto currentUser) {
        return ResponseEntity.ok(userService.findByLogin(currentUser.getLogin()));
    }

    /**
     * Changes the password of the authenticated user.
     *
     * @param currentUser authenticated user
     * @param request     current and new passwords
     * @return 204 No Content
     */
    @PutMapping("/me/password")
    public ResponseEntity<Void> updateOwnPassword(@AuthenticationPrincipal UserDto currentUser,
            @RequestBody @Valid PasswordUpdateDto request) {
        userService.updatePassword(currentUser.getLogin(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param status ACTIVE (default), DELETED or ALL
     * @return 200 with the matching users, ordered by id
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(defaultValue = "ACTIVE") UserStatus status) {
        return ResponseEntity.ok(userService.findAll(status));
    }

    /**
     * @param login login of the user
     * @return 200 with the active user having this login
     */
    @GetMapping("/{login}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<UserDto> getUser(@PathVariable String login) {
        return ResponseEntity.ok(userService.findByLogin(login));
    }

    /**
     * Creates a user.
     *
     * @param request     new user's data
     * @param currentUser authenticated user
     * @return 201 with the created user and its URL in the Location header
     */
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<UserDto> createUser(@RequestBody @Valid CreateUserDto request,
            @AuthenticationPrincipal UserDto currentUser) {
        UserDto created = userService.create(request, currentUser.getLogin());
        URI location = UriComponentsBuilder.fromPath("/users/{login}")
                .buildAndExpand(created.getLogin())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates the first name, last name and login of a user.
     *
     * @param login       current login of the user
     * @param request     new values
     * @param currentUser authenticated user
     * @return 200 with the updated user
     */
    @PutMapping("/{login}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<UserDto> updateUser(@PathVariable String login,
            @RequestBody @Valid UpdateUserDto request,
            @AuthenticationPrincipal UserDto currentUser) {
        return ResponseEntity.ok(userService.update(login, request, currentUser.getLogin()));
    }

    /**
     * Changes the role of a user.
     *
     * @param login       login of the user
     * @param request     new role
     * @param currentUser authenticated user
     * @return 200 with the updated user
     */
    @PutMapping("/{login}/role")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable String login,
            @RequestBody @Valid RoleUpdateDto request,
            @AuthenticationPrincipal UserDto currentUser) {
        return ResponseEntity.ok(userService.changeRole(login, request.role(), currentUser.getLogin()));
    }

    /**
     * Deletes a user. The deletion is soft (restorable) unless {@code permanent=true}.
     *
     * @param login       login of the user
     * @param permanent   true to remove the user from the database
     * @param currentUser authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/{login}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable String login,
            @RequestParam(defaultValue = "false") boolean permanent,
            @AuthenticationPrincipal UserDto currentUser) {
        userService.delete(login, permanent, currentUser.getLogin());
        return ResponseEntity.noContent().build();
    }

    /**
     * Restores a soft-deleted user.
     *
     * @param login       login of the user
     * @param currentUser authenticated user
     * @return 200 with the restored user
     */
    @PostMapping("/{login}/restore")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<UserDto> restoreUser(@PathVariable String login,
            @AuthenticationPrincipal UserDto currentUser) {
        return ResponseEntity.ok(userService.restore(login, currentUser.getLogin()));
    }
}
