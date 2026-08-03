param(
    [string]$Name = 'device-current',
    [switch]$Record
)

$ErrorActionPreference = 'Stop'

if ($Name -notmatch '^[a-z0-9-]+$') {
    throw 'Name must contain only lowercase letters, numbers, and hyphens.'
}

$assetDir = Join-Path $PSScriptRoot 'assets'
New-Item -ItemType Directory -Force -Path $assetDir | Out-Null

$deviceState = (adb get-state 2>$null | Out-String).Trim()
if ($deviceState -ne 'device') {
    throw 'No authorized Android device is connected.'
}

$imagePath = Join-Path $assetDir "$Name.png"
$processInfo = New-Object System.Diagnostics.ProcessStartInfo
$processInfo.FileName = 'adb'
$processInfo.Arguments = 'exec-out screencap -p'
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardOutput = $true
$captureProcess = New-Object System.Diagnostics.Process
$captureProcess.StartInfo = $processInfo
$captureProcess.Start() | Out-Null
$outputStream = [System.IO.File]::Open($imagePath, [System.IO.FileMode]::Create)
$captureProcess.StandardOutput.BaseStream.CopyTo($outputStream)
$outputStream.Dispose()
$captureProcess.WaitForExit()
if ($captureProcess.ExitCode -ne 0) {
    throw "adb screencap failed with exit code $($captureProcess.ExitCode)."
}

if ($Record) {
    $remoteVideoPath = '/sdcard/clipflow-promo.mp4'
    $localVideoPath = Join-Path $assetDir 'device-flow.mp4'
    adb shell screenrecord --time-limit 12 --bit-rate 8000000 $remoteVideoPath
    adb pull $remoteVideoPath $localVideoPath | Out-Null
    adb shell rm $remoteVideoPath | Out-Null
}

Write-Output "Captured $imagePath"
