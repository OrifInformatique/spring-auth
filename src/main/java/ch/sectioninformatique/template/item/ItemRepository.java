package ch.sectioninformatique.template.item;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Item entities in the database.
 * This interface extends Spring's CrudRepository to provide basic CRUD operations
 * for the Item entity. It is automatically implemented by Spring Data JPA.
 */
@Repository
public interface ItemRepository extends CrudRepository<Item, Long> {

}
