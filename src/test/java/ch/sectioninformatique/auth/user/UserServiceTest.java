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
import org.springframework.http.HttpStatus;

import java.nio.CharBuffer;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void login_Successful_ReturnsUserDto() {
        // Arrange
        String login = "john@test.com";
        String password = "password123";
        User user = new User(1L, "John", "Doe", login, "hashedPassword", null, null, null);
        UserDto expectedDto = new UserDto(1L, "John", "Doe", login, null, null, "USER", null);
        
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

    @Test
    void login_UserNotFound_ThrowsAppException() {
        // Arrange
        String login = "nonexistent@test.com";
        String password = "password123";
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(new CredentialsDto(login, password.toCharArray())));
        assertEquals("Unknown user", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void login_InvalidPassword_ThrowsAppException() {
        // Arrange
        String login = "john@test.com";
        String password = "wrongpassword";
        User user = new User(1L, "John", "Doe", login, "hashedPassword", null, null,null);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CharBuffer.wrap(password), user.getPassword())).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.login(new CredentialsDto(login, password.toCharArray())));
        assertEquals("Invalid password", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

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
        
        UserDto expectedDto = new UserDto(1L, "New", "User", login, null, null, "USER", null);
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

    @Test
    void register_LoginExists_ThrowsAppException() {
        // Arrange
        String login = "existing@test.com";
        String password = "password123";
        SignUpDto signUpDto = new SignUpDto("Existing", "User", login, password.toCharArray());
        
        User existingUser = new User(1L, "Existing", "User", login, "hashedPassword", null, null, null);
        
        when(userRepository.findByLogin(login)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, 
            () -> userService.register(signUpDto));
        assertEquals("Login already exists", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

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
        
        UserDto expectedDto = new UserDto(userId, "John", "Doe", "john@test.com", null, null, "ROLE_MANAGER", null);
        
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

    @Test
    void promoteToManager_UserNotFound_ThrowsRuntimeException() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.promoteToManager(userId));
        assertEquals("User not found", exception.getMessage());
    }

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
        assertEquals("The user is already a manager", exception.getMessage());
    }

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
        
        UserDto authenticatedUserDto = new UserDto(3L, "Manager", "User", "manager@test.com", null, null, "ROLE_MANAGER", null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDto);
        when(userRepository.findByLogin("manager@test.com")).thenReturn(Optional.of(authenticatedUser));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).findByLogin("manager@test.com");
        verify(userRepository).deleteById(userId);
    }

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
        
        UserDto authenticatedUserDto = new UserDto(3L, "Regular", "User", "user@test.com", null, null, "USER", null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDto);
        when(userRepository.findByLogin("user@test.com")).thenReturn(Optional.of(authenticatedUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.deleteUser(userId));
        assertEquals("You don't have the necessary rights to perform this action", exception.getMessage());
    }
}