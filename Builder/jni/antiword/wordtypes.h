/*
 * wordtypes.h
 * Copyright (C) 1998-2004 A.J. van Os; Released under GPL
 *
 * Description:
 * Typedefs for the interpretation of MS Word files
 */

#if !defined(__wordtypes_h)
#define __wordtypes_h 1

#include <time.h>
#if defined(__riscos)
#include "DeskLib:Font.h"
#include "DeskLib:Wimp.h"
#endif /* __riscos */

typedef unsigned char	UCHAR;
typedef unsigned short	USHORT;
typedef unsigned int	UINT;
typedef unsigned long	ULONG;

#if defined(__riscos)
typedef struct diagram_tag {
	drawfile_info	tInfo;
	window_handle	tMainWindow;
	window_handle	tScaleWindow;
	menu_ptr	pSaveMenu;
	long		lXleft;			/* In DrawUnits */
	long		lYtop;			/* In DrawUnits */
	size_t		tMemorySize;
	int		iScaleFactorCurr;	/* In percentage */
	int		iScaleFactorTemp;	/* In percentage */
	char		szFilename[19+1];
} diagram_type;
#else
typedef struct diagram_tag {
	FILE		*pOutFile;
	long		lXleft;			/* In DrawUnits */
	long		lYtop;			/* In DrawUnits */
} diagram_type;
typedef UCHAR		drawfile_fontref;
#endif /* __riscos */

typedef struct output_tag {
	char	*szStorage;
	long	lStringWidth;		/* In millipoints */
	size_t	tStorageSize;
	size_t	tNextFree;
	USHORT	usFontStyle;
	USHORT	usFontSize;
	UCHAR	ucFontColor;
	drawfile_fontref	tFontRef;
	struct output_tag	*pPrev;
	struct output_tag	*pNext;
} output_type;

/* Types of conversion */
typedef enum conversion_tag {
	conversion_unknown = 0,
	conversion_text,
	conversion_draw,
	conversion_ps,
	conversion_xml,
	conversion_pdf,
	conversion_fmt_text
} conversion_type;

/* Types of encoding */
typedef enum encoding_tag {
	encoding_neutral = 100,
	encoding_latin_1 = 801,
	encoding_latin_2 = 802,
	encoding_cyrillic = 805,
	encoding_utf_8 = 1601
} encoding_type;

/* Font translation table entry */
typedef struct font_table_tag {
	USHORT	usFontStyle;
	UCHAR	ucWordFontNumber;
	UCHAR	ucFFN;
	UCHAR	ucEmphasis;
	UCHAR	ucInUse;
	char	szWordFontname[65];
	char	szOurFontname[33];
} font_table_type;

/* Options */
typedef enum image_level_tag {
	level_gs_special = 0,
	level_no_images,
	level_ps_2,
	level_ps_3,
	level_default = level_ps_2
} image_level_enum;

typedef struct options_tag {
	int		iParagraphBreak;
	conversion_type	eConversionType;
	BOOL		bHideHiddenText;
	BOOL		bRemoveRemovedText;
	BOOL		bUseLandscape;
	encoding_type	eEncoding;
	int		iPageHeight;		/* In points */
	int		iPageWidth;		/* In points */
	image_level_enum	eImageLevel;
#if defined(__riscos)
	BOOL		bAutofiletypeAllowed;
	int		iScaleFactor;		/* As a percentage */
#endif /* __riscos */
} options_type;

/* Property Set Storage */
typedef struct pps_tag {
	ULONG	ulSB;
	ULONG	ulSize;
} pps_type;
typedef struct pps_info_tag {
	pps_type	tWordDocument;	/* Text stream */
	pps_type	tData;		/* Data stream */
	pps_type	tTable;		/* Table stream */
	pps_type	tSummaryInfo;	/* Summary Information */
	pps_type	tDocSummaryInfo;/* Document Summary Information */
	pps_type	t0Table;	/* Table 0 stream */
	pps_type	t1Table;	/* Table 1 stream */
} pps_info_type;

/* Record of data block information */
typedef struct data_block_tag {
	ULONG	ulFileOffset;
	ULONG	ulDataPos;
	ULONG	ulLength;
} data_block_type;

/* Record of text block information */
typedef struct text_block_tag {
	ULONG	ulFileOffset;
	ULONG	ulCharPos;
	ULONG	ulLength;
	BOOL	bUsesUnicode;	/* This block uses 16 bits per character */
	USHORT	usPropMod;
} text_block_type;

/* Record of the document block information */
typedef struct document_block_tag {
	time_t	tCreateDate;		/* Unix timestamp */
	time_t	tRevisedDate;		/* Unix timestamp */
	USHORT	usDefaultTabWidth;	/* In twips */
	UCHAR	ucHdrFtrSpecification;
} document_block_type;

/* Record of table-row block information */
typedef struct row_block_tag {
	ULONG	ulFileOffsetStart;
	ULONG	ulFileOffsetEnd;
	ULONG	ulCharPosStart;
	ULONG	ulCharPosEnd;
	short	asColumnWidth[TABLE_COLUMN_MAX+1];	/* In twips */
	UCHAR	ucNumberOfColumns;
	UCHAR	ucBorderInfo;
} row_block_type;

/* Various level types */
typedef enum level_type_tag {
	level_type_none = 0,
	level_type_outline,
	level_type_numbering,
	level_type_sequence,
	level_type_pause
} level_type_enum;

typedef enum list_id_tag {
	no_list = 0,
	text_list,
	footnote_list,
	hdrftr_list,
	macro_list,
	annotation_list,
	endnote_list,
	textbox_list,
	hdrtextbox_list,
	end_of_lists
} list_id_enum;

/* Linked list of style description information */
typedef struct style_block_tag {
	ULONG	ulFileOffset;   /* The style start with this character */
	list_id_enum	eListID;/* The fileoffset is in this list */
	BOOL	bNumPause;
	BOOL	bNoRestart;	/* Don't restart by more significant levels */
	USHORT	usIstd;		/* Current style */
	USHORT	usIstdNext;	/* Next style unless overruled */
	USHORT	usStartAt;	/* Number at the start of a list */
	USHORT	usBeforeIndent;	/* Vertical indent before paragraph in twips */
	USHORT	usAfterIndent;	/* Vertical indent after paragraph in twips */
	USHORT	usListIndex;	/* Before Word 8 this field was not filled */
	USHORT	usListChar;	/* Character for an itemized list (Unicode) */
	short	sLeftIndent;	/* Left indentation in twips */
	short	sLeftIndent1;	/* First line left indentation in twips */
	short	sRightIndent;	/* Right indentation in twips */
	UCHAR	ucAlignment;
	UCHAR	ucNFC;		/* Number format code */
	UCHAR	ucNumLevel;
	UCHAR	ucListLevel;	/* Before Word 8 this field was not filled */
	char	szListChar[4];	/* Character for an itemized list */
} style_block_type;

/* Font description information */
typedef struct font_block_tag {
	ULONG	ulFileOffset;
	USHORT	usFontStyle;
	USHORT	usFontSize;
	UCHAR	ucFontNumber;
	UCHAR	ucFontColor;
} font_block_type;

/* Picture description information */
typedef struct picture_block_tag {
	ULONG	ulFileOffset;
	ULONG	ulFileOffsetPicture;
	ULONG	ulPictureOffset;
} picture_block_type;

/* Section description information */
typedef struct section_block_tag {
	BOOL	bNewPage;
	USHORT	usNeedPrevLvl;		/* Print previous level numbers */
	USHORT	usHangingIndent;
	UCHAR	aucNFC[9];		/* Number format code */
	UCHAR	ucHdrFtrSpecification;	/* Which headers/footers Word < 8 */
} section_block_type;

/* Header/footer description information */
typedef struct hdrftr_block_tag {
	output_type	*pText;
	long		lHeight;	/* In DrawUnits */
} hdrftr_block_type;

/* Footnote description information */
typedef struct footnote_block_tag {
	char		*szText;
} footnote_block_type;

/* List description information */
typedef struct list_block_tag {
	ULONG	ulStartAt;	/* Number at the start of a list */
	BOOL	bNoRestart;	/* Don't restart by more significant levels */
	USHORT	usListChar;	/* Character for an itemized list (Unicode) */
	short	sLeftIndent;	/* Left indentation in twips */
	UCHAR	ucNFC;		/* Number format code */
} list_block_type;

/* Types of images */
typedef enum imagetype_tag {
	imagetype_is_unknown = 0,
	imagetype_is_external,
	imagetype_is_emf,
	imagetype_is_wmf,
	imagetype_is_pict,
	imagetype_is_jpeg,
	imagetype_is_png,
	imagetype_is_dib
} imagetype_enum;

/* Types of compression */
typedef enum compression_tag {
	compression_unknown = 0,
	compression_none,
	compression_rle4,
	compression_rle8,
	compression_jpeg,
	compression_zlib
} compression_enum;

/* Image information */
typedef struct imagedata_tag {
	/* The type of the image */
	imagetype_enum	eImageType;
	/* Information from the Word document */
	size_t	tPosition;
	size_t	tLength;
	int	iHorSizeScaled;		/* Size in points */
	int	iVerSizeScaled;		/* Size in points */
	/* Information from the image */
	int	iWidth;			/* Size in pixels */
	int	iHeight;		/* Size in pixels */
	int	iComponents;		/* Number of color components */
	UINT	uiBitsPerComponent;	/* Bits per color component */
	BOOL	bAdobe;	/* Image includes Adobe comment marker */
	compression_enum	eCompression;	/* Type of compression */
	BOOL	bColorImage;	/* Is color image */
	int	iColorsUsed;	/* 0 = uses the maximum number of colors */
	UCHAR 	aucPalette[256][3];	/* RGB palette */
} imagedata_type;

typedef enum row_info_tag {
	found_nothing,
	found_a_cell,
	found_not_a_cell,
	found_end_of_row,
	found_not_end_of_row
} row_info_enum;

typedef enum notetype_tag {
	notetype_is_footnote,
	notetype_is_endnote,
	notetype_is_unknown
} notetype_enum;

typedef enum image_info_tag {
	image_no_information,
	image_minimal_information,
	image_full_information
} image_info_enum;

#endif /* __wordtypes_h */
