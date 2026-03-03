package ch.sectioninformatique.auth.auth;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
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
        private final MessageSource messageSource;

        /*
         * Refresh token lifetime (e.g., "30d" for 30 days), configured via environment
         * variable.
         */
        @Value("${SECURITY_JWT_TOKEN_REFRESH_TOKEN_LIFETIME}")
        private Duration refreshTokenLifetime;

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
                userService.storeRefreshToken(userDto.getLogin(), refreshToken,
                                Instant.now().plus(refreshTokenLifetime));

                // Create secure HTTP-only cookie for the refresh token
                ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                                .httpOnly(true)
                                .secure(true)
                                .path("/auth/refresh")
                                .maxAge(refreshTokenLifetime)
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
        public ResponseEntity<TokenResponseDto> refreshLogin(@CookieValue("refresh_token") String refreshToken) {

                DecodedJWT jwt = userAuthenticationProvider.validateRefreshToken(refreshToken);
                String login = jwt.getSubject();

                userService.assertValidRefreshToken(login, refreshToken);

                UserDto user = userService.findByLogin(login);

                // rotate tokens
                String newAccess = userAuthenticationProvider.createToken(user);
                String newRefresh = userAuthenticationProvider.createRefreshToken(user);

                userService.storeRefreshToken(login, newRefresh, jwt.getExpiresAt().toInstant());

                ResponseCookie cookie = ResponseCookie.from("refresh_token", newRefresh)
                                .httpOnly(true)
                                .secure(true)
                                .path("/auth/refresh")
                                .maxAge(refreshTokenLifetime)
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
                userService.storeRefreshToken(createdUser.getLogin(), refreshToken,
                                Instant.now().plus(refreshTokenLifetime));

                // Create secure HTTP-only cookie for the refresh token
                ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                                .httpOnly(true)
                                .secure(true)
                                .path("/auth/refresh")
                                .maxAge(refreshTokenLifetime)
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
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(messageSource.getMessage(
                                                        "error.security.token.invalid",
                                                        null,
                                                        LocaleContextHolder.getLocale()));
                }

                userService.updatePassword(currentUser.getLogin(), passwords); // store securely (hashed!)

                return ResponseEntity.ok(Map.of(
                                "message",
                                messageSource.getMessage(
                                                "message.password.updated",
                                                null,
                                                LocaleContextHolder.getLocale())));
        }

        /**
         * Logs out the authenticated user by invalidating all refresh tokens.
         * 
         * This endpoint handles the logout process by:
         * - Retrieving the authenticated user from the security context.
         * - Deleting all stored refresh tokens for the user from the database.
         * - Returning an expired refresh token in a secure HTTP-only cookie to
         * invalidate on the frontend.
         * - Preventing token reuse after logout for enhanced security.
         * 
         * Security considerations:
         * - Requires authentication (@PreAuthorize("isAuthenticated()")).
         * - Clears refresh tokens to prevent new access tokens from being issued.
         * - Returns an expired refresh token to force frontend cleanup.
         *
         * @return ResponseEntity with a success message and expired refresh token in a
         *         secure cookie.
         * @throws AppException if the user is not properly authenticated.
         */
        @PreAuthorize("isAuthenticated()")
        @PostMapping("/logout")
        public ResponseEntity<?> logout(HttpServletRequest request) {
                var session = request.getSession(false);
                if (session != null) {
                        session.invalidate();
                }
                // Retrieve the current authenticated user from the security context
                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                UserDto currentUser = (UserDto) authentication.getPrincipal();

                // Validate that the user is properly authenticated
                if (currentUser == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
                }

                // Delete all refresh tokens for this user from the database to prevent token
                // reuse
                userService.deleteRefreshTokens(currentUser.getLogin());

                // Create an expired refresh token to signal frontend to clear the token
                String expiredRefreshToken = userAuthenticationProvider.createExpiredRefreshToken(currentUser);

                // Create secure HTTP-only cookie with zero lifespan for the expired refresh
                // token
                ResponseCookie cookie = ResponseCookie.from("refresh_token", expiredRefreshToken)
                                .httpOnly(true)
                                .secure(true)
                                .path("/auth/refresh")
                                .maxAge(Duration.ZERO)
                                .sameSite("Strict")
                                .build();

                SecurityContextHolder.clearContext();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(Map.of(
                                                "message",
                                                messageSource.getMessage(
                                                                "message.logout.success",
                                                                null,
                                                                LocaleContextHolder.getLocale())));
        }
}
