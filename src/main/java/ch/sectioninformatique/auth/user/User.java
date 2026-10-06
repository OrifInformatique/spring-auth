package ch.sectioninformatique.auth.user;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.sectioninformatique.auth.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * User stored in the 'users' table.
 *
 * Deletion is soft by default: {@code repository.delete(user)} only sets the
 * {@code deleted} flag (see {@link SQLDelete}), so the account can be restored.
 * A soft-deleted user cannot log in.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    @Column(nullable = false, name = "first_name")
    private String firstName;

    /** Optional: some Azure accounts have no last name. */
    @Column(nullable = true, name = "last_name")
    private String lastName;

    /** Unique login, the user's email. */
    @Column(unique = true, nullable = false)
    private String login;

    /** BCrypt hash of the password. */
    @Column(nullable = false, length = 72)
    private String password;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    /** Soft-delete flag. */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @ManyToOne(fetch = FetchType.EAGER)
    private Role mainRole;

    /**
     * @return the authorities of the user's role, or none if the user has no role
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (mainRole == null || mainRole.getName() == null) {
            return Set.of();
        }
        return mainRole.getName().getGrantedAuthorities();
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !deleted;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !deleted;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !deleted;
    }

    @Override
    public boolean isEnabled() {
        return !deleted;
    }
}
