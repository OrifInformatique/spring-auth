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
 * This controller manages the OAuth2 authentication process, specifically
 * handling
 * the success callback from OAuth2 providers and generating JWT tokens for
 * authenticated users.
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
     * This endpoint stores the redirect URL in the session before initiating the OAuth2 flow.
     * After successful authentication, the user will be redirected back to the stored URL.
     *
     * @param redirectUrl The URL to redirect to after successful authentication (optional)
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
     * Handles the OAuth2 authentication success callback.
     * This endpoint processes the OAuth2 authentication token, extracts user
     * information,
     * and generates a JWT token for the authenticated user. It then redirects to
     * the frontend
     * with the generated token. The redirect URL is retrieved from the session if available.
     * 
     * Frontend link : window.location.href = `${AUTH_API_URL}/auth/oauth2/login?returnTo=` + encodeURIComponent(window.location.href);
     *
     * @param authentication The OAuth2 authentication token containing user
     *                       information
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
                .sameSite("Strict")
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
