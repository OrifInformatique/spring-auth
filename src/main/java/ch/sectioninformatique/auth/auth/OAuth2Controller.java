package ch.sectioninformatique.auth.auth;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller handling OAuth2 authentication flows.
 * This controller manages the OAuth2 authentication process, specifically handling
 * the success callback from OAuth2 providers and generating JWT tokens for authenticated users.
 */
@RequestMapping("/oauth2")
@RestController
public class OAuth2Controller {

    private final UserAuthenticationProvider userAuthenticationProvider;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    /**
     * Constructs a new Oauth2Controller with the required dependencies.
     *
     * @param userAuthenticationProvider Provider for user authentication and token generation
     * @param userService Service for user management
     */
    public OAuth2Controller(UserAuthenticationProvider userAuthenticationProvider,
                            UserService userService) {
        this.userAuthenticationProvider = userAuthenticationProvider;
        this.userService = userService;
    }

    /**
     * Handles the OAuth2 authentication success callback.
     * This endpoint processes the OAuth2 authentication token, extracts user information,
     * and generates a JWT token for the authenticated user. It then redirects to the frontend
     * with the generated token.
     *
     * @param authentication The OAuth2 authentication token containing user information
     * @param response The HTTP response object used for redirection
     * @throws IOException If an I/O error occurs during the response handling
     */
    @GetMapping("/success")
    public void oauth2Success(OAuth2AuthenticationToken authentication, HttpServletResponse response) throws IOException {
        if (authentication == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication token is missing.");
            return;
        }

        // Retrieve OAuth2User principal from the authentication token.
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        if (principal == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "OAuth2 user details not found.");
            return;
        }

        // Map OAuth2User attributes to your UserDto.
        // Adjust the attribute keys as needed based on your Azure configuration.
        String email = principal.getAttribute("email");
        String givenName = principal.getAttribute("given_name");
        String familyName = principal.getAttribute("family_name");

        if (Objects.isNull(email)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Required user attribute not found.");
            return;
        }

        UserDto user = UserDto.builder()
                .login(email)
                .firstName(givenName)
                .lastName(familyName)
                .build();

        // Create or get Azure user in local database
        user = userService.createAzureUser(user);

        // Generate a JWT using your custom UserAuthenticationProvider.
        String jwt = userAuthenticationProvider.createToken(user);

        // Construct a redirect URL for your frontend with the token
        String redirectUrl = String.format("http://localhost:4000/oauth2/success?token=%s&loginType=azure",
                URLEncoder.encode(jwt, StandardCharsets.UTF_8));

        log.debug("Redirecting to frontend with JWT token");
        response.sendRedirect(redirectUrl);
    }
}
