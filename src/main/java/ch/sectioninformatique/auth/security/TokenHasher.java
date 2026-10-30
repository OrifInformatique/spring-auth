package ch.sectioninformatique.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Hashes secrets (refresh tokens, authentication codes) before they are stored,
 * so that a database leak does not expose usable credentials.
 *
 * SHA-256 is appropriate here because the hashed values are long random tokens,
 * not user-chosen passwords (those are hashed with BCrypt).
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    /**
     * @param value raw secret
     * @return Base64-encoded SHA-256 hash of the value
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Every Java platform is required to support SHA-256
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
