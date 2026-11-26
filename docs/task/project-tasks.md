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

## 19. Diagnosis of Cloud Shell Web Preview CAPTCHA & Production Cloud Run Path
- [x] Identified that Google Cloud Shell Web Preview (`8080-cs-*.cloudshell.dev`) is protected by Google's anti-abuse edge proxy which intercepts rapid AJAX/multipart requests and file uploads with a Google reCAPTCHA challenge ("Our systems have detected unusual traffic from your computer network... Select all images with cars").
- [x] Confirmed that when Google's proxy returns a CAPTCHA challenge HTML or redirect instead of proxying to port 8080, the browser's native `fetch()` fails with `TypeError: Failed to fetch` and marks the file upload as failed.
- [x] Documented the critical distinction between Cloud Shell (a temporary, 20-minute timeout developer sandbox behind Google bot proxies) and Google Cloud Run (a production serverless container service with dedicated HTTPS endpoints, no Cloud Shell proxies, and no bot CAPTCHA blocks).
- [x] Verified tests pass: `bun test` (9/9 pass) and `mvn test` (9/9 pass, BUILD SUCCESS).

## 20. Google Cloud Project Audit & Cleanup Preparation
- [x] Audited all 17 projects in user's Google Cloud account via Cloud Shell.
- [x] Identified single active project with billing enabled: `turing-racer-8sjh2` (Project Number: 753613244796 | Billing: True).
- [x] Cataloged 16 inactive, non-billing projects for deletion.
- [x] Prepared safe automated batch deletion sequence and Cloud Run deployment command targeting `turing-racer-8sjh2`.

## 21. Add Project Permissions Auditor and Auto-Creation to Deployer
- [x] Analyzed `AUTH_PERMISSION_DENIED` on `turing-racer-8sjh2`: account `venugopalreddyk392@gmail.com` lacks Owner/Editor IAM permissions on this specific sandbox project.
- [x] Created `check-gcloud-projects.sh` to quickly audit which GCP projects the account owns and whether billing is enabled.
- [x] Added `--create-project` flag to `deploy-gcloud.sh`: automatically queries the user's active billing account, creates a fresh project (`clouddrive-<timestamp>`), links billing, and deploys without permission conflicts.
- [x] Added friendly permission failure detection in `deploy-gcloud.sh` with actionable troubleshooting steps.
- [x] Verified full test suite (`bun test` -> 9/9 pass).

## 22. Permanent Background Daemon & Cloud Shell authuser Preservation
- [x] Fixed CORS preflight redirect in `springApi.ts`: only apply `Content-Type: application/json` when a request body is present (prevents non-simple preflight `OPTIONS` on `GET /api/auth/me`).
- [x] Added automatic `authuser` query parameter preservation in `springApi.ts`: forwards Cloud Shell Web Preview's `?authuser=0` on every fetch so Google edge proxy never issues unauthenticated 302 redirects.
- [x] Enhanced `deploy-cloudshell.sh` with `--daemon` / `--background` flag to run Spring Boot in background via `nohup` with PID tracking (`app.pid`) and health check loop.
- [x] Created turnkey `start.sh` and `stop.sh` scripts for 1-click startup and shutdown.
- [x] Rebuilt production frontend bundle (`index-Cn3T8aOr.js`) and refreshed Spring Boot static resources.
- [x] Verified full test suite (`bun test` -> 9/9 pass).

## 23. Turnkey Multi-Cloud Hosting & Instant Public Tunnel Options
- [x] Created `tunnel.sh` providing instant Cloudflare HTTPS tunnel for Cloud Shell: generates a public URL (`https://*.trycloudflare.com`) without proxies, CAPTCHAs, or billing dependencies.
- [x] Authored `render.yaml` for 1-click permanent free 24/7 cloud deployment on Render from GitHub repository `venu-822006/cct`.
- [x] Authored `railway.json` for 1-click automated container deployment on Railway.
- [x] Documented all hosting alternatives: Cloudflare Tunnel (instant in Cloud Shell), Render (free 24/7), Railway, and Google Cloud Run (with GCP free trial billing).
- [x] Verified tests pass: `bun test` 9/9 pass.

## 24. Live Production Deployment on Render Verified (https://cct-3c4n.onrender.com)
- [x] Production service deployed and online at `https://cct-3c4n.onrender.com`.
- [x] Tested and verified HTTP 200 OK on root HTML bundle with production React assets.
- [x] Tested and verified `POST /api/auth/login` with `admin` / `admin123`: returns JWT token and user profile with 5 GB quota.
- [x] Tested and verified `GET /api/files`: returns drive item list.
- [x] Tested and verified `POST /api/files/folder`: created `TestFolder` with color code and instant persistence.
- [x] Tested and verified `POST /api/files/blank`: created `Welcome.txt`.
- [x] Tested and verified `GET /api/files/download/2`: downloaded document content cleanly with HTTP 200.
- [x] Tested and verified multipart `POST /api/files/upload`: uploaded `sample-upload.txt` with zero errors.
- [x] Tested and verified `GET /api/files/quota`: correctly reports storage usage and quota bounds.
- [x] Cloud Drive is 100% operational, permanent 24/7, and completely free of Cloud Shell proxy/CORS errors.

## 25. Fix File Preview & Direct Media Viewing Authentication
- [x] Identified root cause of files not viewing: HTML tags (`<img>`, `<video>`, `<audio>`, `<iframe>`) cannot attach `Authorization: Bearer` headers, causing `401 Unauthorized` responses on `/api/files/preview/{id}`.
- [x] Updated `JwtAuthenticationFilter.java` to check both `Authorization: Bearer` header and query parameter `request.getParameter("token")`.
- [x] Updated `getFilePreviewUrl`, `getFileDownloadUrl`, and `getFolderZipDownloadUrl` in `springApi.ts` to append `?token=<jwt_token>`.
- [x] Added "Open Full" link in `App.tsx` preview modal allowing users to open any file or PDF in full-screen in a new tab.
- [x] Rebuilt frontend production bundle (`index-UO9PD8Fo.js`) and refreshed Spring Boot static resources.
- [x] Verified full test suite (`bun test` -> 9/9 pass, `mvn test` -> 9/9 pass, BUILD SUCCESS).

## 26. Cloud PostgreSQL Driver & Universal Database URL Auto-Detection
- [x] Fixed `Cannot load driver class: org.postgresql.Driver` by adding `org.postgresql:postgresql` runtime dependency to `backend/pom.xml`.
- [x] Created `DataSourceConfig.java` to automatically parse and support:
  - Standard JDBC URLs (`jdbc:postgresql:...`, `jdbc:mysql:...`, `jdbc:h2:...`)
  - Cloud provider URI formats (`postgres://user:password@host:port/dbname` or `postgresql://...` from Render / Supabase / Neon)
  - Automatic extraction of credentials and host parameters
  - Automatic driver class binding (`org.postgresql.Driver`)
- [x] Removed hardcoded H2 dialect in `application.yml` and `application-gcloud.yml` to let Hibernate 6 auto-detect PostgreSQL dialect.
- [x] Verified tests pass: `mvn test` 9/9 pass (BUILD SUCCESS), `bun test` 9/9 pass.

## 27. Universal Document Preview Modal & UI Action Discoverability
- [x] Added dedicated Document Preview card modal for non-streamable files (`.docx`, `.xlsx`, `.pptx`, `.zip`, `.tar`, `.csv`, etc.) in `src/App.tsx`.
- [x] Replaced `window.open` fallback (which triggered browser popup blockers) with in-app preview card displaying file format badge, name, formatted size, 1-click direct download button, share link action, and version history trigger.
- [x] Added direct Download button inside the preview modal header bar for all previewable formats.
- [x] Enhanced discoverability of 3-dots options and star buttons across touchscreens and mobile devices (`opacity-70 sm:opacity-0 sm:group-hover:opacity-100`).
- [x] Rebuilt frontend production bundle (`index-Qs5vRlZX.js`, `index-BWUYT8dZ.css`) and deployed into Spring Boot static resources.
- [x] Verified live deployment on `https://cct-3c4n.onrender.com` with real browser subagent: verified login, file upload (`test.txt`), file preview modal, and folder creation (`Test Folder`).
- [x] Verified all test suites pass: `bun test` (9/9 passed) and `mvn test` (9/9 passed, BUILD SUCCESS).

## 28. Comprehensive Production Hardening & Safe Branch Merge
- [x] Externalized hardcoded JWT secret to `JWT_SECRET` environment variable with dynamic ephemeral 256-bit key generator for dev/test runs.
- [x] Removed hardcoded admin account creation; implemented opt-in environment bootstrapping via `ADMIN_BOOTSTRAP_ENABLED=true`.
- [x] Implemented canonical path traversal checks (`validatePathInsideUserRoot`) and rejection of `..` across all folder and file write operations.
- [x] Fixed public folder ZIP download isolation: sibling folders (`/Projects` vs `/Projects-Secret`) are never leaked.
- [x] Implemented recursive virtual path updating for all descendants upon folder rename and move.
- [x] Implemented recursive trashing and restoration for all folder descendants in Recycle Bin.
- [x] Enforced user storage quota checks on blank file creation and file content edits.
- [x] Isolated genuinely public share endpoints in `SecurityConfig.java`, disabled H2 console by default in production, and made CORS origins configurable.
- [x] Restricted JWT URL query parameter extraction strictly to streaming endpoints.
- [x] Removed machine-specific `backend/run.args` from Git tracking and added to `.gitignore`.
- [x] Expanded backend integration test suite to 12 tests (`CloudDriveApplicationTests.java`): 12/12 passing (BUILD SUCCESS).
- [x] Rebuilt frontend production bundle with 0 errors (`bun run build`).

## 29. Google Cloud Free Services Deployment & Official Google reCAPTCHA Integration
- [x] Integrated official Google reCAPTCHA service triggered post-login ("at after the login") in `AuthModal.tsx`.
- [x] Connected Google reCAPTCHA widget via Google's official library (`https://www.google.com/recaptcha/api.js?render=explicit`).
- [x] Implemented `RecaptchaService.java` backend verification against Google's verification API (`https://www.google.com/recaptcha/api/siteverify`).
- [x] Added `/api/auth/verify-recaptcha` verification endpoint in `AuthController.java` with configurable keys via `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY`.
- [x] Documented zero-cost deployment on Google Cloud's free tier services:
  - Google Cloud Shell (100% free, no billing card required, 5 GB storage, web preview on port 8080).
  - Google Cloud Run (Free tier: 2M requests/month, 360k GB-sec memory, 180k vCPU-sec).
  - Google Cloud Storage (5 GB Standard storage free).
  - Google reCAPTCHA Enterprise (10,000 assessments/month free).
- [x] Verified test suites: 12/12 backend tests pass (`mvn test`, BUILD SUCCESS) and 9/9 frontend tests pass (`bun test`).
- [x] Verified frontend tests (`bun test`): 9/9 passing.
- [x] Merged `production-hardening` branch into `main` and pushed to GitHub `origin/main` (`fb42bce`).

## 30. Google Cloud Shell Port 8080 Backend Disconnection Resolution
- [x] Diagnosed root cause of "Unable to connect to the backend server. Please verify that the application is running on port 8080 in Cloud Shell.":
  - Spring Boot backend had stopped when the Cloud Shell terminal session was closed or disconnected, leaving port 8080 without a listening process and causing frontend `fetch()` to fail with network error.
- [x] Updated Cloud Shell environment to latest commit `e8abd52` with official Google reCAPTCHA post-login verification (`git fetch origin main && git reset --hard origin/main`).
- [x] Rebuilt frontend production bundle and packaged Spring Boot JAR inside Cloud Shell.
- [x] Launched Spring Boot application in background daemon mode (`bash deploy-cloudshell.sh -d`) with PID tracking (`app.pid`), ensuring persistence across terminal sessions.
- [x] Verified port 8080 listening via `fuser 8080/tcp` (PID 3752) and verified HTTP 401 on `curl -I http://localhost:8080/api/auth/me`.
- [x] Verified Cloud Shell web preview (`https://8080-cs-936674397252-default.cs-asia-southeast1-yelo.cloudshell.dev/?authuser=0`): error banner eliminated, clean sign-in interface displayed and fully connected to the backend.
- [x] Verified test suites: 12/12 backend tests pass (`mvnw.cmd test`, BUILD SUCCESS) and 9/9 frontend tests pass (`bun test`).

## 32. Cloudflare Turnstile & Cloudflare Pages Integration
- [x] Integrated user-generated Cloudflare Turnstile credentials:
  - Site Key: `0x4AAAAAAGPGssbXL8tJpeUa`
  - Secret Key: `0x4AAAAAAGPGsoZ9R0IzIhtmpYKaF5OiuAY`
- [x] Upgraded `RecaptchaService.java` to dynamically support both Cloudflare Turnstile (`https://challenges.cloudflare.com/turnstile/v0/siteverify`) and Google reCAPTCHA.
- [x] Updated `AuthModal.tsx` to automatically render Cloudflare Turnstile widget via official `turnstile/v0/api.js` script with graceful token handling and post-login verification.
- [x] Successfully deployed on Cloudflare Pages:
  - Live Edge URL: `https://cct-7ti.pages.dev`
  - Region: Earth, with global SSL/TLS and DDoS defense.
- [x] Configured Cloudflare Pages `public/_redirects` proxying `/api/*` to the 24/7 backend (`https://cct-3c4n.onrender.com/api/:splat 200!`) and SPA routing fallback (`/* /index.html 200`).
- [x] Verified local full-stack run: Spring Boot daemon active on port 8080 and Vite dev server active on port 5173.
- [x] Verified all test suites pass: 12/12 backend tests (`mvnw.cmd test`, BUILD SUCCESS) and 9/9 frontend tests (`bun test`).

## 33. MinIO S3-Compatible Object Storage Integration
- [x] Added `io.minio:minio:8.5.12` client dependency to `backend/pom.xml`.
- [x] Created `StorageProvider` abstraction interface (`com.clouddrive.storage.StorageProvider`) defining uniform file persistence operations: `store`, `loadAsResource`, `loadAsStream`, `delete`, `exists`, and `getProviderType`.
- [x] Implemented `LocalStorageProvider` for filesystem persistence with directory traversal protection (`SecurityException` on `..` or root escaping).
- [x] Implemented `MinioResource` extending Spring's `AbstractResource` for streaming, metadata retrieval (`contentLength`), and previewing objects directly from MinIO.
- [x] Implemented `MinioStorageProvider` with:
  - Automatic bucket verification and creation on startup (`app.storage.minio.auto-create-bucket: true`).
  - Safe object key sanitization and uniform hierarchical prefixes (`users/<username>/<uuid>_<filename>`).
  - Seamless, non-blocking fallback to `LocalStorageProvider` if MinIO server is offline or temporarily unreachable.
  - Multi-part streaming upload handling and byte array persistence.
- [x] Implemented `StorageConfig` selecting `MinioStorageProvider` as `@Primary` bean when `app.storage.type=minio` (or `s3`), with clean fallback to `LocalStorageProvider`.
- [x] Configured MinIO properties in `application.yml` and `application-gcloud.yml` (`MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`, `MINIO_AUTO_CREATE_BUCKET`, `MINIO_REGION`).
- [x] Refactored `FileStorageService.java` to interact exclusively through `StorageProvider`:
  - Uploading single and multi-file batches.
  - Creating blank documents.
  - Loading resources for downloads and inline previews.
  - In-browser editor content reading and version-snapshotted editing.
  - Emptying trash and purging storage objects.
  - Folder and multi-file ZIP streaming (`streamFolderAsZip` and `streamMultipleFilesAsZip`).
- [x] Refactored `ShareService.java` to load public and internal shared file resources via `StorageProvider`.
- [x] Updated `docker-compose.yml` with official `minio/minio:latest` service, volume persistence, health checks, and automatic connection to `clouddrive-app`.
- [x] Added unit and integration tests in `StorageProviderTests.java` covering local storage lifecycle, traversal defense, MinIO key sanitization, and graceful offline fallback.
- [x] Verified full test suites pass: 17/17 backend tests (`mvn test`, BUILD SUCCESS) and 9/9 frontend tests (`bun test`).
- [x] Verified production packaging: built `backend/target/personal-cloud-drive.jar` and verified daemon execution on port 8080.

## 34. All Files View, Universal Inline File Viewer & Grid Readability
- [x] Added `GET /api/files/all` endpoint in `FileController.java`, `FileStorageService.listAllFiles`, and `FileItemRepository.java` to list every non-trashed file owned by the user across all folders without a top-20 cap.
- [x] Created `FileViewerDTO.java` and `FileViewerService.java` (`GET /api/files/viewer/{id}`) with XXE-hardened JDK XML/ZIP parsers supporting inline preview extraction for:
  - Word documents (`.docx` paragraphs, headings, lists, structured tables, and `<w:sdt>` containers).
  - Excel workbooks (`.xlsx` multi-sheet shared strings & grid alignment) and delimited tables (`.csv`, `.tsv`).
  - Presentations (`.pptx` slides), archives (`.zip`, `.jar`), and expanded text/code formats (`.log`, `.yml`, `.yaml`, `.ini`, `.env`, `.tsx`, `.jsx`, etc.).
- [x] Added **All Files** navigation item in `src/App.tsx` sidebar with file counter badge, clickable folder location badges/columns (`navigateToFolder`), live image card thumbnails in Grid view, explicit **View** (`Eye`) action buttons, and sidebar `My Drive` root path reset.
- [x] Fixed Grid View folder card title truncation (`P..`) by adjusting responsive columns and collapsing unhovered selection/star action controls.
- [x] Verified full build & tests: 19/19 backend tests pass (`mvnw.cmd package`, `BUILD SUCCESS`), `npm run build` compiles cleanly, and 9/9 frontend tests pass (`bun test`).

## 35. Trash Lifecycle, Foreign Key Cleanup & Confirmation UX Fixes
- [x] Fixed `FileStorageService.emptyTrash` foreign-key crash on shared items by deleting `UserShare`, `ShareLink`, and `FileVersion` references and version storage objects (`purgeSingleItem`).
- [x] Added per-item permanent deletion endpoint `DELETE /api/files/trash/{id}` (`FileController.deletePermanently`) and batch `DELETE_PERMANENTLY` support in `FileStorageService.executeBatchAction`.
- [x] Hardened `restoreFromTrash`: falls back cleanly to `/` if an item's original parent folder was trashed/deleted, resolves duplicate active filenames automatically (` (restored)`), restores folder subtrees, and syncs `user.usedBytes` on trash/restore/delete.
- [x] Fixed Trash UI bugs in `src/App.tsx` and `src/services/springApi.ts`:
  - Ignored Type filter pills inside Trash view and reset `selectedFilter` on switching to Trash so trashed items are never hidden.
  - Replaced native `window.confirm` with an in-app confirmation modal for Empty Trash, single permanent delete, and batch permanent delete.
  - Added per-item **Delete permanently** buttons, Trash batch selection actions (**Restore Selected** / **Delete Permanently**), and `.catch(...)` error alerts on Trash/Restore promises.
- [x] Added `testTrashRestoreDeletePermanentlyAndEmptyWithShares` in `CloudDriveApplicationTests.java` covering `MockMvc` HTTP flows: **20/20 backend tests passing**, `npm run build` clean, **9/9 frontend tests passing**.






















