package ch.sectioninformatique.template.user;

import ch.sectioninformatique.template.auth.CredentialsDto;
import ch.sectioninformatique.template.auth.SignUpDto;
import ch.sectioninformatique.template.app.exceptions.AppException;
import ch.sectioninformatique.template.security.Role;
import ch.sectioninformatique.template.security.RoleEnum;
import ch.sectioninformatique.template.security.RoleRepository;
import ch.sectioninformatique.template.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.http.HttpStatus;
import ch.sectioninformatique.template.item.Item;

import java.nio.CharBuffer;
import java.util.Optional;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

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
    private ItemRepository itemRepository;

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
        UserDto expectedDto = new UserDto(1L, "John", "Doe", login, null, "ROLE_USER", null);
        
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
        User user = new User(1L, "John", "Doe", login, "hashedPassword", null, null, null);
        
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
        user.setRoles(new HashSet<>());
        
        UserDto expectedDto = new UserDto(1L, "New", "User", login, null, "ROLE_USER", null);
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
    void promoteToAdmin_Successful_ReturnsUserDto() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john@test.com");
        user.setPassword("pass");
        user.setRoles(new HashSet<>());
        
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleEnum.ADMIN);
        user.getRoles().add(userRole);
        
        UserDto expectedDto = new UserDto(userId, "John", "Doe", "john@test.com", null, "ROLE_ADMIN", null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleEnum.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(expectedDto);

        // Act
        UserDto result = userService.promoteToAdmin(userId);

        // Assert
        assertEquals(expectedDto, result);
        verify(userRepository).findById(userId);
        verify(roleRepository).findByName(RoleEnum.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void promoteToAdmin_UserNotFound_ThrowsRuntimeException() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.promoteToAdmin(userId));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void promoteToAdmin_AlreadyAdmin_ThrowsRuntimeException() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setLogin("john@test.com");
        user.setPassword("pass");
        user.setRoles(new HashSet<>());
        
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleEnum.ADMIN);
        user.getRoles().add(adminRole);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userService.promoteToAdmin(userId));
        assertEquals("The user is already an admin", exception.getMessage());
    }

    @Test
    void deleteUser_Successful_DeletesUserAndTransfersItems() {
        // Arrange
        Long userId = 2L;
        User userToDelete = new User();
        userToDelete.setId(userId);
        userToDelete.setFirstName("John");
        userToDelete.setLastName("Doe");
        userToDelete.setLogin("john@test.com");
        userToDelete.setPassword("pass");
        userToDelete.setRoles(new HashSet<>());
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        userToDelete.getRoles().add(userRole);
        
        User authenticatedUser = new User();
        authenticatedUser.setId(3L);
        authenticatedUser.setFirstName("Admin");
        authenticatedUser.setLastName("User");
        authenticatedUser.setLogin("admin@test.com");
        authenticatedUser.setPassword("pass");
        authenticatedUser.setRoles(new HashSet<>());
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleEnum.ADMIN);
        authenticatedUser.getRoles().add(adminRole);
        
        User deletedUser = new User();
        deletedUser.setId(1L);
        deletedUser.setFirstName("Deleted");
        deletedUser.setLastName("User");
        deletedUser.setLogin("deleted@test.com");
        deletedUser.setPassword("pass");
        deletedUser.setRoles(new HashSet<>());
        
        UserDto authenticatedUserDto = new UserDto(3L, "Admin", "User", "admin@test.com", null, "ROLE_ADMIN", null);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(userToDelete));
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUserDto);
        when(userRepository.findByLogin("admin@test.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(deletedUser));
        List<Item> userItems = new ArrayList<>();
        when(itemRepository.findAll()).thenReturn(userItems);

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).findByLogin("admin@test.com");
        verify(userRepository).findById(1L);
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
        userToDelete.setRoles(new HashSet<>());
        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleEnum.ADMIN);
        userToDelete.getRoles().add(adminRole);
        
        User authenticatedUser = new User();
        authenticatedUser.setId(3L);
        authenticatedUser.setFirstName("Regular");
        authenticatedUser.setLastName("User");
        authenticatedUser.setLogin("user@test.com");
        authenticatedUser.setPassword("pass");
        authenticatedUser.setRoles(new HashSet<>());
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.USER);
        authenticatedUser.getRoles().add(userRole);
        
        UserDto authenticatedUserDto = new UserDto(3L, "Regular", "User", "user@test.com", null, "ROLE_USER", null);
        
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