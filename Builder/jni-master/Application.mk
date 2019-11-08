APP_PLATFORM=android-16
APP_ABI := x86 x86_64 armeabi-v7a arm64-v8a
#APP_ABI := arm64-v8a

MY_ARM_MODE := arm
LOCAL_ARM_MODE := $(MY_ARM_MODE)

MY_O = -O2

APP_OPTIM := release


APP_CFLAGS := -O2

APP_CFLAGS += $(MY_O)
APP_STL := c++_static # gnustl_static need for djvuLibre
#c++_static or c++_shared


#NDK_TOOLCHAIN_VERSION=4.9