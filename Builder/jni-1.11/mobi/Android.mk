LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_MODULE  := libmobi

LOCAL_SRC_FILES := buffer.c  common.c  compression.c  debug.c  encryption.c  index.c  memory.c  meta.c  miniz.c mobitool.c  opf.c  parse_rawml.c  read.c  sha1.c  structure.c  util.c  write.c  xmlwriter.c

LOCAL_CFLAGS := -std=c99
LOCAL_CFLAGS += -DUSE_XMLWRITER
LOCAL_CFLAGS += $(MY_O)

LOCAL_LDLIBS  := -lz

include $(BUILD_SHARED_LIBRARY)
