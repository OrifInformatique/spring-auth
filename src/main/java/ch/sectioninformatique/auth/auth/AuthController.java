package ch.sectioninformatique.auth.auth;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.PostMapping;
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
        userDto.setRevalidateToken(userAuthenticationProvider.createRevalidateToken(userDto));
        return ResponseEntity.ok(userDto);
    }

    /**
     * revalidate a user's access token from his revalidate token.
     *
     * @return ResponseEntity containing the authenticated user's information and
     *         JWT token
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/revalidate")
    public ResponseEntity<UserDto> revalidateLogin() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserDto currentUser = (UserDto) authentication.getPrincipal();
        currentUser = userService.revalidateLogin(currentUser.getLogin());
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
        createdUser.setRevalidateToken(userAuthenticationProvider.createRevalidateToken(createdUser));
        return ResponseEntity.created(URI.create("/users/" + createdUser.getId())).body(createdUser);
    }
}
