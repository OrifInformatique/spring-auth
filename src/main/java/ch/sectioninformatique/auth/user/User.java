package ch.sectioninformatique.auth.user;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.sectioninformatique.auth.security.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a user in the system.
 * This class implements Spring Security's UserDetails interface to provide
 * user authentication and authorization functionality. It maps to the 'users'
 * table in the database and contains all necessary user information including
 * personal details, credentials, and roles.
 */
@Data
@Entity
@Table(name = "users")
@Builder
@NoArgsConstructor
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@FilterDef(name = "deletedFilter", parameters = @ParamDef(name = "isDeleted", type = Boolean.class))
@Filter(name = "deletedFilter", condition = "deleted = :isDeleted")
public class User implements UserDetails {

    /**
     * Unique identifier for the user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    /**
     * User's first name.
     */
    @Column(nullable = false, name = "first_name")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(nullable = false, name = "last_name")
    private String lastName;

    /**
     * User's unique login identifier (email).
     */
    @Column(unique = true, length = 100, nullable = false)
    private String login;

    /**
     * User's hashed password.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Timestamp when the user account was created.
     * This field cannot be updated after creation.
     */
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    /**
     * Timestamp when the user account was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * Indicates whether the user account is soft-deleted.
     * A value of true means the account is deleted, false means active.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Retrieves the authorities granted to the user.
     * This method is required by the UserDetails interface and converts
     * the user's roles into Spring Security GrantedAuthority objects.
     *
     * @return Collection of GrantedAuthority objects representing the user's roles
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        Set<Role> roleList = new HashSet<>();
        roleList.add(this.mainRole);
        return authorities;
    }

    /**
     * Main role of the user
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Builder.Default
    private Role mainRole = new Role();

    /**
     * Constructs a new User with all required fields.
     *
     * @param id        The unique identifier for the user
     * @param firstName The user's first name
     * @param lastName  The user's last name
     * @param login     The user's unique login identifier
     * @param password  The user's hashed password
     * @param createdAt The timestamp when the user was created
     * @param updatedAt The timestamp when the user was last updated
     * @param deleted   The deletion status of the user
     * @param mainRole  The Main role assigned to the user
     */
    public User(long id,
            String firstName,
            String lastName,
            String login,
            String password,
            Date createdAt,
            Date updatedAt,
            boolean deleted,
            Role mainRole) {
        super();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.login = login;
        this.password = password;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.mainRole = mainRole;
    }

    /**
     * Returns the user's password.
     * Required by the UserDetails interface.
     *
     * @return The user's hashed password
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username used to authenticate the user.
     * Required by the UserDetails interface.
     *
     * @return The user's login identifier
     */
    @Override
    public String getUsername() {
        return login;
    }

    /**
     * Indicates whether the user's account has expired.
     * Required by the UserDetails interface.
     *
     * @return true if the account is valid (not expired), false otherwise
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked.
     * Required by the UserDetails interface.
     *
     * @return true if the account is not locked, false otherwise
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired.
     * Required by the UserDetails interface.
     *
     * @return true if the credentials are valid (not expired), false otherwise
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled.
     * Required by the UserDetails interface.
     *
     * @return true if the account is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Returns the Main role assigned to the user.
     * This method assumes the user has at least one role.
     *
     * @return The main role
     */
    public Role getMainRole() {
        return mainRole;
    }

    /**
     * Adds a new main role to the user.
     *
     * @param role The main role to set for the user
     */
    public void setMainRole(Role role) {
        mainRole = role;
    }

}
