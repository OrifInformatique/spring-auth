package ch.sectioninformatique.auth.auth.oauth2;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link AuthCode} entities. Only code hashes are stored.
 */
public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    /**
     * @param code      hash of the authentication code
     * @param userLogin login of the user the code was issued for
     * @return the matching code, if any
     */
    Optional<AuthCode> findByCodeAndUserLogin(String code, String userLogin);

    /**
     * Deletes every code that expired at the given instant.
     *
     * @param now reference instant
     * @return number of deleted codes
     */
    @Modifying
    @Query("DELETE FROM AuthCode a WHERE a.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
