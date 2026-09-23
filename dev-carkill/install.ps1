$ErrorActionPreference = 'Stop'

$packageDir = Join-Path $env:USERPROFILE 'Zomboid'
$sourceJar = Join-Path $PSScriptRoot 'car_kill.jar'
$targetJar = Join-Path $packageDir 'car_kill.jar'

function Find-GameDir {
    $roots = @(
        [Environment]::GetEnvironmentVariable('ProgramFiles(x86)'),
        $env:ProgramFiles
    ) | Where-Object { $_ }
    foreach ($root in $roots) {
        $steamApps = Join-Path $root 'Steam\steamapps'
        if (-not (Test-Path -LiteralPath $steamApps)) { continue }
        $hits = Get-ChildItem -LiteralPath $steamApps -Recurse -Filter 'ProjectZomboid64.json' -File -ErrorAction SilentlyContinue
        foreach ($hit in $hits) {
            if (Test-Path -LiteralPath (Join-Path $hit.DirectoryName 'projectzomboid.jar')) {
                return $hit.DirectoryName
            }
        }
    }
    $steamPath = [Microsoft.Win32.Registry]::GetValue(
        'HKEY_CURRENT_USER\Software\Valve\Steam', 'SteamPath', $null)
    if ($steamPath) {
        $steamApps = Join-Path $steamPath 'steamapps'
        if (Test-Path -LiteralPath $steamApps) {
            $hits = Get-ChildItem -LiteralPath $steamApps -Recurse -Filter 'ProjectZomboid64.json' -File -ErrorAction SilentlyContinue
            foreach ($hit in $hits) {
                if (Test-Path -LiteralPath (Join-Path $hit.DirectoryName 'projectzomboid.jar')) {
                    return $hit.DirectoryName
                }
            }
        }
    }
    return $null
}

function Add-Agent([string]$path, [string]$jar) {
    if (-not (Test-Path -LiteralPath $path)) { return }
    $mark = '-javaagent:"' + $jar + '"'
    $text = [IO.File]::ReadAllText($path)
    if ($text.Contains($mark)) {
        Write-Host "[CarKill] Already patched $(Split-Path -Leaf $path)"
        return
    }
    $backup = $path + '.carkill-backup'
    if (-not (Test-Path -LiteralPath $backup)) {
        Copy-Item -LiteralPath $path -Destination $backup
    }
    if ($path.EndsWith('.json', [StringComparison]::OrdinalIgnoreCase)) {
        $agentLine = '    "' + $mark + '",'
        $updated = [regex]::Replace($text, '(\s*"-Xmx[^"]*")', "$agentLine`n`$1")
    } else {
        $markLine = ' -javaagent:"' + $jar + '" '
        $updated = [regex]::Replace($text, '-Xmx', ($markLine + '-Xmx'), 'IgnoreCase')
    }
    [IO.File]::WriteAllText($path, $updated)
    Write-Host "[CarKill] Patched $(Split-Path -Leaf $path)"
}

if (-not (Test-Path -LiteralPath $sourceJar)) {
    $sourceJar = Join-Path $PSScriptRoot 'dist\car_kill.jar'
}
if (-not (Test-Path -LiteralPath $sourceJar)) {
    throw 'car_kill.jar not found (run build.ps1 first)'
}
New-Item -ItemType Directory -Force -Path $packageDir | Out-Null
if ((Resolve-Path -LiteralPath $sourceJar).Path -ne $targetJar) {
    Copy-Item -LiteralPath $sourceJar -Destination $targetJar -Force
}

$gameDir = Find-GameDir
if (-not $gameDir) { throw 'Project Zomboid install directory not found' }
foreach ($name in @('ProjectZomboid64.json', 'ProjectZomboid64.bat', 'ProjectZomboid64ShowConsole.bat')) {
    Add-Agent (Join-Path $gameDir $name) $targetJar
}
Write-Host '[CarKill] Installed. Fully quit and relaunch the game, then press \ to toggle.'
