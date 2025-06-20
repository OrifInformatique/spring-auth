package ch.sectioninformatique.auth.security;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity operations.
 * This interface:
 * - Extends CrudRepository to provide basic CRUD operations for Role entities
 * - Is annotated with @Repository to indicate it's a Spring Data repository
 * - Provides methods to interact with the roles table in the database
 * - Includes custom query methods for role-specific operations
 * 
 * The repository is used throughout the application to:
 * - Manage role assignments
 * - Retrieve role information
 * - Perform role-based queries
 * - Support role management operations
 */
@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {
    /**
     * Finds a role by its name.
     * This method:
     * - Searches for a role using its RoleEnum value
     * - Returns an Optional to handle cases where the role is not found
     * - Is used for role lookup and validation
     * - Supports case-sensitive role name matching
     *
     * @param name The RoleEnum value to search for
     * @return Optional containing the role if found, empty Optional otherwise
     */
    Optional<Role> findByName(RoleEnum name);
}
