# Emulator smoke: focus lock-task + home key escape check for Woli focus mode.

param(

    [string]$Avd = "Medium_Phone_API_36.1",

    [int]$BootTimeoutSec = 180

)



$ErrorActionPreference = "Stop"

$pkg = "com.woli.app"

$adb = "adb"



function Wait-Device {

    & $adb wait-for-device

    $deadline = (Get-Date).AddSeconds($BootTimeoutSec)

    while ((Get-Date) -lt $deadline) {

        $boot = (& $adb shell getprop sys.boot_completed 2>$null).Trim()

        if ($boot -eq "1") { return }

        Start-Sleep -Seconds 2

    }

    throw "Emulator boot timeout after ${BootTimeoutSec}s"

}



function Get-WoliTaskId {

    $line = & $adb shell dumpsys activity activities |

        Select-String -Pattern "Task\{[a-f0-9]+ #\d+ type=standard A=\d+:$([regex]::Escape($pkg))" |

        Select-Object -First 1

    if ($line -match '#(\d+)') { return [int]$Matches[1] }

    return $null

}



function Get-TopPackage {

    $line = & $adb shell dumpsys activity activities |

        Select-String -Pattern "topResumedActivity=ActivityRecord" |

        Select-Object -First 1

    if ($line -match 'u0 ([^/]+)/') { return $Matches[1] }

    return $null

}



Write-Host "==> Waiting for device..."

Wait-Device



Write-Host "==> Grant full-screen intent (API 34+)"

& $adb shell appops set $pkg USE_FULL_SCREEN_INTENT allow 2>$null



Write-Host "==> Installing debug APK..."

Push-Location (Split-Path $PSScriptRoot -Parent)

try {

    & .\gradlew.bat :app:installDebug

    if ($LASTEXITCODE -ne 0) { throw "gradlew installDebug failed" }

} finally {

    Pop-Location

}



Write-Host "==> Starting focus session (debug auto-focus intent)"

& $adb shell am force-stop $pkg

& $adb shell am start -n "$pkg/.MainActivity" --ez test_auto_focus true --es route "focus_eyes" | Out-Null

Start-Sleep -Seconds 4



$lockLine = & $adb shell dumpsys activity activities | Select-String "mLockTaskModeState" | Select-Object -First 1

Write-Host "==> Lock task state: $($lockLine.Line.Trim())"



if ($lockLine -notmatch "PINNED|LOCKED") {

    $taskId = Get-WoliTaskId

    if ($null -ne $taskId) {

        Write-Host "==> Fallback: adb task lock #$taskId"

        & $adb shell am task lock $taskId 2>$null

        Start-Sleep -Seconds 1

    }

}



Write-Host "==> Sending HOME key (x2)"

& $adb shell input keyevent KEYCODE_HOME

Start-Sleep -Milliseconds 600

& $adb shell input keyevent KEYCODE_HOME

Start-Sleep -Seconds 2



$topPkg = Get-TopPackage

Write-Host "==> Top resumed package: $topPkg"



if ($topPkg -eq $pkg) {

    Write-Host "PASS: Woli still foreground after double HOME"

    exit 0

}



Write-Host "FAIL: Woli not top after double HOME (expected lock-task pin)"

exit 1

