package ch.sectioninformatique.auth.security;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    /** Secret key for JWT token signing and verification, configured via application properties */
    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

    /** Service for user-related operations, including user creation and retrieval */
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
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    /**
     * Creates a JWT token for a user with their information and permissions.
     * The token includes:
     * - User login as subject
     * - First name and last name as claims
     * - Role and permissions as claims
     * - Issue time and expiration time (1 hour validity)
     *
     * @param user The user to create a token for
     * @return A JWT token string containing the user's information and permissions
     */
    public String createToken(UserDto user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 3600000); // 1 hour

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withSubject(user.getLogin())
                .withIssuedAt(now)
                .withExpiresAt(validity)
                .withClaim("firstName", user.getFirstName())
                .withClaim("lastName", user.getLastName())
                .withClaim("role", user.getRole())
                .withClaim("permissions", user.getPermissions())
                .sign(algorithm);
    }

    /**
     * Builds a list of authorities from a role and permissions.
     * This method converts:
     * - Role into a "ROLE_" prefixed authority
     * - Permissions into individual authorities
     * The resulting authorities are used by Spring Security for authorization checks.
     *
     * @param role The user's role (e.g., "USER", "MANAGER")
     * @param permissions List of permission strings (e.g., "user:read", "user:write")
     * @return List of SimpleGrantedAuthority objects for Spring Security
     */
    private List<SimpleGrantedAuthority> buildAuthorities(String role, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        if (permissions != null) {
            authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
        }
        log.debug("Built authorities for role {}: {}", role, authorities);
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
     * @return Authentication object containing the user's information and authorities
     */
    public Authentication validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        JWTVerifier verifier = JWT.require(algorithm)
                .build();

        DecodedJWT decoded = verifier.verify(token);
        log.debug("Token verified for subject: {}", decoded.getSubject());

        UserDto user = UserDto.builder()
                .login(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .role(decoded.getClaim("role").asString())
                .permissions(decoded.getClaim("permissions").asList(String.class))
                .build();

        List<SimpleGrantedAuthority> authorities = buildAuthorities(user.getRole(), user.getPermissions());
        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }

    /**
     * Performs strong validation of a JWT token with database verification.
     * This method:
     * 1. Validates the token signature and claims
     * 2. Attempts to find the user in the database
     * 3. If user exists:
     *    - Adds default OAuth2 scopes permissions
     *    - Preserves existing permissions
     * 4. If user doesn't exist:
     *    - Creates a new Azure user with default permissions
     *    - Sets role to "USER"
     *
     * @param token The JWT token to validate
     * @return Authentication object containing the user's information and authorities
     */
    public Authentication validateTokenStrongly(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);

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
                "SCOPE_User.Read"
            ));

            // Add any existing permissions
            if (user.getPermissions() != null) {
                permissions.addAll(user.getPermissions());
            }

            user.setPermissions(permissions);
            user.setToken(token);
            
            List<SimpleGrantedAuthority> authorities = buildAuthorities(user.getRole(), user.getPermissions());
            log.debug("Built authorities for user {}: {}", user.getLogin(), authorities);
            
            return new UsernamePasswordAuthenticationToken(user, null, authorities);
        } catch (Exception e) {
            // If user doesn't exist, create a new Azure user
            log.debug("User not found, creating new Azure user: {}", decoded.getSubject());
            
            UserDto newUser = UserDto.builder()
                .login(decoded.getSubject())
                .firstName(decoded.getClaim("firstName").asString())
                .lastName(decoded.getClaim("lastName").asString())
                .role("USER")
                .build();

            // Save the new user
            userService.createAzureUser(newUser);
            
            // Create authentication with authorities
            List<SimpleGrantedAuthority> authorities = buildAuthorities(newUser.getRole(), newUser.getPermissions());
            return new UsernamePasswordAuthenticationToken(newUser, null, authorities);
        }
    }
}
