/*
 * options.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Read and write the options
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#if defined(__riscos)
#include "DeskLib:Error.h"
#include "DeskLib:Wimp.h"
#else
#include <stdlib.h>
#if defined(__dos) || defined(N_PLAT_NLM) || defined(_WIN32)
extern int getopt(int, char **, const char *);
#else
#include <unistd.h>
#endif /* __dos */
#endif /* __riscos */
#include "antiword.h"

#if defined(__riscos)
#define PARAGRAPH_BREAK		"set paragraph_break=%d"
#define AUTOFILETYPE		"set autofiletype_allowed=%d"
#define USE_OUTLINEFONTS	"set use_outlinefonts=%d"
#define SHOW_IMAGES		"set show_images=%d"
#define HIDE_HIDDEN_TEXT	"set hide_hidden_text=%d"
#define SCALE_FACTOR_START	"set scale_factor_start=%d"
#else
#define LEAFNAME_SIZE		(32+1)
#endif /* __riscos */

#if defined(__riscos)
/* Temporary values for options */
static options_type	tOptionsTemp;
#else
typedef struct papersize_tag {
	char	szName[16];	/* Papersize name */
	USHORT	usWidth;	/* In points */
	USHORT	usHeight;	/* In points */
} papersize_type;

static const papersize_type atPaperSizes[] = {
	{	"10x14",	 720,	1008	},
	{	"a3",		 842,	1191	},
	{	"a4",		 595,	 842	},
	{	"a5",		 420,	 595	},
	{	"b4",		 729,	1032	},
	{	"b5",		 516,	 729	},
	{	"executive",	 540,	 720	},
	{	"folio",	 612,	 936	},
	{	"legal",	 612,	1008	},
	{	"letter",	 612,	 792	},
	{	"note",		 540,	 720	},
	{	"quarto",	 610,	 780	},
	{	"statement",	 396,	 612	},
	{	"tabloid",	 792,	1224	},
	{	"",		   0,	   0	},
};
#endif /* __riscos */
/* Default values for options */
static const options_type	tOptionsDefault = {
	DEFAULT_SCREEN_WIDTH,
#if defined(__riscos)
	conversion_draw,
#else
	conversion_text,
#endif /* __riscos */
	TRUE,
	TRUE,
	FALSE,
	encoding_latin_1,
	INT_MAX,
	INT_MAX,
	level_default,
#if defined(__riscos)
	TRUE,
	DEFAULT_SCALE_FACTOR,
#endif /* __riscos */
};

/* Current values for options */
static options_type	tOptionsCurr;

#if !defined(__riscos)
/*
 * bCorrectPapersize - see if the papersize is correct
 *
 * TRUE if the papersize is correct, otherwise FALSE
 */
static BOOL
bCorrectPapersize(const char *szName, conversion_type eConversionType)
{
	const papersize_type	*pPaperSize;

	for (pPaperSize = atPaperSizes;
	     pPaperSize->szName[0] != '\0';
	     pPaperSize++) {
		if (!STRCEQ(pPaperSize->szName,  szName)) {
			continue;
		}
		DBG_DEC(pPaperSize->usWidth);
		DBG_DEC(pPaperSize->usHeight);
		tOptionsCurr.eConversionType = eConversionType;
		tOptionsCurr.iPageHeight = (int)pPaperSize->usHeight;
		tOptionsCurr.iPageWidth = (int)pPaperSize->usWidth;
		return TRUE;
	}
	return FALSE;
} /* end of bCorrectPapersize */

/*
 * szCreateSuffix - create a suffix for the file
 *
 * Returns the suffix
 */
static const char *
szCreateSuffix(const char *szLeafname)
{
	const char	*pcDot;

	pcDot = strrchr(szLeafname, '.');
	if (pcDot != NULL && STRCEQ(pcDot, ".txt")) {
		/* There is already a .txt suffix, no need for another one */
		return "";
	}
	return ".txt";
} /* end of szCreateSuffix */

/*
 * eMappingFile2Encoding - convert the mapping file to an encoding
 */
static encoding_type
eMappingFile2Encoding(const char *szLeafname)
{
	char	szMappingFile[LEAFNAME_SIZE+4];

	fail(szLeafname == NULL);

	if (strlen(szLeafname) + 4 >= sizeof(szMappingFile)) {
		DBG_MSG(szLeafname);
		return encoding_latin_1;
	}

	sprintf(szMappingFile, "%s%s", szLeafname, szCreateSuffix(szLeafname));

	DBG_MSG(szMappingFile);

	if (STRCEQ(szMappingFile, MAPPING_FILE_UTF_8)) {
		return encoding_utf_8;
	}
	if (STRCEQ(szMappingFile, MAPPING_FILE_CP852) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_CP1250) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_8859_2)) {
		return encoding_latin_2;
	}
	if (STRCEQ(szMappingFile, MAPPING_FILE_KOI8_R) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_KOI8_U) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_CP866) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_CP1251) ||
	    STRCEQ(szMappingFile, MAPPING_FILE_8859_5)) {
		return encoding_cyrillic;
	}
	return encoding_latin_1;
} /* end of eMappingFile2Encoding */
#endif /* !__riscos */

/*
 * pOpenCharacterMappingFile - open the mapping file
 *
 * Returns the file pointer or NULL
 */
static FILE *
pOpenCharacterMappingFile(const char *szLeafname)
{
#if !defined(__riscos)
	FILE	*pFile;
	const char	*szHome, *szAntiword, *szSuffix;
	size_t	tFilenameLen;
	char	szMappingFile[PATH_MAX+1];
#endif /* !__riscos */

	if (szLeafname == NULL || szLeafname[0] == '\0') {
		return NULL;
	}

	DBG_MSG(szLeafname);

#if defined(__riscos)
	return fopen(szLeafname, "r");
#else
	/* Set the suffix */
	szSuffix = szCreateSuffix(szLeafname);

	/* Set length */
	tFilenameLen = strlen(szLeafname) + strlen(szSuffix);

	/* Try the environment version of the mapping file */
	szAntiword = szGetAntiwordDirectory();
	if (szAntiword != NULL && szAntiword[0] != '\0') {
	    if (strlen(szAntiword) + tFilenameLen <
		sizeof(szMappingFile) -
		sizeof(FILE_SEPARATOR)) {
			sprintf(szMappingFile,
				"%s" FILE_SEPARATOR "%s%s",
				szAntiword, szLeafname, szSuffix);
			DBG_MSG(szMappingFile);
			pFile = fopen(szMappingFile, "r");
			if (pFile != NULL) {
				return pFile;
			}
		} else {
			werr(0, "Environment mappingfilename ignored");
		}
	}

	/* Try the local version of the mapping file */
	szHome = szGetHomeDirectory();
	if (strlen(szHome) + tFilenameLen <
	    sizeof(szMappingFile) -
	    sizeof(ANTIWORD_DIR) -
	    2 * sizeof(FILE_SEPARATOR)) {
		sprintf(szMappingFile,
			"%s" FILE_SEPARATOR ANTIWORD_DIR FILE_SEPARATOR "%s%s",
			szHome, szLeafname, szSuffix);
		DBG_MSG(szMappingFile);
		pFile = fopen(szMappingFile, "r");
		if (pFile != NULL) {
			return pFile;
		}
	} else {
		werr(0, "Local mappingfilename too long, ignored");
	}

	/* Try the global version of the mapping file */
	if (tFilenameLen <
	    sizeof(szMappingFile) -
	    sizeof(GLOBAL_ANTIWORD_DIR) -
	    sizeof(FILE_SEPARATOR)) {
		sprintf(szMappingFile,
			GLOBAL_ANTIWORD_DIR FILE_SEPARATOR "%s%s",
			szLeafname, szSuffix);
		DBG_MSG(szMappingFile);
		pFile = fopen(szMappingFile, "r");
		if (pFile != NULL) {
			return pFile;
		}
	} else {
		werr(0, "Global mappingfilename too long, ignored");
	}
	werr(0, "I can't open your mapping file (%s%s)\n"
		"It is not in '%s" FILE_SEPARATOR ANTIWORD_DIR "' nor in '"
		GLOBAL_ANTIWORD_DIR "'.", szLeafname, szSuffix, szHome);
	return NULL;
#endif /* __riscos */
} /* end of pOpenCharacterMappingFile */

/*
 * vCloseCharacterMappingFile - close the mapping file
 */
static void
vCloseCharacterMappingFile(FILE *pFile)
{
	(void)fclose(pFile);
} /* end of pCloseCharacterMappingFile */

#if CR3_ANTIWORD_PATCH!=1
/*
 * iReadOptions - read options
 *
 * returns:	-1: error
 *		 0: help
 *		>0: index first file argument
 */
int
iReadOptions(int argc, char **argv)
{
#if defined(__riscos)
	FILE	*pFile;
	const char	*szAlphabet;
	int	iAlphabet;
	char	szLine[81];
#else
	extern	char	*optarg;
	extern int	optind;
	char	*pcChar, *szTmp;
	int	iChar;
	char	szLeafname[LEAFNAME_SIZE];
#endif /* __riscos */
	FILE	*pCharacterMappingFile;
	int	iTmp;
	BOOL	bSuccess;

	DBG_MSG("iReadOptions");

/* Defaults */
	tOptionsCurr = tOptionsDefault;

#if defined(__riscos)
/* Choices file */
	pFile = fopen("<AntiWord$ChoicesFile>", "r");
	DBG_MSG_C(pFile == NULL, "Choices file not found");
	DBG_HEX_C(pFile != NULL, pFile);
	if (pFile != NULL) {
		while (fgets(szLine, (int)sizeof(szLine), pFile) != NULL) {
			DBG_MSG(szLine);
			if (szLine[0] == '#' ||
			    szLine[0] == '\r' ||
			    szLine[0] == '\n') {
				continue;
			}
			if (sscanf(szLine, PARAGRAPH_BREAK, &iTmp) == 1 &&
			    (iTmp == 0 ||
			    (iTmp >= MIN_SCREEN_WIDTH &&
			     iTmp <= MAX_SCREEN_WIDTH))) {
				tOptionsCurr.iParagraphBreak = iTmp;
				DBG_DEC(tOptionsCurr.iParagraphBreak);
			} else if (sscanf(szLine, AUTOFILETYPE, &iTmp)
								== 1) {
				tOptionsCurr.bAutofiletypeAllowed =
								iTmp != 0;
				DBG_DEC(tOptionsCurr.bAutofiletypeAllowed);
			} else if (sscanf(szLine, USE_OUTLINEFONTS, &iTmp)
								== 1) {
				tOptionsCurr.eConversionType =
					iTmp == 0 ?
					conversion_text : conversion_draw;
				DBG_DEC(tOptionsCurr.eConversionType);
			} else if (sscanf(szLine, SHOW_IMAGES, &iTmp)
								== 1) {
				tOptionsCurr.eImageLevel = iTmp != 0 ?
					level_default : level_no_images;
			} else if (sscanf(szLine, HIDE_HIDDEN_TEXT, &iTmp)
								== 1) {
				tOptionsCurr.bHideHiddenText = iTmp != 0;
				DBG_DEC(tOptionsCurr.bHideHiddenText);
			} else if (sscanf(szLine, SCALE_FACTOR_START, &iTmp)
								== 1) {
				if (iTmp >= MIN_SCALE_FACTOR &&
				    iTmp <= MAX_SCALE_FACTOR) {
					tOptionsCurr.iScaleFactor = iTmp;
					DBG_DEC(tOptionsCurr.iScaleFactor);
				}
			}
		}
		(void)fclose(pFile);
	}
	iAlphabet = iReadCurrentAlphabetNumber();
	switch (iAlphabet) {
	case 101:	/* ISO-8859-1 aka Latin1 */
		szAlphabet = "<AntiWord$Latin1>";
		break;
	case 112:	/* ISO-8859-15 aka Latin9 */
		szAlphabet = "<AntiWord$Latin9>";
		break;
	default:
		werr(0, "Alphabet '%d' is not supported", iAlphabet);
		return -1;
	}
	pCharacterMappingFile = pOpenCharacterMappingFile(szAlphabet);
	if (pCharacterMappingFile != NULL) {
		bSuccess = bReadCharacterMappingTable(pCharacterMappingFile);
		vCloseCharacterMappingFile(pCharacterMappingFile);
	} else {
		bSuccess = FALSE;
	}
	return bSuccess ? 1 : -1;
#else
/* Environment */
	szTmp = getenv("COLUMNS");
	if (szTmp != NULL) {
		DBG_MSG(szTmp);
		iTmp = (int)strtol(szTmp, &pcChar, 10);
		if (*pcChar == '\0') {
			iTmp -= 4;	/* This is for the edge */
			if (iTmp < MIN_SCREEN_WIDTH) {
				iTmp = MIN_SCREEN_WIDTH;
			} else if (iTmp > MAX_SCREEN_WIDTH) {
				iTmp = MAX_SCREEN_WIDTH;
			}
			tOptionsCurr.iParagraphBreak = iTmp;
			DBG_DEC(tOptionsCurr.iParagraphBreak);
		}
	}
	strncpy(szLeafname, szGetDefaultMappingFile(), sizeof(szLeafname) - 1);
	szLeafname[sizeof(szLeafname) - 1] = '\0';
/* Command line */
	while ((iChar = getopt(argc, argv, "La:fhi:m:p:rstw:x:")) != -1) {
		switch (iChar) {
		case 'L':
			tOptionsCurr.bUseLandscape = TRUE;
			break;
		case 'a':
			if (!bCorrectPapersize(optarg, conversion_pdf)) {
				werr(0, "-a without a valid papersize");
				return -1;
			}
			break;
		case 'f':
			tOptionsCurr.eConversionType = conversion_fmt_text;
			break;
		case 'h':
			return 0;
		case 'i':
			iTmp = (int)strtol(optarg, &pcChar, 10);
			if (*pcChar != '\0') {
				break;
			}
			switch (iTmp) {
			case 0:
				tOptionsCurr.eImageLevel = level_gs_special;
				break;
			case 1:
				tOptionsCurr.eImageLevel = level_no_images;
				break;
			case 2:
				tOptionsCurr.eImageLevel = level_ps_2;
				break;
			case 3:
				tOptionsCurr.eImageLevel = level_ps_3;
				break;
			default:
				tOptionsCurr.eImageLevel = level_default;
				break;
			}
			DBG_DEC(tOptionsCurr.eImageLevel);
			break;
		case 'm':
			if (tOptionsCurr.eConversionType == conversion_xml) {
				werr(0, "XML doesn't need a mapping file");
				break;
			}
			strncpy(szLeafname, optarg, sizeof(szLeafname) - 1);
			szLeafname[sizeof(szLeafname) - 1] = '\0';
			DBG_MSG(szLeafname);
			break;
		case 'p':
			if (!bCorrectPapersize(optarg, conversion_ps)) {
				werr(0, "-p without a valid papersize");
				return -1;
			}
			break;
		case 'r':
			tOptionsCurr.bRemoveRemovedText = FALSE;
			break;
		case 's':
			tOptionsCurr.bHideHiddenText = FALSE;
			break;
		case 't':
			tOptionsCurr.eConversionType = conversion_text;
			break;
		case 'w':
			iTmp = (int)strtol(optarg, &pcChar, 10);
			if (*pcChar == '\0') {
				if (iTmp != 0 && iTmp < MIN_SCREEN_WIDTH) {
					iTmp = MIN_SCREEN_WIDTH;
				} else if (iTmp > MAX_SCREEN_WIDTH) {
					iTmp = MAX_SCREEN_WIDTH;
				}
				tOptionsCurr.iParagraphBreak = iTmp;
				DBG_DEC(tOptionsCurr.iParagraphBreak);
			}
			break;
		case 'x':
			if (STREQ(optarg, "db")) {
				tOptionsCurr.iParagraphBreak = 0;
				tOptionsCurr.eConversionType = conversion_xml;
				strcpy(szLeafname, MAPPING_FILE_UTF_8);
			} else {
				werr(0, "-x %s is not supported", optarg);
				return -1;
			}
			break;
		default:
			return -1;
		}
	}

	tOptionsCurr.eEncoding = eMappingFile2Encoding(szLeafname);
	DBG_DEC(tOptionsCurr.eEncoding);

	if (tOptionsCurr.eConversionType == conversion_ps &&
	    tOptionsCurr.eEncoding == encoding_utf_8) {
		werr(0,
		"The combination PostScript and UTF-8 is not supported");
		return -1;
	}

	if (tOptionsCurr.eConversionType == conversion_pdf &&
	    tOptionsCurr.eEncoding == encoding_utf_8) {
		werr(0,
		"The combination PDF and UTF-8 is not supported");
		return -1;
	}

	if (tOptionsCurr.eConversionType == conversion_pdf &&
	    tOptionsCurr.eEncoding == encoding_cyrillic) {
		werr(0,
		"The combination PDF and Cyrillic is not supported");
		return -1;
	}

	if (tOptionsCurr.eConversionType == conversion_ps ||
	    tOptionsCurr.eConversionType == conversion_pdf) {
		/* PostScript or PDF mode */
		if (tOptionsCurr.bUseLandscape) {
			/* Swap the page height and width */
			iTmp = tOptionsCurr.iPageHeight;
			tOptionsCurr.iPageHeight = tOptionsCurr.iPageWidth;
			tOptionsCurr.iPageWidth = iTmp;
		}
		/* The paragraph break depends on the width of the paper */
		tOptionsCurr.iParagraphBreak = iMilliPoints2Char(
			(long)tOptionsCurr.iPageWidth * 1000 -
			lDrawUnits2MilliPoints(
				PS_LEFT_MARGIN + PS_RIGHT_MARGIN));
		DBG_DEC(tOptionsCurr.iParagraphBreak);
	}

	pCharacterMappingFile = pOpenCharacterMappingFile(szLeafname);
	if (pCharacterMappingFile != NULL) {
		bSuccess = bReadCharacterMappingTable(pCharacterMappingFile);
		vCloseCharacterMappingFile(pCharacterMappingFile);
	} else {
		bSuccess = FALSE;
	}
	return bSuccess ? optind : -1;
#endif /* __riscos */
} /* end of iReadOptions */
#endif

/*
 * vGetOptions - get a copy of the current option values
 */
void
vGetOptions(options_type *pOptions)
{
	fail(pOptions == NULL);

	*pOptions = tOptionsCurr;
} /* end of vGetOptions */

/*
 * vSetOptions - set new current option values
 */
void
vSetOptions(options_type *pOptions)
{
    fail(pOptions == NULL);

    tOptionsCurr = *pOptions;
} /* end of vSetOptions */

#if defined(__riscos)
/*
 * vWriteOptions - write the current options to the Options file
 */
static void
vWriteOptions(void)
{
	FILE	*pFile;
	char	*szOptionsFile;

	TRACE_MSG("vWriteOptions");

	szOptionsFile = getenv("AntiWord$ChoicesSave");
	if (szOptionsFile == NULL) {
		werr(0, "Warning: Name of the Choices file not found");
		return;
	}
	if (!bMakeDirectory(szOptionsFile)) {
		werr(0,
		"Warning: I can't make a directory for the Choices file");
		return;
	}
	pFile = fopen(szOptionsFile, "w");
	if (pFile == NULL) {
		werr(0, "Warning: I can't write the Choices file");
		return;
	}
	(void)fprintf(pFile, PARAGRAPH_BREAK"\n",
		tOptionsCurr.iParagraphBreak);
	(void)fprintf(pFile, AUTOFILETYPE"\n",
		tOptionsCurr.bAutofiletypeAllowed);
	(void)fprintf(pFile, USE_OUTLINEFONTS"\n",
		tOptionsCurr.eConversionType == conversion_text ? 0 : 1);
	(void)fprintf(pFile, SHOW_IMAGES"\n",
		tOptionsCurr.eImageLevel == level_no_images ? 0 : 1);
	(void)fprintf(pFile, HIDE_HIDDEN_TEXT"\n",
		tOptionsCurr.bHideHiddenText);
	(void)fprintf(pFile, SCALE_FACTOR_START"\n",
		tOptionsCurr.iScaleFactor);
	(void)fclose(pFile);
} /* end of vWriteOptions */

/*
 * vChoicesOpenAction - action to be taken when the Choices window opens
 */
void
vChoicesOpenAction(window_handle tWindow)
{
	TRACE_MSG("vChoicesOpenAction");

	tOptionsTemp = tOptionsCurr;
	if (tOptionsTemp.iParagraphBreak == 0) {
		vUpdateRadioButton(tWindow, CHOICES_BREAK_BUTTON, FALSE);
		vUpdateRadioButton(tWindow, CHOICES_NO_BREAK_BUTTON, TRUE);
		vUpdateWriteableNumber(tWindow, CHOICES_BREAK_WRITEABLE,
					DEFAULT_SCREEN_WIDTH);
	} else {
		vUpdateRadioButton(tWindow, CHOICES_BREAK_BUTTON, TRUE);
		vUpdateRadioButton(tWindow, CHOICES_NO_BREAK_BUTTON, FALSE);
		vUpdateWriteableNumber(tWindow,
			CHOICES_BREAK_WRITEABLE,
			tOptionsTemp.iParagraphBreak);
	}
	vUpdateRadioButton(tWindow, CHOICES_AUTOFILETYPE_BUTTON,
					tOptionsTemp.bAutofiletypeAllowed);
	vUpdateRadioButton(tWindow, CHOICES_HIDDEN_TEXT_BUTTON,
					tOptionsTemp.bHideHiddenText);
	if (tOptionsTemp.eConversionType == conversion_draw) {
		vUpdateRadioButton(tWindow,
			CHOICES_WITH_IMAGES_BUTTON,
			tOptionsTemp.eImageLevel != level_no_images);
		vUpdateRadioButton(tWindow,
			CHOICES_NO_IMAGES_BUTTON,
			tOptionsTemp.eImageLevel == level_no_images);
		vUpdateRadioButton(tWindow,
			CHOICES_TEXTONLY_BUTTON, FALSE);
	} else {
		vUpdateRadioButton(tWindow,
			CHOICES_WITH_IMAGES_BUTTON, FALSE);
		vUpdateRadioButton(tWindow,
			CHOICES_NO_IMAGES_BUTTON, FALSE);
		vUpdateRadioButton(tWindow,
			CHOICES_TEXTONLY_BUTTON, TRUE);
	}
	vUpdateWriteableNumber(tWindow,
		CHOICES_SCALE_WRITEABLE, tOptionsTemp.iScaleFactor);
	TRACE_MSG("end of vChoicesOpenAction");
} /* end of vChoicesOpenAction */

/*
 * vDefaultButtonAction - action when the default button is clicked
 */
static void
vDefaultButtonAction(window_handle tWindow)
{
	TRACE_MSG("vDefaultButtonAction");

	tOptionsTemp = tOptionsDefault;
	vUpdateRadioButton(tWindow, CHOICES_BREAK_BUTTON, TRUE);
	vUpdateRadioButton(tWindow, CHOICES_NO_BREAK_BUTTON, FALSE);
	vUpdateWriteableNumber(tWindow, CHOICES_BREAK_WRITEABLE,
			tOptionsTemp.iParagraphBreak);
	vUpdateRadioButton(tWindow, CHOICES_AUTOFILETYPE_BUTTON,
			tOptionsTemp.bAutofiletypeAllowed);
	vUpdateRadioButton(tWindow, CHOICES_HIDDEN_TEXT_BUTTON,
			tOptionsTemp.bHideHiddenText);
	vUpdateRadioButton(tWindow, CHOICES_WITH_IMAGES_BUTTON,
			tOptionsTemp.eConversionType == conversion_draw &&
			tOptionsTemp.eImageLevel != level_no_images);
	vUpdateRadioButton(tWindow, CHOICES_NO_IMAGES_BUTTON,
			tOptionsTemp.eConversionType == conversion_draw &&
			tOptionsTemp.eImageLevel == level_no_images);
	vUpdateRadioButton(tWindow, CHOICES_TEXTONLY_BUTTON,
			tOptionsTemp.eConversionType == conversion_text);
	vUpdateWriteableNumber(tWindow, CHOICES_SCALE_WRITEABLE,
			tOptionsTemp.iScaleFactor);
} /* end of vDefaultButtonAction */

/*
 * vApplyButtonAction - action to be taken when the OK button is clicked
 */
static void
vApplyButtonAction(void)
{
	TRACE_MSG("vApplyButtonAction");

	tOptionsCurr = tOptionsTemp;
} /* end of vApplyButtonAction */

/*
 * vSaveButtonAction - action to be taken when the save button is clicked
 */
static void
vSaveButtonAction(void)
{
	TRACE_MSG("vSaveButtonAction");

	vApplyButtonAction();
	vWriteOptions();
} /* end of vSaveButtonAction */

/*
 * vSetParagraphBreak - set the paragraph break to the given number
 */
static void
vSetParagraphBreak(window_handle tWindow, int iNumber)
{
	tOptionsTemp.iParagraphBreak = iNumber;
	if (tOptionsTemp.iParagraphBreak == 0) {
		return;
	}
	vUpdateWriteableNumber(tWindow,
			CHOICES_BREAK_WRITEABLE,
			tOptionsTemp.iParagraphBreak);
} /* end of vSetParagraphBreak */

/*
 * vChangeParagraphBreak - change the paragraph break with the given number
 */
static void
vChangeParagraphBreak(window_handle tWindow, int iNumber)
{
	int	iTmp;

	iTmp = tOptionsTemp.iParagraphBreak + iNumber;
	if (iTmp < MIN_SCREEN_WIDTH || iTmp > MAX_SCREEN_WIDTH) {
	  	/* Ignore */
		return;
	}
	tOptionsTemp.iParagraphBreak = iTmp;
	vUpdateWriteableNumber(tWindow,
			CHOICES_BREAK_WRITEABLE,
			tOptionsTemp.iParagraphBreak);
} /* end of vChangeParagraphBreak */

/*
 * vChangeAutofiletype - invert the permission to autofiletype
 */
static void
vChangeAutofiletype(window_handle tWindow)
{
	tOptionsTemp.bAutofiletypeAllowed =
				!tOptionsTemp.bAutofiletypeAllowed;
	vUpdateRadioButton(tWindow,
			CHOICES_AUTOFILETYPE_BUTTON,
			tOptionsTemp.bAutofiletypeAllowed);
} /* end of vChangeAutofiletype */

/*
 * vChangeHiddenText - invert the hide/show hidden text
 */
static void
vChangeHiddenText(window_handle tWindow)
{
	tOptionsTemp.bHideHiddenText = !tOptionsTemp.bHideHiddenText;
	vUpdateRadioButton(tWindow,
			CHOICES_HIDDEN_TEXT_BUTTON,
			tOptionsTemp.bHideHiddenText);
} /* end of vChangeHiddenText */

/*
 * vUseFontsImages - use outline fonts, show images
 */
static void
vUseFontsImages(BOOL bUseOutlineFonts, BOOL bShowImages)
{
	tOptionsTemp.eConversionType =
		bUseOutlineFonts ? conversion_draw : conversion_text;
	tOptionsTemp.eImageLevel =
		bUseOutlineFonts && bShowImages ?
		level_default : level_no_images;
} /* end of vUseFontsImages */

/*
 * vSetScaleFactor - set the scale factor to the given number
 */
static void
vSetScaleFactor(window_handle tWindow, int iNumber)
{
  	tOptionsTemp.iScaleFactor = iNumber;
	vUpdateWriteableNumber(tWindow,
			CHOICES_SCALE_WRITEABLE,
			tOptionsTemp.iScaleFactor);
} /* end of vSetScaleFactor */

/*
 * vChangeScaleFactor - change the scale factor with the given number
 */
static void
vChangeScaleFactor(window_handle tWindow, int iNumber)
{
	int	iTmp;

	iTmp = tOptionsTemp.iScaleFactor + iNumber;
	if (iTmp < MIN_SCALE_FACTOR || iTmp > MAX_SCALE_FACTOR) {
	  	/* Ignore */
		return;
	}
	tOptionsTemp.iScaleFactor = iTmp;
	vUpdateWriteableNumber(tWindow,
			CHOICES_SCALE_WRITEABLE,
			tOptionsTemp.iScaleFactor);
} /* end of vChangeScaleFactor */

/*
 * bChoicesMouseClick - handle a mouse click in the Choices window
 */
BOOL
bChoicesMouseClick(event_pollblock *pEvent, void *pvReference)
{
	icon_handle	tAction;
	mouse_block	*pMouse;
	BOOL		bCloseWindow;

	TRACE_MSG("bChoicesMouseClick");

	fail(pEvent == NULL);
	fail(pEvent->type != event_CLICK);

	pMouse = &pEvent->data.mouse;
	if (!pMouse->button.data.select && !pMouse->button.data.adjust) {
		/* Not handled here */
		DBG_HEX(pMouse->button.value);
		return FALSE;
	}

	/* Which action should be taken */
	tAction = pMouse->icon;
	if (pMouse->button.data.adjust) {
	  	/* The adjust button reverses the direction */
		switch (pMouse->icon) {
		case CHOICES_BREAK_UP_BUTTON:
			tAction = CHOICES_BREAK_DOWN_BUTTON;
			break;
		case CHOICES_BREAK_DOWN_BUTTON:
			tAction = CHOICES_BREAK_UP_BUTTON;
			break;
		case CHOICES_SCALE_UP_BUTTON:
			tAction = CHOICES_SCALE_DOWN_BUTTON;
			break;
		case CHOICES_SCALE_DOWN_BUTTON:
			tAction = CHOICES_SCALE_UP_BUTTON;
			break;
		default:
			break;
		}
	}

	/* Actions */
	bCloseWindow = FALSE;
	switch (tAction) {
	case CHOICES_DEFAULT_BUTTON:
		vDefaultButtonAction(pMouse->window);
		break;
	case CHOICES_SAVE_BUTTON:
		vSaveButtonAction();
		break;
	case CHOICES_CANCEL_BUTTON:
		bCloseWindow = TRUE;
		break;
	case CHOICES_APPLY_BUTTON:
		vApplyButtonAction();
		bCloseWindow = TRUE;
		break;
	case CHOICES_BREAK_BUTTON:
		vSetParagraphBreak(pMouse->window, DEFAULT_SCREEN_WIDTH);
		break;
	case CHOICES_BREAK_UP_BUTTON:
		vChangeParagraphBreak(pMouse->window, 1);
		break;
	case CHOICES_BREAK_DOWN_BUTTON:
		vChangeParagraphBreak(pMouse->window, -1);
		break;
	case CHOICES_NO_BREAK_BUTTON:
		vSetParagraphBreak(pMouse->window, 0);
		break;
	case CHOICES_AUTOFILETYPE_BUTTON:
		vChangeAutofiletype(pMouse->window);
		break;
	case CHOICES_HIDDEN_TEXT_BUTTON:
		vChangeHiddenText(pMouse->window);
		break;
	case CHOICES_WITH_IMAGES_BUTTON:
		vUseFontsImages(TRUE, TRUE);
		break;
	case CHOICES_NO_IMAGES_BUTTON:
		vUseFontsImages(TRUE, FALSE);
		break;
	case CHOICES_TEXTONLY_BUTTON:
		vUseFontsImages(FALSE, FALSE);
		break;
	case CHOICES_SCALE_UP_BUTTON:
		vChangeScaleFactor(pMouse->window, 5);
		break;
	case CHOICES_SCALE_DOWN_BUTTON:
		vChangeScaleFactor(pMouse->window, -5);
		break;
	default:
		DBG_DEC(pMouse->icon);
		break;
	}
	if (bCloseWindow) {
		Error_CheckFatal(Wimp_CloseWindow(pMouse->window));
	}
	return TRUE;
} /* end of bChoicesMouseClick */

/*
 * bChoicesKeyPressed - handle a key in the Choices window
 */
BOOL
bChoicesKeyPressed(event_pollblock *pEvent, void *pvReference)
{
	icon_block	tIcon;
	caret_block	*pCaret;
	char		*pcChar;
	int		iNumber;

	DBG_MSG("bChoicesKeyPressed");

	fail(pEvent == NULL);
	fail(pEvent->type != event_KEY);

	if (pEvent->data.key.code != '\r') {
		Error_CheckFatal(Wimp_ProcessKey(pEvent->data.key.code));
		return TRUE;
	}

	pCaret = &pEvent->data.key.caret;

	Error_CheckFatal(Wimp_GetIconState(pCaret->window, pCaret->icon, &tIcon));
	if (!tIcon.flags.data.text || !tIcon.flags.data.indirected) {
		werr(1, "Icon %d must be indirected text", (int)pCaret->icon);
	}
	iNumber = (int)strtol(tIcon.data.indirecttext.buffer, &pcChar, 10);

	switch(pCaret->icon) {
	case CHOICES_BREAK_WRITEABLE:
		if (*pcChar != '\0' && *pcChar != '\r') {
			DBG_DEC(*pcChar);
			iNumber = DEFAULT_SCREEN_WIDTH;
		} else if (iNumber < MIN_SCREEN_WIDTH) {
			iNumber = MIN_SCREEN_WIDTH;
		} else if (iNumber > MAX_SCREEN_WIDTH) {
			iNumber = MAX_SCREEN_WIDTH;
		}
		vSetParagraphBreak(pCaret->window, iNumber);
		break;
	case CHOICES_SCALE_WRITEABLE:
		if (*pcChar != '\0' && *pcChar != '\r') {
			DBG_DEC(*pcChar);
			iNumber = DEFAULT_SCALE_FACTOR;
		} else if (iNumber < MIN_SCALE_FACTOR) {
			iNumber = MIN_SCALE_FACTOR;
		} else if (iNumber > MAX_SCALE_FACTOR) {
			iNumber = MAX_SCALE_FACTOR;
		}
		vSetScaleFactor(pCaret->window, iNumber);
		break;
	default:
		DBG_DEC(pCaret->icon);
		break;
	}
	return TRUE;
} /* end of bChoicesKeyPressed */
#endif /* __riscos */
