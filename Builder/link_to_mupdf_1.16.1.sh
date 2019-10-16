#!/usr/bin/env bash
#. ~/.profile

git clone --recursive git://git.ghostscript.com/mupdf.git --branch 1.16.1 mupdf-1.16.1
cd mupdf-1.16.1

#make clean
make release
make generate

#make OS=mingw32-cross
cd ..

MUPDF_ROOT=/home/ivan-dev/git/LibreraReader/Builder/mupdf-1.16.1

MUPDF_JAVA=$MUPDF_ROOT/platform/java

LIBS=/home/ivan-dev/git/LibreraReader/app/src/main/jniLibs

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni-1.16.1 $MUPDF_JAVA/jni

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS

cp -rp jni-1.16.1/~mupdf/css-apply.c $MUPDF_ROOT/source/html/css-apply.c
cp -rp jni-1.16.1/~mupdf/epub-doc.c $MUPDF_ROOT/source/html/epub-doc.c
cp -rp jni-1.16.1/~mupdf/html-doc.c $MUPDF_ROOT/source/html/html-doc.c
cp -rp jni-1.16.1/~mupdf/html-layout.c $MUPDF_ROOT/source/html/html-layout.c
cp -rp jni-1.16.1/~mupdf/html-parse.c $MUPDF_ROOT/source/html/html-parse.c

cp -rp jni-1.16.1/~mupdf/mucbz.c $MUPDF_ROOT/source/cbz/mucbz.c
cp -rp jni-1.16.1/~mupdf/xml.c $MUPDF_ROOT/source/fitz/xml.c
cp -rp jni-1.16.1/~mupdf/svg-doc.c $MUPDF_ROOT/source/svg/svg-doc.c


cd $MUPDF_JAVA
ndk-build $1

echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS