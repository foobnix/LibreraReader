#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

VERSION_TAG="master"
git clone --recursive git://git.ghostscript.com/mupdf.git mupdf-$VERSION_TAG

MUPDF_ROOT=$BUILD_DIR/mupdf-$VERSION_TAG

MUPDF_JAVA=$MUPDF_ROOT/platform/librera
mkdir -p $MUPDF_JAVA/jni

SRC=jni/~mupdf-$VERSION_TAG
DEST=$MUPDF_ROOT/source
LIBS=$BUILD_DIR/../app/src/main/jniLibs

echo "MUPDF :" $VERSION_TAG
echo "================== "

mkdir $SRC
mkdir mupdf-$VERSION_TAG

cd mupdf-$VERSION_TAG

echo "=================="

if [ "$1" == "clean" ]; then
  git reset --hard &&  git clean -f -d
  rm -rf generated
  rm -rf build
  make clean
fi

if [ ! -d "build/release" ]; then
  make generate
  make release
fi

cd ..

rm -rf  $MUPDF_JAVA/jni
cp -Rp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-$VERSION_TAG.mk $MUPDF_JAVA/jni/Android.mk


rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS

if [ "$1" == "copy" ]; then

cp -rpv $DEST/html/css-apply.c    $SRC/css-apply.c
cp -rpv $DEST/html/epub-doc.c     $SRC/epub-doc.c
cp -rpv $DEST/html/html-layout.c  $SRC/html-layout.c
cp -rpv $DEST/html/html-parse.c   $SRC/html-parse.c

cp -rpv $DEST/cbz/mucbz.c         $SRC/mucbz.c
cp -rpv $DEST/cbz/muimg.c         $SRC/muimg.c

cp -rpv $DEST/fitz/load-webp.c    $SRC/load-webp.c
cp -rpv $DEST/fitz/image.c        $SRC/image.c
cp -rpv $DEST/fitz/unzip.c        $SRC/unzip.c
cp -rpv $DEST/fitz/directory.c    $SRC/directory.c
cp -rpv $DEST/fitz/xml.c          $SRC/xml.c

cp -rpv $DEST/fitz/image-imp.h                              $SRC/image-imp.h
cp -rpv $MUPDF_ROOT/include/mupdf/fitz/compressed-buffer.h  $SRC/compressed-buffer.h
cp -rpv $MUPDF_ROOT/include/mupdf/fitz/context.h            $SRC/context.h

else

cp -rpv $SRC/css-apply.c         $DEST/html/css-apply.c
cp -rpv $SRC/epub-doc.c          $DEST/html/epub-doc.c
cp -rpv $SRC/html-layout.c       $DEST/html/html-layout.c
cp -rpv $SRC/html-parse.c        $DEST/html/html-parse.c

cp -rpv $SRC/mucbz.c             $DEST/cbz/mucbz.c
cp -rpv $SRC/muimg.c             $DEST/cbz/muimg.c

cp -rpv $SRC/load-webp.c         $DEST/fitz/load-webp.c
cp -rpv $SRC/image.c             $DEST/fitz/image.c
cp -rpv $SRC/unzip.c             $DEST/fitz/unzip.c
cp -rpv $SRC/directory.c         $DEST/fitz/directory.c
cp -rpv $SRC/xml.c               $DEST/fitz/xml.c

cp -rpv $SRC/image-imp.h         $DEST/fitz/image-imp.h
cp -rpv $SRC/compressed-buffer.h $MUPDF_ROOT/include/mupdf/fitz/compressed-buffer.h
cp -rpv $SRC/context.h $MUPDF_ROOT/include/mupdf/fitz/context.h

cd $MUPDF_JAVA

if [ "$1" == "clean_ndk" ]; then
/Users/dev/Library/Android/Sdk/ndk/25.2.9519653/ndk-build clean
/home/dev/Android/Sdk/ndk/25.2.9519653/ndk-build clean
rm -rf $MUPDF_JAVA/obj
fi

start=`date +%s`

NDK_VERSION="27.2.12479018"
for NDK in "/Users/dev/Library/Android/Sdk/ndk/$NDK_VERSION/ndk-build" "/home/dev/Android/Sdk/ndk/$NDK_VERSION/ndk-build";
do 
  if [ -f "$NDK" ]; then
  $NDK NDK_APPLICATION_MK=jni/Application-19.mk APP_ABI=armeabi-v7a &
  $NDK NDK_APPLICATION_MK=jni/Application-19.mk APP_ABI=arm64-v8a &
  $NDK NDK_APPLICATION_MK=jni/Application-19.mk APP_ABI=x86 &
  $NDK NDK_APPLICATION_MK=jni/Application-19.mk APP_ABI=x86_64
  fi
done

echo "=================="
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "=================="


if [ "$1" == "clean_ndk" ]; then
  cd $BUILD_DIR
  end=`date +%s`
  runtime=$( echo "$end - $start" | bc -l )
  echo "Run time: ${runtime}"
fi

fi