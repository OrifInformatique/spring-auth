package ch.sectioninformatique.auth.auth;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.sectioninformatique.auth.security.UserAuthenticationProvider;
import ch.sectioninformatique.auth.user.UserDto;
import ch.sectioninformatique.auth.user.UserService;

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

    // Name of the Session attribute used to store the frontend redirect URL during login initiation.
    private static final String REDIRECT_URL_SESSION_KEY = "OAUTH2_RETURN_URL";

    // Fallback redirect target when no external redirect URL is available.
    // This should be a valid endpoint in the client application that can handle the post-login state.
    private static final String DEFAULT_REDIRECT_URL = "/auth/redirect-after-login";

    // A custom provider used to generate JWT tokens for authenticated users.
    private final UserAuthenticationProvider userAuthenticationProvider;
    // Service responsible for creating or retrieving users in the local database.
    private final UserService userService;
    // MessageSource for internationalized messages, used for error handling and logging.
    private final MessageSource messageSource;
    // Logger for debugging and monitoring the OAuth2 authentication flow.
    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);

    /**
     * Constructs a new Oauth2Controller with the required dependencies.
     *
     * @param userAuthenticationProvider Provider for user authentication and token generation
     * @param userService                Service for users management
     * @param messageSource              MessageSource for internationalization of messages
     */
    public OAuth2Controller(UserAuthenticationProvider userAuthenticationProvider,
                            UserService userService,
                            MessageSource messageSource) {
        
        this.userAuthenticationProvider = userAuthenticationProvider;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    /**
     * Initiates OAuth2 authentication flow with Azure via a simple redirect.
     * This endpoint provides a direct way to start the OAuth2 login process
     * by redirecting to Spring Security's OAuth2 authorization endpoint.
     * Unlike /login/azure, it does not store a custom redirect URL in the session.
     *
     * @return ResponseEntity with HTTP 302 redirect to Azure authorization endpoint
     */
    @GetMapping("/login")
    public ResponseEntity<Object> testCallOAuth2() {
        System.out.println("Test initiating OAuth2 login flow with Azure...");

        // Redirect frontend to spring-auth OAuth2 login endpoint
        URI uri = URI.create("/oauth2/authorization/azure");
        return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
    }

    /**
     * Initiates OAuth2 authentication flow with Azure.
     * This endpoint is called by the client application to start the OAuth2 flow.
     * It stores the calling URL (from Referer header or redirectUrl parameter) in the session,
     * then redirects to Spring Security's OAuth2 authorization endpoint.
     *
     * @param redirectUrl  The URL to redirect to after successful authentication (optional).
     *                     If not provided, uses the Referer header. If neither is available,
     *                     uses DEFAULT_REDIRECT_URL.
     * @param request      The HTTP request object containing the session
     * @param response     The HTTP response object used for redirection
     * @throws IOException If an I/O error occurs during the response handling
     */
    @GetMapping("/login/azure")
    public void initiateAzureLogin(
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        System.out.println("Initiating OAuth2 login flow with Azure...");

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
     * 6. Redirects back to the client application with the JWT cookie set
     *
     * Complete OAuth2 Flow:
     * 1. Client application → spring-auth (/oauth2/login/azure): Calls with Referer header
     * 2. spring-auth → Azure: Redirects to Azure OAuth2 authorization endpoint
     * 3. Azure → spring-auth (/oauth2/success): Callback after User authentication
     * 4. spring-auth → client application: Redirects to stored Referer URL with JWT cookie
     *
     * @param authentication The OAuth2 authentication token containing user information
     * @param request        The HTTP request object containing the session
     * @param response       The HTTP response object used for redirection
     * @throws IOException   If an I/O error occurs during the response handling
     */
    @GetMapping("/success")
    public void oauth2Success(
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        if (authentication == null) {
            String message = messageSource.getMessage("error.oauth2.missing.authentication", null, LocaleContextHolder.getLocale());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            return;
        }

        // Retrieve OAuth2User principal from the authentication token.
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        if (principal == null) {
            String message = messageSource.getMessage("error.oauth2.user.not.found", null, LocaleContextHolder.getLocale());
            response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            return;
        }

        // Map OAuth2User attributes to your UserDto.
        // Adjust the attribute keys as needed based on your Azure configuration.
        String email = principal.getAttribute("email");
        String givenName = principal.getAttribute("given_name");
        String familyName = principal.getAttribute("family_name");

        if (Objects.isNull(email)) {
            String message = messageSource.getMessage(
                    "error.oauth2.missing.user.attribute",
                    new Object[] {"email"},
                    LocaleContextHolder.getLocale()
            );
            response.sendError(HttpStatus.UNAUTHORIZED.value(), message);
            return;
        }

        UserDto user = UserDto.builder()
                .login(email)
                .firstName(givenName)
                .lastName(familyName)
                .build();

        // Create or get Azure user in local database
        user = userService.getOrCreateAzureUser(user);

        // Generate a JWT using your custom UserAuthenticationProvider.
        String jwt = userAuthenticationProvider.createToken(user);

        // Create a secure HTTP-only cookie with the JWT token
        ResponseCookie cookie = ResponseCookie
                .from("token", jwt)
                .httpOnly(false) // Accessible via JavaScript
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

        log.debug("Redirecting to client application with JWT token in secure cookie: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}
