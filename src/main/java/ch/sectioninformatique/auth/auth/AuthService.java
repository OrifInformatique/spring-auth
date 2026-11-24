package ch.sectioninformatique.auth.auth;

import java.nio.CharBuffer;
import java.util.Arrays;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidAuthCodeException;
import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidCredentialsException;
import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidRefreshTokenException;
import ch.sectioninformatique.auth.auth.dto.CredentialsDto;
import ch.sectioninformatique.auth.auth.oauth2.AuthCodeService;
import ch.sectioninformatique.auth.auth.token.RefreshTokenService;
import ch.sectioninformatique.auth.security.JwtService;
import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserMapper;
import ch.sectioninformatique.auth.user.UserRepository;
import ch.sectioninformatique.auth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;

/**
 * Authentication use cases: credentials login, OAuth2 code exchange, token refresh and logout.
 *
 * Every successful authentication issues a new pair of tokens: a short-lived access token
 * (returned in the response body) and a refresh token (sent in an HTTP-only cookie).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCodeService authCodeService;

    /**
     * Tokens issued after a successful authentication.
     *
     * @param user         authenticated user, with the access token in {@code token}
     * @param refreshToken raw refresh token, to be sent in a cookie
     */
    public record AuthResult(UserDto user, String refreshToken) {
    }

    /**
     * Authenticates a user with login and password. Soft-deleted users cannot log in.
     *
     * @param credentials login and password; the password array is wiped after use
     * @return the user and their new tokens
     * @throws InvalidCredentialsException if the login is unknown or the password is wrong
     */
    @Transactional
    public AuthResult login(CredentialsDto credentials) {
        char[] password = credentials.password();
        try {
            User user = userRepository.findByLoginAndDeletedFalse(credentials.login())
                    .orElseThrow(InvalidCredentialsException::new);
            if (!passwordEncoder.matches(CharBuffer.wrap(password), user.getPassword())) {
                throw new InvalidCredentialsException();
            }
            return issueTokens(userMapper.toUserDto(user));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Exchanges the one-time code obtained at the end of the Azure login for tokens.
     *
     * @param userId user id received with the code
     * @param code   one-time authentication code
     * @return the user and their new tokens
     * @throws InvalidAuthCodeException if the user or the code is unknown, or the code expired
     */
    @Transactional
    public AuthResult exchangeAuthCode(Long userId, String code) {
        UserDto user = userRepository.findByIdAndDeletedFalse(userId)
                .map(userMapper::toUserDto)
                .orElseThrow(InvalidAuthCodeException::new);
        authCodeService.consume(user.getLogin(), code);
        return issueTokens(user);
    }

    /**
     * Issues a new access token and rotates the refresh token.
     *
     * @param refreshToken refresh token read from the cookie
     * @return the user and their new tokens
     * @throws InvalidRefreshTokenException if the token is invalid or its owner no longer exists
     */
    @Transactional
    public AuthResult refresh(String refreshToken) {
        String login = refreshTokenService.verify(refreshToken);
        UserDto user = userRepository.findByLoginAndDeletedFalse(login)
                .map(userMapper::toUserDto)
                .orElseThrow(InvalidRefreshTokenException::new);
        return issueTokens(user);
    }

    /**
     * Revokes the user's refresh token, so that no new access token can be obtained.
     * The current access token stays valid until it expires (a few minutes).
     *
     * @param login login of the user logging out
     */
    @Transactional
    public void logout(String login) {
        refreshTokenService.revokeAll(login);
    }

    private AuthResult issueTokens(UserDto user) {
        user.setToken(jwtService.createAccessToken(user));
        return new AuthResult(user, refreshTokenService.issue(user));
    }
}
