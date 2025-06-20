package ch.sectioninformatique.auth.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Configuration class for password encoding.
 * This class provides a bean for password encoding using BCrypt algorithm,
 * which is used throughout the application for secure password handling.
 */
@Component
public class PasswordConfig {

    /**
     * Creates and configures a BCrypt password encoder bean.
     * This encoder is used for hashing passwords before storing them in the database
     * and for verifying passwords during authentication.
     *
     * @return A configured BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
