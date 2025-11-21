package ch.sectioninformatique.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
     * Returns all users including those that are soft-deleted.
     */
    @Query("SELECT u FROM User u")
    List<User> findAllWithDeleted();

    /**
     * Returns only soft-deleted users.
     */
    @Query("SELECT u FROM User u WHERE u.deleted = true")
    List<User> findAllDeleted();

    /**
     * Returne a deleted user from his id
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deleted = true")
    Optional<User> findByIdDeleted(@Param("id") Long id);

    /*
    * Permanently delete a user
    * from the database, bypassing any soft delete mechanisms.
    */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    void deletePermanentlyById(Long id);

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
