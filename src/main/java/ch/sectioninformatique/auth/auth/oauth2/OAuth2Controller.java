package ch.sectioninformatique.auth.auth.oauth2;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import ch.sectioninformatique.auth.auth.AuthExceptions.MissingOAuth2AttributeException;
import ch.sectioninformatique.auth.auth.AuthExceptions.OAuth2LoginFailedException;
import ch.sectioninformatique.auth.auth.AuthService;
import ch.sectioninformatique.auth.auth.AuthService.AuthResult;
import ch.sectioninformatique.auth.auth.dto.AuthCodeExchangeDto;
import ch.sectioninformatique.auth.auth.token.RefreshTokenCookieFactory;
import ch.sectioninformatique.auth.user.UserService;
import ch.sectioninformatique.auth.user.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure (Microsoft Entra ID) login flow.
 *
 * 1. The client application sends the browser to {@code GET /oauth2/login/azure}; the URL to
 *    come back to is stored in the session.
 * 2. Spring Security redirects to Azure, then handles the callback and calls
 *    {@code GET /oauth2/success}.
 * 3. The user is created locally on first login, a one-time code is generated, and the browser
 *    is redirected to the client application with {@code authCode} and {@code userId}.
 * 4. The client application exchanges the code for tokens with {@code POST /oauth2/token}.
 */
@Slf4j
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    /** Session attribute holding the client URL to redirect to after login. */
    private static final String REDIRECT_URL_SESSION_KEY = "OAUTH2_RETURN_URL";

    /** Fallback redirect target when the client gave no acceptable URL. */
    private static final String DEFAULT_REDIRECT_URL = "/auth/redirect-after-login";

    private final AuthService authService;
    private final AuthCodeService authCodeService;
    private final UserService userService;
    private final RedirectUrlPolicy redirectUrlPolicy;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    /**
     * Starts the Azure login. The URL to come back to is taken from the redirectUrl
     * parameter, else from the Referer header, and must pass {@link RedirectUrlPolicy}.
     *
     * @param redirectUrl client URL to redirect to after login (optional)
     * @param request     current request, holding the session and Referer header
     * @param response    response used to redirect to the Spring Security authorization endpoint
     * @throws IOException if the redirect fails
     */
    @GetMapping("/login/azure")
    public void initiateAzureLogin(
            @RequestParam(required = false) String redirectUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String candidate = redirectUrl != null ? redirectUrl : request.getHeader(HttpHeaders.REFERER);
        String target = redirectUrlPolicy.isAllowed(candidate) ? candidate : DEFAULT_REDIRECT_URL;
        if (candidate != null && target.equals(DEFAULT_REDIRECT_URL)) {
            log.warn("Rejected OAuth2 redirect URL not matching the allowed origins: {}", candidate);
        }

        request.getSession(true).setAttribute(REDIRECT_URL_SESSION_KEY, target);
        response.sendRedirect("/oauth2/authorization/azure");
    }

    /**
     * Called by Spring Security once Azure authenticated the user. Redirects the browser to
     * the client application with a one-time authentication code.
     *
     * @param authentication Azure authentication, provided by Spring Security
     * @param request        current request, holding the session
     * @param response       response used to redirect to the client application
     * @throws IOException if the redirect fails
     */
    @GetMapping("/success")
    public void oauth2Success(
            OAuth2AuthenticationToken authentication,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        OAuth2User principal = authentication.getPrincipal();
        String email = principal.getAttribute("email");
        if (email == null) {
            throw new MissingOAuth2AttributeException("email");
        }
        // Some Azure accounts have no given_name: fall back to the display name
        String firstName = principal.getAttribute("given_name");
        if (firstName == null) {
            firstName = principal.getAttribute("name");
        }
        UserDto user = userService.getOrCreateAzureUser(email, firstName, principal.getAttribute("family_name"));

        // The session was only needed to carry the redirect URL through the Azure round trip
        String redirectUrl = DEFAULT_REDIRECT_URL;
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object stored = session.getAttribute(REDIRECT_URL_SESSION_KEY);
            if (stored instanceof String storedUrl && !storedUrl.isEmpty()) {
                redirectUrl = storedUrl;
            }
            session.invalidate();
        }

        String code = authCodeService.generate(user.getLogin(), redirectUrl);
        UriComponentsBuilder target = UriComponentsBuilder.fromUriString(redirectUrl);
        if (!redirectUrl.contains("loginType=")) {
            target.queryParam("loginType", "azure");
        }
        target.queryParam("authCode", code).queryParam("userId", user.getId());

        log.debug("Redirecting to client application with an authentication code");
        response.sendRedirect(target.encode().build().toUriString());
    }

    /**
     * Target of Spring Security when the Azure login fails: answers 401 with the
     * standard error body.
     */
    @GetMapping("/error")
    public void oauth2Error() {
        throw new OAuth2LoginFailedException();
    }

    /**
     * Exchanges a one-time authentication code for tokens.
     *
     * @param request user id and code received in the redirect URL
     * @return 200 with the user and access token, refresh token in a cookie
     */
    @PostMapping("/token")
    public ResponseEntity<UserDto> exchangeAuthCode(@RequestBody @Valid AuthCodeExchangeDto request) {
        AuthResult result = authService.exchangeAuthCode(request.userId(), request.code());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.create(result.refreshToken()).toString())
                .body(result.user());
    }
}
