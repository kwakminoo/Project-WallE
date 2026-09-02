# Capture focus-flow screens one at a time (avoids auto-navigation / back-stack issues).
$ErrorActionPreference = "Continue"
$pkg = "com.woli.app"
$activity = "$pkg/.MainActivity"
$outDir = Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) "docs\screenshots-capture"

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

function Start-Gallery {
    adb shell am force-stop $pkg 2>$null
    Start-Sleep -Milliseconds 500
    adb shell am start -W -n $activity -e route shell_gallery 2>$null | Out-Null
    Start-Sleep -Seconds 6
    adb shell input swipe 540 2000 540 900 600
    Start-Sleep -Seconds 1
}

function Gallery-Tap($route, $x, $y, $waitMs) {
    Start-Gallery
    adb shell input tap $x $y
    Start-Sleep -Milliseconds $waitMs
    Capture-Png (Join-Path $outDir "$route.png")
    Write-Host "OK $route"
}

function Route-Start($route, $waitMs, $autoFocus = $false) {
    adb shell am force-stop $pkg 2>$null
    Start-Sleep -Milliseconds 500
    $args = @("shell", "am", "start", "-W", "-n", $activity)
    if ($autoFocus) { $args += @("-e", "test_auto_focus", "true") }
    $args += @("-e", "route", $route)
    & adb @args 2>$null | Out-Null
    Start-Sleep -Milliseconds $waitMs
    Capture-Png (Join-Path $outDir "$route.png")
    Write-Host "OK $route"
}

Gallery-Tap "remaining_time" 540 1195 1800
Gallery-Tap "important_call" 540 1360 4000
Gallery-Tap "hand_warning" 540 1527 4000
Route-Start "focus_complete" 2500
Route-Start "quit_confirm" 4000 $true
Route-Start "rhythm_mission" 8000
Route-Start "session_report" 2500
Route-Start "focus_eyes" 6000 $true

Write-Host "Done: $outDir"
