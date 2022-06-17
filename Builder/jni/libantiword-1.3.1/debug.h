/*
 * debug.h
 * Copyright (C) 1998-2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Macro's for debuging.
 */

#if !defined(__debug_h)
#define __debug_h 1

#include <stdio.h>
#include <ctype.h>

#if defined(DEBUG)

#define DBG_MSG(t)	(void)fprintf(stderr,\
				"%s[%3d]: %.240s\n",\
				__FILE__, __LINE__, (t))

#define DBG_STRN(t,m)	(void)fprintf(stderr,\
				"%s[%3d]: %d '%.*s'\n",\
				__FILE__, __LINE__,\
				(int)(m), (int)(m), (const char *)(t))

#define DBG_CHR(m)    (void)fprintf(stderr,\
				"%s[%3d]: "#m" = %3d 0x%02x '%c'\n",\
				__FILE__, __LINE__,\
				(int)(m), (unsigned int)(unsigned char)(m),\
				isprint((int)(unsigned char)(m))?(char)(m):' ')

#define DBG_DEC(m)	(void)fprintf(stderr,\
				"%s[%3d]: "#m" = %ld\n",\
				__FILE__, __LINE__, (long)(m))

#define DBG_HEX(m)	(void)fprintf(stderr,\
				"%s[%3d]: "#m" = 0x%02lx\n",\
				__FILE__, __LINE__, (unsigned long)(m))

#define DBG_FLT(m)	(void)fprintf(stderr,\
				"%s[%3d]: "#m" = %.3f\n",\
				__FILE__, __LINE__, (double)(m))

#define DBG_FIXME()	(void)fprintf(stderr,\
				"%s[%3d]: FIXME\n",\
				__FILE__, __LINE__)

#define DBG_PRINT_BLOCK(b,m)	vPrintBlock(__FILE__, __LINE__,(b),(m))
#define DBG_UNICODE(t)		vPrintUnicode(__FILE__, __LINE__,\
					(const UCHAR *)(t),unilen(t))
#define DBG_UNICODE_N(t,m)	vPrintUnicode(__FILE__, __LINE__,\
					(const UCHAR *)(t),(m))

#define DBG_MSG_C(c,t)		do { if (c) DBG_MSG(t); } while(0)
#define DBG_STRN_C(c,t,m)	do { if (c) DBG_STRN(t,m); } while(0)
#define DBG_CHR_C(c,m)		do { if (c) DBG_CHR(m); } while(0)
#define DBG_DEC_C(c,m)		do { if (c) DBG_DEC(m); } while(0)
#define DBG_HEX_C(c,m)		do { if (c) DBG_HEX(m); } while(0)
#define DBG_FLT_C(c,m)		do { if (c) DBG_FLT(m); } while(0)

#else

#define DBG_MSG(t)		/* EMPTY */
#define DBG_STRN(t,m)		/* EMPTY */
#define DBG_CHR(m)		/* EMPTY */
#define DBG_DEC(m)		/* EMPTY */
#define DBG_HEX(m)		/* EMPTY */
#define DBG_FLT(m)		/* EMPTY */

#define DBG_FIXME()		/* EMPTY */
#define DBG_PRINT_BLOCK(b,m)	/* EMPTY */
#define DBG_UNICODE(t)		/* EMPTY */
#define DBG_UNICODE_N(t,m)	/* EMPTY */

#define DBG_MSG_C(c,t)		/* EMPTY */
#define DBG_STRN_C(c,t,m)	/* EMPTY */
#define DBG_CHR_C(c,m)		/* EMPTY */
#define DBG_DEC_C(c,m)		/* EMPTY */
#define DBG_HEX_C(c,m)		/* EMPTY */
#define DBG_FLT_C(c,m)		/* EMPTY */

#endif /* DEBUG */

#define NO_DBG_MSG(t)		/* EMPTY */
#define NO_DBG_STRN(t,m)	/* EMPTY */
#define NO_DBG_CHR(m)		/* EMPTY */
#define NO_DBG_DEC(m)		/* EMPTY */
#define NO_DBG_HEX(m)		/* EMPTY */
#define NO_DBG_FLT(m)		/* EMPTY */

#define NO_DBG_PRINT_BLOCK(b,m)	/* EMPTY */
#define NO_DBG_UNICODE(t)	/* EMPTY */
#define NO_DBG_UNICODE_N(t,m)	/* EMPTY */

#define NO_DBG_MSG_C(c,t)	/* EMPTY */
#define NO_DBG_STRN_C(c,t,m)	/* EMPTY */
#define NO_DBG_CHR_C(c,m)	/* EMPTY */
#define NO_DBG_DEC_C(c,m)	/* EMPTY */
#define NO_DBG_HEX_C(c,m)	/* EMPTY */
#define NO_DBG_FLT_C(c,m)	/* EMPTY */

#if defined(TRACE)

#define TRACE_MSG(t)	do {\
			(void)fprintf(stderr,\
				"%s[%3d]: TRACE:%.40s\n",\
				__FILE__, __LINE__, (t));\
			(void)fflush(stderr);\
			} while(0)

#else

#define TRACE_MSG(t)		/* EMPTY */

#endif /* TRACE */

#endif /* !__debug_h */
