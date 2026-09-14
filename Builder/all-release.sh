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

# Native debug symbols for Play Console crash reports (App bundle explorer > Downloads > Assets):
# debug info of the unstripped libMuPDF.so from ndk-build, one folder per ABI
VERSION=$(sed -n 's/^appVersionNumberBase=//p' app/gradle.properties).$(sed -n 's/^appVersionNumberIndex=//p' app/gradle.properties)
if [ "$(uname)" == "Darwin" ]; then
  OBJCOPY=/Users/ivanivanenko/Library/Android/sdk/ndk/30.0.14904198/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objcopy
  RELEASE_DIR=/Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/$VERSION
else
  OBJCOPY=/home/dev/Android/Sdk/ndk/30.0.14904198/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy
  RELEASE_DIR=/home/dev/Dropbox/FREE_PDF_APK/testing/$VERSION
fi
SYMBOLS=$(mktemp -d)
for ABI in armeabi-v7a arm64-v8a x86 x86_64; do
  mkdir -p "$SYMBOLS/$ABI"
  $OBJCOPY --only-keep-debug Builder/mupdf-1.28.3/platform/librera/obj/local/$ABI/libMuPDF.so "$SYMBOLS/$ABI/libMuPDF.so.dbg"
done
mkdir -p "$RELEASE_DIR"
rm -f "$RELEASE_DIR/Librera-$VERSION-native-debug-symbols.zip"
(cd "$SYMBOLS" && zip -qr "$RELEASE_DIR/Librera-$VERSION-native-debug-symbols.zip" .)
rm -rf "$SYMBOLS"
# Native debug end

rm /home/dev/Dropbox/FREE_PDF_APK/testing/*.apk /home/dev/Dropbox/FREE_PDF_APK/testing/*.aab /home/dev/Dropbox/FREE_PDF_APK/testing/*-mapping.txt
rm /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*.apk /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*.aab /Users/ivanivanenko/Library/CloudStorage/Dropbox/FREE_PDF_APK/testing/*-mapping.txt

cd Builder
./remove_all.sh
