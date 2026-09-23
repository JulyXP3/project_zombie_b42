@echo off
setlocal EnableExtensions
where javac >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [CarKill] JDK 25+ not found. Game-only machines: just run install.bat (no build needed).
    pause
    exit /b 1
)
cd /d "%~dp0"
pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
set "CODE=%ERRORLEVEL%"
if not "%CODE%"=="0" echo [CarKill] Build failed.
pause
exit /b %CODE%
