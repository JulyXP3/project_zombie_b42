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

rem Without a wait the console closes as soon as the build ends, so a double-click user
rem cannot read the result. Automation (CI, scripts, tests) sets MODCORE_NO_PAUSE=1.
rem Do not inspect %cmdcmdline% here: it contains "&&" and re-parsing it repeats this
rem file as a command (observed as a setlocal recursion abort).
set "PAUSE_AT_END=1"
if defined MODCORE_NO_PAUSE set "PAUSE_AT_END="

if not exist "gradlew.bat" goto NO_SOURCE
if not exist "build.gradle.kts" goto NO_SOURCE
if not exist "lib\projectzomboid.jar" (
    echo [ERROR] Copy projectzomboid.jar into the source lib folder before building.
    set "RESULT=1"
    goto DONE
)

rem Arguments: [--fast] ["path to ProjectZomboid"].
rem --fast skips the three deployment life-cycle tests; that JAR is a local test build.
set "FAST="
set "ARG_GAME_DIR="
:ARGS
if "%~1"=="" goto ARGS_DONE
if /i "%~1"=="--fast" (
    set "FAST=1"
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

if defined FAST (
    pwsh -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0run-build.ps1" -JavaHome "%BUILD_JAVA_HOME%" -GameDir "%BUILD_GAME_DIR%" -Mode Full -SkipDeploymentTests
) else (
    pwsh -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0run-build.ps1" -JavaHome "%BUILD_JAVA_HOME%" -GameDir "%BUILD_GAME_DIR%" -Mode Full
)
set "RESULT=%ERRORLEVEL%"
if not "%RESULT%"=="0" (
    echo [ERROR] Build or tests failed. A generated JAR does not mean all features passed.
    echo Review build\reports\tests\test\index.html and the output above.
    if exist "build\modcore-*.jar" echo The JAR was still produced under build\ - do not ship it before the failures are explained.
    goto DONE
)
if defined FAST (
    echo Fast build finished: the deployment life-cycle tests were skipped.
    echo build\modcore-*.jar is a LOCAL TEST build, not a verified delivery package.
    echo Run build.bat without --fast before shipping anything.
) else (
    echo Build and tests passed.
    echo Copy exactly these delivery files to the game PC:
    echo   build\modcore-*.jar
    echo   install.bat
    echo   uninstall.bat
    echo Keep all three files for offline installation and removal.
    echo The game PC must install a Windows x64 full JDK 25 and set JAVA_HOME or PATH.
)
set "RESULT=0"
goto DONE

:NO_SOURCE
echo [ERROR] Run build.bat from the source checkout with gradlew.bat and build.gradle.kts.
echo A game-only PC uses install.bat, not build.bat.
set "RESULT=1"
goto DONE

:NO_PWSH
echo [ERROR] PowerShell 7 (pwsh.exe) is required to check the build JDK and prepare the native test runtime.
echo Install PowerShell 7 and make sure "pwsh" is on PATH. Windows PowerShell 5.1 is not supported.
set "RESULT=1"
goto DONE

:NO_HELPER
echo [ERROR] The build helper scripts are missing next to build.bat.
echo Expected run-build.ps1, check-build-jdk.ps1 and prepare-native-fixture.ps1 in the source checkout.
set "RESULT=1"

:DONE
if defined PAUSE_AT_END (
    echo.
    echo Press any key to close this window. Automation can set MODCORE_NO_PAUSE=1 to skip this wait.
    pause >nul
)
exit /b %RESULT%
