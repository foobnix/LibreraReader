/*
 * wordconst.h
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Constants and macros for the interpretation of MS Word files
 */

#if !defined(__wordconst_h)
#define __wordconst_h 1

/*
 * A bit odd definition of the type Boolean, but RISC OS insists
 * on this and Linux/Unix doesn't mind.
 */
#if !defined(BOOL)
#define BOOL int
#define TRUE 1
#define FALSE 0
#endif /* !BOOL */

/* Block sizes */
#define HEADER_SIZE			768
#define BIG_BLOCK_SIZE			512
#define PROPERTY_SET_STORAGE_SIZE	128
#define SMALL_BLOCK_SIZE		 64
/* Switch size of Depot use */
#define MIN_SIZE_FOR_BBD_USE		0x1000
/* Table sizes */
#define TABLE_COLUMN_MAX		 31
/* Maximum number of tabs positions in a paragraph */
#define NUMBER_OF_TABS_MAX		 64
/* Font sizes (in half-points) */
#define MIN_FONT_SIZE			  8
#define DEFAULT_FONT_SIZE		 20
#define MAX_FONT_SIZE			240
#define MIN_TABLEFONT_SIZE		 16
#define MAX_TABLEFONT_SIZE		 20
/* Font styles */
#define FONT_REGULAR			0x0000
#define FONT_BOLD			0x0001
#define FONT_ITALIC			0x0002
#define FONT_UNDERLINE			0x0004
#define FONT_CAPITALS			0x0008
#define FONT_SMALL_CAPITALS		0x0010
#define FONT_STRIKE			0x0020
#define FONT_HIDDEN			0x0040
#define FONT_MARKDEL			0x0080
#define FONT_SUPERSCRIPT		0x0100
#define FONT_SUBSCRIPT			0x0200
/* Font colors */
#define FONT_COLOR_DEFAULT		 0
#define FONT_COLOR_BLACK		 1
#define FONT_COLOR_BLUE			 2
#define FONT_COLOR_CYAN			 3
#define FONT_COLOR_GREEN		 4
#define FONT_COLOR_MAGENTA		 5
#define FONT_COLOR_RED			 6
#define FONT_COLOR_YELLOW		 7
#define FONT_COLOR_WHITE		 8
/* Special block numbers */
#define END_OF_CHAIN			0xfffffffeUL
#define UNUSED_BLOCK			0xffffffffUL
/* Blocksize (512 bytes) and maximum filesize (4 GB) gives 0..7fffff */
#define MAX_BLOCKNUMBER			0x007fffffUL
/* Invalid character position */
#define CP_INVALID			0xffffffffUL
/* Invalid file offset */
#define FC_INVALID			0xffffffffUL
/* Special istd values */
#define ISTD_INVALID			USHRT_MAX
#define ISTD_NORMAL			0
/* Properties modifier without value */
#define IGNORE_PROPMOD			0
/* Types of lists */
#define LIST_ARABIC_NUM			0x00
#define LIST_UPPER_ROMAN		0x01
#define LIST_LOWER_ROMAN		0x02
#define LIST_UPPER_ALPHA		0x03
#define LIST_LOWER_ALPHA		0x04
#define LIST_ORDINAL_NUM		0x05
#define LIST_NUMBER_TXT			0x06
#define LIST_ORDINAL_TXT		0x07
#define LIST_OUTLINE_NUM		0x16
#define LIST_SPECIAL			0x17
#define LIST_SPECIAL2			0x19
#define LIST_BULLETS			0xff
/* Types of paragraph alignment */
#define ALIGNMENT_LEFT			0x00
#define ALIGNMENT_CENTER		0x01
#define ALIGNMENT_RIGHT			0x02
#define ALIGNMENT_JUSTIFY		0x03
/* Minimum vertical space before and after a heading line */
#define HEADING_GAP			120	/* twips */
/* Style identifier */
#define STI_USER			0xffe
#define STI_NIL				0xfff
/* Table border style codes */
#define TABLE_BORDER_TOP		0x01
#define TABLE_BORDER_LEFT		0x02
#define TABLE_BORDER_BOTTOM		0x04
#define TABLE_BORDER_RIGHT		0x08

/* Macros */
	/* Get macros */
#define ucGetByte(i,a)		((unsigned char)(a[i]))
#define usGetWord(i,a)		((unsigned short)\
					((unsigned int)(a[(i)+1])<<8|\
					 (unsigned int)(a[i])))
#define ulGetLong(i,a)		((unsigned long)(a[i])|\
					(unsigned long)(a[(i)+1])<<8|\
					(unsigned long)(a[(i)+2])<<16|\
					(unsigned long)(a[(i)+3])<<24)
#define usGetWordBE(i,a)	((unsigned short)\
					((unsigned int)(a[i])<<8|\
					 (unsigned int)(a[(i)+1])))
#define ulGetLongBE(i,a)	((unsigned long)(a[(i)+3])|\
					(unsigned long)(a[(i)+2])<<8|\
					(unsigned long)(a[(i)+1])<<16|\
					(unsigned long)(a[i])<<24)
	/* Font style macros */
#define bIsBold(x)		(((x) & FONT_BOLD) == FONT_BOLD)
#define bIsItalic(x)		(((x) & FONT_ITALIC) == FONT_ITALIC)
#define bIsUnderline(x)		(((x) & FONT_UNDERLINE) == FONT_UNDERLINE)
#define bIsCapitals(x)		(((x) & FONT_CAPITALS) == FONT_CAPITALS)
#define bIsSmallCapitals(x)	(((x) & FONT_SMALL_CAPITALS) == FONT_SMALL_CAPITALS)
#define bIsStrike(x)		(((x) & FONT_STRIKE) == FONT_STRIKE)
#define bIsHidden(x)		(((x) & FONT_HIDDEN) == FONT_HIDDEN)
#define bIsMarkDel(x)		(((x) & FONT_MARKDEL) == FONT_MARKDEL)
#define bIsSuperscript(x)	(((x) & FONT_SUPERSCRIPT) == FONT_SUPERSCRIPT)
#define bIsSubscript(x)		(((x) & FONT_SUBSCRIPT) == FONT_SUBSCRIPT)
	/* Table border style code macros */
#define bIsTableBorderTop(x)	(((x) & TABLE_BORDER_TOP) == TABLE_BORDER_TOP)
#define bIsTableBorderLeft(x)	(((x) & TABLE_BORDER_LEFT) == TABLE_BORDER_LEFT)
#define bIsTableBorderBottom(x)	(((x) & TABLE_BORDER_BOTTOM) == TABLE_BORDER_BOTTOM)
#define bIsTableBorderRight(x)	(((x) & TABLE_BORDER_RIGHT) == TABLE_BORDER_RIGHT)
	/* Computation macros */
#if defined(__riscos)
/* From Words half-points to draw units (plus a percentage) */
#define lWord2DrawUnits00(x)	((long)(x) * 320)
#define lWord2DrawUnits20(x)	((long)(x) * 384)
#define lToBaseLine(x)		((long)(x) *  45)
#endif /* __riscos */
/* From twips (1/20 of a point) to millipoints */
#define lTwips2MilliPoints(x)	((long)(x) * 50)
/* From twips (1/20 of a point) to points */
#define dTwips2Points(x)	((double)(x) / 20.0)
/* From default characters (16 OS units wide) to millipoints */
#define lChar2MilliPoints(x)	((long)(x) * 6400)
#define iMilliPoints2Char(x)	(int)(((long)(x) + 3200) / 6400)
#define iDrawUnits2Char(x)	(int)(((long)(x) + 2048) / 4096)
/* From draw units (1/180*256 inch) to millipoints (1/72*1000 inch) */
#define lDrawUnits2MilliPoints(x)	(((long)(x) * 25 +  8) / 16)
#define lMilliPoints2DrawUnits(x)	(((long)(x) * 16 + 12) / 25)
#define lPoints2DrawUnits(x)		((long)(x) * 640)
#define dDrawUnits2Points(x)		((double)(x) / 640.0)

/* Special characters */
#define IGNORE_CHARACTER	0x00	/* ^@ */
#define PICTURE			0x01	/* ^A */
#define FOOTNOTE_OR_ENDNOTE	0x02	/* ^B */
#define FOOTNOTE_SEPARATOR	0x03	/* ^C */
#define FOOTNOTE_CONTINUATION	0x04	/* ^D */
#define ANNOTATION		0x05	/* ^E */
#define TABLE_SEPARATOR		0x07	/* ^G */
#define FRAME			0x08	/* ^H */
#define TAB			0x09	/* ^I */
/* End of line characters */
#define LINE_FEED		0x0a	/* ^J */
#define HARD_RETURN		0x0b	/* ^K */
#define PAGE_BREAK		0x0c	/* ^L */
#define PAR_END			0x0d	/* ^M */
#define COLUMN_FEED		0x0e	/* ^N */
/* Embedded stuff */
#define START_EMBEDDED		0x13	/* ^S */
#define END_IGNORE		0x14	/* ^T */
#define END_EMBEDDED		0x15	/* ^U */
/* Special characters */
#if defined(DEBUG)
#define FILLER_CHAR		'~'
#else
#define FILLER_CHAR		' '
#endif /* DEBUG */
#define TABLE_SEPARATOR_CHAR	'|'
/* Pseudo characters. These must be outside the Unicode range */
#define FOOTNOTE_CHAR		((unsigned long)0xffff + 1)
#define ENDNOTE_CHAR		((unsigned long)0xffff + 2)
#define UNKNOWN_NOTE_CHAR	((unsigned long)0xffff + 3)

/* Charactercodes as used by Word */
#define WORD_UNBREAKABLE_JOIN		0x1e
#define WORD_SOFT_HYPHEN		0x1f

/* Unicode characters */
#define UNICODE_DOUBLE_LEFT_ANGLE_QMARK	0x00ab
#define UNICODE_MIDDLE_DOT		0x00b7
#define UNICODE_DOUBLE_RIGHT_ANGLE_QMARK	0x00bb
#define UNICODE_CAPITAL_D_WITH_STROKE	0x0110
#define UNICODE_SMALL_D_WITH_STROKE	0x0111
#define UNICODE_CAPITAL_LIGATURE_OE	0x0152
#define UNICODE_SMALL_LIGATURE_OE	0x0153
#define UNICODE_SMALL_F_HOOK		0x0192
#define UNICODE_GREEK_CAPITAL_CHI	0x03a7
#define UNICODE_GREEK_SMALL_UPSILON	0x03c5
#define UNICODE_MODIFIER_CIRCUMFLEX	0x02c6
#define UNICODE_SMALL_TILDE		0x02dc
#define UNICODE_SMALL_LETTER_OMEGA	0x03c9
#define UNICODE_EN_QUAD			0x2000
#define UNICODE_EM_QUAD			0x2001
#define UNICODE_EN_SPACE		0x2002
#define UNICODE_EM_SPACE		0x2003
#define UNICODE_THREE_PER_EM_SPACE	0x2004
#define UNICODE_FOUR_PER_EM_SPACE	0x2005
#define UNICODE_SIX_PER_EM_SPACE	0x2006
#define UNICODE_FIGURE_SPACE		0x2007
#define UNICODE_PUNCTUATION_SPACE	0x2008
#define UNICODE_THIN_SPACE		0x2009
#define UNICODE_HAIR_SPACE		0x200a
#define UNICODE_ZERO_WIDTH_SPACE	0x200b
#define UNICODE_ZERO_WIDTH_NON_JOINER	0x200c
#define UNICODE_ZERO_WIDTH_JOINER	0x200d
#define UNICODE_LEFT_TO_RIGHT_MARK	0x200e
#define UNICODE_RIGHT_TO_LEFT_MARK	0x200f
#define UNICODE_HYPHEN			0x2010
#define UNICODE_NON_BREAKING_HYPHEN	0x2011
#define UNICODE_FIGURE_DASH		0x2012
#define UNICODE_EN_DASH			0x2013
#define UNICODE_EM_DASH			0x2014
#define UNICODE_HORIZONTAL_BAR		0x2015
#define UNICODE_DOUBLE_VERTICAL_LINE	0x2016
#define UNICODE_DOUBLE_LOW_LINE		0x2017
#define UNICODE_LEFT_SINGLE_QMARK	0x2018
#define UNICODE_RIGHT_SINGLE_QMARK	0x2019
#define UNICODE_SINGLE_LOW_9_QMARK	0x201a
#define UNICODE_SINGLE_HIGH_REV_9_QMARK	0x201b
#define UNICODE_LEFT_DOUBLE_QMARK	0x201c
#define UNICODE_RIGHT_DOUBLE_QMARK	0x201d
#define UNICODE_DOUBLE_LOW_9_QMARK	0x201e
#define UNICODE_DOUBLE_HIGH_REV_9_QMARK	0x201f
#define UNICODE_DAGGER			0x2020
#define UNICODE_DOUBLE_DAGGER		0x2021
#define UNICODE_BULLET			0x2022
#define UNICODE_TRIANGULAR_BULLET	0x2023
#define UNICODE_ONE_DOT_LEADER		0x2024
#define UNICODE_TWO_DOT_LEADER		0x2025
#define UNICODE_ELLIPSIS		0x2026
#define UNICODE_HYPHENATION_POINT	0x2027
#define UNICODE_LEFT_TO_RIGHT_EMBEDDING	0x202a
#define UNICODE_RIGHT_TO_LEFT_EMBEDDING	0x202b
#define UNICODE_POP_DIRECTIONAL_FORMATTING	0x202c
#define UNICODE_LEFT_TO_RIGHT_OVERRIDE	0x202d
#define UNICODE_RIGHT_TO_LEFT_OVERRIDE	0x202e
#define UNICODE_NARROW_NO_BREAK_SPACE	0x202f
#define UNICODE_PER_MILLE_SIGN		0x2030
#define UNICODE_PRIME			0x2032
#define UNICODE_DOUBLE_PRIME		0x2033
#define UNICODE_SINGLE_LEFT_ANGLE_QMARK	0x2039
#define UNICODE_SINGLE_RIGHT_ANGLE_QMARK	0x203a
#define UNICODE_UNDERTIE		0x203f
#define UNICODE_FRACTION_SLASH		0x2044
#define UNICODE_EURO_SIGN		0x20ac
#define UNICODE_CIRCLE			0x20dd
#define UNICODE_SQUARE			0x20de
#define UNICODE_DIAMOND			0x20df
#define UNICODE_NUMERO_SIGN		0x2116
#define UNICODE_TRADEMARK_SIGN		0x2122
#define UNICODE_KELVIN_SIGN		0x212a
#define UNICODE_LEFTWARDS_ARROW		0x2190
#define UNICODE_UPWARDS_ARROW		0x2191
#define UNICODE_RIGHTWARDS_ARROW	0x2192
#define UNICODE_DOWNWARDS_ARROW		0x2193
#define UNICODE_N_ARY_SUMMATION		0x2211
#define UNICODE_MINUS_SIGN		0x2212
#define UNICODE_DIVISION_SLASH		0x2215
#define UNICODE_ASTERISK_OPERATOR	0x2217
#define UNICODE_BULLET_OPERATOR		0x2219
#define UNICODE_RATIO			0x2236
#define UNICODE_TILDE_OPERATOR		0x223c
#define UNICODE_BD_LIGHT_HORIZONTAL	0x2500
#define UNICODE_BD_LIGHT_VERTICAL	0x2502
#define UNICODE_BD_LIGHT_DOWN_RIGHT	0x250c
#define UNICODE_BD_LIGHT_DOWN_AND_LEFT	0x2510
#define UNICODE_BD_LIGHT_UP_AND_RIGHT	0x2514
#define UNICODE_BD_LIGHT_UP_AND_LEFT	0x2518
#define UNICODE_BD_LIGHT_VERTICAL_AND_RIGHT	0x251c
#define UNICODE_BD_LIGHT_VERTICAL_AND_LEFT	0x2524
#define UNICODE_BD_LIGHT_DOWN_AND_HORIZONTAL	0x252c
#define UNICODE_BD_LIGHT_UP_AND_HORIZONTAL	0x2534
#define UNICODE_BD_LIGHT_VERTICAL_AND_HORIZONTAL	0x253c
#define UNICODE_BD_DOUBLE_HORIZONTAL	0x2550
#define UNICODE_BD_DOUBLE_VERTICAL	0x2551
#define UNICODE_BD_DOUBLE_DOWN_AND_RIGHT	0x2554
#define UNICODE_BD_DOUBLE_DOWN_AND_LEFT	0x2557
#define UNICODE_BD_DOUBLE_UP_AND_RIGHT	0x255a
#define UNICODE_BD_DOUBLE_UP_AND_LEFT	0x255d
#define UNICODE_BD_DOUBLE_VERTICAL_AND_RIGHT	0x2560
#define UNICODE_BD_DOUBLE_VERTICAL_AND_LEFT	0x2563
#define UNICODE_BD_DOUBLE_DOWN_AND_HORIZONTAL	0x2566
#define UNICODE_BD_DOUBLE_UP_AND_HORIZONTAL	0x2569
#define UNICODE_BD_DOUBLE_VERTICAL_AND_HORIZONTAL	0x256c
#define UNICODE_LIGHT_SHADE		0x2591
#define UNICODE_MEDIUM_SHADE		0x2592
#define UNICODE_DARK_SHADE		0x2593
#define UNICODE_BLACK_SQUARE		0x25a0
#define UNICODE_BLACK_CLUB_SUIT		0x2663
#define UNICODE_SMALL_LIGATURE_FI	0xfb01
#define UNICODE_SMALL_LIGATURE_FL	0xfb02
#define UNICODE_ZERO_WIDTH_NO_BREAK_SPACE	0xfeff

#if defined(__riscos)
#define OUR_ELLIPSIS			0x8c
#define OUR_EM_DASH			0x98
#define OUR_UNBREAKABLE_JOIN		0x99
#else
#define OUR_ELLIPSIS			'.'
#define OUR_EM_DASH			'-'
#define OUR_UNBREAKABLE_JOIN		'-'
#endif /* __riscos */
#define OUR_DIAMOND			'-'

#endif /* __wordconst_h */
