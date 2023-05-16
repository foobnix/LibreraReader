LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)

MUPDF_ROOT := $(realpath $(LOCAL_PATH)/../../..)


include $(TOP_LOCAL_PATH)/libmobi-0.11/Android.mk
include $(TOP_LOCAL_PATH)/libhqx-1.2/Android.mk
include $(TOP_LOCAL_PATH)/libdjvu-3.5.28/Android.mk
include $(TOP_LOCAL_PATH)/libantiword-1.3.1/Android.mk
include $(TOP_LOCAL_PATH)/libwebp-1.2.2/Android.mk

include $(TOP_LOCAL_PATH)/MuPDF-1.22.1.mk

include $(CLEAR_VARS)

LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_ARM_NEON := true

LOCAL_C_INCLUDES := \
	$(MUPDF_ROOT)/include \
	$(MUPDF_ROOT)/source/fitz \
	$(MUPDF_ROOT)/source/pdf \
	$(TOP_LOCAL_PATH)/libdjvu-3.5.28/src \
	$(TOP_LOCAL_PATH)/libmobi-0.9/src \
	$(TOP_LOCAL_PATH)/libmobi-0.9/tools \
	$(TOP_LOCAL_PATH)/libhqx-1.2 \
	$(TOP_LOCAL_PATH)
    	
LOCAL_CFLAGS := -DHAVE_ANDROID
LOCAL_MODULE := MuPDF

LOCAL_SRC_FILES := \
	ebookdroidjni.c \
	DjvuDroidBridge.cpp \
	cbdroidbridge.c \
	jni_concurrent.c \
	androidfonts.c \
	libmupdf-1.21.1.c


LOCAL_STATIC_LIBRARIES := djvu hqx mupdf_java
LOCAL_WHOLE_STATIC_LIBRARIES:= antiword libmobi

LOCAL_LDLIBS = -ljnigraphics
LOCAL_LDLIBS += -llog
LOCAL_LDLIBS += -lz
LOCAL_LDLIBS += -lm



include $(BUILD_SHARED_LIBRARY)
