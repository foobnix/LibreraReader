# Librera's native code that is not MuPDF, each part a library of its own. Every flavour carries
# all of them - the ones built with MuPDF and the JS ones without it alike - and the JS flavours
# leave out libMuPDF.so alone (app/build.gradle), so a DjVu, a .doc or a .mobi is read by the same
# library in both. AppsConfig loads them.
#
#   libdjvu.so      DjVuLibre + DjvuDroidBridge.cpp   org.ebookdroid.droids.djvu.codec.*
#   libmobi.so      libmobi (tools/mobitool.c)        LibMobi.convertToEpub
#   libantiword.so  antiword (main_u.c)               LibMobi.convertDocToHtml
#   libhqx.so       hqx + cbdroidbridge.c             RawBitmap (the bitmap scaler)
#   libpdfannot.so  librera_pdfannot.c                PdfiumAnnots (annotations through PDFium)
#
# liblame.so, libarchive.so and libtiff.so have makefiles of their own, in their folders. The
# libjpeg DjVu and libtiff.so decode with is libjpeg-turbo (libjpeg-turbo-3.2.1/Android.mk).
#
# Included by Android-<mupdf version>.mk after its sub-makefiles, which build the static
# libraries named here.

# --- libdjvu.so ---
include $(CLEAR_VARS)
LOCAL_PATH       := $(TOP_LOCAL_PATH)
LOCAL_ARM_MODE   := $(MY_ARM_MODE)
LOCAL_ARM_NEON   := true
LOCAL_MODULE     := djvu
LOCAL_CFLAGS     := -DHAVE_ANDROID
LOCAL_C_INCLUDES := $(TOP_LOCAL_PATH)/libdjvu-3.5.28/src $(TOP_LOCAL_PATH)
LOCAL_SRC_FILES  := DjvuDroidBridge.cpp
LOCAL_STATIC_LIBRARIES := djvu_static jpeg_turbo
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

# --- libpdfannot.so ---
# PDFium is looked up at run time in the libpdfium.so pdfiumandroid loads, so nothing is linked
include $(CLEAR_VARS)
LOCAL_PATH      := $(TOP_LOCAL_PATH)
LOCAL_MODULE    := pdfannot
LOCAL_SRC_FILES := librera_pdfannot.c
LOCAL_LDLIBS    := -llog -ldl
include $(BUILD_SHARED_LIBRARY)
