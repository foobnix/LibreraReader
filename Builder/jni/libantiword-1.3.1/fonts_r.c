/*
 * fonts_r.c
 * Copyright (C) 1999-2002 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to deal with fonts (RiscOs version)
 */

#include <stdlib.h>
#include <string.h>
#include "DeskLib:Font.h"
#include "drawfile.h"
#include "antiword.h"

static font_handle	tFontCurr = (font_handle)-1;

/*
 * pOpenFontTableFile - open the Font translation file
 * Copy the file to the proper place if necessary.
 *
 * Returns the file pointer or NULL
 */
FILE *
pOpenFontTableFile(void)
{
	FILE	*pFileR, *pFileW;
	char	*szFontNamesFile;
	size_t	tSize;
	BOOL	bFailed;
	char	acBuffer[256];

	pFileR = fopen("<AntiWord$FontNamesFile>", "r");
	if (pFileR != NULL) {
		/* The font table is already in the right directory */
		return pFileR;
	}

	szFontNamesFile = getenv("AntiWord$FontNamesSave");
	if (szFontNamesFile == NULL) {
		werr(0, "Warning: Name of the FontNames file not found");
		return NULL;
	}
	DBG_MSG(szFontNamesFile);

	pFileR = fopen("<AntiWord$Dir>.Resources.Default", "r");
	if (pFileR == NULL) {
		werr(0, "I can't find 'Resources.Default'");
		return NULL;
	}
	/* Here the default font translation table is known to exist */

	if (!bMakeDirectory(szFontNamesFile)) {
		werr(0,
		"I can't make a directory for the FontNames file");
		return NULL;
	}
	/* Here the proper directory is known to exist */

	pFileW = fopen(szFontNamesFile, "w");
	if (pFileW == NULL) {
		(void)fclose(pFileR);
		werr(0, "I can't create a default FontNames file");
		return NULL;
	}
	/* Here the proper directory is known to be writeable */

	/* Copy the default FontNames file */
	bFailed = FALSE;
	while (!feof(pFileR)) {
		tSize = fread(acBuffer, 1, sizeof(acBuffer), pFileR);
		if (ferror(pFileR)) {
			DBG_MSG("Read error");
			bFailed = TRUE;
			break;
		}
		if (fwrite(acBuffer, 1, tSize, pFileW) != tSize) {
			DBG_MSG("Write error");
			bFailed = TRUE;
			break;
		}
	}
	(void)fclose(pFileW);
	(void)fclose(pFileR);
	if (bFailed) {
		DBG_MSG("Copying the FontNames file failed");
		(void)remove(szFontNamesFile);
		return NULL;
	}
	return fopen(szFontNamesFile, "r");
} /* end of pOpenFontTableFile */

/*
 * vCloseFont - close the current font, if any
 */
void
vCloseFont(void)
{
	os_error	*e;

	NO_DBG_MSG("vCloseFont");

	if (tFontCurr == (font_handle)-1) {
		return;
	}
	e = Font_LoseFont(tFontCurr);
	if (e != NULL) {
		werr(0, "Close font error %d: %s", e->errnum, e->errmess);
	}
	tFontCurr = (font_handle)-1;
} /* end of vCloseFont */

/*
 * tOpenFont - make the specified font the current font
 *
 * Returns the font reference number for use in a draw file
 */
drawfile_fontref
tOpenFont(UCHAR ucWordFontNumber, USHORT usFontStyle, USHORT usWordFontSize)
{
	os_error	*e;
	const char	*szOurFontname;
	font_handle	tFont;
	int	iFontnumber;

	NO_DBG_MSG("tOpenFont");
	NO_DBG_DEC(ucWordFontNumber);
	NO_DBG_HEX(usFontStyle);
	NO_DBG_DEC(usWordFontSize);

	/* Keep the relevant bits */
	usFontStyle &= FONT_BOLD|FONT_ITALIC;
	NO_DBG_HEX(usFontStyle);

	iFontnumber = iGetFontByNumber(ucWordFontNumber, usFontStyle);
	szOurFontname = szGetOurFontname(iFontnumber);
	if (szOurFontname == NULL || szOurFontname[0] == '\0') {
		tFontCurr = (font_handle)-1;
		return (byte)0;
	}
	NO_DBG_MSG(szOurFontname);
	e = Font_FindFont(&tFont, (char *)szOurFontname,
			(int)usWordFontSize * 8, (int)usWordFontSize * 8,
			0, 0);
	if (e != NULL) {
		switch (e->errnum) {
		case 523:
			werr(0, "%s", e->errmess);
			break;
		default:
			werr(0, "Open font error %d: %s",
				e->errnum, e->errmess);
			break;
		}
		tFontCurr = (font_handle)-1;
		return (drawfile_fontref)0;
	}
	tFontCurr = tFont;
	NO_DBG_DEC(tFontCurr);
	return (drawfile_fontref)(iFontnumber + 1);
} /* end of tOpenFont */

/*
 * tOpenTableFont - make the table font the current font
 *
 * Returns the font reference number for use in a draw file
 */
drawfile_fontref
tOpenTableFont(USHORT usWordFontSize)
{
	int	iWordFontnumber;

	NO_DBG_MSG("tOpenTableFont");

	iWordFontnumber = iFontname2Fontnumber(TABLE_FONT, FONT_REGULAR);
	if (iWordFontnumber < 0 || iWordFontnumber > (int)UCHAR_MAX) {
		DBG_DEC(iWordFontnumber);
		tFontCurr = (font_handle)-1;
		return (drawfile_fontref)0;
	}

	return tOpenFont((UCHAR)iWordFontnumber, FONT_REGULAR, usWordFontSize);
} /* end of tOpenTableFont */

/*
 * lComputeStringWidth - compute the string width
 *
 * Returns the string width in millipoints
 */
long
lComputeStringWidth(const char *szString, size_t tStringLength,
	drawfile_fontref tFontRef, USHORT usFontSize)
{
	font_string	tStr;
	os_error	*e;

	fail(szString == NULL);
	fail(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);

	if (szString[0] == '\0' || tStringLength == 0) {
		/* Empty string */
		return 0;
	}
	if (tStringLength == 1 && szString[0] == TABLE_SEPARATOR) {
		/* Font_strwidth doesn't like control characters */
		return 0;
	}
	if (tFontCurr == (font_handle)-1) {
		/* No current font, use systemfont */
		return lChar2MilliPoints(tStringLength);
	}
	tStr.s = (char *)szString;
	tStr.x = INT_MAX;
	tStr.y = INT_MAX;
	tStr.split = -1;
	tStr.term = tStringLength;
	e = Font_StringWidth(&tStr);
	if (e == NULL) {
		return (long)tStr.x;
	}
	DBG_DEC(e->errnum);
	DBG_MSG(e->errmess);
	DBG_DEC(tStringLength);
	DBG_MSG(szString);
	werr(0, "String width error %d: %s", e->errnum, e->errmess);
	return lChar2MilliPoints(tStringLength);
} /* end of lComputeStringWidth */

/*
 * tCountColumns - count the number of columns in a string
 *
 * Returns the number of columns
 */
size_t
tCountColumns(const char *szString, size_t tLength)
{
	fail(szString == NULL);

	/* One byte, one character, one column */
	return tLength;
} /* end of tCountColumns */

/*
 * tGetCharacterLength - the length of the specified character in bytes
 *
 * Returns the length in bytes
 */
size_t
tGetCharacterLength(const char *szString)
{
	return 1;
} /* end of tGetCharacterLength */
