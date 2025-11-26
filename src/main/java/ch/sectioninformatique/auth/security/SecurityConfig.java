package ch.sectioninformatique.auth.security;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;

/**
 * Spring Security configuration for the application.
 * Configures:
 * - JWT-based authentication and authorization
 * - OAuth2 login with custom success/failure handling
 * - CORS settings from application properties
 * - Exception handling for authentication and access denied events
 * - Session management (currently IF_REQUIRED)
 * 
 * Ensures:
 * - Secure endpoints with proper authorization rules
 * - Stateless or minimal session usage
 * - Proper handling of cross-origin requests
 * - Logging of OAuth2 user information for debugging
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

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins; // Origins allowed for cross-origin requests, loaded from properties

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods; // HTTP methods allowed for CORS requests

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders; // HTTP headers allowed for CORS requests

    /**
     * Configures the Spring Security filter chain.
     * 
     * Configuration includes:
     * - Exception handling using custom UserAuthenticationEntryPoint and
     * AccessDeniedHandler
     * - JWT authentication filter added before BasicAuthenticationFilter
     * - CSRF protection disabled (suitable for stateless APIs)
     * - Session management policy set to IF_REQUIRED
     * - CORS configuration using allowed origins, methods, and headers from
     * properties
     * - OAuth2 login with custom success and failure handlers
     * - Authorization rules for public endpoints and secured endpoints
     *
     * @param http the HttpSecurity object to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring SecurityFilterChain");
        http
                .exceptionHandling(customizer -> {
                    log.debug("Configuring exception handling with UserAuthenticationEntryPoint");
                    customizer
                            .authenticationEntryPoint(userAuthenticationEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler);
                })
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class)
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection");
                    csrf.disable();
                })
                .sessionManagement(customizer -> {
                    log.debug("Setting session creation policy to IF_REQUIRED");
                    customizer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                })
                .cors(cors -> {
                    log.debug("Configuring CORS");
                    cors.configurationSource(request -> {
                        var corsConfig = new CorsConfiguration();
                        corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigins));
                        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods));
                        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders));
                        corsConfig.setAllowCredentials(true);
                        return corsConfig;
                    });
                })
                .oauth2Login(oauth2 -> {
                    log.debug("Configuring OAuth2 login");
                    oauth2
                            .failureHandler((request, response, exception) -> {
                                log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);
                                Throwable cause = exception.getCause();
                                if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
                                    var oauth2Ex = (org.springframework.security.oauth2.core.OAuth2AuthenticationException) exception;
                                    log.error("OAuth2 Error Code: {}", oauth2Ex.getError().getErrorCode());
                                    log.error("OAuth2 Error Description: {}", oauth2Ex.getError().getDescription());
                                    if (cause != null) {
                                        log.error("OAuth2 Exception Cause: {}", cause.toString(), cause);
                                    }
                                }
                                // Print full stack trace for debugging
                                log.error("Full stack trace:", exception);
                                response.sendRedirect("/oauth2/error");
                            })
                            .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService()))
                            .successHandler((request, response, authentication) -> {
                                log.debug("OAuth2 authentication successful: {}", authentication);
                                response.sendRedirect("/oauth2/success");
                            });
                })
                .authorizeHttpRequests(requests -> {
                    log.debug("Configuring HTTP request authorization rules");
                    requests
                            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                            .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/oauth2/authorization/**").permitAll()
                            .requestMatchers("/oauth2/success").authenticated()
                            .requestMatchers("/oauth2/error").permitAll()
                            .requestMatchers("/login/oauth2/code/**").permitAll()
                            .anyRequest().authenticated();
                    log.debug("HTTP request authorization rules configured");
                });

        return http.build();
    }

    /**
     * Configures the OAuth2UserService used by Spring Security.
     * 
     * Responsibilities:
     * - Loads user details from the OAuth2 provider using DefaultOAuth2UserService
     * - Converts provider-specific user information into a Spring Security
     * OAuth2User
     * - Logs user attributes for debugging purposes (avoid logging sensitive data
     * in production)
     *
     * @return an OAuth2UserService that returns OAuth2User instances with provider
     *         attributes
     */
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            log.debug("OAuth2 user loaded: {}", user.getAttributes());
            return user;
        };
    }
}
