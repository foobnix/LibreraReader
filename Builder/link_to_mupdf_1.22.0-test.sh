#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

VERSION_TAG="1.22.0-test"
git clone --recursive git://git.ghostscript.com/mupdf.git --branch 1.22.0 mupdf-$VERSION_TAG

MUPDF_ROOT=$BUILD_DIR/mupdf-$VERSION_TAG

MUPDF_JAVA=$MUPDF_ROOT/platform/librera
mkdir -p $MUPDF_JAVA/jni

SRC=jni/~mupdf-$VERSION_TAG
DEST=$MUPDF_ROOT/source
LIBS=$BUILD_DIR/../app/src/main/jniLibs


echo "MUPDF :" $VERSION_TAG
echo "================== "
mkdir mupdf-$VERSION_TAG

cd mupdf-$VERSION_TAG

echo "=================="

if [ "$1" == "clean" ]; then
  rm -rf generated
  rm -rf build
  make clean
fi

if [ ! -d "build/release" ]; then
  make generate
  make release
fi

make release