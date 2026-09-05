@echo off
REM ============================================================
REM Nhu Villas - Quick Start Script (Windows)
REM ============================================================

echo.
echo  ============================================================
echo     Nhu Villas - Development Setup
echo  ============================================================
echo.

REM Check Docker
echo [1/4] Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running. Please start Docker Desktop first.
    pause
    exit /b 1
)

REM Check Docker is running
docker ps >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker daemon is not running. Please start Docker Desktop.
    echo Wait for the Docker icon in the system tray to turn green.
    pause
    exit /b 1
)
echo OK: Docker is running

REM Check .env file
echo.
echo [2/4] Checking configuration...
if not exist ".env" (
    echo WARNING: .env file not found. Creating from template...
    copy ".env.production" ".env"
)
echo OK: Configuration loaded

REM Build and start
echo.
echo [3/4] Building and starting services...
echo This may take a few minutes on first run...
echo.

docker-compose up -d --build

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to start services.
    echo Check logs with: docker-compose logs
    pause
    exit /b 1
)

REM Wait for services
echo.
echo [4/4] Waiting for services to be ready...
echo.

REM Check backend health
set max_attempts=60
set attempt=0

:wait_backend
set /a attempt+=1
if %attempt% gtr %max_attempts% (
    echo ERROR: Backend failed to start after %max_attempts% seconds
    echo Check logs with: docker-compose logs backend
    pause
    exit /b 1
)

timeout /t 3 /nobreak >nul
curl -s http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% neq 0 (
    echo Waiting for backend... (%attempt%/%max_attempts%)
    goto wait_backend
)

echo OK: Backend is ready!

REM Final status
echo.
echo ============================================================
echo     All services started successfully!
echo ============================================================
echo.
echo   Frontend:  http://localhost:3000
echo   Backend:   http://localhost:8080
echo   API:       http://localhost:8080/api/v1
echo   H2 Console: http://localhost:8080/h2-console
echo.
echo   Admin Login: admin / Admin123!
echo.
echo   To view logs: docker-compose logs -f
echo   To stop:      docker-compose down
echo   To restart:   docker-compose restart
echo.
echo ============================================================
echo.

pause
