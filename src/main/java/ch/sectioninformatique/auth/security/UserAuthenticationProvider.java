package ch.sectioninformatique.auth.security;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Provider class for handling user authentication using JWT tokens.
 * This class is responsible for:
 * - Creating and validating JWT tokens
 * - Managing user authentication
 * - Handling OAuth2 integration
 * - Converting user roles and permissions into Spring Security authorities
 * - Managing Azure user creation for OAuth2 users
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class UserAuthenticationProvider {

    /**
     * Secret key for JWT access token signing and verification, configured via environment variable.
     */
    @Value("${SECURITY_JWT_TOKEN_SECRET_ACCESS_KEY}")
    private String secretAccessKey;

    /**
     * Secret key for JWT refresh token verification, configured via environment variable.
     */
    @Value("${SECURITY_JWT_TOKEN_SECRET_REFRESH_KEY}")
    private String secretRefreshKey;

    /**
     * Access token lifetime (e.g., "5m" for 5 minutes), configured via environment variable.
     */
    @Value("${SECURITY_JWT_TOKEN_ACCESS_TOKEN_LIFETIME:5m}")
    private Duration accessTokenLifetime;

    /**
     * Refresh token lifetime (e.g., "30d" for 30 days), configured via environment variable.
     */
    @Value("${SECURITY_JWT_TOKEN_REFRESH_TOKEN_LIFETIME:30d}")
    private Duration refreshTokenLifetime;

    /**
     * Service for user-related operations, including user creation and retrieval
     */
    private final UserService userService;

    /**
     * Initializes the authentication provider by encoding the secret key.
     * This method is called after dependency injection to ensure the secret key
     * is properly encoded before use. The encoding helps prevent the raw secret key
     * from being available in the JVM memory.
     */
    @PostConstruct
    protected void init() {
        // this is to avoid having the raw secret key available in the JVM
        secretAccessKey = Base64.getEncoder().encodeToString(secretAccessKey.getBytes());

        secretRefreshKey = Base64.getEncoder().encodeToString(secretRefreshKey.getBytes());
    }

    /**
     * Creates a JWT token for a user with their information and permissions.
     * The token includes:
     * - User login as subject
     * - First name and last name as claims
     * - Role and permissions as claims
     * - Issue time and expiration time (based on accessTokenLifetime)
     *
     * @param user           The user to create a token for
     * @param creationDate Optional date to use as the token's issue time
     * @return A JWT token string containing the user's information and permissions
     */
    public String createToken(UserDto user, Date... creationDate) {
        Date issueDate = creationDate.length > 0 ? creationDate[0] : new Date();
        Date validity = new Date(issueDate.getTime() + accessTokenLifetime.toMillis());

        Algorithm algorithm = Algorithm.HMAC256(secretAccessKey);
        return JWT.create()
                .withSubject(user.getLogin())
                .withClaim("typ", "access")
                .withIssuedAt(issueDate)
                .withExpiresAt(validity)
                .withClaim("firstName", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withClaim("mainRole", user.getMainRole())
                .withClaim("permissions", user.getPermissions())
                .sign(algorithm);
    }

    /**
     * Creates a JWT token for a user to refresh their access.
     * The token includes:
     * - Issue time and expiration time
     *
     * @param user The user to create a token for
     * @return A JWT token string containing the user's information and permissions
     */
    public String createRefreshToken(UserDto user) {
        Date issueDate = new Date();
        Date validity = new Date(issueDate.getTime() + refreshTokenLifetime.toMillis());

        Algorithm algorithm = Algorithm.HMAC256(secretRefreshKey);
        return JWT.create()
                .withSubject(user.getLogin())
                .withClaim("typ", "refresh")
                .withIssuedAt(issueDate)
                .withExpiresAt(validity)
                .sign(algorithm);
    }

    /**
     * Builds a list of authorities from a role and permissions.
     * This method converts:
     * - Role into a "ROLE_" prefixed authority
     * - Permissions into individual authorities
     * The resulting authorities are used by Spring Security for authorization
     * checks.
     *
     * @param roles       The user's roles (e.g., "USER", "MANAGER")
     * @param permissions List of permission strings (e.g., "user:read",
     *                    "user:write")
     * @return List of SimpleGrantedAuthority objects for Spring Security
     */
    private List<SimpleGrantedAuthority> buildAuthorities(List<String> roles, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            if (role != null && !role.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                Set<SimpleGrantedAuthority> authoritySet = RoleEnum.valueOf(role).getGrantedAuthorities();
                authorities.addAll(authoritySet);
            }
        }

        log.debug("Built authorities for role {}: {}", roles, authorities);
        return authorities;
    }

    /**
     * Validates a JWT token and creates an Authentication object.
     * This method performs basic token validation without checking the database.
     * It verifies:
     * - Token signature using the secret key
     * - Token expiration
     * - Token claims (user information)
     *
     * @param token The JWT token to validate
     * @return Authentication object containing the user's information and
     *         authorities
     */
    public Authentication validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretAccessKey);

        JWTVerifier verifier = JWT.require(algorithm)
                .build();

        DecodedJWT decoded = verifier.verify(token);
        log.debug("Token verified for subject: {}", decoded.getSubject());

        UserDto user = UserDto.builder()
                .login(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .mainRole(decoded.getClaim("mainRole").asString())
                .permissions(decoded.getClaim("permissions").asList(String.class))
                .build();
        List<String> allRoles = new ArrayList<>();
        allRoles.add(user.getMainRole());
        List<SimpleGrantedAuthority> authorities = buildAuthorities(allRoles, user.getPermissions());
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }

    /**
     * Performs strong validation of a JWT token with database verification.
     * This method:
     * 1. Validates the token signature and claims
     * 2. Attempts to find the user in the database
     * 3. If user exists:
     * - Adds default OAuth2 scopes permissions
     * - Preserves existing permissions
     * 4. If user doesn't exist:
     * - Creates a new Azure user with default permissions
     * - Sets role to "USER"
     *
     * @param token The JWT token to validate
     * @return Authentication object containing the user's information and
     *         authorities
     */
    public Authentication validateTokenStrongly(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretAccessKey);

        JWTVerifier verifier = JWT.require(algorithm)
                .build();

        DecodedJWT decoded = verifier.verify(token);
        log.debug("Token strongly verified for subject: {}", decoded.getSubject());

        try {
            // Try to find the user in the database
            UserDto user = userService.findByLogin(decoded.getSubject());

            // Add default permissions for OAuth2 users
            List<String> permissions = new ArrayList<>(List.of(
                    // OAuth2 scopes
                    "SCOPE_openid",
                    "SCOPE_profile",
                    "SCOPE_email",
                    "SCOPE_User.Read"));

            // Add any existing permissions
            if (user.getPermissions() != null) {
                permissions.addAll(user.getPermissions());
            }

            user.setPermissions(permissions);
            user.setToken(token);

            List<String> allRoles = new ArrayList<>();
            allRoles.add(user.getMainRole());

            List<SimpleGrantedAuthority> authorities = buildAuthorities(allRoles, user.getPermissions());
            log.debug("Built authorities for user {}: {}", user.getLogin(), authorities);

            return new UsernamePasswordAuthenticationToken(user, null, authorities);
        } catch (Exception e) {
            // If user doesn't exist, create a new Azure user
            log.debug("User not found, creating new Azure user: {}", decoded.getSubject());

            UserDto newUser = UserDto.builder()
                    .login(decoded.getSubject())
                    .firstName(decoded.getClaim("firstName").asString())
                    .lastName(decoded.getClaim("lastName").asString())
                    .mainRole("USER")
                    .build();

            // Save the new user
            userService.createAzureUser(newUser);

            List<String> allRoles = new ArrayList<>();
            allRoles.add(newUser.getMainRole());

            // Create authentication with authorities
            List<SimpleGrantedAuthority> authorities = buildAuthorities(allRoles,
                    newUser.getPermissions());
            return new UsernamePasswordAuthenticationToken(newUser, null, authorities);
        }
    }

    /**
     * Validates a JWT refresh token and returns its decoded representation.
     * 
     * This method verifies the token's signature using the server's secret key.
     * If the token is invalid, expired, or tampered with, the verifier will throw
     * a JWTVerificationException.
     * 
     * Security considerations:
     * 
     * Always validate refresh tokens before issuing a new access token.
     * Do not trust the token content without verification.
     * Use a strong secret key and keep it secure.
     * 
     *
     * @param token The JWT refresh token to validate.
     * @return A {@link DecodedJWT} object representing the validated token.
     * @throws com.auth0.jwt.exceptions.JWTVerificationException if the token is
     *                                                           invalid or expired.
     */
    public DecodedJWT validateRefreshToken(String token) {
    Algorithm algorithm = Algorithm.HMAC256(secretRefreshKey);
    JWTVerifier verifier = JWT.require(algorithm)
            .withClaim("typ", "refresh")
            .build();

    return verifier.verify(token);
    }
}
