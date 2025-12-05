package ch.sectioninformatique.auth.auth;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller handling authentication, token management, and user
 * registration.
 * 
 * This controller provides endpoints for:
 * 
 * User login and JWT token issuance
 * Refresh token validation and rotation
 * User registration and initial token creation
 * Password updates for authenticated users
 * 
 * Security considerations:
 * 
 * Refresh tokens are stored in HTTP-only cookies to prevent XSS attacks.
 * Access tokens are short-lived and provided in the response body.
 * Password updates require authentication and secure storage (hashed).
 */
@RequestMapping("/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final UserService userService;
    private final UserAuthenticationProvider userAuthenticationProvider;

    /**
     * Authenticates a user with provided credentials and issues JWT access and
     * refresh tokens.
     * 
     * - Validates user credentials via UserService.
     * - Generates an access token for immediate authentication.
     * - Generates a refresh token and stores it securely with expiration.
     * - Sends the refresh token in a secure HTTP-only cookie.
     *
     * @param credentialsDto credentialsDto The DTO containing user login and
     *                       password.
     * @return ResponseEntity with the authenticated user's info and access token in
     *         body.
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody @Valid CredentialsDto credentialsDto) {
        UserDto userDto = userService.login(credentialsDto);

        String accessToken = userAuthenticationProvider.createToken(userDto);
        String refreshToken = userAuthenticationProvider.createRefreshToken(userDto);

        userDto.setToken(accessToken);

        // Store refresh token in database with expiration
        userService.storeRefreshToken(userDto.getLogin(), refreshToken, Instant.now().plus(Duration.ofDays(30)));

        // Create secure HTTP-only cookie for the refresh token
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(userDto);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * 
     * - Validates the refresh token and ensures it matches the stored token.
     * - Rotates refresh tokens to enhance security (prevents reuse).
     * - Returns a new access token and sets the new refresh token as HTTP-only
     * cookie.
     *
     * @param request DTO containing the refresh token.
     * @return ResponseEntity containing a new access token.
     * @throws AppException if the refresh token is invalid or expired.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refreshLogin(@RequestBody RefreshRequestDto request) {

        DecodedJWT jwt = userAuthenticationProvider.validateRefreshToken(request.refreshToken());
        String login = jwt.getSubject();

        if (!userService.validate(login, request.refreshToken())) {
            throw new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        UserDto user = userService.findByLogin(login);

        // rotate tokens
        String newAccess = userAuthenticationProvider.createToken(user);
        String newRefresh = userAuthenticationProvider.createRefreshToken(user);

        userService.storeRefreshToken(login, newRefresh, jwt.getExpiresAt().toInstant());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefresh)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenResponseDto(newAccess));
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

        String accessToken = userAuthenticationProvider.createToken(createdUser);
        String refreshToken = userAuthenticationProvider.createRefreshToken(createdUser);

        createdUser.setToken(accessToken);

        // Store refresh token in database with expiration
        userService.storeRefreshToken(createdUser.getLogin(), refreshToken, Instant.now().plus(Duration.ofDays(30)));

        // Create secure HTTP-only cookie for the refresh token
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(30))
                .sameSite("Strict")
                .build();

        return ResponseEntity.created(URI.create("/auth/users/" + createdUser.getLogin()))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(createdUser);
    }

    /**
     * Change the password of the User
     * 
     * @param passwords A password Dto who contain both the old password for
     *                  verification and the new for update
     * @return ResponseEntity containing a confirmation message
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody @Valid PasswordUpdateDto passwords) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserDto currentUser = (UserDto) authentication.getPrincipal();

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        userService.updatePassword(currentUser.getLogin(), passwords); // store securely (hashed!)

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
