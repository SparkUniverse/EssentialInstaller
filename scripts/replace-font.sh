# Provided by CI
#TITLE_FONT_BASE64="..."
#TITLE_FONT_NAME="Name"

set -e

echo "Setting up font '$TITLE_FONT_NAME'"

if [ -n "$TITLE_FONT_BASE64" ] && [ -n "$TITLE_FONT_NAME" ]; then
  rm ../wrapper/resources/fonts/TitleFont.ttf
  rm ../installer/src/main/resources/fonts/TitleFont.ttf
  rm ../wrapper/resources/fonts/TitleFontName.txt
  printf "%s" "$TITLE_FONT_BASE64" | base64 -d > ../wrapper/resources/fonts/TitleFont.ttf
  printf "%s" "$TITLE_FONT_BASE64" | base64 -d > ../installer/src/main/resources/fonts/TitleFont.ttf
  printf "%s" "$TITLE_FONT_NAME" > ../wrapper/resources/fonts/TitleFontName.txt
  echo "Done!"
else
  echo "Font variables not set..."
fi

