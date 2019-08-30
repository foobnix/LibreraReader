# Makefile for libjpeg-turbo
LOCAL_PATH := $(call my-dir)
##################################################
###                simd                        ###
##################################################

include $(CLEAR_VARS)

LOCAL_MODULE := libsimd
 
LOCAL_MODULE_TAGS := release

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/src \
                    $(LOCAL_PATH)/../jpeg-turbo/android \
                    $(LOCAL_PATH)/../jpeg-turbo/include \
                    $(LOCAL_PATH)/../standalone/include 

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_ARM_MODE  := $(APP_ARM_MODE)
	LOCAL_SRC_FILES := jsimd_arm.c jsimd_arm_neon.S
endif

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_ARM_MODE  := $(APP_ARM_MODE)
	LOCAL_SRC_FILES := jsimd_arm64.c jsimd_arm64_neon.S
endif

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_SRC_FILES := jsimd_i386.c
endif

ifeq ($(TARGET_ARCH_ABI),x86_64)
	LOCAL_SRC_FILES := jsimd_x86_64.c
endif


include $(BUILD_STATIC_LIBRARY)


