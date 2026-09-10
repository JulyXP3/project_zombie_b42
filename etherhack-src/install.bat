@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ============================================================
echo   modcore Installer / Uninstaller (merged)
echo ============================================================
echo.

if not exist projectzomboid.jar (
    echo [ERROR] projectzomboid.jar not found - wrong folder, or the game jar is missing.
    echo         If it was deleted by mistake: Steam - Project Zomboid - Properties -
    echo         Installed Files - Verify integrity of game files.
    echo         NEVER delete projectzomboid.jar - that is the game itself!
    pause
    exit /b 1
)

set "MC_JAR="
for %%f in (modcore-*.jar) do if not defined MC_JAR set "MC_JAR=%%f"
if defined MC_JAR goto INSTALL
goto UNINSTALL

REM ============================================================
REM   INSTALL MODE (modcore-*.jar found in this folder)
REM ============================================================
:INSTALL
echo Mode: INSTALL (found %MC_JAR%)
echo NOTE: Close the game before running this installer.
echo.

echo [1/3] Removing old injected game classes (zombie folder)...
if exist zombie rmdir /s /q zombie
if exist zombie (
    echo [ERROR] Could not remove the zombie folder. Close the game and try again.
    pause
    exit /b 1
)

echo [2/3] Installing modcore (encrypted payload + bootstrap stub)...
set "JAVA_CMD="
if exist "jre64\bin\java.exe" set "JAVA_CMD=jre64\bin\java.exe"
if not defined JAVA_CMD (
    where java >nul 2>nul
    if not errorlevel 1 set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    echo [ERROR] Java was not found. Make sure the game's jre64 folder is present
    echo         or that java is on your PATH.
    pause
    exit /b 1
)
"%JAVA_CMD%" -jar "%MC_JAR%" --install

if not exist "zombie\coreboot.class" (
    echo.
    echo [ERROR] Installation failed: bootstrap stub was not created.
    echo         Check the messages above for details. The jar was kept for retry.
    pause
    exit /b 1
)
if not exist "%USERPROFILE%\Zomboid\modcore.bin" (
    echo.
    echo [ERROR] Installation failed: encrypted payload was not created.
    echo         Check the messages above for details. The jar was kept for retry.
    pause
    exit /b 1
)

echo [3/3] Cleaning up installer jar...
del /f /q "%MC_JAR%"
if exist "%MC_JAR%" (
    echo [WARN] Could not delete %MC_JAR% - please delete it manually.
)

echo.
echo Installation completed. You can now start the game.
echo.
echo   - The installer jar was deleted automatically.
echo   - Keep THIS install.bat: run it again later (with no jar present)
echo     to uninstall modcore completely.
echo   - NEVER delete projectzomboid.jar - that is the game itself!
echo.
pause
exit /b 0

REM ============================================================
REM   UNINSTALL MODE (no modcore-*.jar in this folder)
REM ============================================================
:UNINSTALL
echo Mode: UNINSTALL (no modcore-*.jar found)
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

echo.
echo Uninstall completed. The game is now fully vanilla.
echo.
echo Kept on purpose (harmless, reusable):
echo   - %%USERPROFILE%%\Zomboid\modcore\config\  (key binds and settings)
echo Delete that folder too if you want a complete wipe.
echo You may now delete this install.bat by hand.
pause
exit /b 0
