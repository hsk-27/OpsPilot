@echo off
echo ========================================================
echo Starting OpsPilot Platform (Backend + Frontend)
echo ========================================================
echo.

cd /d "%~dp0"

echo [1/3] Launching Spring Boot Backend on http://localhost:8080 ...
start "OpsPilot Backend" cmd /k "cd /d "%~dp0backend" && .\mvnw.cmd spring-boot:run"

echo [2/3] Launching Vite React Frontend on http://localhost:5173 ...
start "OpsPilot Frontend" cmd /k "cd /d "%~dp0frontend" && npm.cmd run dev"

echo [3/3] Waiting a few seconds and opening OpsPilot in your browser...
timeout /t 5 /nobreak >nul
start http://localhost:5173

echo.
echo ========================================================
echo OpsPilot is running!
echo   Frontend UI:  http://localhost:5173
echo   Backend API:  http://localhost:8080
echo   Swagger Docs: http://localhost:8080/swagger-ui.html
echo ========================================================
echo.
