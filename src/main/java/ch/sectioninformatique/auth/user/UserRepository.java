package ch.sectioninformatique.auth.user;

import java.util.Optional;

import ch.sectioninformatique.auth.app.repository.SoftDeleteRepository;

/**
 * Repository interface for User entity operations.
 * This interface extends JpaRepository to provide basic CRUD operations
 * and adds custom query methods for user-specific operations.
 * It provides methods for:
 * - Finding users by login
 * - Checking user existence
 * - Standard CRUD operations inherited from JpaRepository
 */
public interface UserRepository extends SoftDeleteRepository<User, Long> {

    /**
     * Finds a user by his login, including those that are soft-deleted.
     * This method is used for:
     * - User lookup during operations when soft-deleted users need to be considered
     * - Checking user existence regardless of deletion status
     *
     * @param login The login to search for (case-sensitive)
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByLogin(String login);

    /**
     * Finds a user by his login, excluding soft-deleted users.
     * 
     * @param login The login to search for (case-sensitive)
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByLoginAndDeletedFalse(String login);

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
