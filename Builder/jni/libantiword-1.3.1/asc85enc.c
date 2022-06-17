/*
 * asc85enc.c
 * Copyright (C) 2000-2003 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to for ASCII 85 encoding
 *
 *====================================================================
 * This part of the software is based on:
 * asc85ec.c - ASCII85 and Hex encoding for PostScript Level 2 and PDF
 * Copyright (C) 1994-99 Thomas Merz (tm@muc.de)
 *====================================================================
 * The credit should go to him, but all the bugs are mine.
 */

#include <stdio.h>
#include "antiword.h"

static const ULONG	aulPower85[5] = {
	1UL, 85UL, 85UL * 85, 85UL * 85 * 85, 85UL * 85 * 85 * 85,
};
static int	iOutBytes = 0;	/* Number of characters in an output line */
static char	cCharPrev = '\0';

/*
 * Two percent characters at the start of a line will cause trouble
 * with some post-processing software. In order to avoid this, we
 * simply insert a line break if we encounter two percent characters
 * at the start of the line. Of course, this rather simplistic
 * algorithm may lead to a large line count in pathological cases,
 * but the chance for hitting such a case is very small, and even
 * so it's only a cosmetic flaw and not a functional restriction.
 */

/*
 * vOutputByte - output one byte
 */
static void
vOutputByte(ULONG ulChar, FILE *pOutFile)
{
	if (iOutBytes == 1 && cCharPrev == '%' && ulChar == (ULONG)'%') {
		if (putc('\n', pOutFile) != EOF) {
			iOutBytes = 0;
		}
	}
	if (putc((int)ulChar, pOutFile) == EOF) {
		return;
	}
	iOutBytes++;
	if (iOutBytes > 63) {
		if (putc('\n', pOutFile) != EOF) {
			iOutBytes = 0;
		}
	}
	cCharPrev = (char)ulChar;
} /* end of vOutputByte */

/*
 * vASCII85EncodeByte - ASCII 85 encode a byte
 */
void
vASCII85EncodeByte(FILE *pOutFile, int iByte)
{
	static ULONG	ulBuffer[4] = { 0, 0, 0, 0 };
	static int	iInBuffer = 0;
	ULONG	ulValue, ulTmp;
	int	iIndex;

	fail(pOutFile == NULL);
	fail(iInBuffer < 0);
	fail(iInBuffer > 3);

	if (iByte == EOF) {
		/* End Of File, time to clean up */
		if (iInBuffer > 0 && iInBuffer < 4) {
			/* Encode the remaining bytes */
			ulValue = 0;
			for (iIndex = iInBuffer - 1; iIndex >= 0; iIndex--) {
				ulValue |=
					ulBuffer[iIndex] << (8 * (3 - iIndex));
			}
			for (iIndex = 4; iIndex >= 4 - iInBuffer; iIndex--) {
				ulTmp = ulValue / aulPower85[iIndex];
				vOutputByte(ulTmp + '!', pOutFile);
				ulValue -= ulTmp * aulPower85[iIndex];
			}
		}
		/* Add the End Of Data marker */
		(void)putc('~', pOutFile);
		(void)putc('>', pOutFile);
		(void)putc('\n', pOutFile);
		/* Reset the control variables */
		iInBuffer = 0;
		iOutBytes = 0;
		cCharPrev = '\0';
		return;
	}

	ulBuffer[iInBuffer] = (ULONG)iByte & 0xff;
	iInBuffer++;

	if (iInBuffer >= 4) {
		ulValue = (ulBuffer[0] << 24) | (ulBuffer[1] << 16) |
			(ulBuffer[2] << 8) | ulBuffer[3];
		if (ulValue == 0) {
			vOutputByte((ULONG)'z', pOutFile); /* Shortcut for 0 */
		} else {
			for (iIndex = 4; iIndex >= 0; iIndex--) {
				ulTmp = ulValue / aulPower85[iIndex];
				vOutputByte(ulTmp + '!', pOutFile);
				ulValue -= ulTmp * aulPower85[iIndex];
			}
		}
		/* Reset the buffer */
		iInBuffer = 0;
	}
} /* end of vASCII85EncodeByte */

/*
 * vASCII85EncodeArray - ASCII 85 encode a byte array
 */
void
vASCII85EncodeArray(FILE *pInFile, FILE *pOutFile, size_t tLength)
{
	size_t	tCount;
	int	iByte;

	fail(pInFile == NULL);
	fail(pOutFile == NULL);

	DBG_DEC(tLength);

	for (tCount = 0; tCount < tLength; tCount++) {
		iByte = iNextByte(pInFile);
		if (iByte == EOF) {
			break;
		}
		vASCII85EncodeByte(pOutFile, iByte);
	}
} /* end of vASCII85EncodeArray */

/*
 * vASCII85EncodeFile - ASCII 85 encode part of a file
 */
void
vASCII85EncodeFile(FILE *pInFile, FILE *pOutFile, size_t tLength)
{
	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(tLength == 0);

	vASCII85EncodeArray(pInFile, pOutFile, tLength);
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vASCII85EncodeFile */
