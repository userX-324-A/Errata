# Build the Errata debug APK and install it on connected USB/wireless devices.
#
#   .\scripts\install-device.ps1
#   .\scripts\install-device.ps1 -Serial 65290DLKX000LK
#   .\scripts\install-device.ps1 -SkipBuild
#
param(
    [string]$Serial,
    [switch]$SkipBuild,
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

function Get-AdbPath {
    $fromProps = Join-Path $repo "local.properties"
    if (Test-Path $fromProps) {
        $line = Get-Content $fromProps | Where-Object { $_ -match '^\s*sdk\.dir=' } | Select-Object -First 1
        if ($line) {
            $dir = $line -replace '^\s*sdk\.dir=', '' -replace '\\:', ':' -replace '\\\\', '\'
            $candidate = Join-Path $dir "platform-tools\adb.exe"
            if (Test-Path $candidate) { return $candidate }
        }
    }
    foreach ($root in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk")) {
        if (-not $root) { continue }
        $candidate = Join-Path $root "platform-tools\adb.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $onPath = Get-Command adb -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    throw "adb not found. Install platform-tools or set sdk.dir in local.properties."
}

function Get-DeviceSerials([string]$Adb) {
    $lines = & $Adb devices | Select-Object -Skip 1
    $serials = foreach ($line in $lines) {
        if ($line -match '^(\S+)\s+device\s*$') { $Matches[1] }
    }
    @($serials)
}

$adb = Get-AdbPath
$serials = Get-DeviceSerials $adb
if ($Serial) {
    if ($serials -notcontains $Serial) {
        throw "Device '$Serial' is not connected. adb devices: $($serials -join ', ')"
    }
    $serials = @($Serial)
}
if ($serials.Count -eq 0) {
    throw "No device in 'device' state. Plug in USB (or adb connect), authorize debugging, then retry."
}

Write-Host "Devices: $($serials -join ', ')"

if (-not $SkipBuild) {
    $gradlew = Join-Path $repo "gradlew.bat"
    & $gradlew :app:assembleDebug --quiet
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleDebug failed ($LASTEXITCODE)." }
}

$apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) {
    throw "Missing $apk. Run without -SkipBuild."
}

foreach ($id in $serials) {
    Write-Host "Installing on $id ..."
    & $adb -s $id install -r --no-incremental $apk
    if ($LASTEXITCODE -ne 0) { throw "adb install failed on $id ($LASTEXITCODE)." }
    if (-not $NoLaunch) {
        & $adb -s $id shell am start -n com.errata.app/.MainActivity | Out-Null
    }
}

Write-Host "Done."
