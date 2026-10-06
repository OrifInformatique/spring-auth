package ch.sectioninformatique.auth.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ch.sectioninformatique.auth.role.RoleEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Bean Validation rules of the user request bodies.
 */
class UserDtoValidationTest {

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

    private static CreateUserDto createUser(String firstName, String lastName, String login, String password) {
        return new CreateUserDto(firstName, lastName, login, password == null ? null : password.toCharArray(), null);
    }

    @Test
    void createUser_validRequest_passes() {
        assertThat(invalidFields(createUser("John", "Doe", "john.doe@test.com", "Password1!"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Jean-Pierre", "O'Connor", "Ana Lucía", "Zoë", "Łukasz", "Ab" })
    void createUser_internationalAndComposedNames_pass(String name) {
        assertThat(invalidFields(createUser(name, name, "john.doe@test.com", "Password1!"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "R2D2", "-John", "John-", "A", "   ", "Robert'); DROP TABLE users;--" })
    void createUser_invalidNames_fail(String name) {
        assertThat(invalidFields(createUser(name, name, "john.doe@test.com", "Password1!")))
                .contains("firstName", "lastName");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "not-an-email", "john@", "@test.com" })
    void createUser_invalidLogin_fails(String login) {
        assertThat(invalidFields(createUser("John", "Doe", login, "Password1!"))).contains("login");
    }

    @Test
    void createUser_passwordLengthLimits() {
        assertThat(invalidFields(createUser("John", "Doe", "john@test.com", "1234567"))).contains("password");
        assertThat(invalidFields(createUser("John", "Doe", "john@test.com", "12345678"))).isEmpty();
        assertThat(invalidFields(createUser("John", "Doe", "john@test.com", "a".repeat(72)))).isEmpty();
        assertThat(invalidFields(createUser("John", "Doe", "john@test.com", "a".repeat(73)))).contains("password");
        assertThat(invalidFields(createUser("John", "Doe", "john@test.com", null))).contains("password");
    }

    @Test
    void createUser_roleIsOptionalAndDefaultsToUser() {
        assertThat(createUser("John", "Doe", "john@test.com", "Password1!").mainRoleOrDefault())
                .isEqualTo(RoleEnum.USER);
        assertThat(new CreateUserDto("John", "Doe", "john@test.com", "Password1!".toCharArray(), RoleEnum.MANAGER)
                .mainRoleOrDefault()).isEqualTo(RoleEnum.MANAGER);
    }

    @Test
    void updateUser_lastNameIsOptional() {
        assertThat(invalidFields(new UpdateUserDto("John", null, "john@test.com"))).isEmpty();
        assertThat(invalidFields(new UpdateUserDto("", "Doe", "invalid"))).containsExactlyInAnyOrder("firstName", "login");
    }

    @Test
    void roleUpdate_requiresRole() {
        assertThat(invalidFields(new RoleUpdateDto(null))).containsExactly("role");
        assertThat(invalidFields(new RoleUpdateDto(RoleEnum.ADMIN))).isEmpty();
    }

    @Test
    void passwordUpdate_newPasswordMustDifferFromOldOne() {
        assertThat(invalidFields(new PasswordUpdateDto("Password1!".toCharArray(), "Password1!".toCharArray())))
                .containsExactly("newPassword");
        assertThat(invalidFields(new PasswordUpdateDto("Password1!".toCharArray(), "Password2!".toCharArray())))
                .isEmpty();
    }

    @Test
    void passwordUpdate_requiresBothPasswordsAndValidLength() {
        assertThat(invalidFields(new PasswordUpdateDto(null, null))).contains("oldPassword", "newPassword");
        assertThat(invalidFields(new PasswordUpdateDto("Password1!".toCharArray(), "short".toCharArray())))
                .containsExactly("newPassword");
    }
}
