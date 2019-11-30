#!/usr/bin/env bash
#. ~/.profile

git clone --recursive git://git.ghostscript.com/mupdf.git mupdf-master
cd mupdf-master
git reset --hard origin/master
git fetch --all

echo "=================="
git log -n 20 --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit
echo "=================="
echo -e "\e[33m `git reset --hard 06aec98986c9b471a22762b3a04a9fb69c4bdcd0`"
git log -n 1 --graph --pretty=format:'%Cred%h%Creset -%C(yellow)%d%Creset %s %Cgreen(%cr) %C(bold blue)<%an>%Creset' --abbrev-commit

echo -e "\e[39m=================="

make release
make generate

cd ..

LIBRERA_READER_ROOT="$(dirname "$(dirname "$(realpath "${BASH_SOURCE[0]}")")")"
MUPDF_ROOT="$LIBRERA_READER_ROOT"/Builder/mupdf-master

MUPDF_JAVA="$MUPDF_ROOT"/platform/java

LIBS="$LIBRERA_READER_ROOT"/app/src/main/jniLibs

rm -rf  "$MUPDF_JAVA"/jni
cp -rRp jni-master "$MUPDF_JAVA"/jni

rm -r "$LIBS"
mkdir "$LIBS"

ln -s "$MUPDF_JAVA"/libs/armeabi-v7a "$LIBS"
ln -s "$MUPDF_JAVA"/libs/arm64-v8a "$LIBS"
ln -s "$MUPDF_JAVA"/libs/x86 "$LIBS"
ln -s "$MUPDF_JAVA"/libs/x86_64 "$LIBS"

cd "$MUPDF_JAVA"
echo "=================="
ndk-build "$1"
echo "=================="
echo "MUPDF:" "$MUPDF_JAVA"
echo "LIBS:"  "$LIBS"
echo "=================="
