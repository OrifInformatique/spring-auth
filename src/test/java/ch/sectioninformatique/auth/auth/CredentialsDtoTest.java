package ch.sectioninformatique.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import ch.sectioninformatique.auth.auth.dto.AuthCodeExchangeDto;
import ch.sectioninformatique.auth.auth.dto.CredentialsDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Bean Validation rules of the authentication request bodies.
 */
class CredentialsDtoTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    private static Set<String> invalidFields(Object dto) {
        return VALIDATOR.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void credentials_valid() {
        assertThat(invalidFields(new CredentialsDto("john@test.com", "Password1!".toCharArray()))).isEmpty();
    }

    @Test
    void credentials_loginMustBeAnEmail() {
        assertThat(invalidFields(new CredentialsDto("john", "Password1!".toCharArray()))).containsExactly("login");
        assertThat(invalidFields(new CredentialsDto(" ", "Password1!".toCharArray()))).contains("login");
    }

    @Test
    void credentials_passwordLengthIs8To72() {
        assertThat(invalidFields(new CredentialsDto("john@test.com", null))).containsExactly("password");
        assertThat(invalidFields(new CredentialsDto("john@test.com", "1234567".toCharArray()))).containsExactly("password");
        assertThat(invalidFields(new CredentialsDto("john@test.com", "a".repeat(72).toCharArray()))).isEmpty();
        assertThat(invalidFields(new CredentialsDto("john@test.com", "a".repeat(73).toCharArray())))
                .containsExactly("password");
    }

    @Test
    void authCodeExchange_requiresUserIdAndCode() {
        assertThat(invalidFields(new AuthCodeExchangeDto(null, " "))).containsExactlyInAnyOrder("userId", "code");
        assertThat(invalidFields(new AuthCodeExchangeDto(1L, "code"))).isEmpty();
    }
}
