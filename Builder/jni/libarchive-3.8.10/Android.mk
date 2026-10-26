# libarchive 3.8.10, cut down to the readers of the comic archives: ZIP (.cbz), RAR 1.5-4.x and
# RAR5 (.cbr). Nothing writes archives, and no other format, filter or compression library is
# built in: deflate and CRC-32 come from the system zlib the libraries already link, and with no
# crypto library an encrypted entry is reported as one that cannot be read.
#
# It is built, with ../librera_archive.c which gives Java these archives
# (com.foobnix.ext.NativeArchive), into a library of its own, libarchive.so: the comic engine reads
# its pages through it in every build, and the library its covers and ComicInfo.xml. Like
# liblame.so it comes out of the MuPDF build (Android-<mupdf version>.mk includes this file) and
# goes into every flavour, the ones without MuPDF too; MuPDF itself does not use it.
#
# libarchive/ holds only the sources those readers pull in (each one is needed to link), and
# every header; config/android.h and include/android_lf.h are libarchive's own, from
# contrib/android. Upstream:
# https://github.com/libarchive/libarchive/releases/tag/v3.8.10 (BSD-2-Clause, see COPYING).

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS    := $(APP_CFLAGS)
LOCAL_CPPFLAGS  := $(APP_CPPFLAGS)
LOCAL_ARM_MODE  := $(APP_ARM_MODE)

LOCAL_MODULE := archive

LOCAL_CFLAGS += -DPLATFORM_CONFIG_H=\"$(abspath $(LOCAL_PATH))/config/android.h\"
LOCAL_CFLAGS += -ffunction-sections -fdata-sections
LOCAL_CFLAGS += -Wno-unused-parameter

LOCAL_C_INCLUDES := $(LOCAL_PATH)/libarchive $(LOCAL_PATH)/include

LOCAL_SRC_FILES := \
	libarchive/archive_acl.c \
	libarchive/archive_blake2s_ref.c \
	libarchive/archive_blake2sp_ref.c \
	libarchive/archive_check_magic.c \
	libarchive/archive_crc32.c \
	libarchive/archive_cryptor.c \
	libarchive/archive_entry.c \
	libarchive/archive_entry_sparse.c \
	libarchive/archive_entry_xattr.c \
	libarchive/archive_hmac.c \
	libarchive/archive_options.c \
	libarchive/archive_ppmd7.c \
	libarchive/archive_ppmd8.c \
	libarchive/archive_random.c \
	libarchive/archive_rb.c \
	libarchive/archive_read.c \
	libarchive/archive_read_add_passphrase.c \
	libarchive/archive_read_set_options.c \
	libarchive/archive_read_support_format_rar.c \
	libarchive/archive_read_support_format_rar5.c \
	libarchive/archive_read_support_format_zip.c \
	libarchive/archive_string.c \
	libarchive/archive_string_sprintf.c \
	libarchive/archive_time.c \
	libarchive/archive_util.c \
	libarchive/archive_virtual.c \
	../librera_archive.c

LOCAL_LDLIBS := -lz -llog
# only the JNI entry points are exported (exports.map), so the rest of libarchive that is not
# reached from them is dropped
LOCAL_LDFLAGS := -Wl,--gc-sections -Wl,--version-script=$(LOCAL_PATH)/exports.map

include $(BUILD_SHARED_LIBRARY)
