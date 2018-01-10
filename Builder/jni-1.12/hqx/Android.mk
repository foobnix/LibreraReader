LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_MODULE    := hqx
LOCAL_SRC_FILES := hq4x.c hq2x.c hq3x.c
LOCAL_CFLAGS += $(MY_O)

include $(BUILD_STATIC_LIBRARY)
