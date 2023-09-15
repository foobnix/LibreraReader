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
if [ "$1" == "clean" ]; then
  git reset --hard
  git clean -f -d
  rm -rf generated
  make clean
  make generate
  make release
fi
make generate
echo "================== "
cd ..

MUPDF_ROOT=${BUILD_DIR}/mupdf-1.11

MUPDF_JAVA=$MUPDF_ROOT/platform/librera

mkdir -p $MUPDF_JAVA/jni

LIBS=${BUILD_DIR}/../app/src/main/jniLibs

rm -rf  $MUPDF_JAVA/jni
cp -Rp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-1.11.mk $MUPDF_JAVA/jni/Android.mk

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS


cp -rp jni/~mupdf-1.11/epub-doc.c     $MUPDF_ROOT/source/html/epub-doc.c
cp -rp jni/~mupdf-1.11/css-apply.c    $MUPDF_ROOT/source/html/css-apply.c
cp -rp jni/~mupdf-1.11/html-layout.c  $MUPDF_ROOT/source/html/html-layout.c
cp -rp jni/~mupdf-1.11/xml.c          $MUPDF_ROOT/source/fitz/xml.c
cp -rp jni/~mupdf-1.11/stext-output.c $MUPDF_ROOT/source/fitz/stext-output.c
cp -rp jni/~mupdf-1.11/buffer.c       $MUPDF_ROOT/source/fitz/buffer.c
cp -rp jni/~mupdf-1.11/mucbz.c        $MUPDF_ROOT/source/cbz/mucbz.c

cp -rp jni/~mupdf-1.11/load-webp.c    $MUPDF_ROOT/source/fitz/load-webp.c
cp -rp jni/~mupdf-1.11/load-webp.h    $MUPDF_ROOT/source/fitz/load-webp.h
cp -rp jni/~mupdf-1.11/image.c        $MUPDF_ROOT/source/fitz/image.c

cp -rp jni/~mupdf-1.11/structured-text.h    $MUPDF_ROOT/include/mupdf/fitz/structured-text.h
cp -rp jni/~mupdf-1.11/compressed-buffer.h  $MUPDF_ROOT/include/mupdf/fitz/compressed-buffer.h
cp -rp jni/~mupdf-1.11/context.h  $MUPDF_ROOT/include/mupdf/fitz/context.h


cd $MUPDF_JAVA

whereis ndk-build
echo "================== "

if [ "$1" == "clean_ndk" ]; then
/home/dev/Android/Sdk/ndk/21.4.7075529/ndk-build clean
/Users/dev/Library/Android/Sdk/ndk/25.2.9519653/ndk-build clean

rm -rf $MUPDF_JAVA/obj
fi

/home/dev/Android/Sdk/ndk/21.4.7075529/ndk-build NDK_APPLICATION_MK=jni/Application-16.mk

NDK_VERSION="25.2.9519653"
for NDK in "/Users/dev/Library/Android/Sdk/ndk/$NDK_VERSION/ndk-build";
do
  if [ -f "$NDK" ]; then
  $NDK NDK_APPLICATION_MK=jni/Application-16.mk APP_ABI=armeabi-v7a &
  $NDK NDK_APPLICATION_MK=jni/Application-16.mk APP_ABI=arm64-v8a &
  $NDK NDK_APPLICATION_MK=jni/Application-16.mk APP_ABI=x86 &
  $NDK NDK_APPLICATION_MK=jni/Application-16.mk APP_ABI=x86_64
  fi
done


echo "================== "
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "================== "
