package ch.sectioninformatique.auth.auth.oauth2;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidAuthCodeException;
import ch.sectioninformatique.auth.security.TokenHasher;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the one-time authentication codes of the OAuth2 flow.
 *
 * After an Azure login, the browser is redirected to the client application with a code
 * instead of tokens, so that tokens never appear in URLs (browser history, server logs).
 * The client then exchanges the code for tokens with a back-channel POST request.
 * Codes are short-lived, single-use, and stored hashed.
 */
@Slf4j
@Service
public class AuthCodeService {

    private final AuthCodeRepository authCodeRepository;
    private final Duration lifetime;

    /**
     * @param authCodeRepository code storage
     * @param lifetime           validity of a code (e.g. 5m)
     */
    public AuthCodeService(AuthCodeRepository authCodeRepository,
            @Value("${SECURITY_AUTHENTICATION_CODES_LIFETIME:5m}") Duration lifetime) {
        this.authCodeRepository = authCodeRepository;
        this.lifetime = lifetime;
    }

    /**
     * Generates a code for the user and stores its hash.
     *
     * @param userLogin   login of the user who logged in
     * @param redirectUrl client URL the code will be sent to
     * @return the raw code, to be sent to the client application
     */
    @Transactional
    public String generate(String userLogin, String redirectUrl) {
        authCodeRepository.deleteExpired(Instant.now());

        String code = UUID.randomUUID().toString();
        AuthCode authCode = new AuthCode();
        authCode.setCode(TokenHasher.sha256(code));
        authCode.setUserLogin(userLogin);
        authCode.setRedirectUrl(redirectUrl);
        authCode.setExpiresAt(Instant.now().plus(lifetime));
        authCodeRepository.save(authCode);

        log.debug("Authentication code generated for user {}", userLogin);
        return code;
    }

    /**
     * Checks and deletes a code, so that it can only be used once.
     *
     * @param userLogin login of the user claiming the code
     * @param code      raw code
     * @throws InvalidAuthCodeException if the code is unknown, expired or belongs to another user
     */
    @Transactional
    public void consume(String userLogin, String code) {
        authCodeRepository.deleteExpired(Instant.now());

        AuthCode authCode = authCodeRepository.findByCodeAndUserLogin(TokenHasher.sha256(code), userLogin)
                .orElseThrow(InvalidAuthCodeException::new);
        authCodeRepository.delete(authCode);
    }
}
