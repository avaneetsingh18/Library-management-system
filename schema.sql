-- ============================================================
-- Library Management System - MySQL Schema
-- Author: Avaneet Singh
-- Description: Normalized relational schema for LMS
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- -------------------------------------------------------
-- Table: books
-- Stores book catalogue with availability status
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn        VARCHAR(20)  NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    author      VARCHAR(255) NOT NULL,
    genre       VARCHAR(100),
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Table: members
-- Stores registered library members
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS members (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    phone       VARCHAR(15),
    address     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Table: transactions
-- Tracks book issuance and returns (linked by IDs)
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id       BIGINT NOT NULL,
    member_id     BIGINT NOT NULL,
    issue_date    DATE NOT NULL,
    due_date      DATE NOT NULL,
    return_date   DATE,
    status        ENUM('ISSUED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ISSUED',
    fine_amount   DECIMAL(10, 2) DEFAULT 0.00,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id)   REFERENCES books(id)   ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE
);

-- -------------------------------------------------------
-- Indexes for faster querying
-- -------------------------------------------------------
CREATE INDEX idx_books_isbn    ON books(isbn);
CREATE INDEX idx_books_title   ON books(title);
CREATE INDEX idx_members_mid   ON members(member_id);
CREATE INDEX idx_trans_status  ON transactions(status);
CREATE INDEX idx_trans_book    ON transactions(book_id);
CREATE INDEX idx_trans_member  ON transactions(member_id);

-- -------------------------------------------------------
-- Sample seed data for testing
-- -------------------------------------------------------
INSERT IGNORE INTO books (isbn, title, author, genre, total_copies, available_copies) VALUES
('978-0134685991', 'Effective Java',              'Joshua Bloch',    'Technology', 3, 3),
('978-0596009205', 'Head First Design Patterns',  'Eric Freeman',    'Technology', 2, 2),
('978-0132350884', 'Clean Code',                  'Robert C. Martin','Technology', 2, 2),
('978-0143127741', 'The Alchemist',               'Paulo Coelho',    'Fiction',    4, 4),
('978-0061120084', 'To Kill a Mockingbird',       'Harper Lee',      'Fiction',    3, 3);

INSERT IGNORE INTO members (member_id, name, email, phone, address) VALUES
('MEM001', 'Avaneet Singh', 'avaneetsingh789@gmail.com', '7307818293', 'Kanpur, UP'),
('MEM002', 'Rahul Sharma',  'rahul.sharma@email.com',    '9876543210', 'Lucknow, UP'),
('MEM003', 'Priya Verma',   'priya.verma@email.com',     '8765432109', 'Kanpur, UP');
