BRAND_LOWERCASE=$(tr '[:upper:]' '[:lower:]' < ../wrapper/resources/info/brand.txt)
FILE_NAME="$BRAND_LOWERCASE-installer-$VERSION"
MACOS_FILE_NAME="$FILE_NAME.app.zip"
WINDOWS_FILE_NAME="$FILE_NAME.exe"
#MACOS_CHECKSUM=$(sha256sum "$MACOS_FILE_NAME" | awk '{print $1}')
#WINDOWS_CHECKSUM=$(sha256sum "$WINDOWS_FILE_NAME" | awk '{print $1}')

echo "macOS: $MACOS_FILE_NAME ($MACOS_CHECKSUM)"
echo "Windows: $WINDOWS_FILE_NAME ($WINDOWS_CHECKSUM)"

curl -X 'POST' \
  -u "$RELEASE_USER:$RELEASE_PASSWORD" \
  "https://installer.essential.gg/v1/installer/$VERSION" \
  -H 'accept: application/json' \
  -H 'Content-Type: multipart/form-data' \
  -F "files=@$WINDOWS_FILE_NAME;type=application/octet-stream" \
  -F "files=@$MACOS_FILE_NAME;type=application/zip" \
  -F "request={
  \"metadata\": {
    \"$WINDOWS_FILE_NAME\": {
      \"platform\": \"WINDOWS\"
    },
    \"$MACOS_FILE_NAME\": {
      \"platform\": \"MACOS\"
    }
  }
};type=application/json"
