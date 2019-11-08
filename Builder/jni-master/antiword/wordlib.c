/*
 * wordlib.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Deal with the internals of a MS Word file
 */

#include "antiword.h"

static BOOL	bOldMacFile = FALSE;


/*
 * Common part of the file checking functions
 */
static BOOL
bCheckBytes(FILE *pFile, const UCHAR *aucBytes, size_t tBytes)
{
	int	iIndex, iChar;

	fail(pFile == NULL || aucBytes == NULL || tBytes == 0);

    aw_rewind(pFile);

	for (iIndex = 0; iIndex < (int)tBytes; iIndex++) {
        iChar = aw_getc(pFile);
		if (iChar == EOF || iChar != (int)aucBytes[iIndex]) {
			NO_DBG_HEX(iChar);
			NO_DBG_HEX(aucBytes[iIndex]);
			return FALSE;
		}
	}
	return TRUE;
} /* end of bCheckBytes */

/*
 * This function checks whether the given file is or is not a "Word for DOS"
 * document
 */
BOOL
bIsWordForDosFile(FILE *pFile, long lFilesize)
{
	static UCHAR	aucBytes[] =
		{ 0x31, 0xbe, 0x00, 0x00, 0x00, 0xab };	/* Word for DOS */

	DBG_MSG("bIsWordForDosFile");

	if (pFile == NULL || lFilesize < 0) {
		DBG_MSG("No proper file given");
		return FALSE;
	}
	if (lFilesize < 128) {
		DBG_MSG("File too small to be a Word document");
		return FALSE;
	}
	return bCheckBytes(pFile, aucBytes, elementsof(aucBytes));
} /* end of bIsWordForDosFile */

/*
 * This function checks whether the given file is or is not a file with an
 * OLE envelope (That is a document made by Word 6 or later)
 */
static BOOL
bIsWordFileWithOLE(FILE *pFile, long lFilesize)
{
	static UCHAR	aucBytes[] =
		{ 0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1 };
	int	iTailLen;

	if (pFile == NULL || lFilesize < 0) {
		DBG_MSG("No proper file given");
		return FALSE;
	}
	if (lFilesize < (long)BIG_BLOCK_SIZE * 3) {
		DBG_MSG("This file is too small to be a Word document");
		return FALSE;
	}

	iTailLen = (int)(lFilesize % BIG_BLOCK_SIZE);
	switch (iTailLen) {
	case 0:		/* No tail, as it should be */
		break;
	case 1:
	case 2:		/* Filesize mismatch or a buggy email program */
		if ((int)(lFilesize % 3) == iTailLen) {
			DBG_DEC(lFilesize);
			return FALSE;
		}
		/*
		 * Ignore extra bytes caused by buggy email programs.
		 * They have bugs in their base64 encoding or decoding.
		 * 3 bytes -> 4 ascii chars -> 3 bytes
		 */
		DBG_MSG("Document with extra bytes");
		break;
	default:	/* Wrong filesize for a Word document */
		DBG_DEC(lFilesize);
		DBG_DEC(iTailLen);
		return FALSE;
	}
	return bCheckBytes(pFile, aucBytes, elementsof(aucBytes));
} /* end of bIsWordFileWithOLE */

/*
 * This function checks whether the given file is or is not a RTF document
 */
BOOL
bIsRtfFile(FILE *pFile)
{
	static UCHAR	aucBytes[] =
		{ '{', '\\', 'r', 't', 'f', '1' };

	DBG_MSG("bIsRtfFile");

	return bCheckBytes(pFile, aucBytes, elementsof(aucBytes));
} /* end of bIsRtfFile */

/*
 * This function checks whether the given file is or is not a WP document
 */
BOOL
bIsWordPerfectFile(FILE *pFile)
{
	static UCHAR	aucBytes[] =
		{ 0xff, 'W', 'P', 'C' };

	DBG_MSG("bIsWordPerfectFile");

	return bCheckBytes(pFile, aucBytes, elementsof(aucBytes));
} /* end of bIsWordPerfectFile */

/*
 * This function checks whether the given file is or is not a "Win Word 1 or 2"
 * document
 */
BOOL
bIsWinWord12File(FILE *pFile, long lFilesize)
{
	static UCHAR	aucBytes[2][4] = {
		{ 0x9b, 0xa5, 0x21, 0x00 },	/* Win Word 1.x */
		{ 0xdb, 0xa5, 0x2d, 0x00 },	/* Win Word 2.0 */
	};
	int	iIndex;

	DBG_MSG("bIsWinWord12File");

	if (pFile == NULL || lFilesize < 0) {
		DBG_MSG("No proper file given");
		return FALSE;
	}
	if (lFilesize < 384) {
		DBG_MSG("This file is too small to be a Word document");
		return FALSE;
	}

	for (iIndex = 0; iIndex < (int)elementsof(aucBytes); iIndex++) {
		if (bCheckBytes(pFile,
				aucBytes[iIndex],
				elementsof(aucBytes[iIndex]))) {
			return TRUE;
		}
	}
	return FALSE;
} /* end of bIsWinWord12File */

/*
 * This function checks whether the given file is or is not a "Mac Word 4 or 5"
 * document
 */
BOOL
bIsMacWord45File(FILE *pFile)
{
	static UCHAR	aucBytes[2][6] = {
		{ 0xfe, 0x37, 0x00, 0x1c, 0x00, 0x00 },	/* Mac Word 4 */
		{ 0xfe, 0x37, 0x00, 0x23, 0x00, 0x00 },	/* Mac Word 5 */
	};
	int	iIndex;

	DBG_MSG("bIsMacWord45File");

	for (iIndex = 0; iIndex < (int)elementsof(aucBytes); iIndex++) {
		if (bCheckBytes(pFile,
				aucBytes[iIndex],
				elementsof(aucBytes[iIndex]))) {
			return TRUE;
		}
	}
	return FALSE;
} /* end of bIsMacWord45File */

/*
 * iGuessVersionNumber - guess the Word version number from first few bytes
 *
 * Returns the guessed version number or -1 when no guess it possible
 */
int
iGuessVersionNumber(FILE *pFile, long lFilesize)
{
	if(bIsWordForDosFile(pFile, lFilesize)) {
		return 0;
	}
	if (bIsWinWord12File(pFile, lFilesize)) {
		return 2;
	}
	if (bIsMacWord45File(pFile)) {
		return 5;
	}
	if (bIsWordFileWithOLE(pFile, lFilesize)) {
		return 6;
	}
	return -1;
} /* end of iGuessVersionNumber */

/*
 * iGetVersionNumber - get the Word version number from the header
 *
 * Returns the version number or -1 when unknown
 */
int
iGetVersionNumber(const UCHAR *aucHeader)
{
	USHORT	usFib, usChse;

	usFib = usGetWord(0x02, aucHeader);
	if (usFib >= 0x1000) {
		/* To big: must be MacWord using Big Endian */
		DBG_HEX(usFib);
		usFib = usGetWordBE(0x02, aucHeader);
	}
	DBG_DEC(usFib);
	bOldMacFile = FALSE;
	switch (usFib) {
	case   0:
		DBG_MSG("Word for DOS");
		return 0;
	case  28:
		DBG_MSG("Word 4 for Macintosh");
		bOldMacFile = TRUE;
		return 4;
	case  33:
		DBG_MSG("Word 1.x for Windows");
		return 1;
	case  35:
		DBG_MSG("Word 5 for Macintosh");
		bOldMacFile = TRUE;
		return 5;
	case  45:
		DBG_MSG("Word 2 for Windows");
		return 2;
	case 101:
	case 102:
		DBG_MSG("Word 6 for Windows");
		return 6;
	case 103:
	case 104:
		usChse = usGetWord(0x14, aucHeader);
		DBG_DEC(usChse);
		switch (usChse) {
		case 0:
			DBG_MSG("Word 7 for Win95");
			return 7;
		case 256:
			DBG_MSG("Word 6 for Macintosh");
			bOldMacFile = TRUE;
			return 6;
		default:
			DBG_FIXME();
			if ((int)ucGetByte(0x05, aucHeader) == 0xe0) {
				DBG_MSG("Word 7 for Win95");
				return 7;
			}
			DBG_MSG("Word 6 for Macintosh");
			bOldMacFile = TRUE;
			return 6;
		}
	default:
		usChse = usGetWord(0x14, aucHeader);
		DBG_DEC(usChse);
		if (usFib < 192) {
			/* Unknown or unsupported version of Word */
			DBG_DEC(usFib);
			return -1;
		}
		DBG_MSG_C(usChse != 256, "Word97 for Win95/98/NT");
		DBG_MSG_C(usChse == 256, "Word98 for Macintosh");
		return 8;
	}
} /* end of iGetVersionNumber */

/*
 * TRUE if the current file was made by Word version 6 or older on an
 * Apple Macintosh, otherwise FALSE.
 * This function hides the methode of how to find out from the rest of the
 * program.
 */
BOOL
bIsOldMacFile(void)
{
	return bOldMacFile;
} /* end of bIsOldMacFile */

/*
 * iInitDocument - initialize a document
 *
 * Returns the version of Word that made the document or -1
 */
int
iInitDocument(FILE *pFile, long lFilesize)
{
	int	iGuess, iWordVersion;

	iGuess = iGuessVersionNumber(pFile, lFilesize);
	switch (iGuess) {
	case 0:
		iWordVersion = iInitDocumentDOS(pFile, lFilesize);
		break;
	case 2:
		iWordVersion = iInitDocumentWIN(pFile, lFilesize);
		break;
	case 5:
		iWordVersion = iInitDocumentMAC(pFile, lFilesize);
		break;
	case 6:
		iWordVersion = iInitDocumentOLE(pFile, lFilesize);
		break;
	default:
		DBG_DEC(iGuess);
		iWordVersion = -1;
		break;
	}
	return iWordVersion;
} /* end of iInitDocument */

/*
 * vFreeDocument - free a document by free-ing its parts
 */
void
vFreeDocument(void)
{
	DBG_MSG("vFreeDocument");

	/* Free the memory */
	vDestroyTextBlockList();
	vDestroyDataBlockList();
	vDestroyListInfoList();
	vDestroyRowInfoList();
	vDestroyStyleInfoList();
	vDestroyFontInfoList();
	vDestroyStylesheetList();
	vDestroyPictInfoList();
	vDestroyDocumentInfoList();
	vDestroySectionInfoList();
	vDestroyHdrFtrInfoList();
	vDestroyPropModList();
	vDestroyNotesInfoLists();
	vDestroyFontTable();
	vDestroySummaryInfo();
} /* end of vFreeDocument */
