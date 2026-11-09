/* libtiff's tiffconf.h for Android, written by hand from tiffconf.h.cmake.in: every ABI Librera
   builds for is little-endian, with IEEE floating point. */
#ifndef _TIFFCONF_
#define _TIFFCONF_

#include <stddef.h>
#include <stdint.h>
#include <inttypes.h>

#define TIFF_INT16_T int16_t
#define TIFF_INT32_T int32_t
#define TIFF_INT64_T int64_t
#define TIFF_INT8_T int8_t
#define TIFF_UINT16_T uint16_t
#define TIFF_UINT32_T uint32_t
#define TIFF_UINT64_T uint64_t
#define TIFF_UINT8_T uint8_t
/* signed, the width of a pointer: what libtiff's CMake picks */
#if __SIZEOF_POINTER__ == 8
#define TIFF_SSIZE_T int64_t
#else
#define TIFF_SSIZE_T int32_t
#endif

#define HAVE_IEEEFP 1
#define HOST_FILLORDER FILLORDER_LSB2MSB
#define HOST_BIGENDIAN 0

/* The codecs: all of libtiff's that need no library but zlib, which Android has, and libjpeg -
   libjpeg-turbo, which is built in */
#define CCITT_SUPPORT 1
#define JPEG_SUPPORT 1
#define OJPEG_SUPPORT 1
#define LOGLUV_SUPPORT 1
#define LZW_SUPPORT 1
#define NEXT_SUPPORT 1
#define PACKBITS_SUPPORT 1
#define PIXARLOG_SUPPORT 1
#define THUNDER_SUPPORT 1
#define ZIP_SUPPORT 1

#define STRIPCHOP_DEFAULT TIFF_STRIPCHOP
#define SUBIFD_SUPPORT 1
#define DEFAULT_EXTRASAMPLE_AS_ALPHA 1
#define CHECK_JPEG_YCBCR_SUBSAMPLING 1
#define MDI_SUPPORT 1

#define COLORIMETRY_SUPPORT
#define YCBCR_SUPPORT
#define CMYK_SUPPORT
#define ICC_SUPPORT
#define PHOTOSHOP_SUPPORT
#define IPTC_SUPPORT

#endif /* _TIFFCONF_ */
