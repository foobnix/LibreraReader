. ~/.profile
#MUPDF=/home/ivan-dev/dev/mupdf-1.11-source/platform/java
MUPDF_ROOT=/home/ivan-dev/dev/mupdf-1.12.0-source
MUPDF_JAVA=$MUPDF_ROOT/platform/java

MY=/home/ivan-dev/git/LirbiReader

rm -rf  $MUPDF_JAVA/jni
cp -rRp $MY/Builder/jni-1.12 $MUPDF_JAVA/jni


ALL=$MY/cpu_all/libs
ARM=$MY/cpu_arm/libs
ARM64=$MY/cpu_arm64/libs
ARM_ARM64=$MY/cpu_arm+arm64/libs
X86=$MY/cpu_x86/libs

rm -r $ALL
rm -r $ARM
rm -r $ARM64
rm -r $ARM_ARM64
rm -r $X86

mkdir $ALL
mkdir $ARM
mkdir $ARM64
mkdir $ARM_ARM64
mkdir $X86

ln -s $MUPDF_JAVA/libs $ALL
ln -s $MUPDF_JAVA/libs/armeabi-v7a $ARM/armeabi-v7a
ln -s $MUPDF_JAVA/libs/arm64-v8a $ARM64/arm64-v8a

ln -s $MUPDF_JAVA/libs/armeabi-v7a $ARM_ARM64/armeabi-v7a
ln -s $MUPDF_JAVA/libs/arm64-v8a $ARM_ARM64/arm64-v8a

ln -s $MUPDF_JAVA/libs/x86 $X86/x86
ln -s $MUPDF_JAVA/libs/x86_64 $X86/x86_64

#echo "MUPDF:" $MUPDF ">>>" $MY

cp -rp $MY/Builder/jni-1.12/~mupdf/epub-doc.c $MUPDF_ROOT/source/html/epub-doc.c
cp -rp $MY/Builder/jni-1.12/~mupdf/css-apply.c $MUPDF_ROOT/source/html/css-apply.c
cp -rp $MY/Builder/jni-1.12/~mupdf/html-layout.c $MUPDF_ROOT/source/html/html-layout.c
cp -rp $MY/Builder/jni-1.12/~mupdf/colorspace.c $MUPDF_ROOT/source/fitz/colorspace.c
cp -rp $MY/Builder/jni-1.12/~mupdf/xml.c $MUPDF_ROOT/source/fitz/xml.c

cd $MUPDF_JAVA
ndk-build