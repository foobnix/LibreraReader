#!/usr/bin/env bash
#. ~/.profile

# get the location of this script, we will checkout mupdf into the same directory
BUILD_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

cd $BUILD_DIR

echo "MUPDF : master"
echo "================== "
git clone --recursive --jobs 8 git://git.ghostscript.com/mupdf.git mupdf-master
cd mupdf-master


#git reset --hard 1fdc3e9bcdaf1a3746557178542f8ffdf988a377

git reset --hard
git submodule foreach --recursive git reset --hard
git pull --recurse-submodules


echo "=================="
git log -n 20 --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit
echo "=================="
#reset
git log -n 1 --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit

echo -e "\e[39m=================="
#make clean
make -j4 release
make -j4 generate

cd ..

MUPDF_ROOT=${BUILD_DIR}/mupdf-master

MUPDF_JAVA=$MUPDF_ROOT/platform/java

LIBS=${BUILD_DIR}/../app/src/main/jniLibs

SRC=jni/~mupdf-master
DEST=$MUPDF_ROOT/source/

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni $MUPDF_JAVA/jni
mv $MUPDF_JAVA/jni/Android-master.mk $MUPDF_JAVA/jni/Android.mk

#rm -rf ${BUILD_DIR}/../app/src/main/java/com/artifex
#cp -rRp $MUPDF_JAVA/src/com/artifex ${BUILD_DIR}/../app/src/main/java/com


rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS

#cp -rp $SRC/css-apply.c    $DEST/html/css-apply.c
#cp -rp $SRC/epub-doc.c     $DEST/html/epub-doc.c
#cp -rp $SRC/html-layout.c  $DEST/html/html-layout.c
#cp -rp $SRC/html-parse.c   $DEST/html/html-parse.c
#cp -rp $SRC/mucbz.c        $DEST/cbz/mucbz.c


cd $MUPDF_JAVA
echo "=================="
ndk-build -j4 $1
echo "=================="
echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS
echo "=================="
