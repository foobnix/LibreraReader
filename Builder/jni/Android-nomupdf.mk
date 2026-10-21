LOCAL_PATH := $(call my-dir)
TOP_LOCAL_PATH := $(LOCAL_PATH)

# libLibreraDocs.so - everything in Librera's native code that is not MuPDF.
#
# The flavours whose names end in JS render PDFs with PDFium and lay reflowable books out with
# foliate-js, so they ship no MuPDF at all. MuPDF is not all libMuPDF.so holds, though: DjVu, the
# .doc converter (antiword), the .mobi converter (libmobi) and the bitmap scaler (hqx) are linked
# into the same library, and dropping it would drop those four formats with it. They are built
# here instead, into a library of their own, which those flavours load in place of libMuPDF.so -
# see AppsConfig#NATIVE_LIBRARY.
#
# Nothing here includes a MuPDF header, which is what -DLIBRERA_NO_MUPDF tells ebookdroidjni.c:
# its one #include of mupdf/fitz.h is unused, and MuPDF's generated headers are not on the
# include path of this build.
#
# Built by build-nomupdf.sh, which runs ndk-build against this file with its own output
# directories, so the libs/ tree the MuPDF build writes is left exactly as it is.

# DjVu decodes the JPEG-coded chunks a .djvu file may carry with libjpeg, which it takes from
# MuPDF's thirdparty tree: its own makefile reads MUPDF_ROOT for the headers, and in the MuPDF
# build the symbols come from mupdf_thirdparty_libjpeg. There is no MuPDF here to take them from,
# so the same sources are built into a static library of this build's own below. libjpeg carries
# its own permissive licence and is no part of MuPDF itself; nothing of MuPDF's is compiled here.
MUPDF_ROOT := $(realpath $(LOCAL_PATH)/../../..)
MUPDF_PATH := $(MUPDF_ROOT)

include $(MUPDF_PATH)/Makelists

include $(TOP_LOCAL_PATH)/libmobi-0.12/Android.mk
include $(TOP_LOCAL_PATH)/libhqx-1.2/Android.mk
include $(TOP_LOCAL_PATH)/libdjvu-3.5.28/Android.mk
include $(TOP_LOCAL_PATH)/libantiword-1.3.1/Android.mk

# liblame is deliberately left out: it builds a shared library of its own, and the flavours that
# load libLibreraDocs.so take liblame.so from the MuPDF build's output like every other flavour.

# --- libjpeg, for DjVu ---
include $(CLEAR_VARS)
LOCAL_PATH      := $(TOP_LOCAL_PATH)
LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)
LOCAL_MODULE    := nomupdf_libjpeg
# MuPDF leaves libjpeg's memory manager out of LIBJPEG_SRC and supplies its own; jmemnobs.c is
# libjpeg's own, which needs no backing store, and provides jpeg_get_small and the rest.
LOCAL_SRC_FILES := $(patsubst %,$(MUPDF_PATH)/%,$(LIBJPEG_SRC)) \
	$(MUPDF_PATH)/thirdparty/libjpeg/jmemnobs.c
LOCAL_C_INCLUDES := $(patsubst -I%,$(MUPDF_PATH)/%,$(filter -I%,$(LIBJPEG_CFLAGS) $(LIBJPEG_BUILD_CFLAGS)))
LOCAL_CFLAGS += $(filter-out -I%,$(LIBJPEG_CFLAGS) $(LIBJPEG_BUILD_CFLAGS))
include $(BUILD_STATIC_LIBRARY)

# --- the library itself ---
include $(CLEAR_VARS)

# Each sub-makefile above pointed LOCAL_PATH at its own directory, and CLEAR_VARS does not put it
# back; the sources below are in this one.
LOCAL_PATH := $(TOP_LOCAL_PATH)

LOCAL_ARM_MODE := $(MY_ARM_MODE)
LOCAL_ARM_NEON := true

LOCAL_C_INCLUDES := \
	$(TOP_LOCAL_PATH)/libdjvu-3.5.28/src \
	$(TOP_LOCAL_PATH)/libhqx-1.2 \
	$(TOP_LOCAL_PATH)

LOCAL_CFLAGS := -DHAVE_ANDROID -DLIBRERA_NO_MUPDF
LOCAL_MODULE := LibreraDocs

LOCAL_SRC_FILES := \
	ebookdroidjni.c \
	DjvuDroidBridge.cpp \
	cbdroidbridge.c

LOCAL_STATIC_LIBRARIES := djvu hqx nomupdf_libjpeg
LOCAL_WHOLE_STATIC_LIBRARIES:= antiword libmobi_012

LOCAL_LDLIBS = -ljnigraphics
LOCAL_LDLIBS += -llog
LOCAL_LDLIBS += -lz
LOCAL_LDLIBS += -lm

include $(BUILD_SHARED_LIBRARY)
