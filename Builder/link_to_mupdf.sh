MUPDF=/home/ivan-dev/dev/mupdf-android-viewer-mini/libmupdf/platform/java
MY=/home/ivan-dev/git/LirbiReader

rm -rf  $MUPDF/jni
cp -rR $MY/Builder/jni $MUPDF/jni


ALL=$MY/cpu_all/libs
ARM_V7=$MY/arm_v7a/libs
X86=$MY/cpu_x86/libs

rm -r $ALL
rm -r $ARM_V7
rm -r $X86

mkdir $ARM_V7
mkdir $X86

ln -s $MUPDF/libs $ALL
ln -s $MUPDF/libs/armeabi-v7a $ARM_V7/armeabi-v7a
ln -s $MUPDF/libs/x86 $X86/x86

echo $MUPDF ">>>" $MY

cd $MUPDF
#ndk-build