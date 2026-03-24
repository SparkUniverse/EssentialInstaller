#!/bin/bash

set -oe pipefail

BRAND=$(cat ../wrapper/resources/info/brand.txt)
APP_NAME="$BRAND Installer"
APP_ID=$(cat ../wrapper/resources/info/macos-app-id.txt)
EXECUTABLE_NAME="installer"

pwd
cd ../wrapper/target || exit

# Prepare directory
mkdir -p universal-apple-darwin/release

# Make universal binary
lipo \
  -create x86_64-apple-darwin/release/installer-wrapper aarch64-apple-darwin/release/installer-wrapper \
  -output universal-apple-darwin/release/$EXECUTABLE_NAME

echo "Made universal binary"

# Remove previous app
rm -rf "universal-apple-darwin/release/$APP_NAME.app"

echo "Removed old app"

# Make the new app
mkdir -p "universal-apple-darwin/release/$APP_NAME.app/Contents/MacOS"
mkdir -p "universal-apple-darwin/release/$APP_NAME.app/Contents/Resources"

echo "Created directory"

cp "universal-apple-darwin/release/$EXECUTABLE_NAME" "universal-apple-darwin/release/$APP_NAME.app/Contents/MacOS/$EXECUTABLE_NAME"
cp "../resources/installer.jar" "universal-apple-darwin/release/$APP_NAME.app/Contents/Resources/installer.jar"
cp "../resources/macos-icon.icns" "universal-apple-darwin/release/$APP_NAME.app/Contents/Resources/icon.icns"

echo "Copied files"

cd universal-apple-darwin/release || exit

cat >"$APP_NAME.app/Contents/Info.plist" <<EOL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>CFBundleExecutable</key>
	<string>$EXECUTABLE_NAME</string>
	<key>CFBundleIconFile</key>
	<string>icon.icns</string>
	<key>CFBundleIdentifier</key>
	<string>$APP_ID</string>
	<key>CFBundleName</key>
	<string>$APP_NAME</string>
	<key>CFBundleDisplayName</key>
	<string>$APP_NAME</string>
	<key>CFBundlePackageType</key>
	<string>APPL</string>
	<key>CFBundleShortVersionString</key>
	<string>3.1.1</string>
	<key>CFBundleVersion</key>
	<string>1</string>
	<key>NSHighResolutionCapable</key>
	<true />
</dict>
</plist>
EOL

echo "Wrote plist file"

if [ -e "$APP_NAME.app.zip" ]; then
  rm "$APP_NAME.app.zip"
fi
zip -rX9 "$APP_NAME.app.zip" "$APP_NAME.app"

echo "Compressed .app"

echo "Done!"
echo "App is located in wrapper/target/universal-apple-darwin/release"
