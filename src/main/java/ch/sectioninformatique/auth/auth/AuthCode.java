package ch.sectioninformatique.auth.auth;

import java.time.Instant;
import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity defining a temporary authentication code, with a short lifespan.
 * 
 * It is used after a successful OAuth2 login and sent to the client application to let it
 * exchange for JWT access and refresh tokens.
 * 
 * Security considerations:
 * Only hashed codes are stored to prevent leakage if the database is compromised.
 * Codes have an expiration date (`expiresAt`) to limit lifetime.
 * Codes can only be used one time and are erased after use.
 */
@Entity
@Table(name = "auth_codes")
@Setter
@Getter
public class AuthCode {
    /**
     * Primary key of the authentication code.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hash of the authentication code.
     * 
     * Never store the raw code to prevent database leaks from compromising codes.
     */
    @Column(nullable = false, unique = true)
    private String code;

    /**
     * The login (email) of the user who owns this authentication code.
     */
    @Column(nullable = false)
    private String userLogin;

    /**
     * The redirect URL linked to the authentication code.
     */
    @Column(nullable = false)
    private String redirectUrl;

    /**
     * Expiration date/time of the authentication code.
     * 
     * After this time, the code is no longer valid.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Timestamp when the authentication code was created.
     */
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;
}
