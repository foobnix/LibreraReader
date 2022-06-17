/*
 * wordole.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GPL
 *
 * Description:
 * Deal with the OLE internals of a MS Word file
 */

#include <string.h>
#include "antiword.h"

/* Private type for Property Set Storage entries */
typedef struct pps_entry_tag {
	ULONG	ulNext;
	ULONG	ulPrevious;
	ULONG	ulDir;
	ULONG	ulSB;
	ULONG	ulSize;
	int	iLevel;
	char	szName[32];
	UCHAR	ucType;
} pps_entry_type;

/* Show that a PPS number or index should not be used */
#define PPS_NUMBER_INVALID	0xffffffffUL


/* Macro to make sure all such statements will be identical */
#define FREE_ALL()		\
	do {\
		vDestroySmallBlockList();\
		aulRootList = xfree(aulRootList);\
		aulSbdList = xfree(aulSbdList);\
		aulBbdList = xfree(aulBbdList);\
		aulSBD = xfree(aulSBD);\
		aulBBD = xfree(aulBBD);\
	} while(0)


/*
 * ulReadLong - read four bytes from the given file and offset
 */
static ULONG
ulReadLong(FILE *pFile, ULONG ulOffset)
{
	UCHAR	aucBytes[4];

	fail(pFile == NULL);

	if (!bReadBytes(aucBytes, 4, ulOffset, pFile)) {
		werr(1, "Read long 0x%lx not possible", ulOffset);
	}
	return ulGetLong(0, aucBytes);
} /* end of ulReadLong */

/*
 * vName2String - turn the name into a proper string.
 */
static void
vName2String(char *szName, const UCHAR *aucBytes, size_t tNameSize)
{
	char	*pcChar;
	size_t	tIndex;

	fail(aucBytes == NULL || szName == NULL);

	if (tNameSize < 2) {
		szName[0] = '\0';
		return;
	}
	for (tIndex = 0, pcChar = szName;
	     tIndex < 2 * tNameSize;
	     tIndex += 2, pcChar++) {
		*pcChar = (char)aucBytes[tIndex];
	}
	szName[tNameSize - 1] = '\0';
} /* end of vName2String */

/*
 * tReadBlockIndices - read the Big/Small Block Depot indices
 *
 * Returns the number of indices read
 */
static size_t
tReadBlockIndices(FILE *pFile, ULONG *aulBlockDepot,
	size_t tMaxRec, ULONG ulOffset)
{
	size_t	tDone;
	int	iIndex;
	UCHAR	aucBytes[BIG_BLOCK_SIZE];

	fail(pFile == NULL || aulBlockDepot == NULL);
	fail(tMaxRec == 0);

	/* Read a big block with BBD or SBD indices */
	if (!bReadBytes(aucBytes, BIG_BLOCK_SIZE, ulOffset, pFile)) {
		werr(0, "Reading big block from 0x%lx is not possible",
			ulOffset);
		return 0;
	}
	/* Split the big block into indices, an index is four bytes */
	tDone = min(tMaxRec, (size_t)BIG_BLOCK_SIZE / 4);
	for (iIndex = 0; iIndex < (int)tDone; iIndex++) {
		aulBlockDepot[iIndex] = ulGetLong(4 * iIndex, aucBytes);
		NO_DBG_DEC(aulBlockDepot[iIndex]);
	}
	return tDone;
} /* end of tReadBlockIndices */

/*
 * bGetBBD - get the Big Block Depot indices from the index-blocks
 */
static BOOL
bGetBBD(FILE *pFile, const ULONG *aulDepot, size_t tDepotLen,
	ULONG *aulBBD, size_t tBBDLen)
{
	ULONG	ulBegin;
	size_t	tToGo, tDone;
	int	iIndex;

	fail(pFile == NULL || aulDepot == NULL || aulBBD == NULL);

	DBG_MSG("bGetBBD");

	tToGo = tBBDLen;
	for (iIndex = 0; iIndex < (int)tDepotLen && tToGo != 0; iIndex++) {
		ulBegin = (aulDepot[iIndex] + 1) * BIG_BLOCK_SIZE;
		NO_DBG_HEX(ulBegin);
		tDone = tReadBlockIndices(pFile, aulBBD, tToGo, ulBegin);
		fail(tDone > tToGo);
		if (tDone == 0) {
			return FALSE;
		}
		aulBBD += tDone;
		tToGo -= tDone;
	}
	return tToGo == 0;
} /* end of bGetBBD */

/*
 * bGetSBD - get the Small Block Depot indices from the index-blocks
 */
static BOOL
bGetSBD(FILE *pFile, const ULONG *aulDepot, size_t tDepotLen,
	ULONG *aulSBD, size_t tSBDLen)
{
	ULONG	ulBegin;
	size_t	tToGo, tDone;
	int	iIndex;

	fail(pFile == NULL || aulDepot == NULL || aulSBD == NULL);

	DBG_MSG("bGetSBD");

	tToGo = tSBDLen;
	for (iIndex = 0; iIndex < (int)tDepotLen && tToGo != 0; iIndex++) {
		fail(aulDepot[iIndex] >= ULONG_MAX / BIG_BLOCK_SIZE);
		ulBegin = (aulDepot[iIndex] + 1) * BIG_BLOCK_SIZE;
		NO_DBG_HEX(ulBegin);
		tDone = tReadBlockIndices(pFile, aulSBD, tToGo, ulBegin);
		fail(tDone > tToGo);
		if (tDone == 0) {
			return FALSE;
		}
		aulSBD += tDone;
		tToGo -= tDone;
	}
	return tToGo == 0;
} /* end of bGetSBD */

/*
 * vComputePPSlevels - compute the levels of the Property Set Storage entries
 */
static void
vComputePPSlevels(pps_entry_type *atPPSlist, pps_entry_type *pNode,
			int iLevel, int iRecursionLevel)
{
	fail(atPPSlist == NULL || pNode == NULL);
	fail(iLevel < 0 || iRecursionLevel < 0);

	if (iRecursionLevel > 25) {
		/* This removes the possibility of an infinite recursion */
		DBG_DEC(iRecursionLevel);
		return;
	}
	if (pNode->iLevel <= iLevel) {
		/* Avoid entering a loop */
		DBG_DEC(iLevel);
		DBG_DEC(pNode->iLevel);
		return;
	}

	pNode->iLevel = iLevel;

	if (pNode->ulDir != PPS_NUMBER_INVALID) {
		vComputePPSlevels(atPPSlist,
				&atPPSlist[pNode->ulDir],
				iLevel + 1,
				iRecursionLevel + 1);
	}
	if (pNode->ulNext != PPS_NUMBER_INVALID) {
		vComputePPSlevels(atPPSlist,
				&atPPSlist[pNode->ulNext],
				iLevel,
				iRecursionLevel + 1);
	}
	if (pNode->ulPrevious != PPS_NUMBER_INVALID) {
		vComputePPSlevels(atPPSlist,
				&atPPSlist[pNode->ulPrevious],
				iLevel,
				iRecursionLevel + 1);
	}
} /* end of vComputePPSlevels */

/*
 * bGetPPS - search the Property Set Storage for three sets
 *
 * Return TRUE if the WordDocument PPS is found
 */
static BOOL
bGetPPS(FILE *pFile,
	const ULONG *aulRootList, size_t tRootListLen, pps_info_type *pPPS)
{
	pps_entry_type	*atPPSlist;
	ULONG	ulBegin, ulOffset, ulTmp;
	size_t	tNbrOfPPS, tNameSize;
	int	iIndex, iStartBlock, iRootIndex;
	BOOL	bWord, bExcel;
	UCHAR	aucBytes[PROPERTY_SET_STORAGE_SIZE];

	fail(pFile == NULL || aulRootList == NULL || pPPS == NULL);

	DBG_MSG("bGetPPS");

	NO_DBG_DEC(tRootListLen);

	bWord = FALSE;
	bExcel = FALSE;
	(void)memset(pPPS, 0, sizeof(*pPPS));

	/* Read and store all the Property Set Storage entries */

	tNbrOfPPS = tRootListLen * BIG_BLOCK_SIZE / PROPERTY_SET_STORAGE_SIZE;
	atPPSlist = xcalloc(tNbrOfPPS, sizeof(pps_entry_type));
	iRootIndex = 0;

	for (iIndex = 0; iIndex < (int)tNbrOfPPS; iIndex++) {
		ulTmp = (ULONG)iIndex * PROPERTY_SET_STORAGE_SIZE;
		iStartBlock = (int)(ulTmp / BIG_BLOCK_SIZE);
		ulOffset = ulTmp % BIG_BLOCK_SIZE;
		ulBegin = (aulRootList[iStartBlock] + 1) * BIG_BLOCK_SIZE +
				ulOffset;
		NO_DBG_HEX(ulBegin);
		if (!bReadBytes(aucBytes, PROPERTY_SET_STORAGE_SIZE,
							ulBegin, pFile)) {
			werr(0, "Reading PPS %d is not possible", iIndex);
			atPPSlist = xfree(atPPSlist);
			return FALSE;
		}
		tNameSize = (size_t)usGetWord(0x40, aucBytes);
		tNameSize = (tNameSize + 1) / 2;
		vName2String(atPPSlist[iIndex].szName, aucBytes, tNameSize);
		atPPSlist[iIndex].ucType = ucGetByte(0x42, aucBytes);
		if (atPPSlist[iIndex].ucType == 5) {
			iRootIndex = iIndex;
		}
		atPPSlist[iIndex].ulPrevious = ulGetLong(0x44, aucBytes);
		atPPSlist[iIndex].ulNext = ulGetLong(0x48, aucBytes);
		atPPSlist[iIndex].ulDir = ulGetLong(0x4c, aucBytes);
		atPPSlist[iIndex].ulSB = ulGetLong(0x74, aucBytes);
		atPPSlist[iIndex].ulSize = ulGetLong(0x78, aucBytes);
		atPPSlist[iIndex].iLevel = INT_MAX;
		if ((atPPSlist[iIndex].ulPrevious >= (ULONG)tNbrOfPPS &&
		     atPPSlist[iIndex].ulPrevious != PPS_NUMBER_INVALID) ||
		    (atPPSlist[iIndex].ulNext >= (ULONG)tNbrOfPPS &&
		     atPPSlist[iIndex].ulNext != PPS_NUMBER_INVALID) ||
		    (atPPSlist[iIndex].ulDir >= (ULONG)tNbrOfPPS &&
		     atPPSlist[iIndex].ulDir != PPS_NUMBER_INVALID)) {
			DBG_DEC(iIndex);
			DBG_DEC(atPPSlist[iIndex].ulPrevious);
			DBG_DEC(atPPSlist[iIndex].ulNext);
			DBG_DEC(atPPSlist[iIndex].ulDir);
			DBG_DEC(tNbrOfPPS);
			werr(0, "The Property Set Storage is damaged");
			atPPSlist = xfree(atPPSlist);
			return FALSE;
		}
	}

#if 0 /* defined(DEBUG) */
	DBG_MSG("Before");
	for (iIndex = 0; iIndex < (int)tNbrOfPPS; iIndex++) {
		DBG_MSG(atPPSlist[iIndex].szName);
		DBG_HEX(atPPSlist[iIndex].ulDir);
		DBG_HEX(atPPSlist[iIndex].ulPrevious);
		DBG_HEX(atPPSlist[iIndex].ulNext);
		DBG_DEC(atPPSlist[iIndex].ulSB);
		DBG_HEX(atPPSlist[iIndex].ulSize);
		DBG_DEC(atPPSlist[iIndex].iLevel);
	}
#endif /* DEBUG */

	/* Add level information to each entry */
	vComputePPSlevels(atPPSlist, &atPPSlist[iRootIndex], 0, 0);

	/* Check the entries on level 1 for the required information */
	NO_DBG_MSG("After");
	for (iIndex = 0; iIndex < (int)tNbrOfPPS; iIndex++) {
#if 0 /* defined(DEBUG) */
		DBG_MSG(atPPSlist[iIndex].szName);
		DBG_HEX(atPPSlist[iIndex].ulDir);
		DBG_HEX(atPPSlist[iIndex].ulPrevious);
		DBG_HEX(atPPSlist[iIndex].ulNext);
		DBG_DEC(atPPSlist[iIndex].ulSB);
		DBG_HEX(atPPSlist[iIndex].ulSize);
		DBG_DEC(atPPSlist[iIndex].iLevel);
#endif /* DEBUG */
		if (atPPSlist[iIndex].iLevel != 1 ||
		    atPPSlist[iIndex].ucType != 2 ||
		    atPPSlist[iIndex].szName[0] == '\0' ||
		    atPPSlist[iIndex].ulSize == 0) {
			/* This entry can be ignored */
			continue;
		}
		if (pPPS->tWordDocument.ulSize == 0 &&
		    STREQ(atPPSlist[iIndex].szName, "WordDocument")) {
			pPPS->tWordDocument.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->tWordDocument.ulSize = atPPSlist[iIndex].ulSize;
			bWord = TRUE;
		} else if (pPPS->tData.ulSize == 0 &&
			   STREQ(atPPSlist[iIndex].szName, "Data")) {
			pPPS->tData.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->tData.ulSize = atPPSlist[iIndex].ulSize;
		} else if (pPPS->t0Table.ulSize == 0 &&
			   STREQ(atPPSlist[iIndex].szName, "0Table")) {
			pPPS->t0Table.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->t0Table.ulSize = atPPSlist[iIndex].ulSize;
		} else if (pPPS->t1Table.ulSize == 0 &&
			   STREQ(atPPSlist[iIndex].szName, "1Table")) {
			pPPS->t1Table.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->t1Table.ulSize = atPPSlist[iIndex].ulSize;
		} else if (pPPS->tSummaryInfo.ulSize == 0 &&
			   STREQ(atPPSlist[iIndex].szName,
						"\005SummaryInformation")) {
			pPPS->tSummaryInfo.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->tSummaryInfo.ulSize = atPPSlist[iIndex].ulSize;
		} else if (pPPS->tDocSummaryInfo.ulSize == 0 &&
			   STREQ(atPPSlist[iIndex].szName,
					"\005DocumentSummaryInformation")) {
			pPPS->tDocSummaryInfo.ulSB = atPPSlist[iIndex].ulSB;
			pPPS->tDocSummaryInfo.ulSize = atPPSlist[iIndex].ulSize;
		} else if (STREQ(atPPSlist[iIndex].szName, "Book") ||
			   STREQ(atPPSlist[iIndex].szName, "Workbook")) {
			bExcel = TRUE;
		}
	}

	/* Free the space for the Property Set Storage entries */
	atPPSlist = xfree(atPPSlist);

	/* Draw your conclusions */
	if (bWord) {
		return TRUE;
	}

	if (bExcel) {
		werr(0, "Sorry, but this is an Excel spreadsheet");
	} else {
		werr(0, "This OLE file does not contain a Word document");
	}
	return FALSE;
} /* end of bGetPPS */

/*
 * vGetBbdList - make a list of the places to find big blocks
 */
static void
vGetBbdList(FILE *pFile, int iNbr, ULONG *aulBbdList, ULONG ulOffset)
{
	int	iIndex;

	fail(pFile == NULL);
	fail(iNbr > 127);
	fail(aulBbdList == NULL);

	NO_DBG_DEC(iNbr);
	for (iIndex = 0; iIndex < iNbr; iIndex++) {
                aulBbdList[iIndex] =
                        ulReadLong(pFile, ulOffset + 4 * (ULONG)iIndex);
		NO_DBG_DEC(iIndex);
                NO_DBG_HEX(aulBbdList[iIndex]);
        }
} /* end of vGetBbdList */

/*
 * bGetDocumentText - make a list of the text blocks of a Word document
 *
 * Return TRUE when succesful, otherwise FALSE
 */
static BOOL
bGetDocumentText(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader, int iWordVersion)
{
	ULONG	ulBeginOfText;
	ULONG	ulTextLen, ulFootnoteLen, ulEndnoteLen;
	ULONG	ulHdrFtrLen, ulMacroLen, ulAnnotationLen;
	ULONG	ulTextBoxLen, ulHdrTextBoxLen;
	UINT	uiQuickSaves;
	BOOL	bFarEastWord, bTemplate, bFastSaved, bEncrypted, bSuccess;
	USHORT	usIdent, usDocStatus;

	fail(pFile == NULL || pPPS == NULL);
	fail(aulBBD == NULL);
	fail(aulSBD == NULL);

	DBG_MSG("bGetDocumentText");

	/* Get the "magic number" from the header */
	usIdent = usGetWord(0x00, aucHeader);
	DBG_HEX(usIdent);
	bFarEastWord = usIdent == 0x8098 || usIdent == 0x8099 ||
			usIdent == 0xa697 || usIdent == 0xa699;
	/* Get the status flags from the header */
	usDocStatus = usGetWord(0x0a, aucHeader);
	DBG_HEX(usDocStatus);
	bTemplate = (usDocStatus & BIT(0)) != 0;
	DBG_MSG_C(bTemplate, "This document is a Template");
	bFastSaved = (usDocStatus & BIT(2)) != 0;
	uiQuickSaves = (UINT)(usDocStatus & 0x00f0) >> 4;
	DBG_MSG_C(bFastSaved, "This document is Fast Saved");
	DBG_DEC_C(bFastSaved, uiQuickSaves);
	bEncrypted = (usDocStatus & BIT(8)) != 0;
	if (bEncrypted) {
		werr(0, "Encrypted documents are not supported");
		return FALSE;
	}

	/* Get length information */
	ulBeginOfText = ulGetLong(0x18, aucHeader);
	DBG_HEX(ulBeginOfText);
	switch (iWordVersion) {
	case 6:
	case 7:
		ulTextLen = ulGetLong(0x34, aucHeader);
		ulFootnoteLen = ulGetLong(0x38, aucHeader);
		ulHdrFtrLen = ulGetLong(0x3c, aucHeader);
		ulMacroLen = ulGetLong(0x40, aucHeader);
		ulAnnotationLen = ulGetLong(0x44, aucHeader);
		ulEndnoteLen = ulGetLong(0x48, aucHeader);
		ulTextBoxLen = ulGetLong(0x4c, aucHeader);
		ulHdrTextBoxLen = ulGetLong(0x50, aucHeader);
		break;
	case 8:
		ulTextLen = ulGetLong(0x4c, aucHeader);
		ulFootnoteLen = ulGetLong(0x50, aucHeader);
		ulHdrFtrLen = ulGetLong(0x54, aucHeader);
		ulMacroLen = ulGetLong(0x58, aucHeader);
		ulAnnotationLen = ulGetLong(0x5c, aucHeader);
		ulEndnoteLen = ulGetLong(0x60, aucHeader);
		ulTextBoxLen = ulGetLong(0x64, aucHeader);
		ulHdrTextBoxLen = ulGetLong(0x68, aucHeader);
		break;
	default:
		werr(0, "This version of Word is not supported");
		return FALSE;
	}
	DBG_DEC(ulTextLen);
	DBG_DEC(ulFootnoteLen);
	DBG_DEC(ulHdrFtrLen);
	DBG_DEC(ulMacroLen);
	DBG_DEC(ulAnnotationLen);
	DBG_DEC(ulEndnoteLen);
	DBG_DEC(ulTextBoxLen);
	DBG_DEC(ulHdrTextBoxLen);

	/* Make a list of the text blocks */
	switch (iWordVersion) {
	case 6:
	case 7:
		if (bFastSaved) {
			bSuccess = bGet6DocumentText(pFile,
					bFarEastWord,
					pPPS->tWordDocument.ulSB,
					aulBBD, tBBDLen,
					aucHeader);
		} else {
		  	bSuccess = bAddTextBlocks(ulBeginOfText,
				ulTextLen +
				ulFootnoteLen +
				ulHdrFtrLen +
				ulMacroLen + ulAnnotationLen +
				ulEndnoteLen +
				ulTextBoxLen + ulHdrTextBoxLen,
				bFarEastWord,
				IGNORE_PROPMOD,
				pPPS->tWordDocument.ulSB,
				aulBBD, tBBDLen);
		}
		break;
	case 8:
		bSuccess = bGet8DocumentText(pFile,
				pPPS,
				aulBBD, tBBDLen, aulSBD, tSBDLen,
				aucHeader);
		break;
	default:
		werr(0, "This version of Word is not supported");
		bSuccess = FALSE;
		break;
	}

	if (bSuccess) {
		vSplitBlockList(pFile,
				ulTextLen,
				ulFootnoteLen,
				ulHdrFtrLen,
				ulMacroLen,
				ulAnnotationLen,
				ulEndnoteLen,
				ulTextBoxLen,
				ulHdrTextBoxLen,
				!bFastSaved && iWordVersion == 8);
	} else {
		vDestroyTextBlockList();
		werr(0, "I can't find the text of this document");
	}
	return bSuccess;
} /* end of bGetDocumentText */

/*
 * vGetDocumentData - make a list of the data blocks of a Word document
 */
static void
vGetDocumentData(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const UCHAR *aucHeader, int iWordVersion)
{
	options_type	tOptions;
	ULONG	ulBeginOfText;
	BOOL	bFastSaved, bHasImages, bSuccess;
	USHORT	usDocStatus;

	fail(pFile == NULL);
	fail(pPPS == NULL);
	fail(aulBBD == NULL);

	/* Get the options */
	vGetOptions(&tOptions);

	/* Get the status flags from the header */
	usDocStatus = usGetWord(0x0a, aucHeader);
	DBG_HEX(usDocStatus);
	bFastSaved = (usDocStatus & BIT(2)) != 0;
	bHasImages = (usDocStatus & BIT(3)) != 0;

	if (!bHasImages ||
	    tOptions.eConversionType == conversion_text ||
	    tOptions.eConversionType == conversion_fmt_text ||
	    tOptions.eConversionType == conversion_xml ||
	    tOptions.eImageLevel == level_no_images) {
		/*
		 * No images in the document or text-only output or
		 * no images wanted, so no data blocks will be needed
		 */
		vDestroyDataBlockList();
		return;
	}

	/* Get length information */
	ulBeginOfText = ulGetLong(0x18, aucHeader);
	DBG_HEX(ulBeginOfText);

	/* Make a list of the data blocks */
	switch (iWordVersion) {
	case 6:
	case 7:
		/*
		 * The data blocks are in the text stream. The text stream
		 * is in "fast saved" format or "normal saved" format
		 */
		if (bFastSaved) {
			bSuccess = bGet6DocumentData(pFile,
					pPPS->tWordDocument.ulSB,
					aulBBD, tBBDLen,
					aucHeader);
		} else {
		  	bSuccess = bAddDataBlocks(ulBeginOfText,
					(ULONG)LONG_MAX,
					pPPS->tWordDocument.ulSB,
					aulBBD, tBBDLen);
		}
		break;
	case 8:
		/*
		 * The data blocks are in the data stream. The data stream
		 * is always in "normal saved" format
		 */
		bSuccess = bAddDataBlocks(0, (ULONG)LONG_MAX,
				pPPS->tData.ulSB, aulBBD, tBBDLen);
		break;
	default:
		werr(0, "This version of Word is not supported");
		bSuccess = FALSE;
		break;
	}

	if (!bSuccess) {
		vDestroyDataBlockList();
		werr(0, "I can't find the data of this document");
	}
} /* end of vGetDocumentData */

/*
 * iInitDocumentOLE - initialize an OLE document
 *
 * Returns the version of Word that made the document or -1
 */
int
iInitDocumentOLE(FILE *pFile, long lFilesize)
{
	pps_info_type	PPS_info;
	ULONG	*aulBBD, *aulSBD;
	ULONG	*aulRootList, *aulBbdList, *aulSbdList;
	ULONG	ulBdbListStart, ulAdditionalBBDlist;
	ULONG	ulRootStartblock, ulSbdStartblock, ulSBLstartblock;
	ULONG	ulStart, ulTmp;
	long	lMaxBlock;
	size_t	tBBDLen, tSBDLen, tNumBbdBlocks, tRootListLen;
	int	iWordVersion, iIndex, iToGo;
	BOOL	bSuccess;
	USHORT	usIdent, usDocStatus;
	UCHAR	aucHeader[HEADER_SIZE];

	fail(pFile == NULL);

	lMaxBlock = lFilesize / BIG_BLOCK_SIZE - 2;
	DBG_DEC(lMaxBlock);
	if (lMaxBlock < 1) {
		return -1;
	}
	tBBDLen = (size_t)(lMaxBlock + 1);
	tNumBbdBlocks = (size_t)ulReadLong(pFile, 0x2c);
	DBG_DEC(tNumBbdBlocks);
	ulRootStartblock = ulReadLong(pFile, 0x30);
	DBG_DEC(ulRootStartblock);
	ulSbdStartblock = ulReadLong(pFile, 0x3c);
	DBG_DEC(ulSbdStartblock);
	ulAdditionalBBDlist = ulReadLong(pFile, 0x44);
	DBG_HEX(ulAdditionalBBDlist);
	ulSBLstartblock = ulReadLong(pFile,
			(ulRootStartblock + 1) * BIG_BLOCK_SIZE + 0x74);
	DBG_DEC(ulSBLstartblock);
	tSBDLen = (size_t)(ulReadLong(pFile,
			(ulRootStartblock + 1) * BIG_BLOCK_SIZE + 0x78) /
			SMALL_BLOCK_SIZE);
	/* All to be xcalloc-ed pointers to NULL */
	aulRootList = NULL;
	aulSbdList = NULL;
	aulBbdList = NULL;
	aulSBD = NULL;
	aulBBD = NULL;
/* Big Block Depot */
	aulBbdList = xcalloc(tNumBbdBlocks, sizeof(ULONG));
	aulBBD = xcalloc(tBBDLen, sizeof(ULONG));
	iToGo = (int)tNumBbdBlocks;
	vGetBbdList(pFile, min(iToGo, 109),  aulBbdList, 0x4c);
	ulStart = 109;
	iToGo -= 109;
	while (ulAdditionalBBDlist != END_OF_CHAIN && iToGo > 0) {
		ulBdbListStart = (ulAdditionalBBDlist + 1) * BIG_BLOCK_SIZE;
		vGetBbdList(pFile, min(iToGo, 127),
					aulBbdList + ulStart, ulBdbListStart);
		ulAdditionalBBDlist = ulReadLong(pFile,
					ulBdbListStart + 4 * 127);
		DBG_DEC(ulAdditionalBBDlist);
		DBG_HEX(ulAdditionalBBDlist);
		ulStart += 127;
		iToGo -= 127;
	}
	if (!bGetBBD(pFile, aulBbdList, tNumBbdBlocks, aulBBD, tBBDLen)) {
		FREE_ALL();
		return -1;
	}
	aulBbdList = xfree(aulBbdList);
/* Small Block Depot */
	aulSbdList = xcalloc(tBBDLen, sizeof(ULONG));
	aulSBD = xcalloc(tSBDLen, sizeof(ULONG));
	for (iIndex = 0, ulTmp = ulSbdStartblock;
	     iIndex < (int)tBBDLen && ulTmp != END_OF_CHAIN;
	     iIndex++, ulTmp = aulBBD[ulTmp]) {
		if (ulTmp >= (ULONG)tBBDLen) {
			DBG_DEC(ulTmp);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
		aulSbdList[iIndex] = ulTmp;
		NO_DBG_HEX(aulSbdList[iIndex]);
	}
	if (!bGetSBD(pFile, aulSbdList, tBBDLen, aulSBD, tSBDLen)) {
		FREE_ALL();
		return -1;
	}
	aulSbdList = xfree(aulSbdList);
/* Root list */
	for (tRootListLen = 0, ulTmp = ulRootStartblock;
	     tRootListLen < tBBDLen && ulTmp != END_OF_CHAIN;
	     tRootListLen++, ulTmp = aulBBD[ulTmp]) {
		if (ulTmp >= (ULONG)tBBDLen) {
			DBG_DEC(ulTmp);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
	}
	if (tRootListLen == 0) {
		werr(0, "No Rootlist found");
		FREE_ALL();
		return -1;
	}
	aulRootList = xcalloc(tRootListLen, sizeof(ULONG));
	for (iIndex = 0, ulTmp = ulRootStartblock;
	     iIndex < (int)tBBDLen && ulTmp != END_OF_CHAIN;
	     iIndex++, ulTmp = aulBBD[ulTmp]) {
		if (ulTmp >= (ULONG)tBBDLen) {
			DBG_DEC(ulTmp);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
		aulRootList[iIndex] = ulTmp;
		NO_DBG_DEC(aulRootList[iIndex]);
	}
	fail(tRootListLen != (size_t)iIndex);
	bSuccess = bGetPPS(pFile, aulRootList, tRootListLen, &PPS_info);
	aulRootList = xfree(aulRootList);
	if (!bSuccess) {
		FREE_ALL();
		return -1;
	}
/* Small block list */
	if (!bCreateSmallBlockList(ulSBLstartblock, aulBBD, tBBDLen)) {
		FREE_ALL();
		return -1;
	}

	if (PPS_info.tWordDocument.ulSize < MIN_SIZE_FOR_BBD_USE) {
		DBG_DEC(PPS_info.tWordDocument.ulSize);
		FREE_ALL();
		werr(0, "I'm afraid the text stream of this file "
			"is too small to handle.");
		return -1;
	}
	/* Read the headerblock */
	if (!bReadBuffer(pFile, PPS_info.tWordDocument.ulSB,
			aulBBD, tBBDLen, BIG_BLOCK_SIZE,
			aucHeader, 0, HEADER_SIZE)) {
		FREE_ALL();
		return -1;
	}
	usIdent = usGetWord(0x00, aucHeader);
	DBG_HEX(usIdent);
	fail(usIdent != 0x8098 &&	/* Word 7 for oriental languages */
	     usIdent != 0x8099 &&	/* Word 7 for oriental languages */
	     usIdent != 0xa5dc &&	/* Word 6 & 7 */
	     usIdent != 0xa5ec &&	/* Word 7 & 97 & 98 */
	     usIdent != 0xa697 &&	/* Word 7 for oriental languages */
	     usIdent != 0xa699);	/* Word 7 for oriental languages */
	iWordVersion = iGetVersionNumber(aucHeader);
	if (iWordVersion < 6) {
		FREE_ALL();
		werr(0, "This file is from a version of Word before Word 6.");
		return -1;
	}

	/* Get the status flags from the header */
	usDocStatus = usGetWord(0x0a, aucHeader);
        if (usDocStatus & BIT(9)) {
		PPS_info.tTable = PPS_info.t1Table;
	} else {
		PPS_info.tTable = PPS_info.t0Table;
	}
	/* Clean the entries that should not be used */
	memset(&PPS_info.t0Table, 0, sizeof(PPS_info.t0Table));
	memset(&PPS_info.t1Table, 0, sizeof(PPS_info.t1Table));

	bSuccess = bGetDocumentText(pFile, &PPS_info,
			aulBBD, tBBDLen, aulSBD, tSBDLen,
			aucHeader, iWordVersion);
	if (bSuccess) {
		vGetDocumentData(pFile, &PPS_info,
			aulBBD, tBBDLen, aucHeader, iWordVersion);
		vGetPropertyInfo(pFile, &PPS_info,
			aulBBD, tBBDLen, aulSBD, tSBDLen,
			aucHeader, iWordVersion);
		vSetDefaultTabWidth(pFile, &PPS_info,
			aulBBD, tBBDLen, aulSBD, tSBDLen,
			aucHeader, iWordVersion);
		vGetNotesInfo(pFile, &PPS_info,
			aulBBD, tBBDLen, aulSBD, tSBDLen,
			aucHeader, iWordVersion);
	}
	FREE_ALL();
	return bSuccess ? iWordVersion : -1;
} /* end of iInitDocumentOLE */
