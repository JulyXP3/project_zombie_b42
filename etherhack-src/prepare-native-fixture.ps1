#requires -Version 7.0
[CmdletBinding()]
param()

function Get-BuildPath([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path) -or -not [IO.Path]::IsPathFullyQualified($Path)) {
        throw "An absolute build path is required: $Path"
    }
    $full = [IO.Path]::GetFullPath($Path)
    if ($full -ne [IO.Path]::GetPathRoot($full)) { $full = $full.TrimEnd('\', '/') }
    return $full
}

function Test-BuildPathInside([string]$Child, [string]$Parent) {
    $childPath = (Get-BuildPath $Child).TrimEnd('\') + '\'
    $parentPath = (Get-BuildPath $Parent).TrimEnd('\') + '\'
    return $childPath.StartsWith($parentPath, [StringComparison]::OrdinalIgnoreCase)
}

function Assert-BuildPath([string]$Path) {
    $full = Get-BuildPath $Path
    $current = [IO.Path]::GetPathRoot($full)
    foreach ($part in $full.Substring($current.Length).Split('\', [StringSplitOptions]::RemoveEmptyEntries)) {
        $current = [IO.Path]::Combine($current, $part)
        try { $attributes = [IO.File]::GetAttributes($current) }
        catch [IO.FileNotFoundException] { continue }
        catch [IO.DirectoryNotFoundException] { continue }
        if (($attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Refusing a reparse point in the build path: $current"
        }
    }
}

function Get-BuildHash([string]$Path) {
    Assert-BuildPath $Path
    try { return (Get-FileHash -LiteralPath $Path -Algorithm SHA256 -ErrorAction Stop).Hash.ToLowerInvariant() }
    catch { throw "Cannot read the file; it may be in use or locked: $Path. $($_.Exception.Message)" }
}

function Get-TreeDigest($Files, $Directories) {
    [string[]]$directoryNames = @($Directories)
    [Array]::Sort($directoryNames, [StringComparer]::Ordinal)
    $fileMap = @{}
    foreach ($file in $Files) { $fileMap[$file.Relative] = $file }
    [string[]]$fileNames = @($fileMap.Keys)
    [Array]::Sort($fileNames, [StringComparer]::Ordinal)
    $lines = [Collections.Generic.List[string]]::new()
    foreach ($name in $directoryNames) { $lines.Add('D|' + $name) }
    foreach ($name in $fileNames) {
        $file = $fileMap[$name]
        $lines.Add('F|' + $name + '|' + $file.Length + '|' + $file.Hash)
    }
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData(
        [Text.Encoding]::UTF8.GetBytes(($lines -join "`n") + "`n"))).ToLowerInvariant()
}

function Get-FixtureTree([string]$Root) {
    Assert-BuildPath $Root
    $files = [Collections.Generic.List[object]]::new()
    $directories = [Collections.Generic.List[string]]::new()
    $pending = [Collections.Generic.Stack[string]]::new()
    $pending.Push($Root)
    while ($pending.Count -gt 0) {
        $directory = $pending.Pop()
        Assert-BuildPath $directory
        foreach ($path in [IO.Directory]::EnumerateFileSystemEntries($directory)) {
            $attributes = [IO.File]::GetAttributes($path)
            if (($attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw "Refusing a reparse point in the runtime: $path"
            }
            $relative = [IO.Path]::GetRelativePath($Root, $path).Replace('\', '/')
            if (($attributes -band [IO.FileAttributes]::Directory) -ne 0) {
                $directories.Add($relative)
                $pending.Push($path)
            } else {
                $info = [IO.FileInfo]::new($path)
                $files.Add([pscustomobject]@{ Relative = $relative; Length = $info.Length; Hash = Get-BuildHash $path })
            }
        }
    }
    return [pscustomobject]@{ Files = @($files); Directories = @($directories); Digest = Get-TreeDigest $files $directories }
}

function Get-NativeVersionName([string]$Source, [string]$Digest) {
    $sourceId = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData(
        [Text.Encoding]::UTF8.GetBytes((Get-BuildPath $Source).ToUpperInvariant()))).ToLowerInvariant()
    return 'runtime-' + $Digest + '-' + $sourceId.Substring(0, 16)
}

function Assert-ManifestPath([string]$Path, [switch]$Directory) {
    if ($Directory -and $Path -ceq 'jre64') { return }
    if (-not $Path.StartsWith('jre64/', [StringComparison]::Ordinal) -or $Path.Contains('\')) {
        throw "Invalid relative path in the runtime manifest: $Path"
    }
    foreach ($part in $Path.Split('/')) {
        if ([string]::IsNullOrEmpty($part) -or $part -in @('.', '..') -or
            $part -match '[<>:"|?*\x00-\x1f]' -or $part -match '[. ]$' -or
            $part -match '^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(\.|$)') {
            throw "Invalid relative path in the runtime manifest: $Path"
        }
    }
}

function Read-NativeManifest([string]$Root) {
    $manifestPath = Join-Path $Root 'manifest.json'
    Assert-BuildPath $manifestPath
    if (-not [IO.File]::Exists($manifestPath) -or [IO.FileInfo]::new($manifestPath).Length -gt 1048576) {
        throw "Missing or oversized ownership manifest; refusing unknown runtime: $Root"
    }
    try { $record = [IO.File]::ReadAllText($manifestPath) | ConvertFrom-Json -ErrorAction Stop }
    catch { throw "Unreadable runtime ownership manifest: $Root" }
    $fields = @('format', 'source', 'sourceDigest', 'createdUtc', 'files', 'directories')
    if ($null -eq $record -or @($record.PSObject.Properties).Count -ne $fields.Count -or
        $record.format -cne 1 -or $record.source -isnot [string] -or
        $record.sourceDigest -isnot [string] -or $record.sourceDigest -cnotmatch '^[a-f0-9]{64}$' -or
        $record.files -isnot [Array] -or $record.directories -isnot [Array]) {
        throw "Invalid runtime ownership manifest: $Root"
    }
    foreach ($name in $fields) {
        if ($null -eq $record.PSObject.Properties[$name]) { throw "Missing manifest field: $name" }
    }
    if ($record.source -cne (Get-BuildPath $record.source)) { throw 'Noncanonical source path in the runtime manifest' }
    if ((Get-NativeVersionName $record.source $record.sourceDigest) -cne [IO.Path]::GetFileName($Root)) {
        throw "Runtime ownership manifest does not match its source or directory: $Root"
    }
    $expected = [Collections.Generic.Dictionary[string, object]]::new([StringComparer]::OrdinalIgnoreCase)
    $dirs = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($path in $record.directories) {
        if ($path -isnot [string]) { throw 'Invalid directory type in the runtime manifest' }
        Assert-ManifestPath $path -Directory
        if (-not $dirs.Add($path)) { throw "Duplicate directory in the runtime manifest: $path" }
    }
    if (-not $dirs.Contains('jre64')) { throw 'Runtime manifest has no jre64 root directory' }
    $sourceFiles = [Collections.Generic.List[object]]::new()
    foreach ($file in $record.files) {
        if ($null -eq $file -or @($file.PSObject.Properties).Count -ne 3 -or
            $file.path -isnot [string] -or $file.sha256 -isnot [string] -or
            $file.sha256 -cnotmatch '^[a-f0-9]{64}$' -or
            ($file.length -isnot [long] -and $file.length -isnot [int]) -or $file.length -lt 0) {
            throw 'Invalid file entry in the runtime manifest'
        }
        Assert-ManifestPath $file.path
        if ($expected.ContainsKey($file.path) -or $dirs.Contains($file.path)) {
            throw "Duplicate file path in the runtime manifest: $($file.path)"
        }
        $expected.Add($file.path, $file)
        $sourceFiles.Add([pscustomobject]@{ Relative = $file.path.Substring(6); Length = $file.length; Hash = $file.sha256 })
    }
    foreach ($path in @($expected.Keys) + @($dirs)) {
        $parent = $path
        while ($parent.Contains('/')) {
            $parent = $parent.Substring(0, $parent.LastIndexOf('/'))
            if (-not $dirs.Contains($parent)) { throw "Unregistered parent directory in the manifest: $parent" }
        }
    }
    $sourceDirectories = @($record.directories | Where-Object { $_ -cne 'jre64' } | ForEach-Object { $_.Substring(6) })
    if ((Get-TreeDigest $sourceFiles $sourceDirectories) -cne $record.sourceDigest) {
        throw "Runtime manifest changed (source digest mismatch): $Root"
    }
    return $record
}

function Test-NativeOwnership([string]$Root, $Record) {
    $tree = Get-FixtureTree $Root
    $expectedFiles = @{}
    foreach ($file in $Record.files) { $expectedFiles[$file.path] = $file }
    $expectedDirectories = @{}
    foreach ($path in $Record.directories) { $expectedDirectories[$path] = $true }
    $seen = @{}
    foreach ($file in $tree.Files) {
        if ($file.Relative -ceq 'manifest.json') { continue }
        if (-not $expectedFiles.ContainsKey($file.Relative)) { throw "Unknown/unregistered runtime file: $($file.Relative)" }
        $expected = $expectedFiles[$file.Relative]
        if ($expected.length -ne $file.Length -or $expected.sha256 -cne $file.Hash) {
            throw "Runtime file changed (digest mismatch): $($file.Relative)"
        }
        $seen[$file.Relative] = $true
    }
    foreach ($path in $tree.Directories) {
        if (-not $expectedDirectories.ContainsKey($path)) { throw "Unknown/unregistered runtime directory: $path" }
    }
    return $seen.Count -eq $expectedFiles.Count -and $tree.Directories.Count -eq $expectedDirectories.Count
}

function Write-NativeManifest([string]$Root, [string]$Source, $Tree) {
    $record = [ordered]@{
        format = 1
        source = $Source
        sourceDigest = $Tree.Digest
        createdUtc = [DateTime]::UtcNow.ToString('o')
        files = @($Tree.Files | ForEach-Object { [ordered]@{ path = 'jre64/' + $_.Relative; length = $_.Length; sha256 = $_.Hash } })
        directories = @('jre64') + @($Tree.Directories | ForEach-Object { 'jre64/' + $_ })
    }
    [IO.File]::WriteAllText((Join-Path $Root 'manifest.json'), (ConvertTo-Json -InputObject $record -Depth 6), [Text.UTF8Encoding]::new($false))
}

function Remove-NativeBackup([string]$Root, $Before) {
    try {
        $now = Get-FixtureTree $Root
        if ($now.Digest -cne $Before.Digest) { throw 'Backup content changed; leaving it for review' }
        foreach ($file in $Before.Files) {
            $path = Join-Path $Root $file.Relative
            if ((Get-BuildHash $path) -cne $file.Hash) { throw 'Backup file changed during cleanup' }
            [IO.File]::SetAttributes($path, [IO.FileAttributes]::Normal)
            [IO.File]::Delete($path)
        }
        foreach ($directory in @($Before.Directories | Sort-Object -Property Length -Descending)) {
            $path = Join-Path $Root $directory
            Assert-BuildPath $path
            [IO.Directory]::Delete($path, $false)
        }
        Assert-BuildPath $Root
        [IO.Directory]::Delete($Root, $false)
    } catch {
        Write-Warning "Owned backup was not fully removed; review it before the next build: $Root. $($_.Exception.Message)"
    }
}

function Get-GameBuildInput([string]$GameDir, [string]$CompileDependency, [string]$SourceRoot, [switch]$Native) {
    $game = Get-BuildPath $GameDir
    $library = Get-BuildPath $CompileDependency
    $store = Join-Path (Split-Path -Parent $SourceRoot) 'temp/native-runtime-build'
    foreach ($path in @($game, $library, (Join-Path $game 'projectzomboid.jar'), $SourceRoot, $store)) { Assert-BuildPath $path }
    foreach ($output in @($SourceRoot, $store)) {
        if ((Test-BuildPathInside $output $game) -or (Test-BuildPathInside $game $output)) {
            throw "Game directory and build output overlap: $game / $output"
        }
    }
    $gameJar = Join-Path $game 'projectzomboid.jar'
    if (-not [IO.Directory]::Exists($game) -or -not [IO.File]::Exists($gameJar)) {
        throw "The configured game root has no projectzomboid.jar: $game"
    }
    if (-not [IO.File]::Exists($library)) { throw "Missing compile dependency: $library" }
    if ((Get-BuildHash $library) -cne (Get-BuildHash $gameJar)) {
        throw 'lib\projectzomboid.jar does not match the configured game. Neither JAR was changed; copy the matching game JAR into lib\ and retry.'
    }
    $runtime = Join-Path $game 'jre64'
    if ($Native) {
        foreach ($required in @('bin/java.exe', 'bin/keytool.exe', 'bin/server/jvm.dll', 'lib/modules', 'lib/jvm.cfg', 'conf/net.properties', 'release')) {
            if (-not [IO.File]::Exists((Join-Path $runtime $required))) { throw "The game jre64 is incomplete: missing $required under $runtime" }
        }
    }
    return [pscustomobject]@{ Runtime = $runtime; Store = $store }
}

function Get-PreparedNativeFixture($InputPaths) {
    $source = Get-BuildPath $InputPaths.Runtime
    $store = Get-BuildPath $InputPaths.Store
    Assert-BuildPath $store
    $sourceTree = Get-FixtureTree $source
    $target = Join-Path $store (Get-NativeVersionName $source $sourceTree.Digest)
    if ([IO.Directory]::Exists($store)) {
        foreach ($entry in [IO.Directory]::EnumerateFileSystemEntries($store)) {
            Assert-BuildPath $entry
            if (-not [IO.Directory]::Exists($entry) -or [IO.Path]::GetFileName($entry) -cnotmatch '^runtime-[a-f0-9]{64}-[a-f0-9]{16}$') {
                throw "Unknown/unregistered entry in the native runtime store: $entry"
            }
            $record = Read-NativeManifest $entry
            $complete = Test-NativeOwnership $entry $record
            if ($entry -eq $target -and $complete) {
                if ($record.source -cne $source -or $record.sourceDigest -cne $sourceTree.Digest) { throw 'The runtime manifest does not match the configured source' }
                Write-Host "Native fixture reused: $target"
                $selected = $true
            }
        }
        if ($selected) { return $target }
    } elseif ([IO.File]::Exists($store)) { throw "The runtime store is a file: $store" }
    [IO.Directory]::CreateDirectory($store) | Out-Null
    Assert-BuildPath $store
    $staging = Join-Path $store ('staging-' + [Guid]::NewGuid().ToString('N'))
    $backup = $null
    try {
        [IO.Directory]::CreateDirectory($staging) | Out-Null
        $runtimeCopy = Join-Path $staging 'jre64'
        [IO.Directory]::CreateDirectory($runtimeCopy) | Out-Null
        foreach ($directory in $sourceTree.Directories) { [IO.Directory]::CreateDirectory((Join-Path $runtimeCopy $directory)) | Out-Null }
        foreach ($file in $sourceTree.Files) {
            $from = Join-Path $source $file.Relative
            $to = Join-Path $runtimeCopy $file.Relative
            Assert-BuildPath $from
            Assert-BuildPath $to
            [IO.File]::Copy($from, $to, $false)
        }
        if ((Get-FixtureTree $runtimeCopy).Digest -cne $sourceTree.Digest -or
            (Get-FixtureTree $source).Digest -cne $sourceTree.Digest) { throw 'The runtime changed while it was being copied; the existing fixture was not updated' }
        Write-NativeManifest $staging $source $sourceTree
        Assert-BuildPath $target
        if ([IO.Directory]::Exists($target)) {
            $record = Read-NativeManifest $target
            $null = Test-NativeOwnership $target $record
            $before = Get-FixtureTree $target
            $backup = Join-Path $store ('backup-' + [Guid]::NewGuid().ToString('N'))
            [IO.Directory]::Move($target, $backup)
        }
        try { [IO.Directory]::Move($staging, $target) }
        catch {
            if ($backup) { [IO.Directory]::Move($backup, $target) }
            throw
        }
        $staging = $null
        if ($backup) { Remove-NativeBackup $backup $before }
        Write-Host "Native fixture ready: $target"
        return $target
    } finally {
        if ($staging -and [IO.Directory]::Exists($staging)) {
            Write-Warning "Unpublished staging copy retained for review; it is not a usable fixture: $staging"
        }
    }
}
