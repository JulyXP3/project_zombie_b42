@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ============================================================
echo   modcore Uninstaller (standalone)
echo ============================================================
echo.
echo This removes the injected game classes and the unpacked modcore
echo files, restoring a fully vanilla game folder.
echo.

if not exist projectzomboid.jar (
    echo [ERROR] projectzomboid.jar not found - wrong folder, or the game jar is missing.
    echo         Run this script from the Project Zomboid game folder - the one that
    echo         contains projectzomboid.jar.
    echo         If the jar was deleted by mistake: Steam - Project Zomboid - Properties -
    echo         Installed Files - Verify integrity of game files.
    echo         NEVER delete projectzomboid.jar - that is the game itself!
    pause
    exit /b 1
)

echo NOTE: Close the game before running this uninstaller.
echo.

echo [1/4] Removing injected game classes (zombie folder)...
if exist zombie rmdir /s /q zombie
if exist zombie (
    echo [ERROR] Could not remove the zombie folder. Close the game and try again.
    pause
    exit /b 1
)

echo [2/4] Removing unpacked modcore folder...
if exist modcore rmdir /s /q modcore
if exist modcore (
    echo [ERROR] Could not remove the modcore folder. Close the game and try again.
    pause
    exit /b 1
)

echo [3/4] Removing encrypted payload (%%USERPROFILE%%\Zomboid\modcore.bin)...
if exist "%USERPROFILE%\Zomboid\modcore.bin" del /f /q "%USERPROFILE%\Zomboid\modcore.bin"
if exist "%USERPROFILE%\Zomboid\modcore.bin" (
    echo [WARN] Could not delete modcore.bin - delete it manually later.
)

echo [4/4] Removing old-version leftovers (EtherHack folder)...
if exist EtherHack rmdir /s /q EtherHack
if exist EtherHack (
    echo [WARN] Could not remove the EtherHack folder - remove it manually later.
)
REM Legacy cleanup: old versions wrote logs into the game folder; logs now live in
REM %USERPROFILE%\Zomboid\modcore\logs. Remove leftover game-folder logs.
if exist "logs\modcore_*.log" del /f /q "logs\modcore_*.log" >nul 2>nul
if exist logs rmdir "logs" >nul 2>nul

echo.
echo Uninstall completed. The game is now fully vanilla.
echo.
echo Kept on purpose (harmless, reusable):
echo   - %%USERPROFILE%%\Zomboid\modcore\config\  (key binds and settings)
echo Delete that folder too if you want a complete wipe.
echo.
echo   - NEVER delete projectzomboid.jar - that is the game itself!
echo.
pause
exit /b 0
