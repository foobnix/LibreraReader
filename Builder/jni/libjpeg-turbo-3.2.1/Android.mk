# libjpeg-turbo 3.2.1: the one libjpeg of Librera's native code, which libtiff.so (JPEG in TIFF)
# and libdjvu.so (JPEG in DjVu) both link, in every flavour. MuPDF keeps its own.
#
# The library alone, as its CMake build makes it by default - the libjpeg v6b API, arithmetic
# coding, the 8-, 12- and 16-bit wrappers - with the Arm Neon SIMD on arm64-v8a and armeabi-v7a,
# which is C (intrinsics) and needs no assembler. x86 and x86_64 run the C code: their SIMD is
# NASM assembly, and the NDK's yasm, which ndk-build would assemble it with, does not run on an
# Apple silicon Mac without Rosetta.
#
# src/ and simd/ hold the sources the library is built from, as they are upstream; config/ and
# simd/arm/neon-compat.h the headers its CMake would generate, written by hand for Android.
# Upstream: https://github.com/libjpeg-turbo/libjpeg-turbo/releases/tag/3.2.1 (IJG and BSD-style
# licenses, see LICENSE.md and README.ijg).

LOCAL_PATH := $(call my-dir)

# The Neon sources both Arm ABIs build; each adds the CPU check and the Huffman encoder of its own
JPEG_TURBO_NEON := \
	simd/arm/jccolor-neon.c \
	simd/arm/jcgray-neon.c \
	simd/arm/jcphuff-neon.c \
	simd/arm/jcsample-neon.c \
	simd/arm/jdcolor-neon.c \
	simd/arm/jdmerge-neon.c \
	simd/arm/jdsample-neon.c \
	simd/arm/jfdctfst-neon.c \
	simd/arm/jfdctint-neon.c \
	simd/arm/jidctfst-neon.c \
	simd/arm/jidctint-neon.c \
	simd/arm/jidctred-neon.c \
	simd/arm/jquanti-neon.c

include $(CLEAR_VARS)

LOCAL_MODULE := jpeg_turbo

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)
LOCAL_CFLAGS += -ffunction-sections -fdata-sections -fvisibility=hidden
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-sign-compare

LOCAL_C_INCLUDES := $(LOCAL_PATH)/config $(LOCAL_PATH)/src $(LOCAL_PATH)/simd $(LOCAL_PATH)/simd/arm
# what links this finds jpeglib.h and its jconfig.h here
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/config $(LOCAL_PATH)/src

LOCAL_SRC_FILES := \
	src/jcapimin.c \
	src/wrapper/jcapistd-8.c \
	src/wrapper/jcapistd-12.c \
	src/wrapper/jcapistd-16.c \
	src/wrapper/jccoefct-8.c \
	src/wrapper/jccoefct-12.c \
	src/wrapper/jccolor-8.c \
	src/wrapper/jccolor-12.c \
	src/wrapper/jccolor-16.c \
	src/wrapper/jcdctmgr-8.c \
	src/wrapper/jcdctmgr-12.c \
	src/wrapper/jcdiffct-8.c \
	src/wrapper/jcdiffct-12.c \
	src/wrapper/jcdiffct-16.c \
	src/jchuff.c \
	src/jcicc.c \
	src/jcinit.c \
	src/jclhuff.c \
	src/wrapper/jclossls-8.c \
	src/wrapper/jclossls-12.c \
	src/wrapper/jclossls-16.c \
	src/wrapper/jcmainct-8.c \
	src/wrapper/jcmainct-12.c \
	src/wrapper/jcmainct-16.c \
	src/jcmarker.c \
	src/jcmaster.c \
	src/jcomapi.c \
	src/jcparam.c \
	src/jcphuff.c \
	src/wrapper/jcprepct-8.c \
	src/wrapper/jcprepct-12.c \
	src/wrapper/jcprepct-16.c \
	src/wrapper/jcsample-8.c \
	src/wrapper/jcsample-12.c \
	src/wrapper/jcsample-16.c \
	src/jctrans.c \
	src/jdapimin.c \
	src/wrapper/jdapistd-8.c \
	src/wrapper/jdapistd-12.c \
	src/wrapper/jdapistd-16.c \
	src/jdatadst.c \
	src/jdatasrc.c \
	src/wrapper/jdcoefct-8.c \
	src/wrapper/jdcoefct-12.c \
	src/wrapper/jdcolor-8.c \
	src/wrapper/jdcolor-12.c \
	src/wrapper/jdcolor-16.c \
	src/wrapper/jddctmgr-8.c \
	src/wrapper/jddctmgr-12.c \
	src/wrapper/jddiffct-8.c \
	src/wrapper/jddiffct-12.c \
	src/wrapper/jddiffct-16.c \
	src/jdhuff.c \
	src/jdicc.c \
	src/jdinput.c \
	src/jdlhuff.c \
	src/wrapper/jdlossls-8.c \
	src/wrapper/jdlossls-12.c \
	src/wrapper/jdlossls-16.c \
	src/wrapper/jdmainct-8.c \
	src/wrapper/jdmainct-12.c \
	src/wrapper/jdmainct-16.c \
	src/jdmarker.c \
	src/jdmaster.c \
	src/wrapper/jdmerge-8.c \
	src/wrapper/jdmerge-12.c \
	src/jdphuff.c \
	src/wrapper/jdpostct-8.c \
	src/wrapper/jdpostct-12.c \
	src/wrapper/jdpostct-16.c \
	src/wrapper/jdsample-8.c \
	src/wrapper/jdsample-12.c \
	src/wrapper/jdsample-16.c \
	src/jdtrans.c \
	src/jerror.c \
	src/jfdctflt.c \
	src/wrapper/jfdctfst-8.c \
	src/wrapper/jfdctfst-12.c \
	src/wrapper/jfdctint-8.c \
	src/wrapper/jfdctint-12.c \
	src/wrapper/jidctflt-8.c \
	src/wrapper/jidctflt-12.c \
	src/wrapper/jidctfst-8.c \
	src/wrapper/jidctfst-12.c \
	src/wrapper/jidctint-8.c \
	src/wrapper/jidctint-12.c \
	src/wrapper/jidctred-8.c \
	src/wrapper/jidctred-12.c \
	src/jmemmgr.c \
	src/jmemnobs.c \
	src/jpeg_nbits.c \
	src/wrapper/jquant1-8.c \
	src/wrapper/jquant1-12.c \
	src/wrapper/jquant2-8.c \
	src/wrapper/jquant2-12.c \
	src/wrapper/jutils-8.c \
	src/wrapper/jutils-12.c \
	src/wrapper/jutils-16.c \
	src/jaricom.c \
	src/jcarith.c \
	src/jdarith.c

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES += \
	simd/jsimd.c \
	simd/arm/aarch64/jsimdcpu.c \
	simd/arm/aarch64/jchuff-neon.c \
	$(JPEG_TURBO_NEON)
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_ARM_NEON := true
LOCAL_CFLAGS += -DHAVE_GETAUXVAL
LOCAL_SRC_FILES += \
	simd/jsimd.c \
	simd/arm/aarch32/jsimdcpu.c \
	simd/arm/aarch32/jchuff-neon.c \
	$(JPEG_TURBO_NEON)
endif

include $(BUILD_STATIC_LIBRARY)
