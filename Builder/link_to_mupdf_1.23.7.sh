#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

VERSION_TAG="1.23.7"
MUPDF_FOLDER=mupdf-$VERSION_TAG

if [ "$1" == "fdroid" ]; then
  MUPDF_FOLDER=$MUPDF_FOLDER-fdroid
fi

# 克隆下载到
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/mupdf-1.23.7-fdroid

git clone --recursive git://git.ghostscript.com/mupdf.git --branch $VERSION_TAG $MUPDF_FOLDER

MUPDF_ROOT=$BUILD_DIR/$MUPDF_FOLDER

# 创建文件夹
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/mupdf-1.23.7-fdroid/platform

MUPDF_JAVA=$MUPDF_ROOT/platform/librera
mkdir -p $MUPDF_JAVA/jni

# 源码目录
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/jni/~mupdf-1.23.7

SRC=jni/~mupdf-$VERSION_TAG

# 目标目录
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/mupdf-1.23.7-fdroid/source

DEST=$MUPDF_ROOT/source

# lib 目录
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/app/src/main/jniLibs
LIBS=$BUILD_DIR/../app/src/main/jniLibs

echo "MUPDF :" $VERSION_TAG
echo "================== "

mkdir $SRC
mkdir $MUPDF_FOLDER

cd $MUPDF_FOLDER

echo "=================="

# link_to_mupdf_1.23.7.sh  clean
# 参数  clean  清除 mupdf make 的编译
if [ "$1" == "clean" ]; then
  git reset --hard &&  git clean -f -d
  rm -rf generated
  rm -rf build
  make clean
fi

# 没有 release 文件夹，则编译
if [ ! -d "build/release" ]; then
  make generate
  make release
fi

cd ..

# 复制，从.到
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/jni/Android-1.23.7.mk
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/mupdf-1.23.7-fdroid/platform/librera/jni/Android.mk
rm -rf  $MUPDF_JAVA/jni
cp -Rp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-$VERSION_TAG.mk $MUPDF_JAVA/jni/Android.mk


rm -r $LIBS
mkdir $LIBS

# 软链接 so 文件
# /mnt/d/wksp/OneDrive/Github/Code_Android/LibreraReader_work/Builder/mupdf-1.23.7-fdroid/platform/librera/libs/arm64-v8a/libMuPDF.so

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
cp -rpv $DEST/fitz/list-device.c  $SRC/list-device.c

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
cp -rpv $SRC/list-device.c       $DEST/fitz/list-device.c

cp -rpv $SRC/image-imp.h         $DEST/fitz/image-imp.h
cp -rpv $SRC/compressed-buffer.h $MUPDF_ROOT/include/mupdf/fitz/compressed-buffer.h
cp -rpv $SRC/context.h $MUPDF_ROOT/include/mupdf/fitz/context.h

cd $MUPDF_JAVA

# 设置 NDK 版本

NDK_VERSION="27.2.12479018"
FDRIOD_NDK_VERSION="21.4.7075529"
if [ "$(uname)" == "Darwin" ]; then
  FDRIOD_NDK_VERSION=$NDK_VERSION
fi

# 设置我的本地 NDK 目录
PATH1=/Volumes/SSD-USB/Android/Sdk/ndk
# PATH2=/home/dev/Android/Sdk/ndk
PATH2=/mnt/d/0000_opt/Android/Sdk/ndk

# 清除之前的编译
# link_to_mupdf_1.23.7.sh  clean_ndk  fdroid

if [ "$1" == "clean_ndk" ]; then
  if [ "$2" == "fdroid" ]; then
   $PATH1/$FDRIOD_NDK_VERSION/ndk-build clean
   $PATH2/$FDRIOD_NDK_VERSION/ndk-build clean
  else
   $PATH1/$NDK_VERSION/ndk-build clean
   $PATH2/$NDK_VERSION/ndk-build clean
  fi
rm -rf $MUPDF_JAVA/obj
fi

# 编译， API 16 ， 可以不加参数，在 Windows 的 android studio 里面编译
# link_to_mupdf_1.23.7.sh  fdroid
if [ "$1" == "fdroid" ]; then
  for NDK in "$PATH1/$FDRIOD_NDK_VERSION/ndk-build" "$PATH2/$FDRIOD_NDK_VERSION/ndk-build";
    do
      if [ -f "$NDK" ]; then
      $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=armeabi-v7a APP_PLATFORM=android-16 &
      $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=arm64-v8a   APP_PLATFORM=android-16 &
      $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=x86         APP_PLATFORM=android-16 &
      $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=x86_64      APP_PLATFORM=android-16
      echo "=================="
      echo "NDK:"  $NDK
      echo "APP_PLATFORM=android-16"
      fi
    done
else
  for NDK in "$PATH1/$NDK_VERSION/ndk-build" "$PATH2/$NDK_VERSION/ndk-build";
  do
    if [ -f "$NDK" ]; then
    $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=armeabi-v7a APP_PLATFORM=android-21 &
    $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=arm64-v8a   APP_PLATFORM=android-21 &
    $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=x86         APP_PLATFORM=android-21 &
    $NDK NDK_APPLICATION_MK=jni/Application.mk APP_ABI=x86_64      APP_PLATFORM=android-21
    echo "=================="
    echo "NDK:"  $NDK
    echo "APP_PLATFORM=android-21"
    fi
  done

fi

echo "=================="
echo "MUPDF:"$MUPDF_JAVA
echo "JNI:"$LIBS
echo "=================="
fi