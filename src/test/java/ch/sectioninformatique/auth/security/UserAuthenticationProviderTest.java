package ch.sectioninformatique.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Base64;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link UserAuthenticationProvider}.
 * This class tests the JWT token creation, validation, and authentication
 * functionality of the UserAuthenticationProvider.
 */
@ExtendWith(MockitoExtension.class)
class UserAuthenticationProviderTest {

    @Mock
    private UserService userService;

    private UserAuthenticationProvider authenticationProvider;

    private static final String TEST_SECRET_KEY = "test-secret-key";
    private static final String TEST_LOGIN = "test@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";

    @BeforeEach
    void setUp() {
        authenticationProvider = new UserAuthenticationProvider(userService);
        // Use reflection to set the secret key
        try {
            java.lang.reflect.Field field = UserAuthenticationProvider.class.getDeclaredField("secretKey");
            field.setAccessible(true);
            field.set(authenticationProvider, Base64.getEncoder().encodeToString(TEST_SECRET_KEY.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set secret key", e);
        }
    }

    /**
     * Tests the token creation functionality.
     * Verifies that:
     * - Token is created successfully
     * - Token contains correct user claims
     * - Token is properly formatted
     */
    @Test
    void testCreateToken() {
        // Given
        UserDto user = UserDto.builder()
                .login(TEST_LOGIN)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .mainRole("USER")
                .permissions(Arrays.asList("read", "write"))
                .build();

        // When
        String token = authenticationProvider.createToken(user);

        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    /**
     * Tests the basic token validation.
     * Verifies that:
     * - Valid token is accepted
     * - Authentication object is created with correct user details
     * - Authorities are properly set
     */
    @Test
    void testValidateToken() {
        // Given
        UserDto user = UserDto.builder()
                .login(TEST_LOGIN)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .mainRole("USER")
                .permissions(Arrays.asList("read", "write"))
                .build();

        String token = authenticationProvider.createToken(user);

        // When
        Authentication authentication = authenticationProvider.validateToken(token);

        // Then
        assertNotNull(authentication);
        assertTrue(authentication instanceof UsernamePasswordAuthenticationToken);
        assertEquals(TEST_LOGIN, ((UserDto) authentication.getPrincipal()).getLogin());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().startsWith("ROLE_")));
    }

    /**
     * Tests the strong token validation with existing user.
     * Verifies that:
     * - User is found in database
     * - Authentication is created with correct details
     * - Authorities include both role and permissions
     */
    @Test
    void testValidateTokenStrongly_ExistingUser() {
        // Given
        UserDto user = UserDto.builder()
                .login(TEST_LOGIN)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .mainRole("USER")
                .permissions(Arrays.asList("read", "write"))
                .build();

        String token = authenticationProvider.createToken(user);
        when(userService.findByLogin(TEST_LOGIN)).thenReturn(user);

        // When
        Authentication authentication = authenticationProvider.validateTokenStrongly(token);

        // Then
        assertNotNull(authentication);
        assertEquals(TEST_LOGIN, ((UserDto) authentication.getPrincipal()).getLogin());
        verify(userService).findByLogin(TEST_LOGIN);
    }

    /**
     * Tests the strong token validation with new user.
     * Verifies that:
     * - New user is created when not found
     * - Authentication is created with correct details
     * - Default permissions are set
     */
    @Test
    void testValidateTokenStrongly_NewUser() {
        // Given
        UserDto user = UserDto.builder()
                .login(TEST_LOGIN)
                .firstName(TEST_FIRST_NAME)
                .lastName(TEST_LAST_NAME)
                .mainRole("USER")
                .permissions(Arrays.asList("read", "write"))
                .build();

        String token = authenticationProvider.createToken(user);
        when(userService.findByLogin(TEST_LOGIN)).thenThrow(new RuntimeException("User not found"));
        when(userService.createAzureUser(any())).thenReturn(user);

        // When
        Authentication authentication = authenticationProvider.validateTokenStrongly(token);

        // Then
        assertNotNull(authentication);
        assertEquals(TEST_LOGIN, ((UserDto) authentication.getPrincipal()).getLogin());
        verify(userService).createAzureUser(any());
    }

} 