#!/usr/bin/env bash

# The whole release, built on one MuPDF: ./all-release.sh 1.28.5
# (or through all-release-1.23.7.sh / all-release-1.28.5.sh). The MuPDF version goes into the
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

./gradlew assembleFdroidJSRelease -Pmupdf=$MUPDF || exit 1
./gradlew assembleProJSRelease -Pmupdf=$MUPDF || exit 1


./gradlew copyApks -Prelease -Pmupdf=$MUPDF || exit 1
./gradlew -stop

# Native debug symbols for Play Console crash reports (App bundle explorer > Downloads > Assets):
# debug info of every unstripped library from ndk-build - libmupdf.so and the ones of
# Builder/jni/Librera-libs.mk, libarchive.so and liblame.so - one folder per ABI
VERSION_BASE=$(sed -n 's/^appVersionNumberBase=//p' app/gradle.properties)
VERSION_INDEX=$(sed -n 's/^appVersionNumberIndex=//p' app/gradle.properties)
if [ -z "$VERSION_BASE" ] || [ -z "$VERSION_INDEX" ]; then
  echo "ERROR: no version in app/gradle.properties"
  exit 1
fi
VERSION=$VERSION_BASE.$VERSION_INDEX
# The version's folder in librera_release_dir, where copyApks put the APKs, bundles and mappings.
# The symbols stay there: the Testing folders (librera_builds_dir, librera_beta_dir) only get the
# builds themselves.
RELEASE_DIR=$(sed -n 's/^librera_release_dir=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
if [ -z "$RELEASE_DIR" ]; then
  echo "ERROR: librera_release_dir is not set in ~/.gradle/gradle.properties"
  exit 1
fi
RELEASE_DIRS=("$RELEASE_DIR/$VERSION")
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
  for LIB in Builder/mupdf-$MUPDF/platform/librera/obj/local/$ABI/*.so; do
    "$OBJCOPY" --only-keep-debug "$LIB" "$SYMBOLS/$ABI/$(basename "$LIB").dbg" || exit 1
  done
done
SYMBOLS_ZIP="Librera-$VERSION-mupdf$MUPDF-native-debug-symbols.zip"
for D in "${RELEASE_DIRS[@]}"; do
  mkdir -p "$D"
  rm -f "$D/$SYMBOLS_ZIP"
  (cd "$SYMBOLS" && zip -qr "$D/$SYMBOLS_ZIP" .) || exit 1
  echo "Symbols: $D/$SYMBOLS_ZIP"
done
rm -rf "$SYMBOLS"
# Native debug end

cd Builder
./remove_all.sh
