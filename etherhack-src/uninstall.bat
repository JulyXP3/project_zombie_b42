@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"

rem ================================================================
rem This script checks only these three things:
rem   1) projectzomboid.jar is present in this folder (never changed)
rem   2) which uninstaller to run: the HIGHEST modcore-*.jar here,
rem      or the stored copy in %USERPROFILE%\Zomboid\modcore-agent
rem   3) a Windows x64 full JDK 25: JAVA_HOME first, then PATH
rem Nothing else is verified. Only files recorded by the installer
rem are removed, and the original launch batch files are restored.
rem ================================================================

if not exist "projectzomboid.jar" (
    echo [ERROR] Check 1 failed: projectzomboid.jar is missing.
    echo Run this script from the game folder. Nothing was changed.
    echo Never delete projectzomboid.jar - it is the original game.
    exit /b 1
)

set "MC_JAR="
set "MC_FIRST="
set "MC_COUNT=0"
set "MC_HA=0"
set "MC_HB=0"
set "MC_HC=0"
for %%F in (modcore-*.jar) do (
    if not defined MC_FIRST set "MC_FIRST=%%~fF"
    set /a MC_COUNT+=1 >nul
    call :CONSIDER "%%~fF" "%%~nF"
)
if defined MC_JAR goto HAVE_JAR
if defined MC_FIRST set "MC_JAR=%MC_FIRST%"
if defined MC_JAR goto HAVE_JAR
set "MC_STORED=%USERPROFILE%\Zomboid\modcore-agent\installer.jar"
if exist "%MC_STORED%" set "MC_JAR=%MC_STORED%"
if not defined MC_JAR (
    echo [INFO] Nothing to uninstall: no modcore-*.jar here and no stored installer.
    echo If you did install ModCore, put the three delivery files back and run this again.
    exit /b 0
)
:HAVE_JAR
if %MC_COUNT% GTR 1 echo [INFO] %MC_COUNT% installer files found; using the highest version.
echo Using uninstaller: %MC_JAR%

set "MC_JAVA="
if defined JAVA_HOME (
    set "MC_JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    for %%J in (java.exe) do set "MC_JAVA=%%~$PATH:J"
)
if not defined MC_JAVA (
    echo [ERROR] Check 3 failed: no Java found.
    echo Set JAVA_HOME to a Windows x64 full JDK 25, or add its bin folder to PATH.
    exit /b 1
)
if not exist "%MC_JAVA%" (
    echo [ERROR] Check 3 failed: no Windows x64 full JDK 25 at "%MC_JAVA%".
    echo An explicitly set JAVA_HOME is used as-is and is not replaced by PATH.
    exit /b 1
)
"%MC_JAVA%" -cp "%MC_JAR%" modcore.attach.JdkRequirement
if errorlevel 1 (
    echo [ERROR] Check 3 failed: the Java above is not a Windows x64 full JDK 25.
    exit /b 1
)

rem The stored copy is the file being deleted, so run a temporary copy of it instead.
if not "%MC_JAR%"=="%MC_STORED%" goto RUN
set "MC_RUN=%USERPROFILE%\Zomboid\modcore-removal-%RANDOM%-%RANDOM%"
mkdir "%MC_RUN%" >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Cannot create a temporary removal folder.
    exit /b 1
)
copy /b "%MC_STORED%" "%MC_RUN%\installer.jar" >nul
if errorlevel 1 (
    echo [ERROR] Cannot copy the stored installer.
    rd /s /q "%MC_RUN%" >nul 2>nul
    exit /b 1
)
set "MC_JAR=%MC_RUN%\installer.jar"
:RUN

"%MC_JAVA%" -jar "%MC_JAR%" --uninstall-agent --game-dir "%CD%" --user-home "%USERPROFILE%" --yes %*
set "RESULT=%ERRORLEVEL%"
if defined MC_RUN if exist "%MC_RUN%" rd /s /q "%MC_RUN%" >nul 2>nul
if not "%RESULT%"=="0" (
    echo [ERROR] Uninstall did not complete. Read the reason above; backups were preserved.
    exit /b %RESULT%
)
exit /b 0

:CONSIDER
rem %1 = full path, %2 = file name without extension
set "MC_A="
set "MC_B="
set "MC_C="
for /f "tokens=2,3,4 delims=.-" %%a in ("%~2") do (
    set "MC_A=%%a"
    set "MC_B=%%b"
    set "MC_C=%%c"
)
rem The version parts must be plain digits; otherwise this file is not compared.
for /f "delims=0123456789" %%x in ("%MC_A%%MC_B%%MC_C%") do goto :eof
if not defined MC_A goto :eof
if not defined MC_B goto :eof
if not defined MC_C goto :eof
if %MC_A% GTR %MC_HA% goto :TAKE
if %MC_A% LSS %MC_HA% goto :eof
if %MC_B% GTR %MC_HB% goto :TAKE
if %MC_B% LSS %MC_HB% goto :eof
if %MC_C% GTR %MC_HC% goto :TAKE
if %MC_C% LSS %MC_HC% goto :eof
:TAKE
set "MC_JAR=%~1"
set "MC_HA=%MC_A%"
set "MC_HB=%MC_B%"
set "MC_HC=%MC_C%"
goto :eof
