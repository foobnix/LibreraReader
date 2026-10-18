#!/usr/bin/env bash
#
# Builds libLibreraDocs.so - DjVu, the .doc and .mobi converters and the bitmap scaler, without
# MuPDF - for the flavours whose names end in JS. See jni/Android-nomupdf.mk.
#
# It runs against the same jni/ tree the MuPDF build uses, but with its own Android.mk and its own
# output directories, so nothing the MuPDF build wrote is touched:
#
#   platform/librera/libs-nomupdf/<abi>/libLibreraDocs.so   <- app/src/noMupdf/jniLibs
#   platform/librera/obj-nomupdf/
#
# Run link_to_mupdf_<version>.sh at least once first: that is what copies jni/ into the MuPDF
# tree. MuPDF itself does not have to have been built - none of the sources here need it.
#
# Usage: ./build-nomupdf.sh [version] [clean]

set -e

BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$BUILD_DIR"

VERSION_TAG="${1:-1.28.5}"
MUPDF_JAVA="$BUILD_DIR/mupdf-$VERSION_TAG/platform/librera"
LIBS="$BUILD_DIR/../app/src/noMupdf/jniLibs"

if [ ! -d "$MUPDF_JAVA/jni" ]; then
    echo "-- ERROR --"
    echo "[$MUPDF_JAVA/jni] not found, run ./link_to_mupdf_$VERSION_TAG.sh first"
    exit 1
fi

# The jni/ copy inside the MuPDF tree is made by the link script and is not in git, so an edit to
# the master tree here has to reach it. Android-nomupdf.mk is new, and the link script of an older
# checkout would not have carried it over.
cp -p "$BUILD_DIR/jni/Android-nomupdf.mk" "$MUPDF_JAVA/jni/Android-nomupdf.mk"
cp -p "$BUILD_DIR/jni/ebookdroidjni.c" "$MUPDF_JAVA/jni/ebookdroidjni.c"

# The NDK: librera_ndk_dir in the global ~/.gradle/gradle.properties, the same one the MuPDF
# build reads.
GRADLE_PROPERTIES="${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties"
NDK_DIR=$(sed -n 's/^librera_ndk_dir=//p' "$GRADLE_PROPERTIES" | tail -n 1)
NDK="$NDK_DIR/ndk-build"

if [ ! -f "$NDK" ]; then
    echo "-- NDK ERROR --"
    echo "[$NDK] NDK NOT FOUND, set librera_ndk_dir in $GRADLE_PROPERTIES"
    exit 1
fi

cd "$MUPDF_JAVA"

NDK_ARGS=(
    NDK_APPLICATION_MK=jni/Application.mk
    APP_BUILD_SCRIPT=jni/Android-nomupdf.mk
    NDK_OUT=obj-nomupdf
    NDK_LIBS_OUT=libs-nomupdf
    APP_PLATFORM=android-24
)

if [ "$2" == "clean" ]; then
    rm -rf "$MUPDF_JAVA/obj-nomupdf" "$MUPDF_JAVA/libs-nomupdf"
fi

"$NDK" "${NDK_ARGS[@]}" APP_ABI=armeabi-v7a &
"$NDK" "${NDK_ARGS[@]}" APP_ABI=arm64-v8a &
"$NDK" "${NDK_ARGS[@]}" APP_ABI=x86 &
"$NDK" "${NDK_ARGS[@]}" APP_ABI=x86_64
wait

# The app reads the libraries through a source set of its own, so that the flavours built with
# MuPDF never see this library and the flavours built without it never see libMuPDF.so.
rm -rf "$LIBS"
mkdir -p "$LIBS"
for abi in armeabi-v7a arm64-v8a x86 x86_64; do
    ln -s "$MUPDF_JAVA/libs-nomupdf/$abi" "$LIBS/$abi"
done

echo "=================="
echo "NDK:   $NDK"
echo "BUILT: $MUPDF_JAVA/libs-nomupdf"
echo "JNI:   $LIBS"
ls -l "$MUPDF_JAVA/libs-nomupdf"/*/*.so
echo "=================="
