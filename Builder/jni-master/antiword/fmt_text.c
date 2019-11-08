/*
 * fmt_text.c
 * Copyright (C) 2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Functions to deal with the Formatted Text format
 *
 * Based on patches send by: Ofir Reichenberg <ofir@qlusters.com>
 *
 * The credit should go to him, but all the bugs are mine.
 */

#include <string.h>
#include "antiword.h"

/* The character set */
static encoding_type	eEncoding = encoding_neutral;
/* Current vertical position information */
static long		lYtopCurr = 0;
/* Local representation of the non-breaking space */
static UCHAR		ucNbsp = 0;


/*
 * vPrologueFMT - set options and perform the Formatted Text initialization
 */
void
vPrologueFMT(diagram_type *pDiag, const options_type *pOptions)
{
	fail(pDiag == NULL);
	fail(pOptions == NULL);

	eEncoding = pOptions->eEncoding;
	pDiag->lXleft = 0;
	pDiag->lYtop = 0;
	lYtopCurr = 0;
} /* end of vPrologueFMT */

/*
 * vPrintFMT - print a Formatted Text string
 */
static void
vPrintFMT(FILE *pFile,
	const char *szString, size_t tStringLength, USHORT usFontstyle)
{
	const UCHAR	*pucByte, *pucStart, *pucLast, *pucNonSpace;

	fail(szString == NULL);

	if (szString == NULL || szString[0] == '\0' || tStringLength == 0) {
		return;
	}

	if (eEncoding == encoding_utf_8) {
		fprintf(pFile, "%.*s", (int)tStringLength, szString);
		return;
	}

	if (ucNbsp == 0) {
		ucNbsp = ucGetNbspCharacter();
		DBG_HEX_C(ucNbsp != 0xa0, ucNbsp);
	}

	pucStart = (UCHAR *)szString;
	pucLast = pucStart + tStringLength - 1;
	pucNonSpace = pucLast;
	while ((*pucNonSpace == (UCHAR)' ' || *pucNonSpace == ucNbsp) &&
	       pucNonSpace > pucStart) {
		pucNonSpace--;
	}

	/* 1: The spaces at the start */
	pucByte = pucStart;
	while ((*pucByte == (UCHAR)' ' || *pucByte == ucNbsp) &&
	       pucByte <= pucLast) {
		(void)putc(' ', pFile);
		pucByte++;
	}

	if (pucByte > pucLast) {
		/* There is no text, just spaces */
		return;
	}

	/* 2: Start the *bold*, /italic/ and _underline_ */
	if (bIsBold(usFontstyle)) {
		(void)putc('*', pFile);
	}
	if (bIsItalic(usFontstyle)) {
		(void)putc('/', pFile);
	}
	if (bIsUnderline(usFontstyle)) {
		(void)putc('_', pFile);
	}

	/* 3: The text itself */
	while (pucByte <= pucNonSpace) {
		if (*pucByte == ucNbsp) {
			(void)putc(' ', pFile);
		} else {
			(void)putc((char)*pucByte, pFile);
		}
		pucByte++;
	}

	/* 4: End the *bold*, /italic/ and _underline_ */
	if (bIsUnderline(usFontstyle)) {
		(void)putc('_', pFile);
	}
	if (bIsItalic(usFontstyle)) {
		(void)putc('/', pFile);
	}
	if (bIsBold(usFontstyle)) {
		(void)putc('*', pFile);
	}

	/* 5: The spaces at the end */
	while (pucByte <= pucLast) {
		(void)putc(' ', pFile);
		pucByte++;
	}
} /* end of vPrintFMT */

/*
 * vMoveTo - move to the given X,Y coordinates
 *
 * Move the current position of the given diagram to its X,Y coordinates,
 * start on a new page if needed
 */
static void
vMoveTo(diagram_type *pDiag)
{
	int	iCount, iNbr;

	fail(pDiag == NULL);
	fail(pDiag->pOutFile == NULL);

	if (pDiag->lYtop != lYtopCurr) {
		iNbr = iDrawUnits2Char(pDiag->lXleft);
		for (iCount = 0; iCount < iNbr; iCount++) {
			(void)putc(FILLER_CHAR, pDiag->pOutFile);
		}
		lYtopCurr = pDiag->lYtop;
	}
} /* end of vMoveTo */

/*
 * vSubstringFMT - print a sub string
 */
void
vSubstringFMT(diagram_type *pDiag,
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

	vMoveTo(pDiag);
	vPrintFMT(pDiag->pOutFile, szString, tStringLength, usFontstyle);
	pDiag->lXleft += lStringWidth;
} /* end of vSubstringFMT */
