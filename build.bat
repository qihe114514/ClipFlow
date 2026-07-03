@echo off
chcp 65001 >nul
REM ClipFlow 本地编译版本号自动递增 + 构建脚本
REM 用法: build.bat

echo ========================================
echo   ClipFlow - 本地构建脚本
echo ========================================
echo.

REM 1. 递增版本号
echo [1/3] 递增版本号...
powershell -ExecutionPolicy Bypass -File "%~dp0\increment_version.ps1"
if %errorlevel% neq 0 (
    echo ❌ 版本号递增失败
    pause
    exit /b 1
)

REM 2. 清理 + 构建
echo.
echo [2/3] 开始构建 Release APK...
call gradlew assembleRelease
if %errorlevel% neq 0 (
    echo ❌ 构建失败
    pause
    exit /b 1
)

REM 3. 输出 APK 路径
echo.
echo [3/3] ✅ 构建完成！
echo.
echo APK 输出路径:
dir /s /b app\build\outputs\apk\release\*.apk
echo.
pause
