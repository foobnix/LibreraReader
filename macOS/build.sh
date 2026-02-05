#!/bin/bash

# Exit on error
set -e

APP_NAME="Librera"
PROJECT_NAME="Librera Mac"
BUILD_DIR="build"
DIST_DIR="dist"

# Extract version from project.pbxproj
VERSION=$(grep -m 1 "MARKETING_VERSION" "$PROJECT_NAME.xcodeproj/project.pbxproj" | cut -d'=' -f2 | tr -d ' ;')
DMG_NAME="$APP_NAME v$VERSION.dmg"

echo "🚀 Starting build process for $APP_NAME..."

# 1. Clean up previous builds
echo "🧹 Cleaning up..."
rm -rf "$BUILD_DIR"
rm -rf "$DIST_DIR"
rm -f "$DMG_NAME"

# 2. Build the app
echo "🏗️ Building $APP_NAME (Release)..."
xcodebuild -project "$PROJECT_NAME.xcodeproj" \
           -scheme "$APP_NAME" \
           -configuration Release \
           -derivedDataPath "$BUILD_DIR" \
           build

# 3. Prepare DMG content
echo "📦 Preparing DMG content..."
mkdir -p "$DIST_DIR/dmg_content"

# Find the .app bundle
APP_BUNDLE=$(find "$BUILD_DIR" -name "$APP_NAME.app" -type d | head -n 1)

if [ -z "$APP_BUNDLE" ]; then
    echo "❌ Error: Could not find $APP_NAME.app bundle."
    exit 1
fi

cp -R "$APP_BUNDLE" "$DIST_DIR/dmg_content/"
ln -s /Applications "$DIST_DIR/dmg_content/Applications"

# 4. Create DMG
echo "💿 Creating $DMG_NAME..."
hdiutil create -volname "$APP_NAME" \
               -srcfolder "$DIST_DIR/dmg_content" \
               -ov -format UDZO \
               "$DMG_NAME"

# 5. Cleanup
echo "🧹 Final cleanup..."
rm -rf "$BUILD_DIR"
rm -rf "$DIST_DIR"

echo "✅ Done! $DMG_NAME is ready in the project root."


DROPBOX_DIR="/Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/DMG"
echo "📂 Exporting to Dropbox..."
mkdir -p "$DROPBOX_DIR"
#rm -f "$DROPBOX_DIR"/*.dmg
mv "$DMG_NAME" "$DROPBOX_DIR/"

echo "✅ Done! $DMG_NAME is now in $DROPBOX_DIR"
