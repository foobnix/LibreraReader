#!/usr/bin/env bash
#. ~/.profile

git clone --recursive git://git.ghostscript.com/mupdf.git mupdf-master
cd mupdf-master
git fetch --all
git reset --hard origin/master
git status

#make clean
make release
make generate

#make OS=mingw32-cross
cd ..

MUPDF_ROOT=/home/ivan-dev/git/LibreraReader/Builder/mupdf-master

MUPDF_JAVA=$MUPDF_ROOT/platform/java

LIBS=/home/ivan-dev/git/LibreraReader/app/src/main/jniLibs

rm -rf  $MUPDF_JAVA/jni
cp -rRp jni-master $MUPDF_JAVA/jni

rm -r $LIBS
mkdir $LIBS

ln -s $MUPDF_JAVA/libs/armeabi-v7a $LIBS
ln -s $MUPDF_JAVA/libs/arm64-v8a $LIBS
ln -s $MUPDF_JAVA/libs/x86 $LIBS
ln -s $MUPDF_JAVA/libs/x86_64 $LIBS



cd $MUPDF_JAVA
ndk-build $1

echo "MUPDF:" $MUPDF_JAVA
echo "LIBS:"  $LIBS