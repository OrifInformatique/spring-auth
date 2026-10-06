package ch.sectioninformatique.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the spring-auth application: an authentication API used by other
 * applications to identify their users (credentials or Azure login, JWT tokens).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthApplication.class, args);
	}

}
