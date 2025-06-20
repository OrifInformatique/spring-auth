package ch.sectioninformatique.template.user;

import ch.sectioninformatique.template.security.Role;
import ch.sectioninformatique.template.security.RoleEnum;
import ch.sectioninformatique.template.security.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.Set;

/**
 * Seeder class for initializing the database with default user data.
 * This class implements CommandLineRunner to execute the seeding process
 * when the application starts. It creates a set of predefined users with
 * different roles (USER, ADMIN, SUPER_ADMIN) for testing and development purposes.
 * The seeder runs after the RoleSeeder (Order(2)) to ensure roles exist before
 * creating users.
 */
@Component
@Order(2)
public class UserSeeder implements CommandLineRunner {

	/** Repository for user data access */
	private final UserRepository userRepository;

	/** Encoder for password hashing */
	private final PasswordEncoder passwordEncoder;

	/** Repository for role data access */
	private final RoleRepository roleRepository;

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

	/**
	 * Executes the seeding process when the application starts.
	 * This method is called by Spring Boot after the application context is loaded.
	 * It:
	 * 1. Prints a start message
	 * 2. Calls loadUserData() to create default users
	 * 3. Prints a completion message
	 *
	 * @param args Command line arguments passed to the application
	 * @throws Exception if an error occurs during the seeding process
	 */
	@Override
	public void run(String... args) throws Exception {
		System.out.println("Starting User Seeding...");
		loadUserData();
		System.out.println("User Seeding completed.");
	}

	/**
	 * Loads initial user data into the database.
	 * Creates a set of predefined users with different roles if the database is empty.
	 * The users include:
	 * - A deleted user (ID 1) for handling deleted user's items
	 * - Regular users with USER role (John Doe, Alice Johnson, Dan Sergeant, etc.)
	 * - An admin user with ADMIN role (Jane Smith)
	 * - A super admin user with SUPER_ADMIN role (Super Admin)
	 * 
	 * Each user is created with:
	 * - Unique login (email format)
	 * - Secure password (hashed)
	 * - First and last name
	 * - Appropriate role(s)
	 *
	 * @throws RuntimeException if any required role (USER, ADMIN, SUPER_ADMIN) is not found in the database
	 */
	private void loadUserData() {
		if (this.userRepository.count() == 0) {
			Role userRole = roleRepository.findByName(RoleEnum.USER)
					.orElseThrow(() -> new RuntimeException("Role USER not found"));
			Role adminRole = roleRepository.findByName(RoleEnum.ADMIN)
					.orElseThrow(() -> new RuntimeException("Role ADMIN not found"));
			Role superAdminRole = roleRepository.findByName(RoleEnum.SUPER_ADMIN)
					.orElseThrow(() -> new RuntimeException("Role SUPER_ADMIN not found"));

			// Create users with User.builder()
			User user0 = User.builder()
					.firstName("deleted")
					.lastName("user")
					.login("deleted.user@test.com")
					.password(passwordEncoder.encode("NoN33dPassword@nymore!"))
					.roles(Set.of(userRole))
					.build();

			User user1 = User.builder()
					.firstName("John")
					.lastName("DOE")
					.login("john.doe@test.com")
					.password(passwordEncoder.encode("Secure123@Pass"))
					.roles(Set.of(userRole))
					.build();

			User user2 = User.builder()
					.firstName("Jane")
					.lastName("SMITH")
					.login("jane.smith@test.com")
					.password(passwordEncoder.encode("Complex#789Pwd"))
					.roles(Set.of(adminRole))
					.build();

			User user3 = User.builder()
					.firstName("Alice")
					.lastName("JOHNSON")
					.login("alice.johnson@test.com")
					.password(passwordEncoder.encode("Test$4321Now"))
					.roles(Set.of(userRole))
					.build();

			User user4 = User.builder()
					.firstName("Dan")
					.lastName("SERGEANT")
					.login("dan.sergeant@test.com")
					.password(passwordEncoder.encode("Spring2024@Dev"))
					.roles(Set.of(userRole))
					.build();

			User user5 = User.builder()
					.firstName("Bobby")
					.lastName("BALLOONZI")
					.login("bobby.balloonzi@test.com")
					.password(passwordEncoder.encode("P@ssw0rd2024"))
					.roles(Set.of(userRole))
					.build();

			User user6 = User.builder()
					.firstName("Rob")
					.lastName("JAKE")
					.login("rob.jake@test.com")
					.password(passwordEncoder.encode("Inf0#Security24"))
					.roles(Set.of(userRole))
					.build();

			User user7 = User.builder()
					.firstName("Super")
					.lastName("Admin")
					.login("super.admin@test.com")
					.password(passwordEncoder.encode("ReallySecure123@PassWordBecauseIWantToBeSuperSafe"))
					.roles(Set.of(superAdminRole))
					.build();

			userRepository.saveAll(Arrays.asList(user0, user1, user2, user3, user4, user5, user6, user7));
		} else {
			System.out.println("Users table not empty - Skipping user seeding");
		}
	}
}