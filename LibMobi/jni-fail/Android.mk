LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)


include $(TOP_LOCAL_PATH)/djvu/Android.mk
include $(TOP_LOCAL_PATH)/simd/Android.mk
include $(TOP_LOCAL_PATH)/jpeg-turbo/Android.mk
#include $(TOP_LOCAL_PATH)/standalone/Android.mk

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -lm -llog -ljnigraphics

LOCAL_MODULE    := mydjvu

include $(BUILD_SHARED_LIBRARY)