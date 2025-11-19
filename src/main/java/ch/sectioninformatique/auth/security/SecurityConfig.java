package ch.sectioninformatique.auth.security;

import lombok.RequiredArgsConstructor;
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
 * Security configuration class for the application.
 * This class configures Spring Security settings including:
 * - Authentication and authorization rules
 * - CORS configuration
 * - OAuth2 login settings
 * - JWT filter integration
 * - Session management
 * - Exception handling
 * 
 * The configuration ensures:
 * - Secure endpoints with appropriate authorization
 * - Cross-origin request handling
 * - Stateless session management
 * - Custom authentication failure handling
 * - OAuth2 integration for external authentication
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
     * Filter for JWT token authentication.
     * This component:
     * - Validates JWT tokens in requests
     * - Extracts user information from tokens
     * - Sets up authentication context
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configures the security filter chain with all necessary security settings.
     * This method:
     * - Sets up exception handling with custom entry point
     * - Configures JWT authentication filter
     * - Disables CSRF protection (not needed for stateless API)
     * - Sets session management policy to ALWAYS
     * - Configures CORS with allowed origins and methods
     * - Sets up OAuth2 login with success/failure handlers
     * - Defines HTTP request authorization rules
     *
     * @param http The HttpSecurity object to configure
     * @return The configured SecurityFilterChain
     * @throws Exception if security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring SecurityFilterChain");
        http
                .exceptionHandling(customizer -> {
                    log.debug("Configuring exception handling with UserAuthenticationEntryPoint");
                    customizer.authenticationEntryPoint(userAuthenticationEntryPoint);
                })
                .addFilterBefore(jwtAuthFilter, BasicAuthenticationFilter.class)
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection");
                    csrf.disable();
                })
                .sessionManagement(customizer -> {
                    log.debug("Setting session creation policy to ALWAYS");
                    customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .cors(cors -> {
                    log.debug("Configuring CORS");
                    cors.configurationSource(request -> {
                        var corsConfig = new CorsConfiguration();
                        corsConfig.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:4000",
                                "http://localhost:8080"));

                        corsConfig.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "OPTIONS"));

                        corsConfig.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "Accept"));

                        corsConfig.setExposedHeaders(Arrays.asList(
                                "Authorization"));

                        corsConfig.setAllowCredentials(true);
                        return corsConfig;
                    });
                })
                .oauth2Login(oauth2 -> {
                    log.debug("Configuring OAuth2 login");
                    oauth2
                            .defaultSuccessUrl("/oauth2/success", true)
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
     * Creates and configures the OAuth2 user service for handling OAuth2
     * authentication.
     * This service:
     * - Loads user information from the OAuth2 provider
     * - Converts OAuth2 user data into an OAuth2User object
     * - Logs user attributes for debugging
     * - Uses the default OAuth2 user service implementation
     *
     * @return Configured OAuth2UserService instance
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
