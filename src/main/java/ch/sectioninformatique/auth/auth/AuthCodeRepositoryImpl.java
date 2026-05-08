package ch.sectioninformatique.auth.auth;

import java.time.Instant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

/**
 * Implementation of the {@link AuthCodeRepository} interface.
 */
public class AuthCodeRepositoryImpl {

    // EntityManager for interacting with the database
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Deletes all expired authentication codes from the database.
     */
    @Transactional
    public void deleteExpiredCodes() {
        String jpql = "DELETE FROM AuthCode e WHERE e.expiresAt <= :now";

        entityManager
            .createQuery(jpql)
            .setParameter("now", Instant.now())
            .executeUpdate();

    }
}
