# cct - Personal Cloud Drive

A modern, full-stack, self-hosted personal cloud storage and file management platform built with **Spring Boot 3 (Java 21)** and **React 18**.

---

## Features

- **Authentication & Security:** Real registration & login with BCrypt password hashing and stateless JWT tokens.
- **Storage Isolation:** Dedicated, guarded workspaces per user with path traversal (CWE-22) prevention.
- **Quota & Categorical Analytics:** 500 MB default storage quota with category breakdown (Documents, Images, Video, Audio, Code, Archives, Others).
- **Google Drive Parity:**
  - Dynamic hierarchical folder tree with breadcrumb navigation.
  - Folder color-coding (8 customizable palettes).
  - Multi-file drag-and-drop upload and blank document authoring (.txt, .md, .js, .html, .json).
  - Folder ZIP streaming (`streamFolderAsZip`) and multi-select batch ZIP streaming (`streamMultipleFilesAsZip`).
  - File Version History with revision tracking and 1-click snapshot rollback.
  - In-browser code and markdown editor with live editing.
  - Multimedia lightbox previews (images, HTML5 video player, HTML5 audio player, and PDF iframe embed).
  - Recycle Bin (soft-delete) with individual restoration and permanent empty.
  - Public expiring share links with anonymous download capabilities.
  - User-to-user collaboration ("Shared with Me") with VIEW and EDIT permissions.
  - Audit activity logs with relative timestamps.

---

## Tech Stack

- **Frontend:** React 18, Vite, Tailwind CSS, Lucide Icons.
- **Backend:** Spring Boot 3.3.5, Java 21, Spring Security, Spring Data JPA, H2 Database.

---

## Quick Start (Windows)

Double-click `start-drive.bat` or run in PowerShell:
```powershell
.\start-drive.ps1
```

- **Frontend:** http://localhost:5173
- **Backend API:** http://localhost:8080/api
- **H2 Console:** http://localhost:8080/h2-console

### Default Admin Credentials:
- **Username:** `admin`
- **Password:** `admin123`
*(Or click "Create Account" on the login screen to register your own account).*

---

## License
MIT License.

# cct
