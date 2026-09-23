$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$src = 'src\main\java\carkill'
$lib = 'lib'
$work = 'build'
$classes = Join-Path $work 'classes'
$dist = 'dist'
$jar = Join-Path $dist 'car_kill.jar'
$gameJar = Join-Path $PSScriptRoot '..\etherhack-src\lib\zombie.jar'

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

# Self-contained hybrid .bat launchers: base64 PS payload, no sidecar .ps1 to copy.
# $PSScriptRoot does not exist under -EncodedCommand, so bind it to $env:HERE (set by the .bat stub).
function New-HybridBat([string]$ps1Name, [string]$batName, [string]$failLabel) {
    $code = Get-Content -LiteralPath (Join-Path $PSScriptRoot $ps1Name) -Raw
    $code = ($code -replace "`r?`n", "`r`n").Replace('$PSScriptRoot', '$env:HERE')
    $blob = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($code)) -replace '\s+', ''
    Write-Host "[CarKill] $batName code=$($code.Length) blob=$($blob.Length)"
    if ($blob.Length -lt 1000) { throw "blob too short for $batName ($($blob.Length))" }
    # NOTE: do not inline '+' with a multi-KB string inside @(): it splits into two elements.
    $cmdLine = '%PS% -NoProfile -ExecutionPolicy Bypass -EncodedCommand {0}' -f $blob
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
        'if not "%CODE%"=="0" echo [CarKill] ' + $failLabel + ' failed.',
        'pause',
        'exit /b %CODE%',
        ': #>'
    ) -join "`r`n"
    $outPath = Join-Path $PSScriptRoot $batName
    Set-Content -LiteralPath $outPath -Value ($bat + "`r`n") -Encoding Ascii -NoNewline:$false
    $cmdLine = Get-Content -LiteralPath $outPath | Where-Object { $_ -like '*-EncodedCommand *' }
    if ($cmdLine.Length -lt 1000) { throw "$batName payload line broken ($($cmdLine.Length) chars)" }
    Write-Host "[CarKill] wrote $batName"
}
New-HybridBat 'install.ps1' 'install.bat' 'Install'
New-HybridBat 'uninstall.ps1' 'uninstall.bat' 'Uninstall'
