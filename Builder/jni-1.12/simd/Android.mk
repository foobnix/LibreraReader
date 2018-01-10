# Makefile for libjpeg-turbo

LOCAL_PATH := $(call my-dir)

##################################################
###                simd_arm                    ###
##################################################
 
include $(CLEAR_VARS)

LOCAL_MODULE := libsimd_arm
 
LOCAL_MODULE_TAGS := release

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_SRC_FILES := arm/src/jsimd_arm_neon.S

include $(BUILD_STATIC_LIBRARY)
 
##################################################
###                simd_i386                   ###
##################################################
 
include $(CLEAR_VARS)

LOCAL_MODULE := libsimd_i386
 
LOCAL_MODULE_TAGS := release

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)

LOCAL_SRC_FILES := i386/lib/jsimd_i386.a

include $(PREBUILT_STATIC_LIBRARY)
 
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
 
ifeq ($(TARGET_ARCH_ABI),armeabi)
	LOCAL_ARM_MODE  := $(APP_ARM_MODE)
	LOCAL_SRC_FILES := src/jsimd_arm.c
	LOCAL_STATIC_LIBRARIES := libsimd_arm
endif # TARGET_ARCH_ABI == armeabi

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_ARM_MODE  := $(APP_ARM_MODE)
	LOCAL_SRC_FILES := src/jsimd_arm.c
	LOCAL_STATIC_LIBRARIES := libsimd_arm
endif # TARGET_ARCH_ABI == armeabi-v7a

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a-hard)
	LOCAL_ARM_MODE  := $(APP_ARM_MODE)
	LOCAL_SRC_FILES := src/jsimd_arm.c
	LOCAL_STATIC_LIBRARIES := libsimd_arm
endif # TARGET_ARCH_ABI == armeabi-v7a

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
	LOCAL_SRC_FILES := src/jsimd_none.c
endif # TARGET_ARCH_ABI == arm64-v8a

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_SRC_FILES := src/jsimd_i386.c
	LOCAL_STATIC_LIBRARIES := libsimd_i386
endif # TARGET_ARCH_ABI == x86

ifeq ($(TARGET_ARCH_ABI),x86_64)
	LOCAL_SRC_FILES := src/jsimd_none.c
endif # TARGET_ARCH_ABI == x86

ifeq ($(TARGET_ARCH_ABI),mips)
	LOCAL_SRC_FILES := src/jsimd_none.c
endif # TARGET_ARCH_ABI == mips

include $(BUILD_STATIC_LIBRARY)


