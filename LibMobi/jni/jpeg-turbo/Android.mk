# Makefile for libjpeg-turbo

######################################################
###           libjpeg.so                            ##
######################################################

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libjpeg-turbo

LOCAL_MODULE_TAGS := release

# From autoconf-generated Makefile
LOCAL_SRC_FILES := \
	src/jcapimin.c  src/jcapistd.c   src/jccoefct.c  src/jccolor.c \
    src/jcdctmgr.c  src/jchuff.c     src/jcinit.c    src/jcmainct.c \
    src/jcmarker.c  src/jcmaster.c   src/jcomapi.c   src/jcparam.c \
    src/jcphuff.c   src/jcprepct.c   src/jcsample.c  src/jctrans.c \
    src/jdapimin.c  src/jdapistd.c   src/jdatadst.c  src/jdatasrc.c \
    src/jdcoefct.c  src/jdcolor.c    src/jddctmgr.c  src/jdhuff.c \
    src/jdinput.c   src/jdmainct.c   src/jdmarker.c  src/jdmaster.c \
    src/jdmerge.c   src/jdphuff.c    src/jdpostct.c  src/jdsample.c \
    src/jdtrans.c   src/jerror.c     src/jfdctflt.c  src/jfdctfst.c \
    src/jfdctint.c  src/jidctflt.c   src/jidctfst.c  src/jidctint.c \
    src/jidctred.c  src/jquant1.c    src/jquant2.c   src/jutils.c \
    src/jmemmgr.c   src/jmemnobs.c   src/jaricom.c   src/jcarith.c \
    src/jdarith.c   src/turbojpeg.c  src/transupp.c  src/jdatadst-tj.c \
    src/jdatasrc-tj.c

LOCAL_SHARED_LIBRARIES := libcutils
LOCAL_STATIC_LIBRARIES := libsimd
 
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include 
 
LOCAL_CFLAGS := -DAVOID_TABLES -fstrict-aliasing -fprefetch-loop-arrays -DANDROID \
        -DANDROID_TILE_BASED_DECODE -DENABLE_ANDROID_NULL_CONVERT

LOCAL_ARM_MODE := $(APP_ARM_MODE)

include $(BUILD_STATIC_LIBRARY)


