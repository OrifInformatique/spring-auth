package ch.sectioninformatique.auth.item;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing items in the system.
 * This controller provides endpoints for CRUD operations on items,
 * with appropriate security checks and authorization requirements.
 * All responses are automatically converted to JSON format.
 */
@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private Environment environment;

    /**
     * Returns system information and environment variables.
     * This endpoint is used to verify that the application is running
     * and to display configuration information.
     *
     * @return A formatted string containing system information
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/")
    public String getHello() {
        return "<strong>Hello World !</strong><br>" +
               "<strong>JAVA_HOME : </strong>" + environment.getProperty("JAVA_HOME") + "<br>" +
               "<strong>Spring active profile : </strong>" + environment.getProperty("spring.profiles.active") + "<br>" +
               "<strong>Database used : </strong>" + environment.getProperty("spring.datasource.url");
    }

    /**
     * Retrieves all items in the system.
     * Requires the 'item:read' authority to access.
     *
     * @return An Iterable containing all items
     */
    @PreAuthorize("hasAuthority('item:read')")
    @GetMapping("/items")
    public Iterable<Item> getItems() {
        return itemService.getItems();
    }

    /**
     * Retrieves a specific item by its ID.
     * Requires the 'item:read' authority to access.
     *
     * @param id The unique identifier of the item to retrieve
     * @return The requested item
     * @throws ItemNotFoundException if the item is not found
     */
    @PreAuthorize("hasAuthority('item:read')")
    @GetMapping("/items/{id}")
    public Item getItemById(@PathVariable Long id) {
        return itemService.getItem(id)
            .orElseThrow(() -> new ItemNotFoundException(id));
    }
    
    /**
     * Creates a new item in the system.
     * Requires the 'item:write' authority to access.
     *
     * @param item The item data to create
     * @return The newly created item
     */
    @PreAuthorize("hasAuthority('item:write')")
    @PostMapping("/items")
    public Item createItem(@RequestBody Item item) {
        return itemService.createItem(item);
    }

    /**
     * Updates an existing item in the system.
     * Requires the 'item:update' authority to access.
     *
     * @param id The unique identifier of the item to update
     * @param item The updated item data
     * @return The updated item
     * @throws ItemNotFoundException if the item is not found
     * @throws UnauthorizedItemException if the user is not authorized to update the item
     */
    @PreAuthorize("hasAuthority('item:update')")
    @PutMapping("/items/{id}")
    public Item updateItem(@PathVariable Long id, @RequestBody Item item) {
        return itemService.updateItem(id, item);
    }

    /**
     * Deletes an item from the system.
     * Requires either the 'item:delete' authority or a combination of
     * 'item:write' authority and appropriate role (ROLE_USER or ROLE_ADMIN).
     *
     * @param id The unique identifier of the item to delete
     * @throws ItemNotFoundException if the item is not found
     * @throws UnauthorizedItemException if the user is not authorized to delete the item
     */
    @PreAuthorize("hasAuthority('item:delete') || ((hasRole('ROLE_USER') || hasRole('ROLE_ADMIN')) && hasAuthority('item:write'))")
    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
    }
}
