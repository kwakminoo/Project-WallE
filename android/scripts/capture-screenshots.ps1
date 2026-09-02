# Capture all Woli app screens via MainActivity EXTRA_ROUTE
$ErrorActionPreference = "Continue"
$pkg = "com.woli.app"
$activity = "$pkg/.MainActivity"
$outDir = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) "docs\screenshots-capture"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$routes = @(
    @{ route = "home"; wait = 6000 },
    @{ route = "stats"; wait = 6000 },
    @{ route = "settings"; wait = 6000 },
    @{ route = "focus_time"; wait = 6000 },
    @{ route = "device_connect"; wait = 6000 },
    @{ route = "bluetooth_settings"; wait = 6000 },
    @{ route = "important_contacts"; wait = 6000 },
    @{ route = "focus_notification_permission"; wait = 6000 },
    @{ route = "notification_policy"; wait = 6000 },
    @{ route = "notification_diagnostics"; wait = 6000 },
    @{ route = "hardware_diagnostics"; wait = 6000 },
    @{ route = "mount_guide"; wait = 6000 },
    @{ route = "shell_gallery"; wait = 6000 },
    @{ route = "app_info"; wait = 6000 },
    @{ route = "focus_eyes"; wait = 6000; autoFocus = $true },
    @{ route = "remaining_time"; wait = 6000; autoFocus = $true },
    @{ route = "important_call"; wait = 6000 },
    @{ route = "hand_warning"; wait = 6000 },
    @{ route = "focus_complete"; wait = 2500 },
    @{ route = "quit_confirm"; wait = 6000; autoFocus = $true },
    @{ route = "rhythm_mission"; wait = 8000 },
    @{ route = "session_report"; wait = 3000 }
)

$perms = @(
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.CAMERA",
    "android.permission.RECORD_AUDIO",
    "android.permission.READ_CONTACTS",
    "android.permission.READ_PHONE_STATE",
    "android.permission.READ_CALL_LOG",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.BLUETOOTH_SCAN"
)
foreach ($p in $perms) {
    adb shell pm grant $pkg $p 2>$null
}

function Capture-Png($file) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "adb"
    $psi.Arguments = "exec-out screencap -p"
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $ms = New-Object System.IO.MemoryStream
    $proc.StandardOutput.BaseStream.CopyTo($ms)
    $proc.WaitForExit()
    [System.IO.File]::WriteAllBytes($file, $ms.ToArray())
}

function Capture-Route($route, $waitMs, $autoFocus) {
    adb shell am force-stop $pkg 2>$null
    Start-Sleep -Milliseconds 500

    $args = @("shell", "am", "start", "-W", "-n", $activity)
    if ($autoFocus) { $args += @("-e", "test_auto_focus", "true") }
    $args += @("-e", "route", $route)
    & adb @args 2>$null | Out-Null

    Start-Sleep -Milliseconds $waitMs

    $file = Join-Path $outDir "$route.png"
    Capture-Png $file
    if (Test-Path $file) {
        Write-Host "OK $route ($((Get-Item $file).Length) bytes)"
    } else {
        Write-Host "FAIL $route"
    }
}

foreach ($item in $routes) {
    Capture-Route $item.route $item.wait ($item.autoFocus -eq $true)
}

Write-Host "Saved to $outDir"
