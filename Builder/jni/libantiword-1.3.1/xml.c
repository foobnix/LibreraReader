/*
 * xml.c
 * Copyright (C) 2002-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Functions to deal with the XML/DocBook format
 *
 */

#include <string.h>
#include "antiword.h"


#define vAddEndTagsUntil1(p,t)	vAddEndTagsUntil2(p,t,TAG_NOTAG)	

#if defined(DEBUG)
#define vStackTrace()	__vStackTrace(__LINE__)
#else
#define vStackTrace()	/* EMPTY */
#endif /* DEBUG */

/* The character set */
static encoding_type	eEncoding = encoding_neutral;
/* Word version */
static int	iWordVersion = -1;
/* Special treatment for files from Word 4/5/6 on an Apple Macintosh */
static BOOL	bOldMacFile = FALSE;
/* Text is emphasised */
static BOOL	bEmphasisOpen = FALSE;
/* Text is superscript */
static BOOL	bSuperscriptOpen = FALSE;
/* Text is subscript */
static BOOL	bSubscriptOpen = FALSE;
/* Title is open */
static BOOL	bTitleOpen = FALSE;
/* Table is open */
static BOOL	bTableOpen = FALSE;
/* Footnote is open */
static BOOL	bFootnoteOpen = FALSE;
/* Current paragraph level */
static UINT	uiParagraphLevel = 0;
/* Current list level */
static UINT	uiListLevel = 0;
/* Current list level is still empty */
static BOOL	bEmptyListLevel = TRUE;
/* Current header level */
static USHORT	usHeaderLevelCurrent = 0;
/* Current header level is still empty */
static BOOL	bEmptyHeaderLevel = TRUE;
/* Number of columns in the current table */
static int	iTableColumnsCurrent = 0;
/* Footnote number */
static UINT	uiFootnoteNumber = 0;

/* Constants for the stack */
#define INITIAL_STACK_SIZE	10
#if defined(DEBUG)
#define EXTENSION_STACK_SIZE	 2
#else
#define EXTENSION_STACK_SIZE	10
#endif /* DEBUG */

/* Variables for the stack */
static UCHAR	*aucStack = NULL;
static size_t	tStacksize = 0;
static size_t	tStackNextFree = 0;

/* Constants for the tags */
#define TAG_NOTAG		(UCHAR)0
#define TAG_AUTHOR		(UCHAR)1
#define TAG_BEGINPAGE		(UCHAR)2
#define TAG_BOOK		(UCHAR)3
#define TAG_BOOKINFO		(UCHAR)4
#define TAG_CHAPTER		(UCHAR)5
#define TAG_COLSPEC		(UCHAR)6
#define TAG_CORPNAME		(UCHAR)7
#define TAG_DATE		(UCHAR)8
#define TAG_EMPHASIS		(UCHAR)9
#define TAG_ENTRY		(UCHAR)10
#define TAG_FILENAME		(UCHAR)11
#define TAG_FOOTNOTE		(UCHAR)12
#define TAG_INFORMALTABLE	(UCHAR)13
#define TAG_ITEMIZEDLIST	(UCHAR)14
#define TAG_LISTITEM		(UCHAR)15
#define TAG_ORDEREDLIST		(UCHAR)16
#define TAG_PARA		(UCHAR)17
#define TAG_ROW			(UCHAR)18
#define TAG_SECT1		(UCHAR)19
#define TAG_SECT2		(UCHAR)20
#define TAG_SECT3		(UCHAR)21
#define TAG_SECT4		(UCHAR)22
#define TAG_SECT5		(UCHAR)23
#define TAG_SUBSCRIPT		(UCHAR)24
#define TAG_SUBTITLE		(UCHAR)25
#define TAG_SUPERSCRIPT		(UCHAR)26
#define TAG_SURNAME		(UCHAR)27
#define TAG_TBODY		(UCHAR)28
#define TAG_TGROUP		(UCHAR)29
#define TAG_TITLE		(UCHAR)30

typedef struct docbooktags_tag {
	UCHAR	ucTagnumber;
	char	szTagname[15];
	BOOL	bAddNewlineStart;
	BOOL	bAddNewlineEnd;
} docbooktags_type;

static const docbooktags_type atDocBookTags[] = {
	{	TAG_NOTAG, 		"!ERROR!",	TRUE,	TRUE	},
	{	TAG_AUTHOR,		"author",	TRUE,	TRUE	},
	{	TAG_BEGINPAGE,		"beginpage",	TRUE,	TRUE	},
	{	TAG_BOOK, 		"book",		TRUE,	TRUE	},
	{	TAG_BOOKINFO, 		"bookinfo",	TRUE,	TRUE	},
	{	TAG_CHAPTER, 		"chapter",	TRUE,	TRUE	},
	{	TAG_COLSPEC,		"colspec",	TRUE,	TRUE	},
	{	TAG_CORPNAME,		"corpname",	FALSE,	FALSE	},
	{	TAG_DATE,		"date",		FALSE,	FALSE	},
	{	TAG_EMPHASIS,		"emphasis",	FALSE,	FALSE	},
	{	TAG_ENTRY,		"entry",	TRUE,	TRUE	},
	{	TAG_FILENAME,		"filename",	FALSE,	FALSE	},
	{	TAG_FOOTNOTE,		"footnote",	FALSE,	FALSE	},
	{	TAG_INFORMALTABLE,	"informaltable",TRUE,	TRUE	},
	{	TAG_ITEMIZEDLIST,	"itemizedlist",	TRUE,	TRUE	},
	{	TAG_LISTITEM,		"listitem",	TRUE,	TRUE	},
	{	TAG_ORDEREDLIST,	"orderedlist",	TRUE,	TRUE	},
	{	TAG_PARA, 		"para",		TRUE,	TRUE	},
	{	TAG_ROW,		"row",		TRUE,	TRUE	},
	{	TAG_SECT1, 		"sect1",	TRUE,	TRUE	},
	{	TAG_SECT2, 		"sect2",	TRUE,	TRUE	},
	{	TAG_SECT3, 		"sect3",	TRUE,	TRUE	},
	{	TAG_SECT4, 		"sect4",	TRUE,	TRUE	},
	{	TAG_SECT5, 		"sect5",	TRUE,	TRUE	},
	{	TAG_SUBSCRIPT,		"subscript",	FALSE,	FALSE	},
	{	TAG_SUBTITLE,		"subtitle",	FALSE,	FALSE	},
	{	TAG_SUPERSCRIPT,	"superscript",	FALSE,	FALSE	},
	{	TAG_SURNAME,		"surname",	FALSE,	FALSE	},
	{	TAG_TBODY,		"tbody",	TRUE,	TRUE	},
	{	TAG_TGROUP,		"tgroup",	TRUE,	TRUE	},
	{	TAG_TITLE, 		"title",	FALSE,	FALSE	},
};

static void	vAddStartTag(diagram_type *, UCHAR, const char *);
static void	vAddEndTag(diagram_type *, UCHAR);
static void	vAddCombinedTag(diagram_type *, UCHAR, const char *);
static void	vPrintChar(diagram_type *, char);


#if defined(DEBUG)
/*
 * vCheckTagTable - check the tag table
 */
static void
vCheckTagTable(void)
{
	size_t	tIndex;

	for (tIndex = 0; tIndex < elementsof(atDocBookTags); tIndex++) {
		if (tIndex != (size_t)atDocBookTags[tIndex].ucTagnumber) {
			DBG_DEC(tIndex);
			werr(1, "Array atDocBookTags is broken");
		}
	}
} /* end of vCheckTagTable */

/*
 * __vStackTrace - show a stack trace
 */
static void
__vStackTrace(int iLine)
{
	int	iIndex;

	fprintf(stderr, "%s[%3d]:\n", __FILE__, iLine);

	if (tStackNextFree == 0) {
		fprintf(stderr, "The stack is empty\n");
		return;
	}
	for (iIndex = (int)tStackNextFree - 1; iIndex >= 0; iIndex--) {
		fprintf(stderr, "%2d: %2d: '%s'\n",
			iIndex,
			(int)atDocBookTags[(UINT)aucStack[iIndex]].ucTagnumber,
			atDocBookTags[(UINT)aucStack[iIndex]].szTagname);
	}
} /* end of __vStackTrace */
#endif /* DEBUG */

/*
 * vPushStack - push a tag onto the stack
 */
static void
vPushStack(UCHAR ucTag)
{
	fail(tStackNextFree > tStacksize);

	if (tStackNextFree == tStacksize) {
		/* The stack is full; enlarge the stack */
		tStacksize += EXTENSION_STACK_SIZE;
		aucStack = xrealloc(aucStack, tStacksize * sizeof(UCHAR));
		DBG_DEC(tStacksize);
	}

	fail(tStackNextFree >= tStacksize);

	aucStack[tStackNextFree++] = ucTag;
} /* end of vPushStack */

/*
 * vPopStack - pop a tag from the stack
 */
static UCHAR
ucPopStack(void)
{
	DBG_DEC_C(tStackNextFree > tStacksize, tStackNextFree);
	DBG_DEC_C(tStackNextFree > tStacksize, tStacksize);
	fail(tStackNextFree > tStacksize);
	fail(tStackNextFree == 0);

	if (tStackNextFree == 0) {
		werr(1, "The stack is empty, unable to continue");
		return TAG_NOTAG;
	}
	return aucStack[--tStackNextFree];
} /* end of ucPopStack */

/*
 * vReadStack - read a tag from the top of the stack
 */
static UCHAR
ucReadStack(void)
{
	DBG_DEC_C(tStackNextFree > tStacksize, tStackNextFree);
	DBG_DEC_C(tStackNextFree > tStacksize, tStacksize);
	fail(tStackNextFree > tStacksize);

	if (tStackNextFree == 0) {
		/* The stack is empty */
		return TAG_NOTAG;
	}
	return aucStack[tStackNextFree - 1];
} /* end of ucReadStack */

/*
 * vPrintLevel - print the tag level
 */
static void
vPrintLevel(FILE *pOutFile)
{
	size_t	tIndex;

	fail(pOutFile == NULL);

	for (tIndex = 0; tIndex < tStackNextFree; tIndex++) {
		(void)putc(' ', pOutFile);
	}
} /* end of vPrintLevel */

/*
 * vPrintFootnote - print a footnote
 */
static void
vPrintFootnote(diagram_type *pDiag, UINT uiFootnoteIndex)
{
	const char	*szText, *pcTmp;
	BOOL	bSuScript;
	UCHAR	ucTopTag;

	TRACE_MSG("vPrintFootnote");

	szText = szGetFootnootText(uiFootnoteIndex);

	if (szText == NULL) {
		szText = "";
	}

	/* Remove the subscript/superscript (if any) */
	ucTopTag = ucReadStack();
	bSuScript = ucTopTag == TAG_SUBSCRIPT || ucTopTag == TAG_SUPERSCRIPT;
	if (bSuScript) {
		vAddEndTag(pDiag, ucTopTag);
	}

	/* Start a footnote */
	vAddStartTag(pDiag, TAG_FOOTNOTE, NULL);
	vAddStartTag(pDiag, TAG_PARA, NULL);

	/* Print a footnote */
	for (pcTmp = szText; *pcTmp != '\0'; pcTmp++) {
		if (*pcTmp == PAR_END) {
			if (*(pcTmp + 1) != PAR_END && *(pcTmp + 1) != '\0') {
				/* PAR_END is not empty and not last */
				vAddEndTag(pDiag, TAG_PARA);
				vAddStartTag(pDiag, TAG_PARA, NULL);
			}
		} else {
			vPrintChar(pDiag, *pcTmp);
		}
	}

	/* End a footnote */
	vAddEndTag(pDiag, TAG_PARA);
	vAddEndTag(pDiag, TAG_FOOTNOTE);

	/* Repair the subscript/superscript (if any) */
	if (bSuScript) {
		vAddStartTag(pDiag, ucTopTag, NULL);
	}
} /* end of vPrintFootnote */

/*
 * vPrintChar - print a character with XML encoding
 */
static void
vPrintChar(diagram_type *pDiag, char cChar)
{
	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);

	switch (cChar) {
	case FOOTNOTE_OR_ENDNOTE:
		uiFootnoteNumber++;
		vPrintFootnote(pDiag, uiFootnoteNumber - 1);
		break;
	case '<':
		fprintf(pDiag->pOutFile, "%s", "&lt;");
		break;
	case '>':
		fprintf(pDiag->pOutFile, "%s", "&gt;");
		break;
	case '&':
		fprintf(pDiag->pOutFile, "%s", "&amp;");
		break;
	default:
		(void)putc(cChar, pDiag->pOutFile);
		break;
	}
} /* end of vPrintChar */

/*
 * vPrintSpecialChar - convert and print a character
 */
static void
vPrintSpecialChar(diagram_type *pDiag, USHORT usChar)
{
	ULONG   ulChar;
	size_t  tLen, tIndex;
	char    szResult[4];

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(iWordVersion < 0);
	fail(eEncoding == encoding_neutral);

	ulChar = ulTranslateCharacters(usChar, 0, iWordVersion,
				conversion_xml, eEncoding, bOldMacFile);
	tLen = tUcs2Utf8(ulChar, szResult, sizeof(szResult));
	if (tLen == 1) {
		vPrintChar(pDiag, szResult[0]);
	} else {
		for (tIndex = 0; tIndex < tLen; tIndex++) {
			(void)putc(szResult[tIndex], pDiag->pOutFile);
		}
	}
} /* end of vPrintSpecialChar */

/*
 * vPrintSpecialString - convert and print a string
 */
static void
vPrintSpecialString(diagram_type *pDiag, const char *szString)
{
	int	iIndex;
	USHORT	usChar;

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(szString == NULL);

	for (iIndex = 0; szString[iIndex] != '\0'; iIndex++) {
		usChar = (USHORT)(UCHAR)szString[iIndex];
		vPrintSpecialChar(pDiag, usChar);
	}
} /* end of vPrintSpecialString */

/*
 * vAddStartTag - add the specified start tag to the file
 */
static void
vAddStartTag(diagram_type *pDiag, UCHAR ucTag, const char *szAttribute)
{
	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail((size_t)ucTag >= elementsof(atDocBookTags));

	if (atDocBookTags[(UINT)ucTag].bAddNewlineStart) {
		fprintf(pDiag->pOutFile, "\n");
		vPrintLevel(pDiag->pOutFile);
	}

	if (szAttribute == NULL || szAttribute[0] == '\0') {
		fprintf(pDiag->pOutFile, "<%s>",
			atDocBookTags[(UINT)ucTag].szTagname);
	} else {
		fprintf(pDiag->pOutFile, "<%s %s>",
			atDocBookTags[(UINT)ucTag].szTagname, szAttribute);
	}

	if (atDocBookTags[(UINT)ucTag].bAddNewlineEnd) {
		fprintf(pDiag->pOutFile, "\n");
		pDiag->lXleft = 0;
	}

	vPushStack(ucTag);

	/* Set global variables */
	switch (ucTag) {
	case TAG_CHAPTER:
		usHeaderLevelCurrent = 1;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_SECT1:
		usHeaderLevelCurrent = 2;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_SECT2:
		usHeaderLevelCurrent = 3;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_SECT3:
		usHeaderLevelCurrent = 4;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_SECT4:
		usHeaderLevelCurrent = 5;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_SECT5:
		usHeaderLevelCurrent = 6;
		bEmptyHeaderLevel = TRUE;
		break;
	case TAG_TITLE:
		fail(uiParagraphLevel != 0);
		bTitleOpen = TRUE;
		break;
	case TAG_FOOTNOTE:
		bFootnoteOpen = TRUE;
		break;
	case TAG_PARA:
		fail(bTitleOpen && !bFootnoteOpen);
		uiParagraphLevel++;
		bEmptyHeaderLevel = FALSE;
		break;
	case TAG_EMPHASIS:
		bEmphasisOpen = TRUE;
		break;
	case TAG_ITEMIZEDLIST:
	case TAG_ORDEREDLIST:
		uiListLevel++;
		bEmptyListLevel = TRUE;
		bEmptyHeaderLevel = FALSE;
		break;
	case TAG_LISTITEM:
		bEmptyListLevel = FALSE;
		break;
	case TAG_SUPERSCRIPT:
		bSuperscriptOpen = TRUE;
		break;
	case TAG_SUBSCRIPT:
		bSubscriptOpen = TRUE;
		break;
	case TAG_INFORMALTABLE:
		bTableOpen = TRUE;
		bEmptyHeaderLevel = FALSE;
		break;
	default:
		break;
	}
} /* end of vAddStartTag */

/*
 * vAddEndTag - add the specified end tag to the file
 */
static void
vAddEndTag(diagram_type *pDiag, UCHAR ucTag)
{
	UCHAR	ucTopTag;

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail((size_t)ucTag >= elementsof(atDocBookTags));

#if defined(DEBUG)
	ucTopTag = ucReadStack();
	if (ucTag != ucTopTag) {
		DBG_DEC(ucTag);
		DBG_MSG(atDocBookTags[(UINT)ucTag].szTagname);
		vStackTrace();
	}
#endif /* DEBUG */

	ucTopTag = ucPopStack();
	fail((size_t)ucTopTag >= elementsof(atDocBookTags));
	if (ucTag != ucTopTag) {
		DBG_DEC(ucTag);
		DBG_DEC(ucTopTag);
		DBG_FIXME();
		werr(1, "Impossible tag sequence, unable to continue");
	}

	if (atDocBookTags[(UINT)ucTag].bAddNewlineEnd) {
		fprintf(pDiag->pOutFile, "\n");
		vPrintLevel(pDiag->pOutFile);
	}

	fprintf(pDiag->pOutFile, "</%s>", atDocBookTags[(UINT)ucTag].szTagname);

	if (atDocBookTags[(UINT)ucTag].bAddNewlineStart) {
		fprintf(pDiag->pOutFile, "\n");
		pDiag->lXleft = 0;
	}

	/* Set global variables */
	switch (ucTag) {
	case TAG_CHAPTER:
		usHeaderLevelCurrent = 0;
		break;
	case TAG_SECT1:
		usHeaderLevelCurrent = 1;
		break;
	case TAG_SECT2:
		usHeaderLevelCurrent = 2;
		break;
	case TAG_SECT3:
		usHeaderLevelCurrent = 3;
		break;
	case TAG_SECT4:
		usHeaderLevelCurrent = 4;
		break;
	case TAG_SECT5:
		usHeaderLevelCurrent = 5;
		break;
	case TAG_TITLE:
		bTitleOpen = FALSE;
		break;
	case TAG_FOOTNOTE:
		bFootnoteOpen = FALSE;
		break;
	case TAG_PARA:
		uiParagraphLevel--;
		break;
	case TAG_EMPHASIS:
		bEmphasisOpen = FALSE;
		break;
	case TAG_SUPERSCRIPT:
		bSuperscriptOpen = FALSE;
		break;
	case TAG_ITEMIZEDLIST:
	case TAG_ORDEREDLIST:
		uiListLevel--;
		break;
	case TAG_SUBSCRIPT:
		bSubscriptOpen = FALSE;
		break;
	case TAG_INFORMALTABLE:
		bTableOpen = FALSE;
		iTableColumnsCurrent = 0;
		break;
	default:
		break;
	}
} /* end of vAddEndTag */

/*
 * vAddEndTagOptional - add the specified end tag to the file if needed
 */
static void
vAddEndTagOptional(diagram_type *pDiag, UCHAR ucTag)
{
	UCHAR	ucTopTag;

	ucTopTag = ucReadStack();
	if (ucTag == ucTopTag) {
		vAddEndTag(pDiag, ucTag);
	}
} /* end of vAddEndTagOptional */

/*
 * vAddCombinedTag - add the specified start and end tag to the file
 */
static void
vAddCombinedTag(diagram_type *pDiag, UCHAR ucTag, const char *szAttribute)
{
	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail((size_t)ucTag >= elementsof(atDocBookTags));

	if (atDocBookTags[(UINT)ucTag].bAddNewlineStart) {
		fprintf(pDiag->pOutFile, "\n");
		vPrintLevel(pDiag->pOutFile);
	}

	if (szAttribute == NULL || szAttribute[0] == '\0') {
		fprintf(pDiag->pOutFile, "<%s/>",
			atDocBookTags[(UINT)ucTag].szTagname);
	} else {
		fprintf(pDiag->pOutFile, "<%s %s/>",
			atDocBookTags[(UINT)ucTag].szTagname, szAttribute);
	}

	if (atDocBookTags[(UINT)ucTag].bAddNewlineStart) {
		fprintf(pDiag->pOutFile, "\n");
		pDiag->lXleft = 0;
	}
} /* end of vAddCombinedTag */

/*
 * vAddEndTagsUntil2 - add end tags until one the specified tags is seen
 */
static void
vAddEndTagsUntil2(diagram_type *pDiag, UCHAR ucTag1, UCHAR ucTag2)
{
	UCHAR	ucTopTag;

	do {
		ucTopTag = ucReadStack();
		switch (ucTopTag) {
                case TAG_CHAPTER:
                case TAG_SECT1:
                case TAG_SECT2:
                case TAG_SECT3:
                case TAG_SECT4:
                case TAG_SECT5:
			if (bEmptyHeaderLevel) {
				/*
				 * An empty chapter is legal in Word,
				 * but not in DocBook.
				 */
				vAddCombinedTag(pDiag, TAG_PARA, NULL);
				bEmptyHeaderLevel = FALSE;
			}
			break;
		case TAG_ITEMIZEDLIST:
		case TAG_ORDEREDLIST:
			if (bEmptyListLevel) {
				/*
				 * A list without items is legal in Word,
				 * but not in DocBook. (Nor are empty items)
				 */
				vAddStartTag(pDiag, TAG_LISTITEM, NULL);
				vAddCombinedTag(pDiag, TAG_PARA, NULL);
				vAddEndTag(pDiag, TAG_LISTITEM);
				bEmptyListLevel = FALSE;
			}
			break;
		default:
			break;
		}
		vAddEndTag(pDiag, ucTopTag);
	} while (ucTopTag != ucTag1 && ucTopTag != ucTag2);
} /* end of vAddEndTagsUntil2 */

/*
 * vCreateBookIntro - create title and bookinfo
 */
void
vCreateBookIntro(diagram_type *pDiag, int iVersion)
{
	const char	*szTitle, *szSubject, *szAuthor;
	const char	*szLastSaveDtm, *szCompany;
	const char	*szLanguage;
	char		szTmp[13];

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(iVersion < 0);
	fail(eEncoding == encoding_neutral);

	iWordVersion = iVersion;
	bOldMacFile = bIsOldMacFile();
	szTitle = szGetTitle();
	szSubject = szGetSubject();
	szAuthor = szGetAuthor();
	szLastSaveDtm = szGetLastSaveDtm();
	szCompany = szGetCompany();

	/* Start Book */
	szLanguage = szGetLanguage();
	if (szLanguage != NULL) {
		DBG_MSG(szLanguage);
		sprintf(szTmp, "lang='%.5s'", szLanguage);
		szLanguage = szTmp;
	}
	vAddStartTag(pDiag, TAG_BOOK, szLanguage);

	/* Book title */
	if (szTitle != NULL && szTitle[0] != '\0') {
		vAddStartTag(pDiag, TAG_TITLE, NULL);
		vPrintSpecialString(pDiag, szTitle);
		vAddEndTag(pDiag, TAG_TITLE);
	}
	/* Bookinfo */
	if ((szTitle != NULL && szTitle[0] != '\0') ||
	    (szSubject != NULL && szSubject[0] != '\0') ||
	    (szAuthor != NULL && szAuthor[0] != '\0') ||
	    (szLastSaveDtm != NULL && szLastSaveDtm[0] != '\0') ||
	    (szCompany != NULL && szCompany[0] != '\0')) {
		vAddStartTag(pDiag, TAG_BOOKINFO, NULL);
		if (szTitle != NULL && szTitle[0] != '\0') {
			vAddStartTag(pDiag, TAG_TITLE, NULL);
			vPrintSpecialString(pDiag, szTitle);
			vAddEndTag(pDiag, TAG_TITLE);
		}
		if (szSubject != NULL && szSubject[0] != '\0') {
			vAddStartTag(pDiag, TAG_SUBTITLE, NULL);
			vPrintSpecialString(pDiag, szSubject);
			vAddEndTag(pDiag, TAG_SUBTITLE);
		}
		if (szAuthor != NULL && szAuthor[0] != '\0') {
			vAddStartTag(pDiag, TAG_AUTHOR, NULL);
			vAddStartTag(pDiag, TAG_SURNAME, NULL);
			vPrintSpecialString(pDiag, szAuthor);
			vAddEndTag(pDiag, TAG_SURNAME);
			vAddEndTag(pDiag, TAG_AUTHOR);
		}
		if (szLastSaveDtm != NULL && szLastSaveDtm[0] != '\0') {
			vAddStartTag(pDiag, TAG_DATE, NULL);
			vPrintSpecialString(pDiag, szLastSaveDtm);
			vAddEndTag(pDiag, TAG_DATE);
		}
		if (szCompany != NULL && szCompany[0] != '\0') {
			vAddStartTag(pDiag, TAG_CORPNAME, NULL);
			vPrintSpecialString(pDiag, szCompany);
			vAddEndTag(pDiag, TAG_CORPNAME);
		}
		vAddEndTag(pDiag, TAG_BOOKINFO);
	}
} /* end of vCreateBookIntro */

/*
 * vPrologueXML - perform the XML initialization
 */
void
vPrologueXML(diagram_type *pDiag, const options_type *pOptions)
{

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(pOptions == NULL);

#if defined(DEBUG)
	vCheckTagTable();
#endif /* DEBUG */

	/* Set global variables to their start values */
	eEncoding = pOptions->eEncoding;
	bEmphasisOpen = FALSE;
	bSuperscriptOpen = FALSE;
	bSubscriptOpen = FALSE;
	bTitleOpen = FALSE;
	bTableOpen = FALSE;
	bFootnoteOpen = FALSE;
	uiParagraphLevel = 0;
	uiListLevel = 0;
	bEmptyListLevel = TRUE;
	usHeaderLevelCurrent = 0;
	bEmptyHeaderLevel = TRUE;
	iTableColumnsCurrent = 0;
	uiFootnoteNumber = 0;

	pDiag->lXleft = 0;
	pDiag->lYtop = 0;

	/* Create an empty stack */
	tStacksize = INITIAL_STACK_SIZE;
	aucStack = xcalloc(tStacksize, sizeof(UCHAR));
	tStackNextFree = 0;
} /* end of vPrologueXML */

/*
 * vEpilogueXML - clean up after everything is done
 */
void
vEpilogueXML(diagram_type *pDiag)
{
	vStackTrace();

	vAddEndTagsUntil1(pDiag, TAG_BOOK);

	vStackTrace();

	/* Destroy the stack */
	fail(tStackNextFree != 0);
	tStacksize = 0;
	aucStack = xfree(aucStack);
	tStackNextFree = 0;
} /* end of vEpilogueXML */

/*
 * vPrintXML - print a XML string
 */
static void
vPrintXML(diagram_type *pDiag, const char *szString, size_t tStringLength,
		USHORT usFontstyle)
{
	const char	*szAttr;
	int	iCount;
	size_t	tNextFree;
	BOOL	bNotReady, bEmphasisNew, bSuperscriptNew, bSubscriptNew;
	UCHAR	ucTopTag, aucStorage[3];

	fail(szString == NULL);

	if (szString == NULL || szString[0] == '\0' || tStringLength == 0) {
		return;
	}

	if (tStringLength == 1 && szString[0] == FOOTNOTE_OR_ENDNOTE) {
		/* Don't do anything special for just a single footnote */
		bEmphasisNew = FALSE;
		bSuperscriptNew = FALSE;
		bSubscriptNew = FALSE;
	} else {
		/* Situation normal */
		bEmphasisNew = bIsBold(usFontstyle) ||
				bIsItalic(usFontstyle) ||
				bIsUnderline(usFontstyle) ||
				bIsStrike(usFontstyle);
		bSuperscriptNew = bIsSuperscript(usFontstyle);
		bSubscriptNew = bIsSubscript(usFontstyle);
	}

	/* End what has to be ended (or more to keep the stack happy) */
	tNextFree = 0;
	bNotReady = TRUE;
	do {
		ucTopTag = ucReadStack();
		switch (ucTopTag) {
		case TAG_EMPHASIS:
			fail(!bEmphasisOpen);
			if (bEmphasisNew) {
				aucStorage[tNextFree++] = ucTopTag;
			}
			vAddEndTag(pDiag, ucTopTag);
			break;
		case TAG_SUPERSCRIPT:
			fail(!bSuperscriptOpen);
			if (bSuperscriptNew) {
				aucStorage[tNextFree++] = ucTopTag;
			}
			vAddEndTag(pDiag, ucTopTag);
			break;
		case TAG_SUBSCRIPT:
			fail(!bSubscriptOpen);
			if (bSubscriptNew) {
				aucStorage[tNextFree++] = ucTopTag;
			}
			vAddEndTag(pDiag, ucTopTag);
			break;
		default:
			bNotReady = FALSE;
			break;
		}
		fail(tNextFree > elementsof(aucStorage));
		fail(bNotReady && tNextFree == elementsof(aucStorage));
	} while (bNotReady);

	/* Just te make sure */
	vStartOfParagraphXML(pDiag, 1);

	/* Restart to keep the stack happy */
	for (iCount = (int)tNextFree - 1; iCount > 0; iCount--) {
		vAddStartTag(pDiag, aucStorage[iCount], NULL);
	}

	/* Start what has to be started */
	if (bEmphasisNew && !bEmphasisOpen) {
		if (bIsBold(usFontstyle)) {
			szAttr = "role='bold'";
		} else if (bIsItalic(usFontstyle)) {
			szAttr = NULL;
		} else if (bIsUnderline(usFontstyle)) {
			szAttr = "role='underline'";
		} else if (bIsStrike(usFontstyle)) {
			szAttr = "role='strikethrough'";
		} else {
			szAttr = NULL;
		}
		vAddStartTag(pDiag, TAG_EMPHASIS, szAttr);
	}
	if (bSuperscriptNew && !bSuperscriptOpen) {
		vAddStartTag(pDiag, TAG_SUPERSCRIPT, NULL);
	}
	if (bSubscriptNew && !bSubscriptOpen) {
		vAddStartTag(pDiag, TAG_SUBSCRIPT, NULL);
	}

	/* The print the string */
	for (iCount = 0; iCount < (int)tStringLength; iCount++) {
		vPrintChar(pDiag, szString[iCount]);
	}
} /* end of vPrintXML */

/*
 * vMove2NextLineXML - move to the next line
 */
void
vMove2NextLineXML(diagram_type *pDiag)
{
	fail(pDiag == NULL);

	/*
	if (uiParagraphLevel != 0) {
		We need something like HTML's <BR> tag
	}
	*/
} /* end of vMove2NextLineXML */

/*
 * vSubstringXML - put a sub string into a diagram
 */
void
vSubstringXML(diagram_type *pDiag,
	const char *szString, size_t tStringLength, long lStringWidth,
	USHORT usFontstyle)
{
	fail(pDiag == NULL || szString == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(pDiag->lXleft < 0);
	fail(tStringLength != strlen(szString));

	if (szString[0] == '\0' || tStringLength == 0) {
		return;
	}

	vPrintXML(pDiag, szString, tStringLength, usFontstyle);
	pDiag->lXleft += lStringWidth;
} /* end of vSubstringXML */

/*
 * Create an start of a paragraph
 * Only works on paragraph level one, because Word doesn't allow paragraphs
 * in paragraphs. Other paragraph levels result from DocBooks special needs.
 */
void
vStartOfParagraphXML(diagram_type *pDiag, UINT uiMaxLevel)
{
	fail(pDiag == NULL);

	if (uiParagraphLevel >= uiMaxLevel || bTitleOpen) {
		/* In Word a title is just a paragraph */
		return;
	}
	if (uiListLevel != 0 && bEmptyListLevel) {
		/* No paragraphs in a list before the first listitem */
		return;
	}
	if (usHeaderLevelCurrent == 0) {
		/* No paragraphs without an open header */
		vAddStartTag(pDiag, TAG_CHAPTER, NULL);
		/* Dummy title */
		vAddCombinedTag(pDiag, TAG_TITLE, NULL);
	}
	vAddStartTag(pDiag, TAG_PARA, NULL);
} /* end of vStartOfParagraphXML */

/*
 * Create an end of a paragraph
 * Only for paragraph level one and for titles
 */
void
vEndOfParagraphXML(diagram_type *pDiag, UINT uiMaxLevel)
{
	UCHAR	ucTopTag;

	fail(pDiag == NULL);

	if (uiParagraphLevel > uiMaxLevel) {
		DBG_DEC(uiParagraphLevel);
		return;
	}

	for(;;) {
		ucTopTag = ucReadStack();
		switch (ucTopTag) {
		case TAG_EMPHASIS:
			fail(!bEmphasisOpen);
			vAddEndTag(pDiag, TAG_EMPHASIS);
			break;
		case TAG_SUPERSCRIPT:
			fail(!bSuperscriptOpen);
			vAddEndTag(pDiag, TAG_SUPERSCRIPT);
			break;
		case TAG_SUBSCRIPT:
			fail(!bSubscriptOpen);
			vAddEndTag(pDiag, TAG_SUBSCRIPT);
			break;
		case TAG_TITLE:
			fail(!bTitleOpen);
			vAddEndTag(pDiag, TAG_TITLE);
			return;
		case TAG_PARA:
			fail(uiParagraphLevel == 0);
			vAddEndTag(pDiag, TAG_PARA);
			return;
		case TAG_TBODY:
		case TAG_TGROUP:
		case TAG_INFORMALTABLE:
			fail(!bTableOpen);
			vAddEndTag(pDiag, ucTopTag);
			break;
		case TAG_NOTAG:
			DBG_FIXME();
			werr(1, "Impossible tag sequence, unable to continue");
			break;
		default:
			DBG_DEC(ucTopTag);
			DBG_MSG_C((size_t)ucTopTag < elementsof(atDocBookTags),
				atDocBookTags[(UINT)ucTopTag].szTagname);
			return;
		}
	}
} /* end of vEndOfParagraphXML */

/*
 * Create an end of a page
 */
void
vEndOfPageXML(diagram_type *pDiag)
{
	if (bTableOpen || usHeaderLevelCurrent == 0) {
		/* No beginpage in a table or outside a chapter */
		return;
	}
	if (bTitleOpen) {
		/* A beginpage is not allowed when in a title */
		/* So start a new paragraph */
		vEndOfParagraphXML(pDiag, UINT_MAX);
		vStartOfParagraphXML(pDiag, UINT_MAX);
		return;
	}
	vAddCombinedTag(pDiag, TAG_BEGINPAGE, NULL);
} /* end of vEndOfPageXML */

/*
 * vCloseHeaderLevels - close the specified header levels
 */
static void
vCloseHeaderLevels(diagram_type *pDiag, USHORT usIstd)
{
	BOOL	bNotReady;
	UCHAR	ucTopTag;

	DBG_MSG("vCloseHeaderLevels");
	DBG_DEC(usIstd);
	DBG_DEC(usHeaderLevelCurrent);

	vStackTrace();

	bNotReady = TRUE;
	do {
		ucTopTag = ucReadStack();
		switch (ucTopTag) {
		case TAG_TITLE:
		case TAG_PARA:
			vAddEndTag(pDiag, ucTopTag);
			break;
		default:
			bNotReady = FALSE;
			break;
		}
	} while (bNotReady);

	vStackTrace();

	while (usHeaderLevelCurrent >= usIstd) {
		if (bEmptyHeaderLevel) {
			vAddCombinedTag(pDiag, TAG_PARA, NULL);
			bEmptyHeaderLevel = FALSE;
		}
		switch (usHeaderLevelCurrent) {
		case 1: vAddEndTag(pDiag, TAG_CHAPTER); break;
		case 2: vAddEndTag(pDiag, TAG_SECT1); break;
		case 3: vAddEndTag(pDiag, TAG_SECT2); break;
		case 4: vAddEndTag(pDiag, TAG_SECT3); break;
		case 5: vAddEndTag(pDiag, TAG_SECT4); break;
		case 6: vAddEndTag(pDiag, TAG_SECT5); break;
		default:
			DBG_DEC(usHeaderLevelCurrent);
			DBG_FIXME();
			return;
		}
	}

	DBG_DEC(usHeaderLevelCurrent);

	vStackTrace();
} /* end of vCloseHeaderLevels */

/*
 * vSetHeadersXML - set the headers
 */
void
vSetHeadersXML(diagram_type *pDiag, USHORT usIstd)
{
	fail(pDiag == NULL);

	if (usIstd == 0 || usIstd > 6) {
		DBG_DEC_C(usIstd != 0 && usIstd <= 9, usIstd);
		return;
	}
	DBG_DEC(usIstd);

	if (bTableOpen || uiListLevel != 0) {
		/* No headers when you're in a table or in a list */
		return;
	}

	/* Close levels */
	vCloseHeaderLevels(pDiag, usIstd);

	DBG_DEC(usHeaderLevelCurrent);

	/* Open levels */
	while (usHeaderLevelCurrent < usIstd) {
		switch (usHeaderLevelCurrent) {
		case 0: vAddStartTag(pDiag, TAG_CHAPTER, NULL); break;
		case 1: vAddStartTag(pDiag, TAG_SECT1, NULL); break;
		case 2: vAddStartTag(pDiag, TAG_SECT2, NULL); break;
		case 3: vAddStartTag(pDiag, TAG_SECT3, NULL); break;
		case 4: vAddStartTag(pDiag, TAG_SECT4, NULL); break;
		case 5: vAddStartTag(pDiag, TAG_SECT5, NULL); break;
		default:
			DBG_DEC(usHeaderLevelCurrent);
			DBG_FIXME();
			return;
		}
		fail(usIstd == 0);
		/* The next paragraph should be a title */
		if (usHeaderLevelCurrent < usIstd) {
			/* This chapter level is not in the Word document */
			vAddCombinedTag(pDiag, TAG_TITLE, NULL);
		} else {
			vAddStartTag(pDiag, TAG_TITLE, NULL);
		}
	}
} /* end of vSetHeadersXML */

/*
 * Create a start of a list
 */
void
vStartOfListXML(diagram_type *pDiag, UCHAR ucNFC, BOOL bIsEndOfTable)
{
	const char	*szAttr;
	UCHAR		ucTag;

	fail(pDiag == NULL);

	if (bIsEndOfTable) {
		/* FIXME: until a list in a table is allowed */
		vEndOfTableXML(pDiag);
	}

	if (bTableOpen) {
		/* FIXME: a list in a table should be allowed */
		return;
	}

	if (usHeaderLevelCurrent == 0) {
		/* No list without an open header */
		vAddStartTag(pDiag, TAG_CHAPTER, NULL);
		/* Dummy title */
		vAddCombinedTag(pDiag, TAG_TITLE, NULL);
	}

	switch (ucNFC) {
	case LIST_ARABIC_NUM:
	case LIST_ORDINAL_NUM:
	case LIST_NUMBER_TXT:
	case LIST_ORDINAL_TXT:
	case LIST_OUTLINE_NUM:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='arabic'";
		break;
	case LIST_UPPER_ROMAN:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='upperroman'";
		break;
	case LIST_LOWER_ROMAN:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='lowerroman'";
		break;
	case LIST_UPPER_ALPHA:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='upperalpha'";
		break;
	case LIST_LOWER_ALPHA:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='loweralpha'";
		break;
	case LIST_SPECIAL:
	case LIST_SPECIAL2:
	case LIST_BULLETS:
		ucTag = TAG_ITEMIZEDLIST;
		szAttr = "mark='bullet'";
		break;
	default:
		ucTag = TAG_ORDEREDLIST;
		szAttr = "numeration='arabic'";
		DBG_HEX(ucNFC);
		DBG_FIXME();
		break;
	}
	vAddStartTag(pDiag, ucTag, szAttr);
} /* end of vStartOfListXML */

/*
 * Create an end of a list
 */
void
vEndOfListXML(diagram_type *pDiag)
{
	fail(pDiag == NULL);

	if (bTableOpen) {
		/* FIXME: a list in a table should be allowed */
		return;
	}

	if (uiListLevel != 0) {
		vStackTrace();
		vAddEndTagsUntil2(pDiag, TAG_ITEMIZEDLIST, TAG_ORDEREDLIST);
		vStackTrace();
	}
} /* end of vEndOfListXML */

/*
 * Create a start of a list item
 */
void
vStartOfListItemXML(diagram_type *pDiag, BOOL bNoMarks)
{
	const char	*szAttr;
	UCHAR	ucTopTag;

	fail(pDiag == NULL);

	if (bTableOpen) {
		/* FIXME: a list in a table should be allowed */
		return;
	}

	ucTopTag = ucReadStack();
	if (ucTopTag != TAG_ITEMIZEDLIST && ucTopTag != TAG_ORDEREDLIST) {
		/* Must end a previous list item first */
		vAddEndTagsUntil1(pDiag, TAG_LISTITEM);
	}

	DBG_DEC_C(ucReadStack() != TAG_ITEMIZEDLIST &&
		ucReadStack() != TAG_ORDEREDLIST, ucReadStack());

	/* Start a new list item */
	szAttr = bNoMarks ? "override='none'" : NULL;
	vAddStartTag(pDiag, TAG_LISTITEM, szAttr);
	/* Start a new paragraph (independant of level) */
	vAddStartTag(pDiag, TAG_PARA, NULL);
} /* end of vStartOfListItemXML */

/*
 * Create a start of a table
 */
static void
vStartOfTable(diagram_type *pDiag, UCHAR ucBorderInfo)
{
	const char	*szFrame;
	BOOL	bNotReady;
	UCHAR	ucTopTag;
	char	cColSep, cRowSep;
	char	szAttr[40];

	fail(pDiag == NULL);

	/* Close elements that cannot contain a table */
	bNotReady = TRUE;
	do {
		ucTopTag = ucReadStack();
		switch (ucTopTag) {
		case TAG_TITLE:
			fail(!bTitleOpen);
			vAddEndTag(pDiag, TAG_TITLE);
			break;
		case TAG_EMPHASIS:
			fail(!bEmphasisOpen);
			vAddEndTag(pDiag, TAG_EMPHASIS);
			break;
		case TAG_SUPERSCRIPT:
			fail(!bSuperscriptOpen);
			vAddEndTag(pDiag, TAG_SUPERSCRIPT);
			break;
		case TAG_SUBSCRIPT:
			fail(!bSubscriptOpen);
			vAddEndTag(pDiag, TAG_SUBSCRIPT);
			break;
		default:
			bNotReady = FALSE;
			break;
		}
	} while (bNotReady);

	/* Create table attributes */
	switch (ucBorderInfo) {
	case TABLE_BORDER_TOP:
		szFrame = "top";
		break;
	case TABLE_BORDER_LEFT|TABLE_BORDER_RIGHT:
		szFrame = "sides";
		break;
	case TABLE_BORDER_TOP|TABLE_BORDER_BOTTOM:
		szFrame = "topbot";
		break;
	case TABLE_BORDER_BOTTOM:
		szFrame = "bottom";
		break;
	case TABLE_BORDER_TOP|TABLE_BORDER_LEFT|
	     TABLE_BORDER_BOTTOM|TABLE_BORDER_RIGHT:
		szFrame = "all";
		break;
	default:
		szFrame = "none";
		break;
	}
	cColSep = bIsTableBorderLeft(ucBorderInfo) ||
		  bIsTableBorderRight(ucBorderInfo) ? '1' : '0';
	cRowSep = bIsTableBorderTop(ucBorderInfo) ||
		  bIsTableBorderBottom(ucBorderInfo) ? '1' : '0';

	sprintf(szAttr, "frame='%.6s' colsep='%c' rowsep='%c'",
			szFrame, cColSep, cRowSep);

	if (usHeaderLevelCurrent == 0) {
		/* No table without an open header */
		vAddStartTag(pDiag, TAG_CHAPTER, NULL);
		/* Dummy title */
		vAddCombinedTag(pDiag, TAG_TITLE, NULL);
	}
	vAddStartTag(pDiag, TAG_INFORMALTABLE, szAttr);
} /* end of vStartOfTable */

/*
 * Create a start of a table group
 */
static void
vStartOfTableGroup(diagram_type *pDiag,
	int iNbrOfColumns, const short *asColumnWidth)
{
	double	dWidth;
	int	iIndex;
	char	szCols[6 + 3 * sizeof(int) + 1 + 1];
	char	szColWidth[10 + 3 * sizeof(short) + 3 + 3 + 1];

	fail(iNbrOfColumns < 1);
	fail(asColumnWidth == NULL);

	sprintf(szCols, "cols='%d'", iNbrOfColumns);
	vAddStartTag(pDiag, TAG_TGROUP, szCols);

	for (iIndex= 0; iIndex < iNbrOfColumns; iIndex++) {
		fail(asColumnWidth[iIndex] < 0);
		dWidth = dTwips2Points(asColumnWidth[iIndex]);
		if (dWidth <= 1.0) {
			strcpy(szColWidth, "colwidth='1.00pt'");
		} else {
			sprintf(szColWidth, "colwidth='%.2fpt'", dWidth);
		}
		vAddCombinedTag(pDiag, TAG_COLSPEC, szColWidth);
	}
} /* end of vStartOfTableGroup */

/*
 * Create an end of a table
 */
void
vEndOfTableXML(diagram_type *pDiag)
{
	fail(pDiag == NULL);

	if (bTableOpen) {
		vAddEndTag(pDiag, TAG_TBODY);
		vAddEndTag(pDiag, TAG_TGROUP);
		vAddEndTag(pDiag, TAG_INFORMALTABLE);
	}
} /* end of vEndOfTableXML */

/*
 * Add a table row
 */
void
vAddTableRowXML(diagram_type *pDiag, char **aszColTxt,
	int iNbrOfColumns, const short *asColumnWidth, UCHAR ucBorderInfo)
{
	size_t	tCount, tStringLength;
	int	iIndex;

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);
	fail(aszColTxt == NULL);
	fail(iNbrOfColumns < 1);
	fail(asColumnWidth == NULL);

	if (iNbrOfColumns != iTableColumnsCurrent) {
		/* A new number of columns */
		/* End the old table body and table group (if they exist) */
		vAddEndTagOptional(pDiag, TAG_TBODY);
		vAddEndTagOptional(pDiag, TAG_TGROUP);
		if (!bTableOpen) {
			/* No table yet. Start a new table */
			vStartOfTable(pDiag, ucBorderInfo);
		}
		/* Start a new table group and a new table body */
		vStartOfTableGroup(pDiag, iNbrOfColumns, asColumnWidth);
		vAddStartTag(pDiag, TAG_TBODY, NULL);
		iTableColumnsCurrent = iNbrOfColumns;
	}

	/* Add the table row */
	vAddStartTag(pDiag, TAG_ROW, NULL);
	for (iIndex = 0; iIndex < iNbrOfColumns; iIndex++) {
		/* Add a table cell */
		fail(aszColTxt[iIndex] == NULL);
		vAddStartTag(pDiag, TAG_ENTRY, NULL);
		tStringLength = strlen(aszColTxt[iIndex]);
		for (tCount = 0; tCount < tStringLength; tCount++) {
			vPrintChar(pDiag, aszColTxt[iIndex][tCount]);
		}
		vAddEndTag(pDiag, TAG_ENTRY);
	}
	vAddEndTag(pDiag, TAG_ROW);
} /* end of vAddTableRowXML */
