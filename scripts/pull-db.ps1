# Pulls the Scholr app's local Room/SQLite database from a connected
# Android device/emulator into ./local-db/scholr.db (plus -wal/-shm).
#
# Usage:  powershell -ExecutionPolicy Bypass -File scripts\pull-db.ps1
#
# Requires: the app installed as a debug build and running/having run at
# least once on the connected device or emulator (adb devices).

$ErrorActionPreference = "Stop"

$adbCandidates = @(@(
    (Get-Command adb -ErrorAction SilentlyContinue).Source,
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "$env:ANDROID_HOME\platform-tools\adb.exe",
    "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
) | Where-Object { $_ -and (Test-Path $_) })

if ($adbCandidates.Count -eq 0) {
    throw "adb.exe not found. Install Android SDK platform-tools or add it to PATH."
}
$adb = $adbCandidates[0]

$package = "com.scholr.app"
$dbName = "scholr.db"
$outDir = Join-Path $PSScriptRoot "..\local-db"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outDir = Resolve-Path $outDir

foreach ($suffix in @("", "-wal", "-shm")) {
    $remote = "databases/$dbName$suffix"
    $local = Join-Path $outDir "$dbName$suffix"

    # Binary-safe transfer: base64-encode on device, decode locally.
    # (Piping adb's raw output through PowerShell redirection corrupts binary data.)
    $b64Lines = & $adb shell "run-as $package base64 $remote" 2>$null
    if (-not $b64Lines) {
        if ($suffix -eq "") { throw "Could not read $remote - is the app installed and has it run at least once?" }
        continue  # -wal/-shm may not exist if there's no pending WAL data
    }
    $b64 = ($b64Lines -join "").Trim()
    [IO.File]::WriteAllBytes($local, [Convert]::FromBase64String($b64))
}

Write-Host "Pulled database to: $outDir\$dbName"
Write-Host "Open it with DB Browser for SQLite, or inspect it with scripts\inspect-db.py"
