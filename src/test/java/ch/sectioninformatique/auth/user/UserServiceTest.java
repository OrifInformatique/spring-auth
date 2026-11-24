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
import ch.sectioninformatique.auth.user.dto.UpdateUserDto;
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

    private static final Long ADMIN_ID = 1L;
    private static final Long MANAGER_ID = 2L;
    private static final Long USER_ID = 3L;

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

    private User givenActiveUser(Long id, String login, RoleEnum role) {
        User user = User.builder().id(id).firstName("First").lastName("Last")
                .login(login).password(passwordEncoder.encode("Password123!")).mainRole(role(role)).build();
        when(userRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(user));
        return user;
    }

    private void givenRoleExists(RoleEnum name) {
        when(roleRepository.findByName(name)).thenReturn(Optional.of(role(name)));
    }

    @Test
    void findById_unknownUser_throwsNotFound() {
        when(userRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void create_hashesPasswordWipesItAndDefaultsToUserRole() {
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);
        givenRoleExists(RoleEnum.USER);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        char[] password = "NewUser123!".toCharArray();

        UserDto created = userService.create(
                new CreateUserDto("New", "User", "new@test.com", password, null), MANAGER_ID);

        assertThat(created.getMainRole()).isEqualTo("USER");
        assertThat(password).containsOnly('\0');
        verify(userRepository).save(argThat(user ->
                passwordEncoder.matches("NewUser123!", user.getPassword())));
    }

    @Test
    void create_existingLogin_throwsConflict() {
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);
        when(userRepository.existsByLogin(USER)).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new CreateUserDto("New", "User", USER, "NewUser123!".toCharArray(), RoleEnum.USER), MANAGER_ID))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_adminByManager_throwsInsufficientRights() {
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.create(
                new CreateUserDto("New", "Admin", "new@test.com", "NewUser123!".toCharArray(), RoleEnum.ADMIN), MANAGER_ID))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_managerPromotesUserToManager() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);
        givenRoleExists(RoleEnum.MANAGER);

        UserDto updated = userService.changeRole(USER_ID, RoleEnum.MANAGER, MANAGER_ID);

        assertThat(updated.getMainRole()).isEqualTo("MANAGER");
        assertThat(user.getMainRole().getName()).isEqualTo(RoleEnum.MANAGER);
    }

    @Test
    void changeRole_managerCannotGrantAdmin() {
        givenActiveUser(USER_ID, USER, RoleEnum.USER);
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.changeRole(USER_ID, RoleEnum.ADMIN, MANAGER_ID))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_managerCannotDemoteAdmin() {
        givenActiveUser(ADMIN_ID, ADMIN, RoleEnum.ADMIN);
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.changeRole(ADMIN_ID, RoleEnum.USER, MANAGER_ID))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void changeRole_ofOwnAccount_isForbidden() {
        assertThatThrownBy(() -> userService.changeRole(ADMIN_ID, RoleEnum.USER, ADMIN_ID))
                .isInstanceOf(SelfModificationForbiddenException.class);
    }

    @Test
    void changeRole_toCurrentRole_throwsConflict() {
        givenActiveUser(USER_ID, USER, RoleEnum.USER);
        givenActiveUser(ADMIN_ID, ADMIN, RoleEnum.ADMIN);

        assertThatThrownBy(() -> userService.changeRole(USER_ID, RoleEnum.USER, ADMIN_ID))
                .isInstanceOf(UserAlreadyHasRoleException.class);
    }

    @Test
    void userAlreadyHasRole_idIsPassedAsTextToAvoidNumberFormatting() {
        // MessageFormat would render 1234 as "1,234" (en) or "1 234" (fr)
        UserAlreadyHasRoleException exception = new UserAlreadyHasRoleException(1234L, RoleEnum.MANAGER);

        assertThat(exception.getMessageArgs()).containsExactly("1234", "MANAGER");
    }

    @Test
    void changeRole_usesActorRoleFromDatabaseNotFromToken() {
        // The actor was demoted to USER: even with an old MANAGER token, the database role applies
        givenActiveUser(USER_ID, USER, RoleEnum.USER);
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.USER);

        assertThatThrownBy(() -> userService.changeRole(USER_ID, RoleEnum.ADMIN, MANAGER_ID))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void delete_softByDefault() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        givenActiveUser(ADMIN_ID, ADMIN, RoleEnum.ADMIN);

        userService.delete(USER_ID, false, ADMIN_ID);

        verify(userRepository).delete(user);
        verify(userRepository, never()).deletePermanentlyById(any());
    }

    @Test
    void delete_permanent() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        givenActiveUser(ADMIN_ID, ADMIN, RoleEnum.ADMIN);

        userService.delete(USER_ID, true, ADMIN_ID);

        verify(userRepository).deletePermanentlyById(user.getId());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_ownAccount_isForbidden() {
        assertThatThrownBy(() -> userService.delete(ADMIN_ID, false, ADMIN_ID))
                .isInstanceOf(SelfModificationForbiddenException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void update_changesTargetUserNotActor() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        User manager = givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        UserDto updated = userService.update(USER_ID, new UpdateUserDto("New", "Name", "renamed@test.com"), MANAGER_ID);

        assertThat(updated.getId()).isEqualTo(USER_ID);
        assertThat(user.getLogin()).isEqualTo("renamed@test.com");
        assertThat(manager.getLogin()).isEqualTo(MANAGER);
    }

    @Test
    void update_adminByManager_throwsInsufficientRights() {
        givenActiveUser(ADMIN_ID, ADMIN, RoleEnum.ADMIN);
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        assertThatThrownBy(() -> userService.update(ADMIN_ID,
                new UpdateUserDto("New", "Name", "renamed@test.com"), MANAGER_ID))
                .isInstanceOf(InsufficientRightsException.class);
    }

    @Test
    void restore_softDeletedUser_isReactivated() {
        User deleted = User.builder().id(USER_ID).login(USER).deleted(true).mainRole(role(RoleEnum.USER)).build();
        when(userRepository.findByIdAndDeletedTrue(USER_ID)).thenReturn(Optional.of(deleted));
        givenActiveUser(MANAGER_ID, MANAGER, RoleEnum.MANAGER);

        UserDto restored = userService.restore(USER_ID, MANAGER_ID);

        assertThat(restored.isDeleted()).isFalse();
        assertThat(deleted.isDeleted()).isFalse();
    }

    @Test
    void restore_userNotSoftDeleted_throwsNotFound() {
        when(userRepository.findByIdAndDeletedTrue(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.restore(USER_ID, MANAGER_ID))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updatePassword_checksCurrentPasswordAndWipesBothArrays() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        char[] oldPassword = "Password123!".toCharArray();
        char[] newPassword = "BrandNew123!".toCharArray();

        userService.updatePassword(USER_ID, new PasswordUpdateDto(oldPassword, newPassword));

        assertThat(passwordEncoder.matches(CharBuffer.wrap("BrandNew123!"), user.getPassword())).isTrue();
        assertThat(oldPassword).containsOnly('\0');
        assertThat(newPassword).containsOnly('\0');
    }

    @Test
    void updatePassword_wrongCurrentPassword_throwsAndKeepsPassword() {
        User user = givenActiveUser(USER_ID, USER, RoleEnum.USER);
        String previousHash = user.getPassword();

        assertThatThrownBy(() -> userService.updatePassword(USER_ID,
                new PasswordUpdateDto("Wrong123!".toCharArray(), "BrandNew123!".toCharArray())))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        assertThat(user.getPassword()).isEqualTo(previousHash);
    }

    @Test
    void getOrCreateAzureUser_existingUser_isReturnedUnchanged() {
        User existing = givenActiveUser(USER_ID, USER, RoleEnum.MANAGER);
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
