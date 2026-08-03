@echo off
REM ============================================================
REM Nhu Villas Backend Deployment Script (Windows)
REM ============================================================

setlocal enabledelayedexpansion

set RED=[91m
set GREEN=[92m
set YELLOW=[93m
set NC=[0m

echo.
echo %GREEN%========================================%NC%
echo %GREEN%   Nhu Villas Backend Deployment%NC%
echo %GREEN%========================================%NC%
echo.

cd /d "%~dp0"

REM Check environment
echo %YELLOW%Checking environment...%NC%
if "%SPRING_DATASOURCE_URL%"=="" (
    echo %RED%ERROR: SPRING_DATASOURCE_URL not set%NC%
    exit /b 1
)

if "%APP_ADMIN_PASSWORD%"=="" (
    echo %RED%ERROR: APP_ADMIN_PASSWORD must be set in production%NC%
    exit /b 1
)

if not "%STRIPE_SECRET_KEY%"=="" (
    echo %GREEN%✓ Stripe configured%NC%
) else (
    echo %YELLOW%WARNING: STRIPE_SECRET_KEY not set - payments will use mock mode%NC%
)

echo %GREEN%✓ Environment check passed%NC%

REM Build application
echo.
echo %YELLOW%Building application...%NC%
call mvnw.cmd clean package -Dspring.profiles.active=prod -DskipTests

if not exist "target\hotel-backend.jar" (
    echo %RED%ERROR: Build failed%NC%
    exit /b 1
)

echo %GREEN%✓ Build successful%NC%

REM Run migrations
echo.
echo %YELLOW%Running database migrations...%NC%
call mvnw.cmd flyway:migrate -Dspring.profiles.active=prod
echo %GREEN%✓ Migrations completed%NC%

REM Start application
echo.
echo %YELLOW%Starting application...%NC%

REM Kill existing process
for /f "tokens=*" %%i in ('jps -l ^| findstr hotel-backend') do (
    echo %YELLOW%Stopping existing process...%%i%NC%
    for /f "tokens=1" %%p in ("%%i") do taskkill /F /PID %%p
)

REM Start in background
start /B cmd /c "java -jar target\hotel-backend.jar --spring.profiles.active=prod > logs\app.log 2>&1"

echo %GREEN%✓ Application started%NC%
echo %YELLOW%Waiting for startup...%NC%

REM Wait and check health
set ATTEMPTS=0
:wait_loop
set /a ATTEMPTS+=1
timeout /t 3 /nobreak >nul
curl -sf http://localhost:8080/actuator/health >nul 2>&1
if !errorlevel!==0 goto health_ok
if %ATTEMPTS% LSS 30 goto wait_loop

echo %RED%ERROR: Application failed to start%NC%
type logs\app.log
exit /b 1

:health_ok
echo %GREEN%✓ Application is healthy%NC%

echo.
echo %GREEN%========================================%NC%
echo %GREEN%   Deployment Complete!%NC%
echo %GREEN%========================================%NC%
echo Logs: logs\app.log
echo.

endlocal
