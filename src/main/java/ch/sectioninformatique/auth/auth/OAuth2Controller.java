package ch.sectioninformatique.auth.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller handling OAuth2 authentication flows.
 * This controller manages the OAuth2 authentication process for Azure OAuth2 authentication.
 * 
 * Provides two endpoints:
 * - /oauth2/login/azure: Initiates the OAuth2 flow by storing redirect URL and redirecting to Azure
 * - /oauth2/success: Handles the callback from Azure after successful authentication
 * 
 * The controller generates JWT tokens for authenticated users and stores them in secure cookies.
 */
@RequestMapping("/oauth2")
@RestController
public class OAuth2Controller {

    private static final String REDIRECT_URL_SESSION_KEY = "oauth2_redirect_url";
    private static final String DEFAULT_REDIRECT_URL = "http://localhost:4000/oauth2/success?loginType=azure";

    private final UserAuthenticationProvider userAuthenticationProvider;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    /**
     * Constructs a new Oauth2Controller with the required dependencies.
     *
     * @param userAuthenticationProvider Provider for user authentication and token
     *                                   generation
     * @param userService                Service for user management
     */
    public OAuth2Controller(UserAuthenticationProvider userAuthenticationProvider,
            UserService userService) {
        this.userAuthenticationProvider = userAuthenticationProvider;
        this.userService = userService;
    }

    /**
     * Initiates OAuth2 authentication flow with Azure.
     * This endpoint is called by the Spring Client App to start the OAuth2 flow.
     * It stores the calling URL (from Referer header or redirectUrl parameter) in the session,
     * then redirects to Spring Security's OAuth2 authorization endpoint.
     *
     * Flow step: App → spring-auth (step 2)
     *
     * @param redirectUrl The URL to redirect to after successful authentication (optional).
     *                    If not provided, uses the Referer header. If neither is available,
     *                    uses DEFAULT_REDIRECT_URL.
     * @param request     The HTTP request object containing the session
     * @param response    The HTTP response object used for redirection
     * @throws IOException If an I/O error occurs during the response handling
     */
    @GetMapping("/login/azure")
    public void initiateAzureLogin(
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(true);

        // Store redirect URL in session if provided, otherwise store the referer header
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            session.setAttribute(REDIRECT_URL_SESSION_KEY, redirectUrl);
            log.debug("Stored redirect URL from parameter: {}", redirectUrl);
        } else {
            String referer = request.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                session.setAttribute(REDIRECT_URL_SESSION_KEY, referer);
                log.debug("Stored redirect URL from Referer header: {}", referer);
            } else {
                session.setAttribute(REDIRECT_URL_SESSION_KEY, DEFAULT_REDIRECT_URL);
                log.debug("No redirect URL provided, using default: {}", DEFAULT_REDIRECT_URL);
            }
        }

        // Redirect to Spring Security's OAuth2 authorization endpoint
        response.sendRedirect("/oauth2/authorization/azure");
    }

    /**
     * Handles the OAuth2 authentication success callback from Azure.
     * This endpoint is called by Spring Security after successful Azure authentication.
     * It:
     * 1. Extracts user information from the OAuth2 token
     * 2. Creates or updates the user in the local database
     * 3. Generates a JWT token
     * 4. Sets the JWT in a secure HTTP-only cookie
     * 5. Retrieves the stored redirect URL from session
     * 6. Redirects back to the Spring Client App
     *
     * Flow step: Azure → spring-auth (step 4-5)
     *
     * Complete OAuth2 Flow:
     * 1. Frontend → App: User initiates login
     * 2. App → spring-auth (/oauth2/login/azure): Calls with Referer header
     * 3. spring-auth → Azure: Redirects to Azure OAuth2 authorization endpoint
     * 4. Azure → spring-auth (/oauth2/success): User authenticates and is redirected back
     * 5. spring-auth → App: Redirects to stored Referer URL with JWT cookie
     * 6. App → Frontend: Redirects user back to original location with JWT cookie
     *
     * @param authentication The OAuth2 authentication token containing user information
     * @param request        The HTTP request object containing the session
     * @param response       The HTTP response object used for redirection
     * @throws IOException If an I/O error occurs during the response handling
     */
    @GetMapping("/success")
    public void oauth2Success(
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
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

        // Create a secure HTTP-only cookie with the JWT token
        ResponseCookie cookie = ResponseCookie
                .from("token", jwt)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        // Retrieve redirect URL from session, or use default
        HttpSession session = request.getSession(false);
        String redirectUrl = DEFAULT_REDIRECT_URL;

        if (session != null) {
            String storedUrl = (String) session.getAttribute(REDIRECT_URL_SESSION_KEY);
            if (storedUrl != null && !storedUrl.isEmpty()) {
                redirectUrl = storedUrl;
                // Clean up the session attribute after use
                session.removeAttribute(REDIRECT_URL_SESSION_KEY);
                log.debug("Using stored redirect URL from session: {}", redirectUrl);
            }
        }

        // Ensure redirectUrl includes loginType parameter if not already present
        if (!redirectUrl.contains("loginType=")) {
            redirectUrl += (redirectUrl.contains("?") ? "&" : "?") + "loginType=azure";
        }

        log.debug("Redirecting to frontend with JWT token in secure cookie: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
