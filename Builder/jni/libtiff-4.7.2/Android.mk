# libtiff 4.7.2, the decoding side of it: .tif and .tiff books, and the TIFF pages of a comic
# archive (.cbz, .cbr), which Android's own image decoder cannot read. Every codec libtiff has
# that needs no library but zlib and libjpeg is built in: none, CCITT fax (modified Huffman, T.4,
# T.6), LZW, PackBits, Deflate, JPEG and old-style JPEG, NeXT, ThunderScan, PixarLog and LogLuv.
# Nothing writes TIFF; the writing sources are here only because the reading ones reach them,
# and fall away at link time.
#
# It is built, with ../librera_tiff.c which gives Java these images (com.foobnix.ext.NativeTiff),
# into a library of its own, libtiff.so: TIFF books and the comic engine's TIFF pages are decoded
# through it in every build. Like libarchive.so it comes out of the MuPDF build and goes into
# every flavour, the ones without MuPDF too; MuPDF itself does not use it. Deflate comes from the
# system zlib, JPEG from libjpeg-turbo (../libjpeg-turbo-3.2.1). JPEG of 12 bits a sample is left
# out: libtiff's RGBA interface, which these images are drawn through, reads 8 and 16 bits only.
#
# libtiff/ holds libtiff's sources and headers as they are upstream; config/ the headers its
# build would generate, written by hand for Android. Upstream:
# https://github.com/libsdl-org/libtiff/releases/tag/v4.7.2 (libtiff license, see LICENSE.md).

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_MODULE := tiff

LOCAL_CFLAGS += -ffunction-sections -fdata-sections
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-sign-compare

LOCAL_C_INCLUDES := $(LOCAL_PATH)/config $(LOCAL_PATH)/libtiff

LOCAL_SRC_FILES := \
	libtiff/tif_aux.c \
	libtiff/tif_close.c \
	libtiff/tif_codec.c \
	libtiff/tif_color.c \
	libtiff/tif_compress.c \
	libtiff/tif_dir.c \
	libtiff/tif_dirinfo.c \
	libtiff/tif_dirread.c \
	libtiff/tif_dirwrite.c \
	libtiff/tif_dumpmode.c \
	libtiff/tif_error.c \
	libtiff/tif_extension.c \
	libtiff/tif_fax3.c \
	libtiff/tif_fax3sm.c \
	libtiff/tif_flush.c \
	libtiff/tif_getimage.c \
	libtiff/tif_hash_set.c \
	libtiff/tif_jpeg.c \
	libtiff/tif_luv.c \
	libtiff/tif_lzw.c \
	libtiff/tif_next.c \
	libtiff/tif_ojpeg.c \
	libtiff/tif_open.c \
	libtiff/tif_packbits.c \
	libtiff/tif_pixarlog.c \
	libtiff/tif_predict.c \
	libtiff/tif_print.c \
	libtiff/tif_read.c \
	libtiff/tif_strip.c \
	libtiff/tif_swab.c \
	libtiff/tif_thunder.c \
	libtiff/tif_tile.c \
	libtiff/tif_unix.c \
	libtiff/tif_version.c \
	libtiff/tif_warning.c \
	libtiff/tif_write.c \
	libtiff/tif_zip.c \
	../librera_tiff.c

LOCAL_STATIC_LIBRARIES := jpeg_turbo
LOCAL_LDLIBS := -lz -llog -lm -ljnigraphics
# only the JNI entry points are exported (exports.map), so the rest of libtiff and libjpeg-turbo
# that is not reached from them is dropped
LOCAL_LDFLAGS := -Wl,--gc-sections -Wl,--version-script=$(LOCAL_PATH)/exports.map

include $(BUILD_SHARED_LIBRARY)
