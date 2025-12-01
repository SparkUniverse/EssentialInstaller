#!/bin/bash

set -oe pipefail

# Provided by CI, can be uncommented if manually signing
#IDENTITY="identity"
#USERNAME="apple-id email"
#PASSWORD="apple-id app specific password (go to https://appleid.apple.com./)"
#TEAM_ID="developer team id"
#VERSION="1.0.0"

BRAND=$(cat ../wrapper/resources/info/brand.txt)
BRAND_LOWERCASE=$(cat ../wrapper/resources/info/brand.txt | tr '[:upper:]' '[:lower:]')
APP_NAME="$BRAND Installer"
APP_NAME_VERSIONED="$BRAND_LOWERCASE-installer-$VERSION"

function sign() {
    codesign -s "$IDENTITY" --options=runtime --timestamp --strict --deep --force "$1"
}

# Extract the .app.zip file
unzip "$APP_NAME".app.zip

# Ensure that if any of the commands fail, that the script does not execute further.
# Also prints out the commands that are being executed.
set -xe

# Clean up any files from previous signing attempts.
rm "$APP_NAME".zip || true
rm notarisation.result || true

# We need to extract all libraries from the inner JAR to code-sign them.
# This is a required step.
pushd "$APP_NAME".app/Contents/Resources

# Extract the JAR into a temporary directory.
mkdir temp

pushd temp

jar -xf ../installer.jar

# TODO: Should we only sign the ones we need to be signed? It looks like the
#       aarch64 ones do not need to be signed.
find . -name "*.dylib" | while read fname; do
    sign "$fname"
done

find . -name "*.jnilib" | while read fname; do
    sign "$fname"
done

# Remove the existing JAR.
rm ../installer.jar

# Create a new JAR.
zip -qr ../installer.jar .

popd # temp

# Remove the temporary directory.
rm -rf temp

popd # $APP_NAME.app/Contents/Resources

# Now that we have signed everything inside the JAR, we can sign the application itself.
sign "$APP_NAME".app

# Create an archive to submit for notarization.
zip "$APP_NAME".zip -r "$APP_NAME".app

# Notarize and staple the application.
xcrun notarytool submit "$APP_NAME".zip --apple-id "$USERNAME" --team-id "$TEAM_ID" --password "$PASSWORD" --wait
xcrun stapler staple "$APP_NAME".app

# Re-zip the stapled application
rm "$APP_NAME".app.zip || true
rm "$APP_NAME_VERSIONED".app.zip || true
zip "$APP_NAME_VERSIONED".app.zip -r "$APP_NAME".app
