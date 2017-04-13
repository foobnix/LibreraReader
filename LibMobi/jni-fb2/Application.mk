# EbookConv project
NDK_TOOLCHAIN_VERSION := 4.8
APP_PLATFORM := android-9

#APP_CFLAGS += -Wno-write-strings -Wno-logical-op-parentheses -Wno-unsequenced -Wno-parentheses -Wno-switch -Wno-#warnings -Wno-invalid-source-encoding
#APP_CPPFLAGS += -fexceptions -Wno-logical-op-parentheses -Wno-unsequenced -Wno-parentheses -Wno-switch -Wno-#warnings -Wno-invalid-source-encoding

APP_CPPFLAGS += -fexceptions
APP_STL := stlport_static
APP_ARM_MODE := arm


APP_OPTIM := release
APP_ABI := x86
