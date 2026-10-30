package ch.sectioninformatique.auth.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.CharBuffer;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.role.RoleRepository;
import ch.sectioninformatique.auth.security.SecurityExceptions.InsufficientRightsException;
import ch.sectioninformatique.auth.user.UserExceptions.DeletedAccountException;
import ch.sectioninformatique.auth.user.UserExceptions.InvalidCurrentPasswordException;
import ch.sectioninformatique.auth.user.UserExceptions.SelfModificationForbiddenException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyExistsException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyHasRoleException;
import ch.sectioninformatique.auth.user.UserExceptions.UserNotFoundException;
import ch.sectioninformatique.auth.user.dto.CreateUserDto;
import ch.sectioninformatique.auth.user.dto.PasswordUpdateDto;
import ch.sectioninformatique.auth.user.dto.UserDto;

/**
 * Unit tests of the business rules of {@link UserService}, with mocked repositories.
 */
@ExtendWith(MockitoExtension.class)
// Lenient: the givenXxx() helpers stub every lookup a user may need, not only the ones a test uses
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    private static final String ADMIN = "admin@test.com";
    private static final String MANAGER = "manager@test.com";
    private static final String USER = "user@test.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    // Real implementations: the service logic is tested together with the actual mapping and hashing
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UserMapper userMapper = new UserMapperImpl();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder, userMapper);
    }

    private static Role role(RoleEnum name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private User givenActiveUser(String login, RoleEnum role) {
        User user = User.builder().id(login.hashCode()).firstName("First").lastName("Last")
                .login(login).password(passwordEncoder.encode("Password123!")).mainRole(role(role)).build();
        when(userRepository.findByLoginAndDeletedFalse(login)).thenReturn(Optional.of(user));
        return user;
    }

    private void givenRoleExists(RoleEnum name) {
        when(roleRepository.findByName(name)).thenReturn(Optional.of(role(name)));
    }

    @Test
    void findByLogin_unknownUser_throwsNotFound() {
        when(userRepository.findByLoginAndDeletedFalse("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByLogin("unknown@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_hashesPasswordWipesItAndDefaultsToUserRole() {
        givenActiveUser(MANAGER, RoleEnum.MANAGER);
        givenRoleExists(RoleEnum.USER);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        char[] password = "NewUser123!".toCharArray();

        UserDto created = userService.create(
                new CreateUserDto("New", "User", "new@test.com", password, null), MANAGER);

        assertThat(created.getMainRole()).isEqualTo("USER");
        assertThat(password).containsOnly('\0');
        verify(userRepository).save(argThat(user ->
                passwordEncoder.matches("NewUser123!", user.getPassword())));
    }

    @Test
    void create_existingLogin_throwsConflict() {
        givenActiveUser(MANAGER, RoleEnum.MANAGER);
        when(userRepository.existsByLogin(USER)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new CreateUserDto("New", "User", USER, "NewUser123!".toCharArray(), RoleEnum.USER), MANAGER))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_adminByManager_throwsInsufficientRights() {
        givenActiveUser(MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.create(
                new CreateUserDto("New", "Admin", "new@test.com", "NewUser123!".toCharArray(), RoleEnum.ADMIN), MANAGER))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_managerPromotesUserToManager() {
        User user = givenActiveUser(USER, RoleEnum.USER);
        givenActiveUser(MANAGER, RoleEnum.MANAGER);
        givenRoleExists(RoleEnum.MANAGER);

        UserDto updated = userService.changeRole(USER, RoleEnum.MANAGER, MANAGER);

        assertThat(updated.getMainRole()).isEqualTo("MANAGER");
        assertThat(user.getMainRole().getName()).isEqualTo(RoleEnum.MANAGER);
    }

    @Test
    void changeRole_managerCannotGrantAdmin() {
        givenActiveUser(USER, RoleEnum.USER);
        givenActiveUser(MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.changeRole(USER, RoleEnum.ADMIN, MANAGER))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_managerCannotDemoteAdmin() {
        givenActiveUser(ADMIN, RoleEnum.ADMIN);
        givenActiveUser(MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.changeRole(ADMIN, RoleEnum.USER, MANAGER))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_ofOwnAccount_isForbiddenWhateverTheCase() {
        assertThatThrownBy(() -> userService.changeRole("Admin@Test.com", RoleEnum.USER, ADMIN))
                .isInstanceOf(SelfModificationForbiddenException.class);
    }

    @Test
    void changeRole_toCurrentRole_throwsConflict() {
        givenActiveUser(USER, RoleEnum.USER);
        givenActiveUser(ADMIN, RoleEnum.ADMIN);

        assertThatThrownBy(() -> userService.changeRole(USER, RoleEnum.USER, ADMIN))
                .isInstanceOf(UserAlreadyHasRoleException.class);
    }

    @Test
    void changeRole_usesActorRoleFromDatabaseNotFromToken() {
        // The actor was demoted to USER: even with an old MANAGER token, the database role applies
        givenActiveUser(USER, RoleEnum.USER);
        givenActiveUser(MANAGER, RoleEnum.USER);

        assertThatThrownBy(() -> userService.changeRole(USER, RoleEnum.ADMIN, MANAGER))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void delete_softByDefault() {
        User user = givenActiveUser(USER, RoleEnum.USER);
        when(userRepository.findByLogin(USER)).thenReturn(Optional.of(user));
        givenActiveUser(ADMIN, RoleEnum.ADMIN);

        userService.delete(USER, false, ADMIN);

        verify(userRepository).delete(user);
        verify(userRepository, never()).deletePermanentlyById(any());
    }

    @Test
    void delete_permanent() {
        User user = givenActiveUser(USER, RoleEnum.USER);
        when(userRepository.findByLogin(USER)).thenReturn(Optional.of(user));
        givenActiveUser(ADMIN, RoleEnum.ADMIN);

        userService.delete(USER, true, ADMIN);

        verify(userRepository).deletePermanentlyById(user.getId());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_ownAccount_isForbidden() {
        assertThatThrownBy(() -> userService.delete(ADMIN, false, ADMIN))
                .isInstanceOf(SelfModificationForbiddenException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void updatePassword_checksCurrentPasswordAndWipesBothArrays() {
        User user = givenActiveUser(USER, RoleEnum.USER);
        char[] oldPassword = "Password123!".toCharArray();
        char[] newPassword = "BrandNew123!".toCharArray();

        userService.updatePassword(USER, new PasswordUpdateDto(oldPassword, newPassword));

        assertThat(passwordEncoder.matches(CharBuffer.wrap("BrandNew123!"), user.getPassword())).isTrue();
        assertThat(oldPassword).containsOnly('\0');
        assertThat(newPassword).containsOnly('\0');
    }

    @Test
    void updatePassword_wrongCurrentPassword_throwsAndKeepsPassword() {
        User user = givenActiveUser(USER, RoleEnum.USER);
        String previousHash = user.getPassword();

        assertThatThrownBy(() -> userService.updatePassword(USER,
                new PasswordUpdateDto("Wrong123!".toCharArray(), "BrandNew123!".toCharArray())))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        assertThat(user.getPassword()).isEqualTo(previousHash);
    }

    @Test
    void getOrCreateAzureUser_existingUser_isReturnedUnchanged() {
        User existing = givenActiveUser(USER, RoleEnum.MANAGER);
        when(userRepository.findByLogin(USER)).thenReturn(Optional.of(existing));

        UserDto user = userService.getOrCreateAzureUser(USER, "Other", "Name");

        assertThat(user.getFirstName()).isEqualTo("First");
        assertThat(user.getMainRole()).isEqualTo("MANAGER");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getOrCreateAzureUser_newUser_isCreatedWithUserRoleAndRandomPassword() {
        when(userRepository.findByLogin("azure@test.com")).thenReturn(Optional.empty());
        givenRoleExists(RoleEnum.USER);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto user = userService.getOrCreateAzureUser("azure@test.com", "Azure", null);

        assertThat(user.getMainRole()).isEqualTo("USER");
        assertThat(user.getLastName()).isNull();
        verify(userRepository).save(argThat(saved ->
                saved.getPassword() != null && !saved.getPassword().isBlank()));
    }

    @Test
    void getOrCreateAzureUser_softDeletedUser_isRejected() {
        User deleted = User.builder().login(USER).deleted(true).mainRole(role(RoleEnum.USER)).build();
        when(userRepository.findByLogin(USER)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> userService.getOrCreateAzureUser(USER, "First", "Last"))
                .isInstanceOf(DeletedAccountException.class);
    }
}
