package ch.sectioninformatique.auth.support;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.role.RoleRepository;
import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Creates the reference users of the test profile, once per application context.
 * Tests run in rolled-back transactions, so this data is the same for every test.
 *
 * Logins and passwords are exposed as constants in {@link TestUsers}.
 */
@Component
@Order(2)
@Profile("test")
@RequiredArgsConstructor
public class TestUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.saveAll(List.of(
                user("Test", "User", TestUsers.USER, TestUsers.USER_PASSWORD, RoleEnum.USER, false),
                user("Test", "Manager", TestUsers.MANAGER, TestUsers.MANAGER_PASSWORD, RoleEnum.MANAGER, false),
                user("Test", "Admin", TestUsers.ADMIN, TestUsers.ADMIN_PASSWORD, RoleEnum.ADMIN, false),
                user("Other", "Admin", TestUsers.OTHER_ADMIN, TestUsers.ADMIN_PASSWORD, RoleEnum.ADMIN, false),
                user("Deleted", "User", TestUsers.DELETED, TestUsers.USER_PASSWORD, RoleEnum.USER, true)));
    }

    private User user(String firstName, String lastName, String login, String password, RoleEnum role,
            boolean deleted) {
        Role mainRole = roleRepository.findByName(role)
                .orElseThrow(() -> new IllegalStateException("Role " + role + " not seeded"));
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .login(login)
                .password(passwordEncoder.encode(password))
                .mainRole(mainRole)
                .deleted(deleted)
                .build();
    }
}
