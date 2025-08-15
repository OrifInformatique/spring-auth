package ch.sectioninformatique.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * This interface extends JpaRepository to provide basic CRUD operations
 * and adds custom query methods for user-specific operations.
 * It provides methods for:
 * - Finding users by login
 * - Checking user existence
 * - Standard CRUD operations inherited from JpaRepository
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their login username.
     * This method is used for:
     * - User authentication
     * - User lookup during operations
     * - Checking user existence
     *
     * @param login The login username to search for (case-sensitive)
     * @return Optional containing the user if found, empty Optional otherwise
     */
    Optional<User> findByLogin(String login);
    
    /**
     * Checks if a user with the given login username exists.
     * This method is used for:
     * - Validating new user registration
     * - Checking login uniqueness
     * - Preventing duplicate user accounts
     *
     * @param login The login username to check (case-sensitive)
     * @return true if a user with the given login exists, false otherwise
     */
    boolean existsByLogin(String login);
}

