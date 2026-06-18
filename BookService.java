package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * BookService - Service layer for Book operations.
 *
 * OOP Concepts demonstrated:
 * - Single Responsibility Principle: this class ONLY handles Book business logic
 * - Dependency Injection: BookRepository is injected via constructor (Lombok @RequiredArgsConstructor)
 * - Encapsulation: business rules (duplicate check, availability) are hidden here,
 *   not exposed to the controller layer
 *
 * @author Avaneet Singh
 */
@Service
@RequiredArgsConstructor    // Lombok: injects BookRepository via constructor
@Transactional              // All methods run in a DB transaction by default
public class BookService {

    private final BookRepository bookRepository;

    // ---- CREATE ----

    /**
     * Adds a new book to the catalogue.
     * Validates that ISBN is unique before saving.
     */
    public Book addBook(Book book) {
        if (bookRepository.existsByIsbnIgnoreCase(book.getIsbn())) {
            throw new IllegalArgumentException("A book with ISBN '" + book.getIsbn() + "' already exists.");
        }
        // Ensure availableCopies matches totalCopies on first save
        book.setAvailableCopies(book.getTotalCopies());
        return bookRepository.save(book);
    }

    // ---- READ ----

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAllBooks();
        return bookRepository.searchByTitleOrAuthor(keyword.trim());
    }

    @Transactional(readOnly = true)
    public List<Book> getAvailableBooks() {
        return bookRepository.findAvailableBooks();
    }

    // ---- UPDATE ----

    /**
     * Updates book details.
     * ISBN change is allowed only if the new ISBN isn't already taken by another book.
     */
    public Book updateBook(Long id, Book updated) {
        Book existing = getBookById(id);

        // If ISBN changed, ensure no duplicate
        if (!existing.getIsbn().equalsIgnoreCase(updated.getIsbn()) &&
            bookRepository.existsByIsbnIgnoreCase(updated.getIsbn())) {
            throw new IllegalArgumentException("ISBN '" + updated.getIsbn() + "' is already in use.");
        }

        // Calculate the difference in copies to maintain availability correctly
        int diff = updated.getTotalCopies() - existing.getTotalCopies();
        int newAvailable = existing.getAvailableCopies() + diff;
        if (newAvailable < 0) {
            throw new IllegalStateException("Cannot reduce total copies below currently issued count.");
        }

        existing.setIsbn(updated.getIsbn());
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setGenre(updated.getGenre());
        existing.setTotalCopies(updated.getTotalCopies());
        existing.setAvailableCopies(newAvailable);

        return bookRepository.save(existing);
    }

    // ---- DELETE ----

    public void deleteBook(Long id) {
        Book book = getBookById(id);
        int issued = book.getTotalCopies() - book.getAvailableCopies();
        if (issued > 0) {
            throw new IllegalStateException(
                "Cannot delete book '" + book.getTitle() + "': " + issued + " copy(ies) currently issued."
            );
        }
        bookRepository.deleteById(id);
    }

    // ---- STATS (for dashboard) ----

    @Transactional(readOnly = true)
    public long getTotalBookCount() {
        return bookRepository.count();
    }
}
