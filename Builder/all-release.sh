#!/usr/bin/env bash

#git reset --hard
#git pull

./fonts.sh

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  # librera_java_home in the global ~/.gradle/gradle.properties, else the JDK of Android Studio
  LIBRERA_JAVA_HOME=$(sed -n 's/^librera_java_home=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
  export JAVA_HOME="${LIBRERA_JAVA_HOME:-$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr}"
fi
####################################

./link_to_mupdf_1.28.4.sh

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

# Where copyApks puts the builds: librera_builds_dir in the global ~/.gradle/gradle.properties
BUILDS_DIR=$(sed -n 's/^librera_builds_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ -z "$BUILDS_DIR" ]; then
  echo "ERROR: librera_builds_dir is not set in ~/.gradle/gradle.properties"
  exit 1
fi

# Native debug symbols for Play Console crash reports (App bundle explorer > Downloads > Assets):
# debug info of the unstripped libMuPDF.so from ndk-build, one folder per ABI
VERSION=$(sed -n 's/^appVersionNumberBase=//p' app/gradle.properties).$(sed -n 's/^appVersionNumberIndex=//p' app/gradle.properties)
RELEASE_DIR="$BUILDS_DIR/$VERSION"
# llvm-objcopy of the NDK: librera_ndk_dir in the global ~/.gradle/gradle.properties
NDK_DIR=$(sed -n 's/^librera_ndk_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ "$(uname)" == "Darwin" ]; then
  OBJCOPY="$NDK_DIR/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objcopy"
else
  OBJCOPY="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy"
fi
SYMBOLS=$(mktemp -d)
for ABI in armeabi-v7a arm64-v8a x86 x86_64; do
  mkdir -p "$SYMBOLS/$ABI"
  "$OBJCOPY" --only-keep-debug Builder/mupdf-1.28.4/platform/librera/obj/local/$ABI/libMuPDF.so "$SYMBOLS/$ABI/libMuPDF.so.dbg"
done
mkdir -p "$RELEASE_DIR"
rm -f "$RELEASE_DIR/Librera-$VERSION-native-debug-symbols.zip"
(cd "$SYMBOLS" && zip -qr "$RELEASE_DIR/Librera-$VERSION-native-debug-symbols.zip" .)
rm -rf "$SYMBOLS"
# Native debug end

rm -f "$BUILDS_DIR"/*.apk "$BUILDS_DIR"/*.aab "$BUILDS_DIR"/*-mapping.txt

cd Builder
./remove_all.sh
