Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host "    Personal Cloud Drive" -ForegroundColor Yellow
Write-Host "    Frontend: React 18 (Vite + Tailwind CSS)" -ForegroundColor White
Write-Host "    Backend:  Spring Boot 3 (Java 21, Spring Security, JWT, JPA)" -ForegroundColor White
Write-Host "===================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[1/2] Starting Spring Boot Backend on Port 8080..." -ForegroundColor Green
Start-Process cmd -ArgumentList "/k title Spring Boot Backend && java @backend/run.args"

Write-Host "[2/2] Starting React Frontend on Port 5173..." -ForegroundColor Green
$cmdFrontend = if (Get-Command bun -ErrorAction SilentlyContinue) { "bun run dev:ui" } else { "npm run dev:ui -- --host 127.0.0.1" }
Start-Process cmd -ArgumentList "/k title React Frontend && $cmdFrontend"

Start-Sleep -Seconds 5
Write-Host "Opening http://localhost:5173 in browser..." -ForegroundColor Yellow
Start-Process "http://localhost:5173"

Write-Host ""
Write-Host "Personal Cloud Drive is running!" -ForegroundColor Green
Write-Host "  - Frontend: http://localhost:5173" -ForegroundColor White
Write-Host "  - Backend API: http://localhost:8080/api" -ForegroundColor White
Write-Host "  - H2 Database Console: http://localhost:8080/h2-console" -ForegroundColor White
