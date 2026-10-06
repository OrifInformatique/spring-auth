package ch.sectioninformatique.auth.user;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ch.sectioninformatique.auth.role.Role;
import ch.sectioninformatique.auth.role.RoleEnum;
import ch.sectioninformatique.auth.role.RoleRepository;

/**
 * Creates demonstration users with every role when the database is empty.
 * Only active in the dev profile, and runs after {@link ch.sectioninformatique.auth.role.RoleSeeder}.
 */
@Component
@Order(2)
@Profile({"dev"})
public class UserSeeder implements CommandLineRunner {

	/** Repository for user data access */
	private final UserRepository userRepository;

	/** Encoder for password hashing */
	private final PasswordEncoder passwordEncoder;

	/** Repository for role data access */
	private final RoleRepository roleRepository;

	/** Logger for seeding operations */
	private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

	/**
	 * Constructs a new UserSeeder with the required dependencies.
	 *
	 * @param userRepository Repository for user data access
	 * @param passwordEncoder Encoder for password hashing
	 * @param roleRepository Repository for role data access
	 */
	public UserSeeder(UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.roleRepository = roleRepository;
	}

	@Override
	public void run(String... args) {
		log.info("Starting User Seeding...");
		loadUserData();
		log.info("User Seeding completed.");
	}

	/**
	 * Creates regular users (USER), one manager (Jane Smith), one administrator
	 * (Super Admin) and a user meant to be soft-deleted by hand, if no user exists yet.
	 *
	 * @throws IllegalStateException if a role has not been seeded
	 */
	private void loadUserData() {
		if (this.userRepository.count() == 0) {
			Role userRole = roleRepository.findByName(RoleEnum.USER)
					.orElseThrow(() -> new IllegalStateException("Role USER not found"));
			Role managerRole = roleRepository.findByName(RoleEnum.MANAGER)
					.orElseThrow(() -> new IllegalStateException("Role MANAGER not found"));
			Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
					.orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));

			// Create users with User.builder()
			User user0 = User.builder()
					.firstName("deleted")
					.lastName("user")
					.login("deleted.user@test.com")
					.password(passwordEncoder.encode("NoN33dPassword@nymore!"))
					.mainRole(userRole)
					.build();

			User user1 = User.builder()
					.firstName("John")
					.lastName("DOE")
					.login("john.doe@test.com")
					.password(passwordEncoder.encode("Secure123@Pass"))
					.mainRole(userRole)
					.build();

			User user2 = User.builder()
					.firstName("Jane")
					.lastName("SMITH")
					.login("jane.smith@test.com")
					.password(passwordEncoder.encode("Complex#789Pwd"))
					.mainRole(managerRole)
					.build();

			User user3 = User.builder()
					.firstName("Alice")
					.lastName("JOHNSON")
					.login("alice.johnson@test.com")
					.password(passwordEncoder.encode("Test$4321Now"))
					.mainRole(userRole)
					.build();

			User user4 = User.builder()
					.firstName("Dan")
					.lastName("SERGEANT")
					.login("dan.sergeant@test.com")
					.password(passwordEncoder.encode("Spring2024@Dev"))
					.mainRole(userRole)
					.build();

			User user5 = User.builder()
					.firstName("Bobby")
					.lastName("BALLOONZI")
					.login("bobby.balloonzi@test.com")
					.password(passwordEncoder.encode("P@ssw0rd2024"))
					.mainRole(userRole)
					.build();

			User user6 = User.builder()
					.firstName("Rob")
					.lastName("JAKE")
					.login("rob.jake@test.com")
					.password(passwordEncoder.encode("Inf0#Security24"))
					.mainRole(userRole)
					.build();

			User user7 = User.builder()
					.firstName("Super")
					.lastName("Admin")
					.login("super.admin@test.com")
					.password(passwordEncoder.encode("ReallySecure123@PassWordBecauseIWantToBeSuperSafe"))
					.mainRole(adminRole)
					.build();

			userRepository.saveAll(Arrays.asList(user0, user1, user2, user3, user4, user5, user6, user7));
		} else {
			log.info("Users table not empty - Skipping user seeding");
		}
	}
}