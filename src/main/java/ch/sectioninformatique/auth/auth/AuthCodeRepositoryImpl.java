package ch.sectioninformatique.auth.auth;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    public void deleteExpiredCodes() {
        String jpql = "DELETE e FROM AuthCode e WHERE e.dateExpiration <= :now";

        entityManager.createQuery(jpql, AuthCode.class)
            .setParameter("now", LocalDateTime.now())
            .getSingleResult();
    }
}
