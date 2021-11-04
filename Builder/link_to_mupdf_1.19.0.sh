#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

VERSION_TAG=1.19.0

MUPDF_ROOT=$BUILD_DIR/mupdf-$VERSION_TAG
MUPDF_JAVA=$MUPDF_ROOT/platform/librera

SRC=jni/~mupdf-$VERSION_TAG
DEST=$MUPDF_ROOT/source/
SRC_FILES=$SRC/src_files
LIBS=$BUILD_DIR/../app/src/main/jniLibs


echo "MUPDF :" $VERSION_TAG
echo "================== "
mkdir mupdf-$VERSION_TAG
git clone --recursive git://git.ghostscript.com/mupdf.git --branch $VERSION_TAG mupdf-$VERSION_TAG

cd mupdf-$VERSION_TAG

git reset --hard
#git clean -f -d

echo "=================="

if [ "$1" == "clean" ]; then
  make clean
fi

make release
make generate
echo "=================="

cd ..

mkdir -p $MUPDF_JAVA/jni

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-$VERSION_TAG.mk $MUPDF_JAVA/jni/Android.mk

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS


mkdir -p $SRC_FILES
cp -rp $MUPDF_ROOT/platform/java/Android.mk  $SRC_FILES
cp -rp $MUPDF_ROOT/platform/java/mupdf_native.c  $SRC_FILES
cp -rp $MUPDF_ROOT/platform/java/mupdf_native.h  $SRC_FILES

cp -rp $DEST/html/css-apply.c  $SRC_FILES
cp -rp $DEST/html/epub-doc.c  $SRC_FILES
cp -rp $DEST/html/html-doc.c  $SRC_FILES
cp -rp $DEST/html/html-layout.c  $SRC_FILES
cp -rp $DEST/html/html-parse.c  $SRC_FILES
cp -rp $DEST/cbz/mucbz.c  $SRC_FILES
cp -rp $DEST/svg/svg-doc.c  $SRC_FILES
cp -rp $DEST/fitz/xml.c  $SRC_FILES
cp -rp $DEST/pdf/pdf-colorspace.c  $SRC_FILES

#cp -rp $SRC/css-apply.c         $DEST/html/css-apply.c
#cp -rp $SRC/epub-doc.c          $DEST/html/epub-doc.c
#cp -rp $SRC/html-doc.c          $DEST/html/html-doc.c
#cp -rp $SRC/html-layout.c       $DEST/html/html-layout.c
#cp -rp $SRC/html-parse.c        $DEST/html/html-parse.c
#cp -rp $SRC/mucbz.c             $DEST/cbz/mucbz.c
#cp -rp $SRC/svg-doc.c           $DEST/svg/svg-doc.c
#cp -rp $SRC/xml.c               $DEST/fitz/xml.c
#cp -rp $SRC/pdf-colorspace.c    $DEST/pdf/pdf-colorspace.c

cd $MUPDF_JAVA

if [ "$1" == "clean2" ]; then
ndk-build clean
fi

ndk-build

echo "=================="
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "=================="
