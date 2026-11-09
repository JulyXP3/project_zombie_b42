$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$src = 'src\main\java\carkill'
$lib = 'lib'
$work = 'build'
$classes = Join-Path $work 'classes'
$dist = 'dist'
$jar = Join-Path $dist 'car_kill.jar'
$gameJar = Join-Path $PSScriptRoot '..\etherhack-src\lib\projectzomboid.jar'

if (-not (Test-Path $gameJar)) {
    throw "missing compile dependency: $gameJar"
}

$asm = @(
    @{ Name = 'asm-9.9.1.jar'; Url = 'https://repo1.maven.org/maven2/org/ow2/asm/asm/9.9.1/asm-9.9.1.jar' },
    @{ Name = 'asm-tree-9.9.1.jar'; Url = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-tree/9.9.1/asm-tree-9.9.1.jar' },
    @{ Name = 'asm-commons-9.9.1.jar'; Url = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-commons/9.9.1/asm-commons-9.9.1.jar' }
)
# asm-util / asm-analysis: only for offline TransformCheck, never packaged into the agent fat-jar.
$verifyLibs = @(
    @{ Name = 'asm-util-9.9.1.jar'; Url = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-util/9.9.1/asm-util-9.9.1.jar' },
    @{ Name = 'asm-analysis-9.9.1.jar'; Url = 'https://repo1.maven.org/maven2/org/ow2/asm/asm-analysis/9.9.1/asm-analysis-9.9.1.jar' }
)
New-Item -ItemType Directory -Force -Path $lib, $classes, $dist | Out-Null
foreach ($item in $asm) {
    $path = Join-Path $lib $item.Name
    if (-not (Test-Path $path)) {
        Write-Host "[CarKill] download $($item.Name)"
        Invoke-WebRequest -Uri $item.Url -OutFile $path
    }
}
foreach ($item in $verifyLibs) {
    $path = Join-Path $lib $item.Name
    if (-not (Test-Path $path)) {
        Write-Host "[CarKill] download $($item.Name) (verify only)"
        Invoke-WebRequest -Uri $item.Url -OutFile $path
    }
}

if (Test-Path $classes) {
    Remove-Item -Recurse -Force $classes
}
New-Item -ItemType Directory -Force -Path $classes | Out-Null

$asmCp = ($asm | ForEach-Object { Join-Path $lib $_.Name }) -join ';'
Write-Host '[CarKill] compile'
& javac --release 25 -encoding UTF-8 -cp "$gameJar;$asmCp" -d $classes (Get-ChildItem $src -Filter '*.java').FullName
if ($LASTEXITCODE -ne 0) { throw "javac failed: $LASTEXITCODE" }

$asmDir = Join-Path $work 'asm'
if (Test-Path $asmDir) { Remove-Item -Recurse -Force $asmDir }
New-Item -ItemType Directory -Force -Path $asmDir | Out-Null
Push-Location $asmDir
try {
    foreach ($item in $asm) {
        & jar xf (Join-Path $PSScriptRoot (Join-Path $lib $item.Name))
        if ($LASTEXITCODE -ne 0) { throw "jar xf failed: $($item.Name)" }
    }
} finally {
    Pop-Location
}
Get-ChildItem $asmDir -Recurse -Directory -Filter 'META-INF' | Remove-Item -Recurse -Force
Get-ChildItem $asmDir -Filter 'module-info.class' -Recurse | Remove-Item -Force

Copy-Item -Recurse (Join-Path $classes 'carkill') (Join-Path $asmDir 'carkill')
# Drop stale thin-jar classes from previous build.bat runs.
$stale = Join-Path $dist 'carkill'
if (Test-Path -LiteralPath $stale) { Remove-Item -Recurse -Force -LiteralPath $stale }
if (Test-Path $jar) { Remove-Item -Force $jar }
Write-Host '[CarKill] package'
& jar cfm $jar (Join-Path $PSScriptRoot 'MANIFEST.MF') -C $asmDir .
if ($LASTEXITCODE -ne 0) { throw "jar cfm failed: $LASTEXITCODE" }
Copy-Item -Force $jar (Join-Path $PSScriptRoot 'car_kill.jar')
Write-Host "[CarKill] built $jar"

# Generate thin .bat wrappers that call sidecar .ps1 files.
function New-BatWrapper([string]$ps1Name, [string]$batName, [string]$failLabel) {
    $ps1Arg = '%~dp0' + $ps1Name
    $cmdLine = '%PS% -NoProfile -ExecutionPolicy Bypass -File "' + $ps1Arg + '"'
    $bat = @(
        '<# :',
        '@echo off',
        'setlocal EnableExtensions',
        'cd /d "%~dp0"',
        'where pwsh >nul 2>nul',
        'if %ERRORLEVEL%==0 ( set "PS=pwsh" ) else ( set "PS=powershell" )',
        'set "HERE=%~dp0"',
        $cmdLine,
        'set "CODE=%ERRORLEVEL%"',
        ('if not "%CODE%"=="0" echo [CarKill] ' + $failLabel + ' failed.'),
        'pause',
        'exit /b %CODE%',
        ': #>'
    ) -join "`r`n"
    $outPath = Join-Path $PSScriptRoot $batName
    Set-Content -LiteralPath $outPath -Value ($bat + "`r`n") -Encoding Ascii -NoNewline:$false
    Write-Host "[CarKill] wrote $batName"
}
New-BatWrapper 'install.ps1' 'install.bat' 'Install'
New-BatWrapper 'uninstall.ps1' 'uninstall.bat' 'Uninstall'

# ============================================================
# 手动挂载包: 目标机没有 Java 时也能用。
# attach-java = 只含 jdk.attach + java.instrument 的 jlink 运行时 (约 31MB), 用它的 java 去
# VirtualMachine.attach 游戏进程; 游戏 JVM 自身不需要 Java 环境, 也不会
# 出现 -javaagent / agents=1。整个包不修改游戏目录。
# ============================================================
$package = Join-Path $dist 'car-kill'
$staging = Join-Path $dist 'car-kill-build'
if (Test-Path -LiteralPath $staging) { Remove-Item -Recurse -Force -LiteralPath $staging }
New-Item -ItemType Directory -Force -Path $staging | Out-Null
Copy-Item -LiteralPath $jar -Destination $staging
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination $staging
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'find_pid.ps1') -Destination $staging

$jlink = Get-Command jlink -ErrorAction SilentlyContinue
if (-not $jlink) { throw 'jlink not found; build needs a full JDK 25 on PATH (JAVA_HOME\\bin)' }
Write-Host '[CarKill] building attach-java (jdk.attach + java.instrument)'
& $jlink.Source --add-modules jdk.attach,java.instrument --output (Join-Path $staging 'attach-java') `
    --strip-debug --no-man-pages --no-header-files --compress=zip-6
if ($LASTEXITCODE -ne 0) { throw "jlink failed: $LASTEXITCODE" }

$attachBat = @(
    '@echo off',
    'setlocal EnableExtensions',
    'cd /d "%~dp0"',
    'set "JAVA=%~dp0attach-java\bin\java.exe"',
    'if not exist "%JAVA%" (',
    '  echo [CarKill] attach-java runtime is missing. Use the packaged folder.',
    '  pause',
    '  exit /b 1',
    ')',
    'if not "%~1"=="" (',
    '  "%JAVA%" -cp "%~dp0car_kill.jar" carkill.CarKillAgent %*',
    '  if errorlevel 1 (',
    '    pause',
    '    exit /b 1',
    '  )',
    '  exit /b 0',
    ')',
    'set "FOUND="',
    'for /f "usebackq delims=" %%P in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find_pid.ps1" 2^>nul`) do set "FOUND=%%P"',
    'if defined FOUND (',
    '  "%JAVA%" -cp "%~dp0car_kill.jar" carkill.CarKillAgent %FOUND%',
    '  if not errorlevel 1 goto :ok',
    '  echo [CarKill] Attach to the detected PID failed.',
    '  pause',
    '  exit /b 1',
    ')',
    '"%JAVA%" -cp "%~dp0car_kill.jar" carkill.CarKillAgent --wait 120',
    'if not errorlevel 1 goto :ok',
    'set "FOUND="',
    'for /f "usebackq delims=" %%P in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0find_pid.ps1" 2^>nul`) do set "FOUND=%%P"',
    'if defined FOUND "%JAVA%" -cp "%~dp0car_kill.jar" carkill.CarKillAgent %FOUND%',
    'if errorlevel 1 (',
    '  echo [CarKill] Attach failed. Run car_kill_diag.bat and check the JVM list.',
    '  pause',
    '  exit /b 1',
    ')',
    ':ok',
    'exit /b 0'
) -join "`r`n"
Set-Content -LiteralPath (Join-Path $staging 'car_kill_attach.bat') -Value ($attachBat + "`r`n") -Encoding Ascii
Write-Host '[CarKill] wrote car_kill_attach.bat'

$diagBat = @(
    '@echo off',
    'setlocal EnableExtensions',
    'cd /d "%~dp0"',
    'set "JAVA=%~dp0attach-java\bin\java.exe"',
    'if not exist "%JAVA%" (',
    '  echo [CarKill] attach-java runtime is missing. Use the packaged folder.',
    '  pause',
    '  exit /b 1',
    ')',
    '"%JAVA%" -cp "%~dp0car_kill.jar" carkill.CarKillAgent --diagnose %*',
    'echo.',
    'pause',
    'exit /b 0'
) -join "`r`n"
Set-Content -LiteralPath (Join-Path $staging 'car_kill_diag.bat') -Value ($diagBat + "`r`n") -Encoding Ascii
Write-Host '[CarKill] wrote car_kill_diag.bat'

$suffix = Get-Date -Format 'yyyyMMdd-HHmmss'
if (Test-Path -LiteralPath $package) {
    try {
        Remove-Item -Recurse -Force -LiteralPath $package -ErrorAction Stop
    } catch {
        $package = Join-Path $dist ("car-kill-$suffix")
        Write-Host "[CarKill] old package is in use (game running?); writing $package instead"
    }
}
if (Test-Path -LiteralPath $package) { Remove-Item -Recurse -Force -LiteralPath $package }
Move-Item -LiteralPath $staging -Destination $package

$zip = Join-Path $dist 'car-kill.zip'
if (Test-Path -LiteralPath $zip) {
    try {
        Remove-Item -Force -LiteralPath $zip -ErrorAction Stop
    } catch {
        $zip = Join-Path $dist ("car-kill-$suffix.zip")
        Write-Host "[CarKill] old zip is in use; writing $zip instead"
    }
}
Compress-Archive -Path $package -DestinationPath $zip
Write-Host "[CarKill] packaged $package and $zip"
