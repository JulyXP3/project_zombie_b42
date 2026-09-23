<# :
@echo off
setlocal EnableExtensions
cd /d "%~dp0"
where pwsh >nul 2>nul
if %ERRORLEVEL%==0 ( set "PS=pwsh" ) else ( set "PS=powershell" )
set "HERE=%~dp0"
%PS% -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
set "CODE=%ERRORLEVEL%"
if not "%CODE%"=="0" echo [CarKill] 
Install
 failed.
pause
exit /b %CODE%
: #>

