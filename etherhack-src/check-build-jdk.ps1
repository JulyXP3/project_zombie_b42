#requires -Version 7.0
<#
    Validates the build JDK configured at the top of build.bat / build-fast.bat.

    A full Windows x64 JDK 25 is required for compilation and tests. The game's
    jre64 is a runtime and must never stand in for it. This check uses the real
    tools and module list of the configured JDK; it does not depend on a
    previously built installer.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$JavaHome
)

$ErrorActionPreference = 'Stop'

function Stop-Check([string]$message) {
    Write-Host "[ERROR] $message"
    exit 1
}

try {
    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        Stop-Check 'BUILD_JAVA_HOME is empty. Set it to the root of a full Windows x64 JDK 25 (not its bin folder).'
    }
    if (-not [IO.Path]::IsPathRooted($JavaHome)) {
        Stop-Check "BUILD_JAVA_HOME must be an absolute path: $JavaHome"
    }
    $jdkHome = [IO.Path]::GetFullPath($JavaHome)
    if (-not [IO.Directory]::Exists($jdkHome)) {
        Stop-Check "The configured build JDK does not exist: $jdkHome. Set BUILD_JAVA_HOME to a full Windows x64 JDK 25 root (not bin)."
    }
    foreach ($tool in @('java', 'javac', 'keytool', 'jarsigner')) {
        $executable = Join-Path $jdkHome ('bin\' + $tool + '.exe')
        if (-not [IO.File]::Exists($executable)) {
            Stop-Check "The configured build JDK is incomplete: missing bin\$tool.exe under $jdkHome. A full Windows x64 JDK 25 is required; the game jre64 is not a build JDK."
        }
    }

    $releasePath = Join-Path $jdkHome 'release'
    if (-not [IO.File]::Exists($releasePath)) {
        Stop-Check "The configured build JDK has no release file: $jdkHome. A full Windows x64 JDK 25 is required."
    }
    $release = [IO.File]::ReadAllText($releasePath)
    if ($release -match 'JAVA_VERSION="(?<version>[^"]+)"') {
        $version = $Matches['version']
    } else {
        Stop-Check "Cannot read JAVA_VERSION from $releasePath. A full Windows x64 JDK 25 is required."
    }
    if ($version -notmatch '^25(\.|$)') {
        Stop-Check "The configured build JDK is version $version; JDK 25 is required. Set BUILD_JAVA_HOME to a full Windows x64 JDK 25."
    }
    if ($release -notmatch 'OS_ARCH="x86_64"') {
        Stop-Check "The configured build JDK is not Windows x64: $jdkHome. A Windows x64 JDK 25 is required."
    }

    $java = Join-Path $jdkHome 'bin\java.exe'
    $javac = Join-Path $jdkHome 'bin\javac.exe'

    $javaVersion = (& $java -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0) {
        Stop-Check "The configured build JDK cannot run java.exe: $jdkHome"
    }
    if ($javaVersion -notmatch 'version "25(\.|")') {
        Stop-Check "The configured build JDK reports an unexpected java version:`n$javaVersion`nJDK 25 is required."
    }

    $settings = (& $java -XshowSettings:properties -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $settings -notmatch 'os\.arch = amd64') {
        Stop-Check "The configured build JDK is not a Windows x64 JDK: $jdkHome"
    }

    $javacVersion = (& $javac -version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $javacVersion -notmatch 'javac 25(\.|\s|$)') {
        Stop-Check "The configured build JDK has no working javac 25: $jdkHome`n$javacVersion"
    }

    $moduleLines = @(& $java --list-modules 2>&1 | ForEach-Object { $_.ToString() })
    if ($LASTEXITCODE -ne 0) {
        Stop-Check "Cannot list the modules of the configured build JDK: $jdkHome"
    }
    foreach ($module in @('java.instrument', 'jdk.attach', 'jdk.compiler')) {
        if (-not ($moduleLines | Where-Object { $_ -match ('^' + [regex]::Escape($module) + '@') })) {
            Stop-Check "The configured build JDK is missing the $module module required for build and tests: $jdkHome"
        }
    }

    Write-Host "Build Java: $jdkHome"
    exit 0
} catch {
    Write-Host "[ERROR] JDK check failed: $($_.Exception.Message)"
    exit 1
}
