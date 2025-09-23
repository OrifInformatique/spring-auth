package ch.sectioninformatique.auth.security;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Seeder class for initializing the database with default roles.
 * This class implements ApplicationListener to execute the seeding process
 * when the application context is refreshed. It creates the basic roles
 * (USER, MANAGER, ADMIN) if they don't already exist in the database.
 */
@Component
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {

    /** Repository for role data access */
    private final RoleRepository roleRepository;

    /**
     * Constructs a new RoleSeeder with the required repository.
     *
     * @param roleRepository Repository for role data access
     */
    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Handles the application context refresh event by loading roles.
     * This method is called by Spring when the application context is refreshed.
     *
     * @param contextRefreshedEvent The event indicating that the application context has been refreshed
     */
    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent contextRefreshedEvent) {
        this.loadRoles();
    }

    /**
     * Loads the default roles into the database.
     * Creates the following roles if they don't exist:
     * - USER: Default user role
     * - MANAGER: Manager role
     * - ADMIN: Administrator role
     * Each role is created with a descriptive name and description.
     */
    private void loadRoles() {
        RoleEnum[] roleNames = new RoleEnum[] { RoleEnum.USER, RoleEnum.MANAGER,
                RoleEnum.ADMIN };

        Map<RoleEnum, String> roleDescriptionMap = Map.of(
                RoleEnum.USER, "Default user role",
                RoleEnum.MANAGER, "Manager role",
                RoleEnum.ADMIN, "Administrator role");

        Arrays.stream(roleNames).forEach((roleName) -> {
            Optional<Role> optionalRole = roleRepository.findByName(roleName);
            optionalRole.ifPresentOrElse(System.out::println, () -> {
                Role roleToCreate = new Role();
                roleToCreate.setName(roleName);
                roleToCreate.setDescription(roleDescriptionMap.get(roleName));
                roleRepository.save(roleToCreate);
            });
        });
    }
}
