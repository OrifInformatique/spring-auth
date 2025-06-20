package ch.sectioninformatique.auth.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.auth.CredentialsDto;
import ch.sectioninformatique.auth.auth.SignUpDto;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.security.RoleRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

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

    /** Repository for user data access */
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
                .orElseThrow(() -> new AppException("Unknown user", HttpStatus.NOT_FOUND));

        if (passwordEncoder.matches(CharBuffer.wrap(credentialsDto.password()), user.getPassword())) {
            return userMapper.toUserDto(user);
        }
        throw new AppException("Invalid password", HttpStatus.BAD_REQUEST);
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
     * @throws AppException if the login already exists or the default role is not found
     */
    public UserDto register(SignUpDto userDto) {
        Optional<User> optionalUser = userRepository.findByLogin(userDto.login());

        if (optionalUser.isPresent()) {
            throw new AppException("Login already exists", HttpStatus.BAD_REQUEST);
        }

        User user = userMapper.signUpToUser(userDto);
        user.setPassword(passwordEncoder.encode(CharBuffer.wrap(userDto.password())));

        // Add default USER role
        Role userRole = roleRepository.findByName(RoleEnum.USER)
            .orElseThrow(() -> new AppException("Default role not found", HttpStatus.INTERNAL_SERVER_ERROR));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);
        return userMapper.toUserDto(savedUser);
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
            user.getRoles().stream().map(role -> role.getName().toString()).toList());
            
        UserDto userDto = userMapper.toUserDto(user);
        log.debug("Mapped to UserDto - ID: {}, FirstName: {}, LastName: {}, Role: {}", 
            userDto.getId(), userDto.getFirstName(), userDto.getLastName(), userDto.getRole());
            
        return userDto;
    }

    /**
     * Retrieves all users in the system.
     *
     * @return List of all User entities
     */
    public List<User> allUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    /**
     * Promotes a user to the admin role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already an admin or super admin
     * - Removes existing roles and assigns the admin role
     *
     * @param userId The ID of the user to promote
     * @return UserDto containing the updated user's information
     * @throws RuntimeException if the user is not found, already an admin, or the admin role is not found
     */
    public UserDto promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        if (user.getRole().getName().equals(RoleEnum.ADMIN)) {
            throw new RuntimeException("The user is already an admin");
        }
        if (user.getRole().getName().equals(RoleEnum.SUPER_ADMIN)) {
            throw new RuntimeException("The user is already a super admin");
        }
        
        Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
            .orElseThrow(() -> new RuntimeException("Admin role not found"));
            
        user.getRoles().clear();
        user.getRoles().add(adminRole);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Revokes the admin role from a user.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a regular user or super admin
     * - Removes existing roles and assigns the user role
     *
     * @param userId The ID of the user to revoke the admin role from
     * @throws RuntimeException if the user is not found, already a user, or the user role is not found
     */
    public void revokeAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole().getName().equals(RoleEnum.USER)) {
            throw new RuntimeException("The user is already a user");
        }
        if (user.getRole().getName().equals(RoleEnum.SUPER_ADMIN)) {
            throw new RuntimeException("You don't have the necessary rights to delete a super admin");
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
            .orElseThrow(() -> new RuntimeException("User role not found"));

        user.getRoles().clear();
        user.getRoles().add(userRole);
        userRepository.save(user);
    }

    /**
     * Promotes a user to the super admin role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a super admin
     * - Removes existing roles and assigns the super admin role
     *
     * @param userId The ID of the user to promote
     * @return UserDto containing the updated user's information
     * @throws RuntimeException if the user is not found, already a super admin, or the super admin role is not found
     */
    public UserDto promoteToSuperAdmin(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole().getName().equals(RoleEnum.SUPER_ADMIN)) {
            throw new RuntimeException("The user is already a super admin");
        }

        Role superAdminRole = roleRepository.findByName(RoleEnum.SUPER_ADMIN)
            .orElseThrow(() -> new RuntimeException("Super admin role not found"));

        user.getRoles().clear();
        user.getRoles().add(superAdminRole);
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Downgrades a super admin to an admin role.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already an admin or has lower rights
     * - Removes existing roles and assigns the admin role
     *
     * @param userId The ID of the user to downgrade
     * @throws RuntimeException if the user is not found, already an admin, or the admin role is not found
     */
    public void downgradeSuperAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole().getName().equals(RoleEnum.USER)) {
            throw new RuntimeException("The user has lower rights than desired");
        }
        if (user.getRole().getName().equals(RoleEnum.ADMIN)) {
            throw new RuntimeException("The user is already an admin");
        }

        Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
            .orElseThrow(() -> new RuntimeException("Admin role not found"));

        user.getRoles().clear();
        user.getRoles().add(adminRole);
        userRepository.save(user);
    }

    /**
     * Revokes the super admin role from a user.
     * This operation:
     * - Verifies the user exists
     * - Checks if the user is already a regular user
     * - Removes existing roles and assigns the user role
     *
     * @param userId The ID of the user to revoke the super admin role from
     * @throws RuntimeException if the user is not found, already a user, or the user role is not found
     */
    public void revokeSuperAdminRole(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole().getName().equals(RoleEnum.USER)) {
            throw new RuntimeException("The user is already a user");
        }

        Role userRole = roleRepository.findByName(RoleEnum.USER)
            .orElseThrow(() -> new RuntimeException("User role not found"));

        user.getRoles().clear();
        user.getRoles().add(userRole);
        userRepository.save(user);
    }

    /**
     * Checks if an actor can perform an action on a target based on their roles.
     * The hierarchy is:
     * - SUPER_ADMIN can perform actions on all roles
     * - ADMIN can perform actions on USER and ADMIN roles
     * - USER cannot perform actions on any role
     *
     * @param actorRole The role of the actor performing the action
     * @param targetRole The role of the target of the action
     * @return true if the actor can perform the action, false otherwise
     */
    private boolean canPerformAction(RoleEnum actorRole, RoleEnum targetRole) {
        switch (actorRole) {
            case SUPER_ADMIN:
                return true;
            case ADMIN:
                if (targetRole == RoleEnum.SUPER_ADMIN) {
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
     * @throws RuntimeException if the user is not found or the authenticated user lacks permissions
     */
    public void deleteUser(Long userId) {
        // Get the user to delete
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get the authenticated user (the actor)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDto authenticatedUser = (UserDto) authentication.getPrincipal();

        // Get the full user entity for the authenticated user
        User authenticatedUserEntity = userRepository.findByLogin(authenticatedUser.getLogin())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // Check if the action is authorized
        if (!canPerformAction(authenticatedUserEntity.getRole().getName(), userToDelete.getRole().getName())) {
            throw new RuntimeException("You don't have the necessary rights to perform this action");
        }

        // Delete the user
        userRepository.deleteById(userId);
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
        user.addRole(userRole);

        // Save the user
        User savedUser = userRepository.save(user);
        log.debug("Azure user created successfully: {}", savedUser.getLogin());
        
        return userMapper.toUserDto(savedUser);
    }
}

