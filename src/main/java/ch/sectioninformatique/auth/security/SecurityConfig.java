package ch.sectioninformatique.auth.security;

import java.time.Duration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security configuration for the application.
 * Two filter chains keep JWT APIs isolated from the OAuth2 browser flow:
 * - /auth/** and /users/** are stateless (JWT, no session RequestCache)
 * - /oauth2/** and /login/** use sessions for the Azure authorization code flow
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private static final PathPatternRequestMatcher.Builder PATH = PathPatternRequestMatcher.withDefaults();

    /** Writes 401 responses for unauthenticated requests. */
    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;

    /** Writes 403 responses for authenticated requests lacking an authority. */
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /** Authenticates requests carrying a bearer access token. */
    private final JwtAuthFilter jwtAuthFilter;

    /** Used to log OAuth2 details only in dev and test profiles. */
    private final Environment environment;

    private final CorsProperties corsProperties;

    /**
     * Prevents JwtAuthFilter from also being registered as a servlet Filter.
     * It must run only inside the API SecurityFilterChain, after the security context
     * is loaded and before authorization. A second servlet registration would mark the
     * filter as already executed (OncePerRequestFilter) or overwrite the context.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * JWT API chain: /auth/** and /users/**.
     * Stateless, no RequestCache (avoids MockMvc/session replay of a previous 401
     * as GET ...?continue without the Authorization header).
     *
     * Only the endpoints used to obtain tokens are public; fine-grained authorization
     * is declared on controller methods with {@code @PreAuthorize}.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring API SecurityFilterChain");
        http
                .securityMatcher(new OrRequestMatcher(
                        PATH.matcher("/auth/**"),
                        PATH.matcher("/users/**")))
                .exceptionHandling(customizer -> customizer
                        .authenticationEntryPoint(userAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(jwtAuthFilter, SecurityContextHolderFilter.class)
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(customizer -> customizer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        // Landing page of the OAuth2 flow when no client redirect URL was given:
                        // reached by a browser redirect, hence without access token
                        .requestMatchers(HttpMethod.GET, "/auth/redirect-after-login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * OAuth2 browser chain (Azure authorization code) plus remaining paths.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring OAuth2 SecurityFilterChain");
        http
                .exceptionHandling(customizer -> customizer
                        .authenticationEntryPoint(userAuthenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(jwtAuthFilter, SecurityContextHolderFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(customizer -> customizer
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2Login(oauth2 -> {
                    log.debug("Configuring OAuth2 login");
                    oauth2
                            .failureHandler((request, response, exception) -> {
                                log.error("OAuth2 authentication failed: {}", exception.getMessage());

                                if (isDevelopmentOrTest()) {
                                    log.debug("OAuth2 authentication failure details:", exception);
                                    Throwable cause = exception.getCause();
                                    if (exception instanceof OAuth2AuthenticationException oauth2Ex) {
                                        log.debug("OAuth2 Error Code: {}", oauth2Ex.getError().getErrorCode());
                                        log.debug("OAuth2 Error Description: {}", oauth2Ex.getError().getDescription());
                                        if (cause != null) {
                                            log.debug("OAuth2 Exception Cause: {}", cause.toString(), cause);
                                        }
                                    }
                                }

                                response.sendRedirect("/oauth2/error");
                            })
                            .tokenEndpoint(token -> token.accessTokenResponseClient(accessTokenResponseClient()))
                            .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService()))
                            .successHandler((request, response, authentication) -> {
                                if (isDevelopmentOrTest()) {
                                    log.debug("OAuth2 authentication successful: {}", authentication);
                                } else {
                                    log.info("OAuth2 authentication successful");
                                }
                                response.sendRedirect("/oauth2/success");
                            });
                })
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/oauth2/login").permitAll()
                        .requestMatchers("/oauth2/login/**").permitAll()
                        .requestMatchers("/oauth2/authorization/**").permitAll()
                        .requestMatchers("/oauth2/success").authenticated()
                        .requestMatchers(HttpMethod.POST, "/oauth2/token").permitAll()
                        .requestMatchers("/oauth2/error").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * CORS policy shared by both chains. Credentials are allowed because the refresh
     * token travels in a cookie.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(corsProperties.allowedOrigins());
        corsConfig.setAllowedMethods(corsProperties.allowedMethods());
        corsConfig.setAllowedHeaders(corsProperties.allowedHeaders());
        corsConfig.setAllowCredentials(true);
        return request -> corsConfig;
    }

    /**
     * Sensitive data (user emails, OAuth2 attributes) is only logged in dev and test.
     */
    private boolean isDevelopmentOrTest() {
        return environment.matchesProfiles("dev", "test");
    }

    /**
     * Configures the client used to exchange the Azure authorization code for tokens.
     *
     * The default client reuses pooled HTTPS connections to the token endpoint. When a
     * pooled connection sits idle for a few minutes, it can be silently dropped on the
     * network path (Docker NAT, firewall, Azure idle timeout), so the next login hangs
     * on the dead connection and fails with an I/O error, while an immediate retry works.
     *
     * This client uses HttpURLConnection, whose idle keep-alive connections expire after
     * a few seconds, so a stale connection is never reused. Explicit timeouts make a
     * network failure surface quickly. Message converters and error handler are the
     * same as Spring Security's defaults.
     *
     * @return the OAuth2AccessTokenResponseClient used for the authorization code grant
     */
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new FormHttpMessageConverter());
                    converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();

        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(restClient);
        return client;
    }

    /**
     * Configures the OAuth2UserService used by Spring Security.
     *
     * Responsibilities:
     * - Loads user details from the OAuth2 provider using DefaultOAuth2UserService
     * - Converts provider-specific user information into a Spring Security OAuth2User
     * - Logs user attributes for debugging purposes (only in development/test environments)
     *
     * Security Note:
     * - User attributes (email, profile info) are ONLY logged in dev/test environments
     * - Production logging does not include sensitive user data
     *
     * @return an OAuth2UserService that returns OAuth2User instances with provider attributes
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);

            if (isDevelopmentOrTest()) {
                log.debug("OAuth2 user loaded with attributes: {}", user.getAttributes());
            } else {
                log.info("OAuth2 user loaded successfully");
            }

            return user;
        };
    }
}
