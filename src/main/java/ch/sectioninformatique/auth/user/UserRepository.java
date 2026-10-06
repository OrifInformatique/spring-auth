package ch.sectioninformatique.auth.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link User} entities.
 *
 * Methods without a "Deleted" criterion also return soft-deleted users.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** @return the user with this login, soft-deleted or not */
    Optional<User> findByLogin(String login);

    /** @return the active (not soft-deleted) user with this login */
    Optional<User> findByLoginAndDeletedFalse(String login);

    /** @return the soft-deleted user with this login */
    Optional<User> findByLoginAndDeletedTrue(String login);

    /** @return the active (not soft-deleted) user with this id */
    Optional<User> findByIdAndDeletedFalse(Long id);

    /** @return active users ({@code deleted = false}) or soft-deleted users ({@code deleted = true}) */
    List<User> findAllByDeleted(boolean deleted, Sort sort);

    /** @return true if a user, soft-deleted or not, already uses this login */
    boolean existsByLogin(String login);

    /**
     * Permanently deletes a user. A native query is required because
     * {@code delete()} is turned into a soft delete by {@link org.hibernate.annotations.SQLDelete}.
     *
     * @param id id of the user to delete
     */
    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    void deletePermanentlyById(@Param("id") Long id);
}
