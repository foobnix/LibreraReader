. ~/.profile
MUPDF=/home/ivan-dev/dev/mupdf-1.11-source/platform/java
MY=/home/ivan-dev/git/LirbiReader

rm -rf  $MUPDF/jni
cp -rR $MY/Builder/jni $MUPDF/jni


ALL=$MY/cpu_all/libs
ARM=$MY/cpu_arm/libs
ARM64=$MY/cpu_arm64/libs
ARM_ARM64=$MY/cpu_arm+arm64/libs
X86=$MY/cpu_x86/libs

rm -r $ALL
rm -r $ARM
rm -r $ARM64
rm -r $X86

mkdir $ARM
mkdir $ARM64
mkdir $X86

ln -s $MUPDF/libs $ALL
ln -s $MUPDF/libs/armeabi-v7a $ARM/armeabi-v7a
ln -s $MUPDF/libs/arm64-v8a $ARM64/arm64-v8a

ln -s $MUPDF/libs/armeabi-v7a $ARM_ARM64/armeabi-v7a
ln -s $MUPDF/libs/arm64-v8a $ARM_ARM64/arm64-v8a

ln -s $MUPDF/libs/x86 $X86/x86

echo "MUPDF:" $MUPDF ">>>" $MY

#cd $MUPDF
#ndk-build