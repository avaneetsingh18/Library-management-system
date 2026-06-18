package com.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Transaction - Entity class representing a book issuance or return event.
 *
 * OOP Concepts demonstrated:
 * - Encapsulation: status transitions managed internally
 * - Polymorphism: Status enum provides type-safe state management
 * - Association: Many-to-one relationships to both Book and Member (FK links)
 *
 * Maps to the `transactions` table in MySQL.
 *
 * @author Avaneet Singh
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    /**
     * Status enum - encapsulates all valid states for a transaction.
     * This is polymorphism in action: each status has specific behaviour.
     */
    public enum Status {
        ISSUED,    // Book currently with the member
        RETURNED,  // Book returned (on time or late)
        OVERDUE    // Book not returned past due date
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many transactions can refer to the same book.
     * JoinColumn specifies the FK column name in the transactions table.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull(message = "Book must be specified")
    private Book book;

    /**
     * Many transactions can belong to the same member.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = false)
    @NotNull(message = "Member must be specified")
    private Member member;

    @Column(nullable = false)
    @NotNull(message = "Issue date cannot be null")
    private LocalDate issueDate;

    @Column(nullable = false)
    @NotNull(message = "Due date cannot be null")
    private LocalDate dueDate;

    /**
     * returnDate is null until the book is actually returned.
     */
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ISSUED;

    @Column(precision = 10, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Fine rate: ₹2 per day overdue
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("2.00");

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (issueDate == null) issueDate = LocalDate.now();
        if (dueDate == null) dueDate = issueDate.plusDays(14); // 2-week default
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Business logic methods (encapsulated behaviour) ----

    /**
     * Processes a book return.
     * Calculates fine if returned after due date.
     * Updates status and returnDate.
     *
     * @param returnDate the date the book is being returned
     */
    public void processReturn(LocalDate returnDate) {
        this.returnDate = returnDate;
        this.status = Status.RETURNED;

        // Calculate overdue fine
        if (returnDate.isAfter(dueDate)) {
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, returnDate);
            this.fineAmount = FINE_PER_DAY.multiply(new BigDecimal(daysOverdue));
        } else {
            this.fineAmount = BigDecimal.ZERO;
        }
    }

    /**
     * Checks if this transaction is overdue (not returned past due date).
     * Updates status to OVERDUE if applicable.
     */
    public boolean checkAndMarkOverdue() {
        if (status == Status.ISSUED && LocalDate.now().isAfter(dueDate)) {
            this.status = Status.OVERDUE;
            long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
            this.fineAmount = FINE_PER_DAY.multiply(new BigDecimal(daysOverdue));
            return true;
        }
        return false;
    }

    /**
     * Returns how many days remain until due date (negative if overdue).
     */
    public long getDaysUntilDue() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }
}
