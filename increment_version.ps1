# Increment versionCode before release packaging and keep versionName unchanged.
$buildFile = "$PSScriptRoot\app\build.gradle.kts"

if (-not (Test-Path $buildFile)) {
    Write-Host "Build file not found: $buildFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $buildFile -Raw -Encoding UTF8
$currentCode = [regex]::Match($content, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$currentName = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value

if (-not $currentCode -or -not $currentName) {
    Write-Host "Unable to read version metadata." -ForegroundColor Red
    exit 1
}

$newCode = [int]$currentCode + 1
Write-Host "Version code: $currentCode -> $newCode; version name remains $currentName" -ForegroundColor Cyan

$content = $content -replace "versionCode = $currentCode", "versionCode = $newCode"
Set-Content $buildFile $content -Encoding UTF8 -NoNewline

Write-Host "Version metadata updated." -ForegroundColor Green
