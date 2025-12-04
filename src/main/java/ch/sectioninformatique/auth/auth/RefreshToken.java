package ch.sectioninformatique.auth.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a refresh token stored in the database.
 * 
 * Refresh tokens are used to issue new access tokens without requiring
 * the user to log in again. For security reasons, only the **hash** of
 * the token is stored in the database, never the raw token itself.
 * 
 * Security considerations:
 * 
 * Only hashed tokens are stored to prevent leakage if the database is
 * compromised.
 * Tokens have an expiration date (`expiresAt`) to limit lifetime.
 * Tokens can be revoked manually or automatically (`revoked` field).
 * Optional `createdAt` allows detection of token reuse.
 * 
 */
@Entity
@Table(name = "refresh_tokens")
@Setter
@Getter
public class RefreshToken {

    /**
     * Primary key of the refresh token record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hash of the refresh token.
     * 
     * Never store the raw token to prevent database leaks from compromising tokens.
     */
    @Column(nullable = false, unique = true)
    private String tokenHash;

    /**
     * The login (email) of the user who owns this refresh token.
     */
    @Column(nullable = false)
    private String userLogin;

    /**
     * Expiration date/time of the refresh token.
     * 
     * After this time, the token is no longer valid.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Indicates whether the token has been revoked
     * 
     * Revoked tokens cannot be used to obtain new access tokens.
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Timestamp when the refresh token record was created.
     * 
     * Optional: useful for detecting reuse or auditing purposes.
     */
    private Instant createdAt = Instant.now();
}
