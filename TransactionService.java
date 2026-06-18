package com.library.service;

import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * TransactionService - Core business logic for issuing and returning books.
 *
 * OOP Concepts demonstrated:
 * - Orchestration: coordinates between Book, Member, and Transaction objects
 * - Encapsulation: all issuance/return rules are hidden in this layer
 * - Exception handling: meaningful errors for invalid state transitions
 *
 * @author Avaneet Singh
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BookService bookService;
    private final MemberService memberService;

    /**
     * Issues a book to a member.
     *
     * Business rules:
     * 1. Book must have available copies
     * 2. Member must not already have the same book issued
     * 3. Due date is 14 days from issue date by default
     */
    public Transaction issueBook(Long bookId, Long memberId, LocalDate dueDate) {
        Book book = bookService.getBookById(bookId);
        Member member = memberService.getMemberById(memberId);

        // Rule 1: Book must be available
        if (!book.isAvailable()) {
            throw new IllegalStateException(
                "Book '" + book.getTitle() + "' has no available copies."
            );
        }

        // Rule 2: Member must not already have this book
        transactionRepository.findActiveTransaction(bookId, memberId).ifPresent(t -> {
            throw new IllegalStateException(
                "Member '" + member.getName() + "' already has '" + book.getTitle() + "' issued."
            );
        });

        // Decrement available copies (business logic in Book entity)
        book.issueBook();

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .book(book)
                .member(member)
                .issueDate(LocalDate.now())
                .dueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(14))
                .status(Transaction.Status.ISSUED)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Returns a previously issued book.
     *
     * Business rules:
     * 1. Active transaction must exist for this book + member pair
     * 2. Fine is calculated if returned after due date (₹2/day)
     * 3. Available copies are incremented
     */
    public Transaction returnBook(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (transaction.getStatus() != Transaction.Status.ISSUED &&
            transaction.getStatus() != Transaction.Status.OVERDUE) {
            throw new IllegalStateException("This book has already been returned.");
        }

        // Process return and calculate fine
        transaction.processReturn(LocalDate.now());

        // Increment available copies back
        transaction.getBook().returnBook();

        return transactionRepository.save(transaction);
    }

    // ---- READ ----

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findRecentTransactions();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getOverdueTransactions() {
        // Also update statuses to OVERDUE where applicable
        List<Transaction> overdue = transactionRepository.findOverdueTransactions(LocalDate.now());
        overdue.forEach(t -> t.checkAndMarkOverdue());
        return overdue;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByMember(Long memberId) {
        return transactionRepository.findByMemberId(memberId);
    }

    // ---- STATS ----

    @Transactional(readOnly = true)
    public long getCurrentlyIssuedCount() {
        return transactionRepository.countByStatus(Transaction.Status.ISSUED);
    }

    @Transactional(readOnly = true)
    public long getOverdueCount() {
        return transactionRepository.findOverdueTransactions(LocalDate.now()).size();
    }
}
