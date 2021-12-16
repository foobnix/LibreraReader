#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

echo "MUPDF : 1.11"
echo "================== "

git clone --recursive git://git.ghostscript.com/mupdf.git --branch 1.11 mupdf-1.11
cd mupdf-1.11
echo "================== "
make
echo "================== "
cd ..

MUPDF_ROOT=${BUILD_DIR}/mupdf-1.11

MUPDF_JAVA=$MUPDF_ROOT/platform/java

LIBS=${BUILD_DIR}/../app/src/main/jniLibs

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-1.11.mk $MUPDF_JAVA/jni/Android.mk

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS


cp -rp jni/~mupdf-1.11/epub-doc.c $MUPDF_ROOT/source/html/epub-doc.c
cp -rp jni/~mupdf-1.11/css-apply.c $MUPDF_ROOT/source/html/css-apply.c
cp -rp jni/~mupdf-1.11/html-layout.c $MUPDF_ROOT/source/html/html-layout.c
cp -rp jni/~mupdf-1.11/xml.c $MUPDF_ROOT/source/fitz/xml.c
cp -rp jni/~mupdf-1.11/stext-output.c $MUPDF_ROOT/source/fitz/stext-output.c
cp -rp jni/~mupdf-1.11/mucbz.c $MUPDF_ROOT/source/cbz/mucbz.c

cp -rp jni/~mupdf-1.11/structured-text.h $MUPDF_ROOT/include/mupdf/fitz/structured-text.h


cd $MUPDF_JAVA

whereis ndk-build
echo "================== "
ndk-build $1
echo "================== "
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "================== "
