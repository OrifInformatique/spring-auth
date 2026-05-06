package ch.sectioninformatique.auth.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link AuthCode} entities.
 * 
 * Provides methods to access, find, and delete authentication codes
 * stored in the database. Uses Spring Data JPA to simplify database operations.
 * 
 * Security considerations for service layer:
 * Only store hashed authentication codes.
 * Ensure only valid, unused authentication codes are used to get JWT tokens.
 * Codes are one use only and are deleted immediately after use.
 */
@Repository
public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    // Define a method to get the authentication code(s) corresponding to one user
    List<AuthCode> findByUserLogin(String userLogin);

    // Define a method to delete all expired authentication codes
    void deleteExpiredCodes();
}
