LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_MODULE    := hqx
LOCAL_SRC_FILES := init.c hq2x.c hq3x.c  hq4x.c

include $(BUILD_STATIC_LIBRARY)
