@echo off
setlocal EnableExtensions
cd /d "%~dp0backend-management-service"

echo.
echo [%~nx0] Starting Spring Boot API on http://localhost:8080
echo Base services URL: http://localhost:8080/management/api/services
echo See docs at backend-management-service\docs\services-lifecycle-api.md
echo Keep this window open while you use Postman. Press Ctrl+C to stop the server.
echo.

mvn spring-boot:run
if errorlevel 1 (
    echo.
    echo ERROR: Maven failed. Is JDK 21 installed and mvn on PATH?
    echo.
    pause
    exit /b 1
)

endlocal
