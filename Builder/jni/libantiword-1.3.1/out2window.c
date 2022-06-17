/*
 * out2window.c
 * Copyright (C) 1998-2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Output to a text window
 */

#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include "antiword.h"

/* Used for numbering the chapters */
static unsigned int	auiHdrCounter[9];


/*
 * vString2Diagram - put a string into a diagram
 */
static void
vString2Diagram(diagram_type *pDiag, output_type *pAnchor)
{
	output_type	*pOutput;
	long		lWidth;
	USHORT		usMaxFontSize;

	TRACE_MSG("vString2Diagram");

	fail(pDiag == NULL);
	fail(pAnchor == NULL);

	/* Compute the maximum fontsize in this string */
	usMaxFontSize = MIN_FONT_SIZE;
	for (pOutput = pAnchor; pOutput != NULL; pOutput = pOutput->pNext) {
		if (pOutput->usFontSize > usMaxFontSize) {
			usMaxFontSize = pOutput->usFontSize;
		}
	}

	/* Goto the next line */
	vMove2NextLine(pDiag, pAnchor->tFontRef, usMaxFontSize);

	/* Output all substrings */
	for (pOutput = pAnchor; pOutput != NULL; pOutput = pOutput->pNext) {
		lWidth = lMilliPoints2DrawUnits(pOutput->lStringWidth);
		vSubstring2Diagram(pDiag, pOutput->szStorage,
			pOutput->tNextFree, lWidth, pOutput->ucFontColor,
			pOutput->usFontStyle, pOutput->tFontRef,
			pOutput->usFontSize, usMaxFontSize);
	}

	/* Goto the start of the line */
	pDiag->lXleft = 0;
	TRACE_MSG("leaving vString2Diagram");
} /* end of vString2Diagram */

/*
 * vSetLeftIndentation - set the left indentation of the specified diagram
 */
void
vSetLeftIndentation(diagram_type *pDiag, long lLeftIndentation)
{
	long	lX;

	TRACE_MSG("vSetLeftIndentation");

	fail(pDiag == NULL);
	fail(lLeftIndentation < 0);

	lX = lMilliPoints2DrawUnits(lLeftIndentation);
	if (lX > 0) {
		pDiag->lXleft = lX;
	} else {
		pDiag->lXleft = 0;
	}
} /* end of vSetLeftIndentation */

/*
 * lComputeNetWidth - compute the net string width
 */
static long
lComputeNetWidth(output_type *pAnchor)
{
	output_type	*pTmp;
	long		lNetWidth;

	TRACE_MSG("lComputeNetWidth");

	fail(pAnchor == NULL);

	/* Step 1: Count all but the last sub-string */
	lNetWidth = 0;
	for (pTmp = pAnchor; pTmp->pNext != NULL; pTmp = pTmp->pNext) {
		fail(pTmp->lStringWidth < 0);
		lNetWidth += pTmp->lStringWidth;
	}
	fail(pTmp == NULL);
	fail(pTmp->pNext != NULL);

	/* Step 2: remove the white-space from the end of the string */
	while (pTmp->tNextFree != 0 &&
	       isspace((int)(UCHAR)pTmp->szStorage[pTmp->tNextFree - 1])) {
		pTmp->szStorage[pTmp->tNextFree - 1] = '\0';
		pTmp->tNextFree--;
		NO_DBG_DEC(pTmp->lStringWidth);
		pTmp->lStringWidth = lComputeStringWidth(
						pTmp->szStorage,
						pTmp->tNextFree,
						pTmp->tFontRef,
						pTmp->usFontSize);
		NO_DBG_DEC(pTmp->lStringWidth);
	}

	/* Step 3: Count the last sub-string */
	lNetWidth += pTmp->lStringWidth;
	return lNetWidth;
} /* end of lComputeNetWidth */

/*
 * iComputeHoles - compute number of holes
 * (A hole is a number of whitespace characters followed by a
 *  non-whitespace character)
 */
static int
iComputeHoles(output_type *pAnchor)
{
	output_type	*pTmp;
	size_t	tIndex;
	int	iCounter;
	BOOL	bWasSpace, bIsSpace;

	TRACE_MSG("iComputeHoles");

	fail(pAnchor == NULL);

	iCounter = 0;
	bIsSpace = FALSE;
	/* Count the holes */
	for (pTmp = pAnchor; pTmp != NULL; pTmp = pTmp->pNext) {
		fail(pTmp->tNextFree != strlen(pTmp->szStorage));
		for (tIndex = 0; tIndex <= pTmp->tNextFree; tIndex++) {
			bWasSpace = bIsSpace;
			bIsSpace = isspace((int)(UCHAR)pTmp->szStorage[tIndex]);
			if (bWasSpace && !bIsSpace) {
				iCounter++;
			}
		}
	}
	return iCounter;
} /* end of iComputeHoles */

/*
 * vAlign2Window - Align a string and insert it into the text
 */
void
vAlign2Window(diagram_type *pDiag, output_type *pAnchor,
	long lScreenWidth, UCHAR ucAlignment)
{
	long	lNetWidth, lLeftIndentation;

	TRACE_MSG("vAlign2Window");

	fail(pDiag == NULL || pAnchor == NULL);
	fail(lScreenWidth < lChar2MilliPoints(MIN_SCREEN_WIDTH));

	lNetWidth = lComputeNetWidth(pAnchor);

	if (lScreenWidth > lChar2MilliPoints(MAX_SCREEN_WIDTH) ||
	    lNetWidth <= 0) {
		/*
		 * Screenwidth is "infinite", so no alignment is possible
		 * Don't bother to align an empty line
		 */
		vString2Diagram(pDiag, pAnchor);
		TRACE_MSG("leaving vAlign2Window #1");
		return;
	}

	switch (ucAlignment) {
	case ALIGNMENT_CENTER:
		lLeftIndentation = (lScreenWidth - lNetWidth) / 2;
		DBG_DEC_C(lLeftIndentation < 0, lLeftIndentation);
		if (lLeftIndentation > 0) {
			vSetLeftIndentation(pDiag, lLeftIndentation);
		}
		break;
	case ALIGNMENT_RIGHT:
		lLeftIndentation = lScreenWidth - lNetWidth;
		DBG_DEC_C(lLeftIndentation < 0, lLeftIndentation);
		if (lLeftIndentation > 0) {
			vSetLeftIndentation(pDiag, lLeftIndentation);
		}
		break;
	case ALIGNMENT_JUSTIFY:
	case ALIGNMENT_LEFT:
	default:
		break;
	}
	vString2Diagram(pDiag, pAnchor);
	TRACE_MSG("leaving vAlign2Window #2");
} /* end of vAlign2Window */

/*
 * vJustify2Window - Justify a string and insert it into the text
 */
void
vJustify2Window(diagram_type *pDiag, output_type *pAnchor,
	long lScreenWidth, long lRightIndentation, UCHAR ucAlignment)
{
	output_type	*pTmp;
	char	*pcNew, *pcOld, *szStorage;
	long	lNetWidth, lSpaceWidth, lToAdd;
	int	iFillerLen, iHoles;

	TRACE_MSG("vJustify2Window");

	fail(pDiag == NULL || pAnchor == NULL);
	fail(lScreenWidth < MIN_SCREEN_WIDTH);
	fail(lRightIndentation > 0);

	if (ucAlignment != ALIGNMENT_JUSTIFY) {
		vAlign2Window(pDiag, pAnchor, lScreenWidth, ucAlignment);
		return;
	}

	lNetWidth = lComputeNetWidth(pAnchor);

	if (lScreenWidth > lChar2MilliPoints(MAX_SCREEN_WIDTH) ||
	    lNetWidth <= 0) {
		/*
		 * Screenwidth is "infinite", so justify is not possible
		 * Don't bother to justify an empty line
		 */
		vString2Diagram(pDiag, pAnchor);
		TRACE_MSG("leaving vJustify2Window #1");
		return;
	}

	/* Justify */
	fail(ucAlignment != ALIGNMENT_JUSTIFY);
	lSpaceWidth = lComputeStringWidth(" ", 1,
				pAnchor->tFontRef, pAnchor->usFontSize);
	lToAdd = lScreenWidth -
			lNetWidth -
			lDrawUnits2MilliPoints(pDiag->lXleft) +
			lRightIndentation;
#if defined(DEBUG)
	if (lToAdd / lSpaceWidth < -1) {
		DBG_DEC(lSpaceWidth);
		DBG_DEC(lToAdd);
		DBG_DEC(lScreenWidth);
		DBG_DEC(lNetWidth);
		DBG_DEC(lDrawUnits2MilliPoints(pDiag->lXleft));
		DBG_DEC(pDiag->lXleft);
		DBG_DEC(lRightIndentation);
	}
#endif /* DEBUG */
	lToAdd /= lSpaceWidth;
	DBG_DEC_C(lToAdd < 0, lToAdd);
	if (lToAdd <= 0) {
		vString2Diagram(pDiag, pAnchor);
		TRACE_MSG("leaving vJustify2Window #2");
		return;
	}

	/* Justify by adding spaces */
	iHoles = iComputeHoles(pAnchor);
	for (pTmp = pAnchor; pTmp != NULL; pTmp = pTmp->pNext) {
		fail(pTmp->tNextFree != strlen(pTmp->szStorage));
		fail(lToAdd < 0);
		szStorage = xmalloc(pTmp->tNextFree + (size_t)lToAdd + 1);
		pcNew = szStorage;
		for (pcOld = pTmp->szStorage; *pcOld != '\0'; pcOld++) {
			*pcNew++ = *pcOld;
			if (*pcOld == ' ' &&
			    *(pcOld + 1) != ' ' &&
			    iHoles > 0) {
				iFillerLen = (int)(lToAdd / iHoles);
				lToAdd -= iFillerLen;
				iHoles--;
				for (; iFillerLen > 0; iFillerLen--) {
					*pcNew++ = ' ';
				}
			}
		}
		*pcNew = '\0';
		pTmp->szStorage = xfree(pTmp->szStorage);
		pTmp->szStorage = szStorage;
		pTmp->tStorageSize = pTmp->tNextFree + (size_t)lToAdd + 1;
		pTmp->lStringWidth +=
			(pcNew - szStorage - (long)pTmp->tNextFree) *
			lSpaceWidth;
		fail(pcNew < szStorage);
		pTmp->tNextFree = (size_t)(pcNew - szStorage);
		fail(pTmp->tNextFree != strlen(pTmp->szStorage));
	}
	DBG_DEC_C(lToAdd != 0, lToAdd);
	vString2Diagram(pDiag, pAnchor);
	TRACE_MSG("leaving vJustify2Window #3");
} /* end of vJustify2Window */

/*
 * vResetStyles - reset the style information variables
 */
void
vResetStyles(void)
{
	TRACE_MSG("vResetStyles");

	(void)memset(auiHdrCounter, 0, sizeof(auiHdrCounter));
} /* end of vResetStyles */

/*
 * tStyle2Window - Add the style characters to the line
 *
 * Returns the length of the resulting string
 */
size_t
tStyle2Window(char *szLine, size_t tLineSize, const style_block_type *pStyle,
	const section_block_type *pSection)
{
	char	*pcTxt;
	size_t	tIndex, tStyleIndex;
	BOOL	bNeedPrevLvl;
	level_type_enum	eNumType;
	UCHAR	ucNFC;

	TRACE_MSG("tStyle2Window");

	fail(szLine == NULL || pStyle == NULL || pSection == NULL);

	if (pStyle->usIstd == 0 || pStyle->usIstd > 9) {
		szLine[0] = '\0';
		return 0;
	}

	/* Set the numbers */
	tStyleIndex = (size_t)pStyle->usIstd - 1;
	for (tIndex = 0; tIndex < 9; tIndex++) {
		if (tIndex == tStyleIndex) {
			auiHdrCounter[tIndex]++;
		} else if (tIndex > tStyleIndex) {
			auiHdrCounter[tIndex] = 0;
		} else if (auiHdrCounter[tIndex] == 0) {
			auiHdrCounter[tIndex] = 1;
		}
	}

	eNumType = eGetNumType(pStyle->ucNumLevel);
	if (eNumType != level_type_outline) {
		szLine[0] = '\0';
		return 0;
	}

	/* Print the numbers */
	pcTxt = szLine;
	bNeedPrevLvl = (pSection->usNeedPrevLvl & BIT(tStyleIndex)) != 0;
	for (tIndex = 0; tIndex <= tStyleIndex; tIndex++) {
		if (tIndex == tStyleIndex ||
		    (bNeedPrevLvl && tIndex < tStyleIndex)) {
			if (pcTxt - szLine >= tLineSize - 25) {
				/* Prevent a possible buffer overflow */
				DBG_DEC(pcTxt - szLine);
				DBG_DEC(tLineSize - 25);
				DBG_FIXME();
				szLine[0] = '\0';
				return 0;
			}
			ucNFC = pSection->aucNFC[tIndex];
			switch(ucNFC) {
			case LIST_ARABIC_NUM:
			case LIST_NUMBER_TXT:
			case LIST_ORDINAL_TXT:
				pcTxt += sprintf(pcTxt, "%u",
					auiHdrCounter[tIndex]);
				break;
			case LIST_UPPER_ROMAN:
			case LIST_LOWER_ROMAN:
				pcTxt += tNumber2Roman(
					auiHdrCounter[tIndex],
					ucNFC == LIST_UPPER_ROMAN,
					pcTxt);
				break;
			case LIST_UPPER_ALPHA:
			case LIST_LOWER_ALPHA:
				pcTxt += tNumber2Alpha(
					auiHdrCounter[tIndex],
					ucNFC == LIST_UPPER_ALPHA,
					pcTxt);
				break;
			case LIST_OUTLINE_NUM:
				pcTxt += sprintf(pcTxt, "%02u",
					auiHdrCounter[tIndex]);
				break;
			default:
				DBG_DEC(ucNFC);
				DBG_FIXME();
				pcTxt += sprintf(pcTxt, "%u",
					auiHdrCounter[tIndex]);
				break;
			}
			if (tIndex < tStyleIndex) {
				*pcTxt++ = '.';
			} else if (tIndex == tStyleIndex) {
				*pcTxt++ = ' ';
			}
		}
	}
	*pcTxt = '\0';
	NO_DBG_MSG_C((int)pStyle->usIstd >= 1 &&
		(int)pStyle->usIstd <= 9 &&
		eNumType != level_type_none &&
		eNumType != level_type_outline, szLine);
	NO_DBG_MSG_C(szLine[0] != '\0', szLine);
	fail(pcTxt < szLine);
	return (size_t)(pcTxt - szLine);
} /* end of tStyle2Window */

/*
 * vRemoveRowEnd - remove the end of table row indicator
 *
 * Remove the double TABLE_SEPARATOR characters from the end of the string.
 * Special: remove the TABLE_SEPARATOR, 0x0a sequence
 */
static void
vRemoveRowEnd(char *szRowTxt)
{
	int	iLastIndex;

	TRACE_MSG("vRemoveRowEnd");

	fail(szRowTxt == NULL || szRowTxt[0] == '\0');

	iLastIndex = (int)strlen(szRowTxt) - 1;

	if (szRowTxt[iLastIndex] == TABLE_SEPARATOR ||
	    szRowTxt[iLastIndex] == (char)0x0a) {
		szRowTxt[iLastIndex] = '\0';
		iLastIndex--;
	} else {
		DBG_HEX(szRowTxt[iLastIndex]);
	}

	if (iLastIndex >= 0 && szRowTxt[iLastIndex] == (char)0x0a) {
		szRowTxt[iLastIndex] = '\0';
		iLastIndex--;
	}

	if (iLastIndex >= 0 && szRowTxt[iLastIndex] == TABLE_SEPARATOR) {
		szRowTxt[iLastIndex] = '\0';
		return;
	}

	DBG_DEC(iLastIndex);
	DBG_HEX(szRowTxt[iLastIndex]);
	DBG_MSG(szRowTxt);
} /* end of vRemoveRowEnd */

/*
 * tComputeStringLengthMax - max string length in relation to max column width
 *
 * Return the maximum string length
 */
static size_t
tComputeStringLengthMax(const char *szString, size_t tColumnWidthMax)
{
	const char	*pcTmp;
	size_t	tLengthMax, tLenPrev, tLen, tWidth;

	TRACE_MSG("tComputeStringLengthMax");

	fail(szString == NULL);
	fail(tColumnWidthMax == 0);

	pcTmp = strchr(szString, '\n');
	if (pcTmp != NULL) {
		tLengthMax = (size_t)(pcTmp - szString + 1);
	} else {
		tLengthMax = strlen(szString);
	}
	if (tLengthMax == 0) {
		return 0;
	}

	tLen = 0;
	tWidth = 0;
	for (;;) {
		tLenPrev = tLen;
		tLen += tGetCharacterLength(szString + tLen);
		DBG_DEC_C(tLen > tLengthMax, tLen);
		DBG_DEC_C(tLen > tLengthMax, tLengthMax);
		fail(tLen > tLengthMax);
		tWidth = tCountColumns(szString, tLen);
		if (tWidth > tColumnWidthMax) {
			return tLenPrev;
		}
		if (tLen >= tLengthMax) {
			return tLengthMax;
		}
	}
} /* end of tComputeStringLengthMax */

/*
 * tGetBreakingPoint - get the number of bytes that fit the column
 *
 * Returns the number of bytes that fit the column
 */
static size_t
tGetBreakingPoint(const char *szString,
	size_t tLen, size_t tWidth, size_t tColumnWidthMax)
{
	int	iIndex;

	TRACE_MSG("tGetBreakingPoint");

	fail(szString == NULL);
	fail(tLen > strlen(szString));
	fail(tWidth > tColumnWidthMax);

	if (tWidth < tColumnWidthMax ||
	    (tWidth == tColumnWidthMax &&
	     (szString[tLen] == ' ' ||
	      szString[tLen] == '\n' ||
	      szString[tLen] == '\0'))) {
		/* The string already fits, do nothing */
		return tLen;
	}
	/* Search for a breaking point */
	for (iIndex = (int)tLen - 1; iIndex >= 0; iIndex--) {
		if (szString[iIndex] == ' ') {
			return (size_t)iIndex;
		}
	}
	/* No breaking point found, just fill the column */
	return tLen;
} /* end of tGetBreakingPoint */

/*
 * tComputeColumnWidthMax - compute the maximum column width
 */
static size_t
tComputeColumnWidthMax(short sWidth, long lCharWidth, double dFactor)
{
	size_t	tColumnWidthMax;

	TRACE_MSG("tComputeColumnWidthMax");

	fail(sWidth < 0);
	fail(lCharWidth <= 0);
	fail(dFactor <= 0.0);

	tColumnWidthMax = (size_t)(
		(lTwips2MilliPoints(sWidth) * dFactor + lCharWidth / 2.0) /
		 lCharWidth);
	if (tColumnWidthMax == 0) {
		/* Minimum column width */
		return 1;
	}
	if (tColumnWidthMax > 1) {
		/* Make room for the TABLE_SEPARATOR_CHAR */
		tColumnWidthMax--;
	}
	NO_DBG_DEC(tColumnWidthMax);
	return tColumnWidthMax;
} /* end of tComputeColumnWidthMax */

/*
 * vTableRow2Window - put a table row into a diagram
 */
void
vTableRow2Window(diagram_type *pDiag, output_type *pOutput,
	const row_block_type *pRowInfo,
	conversion_type eConversionType, int iParagraphBreak)
{
	output_type	tRow;
	char	*aszColTxt[TABLE_COLUMN_MAX];
	char	*szLine, *pcTxt;
	double	dMagnify;
	long	lCharWidthLarge, lCharWidthSmall;
	size_t	tColumnWidthTotal, atColumnWidthMax[TABLE_COLUMN_MAX];
	size_t	tSize, tColumnWidthMax, tWidth, tLen;
	int	iIndex, iNbrOfColumns, iTmp;
	BOOL	bNotReady;

	TRACE_MSG("vTableRow2Window");

	fail(pDiag == NULL || pOutput == NULL || pRowInfo == NULL);
	fail(pOutput->szStorage == NULL);
	fail(pOutput->pNext != NULL);
	fail(iParagraphBreak < 0);

	/* Character sizes */
	lCharWidthLarge = lComputeStringWidth("W", 1,
				pOutput->tFontRef, pOutput->usFontSize);
	NO_DBG_DEC(lCharWidthLarge);
	lCharWidthSmall = lComputeStringWidth("i", 1,
				pOutput->tFontRef, pOutput->usFontSize);
	NO_DBG_DEC(lCharWidthSmall);
	/* For the time being: use a fixed width font */
	fail(lCharWidthLarge != lCharWidthSmall);

	vRemoveRowEnd(pOutput->szStorage);

	/* Split the row text into a set of column texts */
	aszColTxt[0] = pOutput->szStorage;
	for (iNbrOfColumns = 1;
	     iNbrOfColumns < TABLE_COLUMN_MAX;
	     iNbrOfColumns++) {
		aszColTxt[iNbrOfColumns] =
				strchr(aszColTxt[iNbrOfColumns - 1],
					TABLE_SEPARATOR);
		if (aszColTxt[iNbrOfColumns] == NULL) {
			break;
		}
		*aszColTxt[iNbrOfColumns] = '\0';
		aszColTxt[iNbrOfColumns]++;
		NO_DBG_DEC(iNbrOfColumns);
		NO_DBG_MSG(aszColTxt[iNbrOfColumns]);
	}

	/* Work around a bug in Word */
	while (iNbrOfColumns > (int)pRowInfo->ucNumberOfColumns &&
	       pRowInfo->asColumnWidth[iNbrOfColumns] == 0) {
		iNbrOfColumns--;
	}

	DBG_DEC_C(iNbrOfColumns != (int)pRowInfo->ucNumberOfColumns,
		iNbrOfColumns);
	DBG_DEC_C(iNbrOfColumns != (int)pRowInfo->ucNumberOfColumns,
		pRowInfo->ucNumberOfColumns);
	if (iNbrOfColumns != (int)pRowInfo->ucNumberOfColumns) {
		werr(0, "Skipping an unmatched table row");
		return;
	}

#if defined(__FULL_TEXT_SEARCH)
	/* No table formatting: use for full-text search (untested) */
	for (iIndex = 0; iIndex < iNbrOfColumns; iIndex++) {
		fprintf(pDiag->pOutFile, "%s\n" , aszColTxt[iIndex]);
	}
#else
	if (bAddTableRow(pDiag, aszColTxt, iNbrOfColumns,
			pRowInfo->asColumnWidth, pRowInfo->ucBorderInfo)) {
		/* All work has been done */
		return;
	}

	/* Fill the table with maximum column widths */
	if (eConversionType == conversion_text ||
	    eConversionType == conversion_fmt_text) {
		if (iParagraphBreak == 0 ||
		    iParagraphBreak >= MAX_SCREEN_WIDTH) {
			dMagnify = (double)MAX_SCREEN_WIDTH;
		} else if (iParagraphBreak <= MIN_SCREEN_WIDTH) {
			dMagnify = (double)MIN_SCREEN_WIDTH;
		} else {
			dMagnify = (double)iParagraphBreak;
		}
		dMagnify /= (double)DEFAULT_SCREEN_WIDTH;
		DBG_FLT_C(dMagnify < 0.99 || dMagnify > 1.01, dMagnify);
	} else {
		dMagnify = 1.0;
	}
	tColumnWidthTotal = 0;
	for (iIndex = 0; iIndex < iNbrOfColumns; iIndex++) {
		atColumnWidthMax[iIndex] = tComputeColumnWidthMax(
					pRowInfo->asColumnWidth[iIndex],
					lCharWidthLarge,
					dMagnify);
		tColumnWidthTotal += atColumnWidthMax[iIndex];
	}

	/*
	 * Get enough space for the row.
	 * Worst case: three bytes per UTF-8 character
	 */
	tSize = 3 * (1 + tColumnWidthTotal + (size_t)iNbrOfColumns + 3);
	szLine = xmalloc(tSize);

	do {
		/* Print one line of a table row */
		bNotReady = FALSE;
		pcTxt = szLine;
		*pcTxt++ = TABLE_SEPARATOR_CHAR;
		for (iIndex = 0; iIndex < iNbrOfColumns; iIndex++) {
			tColumnWidthMax = atColumnWidthMax[iIndex];
			if (aszColTxt[iIndex] == NULL) {
				/* Add an empty column */
				for (iTmp = 0;
				     iTmp < (int)tColumnWidthMax;
				     iTmp++) {
					*pcTxt++ = (char)FILLER_CHAR;
				}
				*pcTxt++ = TABLE_SEPARATOR_CHAR;
				*pcTxt = '\0';
				continue;
			}
			/* Compute the length and width of the column text */
			tLen = tComputeStringLengthMax(
					aszColTxt[iIndex], tColumnWidthMax);
			NO_DBG_DEC(tLen);
			while (tLen != 0 &&
					(aszColTxt[iIndex][tLen - 1] == '\n' ||
					 aszColTxt[iIndex][tLen - 1] == ' ')) {
				aszColTxt[iIndex][tLen - 1] = ' ';
				tLen--;
			}
			tWidth = tCountColumns(aszColTxt[iIndex], tLen);
			fail(tWidth > tColumnWidthMax);
			tLen = tGetBreakingPoint(aszColTxt[iIndex],
					tLen, tWidth, tColumnWidthMax);
			tWidth = tCountColumns(aszColTxt[iIndex], tLen);
			if (tLen == 0 && *aszColTxt[iIndex] == '\0') {
				/* No text at all */
				aszColTxt[iIndex] = NULL;
			} else {
				/* Add the text */
				pcTxt += sprintf(pcTxt,
					"%.*s", (int)tLen, aszColTxt[iIndex]);
				if (tLen == 0 && *aszColTxt[iIndex] != ' ') {
					tLen = tGetCharacterLength(
							aszColTxt[iIndex]);
					DBG_CHR(*aszColTxt[iIndex]);
					DBG_FIXME();
					fail(tLen == 0);
				}
				aszColTxt[iIndex] += tLen;
				while (*aszColTxt[iIndex] == ' ') {
					aszColTxt[iIndex]++;
				}
				if (*aszColTxt[iIndex] == '\0') {
					/* This row is now complete */
					aszColTxt[iIndex] = NULL;
				} else {
					/* This row needs more lines */
					bNotReady = TRUE;
				}
			}
			/* Fill up the rest */
			for (iTmp = 0;
			     iTmp < (int)tColumnWidthMax - (int)tWidth;
			     iTmp++) {
				*pcTxt++ = (char)FILLER_CHAR;
			}
			/* End of column */
			*pcTxt++ = TABLE_SEPARATOR_CHAR;
			*pcTxt = '\0';
		}
		/* Output the table row line */
		*pcTxt = '\0';
		tRow = *pOutput;
		tRow.szStorage = szLine;
		fail(pcTxt < szLine);
		tRow.tNextFree = (size_t)(pcTxt - szLine);
		tRow.lStringWidth = lComputeStringWidth(
					tRow.szStorage,
					tRow.tNextFree,
					tRow.tFontRef,
					tRow.usFontSize);
		vString2Diagram(pDiag, &tRow);
		TRACE_MSG("after vString2Diagram in vTableRow2Window");
	} while (bNotReady);
	/* Clean up before you leave */
	szLine = xfree(szLine);
	TRACE_MSG("leaving vTableRow2Window");
#endif /* __FULL_TEXT_SEARCH */
} /* end of vTableRow2Window */
