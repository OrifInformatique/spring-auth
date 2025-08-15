package ch.sectioninformatique.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.sectioninformatique.auth.user.User;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing a role in the system.
 * This class maps to the 'roles' table in the database and defines the roles
 * available in the application. Each role has:
 * - A unique identifier
 * - A name defined by RoleEnum
 * - A human-readable description
 * - Creation and update timestamps
 * - A set of users assigned to this role
 * 
 * The class uses JPA annotations for persistence and Hibernate annotations
 * for automatic timestamp management. It implements a many-to-many relationship
 * with the User entity to manage role assignments.
 */
@Entity
@Table(name = "roles")
@Setter
@Getter
public class Role {
    /**
     * Unique identifier for the role.
     * This field is:
     * - Auto-generated using database identity
     * - Cannot be null
     * - Used as the primary key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private long id;

    /**
     * The name of the role, defined by the RoleEnum.
     * This field:
     * - Is unique across all roles
     * - Cannot be null
     * - Is stored as a string in the database
     * - Maps to predefined role types (USER, ADMIN, SUPER_ADMIN)
     */
    @Column(unique = true, nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleEnum name;

    /**
     * A human-readable description of the role.
     * This field:
     * - Cannot be null
     * - Provides context about the role's purpose
     * - Helps administrators understand role assignments
     */
    @Column(nullable = false)
    private String description;

    /**
     * Timestamp when the role was created.
     * This field:
     * - Is automatically set by Hibernate
     * - Cannot be updated after creation
     * - Helps track when roles were added to the system
     */
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    /**
     * Timestamp when the role was last updated.
     * This field:
     * - Is automatically updated by Hibernate
     * - Tracks when role details were last modified
     * - Helps with auditing and change tracking
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * Set of users who have this role.
     * This field:
     * - Implements a many-to-many relationship with User entity
     * - Is mapped by the 'roles' field in the User class
     * - Uses eager fetching to ensure roles are always available
     * - Is ignored during JSON serialization to prevent infinite recursion
     * - Is initialized as an empty HashSet to prevent null pointer exceptions
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.EAGER)
    @JsonIgnore
    private Set<User> users = new HashSet<>();
}

