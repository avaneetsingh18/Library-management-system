package com.library.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Member - Entity class representing a registered library member.
 *
 * OOP Concepts demonstrated:
 * - Encapsulation: all fields private, accessible only via getters/setters
 * - Association: one Member can have many Transactions (one-to-many)
 *
 * Maps to the `members` table in MySQL.
 *
 * @author Avaneet Singh
 */
@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * memberId - human-readable unique ID (e.g., MEM001).
     * Different from the auto-generated primary key `id`.
     */
    @Column(name = "member_id", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Member ID cannot be empty")
    @Pattern(regexp = "^MEM\\d{3,6}$",
             message = "Member ID format must be MEM followed by 3-6 digits (e.g. MEM001)")
    private String memberId;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Column(length = 15)
    @Pattern(regexp = "^[6-9]\\d{9}$",
             message = "Phone must be a valid 10-digit Indian mobile number")
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    /**
     * Bidirectional relationship: one member -> many transactions.
     * mappedBy refers to the 'member' field in Transaction.
     * FetchType.LAZY avoids loading all transactions on every member fetch.
     */
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude   // prevents infinite loop in toString()
    @EqualsAndHashCode.Exclude
    private List<Transaction> transactions;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Business logic ----

    /**
     * Returns total number of books currently issued to this member.
     * Demonstrates polymorphic behaviour through stream operations.
     */
    public long getActiveIssuedCount() {
        if (transactions == null) return 0;
        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.Status.ISSUED)
                .count();
    }
}
