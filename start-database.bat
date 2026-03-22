@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo [%~nx0] Starting PostgreSQL from docker-compose.yml ...
echo.

docker compose up -d
if errorlevel 1 (
    docker-compose up -d
)
if errorlevel 1 (
    echo.
    echo ERROR: Could not start containers. Is Docker Desktop running?
    echo.
    pause
    exit /b 1
)

echo.
echo Database container should be up. Check with: docker compose ps
echo PostgreSQL: localhost:5432  database=tourpresence  user=synexis
echo.
echo Postman uses port 8080 ^(Spring Boot API^), NOT 5432.
echo Start the API in another window: run start-backend.bat
echo.
pause
endlocal
