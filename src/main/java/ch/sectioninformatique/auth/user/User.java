package ch.sectioninformatique.auth.user;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import org.hibernate.annotations.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import ch.sectioninformatique.auth.security.Role;

import org.springframework.security.core.GrantedAuthority;
import java.util.*;
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
     * Retrieves the authorities granted to the user.
     * This method is required by the UserDetails interface and converts
     * the user's roles into Spring Security GrantedAuthority objects.
     *
     * @return Collection of GrantedAuthority objects representing the user's roles
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        Set<Role> roleList = this.appSpecificRoles;
        roleList.add(this.mainRole);
        for (Role role : roleList) {
            authorities.addAll(role.getName().getGrantedAuthorities());
        }
        return authorities;
    }

    /**
     * Main role of the user
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @Builder.Default
    private Role mainRole = new Role();
    
    /**
     * Set of roles assigned to the user.
     * Uses eager fetching to ensure roles are always available.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @Builder.Default
    private Set<Role> appSpecificRoles = new HashSet<>();

    /**
     * Constructs a new User with all required fields.
     *
     * @param id The unique identifier for the user
     * @param firstName The user's first name
     * @param lastName The user's last name
     * @param login The user's unique login identifier
     * @param password The user's hashed password
     * @param createdAt The timestamp when the user was created
     * @param updatedAt The timestamp when the user was last updated
     * @param mainRole The Main role assigned to the user
     * @param appSpecificRoles The set of app specifique roles assigned to the user
     */
    public User(long id,
                String firstName, 
                String lastName, 
                String login,
                String password, 
                Date createdAt, 
                Date updatedAt,
                Role mainRole,
                Set<Role> appSpecificRoles) {
        super();
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.login = login;
        this.password = password;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.mainRole = mainRole;
        this.appSpecificRoles = appSpecificRoles;
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
     * Returns a list of app specific roles' names assigned to the user.
     *
     * @return The user's app specific role set
     */
    public List<String> getAppSpecificRolesString() {
        List<String> appSpecificRolesSrting = new ArrayList<String>();
        for(Role role : appSpecificRoles){
            appSpecificRolesSrting.add(role.getName().name());
        }
        return appSpecificRolesSrting;
    }
    
    /**
     * Returns all roles assigned to the user.
     *
     * @return The all roles assigned to the user
     */
    public Set<Role> getAllRoles() {
        Set<Role> allRoles = appSpecificRoles;
        allRoles.add(mainRole);
        return appSpecificRoles;
    }

    /**
     * Adds a new main role to the user.
     *
     * @param role The main role to set for the user
     */
    public void setMainRole(Role role) {
        mainRole = role;
    } 
    
    /**
     * Adds a new role to the user's set of app specific roles.
     *
     * @param role The app specific role to add to the user
     */
    public void addAppSpecificRoles(Role role) {
        appSpecificRoles.add(role);
    } 
}
