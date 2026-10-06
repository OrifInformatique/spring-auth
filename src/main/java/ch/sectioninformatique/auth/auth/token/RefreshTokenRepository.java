package ch.sectioninformatique.auth.auth.token;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * Data access for {@link RefreshToken} entities. Only token hashes are stored.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * @param userLogin user's login
     * @return the user's current non-revoked refresh token, if any
     */
    Optional<RefreshToken> findByUserLoginAndRevokedFalse(String userLogin);

    /**
     * Deletes every refresh token of a user (logout, token rotation).
     *
     * @param userLogin user's login
     */
    @Modifying
    void deleteByUserLogin(String userLogin);
}
