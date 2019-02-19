/*
 * draw.h
 * Copyright (C) 2001 A.J. van Os; Released under GPL
 *
 * Description:
 * Constants and macros to deal with the Draw format
 */

#if !defined(__draw_h)
#define __draw_h 1

#include "drawftypes.h"

typedef struct draw_jpegstrhdr_tag {
	draw_tagtyp	tag;	/* 1 word  */
	draw_sizetyp	size;	/* 1 word  */
	draw_bboxtyp	bbox;	/* 4 words */
	int	width;		/* 1 word  */
	int	height;		/* 1 word  */
	int	xdpi;		/* 1 word  */
	int	ydpi;		/* 1 word  */
	int	trfm[6];	/* 6 words */
	int	len;		/* 1 word  */
} draw_jpegstrhdr;

typedef struct draw_jpegstr_tag {
	draw_tagtyp	tag;	/* 1 word  */
	draw_sizetyp	size;	/* 1 word  */
	draw_bboxtyp	bbox;	/* 4 words */
	int	width;		/* 1 word  */
	int	height;		/* 1 word  */
	int	xdpi;		/* 1 word  */
	int	ydpi;		/* 1 word  */
	int	trfm[6];	/* 6 words */
	int	len;		/* 1 word  */
	unsigned char	*jpeg;
} draw_jpegstr;

typedef union draw_imageType_tag {
	draw_spristr	*sprite;
	draw_jpegstr	*jpeg;
	char		*bytep;
	int		*wordp;
} draw_imageType;

#endif /* !__draw_h */
