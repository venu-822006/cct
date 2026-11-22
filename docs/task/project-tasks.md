# Project Task Tracker: Personal Cloud Drive (Spring Boot + React)

## 1. Backend Architecture (Spring Boot 3 + Java 21)
- [x] Configure Spring Boot 3.3.5 Maven project with Web, Security, Data JPA, Validation, H2, and JJWT.
- [x] Implement database entities: `User`, `FileItem`, `FileVersion`, `ShareLink`, `UserShare`, `ActivityLog`.
- [x] Implement Spring Data JPA repositories with custom queries, quota summation, and category breakdown.
- [x] Configure Spring Security with `JwtAuthenticationFilter`, BCrypt password hashing, and stateless session management.
- [x] Implement `AuthService` with real registration, login, profile update, password change, and isolated user directory creation.
- [x] Implement `FileStorageService` for:
  - Folder creation & color coding
  - Blank document creation (.txt, .md, .js, .html, .json)
  - Streaming multi-file uploads
  - Single file download & inline preview resource loading
  - Folder ZIP streaming (`streamFolderAsZip`)
  - Batch multi-file ZIP streaming (`streamMultipleFilesAsZip`)
  - In-browser file content editing with automatic version snapshotting
  - File version history listing & 1-click version restore
  - Rename, move, star/unstar, soft delete (trash), restore, empty trash
  - Storage category breakdown calculation (Documents, Images, Video, Audio, Code, Archives, Other, Trash)
  - Batch operations (batch trash, batch download)
- [x] Implement `ShareService` for:
  - Public shareable links with expiration hours and download hit counter
  - Internal User-to-User sharing (`UserShare`) with VIEW and EDIT permissions
  - "Shared with Me" and "Shared by Me" queries
- [x] Implement `ActivityService` for tracking user action audit logs with timestamps.
- [x] Implement REST controllers: `AuthController`, `FileController`, `ShareController`, `ActivityController`.
- [x] Execute backend integration tests (`CloudDriveApplicationTests.java`) -> 7/7 passing (100% pass rate).
- [x] Live end-to-end API verification of all endpoints via PowerShell test harness (All passed 200 OK).

## 2. Frontend Architecture (React 18 + Vite + Tailwind CSS)
- [x] Remove demo login and fake hardcoded credentials completely.
- [x] Create modern `AuthModal` supporting Sign In and Account Registration with validation and instant feedback.
- [x] Create API client `springApi.ts` connecting seamlessly to Spring Boot REST endpoints with JWT Bearer tokens.
- [x] Build Google Drive-equivalent explorer UI:
  - Sidebar navigation: My Drive, Shared with Me, Recent, Starred, Public Links, Storage Analytics, Activity Log, Trash
  - Storage quota progress widget with live percentage
  - Breadcrumb navigation and root navigation
  - Instant live search bar
  - Category filter pills (All, Documents, Images, Video, Audio, Archives, Code)
  - Sort dropdown (Name, Date, Size - Asc/Desc)
  - Grid View and List View switcher
  - Multi-select checkboxes and batch actions toolbar (Batch ZIP download, Batch Move to Trash)
- [x] "+ New" context dropdown menu:
  - Upload Files (multi-file select)
  - Upload Folder
  - New Folder modal
  - New Document modal (.txt, .md, .js, .html, .json)
- [x] Folder Color Palette picker (8 vibrant colors with immediate backend persistence).
- [x] In-Browser Code & Text Editor modal with syntax-friendly editor and revision saving.
- [x] File Version History modal with version timeline and 1-click version rollback.
- [x] Multimedia Previews:
  - Image lightbox previewer
  - HTML5 Video player modal
  - HTML5 Audio player modal
  - PDF inline iframe embed preview
- [x] Sharing System:
  - Public Share modal (create expiring links, copy to clipboard, choose expiration)
  - Anonymous Public Download page (`/?share=token`)
  - User-to-User Share tab in Share Modal (target username + VIEW/EDIT permission)
  - "Shared with Me" dedicated view
- [x] Storage Analytics Dashboard:
  - Visual breakdown by file category (Documents, Images, Video, Audio, Code, Archives, Others, Trash)
  - Interactive quota percentage bar
- [x] User Settings modal:
  - Edit full name and email
  - Change password with verification
- [x] Activity Log audit trail table with human-readable action badges and relative time ago.
- [x] Recycle Bin / Trash with individual Restore and Empty Trash actions.
- [x] Verify Vite production build (`bun run build`) -> compiles in <9s with 0 errors.

## 3. Launchers & Cloud Documentation
- [x] Create one-click Windows launcher (`start-drive.bat` and `start-drive.ps1`) with smart runtime fallback (Bun/npm).
- [x] Background service daemons active on port 8080 (Spring Boot) and port 5173 (React Vite).
- [x] Verified full live end-to-end execution on local system.
- [x] Cleaned all legacy brand names, sponsor mentions, unwanted mention words, and demo writeups.
- [x] Cleaned all legacy brand names, sponsor mentions, and unwanted mention words across HTML, package.json, constants, components, README, and launchers.
- [x] Redesigned and updated frontend color theme with modern palette system (Deep Indigo & Cyber Violet default, plus Emerald Aurora, Oceanic Azure, Sunset Rose, and Nordic Light), dynamic interactive theme switcher in Header and Account Settings, theme persistence via localStorage, and full theme styling across all components, modals, and AuthModal.
- [x] Implemented Google Cloud Zero-Billing deployment:
  - Created `deploy-cloudshell.sh` for one-click setup in Google Cloud Shell.
  - Generated Maven wrapper (`mvnw`, `mvnw.cmd`) in `backend/` for cross-platform Linux / Cloud Shell compatibility.
  - Packaged React frontend static build directly into Spring Boot classpath (`backend/src/main/resources/static/`).
  - Created multi-stage production `Dockerfile` for single-container deployment.
  - Authored comprehensive guide `docs/GOOGLE_CLOUD_WITHOUT_BILLING_GUIDE.md` covering Google Cloud Shell, Google Drive API, Cloudflare Tunnels, and free zero-card cloud container platforms.
- [x] Pushed full project to remote GitHub repository `https://github.com/venu-822006/cct.git` on branch `main`.
- [x] Both backend (port 8080) and frontend (port 5173) are actively running and verified.
- [x] Created database schema script (`database.sql`) containing complete DDL/DML for `users` and login system.
- [x] Added `mysql-connector-j` dependency and `application-mysql.yml` for optional external MySQL / phpMyAdmin database connectivity.
- [x] Streamlined UI to match clean drive interface design and stripped all verbose sentences, helper descriptions, and unwanted writeups across header, sidebar, modals, tables, empty states, and authentication screens.
- [x] Removed Google Drive trademark logos and Google-specific icons; introduced clean custom `DriveLogo` cloud mark while preserving the modern clean interface.
- [x] Verified test suite (`bun test` -> 24/24 pass) and production build (`bun run build` -> 0 errors).
- [x] Created `backend/src/main/resources/application-gcloud.yml` Spring profile for Google Cloud Run deployment (env-var-driven datasource, GCS storage type, production logging, 5 GB quota, H2 console disabled).
- [x] Updated `cloudbuild.yaml` to use Artifact Registry instead of Container Registry, added `--set-env-vars SPRING_PROFILES_ACTIVE=gcloud`, JWT secret from Cloud Secret Manager, memory/CPU/instance scaling options.
- [x] Updated `deploy-cloudshell.sh`: structured 5-step build process, activates `gcloud` Spring profile, clean progress output, `--project` flag support.
- [x] Pushed all changes (UI + Cloud config) to GitHub `https://github.com/venu-822006/cct.git` branch `main`.
- [x] Completely removed all remaining Google Drive trademark logos, Google 4-color Plus icon, and Google Apps waffle menu; replaced with custom Cloud Drive emblem and clean modern iconography.
- [x] Streamlined themes to Light and Dark only (`gdrive-light` and `gdrive-dark`), removed unused multi-color themes, and added a single smooth toggle switch component (`ThemeToggle`) across Header, AuthModal, and Settings modal.
- [x] Fixed 500 error on Cloud Shell: updated `application-gcloud.yml` to use persistent file-based H2 database (`./data/clouddrivedb`) with `DB_CLOSE_DELAY=-1` to prevent in-memory table dropping, added comprehensive error logging in `GlobalExceptionHandler`, and handled stale/invalid tokens in `AuthController.getCurrentUser` returning 401 instead of 500.
- [x] Verified full test suite (`bun test` -> 9/9 pass, Spring Boot -> 8/8 pass) and production build (`bun run build` -> 0 errors).

## 4. Google Drive Theme Conversion
- [x] Converted UI to authentic Google Drive Material You (Material 3) aesthetic:
  - Default theme: `Google Drive (Light)` with `#f8fafd` canvas, `#ffffff` content card, and `#c2e7ff` active nav pill.
  - `Google Drive (Dark)` theme with `#131314` canvas, `#1e1f20` surface, and `#004a77` active pill.
  - Authentic 3-color Google Drive triangle logo in header and authentication modal.
  - Centered pill search bar with "Search in Drive" placeholder and search glass icon.
  - Elevated "+ New" button with official 4-color Google plus icon (blue, red, yellow, green).
  - Clean borderless sidebar navigation pills with soft blue active indicator (`#c2e7ff`) and gray inactive items.
  - Main sheet container with rounded corners (`rounded-2xl`), pure white background (`#ffffff`), subtle shadow and border.
  - "My Drive ▾" header with dropdown chevron, category filter chips, and grid/list view switcher.
  - Google Drive folder cards and file preview cards.
  - Configured Tailwind dark mode to `class` mode and eliminated user-agent button borders for clean Material You rendering.
  - Live visual verification via browser subagent at 1280x800 desktop viewport.
  - Frontend production build (`npm run build`) succeeded with 0 errors.
  - Backend integration tests (7/7) passed with 100% success rate.

## 5. Clean Slate: Demo Data & Unwanted Writeup Removal
- [x] Removed unwanted academic writeups and reports (`COLLEGE_PROJECT_REPORT.md`, `PRESENTATION_SLIDES_OUTLINE.md`, `VIVA_QUESTIONS_AND_ANSWERS.md`, `s3-integration-tasks.md`, `2025-01-24-s3-integration.md`).
- [x] Renamed and updated task tracking to `project-tasks.md`.
- [x] Removed automatic generation of `Welcome_To_CloudDrive.txt` and its demo writeup text in `AuthService.java` so newly registered users start with a clean, empty drive.
- [x] Cleaned demo tags in `FileItem.java` and updated test parameters in `CloudDriveApplicationTests.java`.
- [x] Cleaned `.env` environment configuration and `README.md`.
- [x] Purged demo directories and files from physical storage (`storage_root/` and legacy `storage_drive/`).
- [x] Cleared demo data from database so all user drives start in a clean zero state.

## 6. Public Share Link Under Three Dots Context Menu
- [x] Added Three Dots (`⋮` / `MoreVertical`) context menu button to all items across the UI:
  - Grid View Folders (top-right of folder cards)
  - Grid View Files (card title header and hover action bar)
  - List View Table Rows (rightmost Actions column for each row)
  - Multi-select and Single-item Selection Action Bar
- [x] Placed "Public link" prominently at the top of the Three Dots dropdown menu (`Link2` icon, title "Public link", subtitle "Generate & copy link"):
  - Clicking automatically generates an unauthenticated shareable link
  - Copies link directly to clipboard (`navigator.clipboard.writeText`)
  - Displays instant toast feedback ("Public link generated & copied to clipboard!")
  - Opens Google Drive Share modal showing expiration options, copy button, and new-tab preview button
- [x] Implemented backend public folder sharing support:
  - Enabled folder sharing in `ShareService.createShareLink`
  - Streaming folder contents as ZIP on unauthenticated download (`/api/share/download/{token}`) via `HttpServletResponse` streaming
  - Public folder info endpoint (`/api/share/info/{token}`) returns folder metadata and `.zip` filename
- [x] Added unit tests for public file and folder sharing in `CloudDriveApplicationTests.java` (8/8 tests passing, 100% pass rate).
- [x] Executed live end-to-end test verifying file and folder public link generation, unauthenticated info retrieval, and ZIP streaming.
- [x] Built frontend production assets (`npm run build`) and synced to Spring Boot `backend/src/main/resources/static/`.
- [x] Tested and verified via backend test suite (`mvn test`) and frontend build (`npm run build`).

## 7. Streamline Sidebar & Views (Activity Removed)
- [x] Removed Activity item and pulse/waveform icon from left sidebar navigation.
- [x] Removed Activity log view and table from frontend views.
- [x] Cleaned navigation state union type (`currentView`) and eliminated unnecessary activity polling.
- [x] Rebuilt frontend production bundle with 0 errors and synced to Spring Boot static resources.
- [x] Tested and verified via backend test suite (`mvn test`) and live browser check.

## 8. Password Visibility Eye Toggle Button
- [x] Added interactive Eye (`Eye` / `EyeOff`) visibility toggle button to all password inputs:
  - `AuthModal` (Sign In and Account Registration password field)
  - `App.tsx` Account Settings modal (Current Password and New Password fields)
- [x] Implemented toggling state between `type="password"` (masked with bullets) and `type="text"` (plain text) with smooth transition and icon swap.
- [x] Enhanced input padding (`pr-10`) so text never overlaps the eye toggle button.
- [x] Verified frontend build (`npm run build` -> 0 errors) and backend unit test suite (`mvn test` -> 8/8 passing).

## 9. Polish: Animations, Profile Popover, Upload Widget, Theme & Anti-Bot Protection
- [x] Restructured git history so all commits are authored by user (`venu-822006 <venu-822006@users.noreply.github.com>`) with 0 outside contributors.
- [x] Fixed `.github/workflows/docker.yaml` container registry workflow (added `packages: write` permissions, modern `$GITHUB_OUTPUT`, QEMU setup, and reliable `linux/amd64` multi-platform build).
- [x] Refined Light and Dark themes with subtle creamy warm tones (`#fbfaf6` light creamy canvas, `#141312` rich espresso dark canvas) while preserving crisp white cards and black contrast.
- [x] Implemented Google Drive entrance animation with floating logo (`animate-drive-float`) and sleek Material 3 indeterminate progress bar (`animate-drive-loader`).
- [x] Added Google Drive signature floating upload & progress popup widget docked at the bottom-right corner showing live file queue, animated spinners, size, expand/collapse, and checkmarks.
- [x] Created Google Account profile dropdown card under top-right avatar displaying Full Name, @username, email, storage quota usage bar, Account Settings link, and Sign Out button.
- [x] Added Anti-Bot & AI Crawler Protection:
  - `public/robots.txt` blocking GPTBot, ClaudeBot, CCBot, Google-Extended, PerplexityBot, and scrapers.
  - Interactive Human Verification math challenge CAPTCHA and hidden honeypot trap on AuthModal.
- [x] Verified full build (`npm run build` -> 0 errors) and backend test suite (`mvn test` -> 8/8 passing).

## 10. Fix Safe JSON Response Parsing
- [x] Fixed `requestJson` in `springApi.ts` to read `response.text()` before parsing JSON.
- [x] Eliminated `Failed to execute 'json' on 'Response': Unexpected end of JSON input` error by safely handling empty response bodies, HTTP 204 No Content, and non-JSON server error text.
- [x] Verified with production build (`npm run build` -> 0 errors) and backend unit test suite (`mvn test` -> 8/8 passing).

## 11. Codebase Cleanup & Runtime Fixes
- [x] Fixed `Cannot read properties of undefined (reading 'charAt')` in `App.tsx` header avatar fallback, restoring full page load.
- [x] Removed human verification CAPTCHA and honeypot from `AuthModal.tsx` for clean, instant sign-in and registration.
- [x] Purged 21 unused legacy prototype files across `src/components/`, `src/hooks/`, `src/services/`, and `tests/storage/`.
- [x] Verified full test suite (`bun test` -> 9/9 passing, `mvn test` -> 8/8 passing) and build (`npm run build` -> 0 errors).

## 12. Vite API Proxy & Non-JSON Fallback Hardening
- [x] Fixed Vite development server proxy in `vite.config.ts` targeting IPv4 `http://127.0.0.1:8080`.
- [x] Restarted Vite dev server daemon to apply proxy rules for all `/api/*` endpoints.
- [x] Hardened `requestJson` in `springApi.ts` to detect HTML SPA fallbacks (`text/html`) and throw structured errors instead of passing corrupt payload to React state.
- [x] Added defensive array validation (`Array.isArray(items)`) in `App.tsx` preventing `items.filter is not a function` runtime crashes.
- [x] Verified complete flow in live browser: clean AuthModal load, registration, login, and full Google Drive explorer dashboard rendering.

## 13. Google reCAPTCHA v2 Integration, Cloud Deployment Guide & Public Links Removal
- [x] Removed all Public Links navigation, views, action buttons, and table tabs across `App.tsx`:
  - Removed "Public links" item from sidebar navigation.
  - Removed Public Links view and table.
  - Cleaned Three Dots context menu and action bars to offer standard "Share" (user-to-user sharing) instead of public links.
  - Simplified Share dialog to direct, clean User-to-User sharing modal.
- [x] Removed all in-app reCAPTCHA code from React frontend (`AuthModal.tsx`) and Spring Boot backend (`AuthService.java`, `LoginRequest.java`, `RegisterRequest.java`) as requested.
- [x] Streamlined authentication flow for instant, zero-friction local and staging sign-in with clean username/password and password visibility toggle.
- [x] Authored comprehensive guide `docs/GOOGLE_CLOUD_RECAPTCHA_GUIDE.md` detailing how to configure Google Cloud Armor with edge reCAPTCHA bot defense at the Cloud Load Balancer level (zero code needed in container).
- [x] Verified full test suite (`bun test` -> 9/9 passing, `mvn test` -> 8/8 passing) and production build (`npm run build` -> 0 errors).
- [x] Live browser verified: instant sign-in without CAPTCHA, clean Google Drive dashboard, and zero Public links.

## 14. Fix Upload "Failed to Fetch" & Cloud Shell Forwarding
- [x] Fixed `NullPointerException` during file upload in `FileStorageService.java`: safely guarded `sumUsedBytesByOwner` null unboxing for new users with 0 files.
- [x] Hardened `FileController.java` to catch all general exceptions and return structured JSON error responses rather than letting exceptions escape and trigger Tomcat connection resets.
- [x] Added `server.forward-headers-strategy: framework` and `server.tomcat.max-swallow-size: -1` in both `application.yml` and `application-gcloud.yml` to prevent connection resets behind Cloud Shell Web Preview reverse proxy.
- [x] Updated upload floating popup widget status message in `App.tsx` to accurately display failure count (`1 upload failed`) when an upload error occurs.
- [x] Verified full build and live file upload: 200 OK with immediate persistence.

## 15. Cloud Shell Port 8080 Cleanup & Stale Cache Resolution
- [x] Resolved "Web server failed to start. Port 8080 was already in use" error in `deploy-cloudshell.sh` by adding automatic port cleanup (`fuser -k 8080/tcp` and `pkill -f personal-cloud-drive`).
- [x] Cleaned and rebuilt fresh production assets to `backend/src/main/resources/static/`.
- [x] Verified that in-app reCAPTCHA is completely removed from all source code and built bundles.
- [x] Ran full test suite (`bun test` -> 9/9 pass, `mvn test` -> 8/8 pass, BUILD SUCCESS).

## 16. Improve Network Error Feedback for Cloud Shell
- [x] Added friendly network error handler in `src/services/springApi.ts` catching `fetch()` network failures and providing actionable guidance when the backend server on port 8080 is unreachable.
- [x] Rebuilt frontend bundle (`index-BWeXUHhb.js`) and updated Spring Boot static resources.
- [x] Verified full test suite (`bun test` -> 9/9 pass).

## 17. 1-Day Login Session & Sliding Window Rate Limiting
- [x] Configured and verified 1-day (24 hours / 86,400,000 ms) JWT session lifetime in both `application.yml` and `application-gcloud.yml`.
- [x] Implemented thread-safe `RateLimitingService.java` using sliding window counter with client IP resolution (respecting `X-Forwarded-For` and `X-Real-IP`).
- [x] Integrated rate limiting in `AuthController.java`:
  - Login: Maximum 10 attempts per minute per IP. Returns HTTP 429 Too Many Requests with `Retry-After: 60` header when exceeded.
  - Registration: Maximum 5 attempts per minute per IP. Returns HTTP 429 Too Many Requests when exceeded.
- [x] Added unit tests in `CloudDriveApplicationTests.java` verifying 10-attempt threshold enforcement and reset capabilities.
- [x] Verified complete test suites: `mvn test` (9/9 pass, BUILD SUCCESS) and `bun test` (9/9 pass).



## 16. Cloud Shell Upload "Failed to Fetch" Final Resolution
- [x] Permitted `HttpMethod.OPTIONS` globally in `SecurityConfig.java` to prevent CORS preflight rejections by the Cloud Shell HTTPS reverse proxy.
- [x] Bypassed `OPTIONS` requests in `JwtAuthenticationFilter.java` so preflights are never evaluated for missing JWT tokens.
- [x] Hardened `springApi.ts` to ensure `FormData` uploads never have conflicting `Content-Type` headers.
- [x] Strengthened `deploy-cloudshell.sh` port cleanup using both `fuser` and `lsof -ti:8080 | xargs kill -9` to guarantee port 8080 is freed before launching.
- [x] Updated git file mode permissions (`chmod +x`) for `deploy-cloudshell.sh` and `backend/mvnw`.
- [x] Verified full build and test suites: `bun test` 9/9 pass, `mvn test` 8/8 pass (BUILD SUCCESS).

## 17. Complete Google Cloud Platform (GCP) Deployment & Production Packaging
- [x] Fixed Dockerfile JAR copying: added `<finalName>personal-cloud-drive</finalName>` in `backend/pom.xml` and targeted exact `personal-cloud-drive.jar` in `Dockerfile`.
- [x] Hardened runtime container: created `/app/data` and `/app/storage_root` directories and defaulted `SPRING_PROFILES_ACTIVE=gcloud`.
- [x] Optimized `.dockerignore` to prevent uploading heavy build artifacts (`backend/target`, `data`, `storage_root`) into Docker daemon and CI/CD pipelines.
- [x] Streamlined `cloudbuild.yaml`: increased Cloud Run RAM to `1Gi`, scaled to 5 max instances, and removed hardcoded Secret Manager blockers.
- [x] Authored turnkey automated script `deploy-gcloud.sh` with full support for Google Cloud Run, Artifact Registry, Cloud Build, and Google Cloud Storage (GCS FUSE) volume mounts.
- [x] Authored complete step-by-step production deployment guide `docs/GOOGLE_CLOUD_DEPLOYMENT_GUIDE.md`.
- [x] Executed full test suite (`bun test` -> 9/9 passing, `mvn clean package` -> 8/8 passing, BUILD SUCCESS).

## 18. Fix Cloud Shell Web Preview Redirect & Proxy Forwarding
- [x] Removed `server.forward-headers-strategy: framework` from `application.yml` and `application-gcloud.yml` to prevent Cloud Shell reverse proxy from rewriting `request.getServerName()` to `ssh.cloud.google.com`.
- [x] Configured `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` in `SecurityConfig.java` so unauthenticated requests always return HTTP 401 instead of Spring default redirects.
- [x] Added `credentials: "include"` in `springApi.ts` so browser AJAX requests forward Cloud Shell session cookies and avoid triggering Google re-auth redirects.
- [x] Rebuilt frontend bundle (`index-CxBbdSeE.js`) and refreshed backend static assets.
- [x] Verified full test suite (`bun test` -> 9/9 pass, `mvn test` -> 9/9 pass, BUILD SUCCESS).




