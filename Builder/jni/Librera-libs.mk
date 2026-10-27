# Librera's native code that is not MuPDF, each part a library of its own. Every flavour carries
# all of them - the ones built with MuPDF and the JS ones without it alike - and the JS flavours
# leave out libMuPDF.so alone (app/build.gradle), so a DjVu, a .doc or a .mobi is read by the same
# library in both. AppsConfig loads them.
#
#   libdjvu.so      DjVuLibre + DjvuDroidBridge.cpp   org.ebookdroid.droids.djvu.codec.*
#   libmobi.so      libmobi (tools/mobitool.c)        LibMobi.convertToEpub
#   libantiword.so  antiword (main_u.c)               LibMobi.convertDocToHtml
#   libhqx.so       hqx + cbdroidbridge.c             RawBitmap (the bitmap scaler)
#
# liblame.so and libarchive.so have makefiles of their own, in their folders.
#
# Included by Android-<mupdf version>.mk after its sub-makefiles (which build the static
# libraries named here) and after MuPDF-<version>.mk, which reads MuPDF's Makelists: the libjpeg
# DjVu decodes with is MuPDF's thirdparty copy, and LIBJPEG_SRC comes from there.

# --- libjpeg, for DjVu ---
# MuPDF's own build of it, mupdf_thirdparty_libjpeg, goes without libjpeg's memory manager, as
# MuPDF supplies one; jmemnobs.c is libjpeg's own, which needs no backing store.
include $(CLEAR_VARS)
LOCAL_PATH      := $(TOP_LOCAL_PATH)
LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)
LOCAL_MODULE    := djvu_libjpeg
LOCAL_SRC_FILES := $(patsubst %,$(MUPDF_PATH)/%,$(LIBJPEG_SRC)) \
	$(MUPDF_PATH)/thirdparty/libjpeg/jmemnobs.c
LOCAL_C_INCLUDES := $(patsubst -I%,$(MUPDF_PATH)/%,$(filter -I%,$(LIBJPEG_CFLAGS) $(LIBJPEG_BUILD_CFLAGS)))
LOCAL_CFLAGS += $(filter-out -I%,$(LIBJPEG_CFLAGS) $(LIBJPEG_BUILD_CFLAGS))
include $(BUILD_STATIC_LIBRARY)

# --- libdjvu.so ---
include $(CLEAR_VARS)
LOCAL_PATH       := $(TOP_LOCAL_PATH)
LOCAL_ARM_MODE   := $(MY_ARM_MODE)
LOCAL_ARM_NEON   := true
LOCAL_MODULE     := djvu
LOCAL_CFLAGS     := -DHAVE_ANDROID
LOCAL_C_INCLUDES := $(TOP_LOCAL_PATH)/libdjvu-3.5.28/src $(TOP_LOCAL_PATH)
LOCAL_SRC_FILES  := DjvuDroidBridge.cpp
LOCAL_STATIC_LIBRARIES := djvu_static djvu_libjpeg
LOCAL_LDLIBS     := -ljnigraphics -llog
# the static libraries' own symbols stay inside, the JNI entry points are all that is exported
LOCAL_LDFLAGS    := -Wl,--exclude-libs,ALL
include $(BUILD_SHARED_LIBRARY)

# --- libmobi.so ---
include $(CLEAR_VARS)
LOCAL_PATH      := $(TOP_LOCAL_PATH)
LOCAL_MODULE    := mobi
# the JNI entry point is in the static library, which is why the whole of it goes in
LOCAL_WHOLE_STATIC_LIBRARIES := libmobi_012
LOCAL_LDLIBS    := -lz -llog -lm
include $(BUILD_SHARED_LIBRARY)

# --- libantiword.so ---
include $(CLEAR_VARS)
LOCAL_PATH      := $(TOP_LOCAL_PATH)
LOCAL_MODULE    := antiword
# the JNI entry point is in the static library, which is why the whole of it goes in
LOCAL_WHOLE_STATIC_LIBRARIES := antiword_static
LOCAL_LDLIBS    := -llog -lm
include $(BUILD_SHARED_LIBRARY)

# --- libhqx.so ---
include $(CLEAR_VARS)
LOCAL_PATH       := $(TOP_LOCAL_PATH)
LOCAL_ARM_MODE   := $(MY_ARM_MODE)
LOCAL_MODULE     := hqx
LOCAL_C_INCLUDES := $(TOP_LOCAL_PATH)/libhqx-1.2
LOCAL_SRC_FILES  := cbdroidbridge.c
LOCAL_STATIC_LIBRARIES := hqx_static
LOCAL_LDLIBS     := -llog
LOCAL_LDFLAGS    := -Wl,--exclude-libs,ALL
include $(BUILD_SHARED_LIBRARY)
