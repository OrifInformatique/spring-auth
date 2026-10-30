package ch.sectioninformatique.auth.auth.token;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth0.jwt.exceptions.JWTVerificationException;

import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidRefreshTokenException;
import ch.sectioninformatique.auth.security.JwtService;
import ch.sectioninformatique.auth.security.TokenHasher;
import ch.sectioninformatique.auth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;

/**
 * Issues, verifies and revokes refresh tokens.
 *
 * A user has at most one valid refresh token: issuing a new one replaces the previous
 * (rotation), so a stolen token stops working as soon as the legitimate client refreshes.
 * Only the SHA-256 hash of each token is stored.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /**
     * Creates a refresh token for the user and stores its hash, replacing any previous token.
     *
     * @param user user the token is issued for
     * @return the raw refresh token, to be sent to the client in a cookie
     */
    @Transactional
    public String issue(UserDto user) {
        String token = jwtService.createRefreshToken(user);

        refreshTokenRepository.deleteByUserLogin(user.getLogin());
        // Make sure the delete reaches the database before the insert (unique hash constraint)
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserLogin(user.getLogin());
        refreshToken.setTokenHash(TokenHasher.sha256(token));
        refreshToken.setExpiresAt(Instant.now().plus(jwtService.getRefreshTokenLifetime()));
        refreshTokenRepository.save(refreshToken);

        return token;
    }

    /**
     * Checks that a refresh token is correctly signed, not expired, and is the user's
     * current token in the database.
     *
     * @param token raw refresh token
     * @return the login of the token owner
     * @throws InvalidRefreshTokenException if any of the checks fails
     */
    @Transactional(readOnly = true)
    public String verify(String token) {
        String login;
        try {
            login = jwtService.validateRefreshToken(token).getSubject();
        } catch (JWTVerificationException e) {
            throw new InvalidRefreshTokenException();
        }

        String hash = TokenHasher.sha256(token);
        boolean isCurrentToken = refreshTokenRepository.findByUserLoginAndRevokedFalse(login)
                .filter(stored -> hash.equals(stored.getTokenHash()))
                .filter(stored -> stored.getExpiresAt().isAfter(Instant.now()))
                .isPresent();
        if (!isCurrentToken) {
            throw new InvalidRefreshTokenException();
        }
        return login;
    }

    /**
     * Revokes every refresh token of a user.
     *
     * @param login user's login
     */
    @Transactional
    public void revokeAll(String login) {
        refreshTokenRepository.deleteByUserLogin(login);
    }
}
