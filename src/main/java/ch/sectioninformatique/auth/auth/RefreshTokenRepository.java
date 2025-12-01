package ch.sectioninformatique.auth.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link RefreshToken} entities.
 * 
 * Provides methods to access, find, and delete refresh tokens stored in the
 * database.
 * Uses Spring Data JPA to simplify database operations.
 * 
 * Security considerations:
 * 
 * Ensure only valid, non-revoked tokens are used for issuing new access tokens.
 * Deleting tokens for a user should be done when they log out or reset their
 * password.
 * 
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a non-revoked refresh token for a specific user.
     * 
     * Used when validating a refresh token to ensure it is still active and has not
     * been revoked.
     *
     * @param userLogin The login/username of the user.
     * @return An {@link Optional} containing the refresh token if found and not
     *         revoked, or empty otherwise.
     */
    Optional<RefreshToken> findByUserLoginAndRevokedFalse(String userLogin);

    
    /**
     * Deletes all refresh tokens associated with a specific user.
     * 
     * Typically used when a user logs out, resets their password, or when tokens need to be invalidated.
     *
     * @param userLogin The login/username of the user whose tokens should be deleted.
     */
    void deleteByUserLogin(String userLogin);
}