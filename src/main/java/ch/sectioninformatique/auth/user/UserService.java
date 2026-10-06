package ch.sectioninformatique.auth.user;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.role.RoleRepository;
import ch.sectioninformatique.auth.security.SecurityExceptions.InsufficientRightsException;
import ch.sectioninformatique.auth.user.UserExceptions.DeletedAccountException;
import ch.sectioninformatique.auth.user.UserExceptions.InvalidCurrentPasswordException;
import ch.sectioninformatique.auth.user.UserExceptions.RoleNotFoundException;
import ch.sectioninformatique.auth.user.UserExceptions.SelfModificationForbiddenException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyExistsException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyHasRoleException;
import ch.sectioninformatique.auth.user.UserExceptions.UserNotFoundException;
import ch.sectioninformatique.auth.user.dto.CreateUserDto;
import ch.sectioninformatique.auth.user.dto.PasswordUpdateDto;
import ch.sectioninformatique.auth.user.dto.UpdateUserDto;
import ch.sectioninformatique.auth.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * User management use cases.
 *
 * Endpoint-level permissions (user:read, user:update...) are checked by the controller.
 * This service enforces the rules that depend on the data:
 * - only an admin may act on an admin account or grant the admin role;
 * - nobody may change their own role or delete their own account, so that the
 *   application cannot be left without administrator by mistake.
 *
 * The acting user's role is always read from the database rather than from the access
 * token, so that a role change takes effect immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Sort BY_ID = Sort.by("id");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * @param login user's login
     * @return the active user with this login
     * @throws UserNotFoundException if there is no active user with this login
     */
    public UserDto findByLogin(String login) {
        return userMapper.toUserDto(getActiveUser(login));
    }

    /**
     * @param status which users to return
     * @return the matching users, ordered by id
     */
    public List<UserDto> findAll(UserStatus status) {
        List<User> users = switch (status) {
            case ACTIVE -> userRepository.findAllByDeleted(false, BY_ID);
            case DELETED -> userRepository.findAllByDeleted(true, BY_ID);
            case ALL -> userRepository.findAll(BY_ID);
        };
        return users.stream().map(userMapper::toUserDto).toList();
    }

    /**
     * Creates a user. Granting the admin role requires the actor to be an admin.
     *
     * @param request    creation request; its password array is wiped once hashed
     * @param actorLogin login of the authenticated user
     * @return the created user
     * @throws UserAlreadyExistsException  if the login is already used, even by a soft-deleted user
     * @throws InsufficientRightsException if a non-admin tries to create an admin
     */
    @Transactional
    public UserDto create(CreateUserDto request, String actorLogin) {
        RoleEnum role = request.mainRoleOrDefault();
        assertCanManage(getActiveUser(actorLogin), role);

        if (userRepository.existsByLogin(request.login())) {
            throw new UserAlreadyExistsException(request.login());
        }

        User user = userMapper.toUser(request);
        user.setPassword(hashAndWipe(request.password()));
        user.setMainRole(getRole(role));
        return userMapper.toUserDto(userRepository.save(user));
    }

    /**
     * Updates the identity fields of a user (first name, last name, login).
     *
     * @param login      current login of the user to update
     * @param request    new values
     * @param actorLogin login of the authenticated user
     * @return the updated user
     * @throws UserNotFoundException       if there is no active user with this login
     * @throws UserAlreadyExistsException  if the new login is used by another user
     * @throws InsufficientRightsException if a non-admin tries to update an admin
     */
    @Transactional
    public UserDto update(String login, UpdateUserDto request, String actorLogin) {
        User user = getActiveUser(login);
        assertCanManage(getActiveUser(actorLogin), user.getMainRole().getName());

        if (!request.login().equals(user.getLogin()) && userRepository.existsByLogin(request.login())) {
            throw new UserAlreadyExistsException(request.login());
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setLogin(request.login());
        return userMapper.toUserDto(user);
    }

    /**
     * Assigns a role to a user. Only an admin can grant the admin role or change the
     * role of an admin.
     *
     * @param login      login of the user whose role changes
     * @param newRole    role to assign
     * @param actorLogin login of the authenticated user
     * @return the updated user
     * @throws UserNotFoundException              if there is no active user with this login
     * @throws SelfModificationForbiddenException if users target their own account
     * @throws UserAlreadyHasRoleException        if the user already has this role
     * @throws InsufficientRightsException        if a non-admin grants or removes the admin role
     */
    @Transactional
    public UserDto changeRole(String login, RoleEnum newRole, String actorLogin) {
        assertNotSelf(login, actorLogin);
        User user = getActiveUser(login);
        User actor = getActiveUser(actorLogin);

        RoleEnum currentRole = user.getMainRole().getName();
        if (currentRole == newRole) {
            throw new UserAlreadyHasRoleException(login, newRole);
        }
        assertCanManage(actor, currentRole);
        assertCanManage(actor, newRole);

        user.setMainRole(getRole(newRole));
        log.info("Role of user {} changed from {} to {} by {}", login, currentRole, newRole, actorLogin);
        return userMapper.toUserDto(user);
    }

    /**
     * Deletes a user, softly (restorable) by default.
     *
     * @param login      login of the user to delete
     * @param permanent  true to remove the user from the database
     * @param actorLogin login of the authenticated user
     * @throws UserNotFoundException              if no user, soft-deleted or not, has this login
     * @throws SelfModificationForbiddenException if users target their own account
     * @throws InsufficientRightsException        if a non-admin tries to delete an admin
     */
    @Transactional
    public void delete(String login, boolean permanent, String actorLogin) {
        assertNotSelf(login, actorLogin);
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(login));
        assertCanManage(getActiveUser(actorLogin), user.getMainRole().getName());

        if (permanent) {
            userRepository.deletePermanentlyById(user.getId());
        } else {
            userRepository.delete(user);
        }
        log.info("User {} {} deleted by {}", login, permanent ? "permanently" : "softly", actorLogin);
    }

    /**
     * Restores a soft-deleted user.
     *
     * @param login      login of the user to restore
     * @param actorLogin login of the authenticated user
     * @return the restored user
     * @throws UserNotFoundException       if there is no soft-deleted user with this login
     * @throws InsufficientRightsException if a non-admin tries to restore an admin
     */
    @Transactional
    public UserDto restore(String login, String actorLogin) {
        User user = userRepository.findByLoginAndDeletedTrue(login)
                .orElseThrow(() -> new UserNotFoundException(login));
        assertCanManage(getActiveUser(actorLogin), user.getMainRole().getName());

        user.setDeleted(false);
        return userMapper.toUserDto(user);
    }

    /**
     * Changes the password of a user after checking the current one.
     *
     * @param login   login of the user (always the authenticated user)
     * @param request current and new passwords; both arrays are wiped after use
     * @throws InvalidCurrentPasswordException if the current password is wrong
     */
    @Transactional
    public void updatePassword(String login, PasswordUpdateDto request) {
        try {
            User user = getActiveUser(login);
            if (!passwordEncoder.matches(CharBuffer.wrap(request.oldPassword()), user.getPassword())) {
                throw new InvalidCurrentPasswordException();
            }
            user.setPassword(passwordEncoder.encode(CharBuffer.wrap(request.newPassword())));
        } finally {
            Arrays.fill(request.oldPassword(), '\0');
            Arrays.fill(request.newPassword(), '\0');
        }
    }

    /**
     * Returns the local account of a user authenticated by Azure, creating it on first login
     * with the USER role. Azure users authenticate through Azure only: their local password
     * is a random value nobody knows.
     *
     * @param login     user's email given by Azure
     * @param firstName first name given by Azure
     * @param lastName  last name given by Azure, may be null
     * @return the local user
     * @throws DeletedAccountException if the account exists but is soft-deleted
     */
    @Transactional
    public UserDto getOrCreateAzureUser(String login, String firstName, String lastName) {
        return userRepository.findByLogin(login)
                .map(existing -> {
                    if (existing.isDeleted()) {
                        throw new DeletedAccountException(login);
                    }
                    return userMapper.toUserDto(existing);
                })
                .orElseGet(() -> {
                    User user = User.builder()
                            .login(login)
                            .firstName(firstName)
                            .lastName(lastName)
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            .mainRole(getRole(RoleEnum.USER))
                            .build();
                    log.info("New Azure user registered: {}", login);
                    return userMapper.toUserDto(userRepository.save(user));
                });
    }

    private User getActiveUser(String login) {
        return userRepository.findByLoginAndDeletedFalse(login)
                .orElseThrow(() -> new UserNotFoundException(login));
    }

    private Role getRole(RoleEnum role) {
        return roleRepository.findByName(role).orElseThrow(() -> new RoleNotFoundException(role));
    }

    private String hashAndWipe(char[] password) {
        try {
            return passwordEncoder.encode(CharBuffer.wrap(password));
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Only an admin may act on an admin account or grant the admin role.
     */
    private static void assertCanManage(User actor, RoleEnum targetRole) {
        if (targetRole == RoleEnum.ADMIN && actor.getMainRole().getName() != RoleEnum.ADMIN) {
            throw new InsufficientRightsException(actor.getLogin());
        }
    }

    private static void assertNotSelf(String login, String actorLogin) {
        if (login.equalsIgnoreCase(actorLogin)) {
            throw new SelfModificationForbiddenException();
        }
    }
}
