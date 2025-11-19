package ch.sectioninformatique.auth.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.auth.CredentialsDto;
import ch.sectioninformatique.auth.auth.PasswordUpdateDto;
import ch.sectioninformatique.auth.auth.SignUpDto;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.security.RoleRepository;
import jakarta.persistence.EntityManager;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

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

    /** EntityManager for database operations */
    @Autowired
    private EntityManager entityManager;

    /** Repository for user data access */
    @Autowired
    private final UserRepository userRepository;

    /** Encoder for password hashing */
    private final PasswordEncoder passwordEncoder;

    /** Repository for role data access */
    private final RoleRepository roleRepository;

    /** Mapper for converting between User entities and DTOs */
    private final UserMapper userMapper;

    /**
     * Authenticates a user with their credentials.
     *
     * @param credentialsDto The user's login credentials
     * @return UserDto containing the authenticated user's information
     * @throws AppException if the user is not found or the password is invalid
     */
    public UserDto login(CredentialsDto credentialsDto) {
        User user = userRepository.findByLogin(credentialsDto.login())
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), user.getPassword())) {
            return userMapper.toUserDto(user);
        }
        throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Authenticates a user refreshing his login.
     *
     * @param login The user's login
     * @return UserDto containing the authenticated user's information
     * @throws AppException if the user is not found
     */
    public UserDto refreshLogin(String login) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        return userMapper.toUserDto(user);
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
     * @throws AppException if the login already exists or the default role is not
     *                      found
     */
    public UserDto register(SignUpDto userDto) {
        Optional<User> optionalUser = userRepository.findByLogin(userDto.login());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.CONFLICT);
        }

        User user = userMapper.signUpToUser(userDto);
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        // Add default USER role
        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new AppException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));
        user.setMainRole(userRole);

        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
    }

    /**
     * Update the User Password
     * 
     * @param login       The user email
     * @param newPassword A password Dto who contain bothe the old password for
     *                    verification and the new for update
     */
    @Transactional
    public void updatePassword(String login, PasswordUpdateDto passwords) {

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED));

                if(passwordEncoder.matches(CharBuffer.wrap(passwords.oldPassword()), user.getPassword())== false){
                    throw new AppException("Invalid credentials", HttpStatus.UNAUTHORIZED);
                }

        String encodedPassword = passwordEncoder.encode(CharBuffer.wrap(passwords.newPassword()));
        user.setPassword(encodedPassword);
    }

    /**
     * Finds a user by their login.
     * This method includes detailed logging for debugging purposes.
     *
     * @param login The user's login
     * @return UserDto containing the user's information
     * @throws AppException if the user is not found
     */
    public UserDto findByLogin(String login) {
        log.debug("Searching for user with login: {}", login);

        Optional<User> userOptional = userRepository.findByLogin(login);
        log.debug("User found in database: {}", userOptional.isPresent());

        User user = userOptional
                .orElseThrow(() -> {
                    log.error("User not found with login: {}", login);
                    return new AppException("Unknown user", HttpStatus.NOT_FOUND);
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
    public List<User> allUsers() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedFilter").setParameter("isDeleted", false);
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    /**
     * Retrieves all users including soft-deleted ones.
     *
     * @return List of all User entities including soft-deleted
     */
    public List<User> allDeletedUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAllIncludingDeleted().forEach(users::add);
        return users;
    }

    /**
     * Retrieves only soft-deleted users.
     *
     * @return List of soft-deleted User entities
     */
    public List<User> deletedUsers() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedFilter").setParameter("isDeleted", true);
        List<User> users = new ArrayList<>();
        userRepository.findAllDeleted().forEach(users::add);
        return users;
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
     * @throws RuntimeException if the user is not found, already an manager, or the
     *                          manager role is not found
     */
    public UserDto promoteToManager(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getMainRole().getName().equals(RoleEnum.MANAGER)) {
            throw new AppException("The user is already a manager", HttpStatus.CONFLICT);
        }
        if (user.getMainRole().getName().equals(RoleEnum.ADMIN)) {
            throw new AppException("The user is already an admin", HttpStatus.CONFLICT);
        }

        Role managerRole = roleRepository.findByName(RoleEnum.MANAGER)
                .orElseThrow(() -> new AppException("Manager role not found", HttpStatus.INTERNAL_SERVER_ERROR));

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
     * @throws RuntimeException if the user is not found, already a user, or the
     *                          user role is not found
     */
    public UserDto revokeManagerRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new AppException("The user is already a user", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new AppException("User role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        user.setMainRole(userRole);
        userRepository.save(user);

        return userMapper.toUserDto(user);
    }

    /**
     * Promotes a user to the admin role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a admin
     * - Removes existing roles and assigns the admin role
     *
     * @param userId The ID of the user to promote
     * @return UserDto containing the updated user's information
     * @throws RuntimeException if the user is not found, already a admin, or the
     *                          admin role is not found
     */
    public UserDto promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getMainRole().getName().equals(RoleEnum.ADMIN)) {
            throw new AppException("The user is already an admin", HttpStatus.CONFLICT);
        }

        Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
                .orElseThrow(() -> new AppException("Admin role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        user.setMainRole(adminRole);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Downgrades a admin to an manager role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already an manager or has lower rights
     * - Removes existing roles and assigns the manager role
     *
     * @param userId The ID of the user to downgrade
     * @throws RuntimeException if the user is not found, already an manager, or the
     *                          manager role is not found
     */
    public UserDto downgradeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new AppException("The user has lower rights than desired", HttpStatus.FORBIDDEN);
        }
        if (user.getMainRole().getName().equals(RoleEnum.MANAGER)) {
            throw new AppException("The user is already a manager", HttpStatus.CONFLICT);
        }

        Role managerRole = roleRepository.findByName(RoleEnum.MANAGER)
                .orElseThrow(() -> new AppException("Manager role not found", HttpStatus.INTERNAL_SERVER_ERROR));

        user.setMainRole(managerRole);
        userRepository.save(user);

        return userMapper.toUserDto(user);
    }

    /**
     * Revokes the admin role from a user.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a regular user
     * - Removes existing roles and assigns the user role
     *
     * @param userId The ID of the user to revoke the admin role from
     * @throws RuntimeException if the user is not found, already a user, or the
     *                          user role is not found
     */
    public UserDto revokeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (user.getMainRole().getName().equals(RoleEnum.USER)) {
            throw new AppException("The user is already a user", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
                .orElseThrow(() -> new AppException("User role not found", HttpStatus.INTERNAL_SERVER_ERROR));

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
     * Deletes a user from the system.
     * This operation:
     * - Verifies the user exists
     * - Checks if the authenticated user has sufficient permissions
     * - Deletes the user
     *
     * @param userId The ID of the user to delete
     * @return The deleted User entity, or null if deletion was successful
     * @throws RuntimeException if the user is not found or the authenticated user
     *                          lacks permissions
     */
    public UserDto deleteUser(Long userId) {
        // Get the user to delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Get the authenticated user (the actor)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto authenticatedUser = (UserDto) authentication.getPrincipal();

        // Get the full user entity for the authenticated user
        User authenticatedUserEntity = userRepository.findByLogin(authenticatedUser.getLogin())
                .orElseThrow(() -> new AppException("Authenticated user not found", HttpStatus.NOT_FOUND));

        // Check if the action is authorized
        if (!canPerformAction(authenticatedUserEntity.getMainRole().getName(), userToDelete.getMainRole().getName())) {
            throw new AppException("You don't have the necessary rights to perform this action", HttpStatus.UNAUTHORIZED);
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
     * @return The deleted User entity, or null if deletion was successful
     * @throws RuntimeException if the user is not found or the authenticated user
     *                          lacks permissions
     */
    public UserDto hardDeleteUser(Long userId) {
        // Get the user to delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        // Get the authenticated user (the actor)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto authenticatedUser = (UserDto) authentication.getPrincipal();

        // Get the full user entity for the authenticated user
        User authenticatedUserEntity = userRepository.findByLogin(authenticatedUser.getLogin())
                .orElseThrow(() -> new AppException("Authenticated user not found", HttpStatus.NOT_FOUND));

        // Check if the action is authorized
        if (!canPerformAction(authenticatedUserEntity.getMainRole().getName(), userToDelete.getMainRole().getName())) {
            throw new AppException("You don't have the necessary rights to perform this action", HttpStatus.FORBIDDEN);
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
     * @throws AppException if the default role is not found
     */
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
                .orElseThrow(() -> new AppException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));
        user.setMainRole(userRole);

        // Save the user
        User savedUser = userRepository.save(user);
        log.debug("Azure user created successfully: {}", savedUser.getLogin());

        return userMapper.toUserDto(savedUser);
    }
}
