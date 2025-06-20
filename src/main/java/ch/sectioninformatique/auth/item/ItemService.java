package ch.sectioninformatique.auth.item;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.sectioninformatique.auth.user.User;
import ch.sectioninformatique.auth.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Service class for managing items in the system.
 * This class handles all business logic related to items, including CRUD operations
 * and authorization checks. It acts as a bridge between the repository layer and
 * the controller layer.
 */
@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * Constructs a new ItemService with the required repositories.
     *
     * @param itemRepository Repository for item operations
     * @param userRepository Repository for user operations
     */
    public ItemService(ItemRepository itemRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Extracts the current user's email from the authentication principal.
     * This method is used to identify the currently authenticated user.
     *
     * @return The current user's email address
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("Full authentication principal: {}", authentication.getPrincipal());
        
        String currentUserEmail = authentication.getPrincipal().toString();
        // Extract only the login from the principal string
        currentUserEmail = currentUserEmail.substring(currentUserEmail.indexOf("login=") + 6);
        currentUserEmail = currentUserEmail.substring(0, currentUserEmail.indexOf(","));
        logger.debug("Extracted user email: {}", currentUserEmail);
        
        return currentUserEmail;
    }

    /**
     * Creates a new item in the system.
     * If the current user doesn't exist in the system, a new user record is created.
     * The item is associated with the current user as its author.
     *
     * @param newItem The item to create
     * @return The created item with its author set
     */
    public Item createItem(Item newItem) {
        String currentUserEmail = getCurrentUserEmail();
        
        if (!userRepository.existsByLogin(currentUserEmail)) {
            User newUser = new User();
            newUser.setLogin(currentUserEmail);
            newUser.setFirstName("Azure User");
            userRepository.save(newUser);
        }
        
        User author = userRepository.findByLogin(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        newItem.setAuthor(author);
        
        return itemRepository.save(newItem);
    }

    /**
     * Retrieves an item by its unique identifier.
     *
     * @param id The unique identifier of the item
     * @return An Optional containing the item if found, empty otherwise
     */
    public Optional<Item> getItem(final Long id) {
        return itemRepository.findById(id);
    }

    /**
     * Retrieves all items in the system.
     *
     * @return An Iterable containing all items
     */
    public Iterable<Item> getItems() {
        return itemRepository.findAll();
    }

    /**
     * Deletes an item from the system.
     * Only the item's author or users with admin privileges can delete an item.
     *
     * @param id The unique identifier of the item to delete
     * @throws ItemNotFoundException if the item is not found
     * @throws UnauthorizedItemException if the current user is not authorized to delete the item
     */
    public void deleteItem(final Long id) {
        String currentUserEmail = getCurrentUserEmail();
        
        User currentUser = userRepository.findByLogin(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException(id));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isSuperAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        
        if (!isAdmin && !isSuperAdmin) {
            if (item.getAuthor().getId() != currentUser.getId()) {
                throw new UnauthorizedItemException("delete");
            }
        }
        
        itemRepository.deleteById(id);
    }

    /**
     * Updates an existing item in the system.
     * Only the item's author or users with admin privileges can update an item.
     *
     * @param id The unique identifier of the item to update
     * @param newItem The new item data to apply
     * @return The updated item
     * @throws ItemNotFoundException if the item is not found
     * @throws UnauthorizedItemException if the current user is not authorized to update the item
     */
    public Item updateItem(Long id, Item newItem) {
        String currentUserEmail = getCurrentUserEmail();
        
        User currentUser = userRepository.findByLogin(currentUserEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        logger.debug("Found user with ID: {}", currentUser.getId());
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return itemRepository.findById(id)
            .map(item -> {
                boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
                boolean isSuperAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                logger.debug("User roles - isAdmin: {}, isSuperAdmin: {}", isAdmin, isSuperAdmin);

                if (!isAdmin && !isSuperAdmin) {
                    logger.debug("Checking authorization - Item author ID: {}, Current user ID: {}", 
                        item.getAuthor().getId(), currentUser.getId());
                    if (item.getAuthor().getId() != currentUser.getId()) {
                        logger.debug("Authorization failed - User is not the author of the item");
                        throw new UnauthorizedItemException("update");
                    }
                } 
                
                item.setName(newItem.getName());
                item.setDescription(newItem.getDescription());
                item.setAuthor(currentUser);
                logger.debug("Item updated successfully - ID: {}, New name: {}", id, newItem.getName());
                return itemRepository.save(item);
            })
            .orElseThrow(() -> new ItemNotFoundException(id));
    }
}
