package ch.sectioninformatique.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenHasherTest {

    @Test
    void sha256_isDeterministicBase64AndDifferentFromInput() {
        String hash = TokenHasher.sha256("my-token");

        assertThat(hash).isEqualTo(TokenHasher.sha256("my-token"))
                .isNotEqualTo("my-token")
                .isNotEqualTo(TokenHasher.sha256("my-token2"))
                .hasSize(44);
        // Known SHA-256 of "abc", Base64-encoded
        assertThat(TokenHasher.sha256("abc")).isEqualTo("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=");
    }
}
