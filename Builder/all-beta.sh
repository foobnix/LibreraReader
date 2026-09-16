#!/usr/bin/env bash
#./fonts.sh

# every translation must have the same number of lines as the English strings.xml
RES="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )/../app/src/main/res"
if [ ! -f "$RES/values/strings.xml" ]; then
  echo "ERROR: $RES/values/strings.xml not found"
  exit 1
fi
EN_LINES=$(wc -l < "$RES/values/strings.xml" | tr -d " ")
TRANSLATION_ERROR=0
for FILE in "$RES"/values-*/strings.xml; do
  LINES=$(wc -l < "$FILE" | tr -d " ")
  if [ "$LINES" -ne "$EN_LINES" ]; then
    echo "ERROR: $(basename "$(dirname "$FILE")")/strings.xml has $LINES lines, values/strings.xml (en) has $EN_LINES"
    TRANSLATION_ERROR=1
  fi
done
if [ "$TRANSLATION_ERROR" -ne 0 ]; then
  echo "ERROR: not all strings are translated"
  exit 1
fi
echo "Translations OK: $EN_LINES lines"

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  export JAVA_HOME=/home/dev/.local/share/JetBrains/Toolbox/apps/android-studio/jbr
fi


./link_to_mupdf_1.28.3.sh

cd ../

./gradlew clean incVersion
./gradlew assembleProRelease
./gradlew assembleLibreraRelease
./gradlew assembleFdroidRelease

####################################

./gradlew copyApks -Pbeta
./gradlew -stop

####################################


#rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-x86*
#rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-arm.apk

cd Builder
./remove_all.sh
./install_all.sh