/**
 * Library Management System - Frontend JavaScript
 * Author: Avaneet Singh
 *
 * Architecture mirrors Core Java OOP from the resume:
 *  - ES6 Classes (encapsulation, single responsibility)
 *  - API communication layer (replaces JDBC/MySQL calls in Java)
 *  - Controller pattern (mirrors Spring MVC)
 *
 * Class hierarchy:
 *  ApiService  → Handles all HTTP calls to the Spring Boot backend
 *  Book        → Book data model with validation
 *  Member      → Member data model with validation
 *  ToastManager→ Notification system
 *  ModalManager→ Modal open/close management
 *  LibraryApp  → Main controller (orchestrates everything)
 */

'use strict';

// ============================================================
// Configuration
// ============================================================
const CONFIG = {
    API_BASE: 'http://localhost:8080/api',
    DEBOUNCE_MS: 350,
};

// ============================================================
// ApiService — mirrors the Repository/DAO layer in Java
// ============================================================
class ApiService {

    /**
     * Generic fetch wrapper — handles JSON, errors, and status codes.
     * @param {string} endpoint - e.g. '/books'
     * @param {object} options  - fetch options (method, body, etc.)
     */
    static async request(endpoint, options = {}) {
        const url = CONFIG.API_BASE + endpoint;
        const defaults = {
            headers: { 'Content-Type': 'application/json' },
        };
        const config = { ...defaults, ...options };

        if (config.body && typeof config.body === 'object') {
            config.body = JSON.stringify(config.body);
        }

        try {
            const response = await fetch(url, config);
            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                // Server returned a business-logic error (e.g. 400/409)
                throw new Error(data.error || `HTTP ${response.status}`);
            }
            return data;
        } catch (err) {
            if (err.message.includes('Failed to fetch')) {
                throw new Error('Cannot connect to backend. Make sure Spring Boot is running on port 8080.');
            }
            throw err;
        }
    }

    // ---- Books ----
    static getBooks(search = '') {
        const q = search ? `?search=${encodeURIComponent(search)}` : '';
        return ApiService.request(`/books${q}`);
    }

    static addBook(book)          { return ApiService.request('/books', { method: 'POST', body: book }); }
    static updateBook(id, book)   { return ApiService.request(`/books/${id}`, { method: 'PUT', body: book }); }
    static deleteBook(id)         { return ApiService.request(`/books/${id}`, { method: 'DELETE' }); }

    // ---- Members ----
    static getMembers(search = '') {
        const q = search ? `?search=${encodeURIComponent(search)}` : '';
        return ApiService.request(`/members${q}`);
    }

    static addMember(member)        { return ApiService.request('/members', { method: 'POST', body: member }); }
    static updateMember(id, member) { return ApiService.request(`/members/${id}`, { method: 'PUT', body: member }); }
    static deleteMember(id)         { return ApiService.request(`/members/${id}`, { method: 'DELETE' }); }

    // ---- Transactions ----
    static getTransactions()        { return ApiService.request('/transactions'); }
    static issueBook(payload)       { return ApiService.request('/transactions/issue', { method: 'POST', body: payload }); }
    static returnBook(id)           { return ApiService.request(`/transactions/${id}/return`, { method: 'PUT' }); }
    static getStats()               { return ApiService.request('/transactions/stats'); }
}

// ============================================================
// Book — Data model + client-side validation (OOP: encapsulation)
// ============================================================
class Book {
    constructor({ id, isbn, title, author, genre, totalCopies, availableCopies }) {
        this.id              = id || null;
        this.isbn            = isbn?.trim() || '';
        this.title           = title?.trim() || '';
        this.author          = author?.trim() || '';
        this.genre           = genre?.trim() || '';
        this.totalCopies     = parseInt(totalCopies) || 1;
        this.availableCopies = parseInt(availableCopies) ?? this.totalCopies;
    }

    /**
     * Validates all fields — mirrors Spring @Valid constraints.
     * Returns an errors object { fieldName: message } or null if valid.
     */
    validate() {
        const errors = {};
        const isbnPattern = /^(97[89])-?\d{1,5}-?\d{1,7}-?\d{1,7}-?\d$/;

        if (!this.isbn)               errors.isbn = 'ISBN is required.';
        else if (!isbnPattern.test(this.isbn)) errors.isbn = 'Invalid ISBN format (e.g. 978-0134685991)';

        if (!this.title)  errors.title  = 'Title is required.';
        if (!this.author) errors.author = 'Author is required.';
        if (this.totalCopies < 1) errors.copies = 'Copies must be at least 1.';

        return Object.keys(errors).length ? errors : null;
    }

    /** Returns whether any copies are available. */
    get isAvailable() { return this.availableCopies > 0; }
}

// ============================================================
// Member — Data model + client-side validation
// ============================================================
class Member {
    constructor({ id, memberId, name, email, phone, address }) {
        this.id       = id || null;
        this.memberId = memberId?.trim() || '';
        this.name     = name?.trim() || '';
        this.email    = email?.trim() || '';
        this.phone    = phone?.trim() || '';
        this.address  = address?.trim() || '';
    }

    validate() {
        const errors = {};
        const midPattern   = /^MEM\d{3,6}$/;
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        const phonePattern = /^[6-9]\d{9}$/;

        if (!this.memberId)              errors.mid   = 'Member ID is required.';
        else if (!midPattern.test(this.memberId)) errors.mid = 'Format must be MEM followed by 3-6 digits (e.g. MEM001)';

        if (!this.name)                  errors.name  = 'Name is required.';
        if (!this.email)                 errors.email = 'Email is required.';
        else if (!emailPattern.test(this.email)) errors.email = 'Invalid email address.';

        if (this.phone && !phonePattern.test(this.phone))
            errors.phone = 'Must be a valid 10-digit Indian mobile number.';

        return Object.keys(errors).length ? errors : null;
    }
}

// ============================================================
// ToastManager — notification system
// ============================================================
class ToastManager {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
    }

    show(message, type = 'info', duration = 3500) {
        const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info' };
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<i class="fa-solid ${icons[type] || icons.info}"></i><span>${message}</span>`;
        this.container.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('hide');
            toast.addEventListener('animationend', () => toast.remove());
        }, duration);
    }

    success(msg) { this.show(msg, 'success'); }
    error(msg)   { this.show(msg, 'error', 5000); }
    info(msg)    { this.show(msg, 'info'); }
}

// ============================================================
// ModalManager — open/close modal overlays
// ============================================================
class ModalManager {
    static open(id)  { document.getElementById(id)?.classList.add('open'); }
    static close(id) { document.getElementById(id)?.classList.remove('open'); }

    static closeAll() {
        document.querySelectorAll('.modal-overlay.open')
                .forEach(m => m.classList.remove('open'));
    }
}

// ============================================================
// LibraryApp — Main application controller
// Orchestrates all modules (mirrors Spring MVC controllers)
// ============================================================
class LibraryApp {
    constructor() {
        this.toast        = new ToastManager('toastContainer');
        this.currentPage  = 'dashboard';
        this.allBooks     = [];
        this.allMembers   = [];
        this.allTxns      = [];
        this.txnFilter    = 'ALL';
        this.editingBookId   = null;
        this.editingMemberId = null;
        this.pendingDeleteFn = null;

        this._initUI();
        this._checkApiConnection();
        this._loadDashboard();
    }

    // ---- UI Wiring ----

    _initUI() {
        // Sidebar navigation
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', e => {
                e.preventDefault();
                this.navigate(item.dataset.page);
            });
        });

        // Hamburger menu (mobile)
        document.getElementById('hamburger').addEventListener('click', () => this._openSidebar());
        document.getElementById('sidebarClose').addEventListener('click', () => this._closeSidebar());
        document.getElementById('sidebarOverlay').addEventListener('click', () => this._closeSidebar());

        // Modal close buttons
        document.querySelectorAll('.modal-close, [data-modal]').forEach(btn => {
            btn.addEventListener('click', () => {
                const modalId = btn.dataset.modal || btn.closest('.modal-overlay')?.id;
                if (modalId) ModalManager.close(modalId);
            });
        });

        // Close modal on overlay click
        document.querySelectorAll('.modal-overlay').forEach(overlay => {
            overlay.addEventListener('click', e => {
                if (e.target === overlay) ModalManager.close(overlay.id);
            });
        });

        // ---- Book events ----
        document.getElementById('addBookBtn').addEventListener('click', () => this._openBookModal());
        document.getElementById('saveBookBtn').addEventListener('click', () => this._saveBook());
        document.getElementById('bookSearch').addEventListener('input',
            this._debounce(e => this._loadBooks(e.target.value), CONFIG.DEBOUNCE_MS));

        // ---- Member events ----
        document.getElementById('addMemberBtn').addEventListener('click', () => this._openMemberModal());
        document.getElementById('saveMemberBtn').addEventListener('click', () => this._saveMember());
        document.getElementById('memberSearch').addEventListener('input',
            this._debounce(e => this._loadMembers(e.target.value), CONFIG.DEBOUNCE_MS));

        // ---- Transaction events ----
        document.getElementById('issueBookBtn').addEventListener('click', () => this._openIssueModal());
        document.getElementById('confirmIssueBtn').addEventListener('click', () => this._confirmIssue());

        // Filter tabs
        document.querySelectorAll('.filter-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                document.querySelectorAll('.filter-tab').forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                this.txnFilter = tab.dataset.filter;
                this._renderTransactions();
            });
        });

        // Confirm delete
        document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
            if (this.pendingDeleteFn) {
                this.pendingDeleteFn();
                this.pendingDeleteFn = null;
            }
            ModalManager.close('confirmModal');
        });

        // Default due date: 14 days from today
        const dueDateEl = document.getElementById('issueDueDate');
        const twoWeeks = new Date();
        twoWeeks.setDate(twoWeeks.getDate() + 14);
        dueDateEl.value = twoWeeks.toISOString().split('T')[0];
        dueDateEl.min   = new Date().toISOString().split('T')[0];
    }

    // ---- Navigation ----

    navigate(page) {
        this.currentPage = page;

        // Update sidebar active state
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.toggle('active', item.dataset.page === page);
        });

        // Show the correct page
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.getElementById(`page-${page}`)?.classList.add('active');

        // Update topbar title
        const titles = { dashboard: 'Dashboard', books: 'Book Catalogue', members: 'Members', transactions: 'Transactions' };
        document.getElementById('pageTitle').textContent = titles[page] || page;

        // Load data for the page
        const loaders = {
            dashboard:    () => this._loadDashboard(),
            books:        () => this._loadBooks(),
            members:      () => this._loadMembers(),
            transactions: () => this._loadTransactions(),
        };
        loaders[page]?.();

        this._closeSidebar();
    }

    // ---- API Connection Check ----

    async _checkApiConnection() {
        const dot  = document.querySelector('.status-dot');
        const text = document.querySelector('.status-text');
        try {
            await ApiService.getStats();
            dot.className  = 'status-dot online';
            text.textContent = 'Backend Online';
        } catch {
            dot.className  = 'status-dot offline';
            text.textContent = 'Backend Offline';
            this.toast.error('Cannot reach Spring Boot backend. Start it with: mvn spring-boot:run');
        }
    }

    // ---- Dashboard ----

    async _loadDashboard() {
        try {
            const [stats, txns] = await Promise.all([
                ApiService.getStats(),
                ApiService.getTransactions(),
            ]);
            document.getElementById('stat-totalBooks').textContent   = stats.totalBooks   ?? 0;
            document.getElementById('stat-totalMembers').textContent = stats.totalMembers ?? 0;
            document.getElementById('stat-issuedBooks').textContent  = stats.issuedBooks  ?? 0;
            document.getElementById('stat-overdueBooks').textContent = stats.overdueBooks ?? 0;

            this._renderDashboardTxns(txns.slice(0, 8));
        } catch (err) {
            this._renderDashboardTxns([]);
        }
    }

    _renderDashboardTxns(txns) {
        const tbody = document.getElementById('dashboardTransactions');
        if (!txns.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No recent transactions found.</td></tr>`;
            return;
        }
        tbody.innerHTML = txns.map(t => `
            <tr>
                <td>${this._esc(t.book?.title ?? 'N/A')}</td>
                <td>${this._esc(t.member?.name ?? 'N/A')}</td>
                <td>${this._fmtDate(t.issueDate)}</td>
                <td>${this._fmtDate(t.dueDate)}</td>
                <td>${this._statusBadge(t.status)}</td>
            </tr>`).join('');
    }

    // ---- Books ----

    async _loadBooks(search = '') {
        const tbody = document.getElementById('booksTable');
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">Loading...</td></tr>`;
        try {
            this.allBooks = await ApiService.getBooks(search);
            this._renderBooks();
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="7" class="empty-state" style="color:var(--color-danger)">${err.message}</td></tr>`;
        }
    }

    _renderBooks() {
        const tbody = document.getElementById('booksTable');
        if (!this.allBooks.length) {
            tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No books found. Add your first book!</td></tr>`;
            return;
        }
        tbody.innerHTML = this.allBooks.map(b => `
            <tr>
                <td><code style="font-size:.8rem">${this._esc(b.isbn)}</code></td>
                <td><strong>${this._esc(b.title)}</strong></td>
                <td>${this._esc(b.author)}</td>
                <td>${b.genre ? `<span class="badge" style="background:var(--color-bg);color:var(--color-text-muted)">${this._esc(b.genre)}</span>` : '—'}</td>
                <td>${b.totalCopies}</td>
                <td>
                    <span class="badge ${b.availableCopies > 0 ? 'badge-available' : 'badge-unavailable'}">
                        ${b.availableCopies} / ${b.totalCopies}
                    </span>
                </td>
                <td>
                    <button class="action-btn edit" onclick="app.editBook(${b.id})" title="Edit">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="action-btn delete" onclick="app.confirmDeleteBook(${b.id}, '${this._esc(b.title)}')" title="Delete">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>`).join('');
    }

    _openBookModal(book = null) {
        this.editingBookId = book ? book.id : null;
        document.getElementById('bookModalTitle').textContent = book ? 'Edit Book' : 'Add New Book';
        document.getElementById('bookId').value     = book?.id || '';
        document.getElementById('bookIsbn').value   = book?.isbn || '';
        document.getElementById('bookTitle').value  = book?.title || '';
        document.getElementById('bookAuthor').value = book?.author || '';
        document.getElementById('bookGenre').value  = book?.genre || '';
        document.getElementById('bookCopies').value = book?.totalCopies || 1;
        this._clearFormErrors(['isbn', 'title', 'author']);
        ModalManager.open('bookModal');
        document.getElementById('bookIsbn').focus();
    }

    editBook(id) {
        const book = this.allBooks.find(b => b.id === id);
        if (book) this._openBookModal(book);
    }

    async _saveBook() {
        const book = new Book({
            isbn:         document.getElementById('bookIsbn').value,
            title:        document.getElementById('bookTitle').value,
            author:       document.getElementById('bookAuthor').value,
            genre:        document.getElementById('bookGenre').value,
            totalCopies:  document.getElementById('bookCopies').value,
        });

        const errors = book.validate();
        if (errors) {
            this._showFormErrors({ isbn: 'err-isbn', title: 'err-title', author: 'err-author' }, errors);
            return;
        }

        const btn = document.getElementById('saveBookBtn');
        btn.disabled = true;
        btn.textContent = 'Saving...';

        try {
            if (this.editingBookId) {
                await ApiService.updateBook(this.editingBookId, book);
                this.toast.success('Book updated successfully!');
            } else {
                await ApiService.addBook(book);
                this.toast.success('Book added to catalogue!');
            }
            ModalManager.close('bookModal');
            this._loadBooks();
            this._loadDashboard();
        } catch (err) {
            this.toast.error(err.message);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Save Book';
        }
    }

    confirmDeleteBook(id, title) {
        document.getElementById('confirmMessage').textContent =
            `Delete "${title}"? This cannot be undone.`;
        this.pendingDeleteFn = async () => {
            try {
                await ApiService.deleteBook(id);
                this.toast.success('Book deleted.');
                this._loadBooks();
                this._loadDashboard();
            } catch (err) {
                this.toast.error(err.message);
            }
        };
        ModalManager.open('confirmModal');
    }

    // ---- Members ----

    async _loadMembers(search = '') {
        const tbody = document.getElementById('membersTable');
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">Loading...</td></tr>`;
        try {
            this.allMembers = await ApiService.getMembers(search);
            this._renderMembers();
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state" style="color:var(--color-danger)">${err.message}</td></tr>`;
        }
    }

    _renderMembers() {
        const tbody = document.getElementById('membersTable');
        if (!this.allMembers.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No members registered yet.</td></tr>`;
            return;
        }
        tbody.innerHTML = this.allMembers.map(m => `
            <tr>
                <td><code style="font-size:.8rem;color:var(--color-primary)">${this._esc(m.memberId)}</code></td>
                <td><strong>${this._esc(m.name)}</strong></td>
                <td>${this._esc(m.email)}</td>
                <td>${m.phone || '—'}</td>
                <td>
                    <button class="action-btn edit" onclick="app.editMember(${m.id})" title="Edit">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                    <button class="action-btn delete" onclick="app.confirmDeleteMember(${m.id}, '${this._esc(m.name)}')" title="Delete">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </td>
            </tr>`).join('');
    }

    _openMemberModal(member = null) {
        this.editingMemberId = member ? member.id : null;
        document.getElementById('memberModalTitle').textContent = member ? 'Edit Member' : 'Add New Member';
        document.getElementById('memberId').value      = member?.id || '';
        document.getElementById('memberMID').value     = member?.memberId || '';
        document.getElementById('memberName').value    = member?.name || '';
        document.getElementById('memberEmail').value   = member?.email || '';
        document.getElementById('memberPhone').value   = member?.phone || '';
        document.getElementById('memberAddress').value = member?.address || '';

        // Disable member ID field when editing
        document.getElementById('memberMID').disabled = !!member;

        this._clearFormErrors(['mid', 'name', 'email', 'phone']);
        ModalManager.open('memberModal');
        document.getElementById('memberName').focus();
    }

    editMember(id) {
        const member = this.allMembers.find(m => m.id === id);
        if (member) this._openMemberModal(member);
    }

    async _saveMember() {
        const member = new Member({
            memberId: document.getElementById('memberMID').value,
            name:     document.getElementById('memberName').value,
            email:    document.getElementById('memberEmail').value,
            phone:    document.getElementById('memberPhone').value,
            address:  document.getElementById('memberAddress').value,
        });

        const errors = member.validate();
        if (errors) {
            this._showFormErrors({ mid: 'err-mid', name: 'err-name', email: 'err-email', phone: 'err-phone' }, errors);
            return;
        }

        const btn = document.getElementById('saveMemberBtn');
        btn.disabled = true;
        btn.textContent = 'Saving...';

        try {
            if (this.editingMemberId) {
                await ApiService.updateMember(this.editingMemberId, member);
                this.toast.success('Member updated!');
            } else {
                await ApiService.addMember(member);
                this.toast.success('Member registered!');
            }
            ModalManager.close('memberModal');
            this._loadMembers();
            this._loadDashboard();
        } catch (err) {
            this.toast.error(err.message);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Save Member';
        }
    }

    confirmDeleteMember(id, name) {
        document.getElementById('confirmMessage').textContent =
            `Remove member "${name}"? All their transaction history will also be deleted.`;
        this.pendingDeleteFn = async () => {
            try {
                await ApiService.deleteMember(id);
                this.toast.success('Member removed.');
                this._loadMembers();
                this._loadDashboard();
            } catch (err) {
                this.toast.error(err.message);
            }
        };
        ModalManager.open('confirmModal');
    }

    // ---- Transactions ----

    async _loadTransactions() {
        const tbody = document.getElementById('transactionsTable');
        tbody.innerHTML = `<tr><td colspan="9" class="empty-state">Loading...</td></tr>`;
        try {
            this.allTxns = await ApiService.getTransactions();
            this._renderTransactions();
        } catch (err) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty-state" style="color:var(--color-danger)">${err.message}</td></tr>`;
        }
    }

    _renderTransactions() {
        const tbody = document.getElementById('transactionsTable');
        const filtered = this.txnFilter === 'ALL'
            ? this.allTxns
            : this.allTxns.filter(t => t.status === this.txnFilter);

        if (!filtered.length) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty-state">No transactions found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filtered.map(t => `
            <tr>
                <td style="color:var(--color-text-muted);font-size:.8rem">#${t.id}</td>
                <td><strong>${this._esc(t.book?.title ?? 'N/A')}</strong></td>
                <td>${this._esc(t.member?.name ?? 'N/A')}</td>
                <td>${this._fmtDate(t.issueDate)}</td>
                <td>${this._fmtDate(t.dueDate)}</td>
                <td>${t.returnDate ? this._fmtDate(t.returnDate) : '—'}</td>
                <td>${t.fineAmount > 0 ? `<span style="color:var(--color-danger);font-weight:600">₹${t.fineAmount}</span>` : '₹0'}</td>
                <td>${this._statusBadge(t.status)}</td>
                <td>
                    ${(t.status === 'ISSUED' || t.status === 'OVERDUE')
                        ? `<button class="action-btn return" onclick="app.returnBook(${t.id})" title="Return Book">
                               <i class="fa-solid fa-rotate-left"></i> Return
                           </button>`
                        : '<span style="color:var(--color-text-muted);font-size:.8rem">Completed</span>'
                    }
                </td>
            </tr>`).join('');
    }

    async _openIssueModal() {
        // Load fresh book/member lists for the dropdowns
        try {
            const [books, members] = await Promise.all([
                ApiService.getBooks(),
                ApiService.getMembers(),
            ]);
            const bookSel = document.getElementById('issueBookId');
            const memSel  = document.getElementById('issueMemberId');

            const available = books.filter(b => b.availableCopies > 0);
            bookSel.innerHTML = `<option value="">— Select a book —</option>`
                + available.map(b => `<option value="${b.id}">${this._esc(b.title)} (${b.availableCopies} left)</option>`).join('');

            memSel.innerHTML = `<option value="">— Select a member —</option>`
                + members.map(m => `<option value="${m.id}">${this._esc(m.name)} (${this._esc(m.memberId)})</option>`).join('');

            ModalManager.open('issueModal');
        } catch (err) {
            this.toast.error('Failed to load data: ' + err.message);
        }
    }

    async _confirmIssue() {
        const bookId   = document.getElementById('issueBookId').value;
        const memberId = document.getElementById('issueMemberId').value;
        const dueDate  = document.getElementById('issueDueDate').value;

        if (!bookId || !memberId || !dueDate) {
            this.toast.error('Please select a book, member, and due date.');
            return;
        }

        const btn = document.getElementById('confirmIssueBtn');
        btn.disabled = true;

        try {
            await ApiService.issueBook({ bookId, memberId, dueDate });
            this.toast.success('Book issued successfully!');
            ModalManager.close('issueModal');
            this._loadTransactions();
            this._loadDashboard();
        } catch (err) {
            this.toast.error(err.message);
        } finally {
            btn.disabled = false;
        }
    }

    async returnBook(transactionId) {
        try {
            const t = await ApiService.returnBook(transactionId);
            const fine = parseFloat(t.fineAmount) || 0;
            const msg = fine > 0
                ? `Book returned. Overdue fine: ₹${fine}.`
                : 'Book returned on time. No fine!';
            this.toast.success(msg);
            this._loadTransactions();
            this._loadDashboard();
        } catch (err) {
            this.toast.error(err.message);
        }
    }

    // ---- Utility Methods ----

    /** Sidebar helpers */
    _openSidebar() {
        document.getElementById('sidebar').classList.add('open');
        document.getElementById('sidebarOverlay').classList.add('open');
    }

    _closeSidebar() {
        document.getElementById('sidebar').classList.remove('open');
        document.getElementById('sidebarOverlay').classList.remove('open');
    }

    /** Debounce utility for search inputs */
    _debounce(fn, delay) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        };
    }

    /** Escape HTML to prevent XSS */
    _esc(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /** Format date arrays from Java's LocalDate serialization */
    _fmtDate(d) {
        if (!d) return '—';
        // Java LocalDate serialises as [year, month, day] array
        if (Array.isArray(d)) {
            const [y, m, day] = d;
            return `${String(day).padStart(2,'0')}/${String(m).padStart(2,'0')}/${y}`;
        }
        // ISO string
        const parts = String(d).split('-');
        if (parts.length === 3) return `${parts[2]}/${parts[1]}/${parts[0]}`;
        return d;
    }

    /** Status badge HTML */
    _statusBadge(status) {
        const classes = { ISSUED: 'badge-issued', RETURNED: 'badge-returned', OVERDUE: 'badge-overdue' };
        const icons   = { ISSUED: 'fa-bookmark', RETURNED: 'fa-check', OVERDUE: 'fa-exclamation' };
        return `<span class="badge ${classes[status] || ''}">
                    <i class="fa-solid ${icons[status] || ''}"></i> ${status}
                </span>`;
    }

    /** Show field-level validation errors */
    _showFormErrors(fieldToErrId, errors) {
        Object.entries(fieldToErrId).forEach(([field, errId]) => {
            document.getElementById(errId).textContent = errors[field] || '';
        });
    }

    /** Clear validation errors */
    _clearFormErrors(fields) {
        fields.forEach(f => {
            const el = document.getElementById(`err-${f}`);
            if (el) el.textContent = '';
        });
    }
}

// ============================================================
// Bootstrap
// ============================================================
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new LibraryApp();
});
