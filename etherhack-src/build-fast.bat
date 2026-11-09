@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"

rem ============================================================
rem Fill in these two roots once. Do not point at bin or jre64.
rem ============================================================
set "BUILD_JAVA_HOME=E:\JDK25"
set "BUILD_GAME_DIR=E:\Steam\steamapps\common\ProjectZomboid"
rem ============================================================

rem Automation overrides. An invalid explicit value stops the build; it never
rem falls back to JAVA_HOME, PATH or another configured path.
if defined PZ_GAME_DIR set "BUILD_GAME_DIR=%PZ_GAME_DIR%"
if defined MODCORE_BUILD_GAME_DIR set "BUILD_GAME_DIR=%MODCORE_BUILD_GAME_DIR%"
if defined MODCORE_BUILD_JAVA_HOME set "BUILD_JAVA_HOME=%MODCORE_BUILD_JAVA_HOME%"

rem Minimal local build: compile and package only. The full build prepares the
rem game-specific test runtime; the installer prepares and verifies the loadout
rem on installation. No tests or prebuilt bin are run here.
if not exist "gradlew.bat" goto NO_SOURCE
if not exist "build.gradle.kts" goto NO_SOURCE
if not exist "lib\projectzomboid.jar" (
    echo [ERROR] Copy projectzomboid.jar into the source lib folder before building.
    set "RESULT=1"
    goto DONE
)

rem Arguments are accepted for compatibility: [--fast] ["path to ProjectZomboid"].
rem This entry never tests and never prepares a native fixture.
set "ARG_GAME_DIR="
:ARGS
if "%~1"=="" goto ARGS_DONE
if /i "%~1"=="--fast" (
    shift
    goto ARGS
)
set "ARG_GAME_DIR=%~1"
shift
goto ARGS
:ARGS_DONE
if defined ARG_GAME_DIR set "BUILD_GAME_DIR=%ARG_GAME_DIR%"

set "RESULT=1"
where pwsh >nul 2>nul
if errorlevel 1 goto NO_PWSH
if not exist "%~dp0check-build-jdk.ps1" goto NO_HELPER
if not exist "%~dp0prepare-native-fixture.ps1" goto NO_HELPER
if not exist "%~dp0run-build.ps1" goto NO_HELPER

pwsh -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0run-build.ps1" -JavaHome "%BUILD_JAVA_HOME%" -GameDir "%BUILD_GAME_DIR%" -Mode Fast
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
    echo [ERROR] Fast build failed. Do not use a partial output.
    goto DONE
)
echo Fast build finished: all tests and game-specific loadout preparation were skipped.
echo build\modcore-*.jar is a LOCAL TEST build, not a verified delivery package.
echo No prebuilt modcore.bin was generated. The installer prepares it during installation.
echo Run build.bat for full validation.
goto DONE

:NO_SOURCE
echo [ERROR] Run build-fast.bat from the source checkout with gradlew.bat and build.gradle.kts.
echo A game-only PC uses install.bat, not build-fast.bat.
set "RESULT=1"
goto DONE

:NO_PWSH
echo [ERROR] PowerShell 7 (pwsh.exe) is required to check the build JDK.
echo Install PowerShell 7 and make sure "pwsh" is on PATH. Windows PowerShell 5.1 is not supported.
set "RESULT=1"
goto DONE

:NO_HELPER
echo [ERROR] The build helper scripts run-build.ps1, check-build-jdk.ps1 and prepare-native-fixture.ps1 must be next to build-fast.bat.
set "RESULT=1"

:DONE
if not defined MODCORE_NO_PAUSE (
    echo.
    echo Press any key to close this window. Automation can set MODCORE_NO_PAUSE=1 to skip this wait.
    pause >nul
)
exit /b %RESULT%
