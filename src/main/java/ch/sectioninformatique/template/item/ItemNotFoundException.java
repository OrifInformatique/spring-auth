package ch.sectioninformatique.template.item;

/**
 * Exception thrown when an item with the specified ID cannot be found in the system.
 * This exception is used to indicate that a requested item does not exist
 * in the database or is not accessible.
 */
public class ItemNotFoundException extends RuntimeException {
  
    /**
     * Constructs a new ItemNotFoundException with a message indicating
     * that the item with the specified ID could not be found.
     *
     * @param id The ID of the item that was not found
     */
    public ItemNotFoundException(Long id) {
        super("Could not find item " + id);
    }
}
