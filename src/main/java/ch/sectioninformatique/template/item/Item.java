package ch.sectioninformatique.template.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

import ch.sectioninformatique.template.user.User;

/**
 * Entity class representing an item in the system.
 * This class maps to the 'items' table in the database and contains information
 * about items including their name, description, author, and timestamps.
 */
@Data
@Table(name = "items")
@Entity
public class Item {

    /**
     * Unique identifier for the item.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the item.
     */
    private String name;

    /**
     * Detailed description of the item.
     * Maximum length is 1000 characters.
     */
    @Column(length=1000)
    private String description;

    /**
     * The user who created this item.
     * Uses eager fetching to ensure author information is always available.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private User author;

    /**
     * Timestamp when the item was created.
     * This field cannot be updated after creation.
     */
    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    /**
     * Timestamp when the item was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    /**
     * Default constructor for JPA.
     */
    public Item() {
    }
    
    /**
     * Creates a new item with the specified details.
     *
     * @param name The name of the item
     * @param description The description of the item
     * @param author The user who created the item
     */
    public Item(String name, String description, User author) {
        this.name = name;   
        this.description = description;
        this.author = author;
    }
}
