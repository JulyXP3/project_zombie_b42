# Fallback PID discovery for cases where VirtualMachine.list() cannot see the game JVM
# (for example different elevation/user, or the JVM is not listed by the attach API).
# Prints one PID, or nothing. PowerShell 5.1 compatible.
$ErrorActionPreference = 'SilentlyContinue'

$candidates = Get-CimInstance Win32_Process | Where-Object {
    ($_.Name -eq 'java.exe' -or $_.Name -eq 'javaw.exe') -and
    $_.CommandLine -match 'projectzomboid\.jar' -and
    $_.CommandLine -notmatch 'coordinator\.jar' -and
    $_.CommandLine -notmatch 'car_kill\.jar'
}

$first = $candidates | Sort-Object {
    if ($_.CommandLine -match 'MainScreenState|modcore\.launch') { 0 } else { 1 }
} | Select-Object -First 1

if ($first) { $first.ProcessId }
