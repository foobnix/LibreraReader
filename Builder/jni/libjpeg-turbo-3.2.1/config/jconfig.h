/* libjpeg-turbo's jconfig.h for Android, written by hand from src/jconfig.h.in with the values
   its CMake build picks by default: the libjpeg v6b API, arithmetic coding on, SIMD where the
   SIMD sources are built in (Android.mk) - the Arm ones. */

/* Version ID for the JPEG library.
 * Might be useful for tests like "#if JPEG_LIB_VERSION >= 60".
 */
#define JPEG_LIB_VERSION  62

/* libjpeg-turbo version */
#define LIBJPEG_TURBO_VERSION  3.2.1

/* libjpeg-turbo version in integer form */
#define LIBJPEG_TURBO_VERSION_NUMBER  3002001

/* Support arithmetic encoding when using 8-bit samples */
#define C_ARITH_CODING_SUPPORTED 1

/* Support arithmetic decoding when using 8-bit samples */
#define D_ARITH_CODING_SUPPORTED 1

/* Support in-memory source/destination managers */
#define MEM_SRCDST_SUPPORTED  1

/* Use accelerated SIMD routines when using 8-bit samples: the Arm Neon ones. The x86 ones are
   NASM assembly, which this build does not assemble, so x86 runs the C code. */
#if defined(__aarch64__) || defined(__arm__)
#define WITH_SIMD 1
#endif

/* See src/jconfig.h.in: the 8-bit API is the one the unmodified libjpeg API speaks */
#ifndef BITS_IN_JSAMPLE
#define BITS_IN_JSAMPLE  8
#endif
