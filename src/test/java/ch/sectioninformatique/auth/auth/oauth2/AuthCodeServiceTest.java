package ch.sectioninformatique.auth.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidAuthCodeException;
import ch.sectioninformatique.auth.security.TokenHasher;
import ch.sectioninformatique.auth.support.AbstractIntegrationTest;
import ch.sectioninformatique.auth.support.TestUsers;

/**
 * Tests of the one-time authentication codes against the database.
 */
class AuthCodeServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AuthCodeService authCodeService;

    @Autowired
    private AuthCodeRepository authCodeRepository;

    @Test
    void generate_storesOnlyTheHashOfTheCode() {
        String code = authCodeService.generate(TestUsers.USER, "http://localhost:3000");

        assertThat(authCodeRepository.findByCodeAndUserLogin(TokenHasher.sha256(code), TestUsers.USER)).isPresent();
        assertThat(authCodeRepository.findByCodeAndUserLogin(code, TestUsers.USER)).isEmpty();
    }

    @Test
    void consume_acceptsTheCodeOnce() {
        String code = authCodeService.generate(TestUsers.USER, "http://localhost:3000");

        assertThatCode(() -> authCodeService.consume(TestUsers.USER, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> authCodeService.consume(TestUsers.USER, code))
                .isInstanceOf(InvalidAuthCodeException.class);
    }

    @Test
    void consume_rejectsCodeOfAnotherUser() {
        String code = authCodeService.generate(TestUsers.USER, "http://localhost:3000");

        assertThatThrownBy(() -> authCodeService.consume(TestUsers.MANAGER, code))
                .isInstanceOf(InvalidAuthCodeException.class);
    }

    @Test
    void consume_rejectsExpiredCode() {
        AuthCodeService shortLived = new AuthCodeService(authCodeRepository, Duration.ofSeconds(-1));
        String code = shortLived.generate(TestUsers.USER, "http://localhost:3000");

        assertThatThrownBy(() -> authCodeService.consume(TestUsers.USER, code))
                .isInstanceOf(InvalidAuthCodeException.class);
    }

    @Test
    void expiredCodes_arePurged() {
        AuthCodeService shortLived = new AuthCodeService(authCodeRepository, Duration.ofSeconds(-1));
        shortLived.generate(TestUsers.USER, "http://localhost:3000");

        assertThat(authCodeRepository.deleteExpired(Instant.now())).isEqualTo(1);
        assertThat(authCodeRepository.count()).isZero();
    }
}
