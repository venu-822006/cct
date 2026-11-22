# COLLEGE PROJECT PRESENTATION SLIDE OUTLINE
## Personal Cloud Drive: A Secure Cloud Storage Platform using Spring Boot & React

Use this structured 12-slide presentation outline for your final capstone defense, semester seminar, or viva evaluation.

---

### Slide 1: Title & Introduction
- **Project Title:** Personal Cloud Drive (CloudDrive)
- **Subtitle:** A Full-Stack Cloud Storage and File Management System
- **Technology Stack:** Spring Boot 3 (Java 21) &bull; React 18 &bull; Spring Security &bull; JWT &bull; JPA/Hibernate &bull; Tailwind CSS
- **Presenter:** [Your Name & Roll No]
- **Supervisor:** [Guide / Professor Name]
- **Department:** Department of Computer Science & Engineering, [College Name]

---

### Slide 2: Problem Statement & Motivation
- **The Challenge with Commercial Cloud Storage:**
  - High recurring subscription costs with rigid storage tiers.
  - Zero privacy: files scanned by proprietary AI algorithms and third parties.
  - Vendor lock-in and dependency on remote internet servers for local intranet sharing.
- **Project Goal:**
  - Build a lightweight, self-hosted, enterprise-grade Personal Cloud Drive with full Google Drive feature equivalence using industry-standard enterprise Java and React frameworks.

---

### Slide 3: System Architecture
- **Decoupled 3-Tier Architecture:**
  - **Frontend Tier:** React 18 Single Page Application (Vite, Tailwind CSS, Lucide Icons).
  - **Application Tier:** Spring Boot 3 REST API (Spring Security, JWT Filter, Controllers, Services).
  - **Data Tier:** Spring Data JPA + Hibernate ORM backed by file-persisted H2 Database.
  - **Storage Tier:** Host file system with isolated user roots (`./storage_root/users/{username}/`).

---

### Slide 4: Data Flow & Request Lifecycle
- **Context Diagram (Level 0 DFD):**
  - User authenticates via React UI -> Receives cryptographically signed JWT.
  - Subsequent requests pass `Authorization: Bearer <token>`.
  - Spring Security `JwtAuthenticationFilter` verifies claims and grants access.
- **Level 1 DFD:**
  - Multipart upload / Editor save -> Quota validation -> Disk streaming & Version snapshot -> Database metadata save -> Audit logging.

---

### Slide 5: Security Architecture & Defenses
- **1. BCrypt Password Security:**
  - Adaptive logarithmic salt and hashing (Strength 10) prevents brute-force & rainbow tables.
- **2. Stateless JWT Authentication:**
  - HMAC-SHA512 cryptographic signatures with 24-hour expiration.
  - Zero server-side session memory overhead.
- **3. Path Traversal Mitigation (CWE-22):**
  - String sanitization via `StringUtils.cleanPath()` and explicit rejection of relative paths (`..`).
  - Strict physical isolation inside `./storage_root/users/{username}/`.

---

### Slide 6: Database Design (Entity-Relationship)
- **Relational Schema (H2 File Database):**
  - `users`: Credentials, full name, role, storage quota (500 MB), and used bytes.
  - `file_items`: Hierarchical path, name, size, MIME type, starred flag, color, version count, and trash status.
  - `file_versions`: Revision history, snapshot disk path, byte size, and change summaries.
  - `share_links`: Unique public token, expiration timestamp, and download counters.
  - `user_shares`: User-to-user sharing records with VIEW/EDIT permissions.
  - `activity_logs`: Complete audit trail of all file and user operations.

---

### Slide 7: Core Google Drive Features
- **File & Folder Operations:**
  - Drag-and-drop batch upload, file download, blank document authoring (.txt, .md, .js, .html, .json), folder creation, rename, and move.
- **Folder Color Coding:**
  - 8 distinct visual color palettes persisted to the database for visual organization.
- **Streaming ZIP Downloads:**
  - Dynamic on-the-fly zip packaging of entire folder trees and multi-select batch file downloads without temporary files on disk.
- **Recycle Bin (Soft Delete):**
  - Moves items to Trash preserving original paths; provides one-click restoration or permanent empty.
- **Favorites / Starred & Search:**
  - Real-time instant search bar; category pills (Documents, Images, Video, Audio, Archives, Code).

---

### Slide 8: Collaboration & Sharing Systems
- **1. Public Expiring Share Links:**
  - Generates unique public URLs with expiring tokens (e.g. `http://localhost:5173/?share=TOKEN`).
  - Configurable expiration (24h, 7 days, 30 days, or Never) with download counter tracking.
  - External users can preview file details and download directly without an account.
- **2. Internal User-to-User Collaboration ("Shared with Me"):**
  - Share files directly with registered users with granular VIEW or EDIT permissions.
  - Recipient sees all shared items in their dedicated "Shared with Me" view.

---

### Slide 9: In-Browser Editor, Versioning & Previews
- **Built-in Editor with Version Snapshots:**
  - Edit code, Markdown, HTML, JSON, and text directly in the browser.
  - Each save automatically archives the previous version to `.versions/`.
  - Version History timeline modal with 1-click snapshot restore.
- **Multimedia Lightbox & Players:**
  - Image lightbox preview modal.
  - HTML5 video player and audio player modals.
  - PDF inline iframe viewer.

---

### Slide 10: Storage Quota Management & Audit Logging
- **Categorical Storage Analytics:**
  - Interactive dashboard breaking down usage by file type: Documents, Images, Video, Audio, Code, Archives, Others, and Trash.
  - Pre-upload quota validation prevents disk over-allocation (default 500 MB per user).
- **Audit Activity Trail:**
  - Tracks user action history (Registrations, Logins, Uploads, Edits, Shares, Trash, Restore) with relative time-ago timestamps.

---

### Slide 11: Testing & Results
- **Automated Integration Testing:**
  - Spring Boot test suite (`CloudDriveApplicationTests.java`).
  - 7 comprehensive tests: registration, JWT validation, duplicate constraints, folder hierarchy, trash lifecycle, version history rollback, user sharing, and zip streaming.
  - **Results: 7/7 Tests Passing (100% Pass Rate, 0 Errors, 0 Failures).**
- **Frontend Build & Live Verification:**
  - Vite production build compiled in 8.79 seconds with 0 errors.
  - PowerShell live REST API harness verified all endpoints return 200 OK.

---

### Slide 12: Live Demonstration & Conclusion
- **Demo Steps:**
  1. Register a new user account with email and password (real JWT authentication).
  2. Create a color-coded folder and create a new document in it.
  3. Edit the document in the in-browser editor and demonstrate version history rollback.
  4. Stream download a folder as a ZIP archive.
  5. Share a file with a peer user and demonstrate the "Shared with Me" view.
  6. Generate an expiring public link, open in incognito mode, and download anonymously.
  7. Check Storage Analytics category breakdown and Recycle Bin restoration.
- *Thank you! Questions and Answers.*
