# 📚 Library Management System
**Author: Avaneet Singh | B.Tech CSE 2027 | KIT, Kanpur**

A full-stack Library Management System built to demonstrate:
- **Core Java OOP** (Encapsulation, Inheritance, Polymorphism)
- **Spring Boot** REST API backend
- **MySQL** normalized relational database
- **Vanilla JS ES6 Classes** frontend (HTML5 + CSS3)

---

## 🏗️ Architecture

```
library-management/
├── backend/                         ← Spring Boot (Java 17)
│   ├── pom.xml
│   └── src/main/java/com/library/
│       ├── LibraryManagementApplication.java   ← Entry point
│       ├── model/
│       │   ├── Book.java            ← OOP: Encapsulation + business logic
│       │   ├── Member.java          ← OOP: One-to-Many relationship
│       │   └── Transaction.java     ← OOP: Polymorphism (Status enum)
│       ├── repository/
│       │   ├── BookRepository.java  ← DAO pattern (Spring Data JPA)
│       │   ├── MemberRepository.java
│       │   └── TransactionRepository.java
│       ├── service/
│       │   ├── BookService.java     ← Business logic layer
│       │   ├── MemberService.java
│       │   └── TransactionService.java
│       ├── controller/
│       │   └── LibraryControllers.java  ← REST API + Exception Handler
│       └── resources/
│           ├── application.properties   ← DB config
│           └── schema.sql               ← MySQL schema + seed data
│
└── frontend/                        ← Vanilla HTML/CSS/JS
    ├── index.html                   ← Single-page app
    ├── style.css                    ← Responsive design (mobile-first)
    └── script.js                    ← ES6 Classes (OOP in JS)
```

---

## ⚙️ Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0+

---

### Step 1 — Set up MySQL

```sql
-- Run this in MySQL Workbench or terminal:
mysql -u root -p < backend/src/main/resources/schema.sql
```

This creates the `library_db` database with sample data.

---

### Step 2 — Configure database credentials

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=Asia/Kolkata
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD   ← Change this
```

---

### Step 3 — Start the Spring Boot backend

```bash
cd backend
mvn spring-boot:run
```

You should see:
```
  Library Management System Started!
  Backend: http://localhost:8080
```

**Test the API:**
```bash
curl http://localhost:8080/api/books
curl http://localhost:8080/api/transactions/stats
```

---

### Step 4 — Open the frontend

Simply open `frontend/index.html` in your browser:

```bash
# macOS / Linux
open frontend/index.html

# Windows
start frontend/index.html
```

The app auto-connects to `http://localhost:8080`. The status indicator (top-right) shows **Backend Online** when connected.

---

## 🔌 REST API Endpoints

| Method | Endpoint                        | Description                  |
|--------|---------------------------------|------------------------------|
| GET    | /api/books                      | Get all books (+ search)     |
| POST   | /api/books                      | Add a book                   |
| PUT    | /api/books/{id}                 | Update a book                |
| DELETE | /api/books/{id}                 | Delete a book                |
| GET    | /api/members                    | Get all members (+ search)   |
| POST   | /api/members                    | Register a member            |
| PUT    | /api/members/{id}               | Update a member              |
| DELETE | /api/members/{id}               | Remove a member              |
| GET    | /api/transactions               | All transactions             |
| POST   | /api/transactions/issue         | Issue a book                 |
| PUT    | /api/transactions/{id}/return   | Return a book                |
| GET    | /api/transactions/stats         | Dashboard stats              |
| GET    | /api/transactions/overdue       | Overdue transactions         |

---

## 💡 OOP Concepts Used

| Concept         | Java Example                                        | JS Mirror                          |
|-----------------|-----------------------------------------------------|------------------------------------|
| Encapsulation   | `Book.java` — private fields + `issueBook()` method | `Book` class + `validate()` method |
| Inheritance     | Spring's `JpaRepository` extends `CrudRepository`  | `LibraryApp` uses `ApiService`     |
| Polymorphism    | `Transaction.Status` enum (ISSUED/RETURNED/OVERDUE) | `_statusBadge()` method            |
| Abstraction     | `BookRepository` interface (Spring generates impl)  | `ApiService` hides fetch details   |
| SRP             | Separate Service classes per entity                 | Separate class per responsibility  |

---

## 🎨 Tech Stack

| Layer      | Technology                       |
|------------|----------------------------------|
| Frontend   | HTML5, CSS3, Vanilla JS (ES6)    |
| Backend    | Java 17, Spring Boot 3.2         |
| ORM        | Spring Data JPA + Hibernate      |
| Database   | MySQL 8.0                        |
| Icons      | Font Awesome 6 (CDN)             |
| Fonts      | Inter + Space Grotesk (Google)   |

---

*Built as a portfolio project to demonstrate full-stack Java development skills.*
