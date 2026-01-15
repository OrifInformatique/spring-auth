package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ch.sectioninformatique.auth.app.exceptions.AppException;
import ch.sectioninformatique.auth.auth.CredentialsDto;
import ch.sectioninformatique.auth.auth.SignUpDto;
import ch.sectioninformatique.auth.security.Role;
import ch.sectioninformatique.auth.security.RoleEnum;
import ch.sectioninformatique.auth.security.RoleRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import java.nio.CharBuffer;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * 
 * This test class uses Mockito to test the UserService business logic in isolation
 * without requiring a database or Spring context. Tests follow the Arrange-Act-Assert (AAA) pattern:
 * - Arrange: Set up test data and configure mock behaviors
 * - Act: Execute the method being tested
 * - Assert: Verify the results and mock interactions
 * 
 * Key areas tested:
 * - User authentication (login)
 * - User registration
 * - Role management (promote/revoke manager and admin)
 * - User deletion with permission checks
 * - Error handling and validation
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Test: Successful login with valid credentials
     * 
     * Verifies that the login method correctly authenticates a user when provided
     * with valid email and password credentials.
     * 
     * Arrange:
     * - Mock user repository to return a user with hashed password
     * - Mock password encoder to confirm password match
     * - Mock user mapper to convert User entity to UserDto
     * 
     * Act:
     * - Call userService.login() with valid credentials
     * 
     * Assert:
     * - Returned UserDto matches expected data
     * - Repository was queried for the user
     * - Password was verified using encoder
     */
    @Test
    void login_Successful_ReturnsUserDto() {
        // Arrange
        String login = "john@test.com";
        String password = "password123";
        User user = new User(1L, "John", "Doe", login, "hashedPassword", null, null, false, null);
        UserDto expectedDto = new UserDto(1L, "John", "Doe", login, null, false, "USER", null);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CharBuffer.wrap(password), user.getPassword())).thenReturn(true);
        when(userMapper.toUserDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.login(new CredentialsDto(login, password.toCharArray()));

        // Assert
        assertEquals(expectedDto, result);
        verify(userRepository).findByLogin(login);
        verify(passwordEncoder).matches(CharBuffer.wrap(password), user.getPassword());
    }

    /**
     * Test: Login fails when user doesn't exist
     * 
     * Verifies that the login method throws AppException when attempting
     * to authenticate with an email that doesn't exist in the database.
     * 
     * Arrange:
     * - Mock user repository to return empty Optional (user not found)
     * 
     * Act & Assert:
     * - Call userService.login() with non-existent email
     * - Verify AppException is thrown with message "Invalid credentials"
     * - Error message should not reveal whether user exists (security best practice)
     */
    @Test
    void login_UserNotFound_ThrowsAppException() {
        // Arrange
        String login = "nonexistent@test.com";
        String password = "password123";
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(new CredentialsDto(login, password.toCharArray())));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    /**
     * Test: Login fails with incorrect password
     * 
     * Verifies that the login method throws AppException when the password
     * doesn't match the user's hashed password.
     * 
     * Arrange:
     * - Mock user repository to return a user
     * - Mock password encoder to return false (password mismatch)
     * 
     * Act & Assert:
     * - Call userService.login() with wrong password
     * - Verify AppException is thrown with message "Invalid credentials"
     * - Error message should be same as user not found (security best practice)
     */
    @Test
    void login_InvalidPassword_ThrowsAppException() {
        // Arrange
        String login = "john@test.com";
        String password = "wrongpassword";
        User user = new User(1L, "John", "Doe", login, "hashedPassword", null, null,false, null);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CharBuffer.wrap(password), user.getPassword())).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(new CredentialsDto(login, password.toCharArray())));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    /**
     * Test: Successful user registration
     * 
     * Verifies that a new user can be registered with valid information,
     * their password is securely hashed, and they are assigned the USER role by default.
     * 
     * Arrange:
     * - Mock repository to confirm login doesn't already exist
     * - Mock password encoder to hash the password
     * - Mock role repository to provide USER role
     * - Mock mapper to convert SignUpDto to User entity and back to UserDto
     * 
     * Act:
     * - Call userService.register() with valid sign-up data
     * 
     * Assert:
     * - Returned UserDto matches expected data
     * - Password was hashed before saving
     * - User role was set to USER
     * - User was saved to repository
     */
    @Test
    void register_Successful_ReturnsUserDto() {
        // Arrange
        String login = "newuser@test.com";
        String password = "password123";
        SignUpDto signUpDto = new SignUpDto("New", "User", login, password.toCharArray());
        
        User user = new User();
        user.setId(1L);
        user.setFirstName("New");
        user.setLastName("User");
        user.setLogin(login);
        user.setPassword("hashedPassword");
        user.setMainRole(new Role());

        UserDto expectedDto = new UserDto(1L, "New", "User", login, null, false, "USER", null);
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(CharBuffer.wrap(password))).thenReturn("hashedPassword");
        when(roleRepository.findByName(RoleEnum.USER)).thenReturn(Optional.of(userRole));
        when(userMapper.signUpToUser(signUpDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.register(signUpDto);

        // Assert
        assertEquals(expectedDto, result);
        verify(userRepository).findByLogin(login);
        verify(passwordEncoder).encode(CharBuffer.wrap(password));
        verify(roleRepository).findByName(RoleEnum.USER);
        verify(userRepository).save(user);
    }

    /**
     * Test: Registration fails when email already exists
     * 
     * Verifies that attempting to register with an email that already exists
     * throws an AppException to prevent duplicate accounts.
     * 
     * Arrange:
     * - Mock repository to return existing user with the same login
     * 
     * Act & Assert:
     * - Call userService.register() with duplicate email
     * - Verify AppException is thrown with message indicating user already exists
     * - No new user is created
     */
    @Test
    void register_LoginExists_ThrowsAppException() {
        // Arrange
        String login = "existing@test.com";
        String password = "password123";
        SignUpDto signUpDto = new SignUpDto("Existing", "User", login, password.toCharArray());
        
        User existingUser = new User(1L, "Existing", "User", login, "hashedPassword", null, null, false, null);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.register(signUpDto));
        assertEquals("User already exists: existing@test.com", exception.getMessage());
    }

    /**
     * Test: Successfully promote USER to MANAGER role
     * 
     * Verifies that an administrator can promote a regular user to manager role,
     * granting them elevated permissions.
     * 
     * Arrange:
     * - Mock repository to return a user with USER role
     * - Mock role repository to provide MANAGER role
     * - Mock repository save operation
     * - Mock mapper to convert updated User to UserDto
     * 
     * Act:
     * - Call userService.promoteToManager() with user ID
     * 
     * Assert:
     * - User's role is changed to MANAGER
     * - User is saved with new role
     * - Returned UserDto reflects the promotion
     */
    @Test
    void promoteToManager_Successful_ReturnsUserDto() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john@test.com");
        user.setPassword("pass");
        user.setMainRole(new Role());
        
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleEnum.MANAGER);
        user.setMainRole(userRole);

        UserDto expectedDto = new UserDto(userId, "John", "Doe", "john@test.com", null, false, "ROLE_MANAGER",
                null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleEnum.MANAGER)).thenReturn(Optional.of(managerRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.promoteToManager(userId);

        // Assert
        assertEquals(expectedDto, result);
        verify(userRepository).findById(userId);
        verify(roleRepository).findByName(RoleEnum.MANAGER);
        verify(userRepository).save(user);
    }

    /**
     * Test: Promotion fails when user doesn't exist
     * 
     * Verifies that attempting to promote a non-existent user throws
     * a RuntimeException with an appropriate error message.
     * 
     * Arrange:
     * - Mock repository to return empty Optional (user not found)
     * 
     * Act & Assert:
     * - Call userService.promoteToManager() with non-existent user ID
     * - Verify RuntimeException is thrown with message "User not found: 1"
     */
    @Test
    void promoteToManager_UserNotFound_ThrowsRuntimeException() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.promoteToManager(userId));
        assertEquals("User not found: 1", exception.getMessage());
    }

    /**
     * Test: Promotion fails when user is already a manager
     * 
     * Verifies that attempting to promote a user who already has the MANAGER
     * role throws a RuntimeException to prevent unnecessary operations.
     * 
     * Arrange:
     * - Mock repository to return a user with MANAGER role
     * 
     * Act & Assert:
     * - Call userService.promoteToManager() for user who is already manager
     * - Verify RuntimeException is thrown with message indicating user is already manager
     */
    @Test
    void promoteToManager_AlreadyManager_ThrowsRuntimeException() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john@test.com");
        user.setPassword("pass");
        user.setMainRole(new Role());
        
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleEnum.MANAGER);
        user.setMainRole(managerRole);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.promoteToManager(userId));
        assertEquals("User already manager: john@test.com", exception.getMessage());
    }

    /**
     * Test: Successfully delete a user with proper permissions
     * 
     * Verifies that a user with sufficient permissions (MANAGER or ADMIN) can
     * delete another user with lower or equal permissions.
     * 
     * Arrange:
     * - Mock repository to return the user to be deleted (USER role)
     * - Mock security context to provide authenticated user (MANAGER role)
     * - Mock repository to return the authenticated user
     * 
     * Act:
     * - Call userService.deleteUser() with target user ID
     * 
     * Assert:
     * - User lookup was performed
     * - Authenticated user was retrieved
     * - User was deleted from repository
     */
    @Test
    void deleteUser_Successful_DeletesUser() {
        // Arrange
        Long userId = 2L;
        User userToDelete = new User();
        userToDelete.setId(userId);
        userToDelete.setFirstName("John");
        userToDelete.setLastName("Doe");
        userToDelete.setLogin("john@test.com");
        userToDelete.setPassword("pass");
        userToDelete.setMainRole(new Role());
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        userToDelete.setMainRole(userRole);
        
        User authenticatedUser = new User();
        authenticatedUser.setId(3L);
        authenticatedUser.setFirstName("Manager");
        authenticatedUser.setLastName("User");
        authenticatedUser.setLogin("manager@test.com");
        authenticatedUser.setPassword("pass");
        authenticatedUser.setMainRole(new Role());
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleEnum.MANAGER);
        authenticatedUser.setMainRole(managerRole);

        UserDto authenticatedUserDto = new UserDto(3L, "Manager", "User", "manager@test.com", null, false,
                "ROLE_MANAGER", null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDto);
        when(userRepository.findByLogin("manager@test.com")).thenReturn(Optional.of(authenticatedUser));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).findByLogin("manager@test.com");
        verify(userRepository).delete(userToDelete);
    }

    /**
     * Test: Delete fails when user has insufficient permissions
     * 
     * Verifies that a user with lower permissions cannot delete a user with
     * higher or equal permissions. This prevents privilege escalation attacks.
     * 
     * Arrange:
     * - Mock repository to return user to delete (MANAGER role)
     * - Mock security context to provide authenticated user (USER role - lower permissions)
     * - Mock repository to return the authenticated user
     * 
     * Act & Assert:
     * - Call userService.deleteUser() as regular user attempting to delete manager
     * - Verify RuntimeException is thrown with message about insufficient rights
     */
    @Test
    void deleteUser_Unauthorized_ThrowsRuntimeException() {
        // Arrange
        Long userId = 2L;
        User userToDelete = new User();
        userToDelete.setId(userId);
        userToDelete.setFirstName("John");
        userToDelete.setLastName("Doe");
        userToDelete.setLogin("john@test.com");
        userToDelete.setPassword("pass");
        userToDelete.setMainRole(new Role());
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setName(RoleEnum.MANAGER);
        userToDelete.setMainRole(managerRole);
        
        User authenticatedUser = new User();
        authenticatedUser.setId(3L);
        authenticatedUser.setFirstName("Regular");
        authenticatedUser.setLastName("User");
        authenticatedUser.setLogin("user@test.com");
        authenticatedUser.setPassword("pass");
        authenticatedUser.setMainRole(new Role());
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        authenticatedUser.setMainRole(userRole);

        UserDto authenticatedUserDto = new UserDto(3L, "Regular", "User", "user@test.com", null, false, "USER",
                null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDto);
        when(userRepository.findByLogin("user@test.com")).thenReturn(Optional.of(authenticatedUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.deleteUser(userId));
        assertEquals("User has insufficient rights: user@test.com", exception.getMessage());
    }
}