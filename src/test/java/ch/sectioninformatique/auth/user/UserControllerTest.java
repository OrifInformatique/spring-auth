package ch.sectioninformatique.auth.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.sectioninformatique.auth.security.RoleRepository;

import org.springframework.http.HttpStatus;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void authenticatedUser_ReturnsCurrentUser() {
        // Arrange
        UserDto currentUser = new UserDto(1L, "John", "Doe", "john@test.com", null, "ROLE_USER", null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(currentUser);

        // Act
        ResponseEntity<UserDto> response = userController.authenticatedUser();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(currentUser, response.getBody());
    }

    @Test
    void allUsers_ReturnsListOfUsers() {
        // Arrange
        List<User> users = Arrays.asList(
            new User(1L, "John", "Doe", "john@test.com", "pass", null, null, null),
            new User(2L, "Jane", "Smith", "jane@test.com", "pass", null, null, null)
        );
        when(userService.allUsers()).thenReturn(users);

        // Act
        ResponseEntity<List<User>> response = userController.allUsers();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(users, response.getBody());
    }

    @Test
    void promoteToManager_Successful_ReturnsUserDto() {
        // Arrange
        Long userId = 1L;
        UserDto expectedDto = new UserDto(1L, "John", "Doe", "john@test.com", null, "ROLE_MANAGER", null);
        
        when(userService.promoteToManager(userId)).thenReturn(expectedDto);

        // Act
        ResponseEntity<?> response = userController.promoteToManager(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User promoted to manager successfully", response.getBody());
        verify(userService).promoteToManager(userId);
    }

    @Test
    void promoteToManager_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("User not found")).when(userService).promoteToManager(userId);

        // Act
        ResponseEntity<?> response = userController.promoteToManager(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).promoteToManager(userId);
    }

    @Test
    void revokeManagerRole_SuccessfulRevocation_ReturnsOkResponse() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).revokeManagerRole(userId);

        // Act
        ResponseEntity<?> response = userController.revokeManagerRole(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Manager role revoked successfully", response.getBody());
        verify(userService, times(1)).revokeManagerRole(userId);
    }

    @Test
    void revokeManagerRole_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("User not found")).when(userService).revokeManagerRole(userId);

        // Act
        ResponseEntity<?> response = userController.revokeManagerRole(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).revokeManagerRole(userId);
    }

    @Test
    void promoteToSuperManager_SuccessfulPromotion_ReturnsOkResponse() {
        // Arrange
        Long userId = 1L;
        UserDto expectedUser = new UserDto(userId, "John", "Doe", "john@test.com", null, "ROLE_SUPER_ADMIN", null);
        when(userService.promoteToSuperAdmin(userId)).thenReturn(expectedUser);

        // Act
        ResponseEntity<?> response = userController.promoteToSuperAdmin(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Super admin role assigned successfully", response.getBody());
        verify(userService, times(1)).promoteToSuperAdmin(userId);
    }

    @Test
    void promoteToSuperAdmin_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("User not found")).when(userService).promoteToSuperAdmin(userId);

        // Act
        ResponseEntity<?> response = userController.promoteToSuperAdmin(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).promoteToSuperAdmin(userId);
    }

    @Test
    void revokeSuperAdminRole_SuccessfulRevocation_ReturnsOkResponse() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).revokeSuperAdminRole(userId);

        // Act
        ResponseEntity<?> response = userController.revokeSuperAdminRole(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Super admin role revoked successfully", response.getBody());
        verify(userService, times(1)).revokeSuperAdminRole(userId);
    }

    @Test
    void revokeSuperAdminRole_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("User not found")).when(userService).revokeSuperAdminRole(userId);

        // Act
        ResponseEntity<?> response = userController.revokeSuperAdminRole(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).revokeSuperAdminRole(userId);
    }

    @Test
    void downgradeSuperAdminRole_SuccessfulDowngrade_ReturnsOkResponse() {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).downgradeSuperAdminRole(userId);

        // Act
        ResponseEntity<?> response = userController.downgradeSuperAdminRole(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Super admin role downgraded successfully", response.getBody());
        verify(userService, times(1)).downgradeSuperAdminRole(userId);
    }

    @Test
    void downgradeSuperAdminRole_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        doThrow(new RuntimeException("User not found")).when(userService).downgradeSuperAdminRole(userId);

        // Act
        ResponseEntity<?> response = userController.downgradeSuperAdminRole(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).downgradeSuperAdminRole(userId);
    }

    @Test
    void deleteUser_SuccessfulDeletion_ReturnsOkResponse() {
        // Arrange
        Long userId = 1L;
        UserDto authenticatedUser = new UserDto(userId, null, null, null, null, null, null);
        authenticatedUser.setLogin("manager@test.com");
        authenticatedUser.setRole("ROLE_MANAGER");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        doNothing().when(userService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = userController.deleteUser(userId);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("User deleted successfully", response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    void deleteUser_UserNotFound_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        UserDto authenticatedUser = new UserDto(userId, null, null, null, null, null, null);
        authenticatedUser.setLogin("manager@test.com");
        authenticatedUser.setRole("ROLE_MANAGER");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = userController.deleteUser(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("User not found", response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    void deleteUser_UnauthorizedAccess_ReturnsBadRequest() {
        // Arrange
        Long userId = 1L;
        UserDto authenticatedUser = new UserDto(userId, null, null, null, null, null, null);
        authenticatedUser.setLogin("user@test.com");
        authenticatedUser.setRole("ROLE_USER");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(authenticatedUser);
        doThrow(new RuntimeException("You don't have the necessary rights to perform this action"))
            .when(userService).deleteUser(userId);

        // Act
        ResponseEntity<?> response = userController.deleteUser(userId);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals("You don't have the necessary rights to perform this action", response.getBody());
        verify(userService, times(1)).deleteUser(userId);
    }
} 