package com.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Book - Entity class representing a book in the library catalogue.
 *
 * OOP Concepts demonstrated:
 * - Encapsulation: private fields with getters/setters (via Lombok)
 * - Abstraction: hides JPA/persistence details behind a clean POJO
 *
 * Maps to the `books` table in MySQL.
 *
 * @author Avaneet Singh
 */
@Entity
@Table(name = "books")
@Data                    // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor       // Lombok: default constructor (required by JPA)
@AllArgsConstructor      // Lombok: full constructor
@Builder                 // Lombok: builder pattern
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ISBN - International Standard Book Number.
     * Must be unique across the entire catalogue.
     */
    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "ISBN cannot be empty")
    @Pattern(regexp = "^(97[89])-?\\d{1,5}-?\\d{1,7}-?\\d{1,7}-?\\d$",
             message = "Invalid ISBN format (e.g. 978-0134685991)")
    private String isbn;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 255, message = "Title must be under 255 characters")
    private String title;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Author cannot be empty")
    private String author;

    @Column(length = 100)
    private String genre;

    /**
     * totalCopies - how many physical copies the library owns.
     * availableCopies - how many are currently on the shelf.
     * availableCopies <= totalCopies always.
     */
    @Column(nullable = false)
    @Min(value = 1, message = "Total copies must be at least 1")
    private int totalCopies = 1;

    @Column(nullable = false)
    @Min(value = 0, message = "Available copies cannot be negative")
    private int availableCopies = 1;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ---- Lifecycle hooks (OOP: behaviour embedded in the object) ----

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Business logic methods (encapsulated behaviour) ----

    /**
     * Returns true if at least one copy is available for issuance.
     */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /**
     * Decrements available copies when a book is issued.
     * Throws IllegalStateException if no copies are available.
     */
    public void issueBook() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No available copies of: " + title);
        }
        availableCopies--;
    }

    /**
     * Increments available copies when a book is returned.
     * Throws IllegalStateException if returning more than owned.
     */
    public void returnBook() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies already returned for: " + title);
        }
        availableCopies++;
    }
}
