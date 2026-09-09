@echo off
setlocal EnableExtensions

echo ============================================================
echo   EtherHack Installer
echo ============================================================
echo.

if not exist projectzomboid.jar (
    echo [ERROR] projectzomboid.jar not found.
    echo         Put this file and EtherHack-*.jar in the game root folder,
    echo         next to ProjectZomboid64.exe, and run it again.
    pause
    exit /b 1
)

echo NOTE: Close the game before running this installer.
echo.

echo [1/3] Removing old injected game classes in the zombie folder...
if exist zombie rmdir /s /q zombie
if exist zombie (
    echo [ERROR] Could not remove the zombie folder. Close the game and try again.
    pause
    exit /b 1
)

echo [2/3] Removing old EtherHack files...
rem User settings (key binds etc.) live in EtherHack\config and must survive
rem reinstalls -- back them up and restore them after the fresh install.
set "ETH_CFG_BAK=%TEMP%\EtherHack_config_bak"
if exist "%ETH_CFG_BAK%" rmdir /s /q "%ETH_CFG_BAK%"
if exist EtherHack\config (
    robocopy EtherHack\config "%ETH_CFG_BAK%" /e /nfl /ndl /njh /njs >nul
    if errorlevel 8 (
        echo [ERROR] Could not back up EtherHack\config. Close the game and try again.
        pause
        exit /b 1
    )
)
if exist EtherHack rmdir /s /q EtherHack
if exist EtherHack (
    echo [ERROR] Could not remove the EtherHack folder. Close the game and try again.
    pause
    exit /b 1
)

echo [3/3] Installing EtherHack...

set "ETH_JAR="
for %%f in (EtherHack-*.jar) do if not defined ETH_JAR set "ETH_JAR=%%f"
if not defined ETH_JAR (
    echo [ERROR] EtherHack-*.jar not found in this folder.
    pause
    exit /b 1
)
echo Using jar: %ETH_JAR%

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
echo Using java: %JAVA_CMD%

"%JAVA_CMD%" -jar "%ETH_JAR%" --install

if not exist EtherHack (
    echo.
    echo [ERROR] Installation failed: the EtherHack folder was not created.
    echo         Check the messages above for details.
    pause
    exit /b 1
)

rem Restore user settings (key binds etc.) over the fresh defaults.
if exist "%ETH_CFG_BAK%" (
    robocopy "%ETH_CFG_BAK%" EtherHack\config /e /nfl /ndl /njh /njs >nul
    if errorlevel 8 (
        echo [WARN] Could not restore EtherHack\config - user settings were lost.
    ) else (
        rmdir /s /q "%ETH_CFG_BAK%"
    )
)

echo.
echo Installation completed. You can now start the game.
pause
