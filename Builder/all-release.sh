#!/usr/bin/env bash

#git reset --hard
#git pull

./fonts.sh

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  export JAVA_HOME=/home/dev/.local/share/JetBrains/Toolbox/apps/android-studio/jbr
fi
####################################

./link_to_mupdf_1.28.3.sh

cd ../

#./gradlew clean incVersion
./gradlew clean

#./gradlew assemblePdf_v2Release bundlePdf_v2Release
#./gradlew assembleEbookaRelease bundleEbookaRelease


./gradlew assembleLibreraRelease
./gradlew assemblePdf_v2Release
./gradlew assembleEbookaRelease
./gradlew assemblePdf_classicRelease
./gradlew assembleTts_readerRelease
./gradlew assembleEpub_readerRelease
./gradlew assembleProRelease
./gradlew assembleTts_readerRelease
./gradlew assembleEpub_readerRelease
./gradlew assembleFdroidRelease

./gradlew copyApks -Prelease
./gradlew -stop

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*.apk /home/dev/Dropbox/FREE_PDF_APK/testing/*.aab /home/dev/Dropbox/FREE_PDF_APK/testing/*-mapping.txt
rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*.apk /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*.aab /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-mapping.txt

cd Builder
./remove_all.sh
