@echo off
title Personal Cloud Drive Launcher
echo ===================================================================
echo     Personal Cloud Drive
echo     Frontend: React 18 (Vite + Tailwind CSS)
echo     Backend:  Spring Boot 3 (Java 21, Spring Security, JWT, JPA)
echo ===================================================================
echo.
echo [1/2] Starting Spring Boot Backend on Port 8080...
if exist "backend\target\personal-cloud-drive.jar" (
    start "Cloud Drive - Spring Boot Backend (Port 8080)" cmd /k "java -jar backend\target\personal-cloud-drive.jar"
) else (
    start "Cloud Drive - Spring Boot Backend (Port 8080)" cmd /k "cd backend && mvnw.cmd spring-boot:run"
)

echo [2/2] Starting React Frontend on Port 5173...
where bun >nul 2>&1
if %errorlevel% equ 0 (
    start "Cloud Drive - React Frontend (Port 5173)" cmd /k "bun run dev:ui"
) else (
    start "Cloud Drive - React Frontend (Port 5173)" cmd /k "npm run dev:ui -- --host 127.0.0.1"
)

echo.
echo Waiting 6 seconds for services to initialize...
timeout /t 6 /nobreak >nul

echo Opening browser at http://localhost:5173 ...
start http://localhost:5173

echo.
echo ===================================================================
echo   System running!
echo   - React Frontend: http://localhost:5173
echo   - Spring Boot API: http://localhost:8080/api
echo   - Full-stack unified app: http://localhost:8080
echo.
echo   Click 'Create Account' in the UI to register your account.
echo ===================================================================
pause
