package com.library.repository;

import com.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookRepository - Data Access Object (DAO) for Book entities.
 *
 * Extends JpaRepository which provides built-in CRUD operations:
 * save(), findById(), findAll(), deleteById(), count(), etc.
 *
 * Custom queries added for search and filter operations.
 *
 * OOP Concept: Interface-based abstraction — the service layer
 * only depends on this interface, not on any concrete implementation.
 * Spring Data JPA generates the implementation at runtime.
 *
 * @author Avaneet Singh
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Find a book by its ISBN (case-insensitive).
     */
    Optional<Book> findByIsbnIgnoreCase(String isbn);

    /**
     * Check if a book with a given ISBN already exists (for duplicate validation).
     */
    boolean existsByIsbnIgnoreCase(String isbn);

    /**
     * Search books by title or author (partial, case-insensitive match).
     * Used for the search bar in the frontend.
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Book> searchByTitleOrAuthor(@Param("keyword") String keyword);

    /**
     * Filter books by genre.
     */
    List<Book> findByGenreIgnoreCase(String genre);

    /**
     * Get all books that have at least one available copy.
     */
    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0")
    List<Book> findAvailableBooks();

    /**
     * Count total available copies across all books (for dashboard stats).
     */
    @Query("SELECT SUM(b.availableCopies) FROM Book b")
    Long countTotalAvailableCopies();
}
