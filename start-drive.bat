@echo off
title Personal Cloud Drive Launcher
echo ===================================================================
echo     Personal Cloud Drive
echo     Frontend: React 18 (Vite + Tailwind CSS)
echo     Backend:  Spring Boot 3 (Java 21, Spring Security, JWT, JPA)
echo ===================================================================
echo.
echo [1/2] Starting Spring Boot Backend on Port 8080...
start "Cloud Drive - Spring Boot Backend (Port 8080)" cmd /k "java @backend/run.args"

echo [2/2] Starting React Frontend on Port 5173...
where bun >nul 2>&1
if %errorlevel% equ 0 (
    start "Cloud Drive - React Frontend (Port 5173)" cmd /k "bun run dev:ui"
) else (
    start "Cloud Drive - React Frontend (Port 5173)" cmd /k "npm run dev:ui -- --host 127.0.0.1"
)

echo.
echo Waiting 5 seconds for services to initialize...
timeout /t 5 /nobreak >nul

echo Opening browser at http://localhost:5173 ...
start http://localhost:5173

echo.
echo ===================================================================
echo   System running!
echo   - React Frontend: http://localhost:5173
echo   - Spring Boot API: http://localhost:8080/api
echo   - H2 Database Console: http://localhost:8080/h2-console
echo.
echo   Default Admin: username: admin    password: admin123
echo   Or click 'Create Account' in the UI to register your own account.
echo ===================================================================
pause
