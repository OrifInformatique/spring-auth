package ch.sectioninformatique.auth.role;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates the roles defined by {@link RoleEnum} if they are missing from the database.
 *
 * Runs in every profile, before the user seeders ({@code @Order(1)}), because users
 * cannot be created without their role.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (RoleEnum roleName : RoleEnum.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(roleName.getDescription());
                roleRepository.save(role);
                log.info("Role {} created", roleName);
            }
        }
    }
}
