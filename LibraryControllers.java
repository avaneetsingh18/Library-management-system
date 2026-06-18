package com.library.controller;

import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.service.BookService;
import com.library.service.MemberService;
import com.library.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// BookController
// ============================================================

/**
 * BookController - REST API endpoints for Book operations.
 * Base URL: /api/books
 *
 * @author Avaneet Singh
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(bookService.searchBooks(search));
        }
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @PostMapping
    public ResponseEntity<Book> addBook(@Valid @RequestBody Book book) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addBook(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id,
                                            @Valid @RequestBody Book book) {
        return ResponseEntity.ok(bookService.updateBook(id, book));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Book deleted successfully.");
        return ResponseEntity.ok(resp);
    }
}

// ============================================================
// MemberController
// ============================================================

/**
 * MemberController - REST API endpoints for Member operations.
 * Base URL: /api/members
 *
 * @author Avaneet Singh
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<List<Member>> getAllMembers(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(memberService.searchMembers(search));
        }
        return ResponseEntity.ok(memberService.getAllMembers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @PostMapping
    public ResponseEntity<Member> addMember(@Valid @RequestBody Member member) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.addMember(member));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> updateMember(@PathVariable Long id,
                                                @Valid @RequestBody Member member) {
        return ResponseEntity.ok(memberService.updateMember(id, member));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Member deleted successfully.");
        return ResponseEntity.ok(resp);
    }
}

// ============================================================
// TransactionController
// ============================================================

/**
 * TransactionController - REST API endpoints for issuance/return.
 * Base URL: /api/transactions
 *
 * @author Avaneet Singh
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
class TransactionController {

    private final TransactionService transactionService;
    private final BookService bookService;
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Transaction>> getOverdue() {
        return ResponseEntity.ok(transactionService.getOverdueTransactions());
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Transaction>> getMemberTransactions(@PathVariable Long memberId) {
        return ResponseEntity.ok(transactionService.getTransactionsByMember(memberId));
    }

    /**
     * Issue a book. Request body: { "bookId": 1, "memberId": 2, "dueDate": "2025-12-31" }
     */
    @PostMapping("/issue")
    public ResponseEntity<Transaction> issueBook(@RequestBody Map<String, Object> payload) {
        Long bookId   = Long.parseLong(payload.get("bookId").toString());
        Long memberId = Long.parseLong(payload.get("memberId").toString());
        LocalDate dueDate = payload.containsKey("dueDate") && payload.get("dueDate") != null
                ? LocalDate.parse(payload.get("dueDate").toString())
                : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.issueBook(bookId, memberId, dueDate));
    }

    /**
     * Return a book. URL: /api/transactions/{id}/return
     */
    @PutMapping("/{id}/return")
    public ResponseEntity<Transaction> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.returnBook(id));
    }

    /**
     * Dashboard stats endpoint - returns counts for summary cards.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks",     bookService.getTotalBookCount());
        stats.put("totalMembers",   memberService.getTotalMemberCount());
        stats.put("issuedBooks",    transactionService.getCurrentlyIssuedCount());
        stats.put("overdueBooks",   transactionService.getOverdueCount());
        return ResponseEntity.ok(stats);
    }
}

// ============================================================
// GlobalExceptionHandler - centralised error handling
// ============================================================

/**
 * GlobalExceptionHandler - Catches all exceptions and returns clean JSON error responses.
 * Demonstrates OOP: cross-cutting concern separated from business logic.
 *
 * @author Avaneet Singh
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
