package ch.sectioninformatique.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import ch.sectioninformatique.auth.user.dto.UserDto;

/**
 * Unit tests of token creation and verification.
 */
class JwtServiceTest {

    private static final String ACCESS_SECRET = "access-secret-for-tests";
    private static final String REFRESH_SECRET = "refresh-secret-for-tests";

    private final JwtService jwtService = new JwtService(ACCESS_SECRET, REFRESH_SECRET,
            Duration.ofMinutes(5), Duration.ofDays(30));

    private static UserDto manager() {
        return UserDto.builder()
                .login("manager@test.com").firstName("Jane").lastName("Smith")
                .mainRole("MANAGER").permissions(List.of("user:read", "user:write", "user:update"))
                .build();
    }

    @Test
    void accessToken_roundTripRebuildsUserAndAuthorities() {
        Authentication authentication = jwtService.validateAccessToken(jwtService.createAccessToken(manager()));

        UserDto principal = (UserDto) authentication.getPrincipal();
        assertThat(principal.getLogin()).isEqualTo("manager@test.com");
        assertThat(principal.getFirstName()).isEqualTo("Jane");
        assertThat(principal.getMainRole()).isEqualTo("MANAGER");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_MANAGER", "user:read", "user:write", "user:update");
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void accessToken_expiresAfterItsLifetime() {
        String token = jwtService.createAccessToken(manager(), Instant.now().minus(Duration.ofMinutes(6)));

        assertThatThrownBy(() -> jwtService.validateAccessToken(token)).isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void accessToken_signedWithAnotherSecret_isRejected() {
        JwtService otherService = new JwtService("another-secret", REFRESH_SECRET,
                Duration.ofMinutes(5), Duration.ofDays(30));
        String token = otherService.createAccessToken(manager());

        assertThatThrownBy(() -> jwtService.validateAccessToken(token))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void refreshToken_isNotAcceptedAsAccessToken() {
        String refreshToken = jwtService.createRefreshToken(manager());

        assertThatThrownBy(() -> jwtService.validateAccessToken(refreshToken))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void accessToken_isNotAcceptedAsRefreshToken() {
        String accessToken = jwtService.createAccessToken(manager());

        assertThatThrownBy(() -> jwtService.validateRefreshToken(accessToken))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void accessToken_withUnknownRole_isRejected() {
        String token = jwtService.createAccessToken(manager().toBuilder().mainRole("SUPERUSER").build());

        assertThatThrownBy(() -> jwtService.validateAccessToken(token)).isInstanceOf(InvalidClaimException.class);
    }

    @Test
    void correctlySignedTokenWithoutAccessType_isRejected() {
        String signingKey = Base64.getEncoder().encodeToString(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        String token = JWT.create().withSubject("manager@test.com").withClaim("mainRole", "ADMIN")
                .sign(Algorithm.HMAC256(signingKey));

        assertThatThrownBy(() -> jwtService.validateAccessToken(token)).isInstanceOf(InvalidClaimException.class);
    }

    @Test
    void refreshToken_onlyCarriesTheLogin() {
        DecodedJWT decoded = jwtService.validateRefreshToken(jwtService.createRefreshToken(manager()));

        assertThat(decoded.getSubject()).isEqualTo("manager@test.com");
        assertThat(decoded.getClaim("permissions").isMissing()).isTrue();
        assertThat(decoded.getExpiresAtAsInstant()).isAfter(Instant.now().plus(Duration.ofDays(29)));
    }
}
