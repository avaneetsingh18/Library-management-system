package com.library.repository;

import com.library.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TransactionRepository - DAO for Transaction entities.
 *
 * @author Avaneet Singh
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** All transactions for a specific member. */
    List<Transaction> findByMemberId(Long memberId);

    /** All transactions for a specific book. */
    List<Transaction> findByBookId(Long bookId);

    /** All currently active (ISSUED) transactions. */
    List<Transaction> findByStatus(Transaction.Status status);

    /** Count currently issued books (for dashboard). */
    long countByStatus(Transaction.Status status);

    /**
     * Find an active (ISSUED) transaction for a specific book + member combo.
     * Used to validate re-issue and to find the right record for return.
     */
    @Query("SELECT t FROM Transaction t WHERE t.book.id = :bookId " +
           "AND t.member.id = :memberId AND t.status = 'ISSUED'")
    Optional<Transaction> findActiveTransaction(@Param("bookId") Long bookId,
                                                 @Param("memberId") Long memberId);

    /**
     * Find all overdue transactions (ISSUED status, past due date).
     * Used for the overdue alert section in the dashboard.
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'ISSUED' AND t.dueDate < :today")
    List<Transaction> findOverdueTransactions(@Param("today") LocalDate today);

    /**
     * Recent transactions for dashboard activity feed.
     */
    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactions();
}
