/*
 * misc.c
 * Copyright (C) 1998-2001 A.J. van Os; Released under GPL
 *
 * Description:
 * misc. functions
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>
#if defined(__riscos)
#include "kernel.h"
#include "swis.h"
#else
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#if defined(__dos)
#define S_ISDIR(x)	(((x) & S_IFMT) == S_IFDIR)
#define S_ISREG(x)	(((x) & S_IFMT) == S_IFREG)
#endif /* __dos */
#endif /* __riscos */
#if defined(WIN32)
#include "windows.h"
#endif // WIN32

#include "antiword.h"

static BOOL	bWord6MacFile = FALSE;


#if !defined(__riscos)
/*
 * szGetHomeDirectory - get the name of the home directory
 */
const char *
szGetHomeDirectory(void)
{
	static char homedir[256];
	const char	*szHome;

	szHome = getenv("HOME");
	if (szHome == NULL || szHome[0] == '\0') {
#if defined(WIN32)
		(void)GetCurrentDirectory(255, &homedir[0]);
		szHome = &homedir[0];
#else
#if defined(__dos)
		szHome = "C:";
 
#else
		werr(0, "I can't find the name of your HOME directory");
		szHome = "";
#endif /* __dos */
#endif // WIN32
	}
	return szHome;
} /* end of szGetHomeDirectory */
#endif /* !__riscos */

/*
 * Get the size of the given file.
 * Returns -1 if the file does not exist or is not a proper file.
 */
long
lGetFilesize(const char *szFilename)
{
#if defined(__riscos)
	_kernel_swi_regs	regs;
	_kernel_oserror		*e;

	(void)memset(&regs, 0, sizeof(regs));
	regs.r[0] = 17;
	regs.r[1] = (int)szFilename;
	e = _kernel_swi(OS_File, &regs, &regs);
	if (e != NULL) {
		werr(0, "Get Filesize error %d: %s",
			e->errnum, e->errmess);
		return -1;
	}
	if (regs.r[0] != 1) {
		/* It's not a proper file or the file does not exist */
		return -1;
	}
	return (long)regs.r[4];
#else
	struct stat	tBuffer;

	if (stat(szFilename, &tBuffer) != 0) {
		werr(0, "Get Filesize error %d", errno);
		return -1;
	}
	if (!S_ISREG(tBuffer.st_mode)) {
		/* It's not a regular file */
		return -1;
	}
	return (long)tBuffer.st_size;
#endif /* __riscos */
} /* end of lGetFilesize */

#if defined(DEBUG)
void
vPrintBlock(const char	*szFile, int iLine,
		const unsigned char *aucBlock, size_t tLength)
{
	int i, j;

	fail(szFile == NULL || iLine < 0 || aucBlock == NULL);

	fprintf(stderr, "%s[%3d]:\n", szFile, iLine);
	for (i = 0; i < 32; i++) {
		if (16 * i >= (int)tLength) {
			return;
		}
		fprintf(stderr, "%03x: ", 16 * i);
		for (j = 0; j < 16; j++) {
			if (16 * i + j < (int)tLength) {
				fprintf(stderr, "%02x ", aucBlock[16 * i + j]);
			}
		}
		fprintf(stderr, "\n");
	}
} /* end of vPrintBlock */

void
vPrintUnicode(const char  *szFile, int iLine, const char *s)
{
	size_t	tLen;
	char	*szASCII;

	tLen = unilen(s) / 2;
	szASCII = xmalloc(tLen + 1);
	(void)unincpy(szASCII, s, tLen);
	szASCII[tLen] = '\0';
	(void)fprintf(stderr, "%s[%3d]: %.240s\n", szFile, iLine, szASCII);
	szASCII = xfree(szASCII);
} /* end of vPrintUnicode */

BOOL
bCheckDoubleLinkedList(output_type *pAnchor)
{
	output_type	*pCurr, *pLast;
	int		iInList;

	pLast = pAnchor;
	iInList = 0;
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		pLast = pCurr;
		iInList++;
	}
	NO_DBG_DEC(iInList);
	for (pCurr = pLast; pCurr != NULL; pCurr = pCurr->pPrev) {
		pLast = pCurr;
		iInList--;
	}
	DBG_DEC_C(iInList != 0, iInList);
	return pAnchor == pLast && iInList == 0;
} /* end of bCheckDoubleLinkedList */
#endif /* DEBUG */

/*
 * bReadBytes
 * This function reads the given number of bytes from the given file,
 * starting from the given offset.
 * Returns TRUE when successfull, otherwise FALSE
 */
BOOL
bReadBytes(unsigned char *aucBytes, size_t tMemb, long lOffset, FILE *pFile)
{
	fail(aucBytes == NULL || pFile == NULL || lOffset < 0);

	if (fseek(pFile, lOffset, SEEK_SET) != 0) {
		return FALSE;
	}
	if (fread(aucBytes, sizeof(unsigned char), tMemb, pFile) != tMemb) {
		return FALSE;
	}
	return TRUE;
} /* end of bReadBytes */

/*
 * bReadBuffer
 * This function fills the given buffer with the given number of bytes,
 * starting at the given offset within the Big/Small Block Depot.
 *
 * Returns TRUE when successful, otherwise FALSE
 */
BOOL
bReadBuffer(FILE *pFile, long lStartBlock,
	const long *alBlockDepot, size_t tBlockDepotLen, size_t tBlockSize,
	unsigned char *aucBuffer, long lOffset, size_t tToRead)
{
	long	lBegin, lIndex;
	size_t	tLen;

	fail(pFile == NULL);
	fail(lStartBlock < 0);
	fail(alBlockDepot == NULL);
	fail(tBlockSize != BIG_BLOCK_SIZE && tBlockSize != SMALL_BLOCK_SIZE);
	fail(aucBuffer == NULL);
	fail(tToRead == 0);

	for (lIndex = lStartBlock;
	     lIndex != END_OF_CHAIN && tToRead != 0;
	     lIndex = alBlockDepot[lIndex]) {
		if (lIndex < 0 || lIndex >= (long)tBlockDepotLen) {
			if (tBlockSize >= BIG_BLOCK_SIZE) {
				werr(1, "The Big Block Depot is corrupt");
			} else {
				werr(1, "The Small Block Depot is corrupt");
			}
		}
		if (lOffset >= (long)tBlockSize) {
			lOffset -= (long)tBlockSize;
			continue;
		}
		lBegin = lDepotOffset(lIndex, tBlockSize) + lOffset;
		tLen = min(tBlockSize - (size_t)lOffset, tToRead);
		lOffset = 0;
		if (!bReadBytes(aucBuffer, tLen, lBegin, pFile)) {
			werr(0, "Read big block %ld not possible", lBegin);
			return FALSE;
		}
		aucBuffer += tLen;
		tToRead -= tLen;
	}
	DBG_DEC_C(tToRead != 0, tToRead);
	return tToRead == 0;
} /* end of bReadBuffer */

/*
 * Translate a Word colornumber into a true color for use in a drawfile
 *
 * Returns the true color
 */
unsigned int
uiColor2Color(int iWordColor)
{
	static const unsigned int	auiColorTable[] = {
		/*  0 */	0x00000000U,	/* Automatic */
		/*  1 */	0x00000000U,	/* Black */
		/*  2 */	0xff000000U,	/* Blue */
		/*  3 */	0xffff0000U,	/* Turquoise */
		/*  4 */	0x00ff0000U,	/* Bright Green */
		/*  5 */	0xff00ff00U,	/* Pink */
		/*  6 */	0x0000ff00U,	/* Red */
		/*  7 */	0x00ffff00U,	/* Yellow */
		/*  8 */	0xffffff00U,	/* White */
		/*  9 */	0x80000000U,	/* Dark Blue */
		/* 10 */	0x80800000U,	/* Teal */
		/* 11 */	0x00800000U,	/* Green */
		/* 12 */	0x80008000U,	/* Violet */
		/* 13 */	0x00008000U,	/* Dark Red */
		/* 14 */	0x00808000U,	/* Dark Yellow */
		/* 15 */	0x80808000U,	/* Gray 50% */
		/* 16 */	0xc0c0c000U,	/* Gray 25% */
	};
	if (iWordColor < 0 ||
	    iWordColor >= (int)elementsof(auiColorTable)) {
		return auiColorTable[0];
	}
	return auiColorTable[iWordColor];
} /* end of uiColor2Color */

/*
 * iFindSplit - find a place to split the string
 *
 * returns the index of the split character or -1 if no split found.
 */
static int
iFindSplit(const char *szString, int iStringLen)
{
	int	iSplit;

	iSplit = iStringLen - 1;
	while (iSplit >= 1) {
		if (szString[iSplit] == ' ' ||
		    (szString[iSplit] == '-' && szString[iSplit - 1] != ' ')) {
			return iSplit;
		}
		iSplit--;
	}
	return -1;
} /* end of iFindSplit */

/*
 * pSplitList - split the given list in a printable part and a leftover part
 *
 * returns the pointer to the leftover part
 */
output_type *
pSplitList(output_type *pAnchor)
{
	output_type	*pCurr, *pLeftOver;
	int		iIndex;

 	fail(pAnchor == NULL);

	for (pCurr = pAnchor; pCurr->pNext != NULL; pCurr = pCurr->pNext)
		;	/* EMPTY */
	iIndex = -1;
	for (; pCurr != NULL; pCurr = pCurr->pPrev) {
		iIndex = iFindSplit(pCurr->szStorage, pCurr->iNextFree);
		if (iIndex >= 0) {
			break;
		}
	}

	if (pCurr == NULL || iIndex < 0) {
		/* No split, no leftover */
		return NULL;
	}
	/* Split over the iIndex-th character */
	NO_DBG_MSG("pLeftOver");
	pLeftOver = xmalloc(sizeof(*pLeftOver));
	pLeftOver->tStorageSize = (size_t)(pCurr->iNextFree - iIndex);
	pLeftOver->szStorage = xmalloc(pLeftOver->tStorageSize);
	pLeftOver->iNextFree = pCurr->iNextFree - iIndex - 1;
	(void)strncpy(pLeftOver->szStorage,
		pCurr->szStorage + iIndex + 1, (size_t)pLeftOver->iNextFree);
	pLeftOver->szStorage[pLeftOver->iNextFree] = '\0';
	NO_DBG_MSG(pLeftOver->szStorage);
	pLeftOver->iColor = pCurr->iColor;
	pLeftOver->ucFontstyle = pCurr->ucFontstyle;
	pLeftOver->tFontRef = pCurr->tFontRef;
	pLeftOver->sFontsize = pCurr->sFontsize;
	pLeftOver->lStringWidth = lComputeStringWidth(
					pLeftOver->szStorage,
					pLeftOver->iNextFree,
					pLeftOver->tFontRef,
					pLeftOver->sFontsize);
	pLeftOver->pPrev = NULL;
	pLeftOver->pNext = pCurr->pNext;
	if (pLeftOver->pNext != NULL) {
		pLeftOver->pNext->pPrev = pLeftOver;
	}
	fail(!bCheckDoubleLinkedList(pLeftOver));

	NO_DBG_MSG("pAnchor");
	NO_DBG_HEX(pCurr->szStorage[iIndex]);
	while (iIndex >= 0 && isspace(pCurr->szStorage[iIndex])) {
		iIndex--;
	}
	pCurr->iNextFree = iIndex + 1;
	pCurr->szStorage[pCurr->iNextFree] = '\0';
	NO_DBG_MSG(pCurr->szStorage);
	pCurr->lStringWidth = lComputeStringWidth(
					pCurr->szStorage,
					pCurr->iNextFree,
					pCurr->tFontRef,
					pCurr->sFontsize);
	pCurr->pNext = NULL;
	fail(!bCheckDoubleLinkedList(pAnchor));

	return pLeftOver;
} /* end of pSplitList */

/*
 * iInteger2Roman - convert an integer to Roman Numerals
 *
 * returns the number of characters written
 */
int
iInteger2Roman(int iNumber, BOOL bUpperCase, char *szOutput)
{
	char *outp, *p, *q;
	int iNextVal, iValue;

	fail(szOutput == NULL);

	if (iNumber <= 0 || iNumber >= 4000) {
		szOutput[0] = '\0';
		return 0;
	}

	outp = szOutput;
	p = bUpperCase ? "M\2D\5C\2L\5X\2V\5I" : "m\2d\5c\2l\5x\2v\5i";
	iValue = 1000;
	for (;;) {
		while (iNumber >= iValue) {
			*outp++ = *p;
			iNumber -= iValue;
		}
		if (iNumber <= 0) {
			*outp = '\0';
			return outp - szOutput;
		}
		q = p + 1;
		iNextVal = iValue / (int)*q;
		if ((int)*q == 2) {		/* magic */
			iNextVal /= (int)*(q += 2);
		}
		if (iNumber + iNextVal >= iValue) {
			*outp++ = *++q;
			iNumber += iNextVal;
		} else {
			p++;
			iValue /= (int)(*p++);
		}
	}
} /* end of iInteger2Roman */

/*
 * iInteger2Alpha - convert an integer to Alphabetic "numbers"
 *
 * returns the number of characters written
 */
int
iInteger2Alpha(int iNumber, BOOL bUpperCase, char *szOutput)
{
	char	*outp;
	int	iTmp;

	fail(szOutput == NULL);

	outp = szOutput;
	iTmp = bUpperCase ? 'A': 'a';
	if (iNumber <= 26) {
		iNumber -= 1;
		*outp++ = (char)(iTmp + iNumber);
	} else if (iNumber <= 26 + 26*26) {
		iNumber -= 26 + 1;
		*outp++ = (char)(iTmp + iNumber / 26);
		*outp++ = (char)(iTmp + iNumber % 26);
	} else if (iNumber <= 26 + 26*26 + 26*26*26) {
		iNumber -= 26 + 26*26 + 1;
		*outp++ = (char)(iTmp + iNumber / (26*26));
		*outp++ = (char)(iTmp + iNumber / 26 % 26);
		*outp++ = (char)(iTmp + iNumber % 26);
	}
	*outp = '\0';
	return outp - szOutput;
} /* end of iInteger2Alpha */

/*
 * unincpy - copy a counted Unicode string to an single-byte string
 */
char *
unincpy(char *s1, const char *s2, size_t n)
{
	char		*dest;
	unsigned long	ulChar;
	size_t		len;
	unsigned short	usUni;

	for (dest = s1, len = 0; len < n; dest++, len++) {
		usUni = usGetWord(len * 2, s2);
		if (usUni == 0) {
			break;
		}
		ulChar = ulTranslateCharacters(usUni, 0, FALSE, FALSE);
		if (ulChar == IGNORE_CHARACTER) {
			ulChar = (unsigned long)'?';
		}
		*dest = (char)ulChar;
	}
	for (; len < n; len++) {
		*dest++ = '\0';
	}
	return s1;
} /* end of unincpy */

/*
 * unilen - calculate the length of a Unicode string
 */
size_t
unilen(const char *s)
{
	size_t		tLen;
	unsigned short	usUni;

	tLen = 0;
	for (;;) {
		usUni = usGetWord(tLen * 2, s);
		if (usUni == 0) {
			return tLen;
		}
		tLen += 2;
	}
} /* end of unilen */

/*
 * szBaseName - get the basename of the given filename
 */
const char *
szBasename(const char *szFilename)
{
	const char	*szTmp;

	fail(szFilename == NULL);

	if (szFilename == NULL || szFilename[0] == '\0') {
		return "null";
	}

	szTmp = strrchr(szFilename, FILE_SEPARATOR[0]);
	if (szTmp == NULL) {
		return szFilename;
	}
	return ++szTmp;
} /* end of szBasename */

/*
 * iGetVersionNumber - get the Word version number from the header
 *
 * Returns the version number or -1 when unknown
 */
int
iGetVersionNumber(const unsigned char *aucHeader)
{
	unsigned short  usFib, usChse;

	usFib = usGetWord(0x02, aucHeader);
	DBG_DEC(usFib);
	if (usFib < 101) {
		/* This file is from a version of Word older than Word 6 */
		return -1;
	}
	usChse = usGetWord(0x14, aucHeader);
	DBG_DEC(usChse);
	bWord6MacFile = FALSE;
	switch (usFib) {
	case 101:
	case 102:
		DBG_MSG("Word 6 for Windows");
		return 6;
	case 103:
	case 104:
		switch (usChse) {
		case 0:
			DBG_MSG("Word 7 for Win95");
			return 7;
		case 256:
			DBG_MSG("Word 6 for Macintosh");
			bWord6MacFile = TRUE;
			return 6;
		default:
			DBG_FIXME();
			if (ucGetByte(0x05, aucHeader) == 0xe0) {
				DBG_MSG("Word 7 for Win95");
				return 7;
			}
			DBG_MSG("Word 6 for Macintosh");
			bWord6MacFile = TRUE;
			return 6;
		}
	default: 
		DBG_MSG_C(usChse != 256, "Word97 for Win95/98/NT");
		DBG_MSG_C(usChse == 256, "Word98 for Macintosh");
		return 8;
	}
} /* end of iGetVersionNumber */

/*
 * TRUE if the current file was made by Word 6 on an Apple Macintosh,
 * otherwise FALSE.
 * This function hides the methode of how to find out from the rest of the
 * program.
 */
BOOL
bIsWord6MacFile(void)
{
	return bWord6MacFile;
} /* end of bIsWord6MacFile */

/*
 * lComputeLeading - compute the leading
 *
 * NOTE: the fontsize is given in half points
 *
 * Returns the leading in drawunits
 */
long
lComputeLeading(int iFontsize)
{
	long	lLeading;

	lLeading = iFontsize * 500L;
	if (iFontsize < 18) {		/* Small text: 112% */
		lLeading *= 112;
	} else if (iFontsize < 28) {	/* Normal text: 124% */
		lLeading *= 124;
	} else if (iFontsize < 48) {	/* Small headlines: 104% */
		lLeading *= 104;
	} else {			/* Large headlines: 100% */
		lLeading *= 100;
	}
	lLeading = lMilliPoints2DrawUnits(lLeading);
	lLeading += 50;
	lLeading /= 100;
	return lLeading;
} /* end of lComputeLeading */
