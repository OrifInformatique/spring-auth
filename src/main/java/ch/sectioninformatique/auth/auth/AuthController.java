package ch.sectioninformatique.auth.auth;

import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.sectioninformatique.auth.auth.AuthService.AuthResult;
import ch.sectioninformatique.auth.auth.dto.CredentialsDto;
import ch.sectioninformatique.auth.auth.dto.TokenResponseDto;
import ch.sectioninformatique.auth.auth.token.RefreshTokenCookieFactory;
import ch.sectioninformatique.auth.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Session endpoints: obtaining, refreshing and revoking tokens.
 *
 * The access token is returned in the response body and must be sent by the client
 * in the "Authorization: Bearer" header. The refresh token is only ever exchanged
 * through an HTTP-only cookie.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;
    private final MessageSource messageSource;

    /**
     * Authenticates a user with login and password.
     *
     * @param credentials login and password
     * @return 200 with the user and access token, refresh token in a cookie
     */
    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody @Valid CredentialsDto credentials) {
        AuthResult result = authService.login(credentials);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(result.refreshToken()).toString())
                .body(result.user());
    }

    /**
     * Issues a new access token from the refresh token cookie, and rotates the refresh token.
     *
     * @param refreshToken refresh token read from the cookie
     * @return 200 with the new access token, new refresh token in a cookie
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(
            @CookieValue(RefreshTokenCookieFactory.COOKIE_NAME) String refreshToken) {
        AuthResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(result.refreshToken()).toString())
                .body(new TokenResponseDto(result.user().getToken()));
    }

    /**
     * Revokes the refresh token of the authenticated user and clears the cookie.
     *
     * @param currentUser authenticated user
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDto currentUser) {
        authService.logout(currentUser.getLogin());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .build();
    }

    /**
     * Landing page of the Azure login flow when the client application gave no redirect URL.
     *
     * @return 200 with a localized confirmation message
     */
    @GetMapping("/redirect-after-login")
    public ResponseEntity<Map<String, String>> redirectAfterLogin() {
        return ResponseEntity.ok(Map.of("message",
                messageSource.getMessage("message.login.success", null, LocaleContextHolder.getLocale())));
    }
}
