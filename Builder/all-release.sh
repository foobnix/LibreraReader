#!/usr/bin/env bash

# The whole release, built on one MuPDF: ./all-release.sh 1.28.4
# (or through all-release-1.23.7.sh / all-release-1.28.4.sh). The MuPDF version goes into the
# names of the APKs, bundles, mappings and native symbols, so both releases sit side by side.
MUPDF=$1
if [ -z "$MUPDF" ] || [ ! -f "./link_to_mupdf_$MUPDF.sh" ]; then
  echo "Usage: $0 <mupdf version>, one of:" $(ls link_to_mupdf_*.sh | sed 's/link_to_mupdf_//; s/\.sh//')
  exit 1
fi

#git reset --hard
#git pull

./fonts.sh || exit 1

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  # librera_java_home in the global ~/.gradle/gradle.properties, else the JDK of Android Studio
  LIBRERA_JAVA_HOME=$(sed -n 's/^librera_java_home=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
  export JAVA_HOME="${LIBRERA_JAVA_HOME:-$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr}"
fi
####################################

./link_to_mupdf_$MUPDF.sh || exit 1

cd ../

#./gradlew clean incVersion
./gradlew clean || exit 1
./gradlew updateFDroid || exit 1

#./gradlew assemblePdf_v2Release bundlePdf_v2Release
#./gradlew assembleEbookaRelease bundleEbookaRelease


./gradlew assembleLibreraRelease -Pmupdf=$MUPDF || exit 1
./gradlew assemblePdf_v2Release -Pmupdf=$MUPDF || exit 1
./gradlew assembleEbookaRelease -Pmupdf=$MUPDF || exit 1
./gradlew assemblePdf_classicRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleTts_readerRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleEpub_readerRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleProRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleTts_readerRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleEpub_readerRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleFdroidRelease -Pmupdf=$MUPDF || exit 1

./gradlew copyApks -Prelease -Pmupdf=$MUPDF || exit 1
./gradlew -stop

# Where copyApks puts the builds: librera_builds_dir in the global ~/.gradle/gradle.properties
BUILDS_DIR=$(sed -n 's/^librera_builds_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ -z "$BUILDS_DIR" ]; then
  echo "ERROR: librera_builds_dir is not set in ~/.gradle/gradle.properties"
  exit 1
fi

# Native debug symbols for Play Console crash reports (App bundle explorer > Downloads > Assets):
# debug info of the unstripped libMuPDF.so from ndk-build, one folder per ABI
VERSION_BASE=$(sed -n 's/^appVersionNumberBase=//p' app/gradle.properties)
VERSION_INDEX=$(sed -n 's/^appVersionNumberIndex=//p' app/gradle.properties)
if [ -z "$VERSION_BASE" ] || [ -z "$VERSION_INDEX" ]; then
  echo "ERROR: no version in app/gradle.properties"
  exit 1
fi
VERSION=$VERSION_BASE.$VERSION_INDEX
RELEASE_DIR="$BUILDS_DIR/$VERSION"
# llvm-objcopy of the NDK: librera_ndk_dir in the global ~/.gradle/gradle.properties
NDK_DIR=$(sed -n 's/^librera_ndk_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ -z "$NDK_DIR" ]; then
  echo "ERROR: librera_ndk_dir is not set in ~/.gradle/gradle.properties"
  exit 1
fi
if [ "$(uname)" == "Darwin" ]; then
  OBJCOPY="$NDK_DIR/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-objcopy"
else
  OBJCOPY="$NDK_DIR/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objcopy"
fi
SYMBOLS=$(mktemp -d)
for ABI in armeabi-v7a arm64-v8a x86 x86_64; do
  mkdir -p "$SYMBOLS/$ABI"
  "$OBJCOPY" --only-keep-debug Builder/mupdf-$MUPDF/platform/librera/obj/local/$ABI/libMuPDF.so "$SYMBOLS/$ABI/libMuPDF.so.dbg" || exit 1
done
mkdir -p "$RELEASE_DIR"
rm -f "$RELEASE_DIR/Librera-$VERSION-mupdf$MUPDF-native-debug-symbols.zip"
(cd "$SYMBOLS" && zip -qr "$RELEASE_DIR/Librera-$VERSION-mupdf$MUPDF-native-debug-symbols.zip" .)
rm -rf "$SYMBOLS"
# Native debug end

rm -f "$BUILDS_DIR"/*.apk "$BUILDS_DIR"/*.aab "$BUILDS_DIR"/*-mapping.txt

cd Builder
./remove_all.sh
