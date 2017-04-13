#NDK_TOOLCHAIN_VERSION := 4.9

APP_STL := gnustl_static
APP_CPPFLAGS += -std=c++11

APP_OPTIM := release

APP_PLATFORM := android-12
#APP_STL := gnustl_static
#APP_CPPFLAGS += -frtti 
#APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -DANDROID

APP_ARM_MODE := arm

#APP_CFLAGS += -DTHREADMODEL=POSIXTHREADS
#APP_CPPFLAGS += -fexceptions 


APP_OPTIM := release
APP_ABI := x86
