/* libjpeg-turbo's jconfigint.h for Android, written by hand from src/jconfigint.h.in. */

/* libjpeg-turbo build number */
#define BUILD  "librera"

/* How to hide global symbols. */
#define HIDDEN  __attribute__((visibility("hidden")))

/* Compiler's inline keyword */
#undef inline

/* How to obtain function inlining. */
#define INLINE  __inline__ __attribute__((always_inline))

/* How to obtain thread-local storage */
#define THREAD_LOCAL  __thread

/* Define to the full name of this package. */
#define PACKAGE_NAME  "libjpeg-turbo"

/* Version number of package */
#define VERSION  "3.2.1"

/* The size of `size_t', as computed by sizeof. */
#define SIZEOF_SIZE_T  __SIZEOF_SIZE_T__

/* Define if your compiler has __builtin_ctzl() and sizeof(unsigned long) == sizeof(size_t). */
#define HAVE_BUILTIN_CTZL

#if defined(__has_attribute)
#if __has_attribute(fallthrough)
#define FALLTHROUGH  __attribute__((fallthrough));
#else
#define FALLTHROUGH
#endif
#else
#define FALLTHROUGH
#endif

#ifndef BITS_IN_JSAMPLE
#define BITS_IN_JSAMPLE  8      /* use 8 or 12 */
#endif

#undef C_ARITH_CODING_SUPPORTED
#undef D_ARITH_CODING_SUPPORTED
#undef WITH_SIMD

#if BITS_IN_JSAMPLE == 8

/* Support arithmetic encoding */
#define C_ARITH_CODING_SUPPORTED 1

/* Support arithmetic decoding */
#define D_ARITH_CODING_SUPPORTED 1

/* Use accelerated SIMD routines: the Arm Neon ones, see jconfig.h */
#if defined(__aarch64__)
#define WITH_SIMD 1
#define SIMD_ARCHITECTURE  ARM64
#elif defined(__arm__)
#define WITH_SIMD 1
#define SIMD_ARCHITECTURE  ARM
#endif

#endif
