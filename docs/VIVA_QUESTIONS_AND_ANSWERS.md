# VIVA VOCE & TECHNICAL DEFENSE PREPARATION GUIDE
## Personal Cloud Drive: Spring Boot & React Capstone Project

This guide prepares you for university viva-voce examinations, project reviews, and technical evaluations by covering architecture, Spring Boot, Spring Security, JWT, JPA, React, and storage engineering.

---

### SECTION 1: ARCHITECTURE & TECHNOLOGY CHOICES

#### Q1: What is the high-level architecture of your project?
**Answer:**  
Our project is structured as a decoupled **Client-Server Single Page Application (SPA)**:
1. **Frontend:** React 18 with Vite, Tailwind CSS, and Lucide React Icons. It is completely decoupled and consumes the backend via RESTful JSON APIs.
2. **Backend:** Spring Boot 3.3.5 running on Java 21, providing security, request validation, business logic, file streaming, and database interactions.
3. **Database Layer:** Spring Data JPA with Hibernate ORM backed by an H2 file database (`clouddrivedb`), ensuring persistent relational metadata across restarts with zero external setup.
4. **Storage Layer:** Guarded local host file system with user-specific directory isolation (`./storage_root/users/{username}/`).

#### Q2: Why did you choose Spring Boot for the backend?
**Answer:**  
1. **Enterprise Reliability:** Spring Boot is the industry gold standard for building robust, scalable enterprise microservices and REST APIs.
2. **Spring Security & Filter Chains:** Provides declarative and programmatic security, token extraction, and authorization with minimal boilerplate.
3. **Spring Data JPA:** Eliminates tedious boilerplate SQL through repository interfaces (`UserRepository`, `FileItemRepository`, `FileVersionRepository`, `UserShareRepository`) with automatic query generation.
4. **Built-in File Streaming:** High-performance `MultipartFile`, `Resource`, and `StreamingResponseBody` streaming that pipes byte streams directly to disk and HTTP responses without consuming excessive server memory.

#### Q3: Why React 18 and Vite for the frontend?
**Answer:**  
1. **Component-Driven Architecture:** Clean modular components (AuthModal, Navigation Sidebar, File Grid, File List, In-Browser Editor, Share Modal, Multimedia Lightbox).
2. **Vite Build Speed:** Vite utilizes native ES modules (ESM) and Rollup, compiling in seconds and delivering instantaneous Hot Module Replacement (HMR).
3. **Tailwind CSS:** Utility-first CSS allowing us to design a modern, responsive Google Drive / Dropbox-style UI with zero runtime overhead.

---

### SECTION 2: AUTHENTICATION & SECURITY

#### Q4: How is authentication handled in your project?
**Answer:**  
We implemented **stateless JWT (JSON Web Token) authentication**:
1. When a user registers or logs in via `/api/auth/login`, Spring Security's `AuthenticationManager` validates the credentials against the database.
2. On successful authentication, the server generates a signed JWT using HMAC-SHA512 containing the user's username, issuance time, and expiration time (24 hours).
3. The React client receives this token and stores it in `localStorage`.
4. For all subsequent requests to protected endpoints, the React frontend attaches the token in the `Authorization: Bearer <token>` header.
5. In Spring Boot, our custom `JwtAuthenticationFilter` intercepts each request, extracts and verifies the token signature using `JwtTokenProvider`, loads the user's authorities, and sets the `SecurityContextHolder`.

#### Q5: How are passwords secured?
**Answer:**  
Passwords are encrypted using **BCrypt** with an adaptive work factor (strength 10). BCrypt incorporates a cryptographically secure random salt into each hash, preventing precomputed dictionary attacks and rainbow table lookups.

#### Q6: How does the application prevent Directory Traversal (CWE-22)?
**Answer:**  
Directory Traversal occurs when an attacker passes path sequences like `../../etc/passwd` to access files outside their directory. We prevent this through:
1. `StringUtils.cleanPath()` normalization.
2. Explicit checks: `if (clean.contains("..")) throw new IllegalArgumentException("Cannot store file with relative path sequence");`
3. Physical path resolution: All files are scoped strictly inside `./storage_root/users/{username}/`.

#### Q7: What is CORS and how did you configure it?
**Answer:**  
CORS (Cross-Origin Resource Sharing) is a browser security mechanism that restricts web pages from making requests to a different domain/port. Because React runs on `http://localhost:5173` and Spring Boot runs on `http://localhost:8080`, we configured a custom `CorsConfigurationSource` bean in `SecurityConfig.java` allowing `GET`, `POST`, `PUT`, `DELETE`, and exposing the `Content-Disposition` header for file downloads. In Vite, we also configured a reverse proxy for `/api`.

---

### SECTION 3: CORE CLOUD DRIVE & GOOGLE DRIVE FEATURES

#### Q8: How does user storage isolation work?
**Answer:**  
When a user registers:
1. Spring Boot creates a database record in the `users` table with their assigned storage quota (default 500 MB).
2. The service creates an isolated directory on disk: `./storage_root/users/{username}/`.
3. Every file item in the database has an `owner_id` foreign key. All repository queries are strictly filtered by the authenticated user (`findByOwnerAndPath...`), guaranteeing that User A can never view, download, or delete User B's files.

#### Q9: How is the storage quota tracked and categorized?
**Answer:**  
1. Each `FileItem` records its exact byte size upon upload.
2. The repository provides a sum query: `SELECT COALESCE(SUM(f.size), 0) FROM FileItem f WHERE f.owner = :owner AND f.isDirectory = false AND f.isTrashed = false`.
3. Before storing any incoming uploaded file, `uploadFiles()` calculates whether `currentUsed + incomingFilesSize > quotaBytes`. If it exceeds the limit, it immediately rejects the request with a `400 Bad Request: Storage quota exceeded`.
4. The `/api/files/storage-breakdown` endpoint aggregates bytes by file category: Documents (.pdf, .doc, .txt, .md), Images (.png, .jpg, .svg), Video (.mp4, .webm, .mkv), Audio (.mp3, .wav), Code (.js, .ts, .py, .java, .json), Archives (.zip, .tar, .rar), and Others, displayed in the React Storage Analytics dashboard.

#### Q10: How does the Recycle Bin (Soft Delete) and Restore feature work?
**Answer:**  
Instead of immediately calling `Files.delete()`:
1. When a user clicks delete, the file's `isTrashed` flag is set to `true`, `trashedAt` is recorded, and `originalPath` stores the folder location where the file previously resided.
2. The file vanishes from the active drive view and appears in the Trash view.
3. When restored, `isTrashed` is set to `false`, and the file is restored to its `originalPath`.
4. Only when the user clicks "Empty Trash" are physical disk files purged via `Files.deleteIfExists()` and records deleted from the database.

#### Q11: How does Public File Sharing work without requiring login?
**Answer:**  
1. When a user clicks "Share" on a file, the system generates a unique 32-character random UUID token and creates a record in `share_links` with optional expiration (24 hours, 7 days, or Never).
2. The frontend copies the public URL: `http://localhost:5173/?share={token}`.
3. When any external user opens this link, the React app detects the `share` parameter and calls the public unauthenticated endpoints:
   - `GET /api/share/info/{token}`: returns file name and size.
   - `GET /api/share/download/{token}`: increments the download counter and streams the file for download.

#### Q12: How does User-to-User Sharing ("Shared with Me") work?
**Answer:**  
1. A user can share any file or folder directly with another registered username in the system, selecting either `VIEW` (read/download only) or `EDIT` permission.
2. The server records this in the `user_shares` table with foreign keys to the owner, recipient, and file item.
3. When the recipient logs in, their **"Shared with Me"** tab queries `GET /api/share/shared-with-me`.
4. Permission checks ensure that users with `VIEW` access cannot modify, rename, or delete the file, while users with `EDIT` access can update content in the in-browser editor.

#### Q13: How does File Version History and Snapshot Rollback work?
**Answer:**  
1. Every time a file is saved via the in-browser editor or updated via revision upload, `FileStorageService.saveFileContent()` copies the existing file to `./storage_root/users/{user}/.versions/{id}_v{num}`.
2. A `FileVersion` entity is persisted recording the revision number, size, disk path, timestamp, and change summary.
3. The parent `FileItem` increments its `versionCount`.
4. In the UI, clicking "Version History" opens a modal displaying the entire revision timeline.
5. Clicking "Restore" on any previous version copies that snapshot back over the active file and updates the database, guaranteeing safe non-destructive rollback.

#### Q14: How does dynamic ZIP compression work for folders and batch downloads?
**Answer:**  
Rather than generating a temporary `.zip` file on disk, the backend uses Java's `ZipOutputStream` wrapped inside Spring's `StreamingResponseBody`:
```java
StreamingResponseBody stream = out -> fileStorageService.streamFolderAsZip(user, id, out);
```
Files are recursively read from disk, compressed on the fly, and streamed directly into the client's HTTP response socket. This prevents memory leaks and ensures near-zero disk overhead on the server.

#### Q15: How are multimedia previews handled?
**Answer:**  
1. **Images:** Rendered in an interactive lightbox modal with full resolution zoom and metadata.
2. **Audio:** Streamed directly into an HTML5 `<audio controls>` player modal (`GET /api/files/preview/{id}`).
3. **Video:** Streamed directly into an HTML5 `<video controls>` player modal.
4. **PDF:** Embedded using an HTML `<iframe>` with native browser PDF reader controls.
5. **Code & Text:** Rendered in the in-browser code editor with full editing, version history, and save capabilities.

---

### SECTION 4: DATABASE & PERFORMANCE

#### Q16: Why did you use H2 database instead of MySQL or PostgreSQL?
**Answer:**  
1. **Zero-Configuration Deployment:** H2 is an embedded Java database that requires no external database server installation, making the project run instantly on any examiner's computer.
2. **File Persistence:** Configured with `jdbc:h2:file:./data/clouddrivedb`, it retains all users, files, shares, and activity logs across application restarts.
3. **Examiner Inspection:** Includes the built-in web-based H2 Console at `http://localhost:8080/h2-console`, allowing professors to inspect relational tables during the viva.
4. **Enterprise Portability:** Because we used standard JPA annotations, switching to MySQL or PostgreSQL in production requires only changing three lines in `application.yml` (`datasource.url`, `driverClassName`, and dialect).

#### Q17: How did you test your application?
**Answer:**  
We implemented 7 automated integration tests in `CloudDriveApplicationTests.java`:
1. `testUserRegistrationAndJwtIssuance`: Asserts registration and JWT signature validity.
2. `testDuplicateUsernameRejection`: Verifies constraint handling for duplicate accounts.
3. `testCreateFolderAndList`: Verifies folder hierarchy and path navigation.
4. `testFileUploadAndTrashLifecycle`: Upload, soft delete to trash, and restore.
5. `testFileVersionHistory`: Automatic revision creation and 1-click restore.
6. `testUserToUserSharing`: Internal sharing and recipient visibility.
7. `testStorageBreakdownAndZipStreaming`: Category byte calculation and on-the-fly zip packaging.
All 7 integration tests pass with 0 failures, and all endpoints were verified live at runtime.
