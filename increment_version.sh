#!/bin/bash
# ClipFlow 本地编译版本号自动递增脚本
# 用法: ./increment_version.sh
# 在每次 ./gradlew assembleRelease 前自动执行

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_FILE="$SCRIPT_DIR/app/build.gradle.kts"

if [ ! -f "$BUILD_FILE" ]; then
    echo "❌ 未找到 $BUILD_FILE"
    exit 1
fi

# 读取当前 versionCode 和 versionName
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K\d+' "$BUILD_FILE")
CURRENT_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_FILE")

if [ -z "$CURRENT_CODE" ] || [ -z "$CURRENT_NAME" ]; then
    echo "❌ 无法读取当前版本号"
    exit 1
fi

NEW_CODE=$((CURRENT_CODE + 1))

# versionName patch 位 +1 (x.y.z -> x.y.z+1)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
NEW_PATCH=$((PATCH + 1))
NEW_NAME="$MAJOR.$MINOR.$NEW_PATCH"

echo "📦 版本递增: $CURRENT_NAME ($CURRENT_CODE) → $NEW_NAME ($NEW_CODE)"

# 替换 versionCode
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$BUILD_FILE"

# 替换 versionName
sed -i "s/versionName = \"$CURRENT_NAME\"/versionName = \"$NEW_NAME\"/" "$BUILD_FILE"

echo "✅ 版本号已更新"
