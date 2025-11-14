package ch.sectioninformatique.auth.auth;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller handling user authentication and registration.
 * This controller provides endpoints for user login and registration,
 * managing the authentication process and user creation.
 */
@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final UserService userService;
    private final UserAuthenticationProvider userAuthenticationProvider;

    /**
     * Authenticates a user with their credentials and returns a JWT token.
     *
     * @param credentialsDto The user credentials containing login and password
     * @return ResponseEntity containing the authenticated user's information and
     *         JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody @Valid CredentialsDto credentialsDto) {
        UserDto userDto = userService.login(credentialsDto);
        userDto.setToken(userAuthenticationProvider.createToken(userDto));
        userDto.setRefreshToken(userAuthenticationProvider.createRefreshToken(userDto));
        return ResponseEntity.ok(userDto);
    }

    /**
     * refresh a user's access token from his refresh token.
     *
     * @return ResponseEntity containing the authenticated user's information and
     *         JWT token
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/refresh")
    public ResponseEntity<UserDto> refreshLogin() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserDto currentUser = (UserDto) authentication.getPrincipal();
        currentUser = userService.refreshLogin(currentUser.getLogin());
        currentUser.setToken(userAuthenticationProvider.createToken(currentUser));
        return ResponseEntity.ok(currentUser);
    }

    /**
     * Registers a new user in the system.
     *
     * @param user The signup data containing the new user's information
     * @return ResponseEntity containing the created user's information and JWT
     *         token
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Valid SignUpDto user) {
        UserDto createdUser = userService.register(user);
        createdUser.setToken(userAuthenticationProvider.createToken(createdUser));
        createdUser.setRefreshToken(userAuthenticationProvider.createRefreshToken(createdUser));
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
    }

    /**
     * Change the password of the User
     * 
     * @param password A password Dto who contain bothe the old password for verification and the new for update
     * @return ResponseEntity containing a confirmation message
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody @Valid NewPasswordDto password) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserDto currentUser = (UserDto) authentication.getPrincipal();

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        userService.updatePassword(currentUser.getLogin(), password); // store securely (hashed!)

        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }
}
