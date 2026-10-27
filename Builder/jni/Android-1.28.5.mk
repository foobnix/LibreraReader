LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)

MUPDF_ROOT := $(realpath $(LOCAL_PATH)/../../..)


include $(TOP_LOCAL_PATH)/libmobi-0.12/Android.mk
include $(TOP_LOCAL_PATH)/libhqx-1.2/Android.mk
include $(TOP_LOCAL_PATH)/libdjvu-3.5.28/Android.mk
include $(TOP_LOCAL_PATH)/libantiword-1.3.1/Android.mk
include $(TOP_LOCAL_PATH)/libwebp-1.3.2/Android.mk
include $(TOP_LOCAL_PATH)/liblame-1.0.9/Android.mk
include $(TOP_LOCAL_PATH)/libarchive-3.8.10/Android.mk

include $(TOP_LOCAL_PATH)/MuPDF-1.28.5.mk

# libdjvu.so, libmobi.so, libantiword.so and libhqx.so - see that file
include $(TOP_LOCAL_PATH)/Librera-libs.mk

# --- libmupdf.so: MuPDF and its JNI, for the flavours built with it ---
include $(CLEAR_VARS)
LOCAL_PATH := $(TOP_LOCAL_PATH)

LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_ARM_NEON := true

LOCAL_C_INCLUDES := \
	$(MUPDF_ROOT)/include \
	$(MUPDF_ROOT)/source/fitz \
	$(MUPDF_ROOT)/source/pdf \
	$(TOP_LOCAL_PATH)/libwebp-1.3.2/src \
	$(TOP_LOCAL_PATH)
    	
LOCAL_CFLAGS := -DHAVE_ANDROID
LOCAL_MODULE := mupdf

LOCAL_SRC_FILES := \
	ebookdroidjni.c \
	jni_concurrent.c \
	androidfonts.c \
	libmupdf_librera_1_28_5.c


LOCAL_STATIC_LIBRARIES := mupdf_java

LOCAL_LDLIBS = -ljnigraphics
LOCAL_LDLIBS += -llog
LOCAL_LDLIBS += -lz
LOCAL_LDLIBS += -lm
# the knobs of MuPDF's own makefile: only a shared library can honour them
LOCAL_LDLIBS += $(MUPDF_EXTRA_LDLIBS)
LOCAL_LDFLAGS += $(MUPDF_EXTRA_LDFLAGS)



include $(BUILD_SHARED_LIBRARY)
