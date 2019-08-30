/*
 * antiword.h
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Generic include file for project 'Antiword'
 */

#if !defined(__antiword_h)
#define __antiword_h 1

#if !defined(DEBUG)
#ifndef NDEBUG
#define NDEBUG
#endif
#endif

#if defined(DEBUG) == defined(NDEBUG)
#error Exactly one of the DEBUG and NDEBUG flags MUST be set
#endif /* DEBUG == NDEBUG */

#include <stdio.h>
#include <limits.h>
#if defined(__riscos)
#include "DeskLib:Font.h"
#include "DeskLib:Wimp.h"
#include "Desklib:Window.h"
#include "drawfile.h"
#define window_ANY	event_ANY
#define icon_ANY	event_ANY
#else
#include <sys/types.h>
#endif /* __riscos */
#include "wordconst.h"
#include "wordtypes.h"
#include "fail.h"
#include "debug.h"

/* Constants */
#if !defined(PATH_MAX)
 #if defined(__riscos)
 #define PATH_MAX		 255
 #else
  #if defined(MAXPATHLEN)
  #define PATH_MAX		MAXPATHLEN
  #else
  #define PATH_MAX		1024
  #endif /* MAXPATHLEN */
 #endif /* __riscos */
#endif /* !PATH_MAX */

#if !defined(CHAR_BIT)
#define CHAR_BIT		8
#endif /* CHAR_BIT */

#if !defined(TIME_T_MIN)
#define TIME_T_MIN		((time_t)0 < (time_t)-1 ?\
				(time_t)0 :\
				(time_t)1 << (sizeof(time_t) * CHAR_BIT - 1))
#endif /* TIMER_T_MIN */

#if !defined(TIME_T_MAX)
#if defined(__TURBOC__)	/* Turbo C chokes on the subtraction below */
#define TIME_T_MAX		(LONG_MAX)
#else	/* All others */
#define TIME_T_MAX		(~(time_t)0 - TIME_T_MIN)
#endif /* __TURBOC__ */
#endif /* TIME_T_MAX */

#if !defined(SIZE_T_MAX)
#define SIZE_T_MAX		(~(size_t)0)
#endif /* SIZE_T_MAX */

#if defined(__riscos)
#define FILE_SEPARATOR		"."
#elif defined(__dos) || defined(__CYGMING__)
#define FILE_SEPARATOR		"\\"
#else	/* All others */
#define FILE_SEPARATOR		"/"
#endif /* __riscos */

/* PNG chunk names */
#define PNG_CN_IDAT		0x49444154
#define PNG_CN_IEND		0x49454e44
#define PNG_CN_IHDR		0x49484452
#define PNG_CN_PLTE		0x504c5445

/* The screen width */
#define MIN_SCREEN_WIDTH	 45
#define DEFAULT_SCREEN_WIDTH	 76
#define MAX_SCREEN_WIDTH	145

#if defined(__riscos)
/* The scale factors as percentages */
#define MIN_SCALE_FACTOR	 25
#define DEFAULT_SCALE_FACTOR	100
#define MAX_SCALE_FACTOR	400

/* Filetypes */
#define FILETYPE_MSWORD		0xae6
#define FILETYPE_DRAW		0xaff
#define FILETYPE_JPEG		0xc85
#define FILETYPE_POSCRIPT	0xff5
#define FILETYPE_SPRITE		0xff9
#define FILETYPE_TEXT		0xfff

/* The button numbers in the choices window */
#define CHOICES_DEFAULT_BUTTON		 3
#define CHOICES_SAVE_BUTTON		 2
#define CHOICES_CANCEL_BUTTON		 1
#define CHOICES_APPLY_BUTTON		 0
#define CHOICES_BREAK_BUTTON		 6
#define CHOICES_BREAK_WRITEABLE		 7
#define CHOICES_BREAK_UP_BUTTON		 8
#define CHOICES_BREAK_DOWN_BUTTON	 9
#define CHOICES_NO_BREAK_BUTTON		11
#define CHOICES_AUTOFILETYPE_BUTTON	14
#define CHOICES_HIDDEN_TEXT_BUTTON	22
#define CHOICES_WITH_IMAGES_BUTTON	17
#define CHOICES_NO_IMAGES_BUTTON	18
#define CHOICES_TEXTONLY_BUTTON		19
#define CHOICES_SCALE_WRITEABLE		25
#define CHOICES_SCALE_UP_BUTTON		26
#define CHOICES_SCALE_DOWN_BUTTON	27

/* The button numbers in the scale view window */
#define SCALE_CANCEL_BUTTON		 1
#define SCALE_SCALE_BUTTON		 0
#define SCALE_SCALE_WRITEABLE		 3
#define SCALE_50_PCT			 5
#define SCALE_75_PCT			 6
#define SCALE_100_PCT			 7
#define SCALE_150_PCT			 8

/* Save menu fields */
#define SAVEMENU_SCALEVIEW		0
#define SAVEMENU_SAVEDRAW		1
#define SAVEMENU_SAVETEXT		2
#else
/* Margins for the PostScript version */
#define PS_LEFT_MARGIN			(72 * 640L)
#define PS_RIGHT_MARGIN			(48 * 640L)
#define PS_TOP_MARGIN			(72 * 640L)
#define PS_BOTTOM_MARGIN		(72 * 640L)
#endif /* __riscos */

/* Macros */
#define STREQ(x,y)	(*(x) == *(y) && strcmp(x,y) == 0)
#define STRNEQ(x,y,n)	(*(x) == *(y) && strncmp(x,y,n) == 0)
#if defined(__dos) || defined(__EMX__) || defined(_WIN32)
#define STRCEQ(x,y)	(_stricmp(x,y) == 0)
#else
#define STRCEQ(x,y)	(strcasecmp(x,y) == 0)
#endif /* __dos or __EMX__ */
#define elementsof(a)	(sizeof(a) / sizeof(a[0]))
#define odd(x)		(((x)&0x01)!=0)
#define ROUND4(x)	(((x)+3)&~0x03)
#define ROUND128(x)	(((x)+127)&~0x7f)
#define BIT(x)		(1UL << (x))
#if !defined(max)
#define max(x,y)	((x)>(y)?(x):(y))
#endif /* !max */
#if !defined(min)
#define min(x,y)	((x)<(y)?(x):(y))
#endif /* !min */

#if defined(__riscos)
/* The name of the table font */
#define TABLE_FONT			"Corpus.Medium"
/* Names of the default fonts */
#define FONT_MONOSPACED_PLAIN		"Corpus.Medium"
#define FONT_MONOSPACED_BOLD		"Corpus.Bold"
#define FONT_MONOSPACED_ITALIC		"Corpus.Medium.Oblique"
#define FONT_MONOSPACED_BOLDITALIC	"Corpus.Bold.Oblique"
#define FONT_SERIF_PLAIN		"Trinity.Medium"
#define FONT_SERIF_BOLD			"Trinity.Bold"
#define FONT_SERIF_ITALIC		"Trinity.Medium.Italic"
#define FONT_SERIF_BOLDITALIC		"Trinity.Bold.Italic"
#define FONT_SANS_SERIF_PLAIN		"Homerton.Medium"
#define FONT_SANS_SERIF_BOLD		"Homerton.Bold"
#define FONT_SANS_SERIF_ITALIC		"Homerton.Medium.Oblique"
#define FONT_SANS_SERIF_BOLDITALIC	"Homerton.Bold.Oblique"
#else
/* The name of the table font */
#define TABLE_FONT			"Courier"
/* Names of the default fonts */
#define FONT_MONOSPACED_PLAIN		"Courier"
#define FONT_MONOSPACED_BOLD		"Courier-Bold"
#define FONT_MONOSPACED_ITALIC		"Courier-Oblique"
#define FONT_MONOSPACED_BOLDITALIC	"Courier-BoldOblique"
#define FONT_SERIF_PLAIN		"Times-Roman"
#define FONT_SERIF_BOLD			"Times-Bold"
#define FONT_SERIF_ITALIC		"Times-Italic"
#define FONT_SERIF_BOLDITALIC		"Times-BoldItalic"
#define FONT_SANS_SERIF_PLAIN		"Helvetica"
#define FONT_SANS_SERIF_BOLD		"Helvetica-Bold"
#define FONT_SANS_SERIF_ITALIC		"Helvetica-Oblique"
#define FONT_SANS_SERIF_BOLDITALIC	"Helvetica-BoldOblique"
/* The name of the antiword directories and the font information file */
#if defined(__dos)
#define GLOBAL_ANTIWORD_DIR	"C:\\antiword"
#define ANTIWORD_DIR		"antiword"
#define FONTNAMES_FILE		"fontname.txt"
#elif defined(__amigaos)
#define GLOBAL_ANTIWORD_DIR	"SYS:.antiword"
#define ANTIWORD_DIR		".antiword"
#define FONTNAMES_FILE		"fontnames"
#elif defined(N_PLAT_NLM)
#define GLOBAL_ANTIWORD_DIR	"SYS:/antiword"
#define ANTIWORD_DIR		"antiword"
#define FONTNAMES_FILE		"fontname.txt"
#elif defined(__vms)
#define GLOBAL_ANTIWORD_DIR	"/usr/share/antiword"
#define ANTIWORD_DIR		"antiword"
#define FONTNAMES_FILE		"fontnames"
#elif defined(__BEOS__)
#define GLOBAL_ANTIWORD_DIR	"/boot/home/config/apps/antiword"
#define ANTIWORD_DIR		"antiword"
#define FONTNAMES_FILE		"fontnames"
#elif defined(__CYGMING__)
#define GLOBAL_ANTIWORD_DIR	"C:\\antiword"
#define ANTIWORD_DIR		"antiword"
#define FONTNAMES_FILE		"fontnames"
#elif defined(__Plan9__)
#define GLOBAL_ANTIWORD_DIR	"/sys/lib/antiword"
#define ANTIWORD_DIR		"lib/antiword"
#define FONTNAMES_FILE		"fontnames"
#elif defined(__sun__)
#define GLOBAL_ANTIWORD_DIR	"/usr/local/share/antiword"
#define ANTIWORD_DIR		".antiword"
#define FONTNAMES_FILE		"fontnames"
#else	/* All others */
#define GLOBAL_ANTIWORD_DIR	"/usr/share/antiword"
#define ANTIWORD_DIR		".antiword"
#define FONTNAMES_FILE		"fontnames"
#endif /* __dos */
/* The names of grouped mapping files */
	/* ASCII */
#define MAPPING_FILE_CP437	"cp437.txt"
	/* Latin1 */
#define MAPPING_FILE_8859_1	"8859-1.txt"
	/* Latin2 */
#define MAPPING_FILE_8859_2	"8859-2.txt"
#define MAPPING_FILE_CP852	"cp852.txt"
#define MAPPING_FILE_CP1250	"cp1250.txt"
	/* Cyrillic */
#define MAPPING_FILE_8859_5	"8859-5.txt"
#define MAPPING_FILE_KOI8_R	"koi8-r.txt"
#define MAPPING_FILE_KOI8_U	"koi8-u.txt"
#define MAPPING_FILE_CP866	"cp866.txt"
#define MAPPING_FILE_CP1251	"cp1251.txt"
	/* Latin9 */
#define MAPPING_FILE_8859_15	"8859-15.txt"
	/* UTF-8 */
#define MAPPING_FILE_UTF_8	"UTF-8.txt"
#endif /* __riscos */

/* Prototypes */

/* asc85enc.c */
extern void	vASCII85EncodeByte(FILE *, int);
extern void	vASCII85EncodeArray(FILE *, FILE *, size_t);
extern void	vASCII85EncodeFile(FILE *, FILE *, size_t);
/* blocklist.c */
extern void	vDestroyTextBlockList(void);
extern BOOL	bAdd2TextBlockList(const text_block_type *);
extern void	vSplitBlockList(FILE *, ULONG, ULONG, ULONG, ULONG, ULONG,
			ULONG, ULONG, ULONG, BOOL);
extern BOOL	bExistsHdrFtr(void);
extern BOOL	bExistsTextBox(void);
extern BOOL	bExistsHdrTextBox(void);
extern USHORT	usNextChar(FILE *, list_id_enum, ULONG *, ULONG *, USHORT *);
extern USHORT	usToHdrFtrPosition(FILE *, ULONG);
extern USHORT	usToFootnotePosition(FILE *, ULONG);
extern ULONG	ulCharPos2FileOffsetX(ULONG, list_id_enum *);
extern ULONG	ulCharPos2FileOffset(ULONG);
extern ULONG	ulHdrFtrOffset2CharPos(ULONG);
extern ULONG	ulGetSeqNumber(ULONG);
#if defined(__riscos)
extern ULONG	ulGetDocumentLength(void);
#endif /* __riscos */
/* chartrans.c */
extern UCHAR	ucGetBulletCharacter(conversion_type, encoding_type);
extern UCHAR	ucGetNbspCharacter(void);
extern BOOL	bReadCharacterMappingTable(FILE *);
extern ULONG	ulTranslateCharacters(USHORT, ULONG, int, conversion_type,
			encoding_type, BOOL);
extern ULONG	ulToUpper(ULONG);
/* datalist.c */
extern void	vDestroyDataBlockList(void);
extern BOOL	bAdd2DataBlockList(const data_block_type *);
extern ULONG	ulGetDataOffset(FILE *);
extern BOOL	bSetDataOffset(FILE *, ULONG);
extern int	iNextByte(FILE *);
extern USHORT	usNextWord(FILE *);
extern ULONG	ulNextLong(FILE *);
extern USHORT	usNextWordBE(FILE *);
extern ULONG	ulNextLongBE(FILE *);
extern size_t	tSkipBytes(FILE *, size_t);
extern ULONG	ulDataPos2FileOffset(ULONG);
/* depot.c */
extern void	vDestroySmallBlockList(void);
extern BOOL	bCreateSmallBlockList(ULONG, const ULONG *, size_t);
extern ULONG	ulDepotOffset(ULONG, size_t);
/* dib2eps & dib2sprt.c */
extern BOOL	bTranslateDIB(diagram_type *,
			FILE *, ULONG, const imagedata_type *);
#if defined(__dos)
/* dos.c */
extern int	iGetCodepage(void);
#endif /* __dos */
/* doclist.c */
extern void	vDestroyDocumentInfoList(void);
extern void	vCreateDocumentInfoList(const document_block_type *);
extern UCHAR	ucGetDopHdrFtrSpecification(void);
/* draw.c & output.c */
extern BOOL	bAddDummyImage(diagram_type *, const imagedata_type *);
extern diagram_type *pCreateDiagram(const char *, const char *);
extern void	vPrologue2(diagram_type *, int);
extern void	vMove2NextLine(diagram_type *, drawfile_fontref, USHORT);
extern void	vSubstring2Diagram(diagram_type *,
			char *, size_t, long, UCHAR, USHORT,
			drawfile_fontref, USHORT, USHORT);
extern void	vStartOfParagraph1(diagram_type *, long);
extern void	vStartOfParagraph2(diagram_type *);
extern void	vEndOfParagraph(diagram_type *, drawfile_fontref, USHORT, long);
extern void	vEndOfPage(diagram_type *, long, BOOL);
extern void	vSetHeaders(diagram_type *, USHORT);
extern void	vStartOfList(diagram_type *, UCHAR, BOOL);
extern void	vEndOfList(diagram_type *);
extern void	vStartOfListItem(diagram_type *, BOOL);
extern void	vEndOfTable(diagram_type *);
extern BOOL	bAddTableRow(diagram_type *, char **, int,
			const short *, UCHAR);
#if defined(__riscos)
extern BOOL	bDestroyDiagram(event_pollblock *, void *);
extern void	vImage2Diagram(diagram_type *, const imagedata_type *,
			UCHAR *, size_t);
extern BOOL	bVerifyDiagram(diagram_type *);
extern void	vShowDiagram(diagram_type *);
extern void	vMainButtonClick(mouse_block *);
extern BOOL	bMainKeyPressed(event_pollblock *, void *);
extern BOOL	bMainEventHandler(event_pollblock *, void *);
extern BOOL	bRedrawMainWindow(event_pollblock *, void *);
extern BOOL	bScaleOpenAction(event_pollblock *, void *);
extern void	vSetTitle(diagram_type *);
extern void	vScaleButtonClick(mouse_block *, diagram_type *);
extern BOOL	bScaleKeyPressed(event_pollblock *, void *);
extern BOOL	bScaleEventHandler(event_pollblock *, void *);
#else
extern void	vImagePrologue(diagram_type *, const imagedata_type *);
extern void	vImageEpilogue(diagram_type *);
extern void	vDestroyDiagram(diagram_type *);
#endif /* __riscos */
/* finddata.c */
extern BOOL	bAddDataBlocks(ULONG , ULONG, ULONG, const ULONG *, size_t);
extern BOOL	bGet6DocumentData(FILE *, ULONG,
				const ULONG *, size_t, const UCHAR *);
/* findtext.c */
extern BOOL	bAddTextBlocks(ULONG , ULONG, BOOL,
				USHORT, ULONG, const ULONG *, size_t);
extern BOOL	bGet6DocumentText(FILE *, BOOL, ULONG,
				const ULONG *, size_t, const UCHAR *);
extern BOOL	bGet8DocumentText(FILE *, const pps_info_type *,
				const ULONG *, size_t, const ULONG *, size_t,
				const UCHAR *);
/* fmt_text.c */
extern void	vPrologueFMT(diagram_type *, const options_type *);
extern void	vSubstringFMT(diagram_type *, const char *, size_t, long,
				USHORT);
/* fontlist.c */
extern void	vDestroyFontInfoList(void);
extern void	vCorrectFontValues(font_block_type *);
extern void	vAdd2FontInfoList(const font_block_type *);
extern const font_block_type	*pGetNextFontInfoListItem(
					const font_block_type *);
/* fonts.c */
extern int	iGetFontByNumber(UCHAR, USHORT);
extern const char	*szGetOurFontname(int);
extern int	iFontname2Fontnumber(const char *, USHORT);
extern void	vCreate0FontTable(void);
extern void	vCreate2FontTable(FILE *, int, const UCHAR *);
extern void	vCreate6FontTable(FILE *, ULONG,
			const ULONG *, size_t, const UCHAR *);
extern void	vCreate8FontTable(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vDestroyFontTable(void);
extern const font_table_type	*pGetNextFontTableRecord(
						const font_table_type *);
extern size_t	tGetFontTableLength(void);
extern void	vCorrectFontTable(conversion_type, encoding_type);
extern long	lComputeSpaceWidth(drawfile_fontref, USHORT);
/* fonts_r.c & fonts_u.c */
extern FILE	*pOpenFontTableFile(void);
extern void	vCloseFont(void);
extern drawfile_fontref	tOpenFont(UCHAR, USHORT, USHORT);
extern drawfile_fontref	tOpenTableFont(USHORT);
extern long	lComputeStringWidth(const char *, size_t, drawfile_fontref, USHORT);
extern size_t	tCountColumns(const char *, size_t);
extern size_t	tGetCharacterLength(const char *);
/* fonts_u.c */
#if !defined(__riscos)
extern const char	*szGetFontname(drawfile_fontref);
#endif /* !__riscos */
/* hdrftrlist.c */
extern void	vDestroyHdrFtrInfoList(void);
extern void	vCreat8HdrFtrInfoList(const ULONG *, size_t);
extern void	vCreat6HdrFtrInfoList(const ULONG *, size_t);
extern void	vCreat2HdrFtrInfoList(const ULONG *, size_t);
extern const hdrftr_block_type *pGetHdrFtrInfo(int, BOOL, BOOL, BOOL);
extern void	vPrepareHdrFtrText(FILE *);
#if defined(__riscos)
/* icons.c */
extern void	vUpdateIcon(window_handle, icon_block *);
extern void	vUpdateRadioButton(window_handle, icon_handle, BOOL);
extern void	vUpdateWriteable(window_handle, icon_handle, const char *);
extern void	vUpdateWriteableNumber(window_handle, icon_handle, int);
#endif /* __riscos */
/* imgexam.c */
extern image_info_enum	eExamineImage(FILE *, ULONG, imagedata_type *);
/* imgtrans */
extern BOOL	bTranslateImage(diagram_type *,
			FILE *, BOOL, ULONG, const imagedata_type *);
/* jpeg2eps.c & jpeg2spr.c */
extern BOOL	bTranslateJPEG(diagram_type *,
			FILE *, ULONG, size_t, const imagedata_type *);
/* listlist.c */
extern void	vDestroyListInfoList(void);
extern void	vBuildLfoList(const UCHAR *, size_t);
extern void	vAdd2ListInfoList(ULONG, USHORT, UCHAR,
			const list_block_type *);
extern const list_block_type	*pGetListInfo(USHORT, UCHAR);
extern USHORT	usGetListValue(int, int, const style_block_type *);
/* misc.c */
#if !defined(__riscos)
extern const char	*szGetHomeDirectory(void);
extern const char	*szGetAntiwordDirectory(void);
#endif /* !__riscos */
extern long	lGetFilesize(const char *);
#if defined(DEBUG)
extern void	vPrintBlock(const char *, int, const UCHAR *, size_t);
extern void	vPrintUnicode(const char *, int, const UCHAR *, size_t);
extern BOOL	bCheckDoubleLinkedList(output_type *);
#endif /* DEBUG */

#if CR3_ANTIWORD_PATCH!=1
#define aw_rewind rewind
#define aw_getc getc
#else
extern void aw_rewind(FILE * f);
extern int aw_getc(FILE * f);
#endif

extern BOOL	bReadBytes(UCHAR *, size_t, ULONG, FILE *);
extern BOOL	bReadBuffer(FILE *, ULONG, const ULONG *, size_t, size_t,
			UCHAR *, ULONG, size_t);
extern ULONG	ulColor2Color(UCHAR);
extern output_type *pSplitList(output_type *);
extern size_t	tNumber2Roman(UINT, BOOL, char *);
extern size_t	tNumber2Alpha(UINT, BOOL, char *);
extern char	*unincpy(char *, const UCHAR *, size_t);
extern size_t	unilen(const UCHAR *);
extern const char	*szBasename(const char *);
extern long	lComputeLeading(USHORT);
extern size_t	tUcs2Utf8(ULONG, char *, size_t);
extern void	vGetBulletValue(conversion_type, encoding_type, char *, size_t);
extern BOOL	bAllZero(const UCHAR *, size_t);
extern BOOL	bGetNormalizedCodeset(char *, size_t, BOOL *);
extern const char	*szGetDefaultMappingFile(void);
extern time_t	tConvertDTTM(ULONG);
/* notes.c */
extern void	vDestroyNotesInfoLists(void);
extern void	vGetNotesInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *, int);
extern void	vPrepareFootnoteText(FILE *);
extern const char	*szGetFootnootText(UINT);
extern notetype_enum eGetNotetype(ULONG);
/* options.c */
extern int	iReadOptions(int, char **);
extern void	vGetOptions(options_type *);
extern void vSetOptions(options_type *pOptions);
#if defined(__riscos)
extern void	vChoicesOpenAction(window_handle);
extern BOOL	bChoicesMouseClick(event_pollblock *, void *);
extern BOOL	bChoicesKeyPressed(event_pollblock *, void *);
#endif /* __riscos */
/* out2window.c */
extern void	vSetLeftIndentation(diagram_type *, long);
extern void	vAlign2Window(diagram_type *, output_type *,
			long, UCHAR);
extern void	vJustify2Window(diagram_type *, output_type *,
			long, long, UCHAR);
extern void	vResetStyles(void);
extern size_t	tStyle2Window(char *, size_t, const style_block_type *,
			const section_block_type *);
extern void	vTableRow2Window(diagram_type *, output_type *,
			const row_block_type *, conversion_type, int);
/* pdf.c */
extern void	vCreateInfoDictionary(diagram_type *, int);
extern void	vProloguePDF(diagram_type *,
			const char *, const options_type *);
extern void	vEpiloguePDF(diagram_type *);
extern void	vImageProloguePDF(diagram_type *, const imagedata_type *);
extern void	vImageEpiloguePDF(diagram_type *);
extern BOOL	bAddDummyImagePDF(diagram_type *, const imagedata_type *);
extern void	vAddFontsPDF(diagram_type *);
extern void	vMove2NextLinePDF(diagram_type *, USHORT);
extern void	vSubstringPDF(diagram_type *,
				char *, size_t, long, UCHAR, USHORT,
				drawfile_fontref, USHORT, USHORT);
extern void	vStartOfParagraphPDF(diagram_type *, long);
extern void	vEndOfParagraphPDF(diagram_type *, USHORT, long);
extern void	vEndOfPagePDF(diagram_type *, BOOL);
/* pictlist.c */
extern void	vDestroyPictInfoList(void);
extern void	vAdd2PictInfoList(const picture_block_type *);
extern ULONG	ulGetPictInfoListItem(ULONG);
/* png2eps.c & png2spr.c */
extern BOOL	bTranslatePNG(diagram_type *,
			FILE *, ULONG, size_t, const imagedata_type *);
/* postscript.c */
extern void	vProloguePS(diagram_type *,
			const char *, const char *, const options_type *);
extern void	vEpiloguePS(diagram_type *);
extern void	vImageProloguePS(diagram_type *, const imagedata_type *);
extern void	vImageEpiloguePS(diagram_type *);
extern BOOL	bAddDummyImagePS(diagram_type *, const imagedata_type *);
extern void	vAddFontsPS(diagram_type *);
extern void	vMove2NextLinePS(diagram_type *, USHORT);
extern void	vSubstringPS(diagram_type *,
				char *, size_t, long, UCHAR, USHORT,
				drawfile_fontref, USHORT, USHORT);
extern void	vStartOfParagraphPS(diagram_type *, long);
extern void	vEndOfParagraphPS(diagram_type *, USHORT, long);
extern void	vEndOfPagePS(diagram_type *, BOOL);
/* prop0.c */
extern void	vGet0DopInfo(FILE *, const UCHAR *);
extern void	vGet0SepInfo(FILE *, const UCHAR *);
extern void	vGet0PapInfo(FILE *, const UCHAR *);
extern void	vGet0ChrInfo(FILE *, const UCHAR *);
/* prop2.c */
extern void	vGet2DopInfo(FILE *, const UCHAR *);
extern void	vGet2SepInfo(FILE *, const UCHAR *);
extern void	vGet2HdrFtrInfo(FILE *, const UCHAR *);
extern row_info_enum	eGet2RowInfo(int,
			const UCHAR *, int, row_block_type *);
extern void	vGet2StyleInfo(int,
			const UCHAR *, int, style_block_type *);
extern void	vGet2PapInfo(FILE *, const UCHAR *);
extern void	vGet1FontInfo(int,
			const UCHAR *, size_t, font_block_type *);
extern void	vGet2FontInfo(int,
			const UCHAR *, size_t, font_block_type *);
extern void	vGet2ChrInfo(FILE *, int, const UCHAR *);
/* prop6.c */
extern void	vGet6DopInfo(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet6SepInfo(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet6HdrFtrInfo(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
extern row_info_enum	eGet6RowInfo(int,
			const UCHAR *, int, row_block_type *);
extern void	vGet6StyleInfo(int,
			const UCHAR *, int, style_block_type *);
extern void	vGet6PapInfo(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet6FontInfo(int, USHORT,
			const UCHAR *, int, font_block_type *);
extern void	vGet6ChrInfo(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
/* prop8.c */
extern void	vGet8DopInfo(FILE *, const pps_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet8SepInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet8HdrFtrInfo(FILE *, const pps_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern row_info_enum	eGet8RowInfo(int,
			const UCHAR *, int, row_block_type *);
extern void	vGet8StyleInfo(int,
			const UCHAR *, int, style_block_type *);
extern void	vGet8LstInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet8PapInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet8FontInfo(int, USHORT,
			const UCHAR *, int, font_block_type *);
extern void	vGet8ChrInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
/* properties.c */
extern void	vGetPropertyInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *, int);
extern row_info_enum	ePropMod2RowInfo(USHORT, int);
/* propmod.c */
extern void	vDestroyPropModList(void);
extern void	vAdd2PropModList(const UCHAR *);
extern const UCHAR	*aucReadPropModListItem(USHORT);
/* rowlist.c */
extern void	vDestroyRowInfoList(void);
extern void	vAdd2RowInfoList(const row_block_type *);
extern const row_block_type	*pGetNextRowInfoListItem(void);
/* riscos.c */
#if defined(__riscos)
extern int	iGetFiletype(const char *);
extern void	vSetFiletype(const char *, int);
extern BOOL	bMakeDirectory(const char *);
extern int	iReadCurrentAlphabetNumber(void);
extern int	iGetRiscOsVersion(void);
extern BOOL	bDrawRenderDiag360(void *, size_t,
			window_redrawblock *, double, os_error *);
#if defined(DEBUG)
extern BOOL	bGetJpegInfo(UCHAR *, size_t);
#endif /* DEBUG */
#endif /* __riscos */
/* saveas.c */
#if defined(__riscos)
extern BOOL	bSaveTextfile(event_pollblock *, void *);
extern BOOL	bSaveDrawfile(event_pollblock *, void *);
#endif /* __riscos */
/* sectlist.c */
extern void	vDestroySectionInfoList(void);
extern void	vAdd2SectionInfoList(const section_block_type *, ULONG);
extern void	vGetDefaultSection(section_block_type *);
extern void	vDefault2SectionInfoList(ULONG);
extern const section_block_type *
		pGetSectionInfo(const section_block_type *, ULONG);
extern size_t	tGetNumberOfSections(void);
extern UCHAR	ucGetSepHdrFtrSpecification(size_t);
/* stylelist.c */
extern void	vDestroyStyleInfoList(void);
extern level_type_enum	eGetNumType(UCHAR);
extern void	vCorrectStyleValues(style_block_type *);
extern void	vAdd2StyleInfoList(const style_block_type *);
extern const style_block_type	*pGetNextStyleInfoListItem(
					const style_block_type *);
extern const style_block_type	*pGetNextTextStyle(const style_block_type *);
extern USHORT	usGetIstd(ULONG);
extern BOOL	bStyleImpliesList(const style_block_type *, int);
/* stylesheet.c */
extern void	vDestroyStylesheetList(void);
extern USHORT	usStc2istd(UCHAR);
extern void	vGet2Stylesheet(FILE *, int, const UCHAR *);
extern void	vGet6Stylesheet(FILE *, ULONG, const ULONG *, size_t,
			const UCHAR *);
extern void	vGet8Stylesheet(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vFillStyleFromStylesheet(USHORT, style_block_type *);
extern void	vFillFontFromStylesheet(USHORT, font_block_type *);
/* summary.c */
extern void	vDestroySummaryInfo(void);
extern void	vSet0SummaryInfo(FILE *, const UCHAR *);
extern void	vSet2SummaryInfo(FILE *, int, const UCHAR *);
extern void	vSet6SummaryInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern void	vSet8SummaryInfo(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *);
extern const char	*szGetTitle(void);
extern const char	*szGetSubject(void);
extern const char	*szGetAuthor(void);
extern const char	*szGetLastSaveDtm(void);
extern const char	*szGetModDate(void);
extern const char	*szGetCreationDate(void);
extern const char	*szGetCompany(void);
extern const char	*szGetLanguage(void);
/* tabstop.c */
extern void	vSetDefaultTabWidth(FILE *, const pps_info_type *,
			const ULONG *, size_t, const ULONG *, size_t,
			const UCHAR *, int);
extern long	lGetDefaultTabWidth(void);
/* text.c */
extern void	vPrologueTXT(diagram_type *, const options_type *);
extern void	vEpilogueTXT(FILE *);
extern void	vMove2NextLineTXT(diagram_type *);
extern void	vSubstringTXT(diagram_type *, const char *, size_t, long);
extern void	vStartOfParagraphTXT(diagram_type *, long);
extern void	vEndOfParagraphTXT(diagram_type *, long);
extern void	vEndOfPageTXT(diagram_type *, long);
/* unix.c */
extern void	werr(int, const char *, ...);
#if !defined(__riscos)
extern void	Hourglass_On(void);
extern void	Hourglass_Off(void);
#endif /* !__riscos */
/* utf8.c */
#if !defined(__riscos)
extern long	utf8_strwidth(const char *, size_t);
extern int	utf8_chrlength(const char *);
extern BOOL	is_locale_utf8(void);
#endif /* !__riscos */
/* word2text.c */
extern BOOL	bOutputContainsText(const output_type *);
extern BOOL	bWordDecryptor(FILE *, long, diagram_type *);
extern output_type	*pHdrFtrDecryptor(FILE *, ULONG, ULONG);
extern char		*szFootnoteDecryptor(FILE *, ULONG, ULONG);
/* worddos.c */
extern int	iInitDocumentDOS(FILE *, long);
/* wordlib.c */
extern BOOL	bIsWordForDosFile(FILE *, long);
extern BOOL	bIsRtfFile(FILE *);
extern BOOL	bIsWordPerfectFile(FILE *);
extern BOOL	bIsWinWord12File(FILE *, long);
extern BOOL	bIsMacWord45File(FILE *);
extern int	iGuessVersionNumber(FILE *, long);
extern int	iGetVersionNumber(const UCHAR *);
extern BOOL	bIsOldMacFile(void);
extern int	iInitDocument(FILE *, long);
extern void	vFreeDocument(void);
/* wordmac.c */
extern int	iInitDocumentMAC(FILE *, long);
/* wordole.c */
extern int	iInitDocumentOLE(FILE *, long);
/* wordwin.c */
extern int	iInitDocumentWIN(FILE *, long);
/* xmalloc.c */
extern void 	*xmalloc(size_t);
extern void	*xcalloc(size_t, size_t);
extern void 	*xrealloc(void *, size_t);
extern char	*xstrdup(const char *);
extern void 	*xfree(void *);
/* xml.c */
extern void	vCreateBookIntro(diagram_type *, int);
extern void	vPrologueXML(diagram_type *, const options_type *);
extern void	vEpilogueXML(diagram_type *);
extern void	vMove2NextLineXML(diagram_type *);
extern void	vSubstringXML(diagram_type *,
				const char *, size_t, long, USHORT);
extern void	vStartOfParagraphXML(diagram_type *, UINT);
extern void	vEndOfParagraphXML(diagram_type *, UINT);
extern void	vEndOfPageXML(diagram_type *);
extern void	vSetHeadersXML(diagram_type *, USHORT);
extern void	vStartOfListXML(diagram_type *, UCHAR, BOOL);
extern void	vEndOfListXML(diagram_type *);
extern void	vStartOfListItemXML(diagram_type *, BOOL);
extern void	vEndOfTableXML(diagram_type *);
extern void	vAddTableRowXML(diagram_type *, char **, int,
			const short *, UCHAR);

#endif /* __antiword_h */
