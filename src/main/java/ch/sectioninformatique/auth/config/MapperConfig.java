package ch.sectioninformatique.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.sectioninformatique.auth.user.UserMapper;

/**
 * Fallback configuration to expose MapStruct mappers as Spring beans.
 *
 * This registers the pre-generated INSTANCE provided by the mapper
 * interface (UserMapper.INSTANCE) as a Spring bean. It's a small and
 * safe fallback that allows the ApplicationContext to load even when
 * annotation processing did not run (for example after a failed build
 * or an interrupted Docker build).
 */
@Configuration
public class MapperConfig {

    @Bean
    public UserMapper userMapper() {
        return UserMapper.INSTANCE;
    }
}
