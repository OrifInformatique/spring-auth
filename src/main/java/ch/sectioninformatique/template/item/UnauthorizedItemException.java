package ch.sectioninformatique.template.item;

/**
 * Exception thrown when a user attempts to perform an operation on an item
 * that they are not authorized to access or modify.
 * This exception is used to enforce item ownership and access control.
 */
public class UnauthorizedItemException extends RuntimeException {
    
    /**
     * Constructs a new UnauthorizedItemException with a message indicating
     * that the user can only perform the specified operation on their own items.
     *
     * @param message The operation that was attempted (e.g., "update", "delete")
     */
    public UnauthorizedItemException(String message) {
        super("You can only " + message + " your own items");
    }
}
