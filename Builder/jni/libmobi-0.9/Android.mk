LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_MODULE  := libmobi

LOCAL_SRC_FILES :=  \
src/buffer.c \
src/compression.c \
src/debug.c \
src/encryption.c \
src/index.c \
src/memory.c \
src/meta.c \
src/miniz.c \
src/opf.c \
src/parse_rawml.c \
src/read.c \
src/sha1.c \
src/structure.c \
src/util.c \
src/write.c \
src/xmlwriter.c \
tools/common.c \
tools/mobitool.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/src $(LOCAL_PATH)/tools

LOCAL_CFLAGS += -std=c99
LOCAL_CFLAGS += -DUSE_XMLWRITER

LOCAL_LDLIBS  := -lz

include $(BUILD_SHARED_LIBRARY)
