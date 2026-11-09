Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Starting OpsPilot Platform (Backend + Frontend)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$root = $PSScriptRoot

Write-Host "`n[1/3] Launching Spring Boot Backend on http://localhost:8080..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\backend'; .\mvnw.cmd spring-boot:run"

Write-Host "[2/3] Launching Vite React Frontend on http://localhost:5173..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$root\frontend'; npm.cmd run dev"

Write-Host "[3/3] Waiting 6 seconds for servers to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 6

Write-Host "Opening http://localhost:5173 in default browser..." -ForegroundColor Cyan
Start-Process "http://localhost:5173"

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "OpsPilot is running!" -ForegroundColor Green
Write-Host "  Frontend UI:  http://localhost:5173"
Write-Host "  Backend API:  http://localhost:8080"
Write-Host "  Swagger Docs: http://localhost:8080/swagger-ui.html"
Write-Host "========================================================" -ForegroundColor Cyan
