git clone --recursive git://git.ghostscript.com/mupdf.git
cd mupdf
make
cd platform/android/viewer
echo "sdk.dir=/home/ivan-dev/dev/android-sdk" > local.properties
sed -s -i 's/APP_ABI := armeabi-v7a/APP_ABI := x86/g' jni/Application.mk
sed -s -i 's/#NDK_TOOLCHAIN_VERSION=4.4.3/NDK_TOOLCHAIN_VERSION=4.9/g' jni/Application.mk

ndk-build
ant debug install
