/* config.h.in.  Generated from configure.in by autoheader.  */

#ifndef LAME_CONFIG_H
#define LAME_CONFIG_H

/* add ieee754_float32_t type */
#undef HAVE_IEEE754_FLOAT32_T
#ifndef HAVE_IEEE754_FLOAT32_T
	typedef float ieee754_float32_t;
#endif

/* add ieee754_float64_t type */
#undef HAVE_IEEE754_FLOAT64_T
#ifndef HAVE_IEEE754_FLOAT64_T
	typedef double ieee754_float64_t;
#endif

/* add ieee854_float80_t type */
#undef HAVE_IEEE854_FLOAT80_T
#ifndef HAVE_IEEE854_FLOAT80_T
	typedef long double ieee854_float80_t;
#endif

/* add int16_t type */
#undef HAVE_INT16_T
#ifndef HAVE_INT16_T
	typedef short int16_t;
#endif

/* add uint16_t type */
#undef HAVE_UINT16_T
#ifndef HAVE_UINT16_T
	typedef unsigned short uint16_t;
#endif

/* Define WORDS_BIGENDIAN to 1 if your processor stores words with the most
   significant byte first (like Motorola and SPARC, unlike Intel). */
#if defined AC_APPLE_UNIVERSAL_BUILD
# if defined __BIG_ENDIAN__
#  define WORDS_BIGENDIAN 1
# endif
#else
# ifndef WORDS_BIGENDIAN
#  undef WORDS_BIGENDIAN
# endif
#endif

/* Define to `__inline__' or `__inline' if that's what the C compiler
   calls it, or to nothing if 'inline' is not supported under any name.  */
#ifndef __cplusplus
#undef inline
#endif

#endif /* LAME_CONFIG_H */
