package ch.sectioninformatique.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.user.dto.UserDto;

/**
 * Creates and verifies the JWTs issued by the application.
 *
 * Two kinds of tokens are signed with distinct HMAC-SHA256 keys:
 * - access tokens: short-lived, sent as "Authorization: Bearer" header, carry the user's
 *   identity, role and permissions so that requests are authorized without a database query;
 * - refresh tokens: long-lived, sent as an HTTP-only cookie, only carry the user's login
 *   and are additionally checked against the database by the refresh token service.
 */
@Component
public class JwtService {

    private static final String TYPE_CLAIM = "typ";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    private final Algorithm accessAlgorithm;
    private final Algorithm refreshAlgorithm;
    private final Duration accessTokenLifetime;
    private final Duration refreshTokenLifetime;

    /**
     * @param secretAccessKey      secret used to sign access tokens
     * @param secretRefreshKey     secret used to sign refresh tokens
     * @param accessTokenLifetime  validity of access tokens (e.g. 5m)
     * @param refreshTokenLifetime validity of refresh tokens (e.g. 30d)
     */
    public JwtService(
            @Value("${SECURITY_JWT_TOKEN_SECRET_ACCESS_KEY}") String secretAccessKey,
            @Value("${SECURITY_JWT_TOKEN_SECRET_REFRESH_KEY}") String secretRefreshKey,
            @Value("${SECURITY_JWT_TOKEN_ACCESS_TOKEN_LIFETIME:5m}") Duration accessTokenLifetime,
            @Value("${SECURITY_JWT_TOKEN_REFRESH_TOKEN_LIFETIME:30d}") Duration refreshTokenLifetime) {
        // Signing keys are derived from the Base64 form of the secrets. This derivation is kept
        // so that tokens issued by previous versions of the application remain valid.
        this.accessAlgorithm = Algorithm.HMAC256(base64(secretAccessKey));
        this.refreshAlgorithm = Algorithm.HMAC256(base64(secretRefreshKey));
        this.accessTokenLifetime = accessTokenLifetime;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    /**
     * Creates an access token valid from now.
     *
     * @param user user the token is issued for
     * @return signed JWT
     */
    public String createAccessToken(UserDto user) {
        return createAccessToken(user, Instant.now());
    }

    /**
     * Creates an access token with an explicit issue date (used to build expired tokens in tests).
     *
     * @param user     user the token is issued for
     * @param issuedAt issue date; the token expires {@code accessTokenLifetime} later
     * @return signed JWT
     */
    public String createAccessToken(UserDto user, Instant issuedAt) {
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(user.getLogin())
                .withClaim(TYPE_CLAIM, ACCESS_TYPE)
                .withIssuedAt(issuedAt)
                .withExpiresAt(issuedAt.plus(accessTokenLifetime))
                .withClaim("firstName", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withClaim("mainRole", user.getMainRole())
                .withClaim("permissions", user.getPermissions())
                .sign(accessAlgorithm);
    }

    /**
     * Creates a refresh token valid for {@code refreshTokenLifetime}.
     *
     * @param user user the token is issued for
     * @return signed JWT carrying only the user's login
     */
    public String createRefreshToken(UserDto user) {
        Instant now = Instant.now();
        // The unique id guarantees that two tokens issued in the same second differ,
        // otherwise refresh token rotation would not invalidate the previous token
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(user.getLogin())
                .withClaim(TYPE_CLAIM, REFRESH_TYPE)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(refreshTokenLifetime))
                .sign(refreshAlgorithm);
    }

    /**
     * Verifies an access token and builds the corresponding Spring Security authentication.
     * The principal is a {@link UserDto} rebuilt from the token claims.
     *
     * @param token raw JWT
     * @return authenticated token holding the user and the authorities of their role
     * @throws JWTVerificationException if the token is malformed, expired, badly signed
     *                                  or does not carry a known role
     */
    public Authentication validateAccessToken(String token) {
        DecodedJWT decoded = JWT.require(accessAlgorithm)
                .withClaim(TYPE_CLAIM, ACCESS_TYPE)
                .build()
                .verify(token);

        RoleEnum role = parseRole(decoded.getClaim("mainRole").asString());
        List<String> permissions = decoded.getClaim("permissions").asList(String.class);

        UserDto user = UserDto.builder()
                .login(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .mainRole(role.name())
                .permissions(permissions != null ? permissions : List.of())
                .build();

        return new UsernamePasswordAuthenticationToken(user, null, role.getGrantedAuthorities());
    }

    /**
     * Verifies the signature, expiration and type of a refresh token.
     *
     * @param token raw JWT
     * @return the decoded token
     * @throws JWTVerificationException if the token is invalid
     */
    public DecodedJWT validateRefreshToken(String token) {
        return JWT.require(refreshAlgorithm)
                .withClaim(TYPE_CLAIM, REFRESH_TYPE)
                .build()
                .verify(token);
    }

    public Duration getRefreshTokenLifetime() {
        return refreshTokenLifetime;
    }

    private static RoleEnum parseRole(String role) {
        try {
            return RoleEnum.valueOf(role);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidClaimException("The token does not carry a valid role");
        }
    }

    private static String base64(String secret) {
        return Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
    }
}
