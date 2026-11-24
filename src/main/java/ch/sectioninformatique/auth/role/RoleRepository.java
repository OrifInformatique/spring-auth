package ch.sectioninformatique.auth.role;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/**
 * Data access for {@link Role} entities.
 */
public interface RoleRepository extends CrudRepository<Role, Long> {

    /**
     * @param name role name
     * @return the role, or empty if it has not been seeded
     */
    Optional<Role> findByName(RoleEnum name);
}
