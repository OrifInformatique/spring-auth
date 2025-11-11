package ch.sectioninformatique.auth.user;

/**
 * Custom repository interface for hard deletion of User entities.
 * This interface defines a method for permanently deleting a user
 * from the database, bypassing any soft delete mechanisms.
 */
public interface UserRepositoryHardDelete {
    void deletePermanentlyById(Long id);
}
