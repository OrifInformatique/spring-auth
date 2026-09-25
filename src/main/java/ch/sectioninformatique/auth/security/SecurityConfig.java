package ch.sectioninformatique.auth.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

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

    /**
     * Entry point for handling authentication failures.
     * This component:
     * - Provides custom responses for unauthenticated requests
     * - Formats error messages in JSON
     * - Sets appropriate HTTP status codes
     */
    private final UserAuthenticationEntryPoint userAuthenticationEntryPoint;

    /**
     * Entry point for denied access failures
     */
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Filter for JWT token authentication.
     * This component:
     * - Validates JWT tokens in requests
     * - Extracts user information from tokens
     * - Sets up authentication context
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Spring environment to check active profiles (dev, test, prod)
     */
    private final Environment environment;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins; // Origins allowed for cross-origin requests, loaded from properties

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods; // HTTP methods allowed for CORS requests

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders; // HTTP headers allowed for CORS requests

    /**
     * Checks if the application is running in development or test mode.
     * Sensitive data (user emails, attributes) is only logged in these environments.
     *
     * @return true if running in dev or test profile, false if in production
     */
    private boolean isDevelopmentOrTest() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("dev") || profile.equalsIgnoreCase("test")) {
                return true;
            }
        }
        // If no profile is set, default to development-like behavior for local development
        return activeProfiles.length == 0;
    }

    private static final PathPatternRequestMatcher.Builder PATH = PathPatternRequestMatcher.withDefaults();

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
                .cors(cors -> cors.configurationSource(request -> corsConfiguration()))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
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
                .cors(cors -> cors.configurationSource(request -> corsConfiguration()))
                .oauth2Login(oauth2 -> {
                    log.debug("Configuring OAuth2 login");
                    oauth2
                            .failureHandler((request, response, exception) -> {
                                log.error("OAuth2 authentication failed: {}", exception.getMessage());

                                if (isDevelopmentOrTest()) {
                                    log.debug("OAuth2 authentication failure details:", exception);
                                    Throwable cause = exception.getCause();
                                    if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
                                        var oauth2Ex = (org.springframework.security.oauth2.core.OAuth2AuthenticationException) exception;
                                        log.debug("OAuth2 Error Code: {}", oauth2Ex.getError().getErrorCode());
                                        log.debug("OAuth2 Error Description: {}", oauth2Ex.getError().getDescription());
                                        if (cause != null) {
                                            log.debug("OAuth2 Exception Cause: {}", cause.toString(), cause);
                                        }
                                    }
                                }

                                response.sendRedirect("/oauth2/error");
                            })
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

    private CorsConfiguration corsConfiguration() {
        var corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods));
        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders));
        corsConfig.setAllowCredentials(true);
        return corsConfig;
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
