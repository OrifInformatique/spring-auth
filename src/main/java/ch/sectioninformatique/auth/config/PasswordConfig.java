package ch.sectioninformatique.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password hashing configuration.
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt encoder used to hash passwords before storage and to verify them at login.
     * BCrypt only uses the first 72 bytes of a password, hence the 72 characters
     * maximum enforced on password fields.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
