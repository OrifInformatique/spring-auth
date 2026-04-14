package ch.sectioninformatique.auth.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.auth.CredentialsDto;
import ch.sectioninformatique.auth.auth.PasswordUpdateDto;
import ch.sectioninformatique.auth.auth.RefreshToken;
import ch.sectioninformatique.auth.auth.RefreshTokenRepository;
import ch.sectioninformatique.auth.auth.SignUpDto;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.security.RoleRepository;
import jakarta.persistence.EntityManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Isolation;

import org.hibernate.Session;

import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidCredentialsException;
import ch.sectioninformatique.auth.auth.AuthExceptions.InvalidRefreshTokenException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyExistsException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyManagerException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyRegularException;
import ch.sectioninformatique.auth.security.SecurityExceptions.UserHasLowerRightsException;
import ch.sectioninformatique.auth.security.SecurityExceptions.HashAlgorithmUnavailableException;
import ch.sectioninformatique.auth.user.UserExceptions.UserNotFoundException;
import ch.sectioninformatique.auth.security.SecurityExceptions.RoleNotFoundException;
import ch.sectioninformatique.auth.user.UserExceptions.UserAlreadyAdminException;

/**
 * Service class for managing user-related operations.
 * This class provides functionality for:
 * - User authentication and registration
 * - User role management (promotion, revocation)
 * - User deletion
 * - Azure user integration
 * - User search and retrieval
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    /** EntityManager for database operations - injected via @PersistenceContext */
    @PersistenceContext
    private EntityManager entityManager;

    /** Repository for user data access */
    private final UserRepository userRepository;

    /** Encoder for password hashing */
    private final PasswordEncoder passwordEncoder;

    /** Repository for role data access */
    private final RoleRepository roleRepository;

    /** Mapper for converting between User entities and DTOs */
    private final UserMapper userMapper;

    private final RefreshTokenRepository refreshTokenRepository;


    /**
     * Authenticates a user with their credentials.
     *
     * @param credentialsDto The user's login credentials
     * @return UserDto containing the authenticated user's information
     * @throws InvalidCredentialsException if the user is not found or the password is invalid (intentionally vague to prevent identification of user mail in use)
     */
    public UserDto login(CredentialsDto credentialsDto) {
        User user = userRepository.findByLogin(credentialsDto.login())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(new String(credentialsDto.password()), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return userMapper.toUserDto(user);
    }

    /**
     * Stores a refresh token for a user, optionally rotating the previous token.
     * 
     * The token is hashed before being saved in the database. Previous tokens
     * are removed to enforce token rotation.
     *
     * @param userLogin    The login/username of the user.
     * @param refreshToken The raw refresh token to store.
     * @param expiresAt    The expiration timestamp of the refresh token.
     */
    @Transactional
    public void storeRefreshToken(String userLogin, String refreshToken, Instant expiresAt) {
        String hashed = hashRefreshToken(refreshToken);

        // remove previous token if rotation enabled
        refreshTokenRepository.deleteByUserLogin(userLogin);
        refreshTokenRepository.flush(); // ensure delete is executed before insert

        RefreshToken token = new RefreshToken();
        token.setUserLogin(userLogin);
        token.setTokenHash(hashed);
        token.setExpiresAt(expiresAt);

        refreshTokenRepository.save(token);
    }

    /**
     * Validates a refresh token and throws if it is invalid or expired.
     *
     * @param userLogin    The login/username of the user.
     * @param refreshToken The raw refresh token to validate.
     * @throws InvalidRefreshTokenException if the token is invalid or expired
     */
    public void assertValidRefreshToken(String userLogin, String refreshToken) {
        String hashedRefreshToken = hashRefreshToken(refreshToken);
        boolean valid = refreshTokenRepository.findByUserLoginAndRevokedFalse(userLogin)
                .filter(stored -> hashedRefreshToken.equals(stored.getTokenHash()))
                .filter(stored -> stored.getExpiresAt().isAfter(Instant.now()))
                .isPresent();

        if (!valid) {
            throw new InvalidRefreshTokenException();
        }
    }

    /**
     * Revokes a refresh token for a given user.
     * 
     * Once revoked, the token cannot be used to obtain new access tokens.
     *
     * @param userLogin The login/username of the user whose token should be
     *                  revoked.
     */
    public void revokeRefreshToken(String userLogin) {
        refreshTokenRepository.findByUserLoginAndRevokedFalse(userLogin)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Hashes a refresh token using SHA-256 before storing it in the database.
     * This provides an extra layer of security by ensuring that even if the
     * database is compromised, the raw tokens cannot be retrieved.
     * 
     * Storing hashed tokens prevents the raw token from being exposed in case
     * of a database compromise.
     *
     * @param token The raw refresh token to hash.
     * @return The Base64-encoded SHA-256 hash of the token.
     */
    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashAlgorithmUnavailableException();
        }
    }

    /**
     * Registers a new user in the system.
     * This method:
     * - Checks if the login is already taken
     * - Encodes the password
     * - Assigns the default USER role
     * - Saves the user to the database
     *
     * @param userDto The user registration data
     * @return UserDto containing the created user's information
     * @throws UserAlreadyExistsException if the login already exists 
     * @throws RoleNotFoundException if the role isn't found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto register(SignUpDto userDto) {
        Optional<User> optionalUser = userRepository.findByLogin(userDto.login());
                
       optionalUser.ifPresent(user -> {
            throw new UserAlreadyExistsException(user.getLogin());
        });

        // IMPORTANT: Encode the password BEFORE mapping, because the mapper's @AfterMapping
        // clears the password array for security reasons
        String encodedPassword = passwordEncoder.encode(new String(userDto.password()));
        
        User user = userMapper.signUpToUser(userDto);
        user.setPassword(encodedPassword);

        // Add default USER role
        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.USER));

        user.setMainRole(userRole);

        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    /**
     * Update the User Password
     * 
     * @param login       The user email
     * @param newPassword A password Dto who contain bothe the old password for verification and the new for update
     * @throws InvalidCredentialsException if the user is not found or the old password is invalid
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updatePassword(String login, PasswordUpdateDto passwords) {

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(new String(passwords.oldPassword()), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String encodedPassword = passwordEncoder.encode(new String(passwords.newPassword()));
        user.setPassword(encodedPassword);
    }

    /**
     * Finds a user by their login.
     * This method includes detailed logging for debugging purposes.
     *
     * @param login The user's login
     * @return UserDto containing the user's information
     * @throws UserNotFoundException if the user is not found
     */
    public UserDto findByLogin(String login) {
        log.debug("Searching for user with login: {}", login);

        Optional<User> userOptional = userRepository.findByLoginAndDeletedFalse(login);
        log.debug("User found in database: {}", userOptional.isPresent());

        User user = userOptional
                .orElseThrow(() -> {
                    log.error("User not found with login: {}", login);
                    return new UserNotFoundException(login);
                });

        log.debug("User details - ID: {}, FirstName: {}, LastName: {}, Roles: {}",
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getMainRole());

        UserDto userDto = userMapper.toUserDto(user);
        log.debug("Mapped to UserDto - ID: {}, FirstName: {}, LastName: {}, Role: {}",
                userDto.getId(), userDto.getFirstName(), userDto.getLastName(), userDto.getMainRole());

        return userDto;
    }

    /**
     * Retrieves all users who are not soft-deleted in the system.
     *
     * @return List of all User entities, excluding soft-deleted
     */
    public List<UserDto> allUsers() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedFilter").setParameter("isDeleted", false);
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users.stream().map(userMapper::toUserDto).toList();
    }

    /**
     * Retrieves all users including soft-deleted ones.
     *
     * @return List of all User entities including soft-deleted
     */
    public List<UserDto> allWithDeletedUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAllWithDeleted().forEach(users::add);
        return users.stream().map(userMapper::toUserDto).toList();
    }

    /**
     * Retrieves only soft-deleted users.
     *
     * @return List of soft-deleted User entities
     */
    public List<UserDto> deletedUsers() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedFilter").setParameter("isDeleted", true);
        List<User> users = new ArrayList<>();
        userRepository.findAllDeleted().forEach(users::add);
        return users.stream().map(userMapper::toUserDto).toList();
    }

    /**
     * Restore a soft deleted user
     * 
     * @param userId The ID of the user to restore
     * @return UserDto containing the restored user's information
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto restoreDeletedUser(Long userId) {
        User user = userRepository.findByIdDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.setDeleted(false);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Promotes a user to the manager role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already an manager or admin
     * - Removes existing roles and assigns the manager role
     *
     * @param userId The ID of the user to promote
     * @return UserDto containing the updated user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserAlreadyManagerException if the user is alreydy a manager
     * @throws UserAlreadyAdminException if the user is alreydy an admin
     * @throws RoleNotFoundException if the role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto promoteToManager(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.getMainRole().getName().equals(RoleEnum.MANAGER)) {
            throw new UserAlreadyManagerException(user.getLogin());
        }

        if (user.getMainRole().getName().equals(RoleEnum.ADMIN)) {
            throw new UserAlreadyAdminException(user.getLogin());
        }

        Role managerRole = roleRepository.findByName(RoleEnum.MANAGER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.MANAGER));

        user.setMainRole(managerRole);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Revokes the manager role from a user.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a regular user or admin
     * - Removes existing roles and assigns the user role
     *
     * @param userId The ID of the user to revoke the manager role from
     * @return UserDto containing the updated user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserAlreadyRegularException if the user is already a regular user
     * @throws UserAlreadyAdminException if the user is an admin
     * @throws RoleNotFoundException if the user role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto revokeManagerRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new UserAlreadyRegularException(user.getLogin());
        }

        if (user.getMainRole().getName().equals(RoleEnum.ADMIN)) {
            throw new UserAlreadyAdminException(user.getLogin());
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.USER));

        user.setMainRole(userRole);
        userRepository.save(user);

        return userMapper.toUserDto(user);
    }

    /**
     * Promotes a user to the admin role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already an admin
     * - Removes existing roles and assigns the admin role
     *
     * @param userId The ID of the user to promote
     * @return UserDto containing the updated user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserAlreadyAdminException if the user is already an admin
     * @throws RoleNotFoundException if the admin role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.getMainRole().getName().equals(RoleEnum.ADMIN)) {
            throw new UserAlreadyAdminException(user.getLogin());
        }

        Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.ADMIN));

        user.setMainRole(adminRole);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Downgrades an admin to a manager role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a manager or has lower rights
     * - Removes existing roles and assigns the manager role
     *
     * @param userId The ID of the user to downgrade
     * @return UserDto containing the updated user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserHasLowerRightsException if the user has lower rights than admin
     * @throws UserAlreadyManagerException if the user is already a manager
     * @throws RoleNotFoundException if the manager role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto downgradeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new UserHasLowerRightsException(user.getLogin());
        }
        if (user.getMainRole().getName().equals(RoleEnum.MANAGER)) {
            throw new UserAlreadyManagerException(user.getLogin());
        }

        Role managerRole = roleRepository.findByName(RoleEnum.MANAGER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.MANAGER));

        user.setMainRole(managerRole);
        userRepository.save(user);

        return userMapper.toUserDto(user);
    }

    /**
     * Revokes the admin role from a user.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a regular user or manager
     * - Removes existing roles and assigns the user role
     *
     * @param userId The ID of the user to revoke the admin role from
     * @return UserDto containing the updated user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserAlreadyRegularException if the user is already a regular user
     * @throws UserAlreadyManagerException if the user is a manager
     * @throws RoleNotFoundException if the user role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto revokeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new UserAlreadyRegularException(user.getLogin());
        }

        if (user.getMainRole().getName().equals(RoleEnum.MANAGER)) {
            throw new UserAlreadyManagerException(user.getLogin());
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.USER));

        user.setMainRole(userRole);
        userRepository.save(user);

        return userMapper.toUserDto(user);
    }

    /**
     * Checks if an actor can perform an action on a target based on their roles.
     * The hierarchy is:
     * - ADMIN can perform actions on all roles
     * - MANGER can perform actions on USER and MANAGER roles
     * - USER cannot perform actions on any role
     *
     * @param actorRole  The role of the actor performing the action
     * @param targetRole The role of the target of the action
     * @return true if the actor can perform the action, false otherwise
     */
    private boolean canPerformAction(RoleEnum actorRole, RoleEnum targetRole) {
        switch (actorRole) {
            case ADMIN:
                return true;
            case MANAGER:
                if (targetRole == RoleEnum.ADMIN) {
                    return false;
                }
                return true;
            case USER:
                return false;
            default:
                return false;
        }
    }

    /**
     * Soft-deletes a user from the system.
     * This operation:
     * - Verifies the user exists
     * - Checks if the authenticated user has sufficient permissions
     * - Soft-deletes the user
     *
     * @param userId The ID of the user to delete
     * @return UserDto containing the deleted user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserHasLowerRightsException if the authenticated user lacks permissions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto deleteUser(Long userId) {
        // Get the user to delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        // Get the authenticated user (the actor)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto authenticatedUser = (UserDto) authentication.getPrincipal();

        // Get the full user entity for the authenticated user
        User authenticatedUserEntity = userRepository.findByLogin(authenticatedUser.getLogin())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getLogin()));

        // Check if the action is authorized
        if (!canPerformAction(authenticatedUserEntity.getMainRole().getName(), userToDelete.getMainRole().getName())) {
            throw new UserHasLowerRightsException(authenticatedUserEntity.getLogin());
        }

        // Delete the user
        userRepository.delete(userToDelete);
        return userMapper.toUserDto(userToDelete);
    }

    /**
     * Permanently deletes a user from the system.
     * This operation:
     * - Verifies the user exists
     * - Checks if the authenticated user has sufficient permissions
     * - Permanently deletes the user
     *
     * @param userId The ID of the user to permanently delete
     * @return UserDto containing the deleted user's information
     * @throws UserNotFoundException if the user is not found
     * @throws UserHasLowerRightsException if the authenticated user lacks permissions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto deletePermanentUser(Long userId) {
        // Get the user to delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        // Get the authenticated user (the actor)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto authenticatedUser = (UserDto) authentication.getPrincipal();

        // Get the full user entity for the authenticated user
        User authenticatedUserEntity = userRepository.findByLogin(authenticatedUser.getLogin())
                .orElseThrow(() -> new UserNotFoundException(authenticatedUser.getLogin()));

        // Check if the action is authorized
        if (!canPerformAction(authenticatedUserEntity.getMainRole().getName(), userToDelete.getMainRole().getName())) {
            throw new UserHasLowerRightsException(authenticatedUser.getLogin());
        }

        // Delete the user
        userRepository.deletePermanentlyById(userId);
        return userMapper.toUserDto(userToDelete);
    }

    /**
     * Creates a new user from Azure authentication.
     * This method:
     * - Checks if the user already exists
     * - Creates a new user with Azure data if they don't exist
     * - Assigns the default USER role
     * - Generates a temporary password
     *
     * @param userDto The user data from Azure
     * @return UserDto containing the created user's information
     * @throws RoleNotFoundException if the default role is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createAzureUser(UserDto userDto) {
        log.debug("Creating new Azure user: {}", userDto.getLogin());

        // Check if user already exists
        if (userRepository.existsByLogin(userDto.getLogin())) {
            log.debug("User already exists: {}", userDto.getLogin());
            return findByLogin(userDto.getLogin());
        }

        // Create new user
        User user = User.builder()
                .login(userDto.getLogin())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .password(passwordEncoder.encode("AzureUser" + System.currentTimeMillis())) // Temporary password
                .build();

        // Add default USER role
        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.USER));
        user.setMainRole(userRole);

        // Save the user
        User savedUser = userRepository.save(user);
        log.debug("Azure user created successfully: {}", savedUser.getLogin());

        return userMapper.toUserDto(savedUser);
    }

    /**
     * Deletes all refresh tokens associated with a user.
     * This operation is typically used during logout or when a user's
     * authentication needs to be invalidated across all sessions.
     *
     * @param userLogin The login/username of the user whose tokens should be deleted
     * @throws UserNotFoundException if the user is not found
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteRefreshTokens(String userLogin) {
        log.debug("Deleting refresh tokens for user: {}", userLogin);
        
        // Verify user exists before deleting tokens
        userRepository.findByLogin(userLogin)
                .orElseThrow(() -> new UserNotFoundException(userLogin));
        
        refreshTokenRepository.deleteByUserLogin(userLogin);
    }

    public User proceedOAuth2User(OAuth2User oAuth2User) {

        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new RoleNotFoundException(RoleEnum.USER));
    
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        return userRepository.findByLogin(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .login(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .mainRole(userRole)
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
