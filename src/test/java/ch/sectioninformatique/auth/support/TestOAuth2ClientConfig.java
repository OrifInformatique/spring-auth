package ch.sectioninformatique.auth.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Static Azure client registration for the test profile.
 *
 * The production configuration sets an issuer-uri, which makes Spring Boot fetch the
 * OpenID discovery document from Azure at startup. Declaring this bean makes the Boot
 * auto-configuration back off, so tests never need network access or a real tenant.
 */
@Configuration
@Profile("test")
public class TestOAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration azure = ClientRegistration.withRegistrationId("azure")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://login.example.test/authorize")
                .tokenUri("https://login.example.test/token")
                .userInfoUri("https://login.example.test/userinfo")
                .jwkSetUri("https://login.example.test/keys")
                .userNameAttributeName("email")
                .clientName("Azure")
                .build();
        return new InMemoryClientRegistrationRepository(azure);
    }
}
