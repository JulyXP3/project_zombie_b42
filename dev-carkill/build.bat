@echo off
setlocal EnableExtensions
cd /d "%~dp0"
pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
set "CODE=%ERRORLEVEL%"
if not "%CODE%"=="0" echo [CarKill] Build failed.
pause
exit /b %CODE%
