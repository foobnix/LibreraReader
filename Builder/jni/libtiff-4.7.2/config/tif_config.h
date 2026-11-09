/* libtiff's tif_config.h for Android, written by hand from tif_config.h.cmake.in. */
#include "tiffconf.h"

#define CCITT_SUPPORT 1
#define CHECK_JPEG_YCBCR_SUBSAMPLING 1
#define CHUNKY_STRIP_READ_SUPPORT 1
#define DEFER_STRILE_LOAD 1

#define HAVE_ASSERT_H 1
#define HAVE_DECL_OPTARG 0
#define HAVE_FCNTL_H 1
#define HAVE_FSEEKO 1
#define HAVE_MMAP 1
#define HAVE_STRINGS_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1

#define LIBJPEG_12_PATH ""

#define PACKAGE "LibTIFF Software"
#define PACKAGE_BUGREPORT "tiff@lists.osgeo.org"
#define PACKAGE_NAME "LibTIFF Software"
#define PACKAGE_TARNAME "tiff"
#define PACKAGE_URL ""

#define SIZEOF_SIZE_T __SIZEOF_SIZE_T__
#define STRIP_SIZE_DEFAULT 8192
#define TIFF_MAX_DIR_COUNT 1048576

#define WORDS_BIGENDIAN 0

#define TIFF_SIZE_FORMAT "zu"
#if SIZEOF_SIZE_T == 8
#  define TIFF_SSIZE_FORMAT PRId64
#elif SIZEOF_SIZE_T == 4
#  define TIFF_SSIZE_FORMAT PRId32
#else
#  error "Unsupported size_t size"
#endif
