# ClipFlow 本地编译版本号自动递增脚本 (PowerShell)
# 用法: .\increment_version.ps1
# 在每次 gradlew assembleRelease 前自动执行

$buildFile = "$PSScriptRoot\app\build.gradle.kts"

if (-not (Test-Path $buildFile)) {
    Write-Host "❌ 未找到 $buildFile" -ForegroundColor Red
    exit 1
}

$content = Get-Content $buildFile -Raw

# 读取当前 versionCode 和 versionName
$currentCode = [regex]::Match($content, 'versionCode\s*=\s*(\d+)').Groups[1].Value
$currentName = [regex]::Match($content, 'versionName\s*=\s*"([^"]+)"').Groups[1].Value

if (-not $currentCode -or -not $currentName) {
    Write-Host "❌ 无法读取当前版本号" -ForegroundColor Red
    exit 1
}

$newCode = [int]$currentCode + 1

# versionName patch 位 +1 (x.y.z → x.y.z+1)
$parts = $currentName -split '\.'
$major = $parts[0]
$minor = $parts[1]
$patch = [int]$parts[2] + 1
$newName = "$major.$minor.$patch"

Write-Host "📦 版本递增: $currentName ($currentCode) → $newName ($newCode)" -ForegroundColor Cyan

# 替换
$content = $content -replace "versionCode = $currentCode", "versionCode = $newCode"
$content = $content -replace "versionName = `"$currentName`"", "versionName = `"$newName`""

Set-Content $buildFile $content -NoNewline

Write-Host "✅ 版本号已更新" -ForegroundColor Green
