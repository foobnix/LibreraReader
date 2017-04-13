MUPDF=/home/ivan-dev/dev/mupdf-test/platform/java
MY=/home/ivan-dev/git/pdf4

rm -rf  $MUPDF/jni
cp -rR $MY/Builder/jni $MUPDF/jni


rm -rf $MY/cpu_all/jni
cp -rR  $MY/Builder/jni $MY/cpu_all/jni

ALL=$MY/cpu_all/libs
ARM=$MY/arm/libs
ARM_V7=$MY/arm_v7a/libs
X86=$MY/cpu_x86/libs

rm -r $ALL
rm -r $ARM
rm -r $ARM_V7
rm -r $X86

mkdir $ARM
mkdir $ARM_V7
mkdir $X86

ln -s $MUPDF/libs $ALL
ln -s $MUPDF/libs/armeabi $ARM/armeabi
ln -s $MUPDF/libs/armeabi-v7a $ARM_V7/armeabi-v7a
ln -s $MUPDF/libs/x86 $X86/x86

cd $MUPDF
ndk-build