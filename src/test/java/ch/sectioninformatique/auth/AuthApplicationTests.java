package ch.sectioninformatique.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.role.RoleRepository;
import ch.sectioninformatique.auth.support.AbstractIntegrationTest;

/**
 * Checks that the application context starts and that reference data is seeded.
 */
class AuthApplicationTests extends AbstractIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void everyRoleIsSeededAtStartup() {
        for (RoleEnum role : RoleEnum.values()) {
            assertThat(roleRepository.findByName(role)).as("role %s", role).isPresent();
        }
    }
}
