$ErrorActionPreference = 'Stop'

$targetJar = Join-Path $env:USERPROFILE 'Zomboid\car_kill.jar'

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

function Restore-LaunchFile([string]$path, [string]$jar) {
    $backup = $path + '.carkill-backup'
    if (Test-Path -LiteralPath $backup) {
        Move-Item -LiteralPath $backup -Destination $path -Force
        Write-Host "[CarKill] Restored $(Split-Path -Leaf $path)"
        return
    }
    if (-not (Test-Path -LiteralPath $path)) { return }
    $mark = ' -javaagent:"' + $jar + '"'
    $text = [IO.File]::ReadAllText($path)
    $updated = $text.Replace($mark, '')
    if ($updated -eq $text) { return }
    [IO.File]::WriteAllText($path, $updated)
    Write-Host "[CarKill] Restored $(Split-Path -Leaf $path)"
}

$gameDir = Find-GameDir
if (-not $gameDir) { throw 'Project Zomboid install directory not found' }
foreach ($name in @('ProjectZomboid64.json', 'ProjectZomboid64.bat', 'ProjectZomboid64ShowConsole.bat')) {
    Restore-LaunchFile (Join-Path $gameDir $name) $targetJar
}
try {
    if (Test-Path -LiteralPath $targetJar) {
        Remove-Item -LiteralPath $targetJar -Force
        Write-Host '[CarKill] Removed car_kill.jar.'
    }
} catch {
    Write-Host '[CarKill] Could not remove car_kill.jar (quit the game first, then delete it manually).'
}
Write-Host '[CarKill] Uninstalled. Fully quit and relaunch the game.'