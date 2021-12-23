#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

VERSION=1.16.1

MUPDF_ROOT=${BUILD_DIR}/mupdf-$VERSION
MUPDF_JAVA=$MUPDF_ROOT/platform/java

LIBS=${BUILD_DIR}/../app/src/main/jniLibs

SRC=jni/~mupdf-$VERSION
DEST=$MUPDF_ROOT/source/

echo "MUPDF :" $VERSION
echo "================== "
git clone --recursive git://git.ghostscript.com/mupdf.git --branch $VERSION mupdf-$VERSION
cd mupdf-$VERSION


echo "=================="
#make clean
make release
make generate
echo "=================="

cd ..

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-$VERSION.mk $MUPDF_JAVA/jni/Android.mk

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS

cp -rp $SRC/css-apply.c         $DEST/html/css-apply.c
cp -rp $SRC/epub-doc.c          $DEST/html/epub-doc.c
cp -rp $SRC/html-doc.c          $DEST/html/html-doc.c
cp -rp $SRC/html-layout.c       $DEST/html/html-layout.c
cp -rp $SRC/html-parse.c        $DEST/html/html-parse.c
cp -rp $SRC/mucbz.c             $DEST/cbz/mucbz.c
cp -rp $SRC/svg-doc.c           $DEST/svg/svg-doc.c
cp -rp $SRC/xml.c               $DEST/fitz/xml.c
cp -rp $SRC/pdf-colorspace.c    $DEST/pdf/pdf-colorspace.c

cd $MUPDF_JAVA

whereis ndk-build
echo "================== "
ndk-build $1
echo "=================="
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "=================="
