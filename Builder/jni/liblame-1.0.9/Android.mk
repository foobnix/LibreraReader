LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS    := $(APP_CFLAGS) -DHAVE_CONFIG_H -DSTDC_HEADERS -DHAVE_MEMCPY -DHAVE_STRCHR
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_MODULE    := lame

LOCAL_SRC_FILES := lame.c \
util.c \
psymodel.c \
fft.c \
tables.c \
encoder.c \
newmdct.c \
quantize_pvt.c \
reservoir.c \
bitstream.c \
VbrTag.c \
version.c \
takehiro.c \
quantize.c \
vbrquantize.c \
gain_analysis.c \
id3tag.c \
set_get.c \
presets.c \
jni.c \

LOCAL_LDLIBS += -llog

include $(BUILD_SHARED_LIBRARY)
