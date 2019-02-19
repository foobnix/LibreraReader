/*
 * prop2.c
 * Copyright (C) 2002-2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Read the property information from a WinWord 1 or 2 file
 */

#include <string.h>
#include "antiword.h"


#define MAX_FILESIZE		0x2000000UL	/* 32 Mb */

/*
 * iGet2InfoLength - the length of the information for WinWord 1/2 files
 */
static int
iGet2InfoLength(int iByteNbr, const UCHAR *aucGrpprl)
{
	int	iTmp, iDel, iAdd;

	switch (ucGetByte(iByteNbr, aucGrpprl)) {
	case   3: case  15: case  78: case 152: case 154: case 155:
		return 2 + (int)ucGetByte(iByteNbr + 1, aucGrpprl);
	case  16: case  17: case  18: case  19: case  21: case  22: case  26:
	case  27: case  28: case  30: case  31: case  32: case  33: case  34:
	case  35: case  36: case  38: case  39: case  40: case  41: case  42:
	case  43: case  45: case  46: case  47: case  48: case  49: case  68:
	case  71: case  72: case  82: case  83: case  96: case  97: case  98:
	case  99: case 115: case 116: case 119: case 120: case 123: case 124:
	case 129: case 130: case 131: case 132: case 135: case 136: case 139:
	case 140: case 141: case 142: case 143: case 144: case 145: case 146:
	case 147: case 148: case 153: case 159: case 161: case 162:
		return 1 + 2;
	case  23:
		iTmp = (int)ucGetByte(iByteNbr + 1, aucGrpprl);
		if (iTmp == 255) {
			iDel = (int)ucGetByte(iByteNbr + 2, aucGrpprl);
			iAdd = (int)ucGetByte(
					iByteNbr + 3 + iDel * 4, aucGrpprl);
			iTmp = 2 + iDel * 4 + iAdd * 3;
		}
		return 2 + iTmp;
	case  70:
		return 1 + 3;
	case  95:
		return 1 + 13;
	case 157: case 163:
		return 1 + 5;
	case 158: case 160: case 164:
		return 1 + 4;
	default:
		return 1 + 1;
	}
} /* end of iGet2InfoLength */

/*
 * Build the lists with Document Property Information for WinWord 1/2 files
 */
void
vGet2DopInfo(FILE *pFile, const UCHAR *aucHeader)
{
	document_block_type	tDocument;
	UCHAR	*aucBuffer;
	ULONG	ulBeginDocpInfo, ulTmp;
	size_t	tDocpInfoLen;
	USHORT	usTmp;

	ulBeginDocpInfo = ulGetLong(0x112, aucHeader); /* fcDop */
	DBG_HEX(ulBeginDocpInfo);
	tDocpInfoLen = (size_t)usGetWord(0x116, aucHeader); /* cbDop */
	DBG_DEC(tDocpInfoLen);
	if (tDocpInfoLen < 28) {
		DBG_MSG("No Document information");
		return;
	}

	aucBuffer = xmalloc(tDocpInfoLen);
	if (!bReadBytes(aucBuffer, tDocpInfoLen, ulBeginDocpInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}

	usTmp = usGetWord(0x00, aucBuffer);
	tDocument.ucHdrFtrSpecification = (UCHAR)(usTmp >> 8); /* grpfIhdt */
	tDocument.usDefaultTabWidth = usGetWord(0x0a, aucBuffer); /* dxaTab */
	ulTmp = ulGetLong(0x14, aucBuffer); /* dttmCreated */
	tDocument.tCreateDate = tConvertDTTM(ulTmp);
	ulTmp = ulGetLong(0x18, aucBuffer); /* dttmRevised */
	tDocument.tRevisedDate = tConvertDTTM(ulTmp);
	vCreateDocumentInfoList(&tDocument);

	aucBuffer = xfree(aucBuffer);
} /* end of vGet2DopInfo */

/*
 * Fill the section information block with information
 * from a WinWord 1/2 file.
 */
static void
vGet2SectionInfo(const UCHAR *aucGrpprl, size_t tBytes,
		section_block_type *pSection)
{
	int	iFodoOff, iInfoLen;
	USHORT	usCcol;
	UCHAR	ucTmp;

	fail(aucGrpprl == NULL || pSection == NULL);

	iFodoOff = 0;
	while (tBytes >= (size_t)iFodoOff + 1) {
		switch (ucGetByte(iFodoOff, aucGrpprl)) {
		case 117:	/* bkc */
			ucTmp = ucGetByte(iFodoOff + 1, aucGrpprl);
			DBG_DEC(ucTmp);
			pSection->bNewPage = ucTmp != 0 && ucTmp != 1;
			break;
		case 119:	/* ccolM1 */
			usCcol = 1 + usGetWord(iFodoOff + 1, aucGrpprl);
			DBG_DEC(usCcol);
			break;
		case 128:	/* grpfIhdt */
			pSection->ucHdrFtrSpecification =
					ucGetByte(iFodoOff + 1, aucGrpprl);
			break;
		default:
			break;
		}
		iInfoLen = iGet2InfoLength(iFodoOff, aucGrpprl);
		fail(iInfoLen <= 0);
		iFodoOff += iInfoLen;
	}
} /* end of vGet2SectionInfo */

/*
 * Build the lists with Section Property Information for WinWord 1/2 files
 */
void
vGet2SepInfo(FILE *pFile, const UCHAR *aucHeader)
{
	section_block_type	tSection;
	ULONG	*aulSectPage, *aulCharPos;
	UCHAR	*aucBuffer, *aucFpage;
	ULONG	ulBeginOfText, ulTextOffset, ulBeginSectInfo;
	size_t	tSectInfoLen, tIndex, tOffset, tLen, tBytes;
	UCHAR	aucTmp[1];

	fail(pFile == NULL || aucHeader == NULL);

	ulBeginOfText = ulGetLong(0x18, aucHeader); /* fcMin */
	NO_DBG_HEX(ulBeginOfText);
	ulBeginSectInfo = ulGetLong(0x7c, aucHeader); /* fcPlcfsed */
	DBG_HEX(ulBeginSectInfo);
	tSectInfoLen = (size_t)usGetWord(0x80, aucHeader); /* cbPlcfsed */
	DBG_DEC(tSectInfoLen);
	if (tSectInfoLen < 4) {
		DBG_DEC(tSectInfoLen);
		return;
	}

	aucBuffer = xmalloc(tSectInfoLen);
	if (!bReadBytes(aucBuffer, tSectInfoLen, ulBeginSectInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tSectInfoLen);

	/* Read the Section Descriptors */
	tLen = (tSectInfoLen - 4) / 10;
	/* Save the section offsets */
	aulCharPos = xcalloc(tLen, sizeof(ULONG));
	for (tIndex = 0, tOffset = 0;
	     tIndex < tLen;
	     tIndex++, tOffset += 4) {
		ulTextOffset = ulGetLong(tOffset, aucBuffer);
		NO_DBG_HEX(ulTextOffset);
		aulCharPos[tIndex] = ulBeginOfText + ulTextOffset;
		NO_DBG_HEX(aulCharPos[tIndex]);
	}
	/* Save the Sepx offsets */
	aulSectPage = xcalloc(tLen, sizeof(ULONG));
	for (tIndex = 0, tOffset = (tLen + 1) * 4;
	     tIndex < tLen;
	     tIndex++, tOffset += 6) {
		aulSectPage[tIndex] = ulGetLong(tOffset + 2, aucBuffer);
		NO_DBG_HEX(aulSectPage[tIndex]); /* fcSepx */
	}
	aucBuffer = xfree(aucBuffer);

	/* Read the Section Properties */
	for (tIndex = 0; tIndex < tLen; tIndex++) {
		if (aulSectPage[tIndex] == FC_INVALID) {
			vDefault2SectionInfoList(aulCharPos[tIndex]);
			continue;
		}
		/* Get the number of bytes to read */
		if (!bReadBytes(aucTmp, 1, aulSectPage[tIndex], pFile)) {
			continue;
		}
		tBytes = 1 + (size_t)ucGetByte(0, aucTmp);
		NO_DBG_DEC(tBytes);
		/* Read the bytes */
		aucFpage = xmalloc(tBytes);
		if (!bReadBytes(aucFpage, tBytes, aulSectPage[tIndex], pFile)) {
			aucFpage = xfree(aucFpage);
			continue;
		}
		NO_DBG_PRINT_BLOCK(aucFpage, tBytes);
		/* Process the bytes */
		vGetDefaultSection(&tSection);
		vGet2SectionInfo(aucFpage + 1, tBytes - 1, &tSection);
		vAdd2SectionInfoList(&tSection, aulCharPos[tIndex]);
		aucFpage = xfree(aucFpage);
	}
	aulCharPos = xfree(aulCharPos);
	aulSectPage = xfree(aulSectPage);
} /* end of vGet2SepInfo */

/*
 * Build the list with Header/Footer Information for WinWord 1/2 files
 */
void
vGet2HdrFtrInfo(FILE *pFile, const UCHAR *aucHeader)
{
	ULONG	*aulCharPos;
	UCHAR	*aucBuffer;
	ULONG	ulHdrFtrOffset, ulBeginHdrFtrInfo;
	size_t	tHdrFtrInfoLen, tIndex, tOffset, tLen;

	fail(pFile == NULL || aucHeader == NULL);

	ulBeginHdrFtrInfo = ulGetLong(0x9a, aucHeader); /* fcPlcfhdd */
	NO_DBG_HEX(ulBeginHdrFtrInfo);
	tHdrFtrInfoLen = (size_t)usGetWord(0x9e, aucHeader); /* cbPlcfhdd */
	NO_DBG_DEC(tHdrFtrInfoLen);
	if (tHdrFtrInfoLen < 8) {
		DBG_DEC_C(tHdrFtrInfoLen != 0, tHdrFtrInfoLen);
		return;
	}

	aucBuffer = xmalloc(tHdrFtrInfoLen);
	if (!bReadBytes(aucBuffer, tHdrFtrInfoLen, ulBeginHdrFtrInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tHdrFtrInfoLen);

	tLen = tHdrFtrInfoLen / 4 - 1;
	/* Save the header/footer offsets */
	aulCharPos = xcalloc(tLen, sizeof(ULONG));
	for (tIndex = 0, tOffset = 0;
	     tIndex < tLen;
	     tIndex++, tOffset += 4) {
		ulHdrFtrOffset = ulGetLong(tOffset, aucBuffer);
		NO_DBG_HEX(ulHdrFtrOffset);
		aulCharPos[tIndex] = ulHdrFtrOffset2CharPos(ulHdrFtrOffset);
		NO_DBG_HEX(aulCharPos[tIndex]);
	}
	vCreat2HdrFtrInfoList(aulCharPos, tLen);
	aulCharPos = xfree(aulCharPos);
	aucBuffer = xfree(aucBuffer);
} /* end of vGet2HdrFtrInfo */

/*
 * Translate the rowinfo to a member of the row_info enumeration
 */
row_info_enum
eGet2RowInfo(int iFodo,
	const UCHAR *aucGrpprl, int iBytes, row_block_type *pRow)
{
	int	iFodoOff, iInfoLen;
	int	iIndex, iSize, iCol;
	int	iPosCurr, iPosPrev;
	USHORT	usTmp;
	BOOL	bFound24_0, bFound24_1, bFound25_0, bFound25_1, bFound154;

	fail(iFodo < 0 || aucGrpprl == NULL || pRow == NULL);

	iFodoOff = 0;
	bFound24_0 = FALSE;
	bFound24_1 = FALSE;
	bFound25_0 = FALSE;
	bFound25_1 = FALSE;
	bFound154 = FALSE;
	while (iBytes >= iFodoOff + 1) {
		iInfoLen = 0;
		switch (ucGetByte(iFodo + iFodoOff, aucGrpprl)) {
		case  24:	/* fIntable */
			if (odd(ucGetByte(iFodo + iFodoOff + 1, aucGrpprl))) {
				bFound24_1 = TRUE;
			} else {
				bFound24_0 = TRUE;
			}
			break;
		case  25:	/* fTtp */
			if (odd(ucGetByte(iFodo + iFodoOff + 1, aucGrpprl))) {
				bFound25_1 = TRUE;
			} else {
				bFound25_0 = TRUE;
			}
			break;
		case 30:	/* brcTop10 */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x01ff;
			NO_DBG_DEC(usTmp >> 6);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_TOP;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_TOP;
			}
			break;
		case 31:	/* brcLeft10 */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x01ff;
			NO_DBG_DEC(usTmp >> 6);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_LEFT;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_LEFT;
			}
			break;
		case 32:	/* brcBottom10 */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x01ff;
			NO_DBG_DEC(usTmp >> 6);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_BOTTOM;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_BOTTOM;
			}
			break;
		case 33:	/* brcRight10 */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x01ff;
			NO_DBG_DEC(usTmp >> 6);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_RIGHT;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_RIGHT;
			}
			break;
		case 38:	/* brcTop */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x0018;
			NO_DBG_DEC(usTmp >> 3);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_TOP;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_TOP;
			}
			break;
		case 39:	/* brcLeft */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x0018;
			NO_DBG_DEC(usTmp >> 3);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_LEFT;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_LEFT;
			}
			break;
		case 40:	/* brcBottom */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x0018;
			NO_DBG_DEC(usTmp >> 3);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_BOTTOM;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_BOTTOM;
			}
			break;
		case 41:	/* brcRight */
			usTmp = usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			usTmp &= 0x0018;
			NO_DBG_DEC(usTmp >> 3);
			if (usTmp == 0) {
				pRow->ucBorderInfo &= ~TABLE_BORDER_RIGHT;
			} else {
				pRow->ucBorderInfo |= TABLE_BORDER_RIGHT;
			}
			break;
		case 152:	/* cDefTable10 */
		case 154:	/* cDefTable */
			iSize = (int)usGetWord(iFodo + iFodoOff + 1, aucGrpprl);
			if (iSize < 6 || iBytes < iFodoOff + 7) {
				DBG_DEC(iSize);
				DBG_DEC(iBytes);
				DBG_DEC(iFodoOff);
				iInfoLen = 1;
				break;
			}
			iCol = (int)ucGetByte(iFodo + iFodoOff + 3, aucGrpprl);
			if (iCol < 1 ||
			    iBytes < iFodoOff + 3 + (iCol + 1) * 2) {
				DBG_DEC(iCol);
				DBG_DEC(iBytes);
				DBG_DEC(iFodoOff);
				DBG_DEC(ucGetByte(iFodo + iFodoOff, aucGrpprl));
				iInfoLen = 1;
				break;
			}
			if (iCol >= (int)elementsof(pRow->asColumnWidth)) {
				DBG_DEC(iCol);
				werr(1, "The number of columns is corrupt");
			}
			pRow->ucNumberOfColumns = (UCHAR)iCol;
			iPosPrev = (int)(short)usGetWord(
					iFodo + iFodoOff + 4,
					aucGrpprl);
			for (iIndex = 0; iIndex < iCol; iIndex++) {
				iPosCurr = (int)(short)usGetWord(
					iFodo + iFodoOff + 6 + iIndex * 2,
					aucGrpprl);
				pRow->asColumnWidth[iIndex] =
						(short)(iPosCurr - iPosPrev);
				iPosPrev = iPosCurr;
			}
			bFound154 = TRUE;
			break;
		default:
			break;
		}
		if (iInfoLen <= 0) {
			iInfoLen =
				iGet2InfoLength(iFodo + iFodoOff, aucGrpprl);
			fail(iInfoLen <= 0);
		}
		iFodoOff += iInfoLen;
	}
	if (bFound24_1 && bFound25_1 && bFound154) {
		return found_end_of_row;
	}
	if (bFound24_0 && bFound25_0 && !bFound154) {
		return found_not_end_of_row;
	}
	if (bFound24_1) {
		return found_a_cell;
	}
	if (bFound24_0) {
		return found_not_a_cell;
	}
	return found_nothing;
} /* end of eGet2RowInfo */

/*
 * Fill the style information block with information
 * from a WinWord 1/2 file.
 */
void
vGet2StyleInfo(int iFodo,
	const UCHAR *aucGrpprl, int iBytes, style_block_type *pStyle)
{
	int	iFodoOff, iInfoLen;
	int	iTmp, iDel, iAdd;
	short	sTmp;
	UCHAR	ucTmp;

	fail(iFodo < 0 || aucGrpprl == NULL || pStyle == NULL);

	NO_DBG_DEC(pStyle->usIstd);

	iFodoOff = 0;
	while (iBytes >= iFodoOff + 1) {
		iInfoLen = 0;
		switch (ucGetByte(iFodo + iFodoOff, aucGrpprl)) {
		case   2:	/* istd */
			sTmp = (short)ucGetByte(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(sTmp);
			break;
		case   5:	/* jc */
			pStyle->ucAlignment = ucGetByte(
					iFodo + iFodoOff + 1, aucGrpprl);
			break;
		case  12:	/* nfcSeqNumb */
			pStyle->ucNFC = ucGetByte(
					iFodo + iFodoOff + 1, aucGrpprl);
			break;
		case  13:	/* nLvlAnm */
			ucTmp = ucGetByte(iFodo + iFodoOff + 1, aucGrpprl);
			pStyle->ucNumLevel = ucTmp;
			pStyle->bNumPause =
				eGetNumType(ucTmp) == level_type_pause;
			break;
		case  15:	/* ChgTabsPapx */
		case  23:	/* ChgTabs */
			iTmp = (int)ucGetByte(iFodo + iFodoOff + 1, aucGrpprl);
			if (iTmp < 2) {
				iInfoLen = 1;
				break;
			}
			NO_DBG_DEC(iTmp);
			iDel = (int)ucGetByte(iFodo + iFodoOff + 2, aucGrpprl);
			if (iTmp < 2 + 2 * iDel) {
				iInfoLen = 1;
				break;
			}
			NO_DBG_DEC(iDel);
			iAdd = (int)ucGetByte(
				iFodo + iFodoOff + 3 + 2 * iDel, aucGrpprl);
			if (iTmp < 2 + 2 * iDel + 2 * iAdd) {
				iInfoLen = 1;
				break;
			}
			NO_DBG_DEC(iAdd);
			break;
		case  16:	/* dxaRight */
			pStyle->sRightIndent = (short)usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(pStyle->sRightIndent);
			break;
		case  17:	/* dxaLeft */
			pStyle->sLeftIndent = (short)usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(pStyle->sLeftIndent);
			break;
		case  18:	/* Nest dxaLeft */
			sTmp = (short)usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			pStyle->sLeftIndent += sTmp;
			if (pStyle->sLeftIndent < 0) {
				pStyle->sLeftIndent = 0;
			}
			NO_DBG_DEC(sTmp);
			NO_DBG_DEC(pStyle->sLeftIndent);
			break;
		case  19:	/* dxaLeft1 */
			pStyle->sLeftIndent1 = (short)usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(pStyle->sLeftIndent1);
			break;
		case  21:	/* dyaBefore */
			pStyle->usBeforeIndent = usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(pStyle->usBeforeIndent);
			break;
		case  22:	/* dyaAfter */
			pStyle->usAfterIndent = usGetWord(
					iFodo + iFodoOff + 1, aucGrpprl);
			NO_DBG_DEC(pStyle->usAfterIndent);
			break;
		default:
			break;
		}
		if (iInfoLen <= 0) {
			iInfoLen =
				iGet2InfoLength(iFodo + iFodoOff, aucGrpprl);
			fail(iInfoLen <= 0);
		}
		iFodoOff += iInfoLen;
	}
} /* end of vGet2StyleInfo */

/*
 * Build the lists with Paragraph Information for WinWord 1/2 files
 */
void
vGet2PapInfo(FILE *pFile, const UCHAR *aucHeader)
{
	row_block_type		tRow;
	style_block_type	tStyle;
	USHORT	*ausParfPage;
	UCHAR	*aucBuffer;
	ULONG	ulCharPos, ulCharPosFirst, ulCharPosLast;
	ULONG	ulBeginParfInfo;
	size_t	tParfInfoLen, tParfPageNum, tOffset, tSize, tLenOld, tLen;
	int	iIndex, iIndex2, iRun, iFodo, iLen;
	row_info_enum	eRowInfo;
	USHORT	usParfFirstPage, usCount, usIstd;
	UCHAR	ucStc;
	UCHAR	aucFpage[BIG_BLOCK_SIZE];

	fail(pFile == NULL || aucHeader == NULL);

	ulBeginParfInfo = ulGetLong(0xa6, aucHeader); /* fcPlcfbtePapx */
	NO_DBG_HEX(ulBeginParfInfo);
	tParfInfoLen = (size_t)usGetWord(0xaa, aucHeader); /* cbPlcfbtePapx */
	NO_DBG_DEC(tParfInfoLen);
	if (tParfInfoLen < 4) {
		DBG_DEC(tParfInfoLen);
		return;
	}

	aucBuffer = xmalloc(tParfInfoLen);
	if (!bReadBytes(aucBuffer, tParfInfoLen, ulBeginParfInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tParfInfoLen);

	tLen = (tParfInfoLen - 4) / 6;
	ausParfPage = xcalloc(tLen, sizeof(USHORT));
	for (iIndex = 0, tOffset = (tLen + 1) * 4;
	     iIndex < (int)tLen;
	     iIndex++, tOffset += 2) {
		ausParfPage[iIndex] = usGetWord(tOffset, aucBuffer);
		NO_DBG_DEC(ausParfPage[iIndex]);
	}
	DBG_HEX(ulGetLong(0, aucBuffer));
	aucBuffer = xfree(aucBuffer);
	tParfPageNum = (size_t)usGetWord(0x144, aucHeader); /* cpnBtePap */
	DBG_DEC(tParfPageNum);
	if (tLen < tParfPageNum) {
		/* Replace ParfPage by a longer version */
		tLenOld = tLen;
		usParfFirstPage = usGetWord(0x140, aucHeader); /* pnPapFirst */
		DBG_DEC(usParfFirstPage);
		tLen += tParfPageNum - 1;
		tSize = tLen * sizeof(USHORT);
		ausParfPage = xrealloc(ausParfPage, tSize);
		/* Add new values */
		usCount = usParfFirstPage + 1;
		for (iIndex = (int)tLenOld; iIndex < (int)tLen; iIndex++) {
			ausParfPage[iIndex] = usCount;
			NO_DBG_DEC(ausParfPage[iIndex]);
			usCount++;
		}
	}

	(void)memset(&tRow, 0, sizeof(tRow));
	ulCharPosFirst = CP_INVALID;
	for (iIndex = 0; iIndex < (int)tLen; iIndex++) {
		if (!bReadBytes(aucFpage, BIG_BLOCK_SIZE,
				(ULONG)ausParfPage[iIndex] * BIG_BLOCK_SIZE,
				pFile)) {
			break;
		}
		NO_DBG_PRINT_BLOCK(aucFpage, BIG_BLOCK_SIZE);
		iRun = (int)ucGetByte(0x1ff, aucFpage);
		NO_DBG_DEC(iRun);
		for (iIndex2 = 0; iIndex2 < iRun; iIndex2++) {
			if ((iRun + 1) * 4 + iIndex2 * 1 >= BIG_BLOCK_SIZE) {
				break;
			}
			NO_DBG_HEX(ulGetLong(iIndex2 * 4, aucFpage));
			iFodo = 2 * (int)ucGetByte(
				(iRun + 1) * 4 + iIndex2 * 1, aucFpage);
			if (iFodo <= 0) {
				continue;
			}

			iLen = 2 * (int)ucGetByte(iFodo, aucFpage);

			ucStc = ucGetByte(iFodo + 1, aucFpage);
			usIstd = usStc2istd(ucStc);

			vFillStyleFromStylesheet(usIstd, &tStyle);
			vGet2StyleInfo(iFodo, aucFpage + 8, iLen - 8, &tStyle);
			ulCharPos = ulGetLong(iIndex2 * 4, aucFpage);
			NO_DBG_HEX(ulCharPos);
			tStyle.ulFileOffset = ulCharPos;
			vAdd2StyleInfoList(&tStyle);

			eRowInfo = eGet2RowInfo(iFodo,
					aucFpage + 8, iLen - 8, &tRow);

			switch(eRowInfo) {
			case found_a_cell:
				if (ulCharPosFirst != CP_INVALID) {
					break;
				}
				ulCharPosFirst = ulGetLong(
						iIndex2 * 4, aucFpage);
				NO_DBG_HEX(ulCharPosFirst);
				tRow.ulCharPosStart = ulCharPosFirst;
				tRow.ulFileOffsetStart = ulCharPosFirst;
				break;
			case found_end_of_row:
				ulCharPosLast = ulGetLong(
						iIndex2 * 4, aucFpage);
				NO_DBG_HEX(ulCharPosLast);
				tRow.ulCharPosEnd = ulCharPosLast;
				/* Add 1 for compatiblity with Word 6 and up */
				tRow.ulFileOffsetEnd = ulCharPosLast + 1;
				vAdd2RowInfoList(&tRow);
				(void)memset(&tRow, 0, sizeof(tRow));
				ulCharPosFirst = CP_INVALID;
				break;
			case found_nothing:
				break;
			default:
				DBG_DEC(eRowInfo);
				break;
			}
		}
	}
	ausParfPage = xfree(ausParfPage);
} /* end of vGet2PapInfo */

/*
 * Fill the font information block with information
 * from a WinWord 1 file.
 */
void
vGet1FontInfo(int iFodo,
	const UCHAR *aucGrpprl, size_t tBytes, font_block_type *pFont)
{
	BOOL	bIcoChange, bFtcChange, bHpsChange, bKulChange;
	USHORT	usTmp;
	UCHAR	ucTmp;
	UCHAR	aucChpx[12];

	fail(iFodo < 0 || aucGrpprl == NULL || pFont == NULL);

	if (tBytes > sizeof(aucChpx)) {
		NO_DBG_PRINT_BLOCK(aucGrpprl + iFodo, tBytes);
		return;
	}

	/* Build the CHPX structure */
	(void)memset(aucChpx, 0, sizeof(aucChpx));
	(void)memcpy(aucChpx, aucGrpprl + iFodo, min(tBytes, sizeof(aucChpx)));

	usTmp = usGetWord(0, aucChpx);
	if ((usTmp & BIT(0)) != 0) {
		pFont->usFontStyle ^= FONT_BOLD;
	}
	if ((usTmp & BIT(1)) != 0) {
		pFont->usFontStyle ^= FONT_ITALIC;
	}
	if ((usTmp & BIT(2)) != 0) {
		pFont->usFontStyle ^= FONT_STRIKE;
	}
	if ((usTmp & BIT(5)) != 0) {
		pFont->usFontStyle ^= FONT_SMALL_CAPITALS;
	}
	if ((usTmp & BIT(6)) != 0) {
		pFont->usFontStyle ^= FONT_CAPITALS;
	}
	if ((usTmp & BIT(7)) != 0) {
		pFont->usFontStyle ^= FONT_HIDDEN;
	}

	ucTmp = ucGetByte(5, aucChpx);
	if (ucTmp != 0) {
		if (ucTmp < 128) {
			pFont->usFontStyle |= FONT_SUPERSCRIPT;
			DBG_MSG("Superscript");
		} else {
			pFont->usFontStyle |= FONT_SUBSCRIPT;
			DBG_MSG("Subscript");
		}
	}

	bIcoChange = (usTmp & BIT(10)) != 0;
	bFtcChange = (usTmp & BIT(11)) != 0;
	bHpsChange = (usTmp & BIT(12)) != 0;
	bKulChange = (usTmp & BIT(13)) != 0;

	if (bFtcChange) {
		usTmp = usGetWord(2, aucChpx);
		if (usTmp <= (USHORT)UCHAR_MAX) {
			pFont->ucFontNumber = (UCHAR)usTmp;
		} else {
			pFont->ucFontNumber = 0;
		}
	}

	if (bHpsChange) {
		pFont->usFontSize = (USHORT)ucGetByte(4, aucChpx);
	}

	if (bIcoChange || bKulChange) {
		usTmp = usGetWord(6, aucChpx);
		if (bIcoChange) {
			pFont->ucFontColor = (UCHAR)((usTmp & 0x0f00) >> 8);
			if (pFont->ucFontColor <= 7) {
				/* Add 1 for compatibility with Word 2 and up */
				pFont->ucFontColor++;
			} else {
				DBG_DEC(pFont->ucFontColor);
				pFont->ucFontColor = 0;
			}
		}
		if (bKulChange) {
			usTmp = (usTmp & 0x7000) >> 12;
			DBG_DEC_C(usTmp > 4, usTmp);
			if (usTmp == 0) {
				pFont->usFontStyle &= ~FONT_UNDERLINE;
			} else {
				pFont->usFontStyle |= FONT_UNDERLINE;
			}
		}
	}
} /* end of vGet1FontInfo */

/*
 * Fill the font information block with information
 * from a WinWord 1/2 file.
 */
void
vGet2FontInfo(int iFodo,
	const UCHAR *aucGrpprl, size_t tBytes, font_block_type *pFont)
{
	BOOL	bIcoChange, bFtcChange, bHpsChange, bKulChange;
	USHORT	usTmp;
	UCHAR	ucTmp;
	UCHAR	aucChpx[18];

	fail(iFodo < 0 || aucGrpprl == NULL || pFont == NULL);

	if (tBytes > sizeof(aucChpx)) {
		NO_DBG_PRINT_BLOCK(aucGrpprl + iFodo, tBytes);
		return;
	}

	/* Build the CHPX structure */
	(void)memset(aucChpx, 0, sizeof(aucChpx));
	(void)memcpy(aucChpx, aucGrpprl + iFodo, min(tBytes, sizeof(aucChpx)));

	usTmp = usGetWord(0, aucChpx);
	if ((usTmp & BIT(0)) != 0) {
		pFont->usFontStyle ^= FONT_BOLD;
	}
	if ((usTmp & BIT(1)) != 0) {
		pFont->usFontStyle ^= FONT_ITALIC;
	}
	if (usTmp & BIT(3)) {
		pFont->usFontStyle ^= FONT_MARKDEL;
	}
	if ((usTmp & BIT(5)) != 0) {
		pFont->usFontStyle ^= FONT_SMALL_CAPITALS;
	}
	if ((usTmp & BIT(6)) != 0) {
		pFont->usFontStyle ^= FONT_CAPITALS;
	}
	if ((usTmp & BIT(7)) != 0) {
		pFont->usFontStyle ^= FONT_HIDDEN;
	}
	if (usTmp & BIT(10)) {
		pFont->usFontStyle ^= FONT_STRIKE;
	}

	ucTmp = ucGetByte(10, aucChpx);
	DBG_MSG_C(ucTmp != 0 && ucTmp < 128, "Superscript");
	DBG_MSG_C(ucTmp >= 128, "Subscript");

	usTmp = usGetWord(2, aucChpx);
	if (usTmp == 0) {
		/* No changes, nothing to do */
		return;
	}

	bIcoChange = (usTmp & BIT(0)) != 0;
	bFtcChange = (usTmp & BIT(1)) != 0;
	bHpsChange = (usTmp & BIT(2)) != 0;
	bKulChange = (usTmp & BIT(3)) != 0;

	if (bFtcChange) {
		usTmp = usGetWord(4, aucChpx);
		if (usTmp <= (USHORT)UCHAR_MAX) {
			pFont->ucFontNumber = (UCHAR)usTmp;
		} else {
			pFont->ucFontNumber = 0;
		}
	}

	if (bHpsChange) {
		pFont->usFontSize = usGetWord(6, aucChpx);
	}

	if (bIcoChange || bKulChange) {
		ucTmp = ucGetByte(9, aucChpx);
		if (bIcoChange) {
			pFont->ucFontColor = ucTmp & 0x1f;
			if (pFont->ucFontColor > 16) {
				DBG_DEC(pFont->ucFontColor);
				pFont->ucFontColor = 0;
			}
		}
		if (bKulChange) {
			ucTmp = (ucTmp & 0xe0) >> 5;
			DBG_DEC_C(ucTmp > 4, ucTmp);
			if (ucTmp == 0) {
				pFont->usFontStyle &= ~FONT_UNDERLINE;
			} else {
				pFont->usFontStyle |= FONT_UNDERLINE;
			}
		}
	}
} /* end of vGet2FontInfo */

/*
 * Fill the picture information block with information from a WinWord 1 file.
 * Returns TRUE when successful, otherwise FALSE
 */
static BOOL
bGet1PicInfo(int iFodo,
	const UCHAR *aucGrpprl, size_t tBytes, picture_block_type *pPicture)
{
	ULONG	ulTmp;
	UCHAR	aucChpx[12];

	fail(iFodo < 0 || aucGrpprl == NULL || pPicture == NULL);

	if (tBytes > sizeof(aucChpx)) {
		NO_DBG_PRINT_BLOCK(aucGrpprl + iFodo, tBytes);
		tBytes = sizeof(aucChpx);
	}

	/* Build the CHPX structure */
	(void)memset(aucChpx, 0, sizeof(aucChpx));
	(void)memcpy(aucChpx, aucGrpprl + iFodo, min(tBytes, sizeof(aucChpx)));

	ulTmp = ulGetLong(8, aucChpx);
	if (ulTmp != 0 && ulTmp < MAX_FILESIZE) {
		pPicture->ulPictureOffset = ulTmp;
		DBG_HEX(pPicture->ulPictureOffset);
		return TRUE;
	}
	return FALSE;
} /* end of bGet1PicInfo */

/*
 * Fill the picture information block with information from a WinWord 2 file.
 * Returns TRUE when successful, otherwise FALSE
 */
static BOOL
bGet2PicInfo(int iFodo,
	const UCHAR *aucGrpprl, size_t tBytes, picture_block_type *pPicture)
{
	ULONG	ulTmp;
	UCHAR	aucChpx[18];

	fail(iFodo < 0 || aucGrpprl == NULL || pPicture == NULL);

	if (tBytes > sizeof(aucChpx)) {
		NO_DBG_PRINT_BLOCK(aucGrpprl + iFodo, tBytes);
		tBytes = sizeof(aucChpx);
	}

	/* Build the CHPX structure */
	(void)memset(aucChpx, 0, sizeof(aucChpx));
	(void)memcpy(aucChpx, aucGrpprl + iFodo, min(tBytes, sizeof(aucChpx)));

	ulTmp = ulGetLong(14, aucChpx);
	if (ulTmp != 0 && ulTmp < MAX_FILESIZE) {
		pPicture->ulPictureOffset = ulTmp;
		DBG_HEX(pPicture->ulPictureOffset);
		DBG_DEC(tBytes);
		return TRUE;
	}
	return FALSE;
} /* end of bGet2PicInfo */

/*
 * Build the lists with Character Information for WinWord 1/2 files
 */
void
vGet2ChrInfo(FILE *pFile, int iWordVersion, const UCHAR *aucHeader)
{
	font_block_type		tFont;
	picture_block_type	tPicture;
	USHORT	*ausCharPage;
	UCHAR	*aucBuffer;
	ULONG	ulFileOffset, ulCharPos, ulBeginCharInfo;
	size_t	tCharInfoLen, tOffset, tSize, tChrLen, tCharPageNum;
	size_t	tLenOld, tLen;
	int	iIndex, iIndex2, iRun, iFodo;
	BOOL	bSuccess1, bSuccess2;
	USHORT	usCharFirstPage, usCount, usIstd;
	UCHAR	aucFpage[BIG_BLOCK_SIZE];

	fail(pFile == NULL || aucHeader == NULL);
	fail(iWordVersion != 1 && iWordVersion != 2);

	ulBeginCharInfo = ulGetLong(0xa0, aucHeader); /* fcPlcfbteChpx */
	DBG_HEX(ulBeginCharInfo);
	tCharInfoLen = (size_t)usGetWord(0xa4, aucHeader); /* cbPlcfbteChpx */
	DBG_DEC(tCharInfoLen);
	if (tCharInfoLen < 4) {
		DBG_DEC(tCharInfoLen);
		return;
	}

	aucBuffer = xmalloc(tCharInfoLen);
	if (!bReadBytes(aucBuffer, tCharInfoLen, ulBeginCharInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tCharInfoLen);

	tLen = (tCharInfoLen - 4) / 6;
	ausCharPage = xcalloc(tLen, sizeof(USHORT));
	for (iIndex = 0, tOffset = (tLen + 1) * 4;
	     iIndex < (int)tLen;
	     iIndex++, tOffset += 2) {
		ausCharPage[iIndex] = usGetWord(tOffset, aucBuffer);
		NO_DBG_DEC(ausCharPage[iIndex]);
	}
	DBG_HEX(ulGetLong(0, aucBuffer));
	aucBuffer = xfree(aucBuffer);
	tCharPageNum = (size_t)usGetWord(0x142, aucHeader); /* cpnBteChp */
	DBG_DEC(tCharPageNum);
	if (tLen < tCharPageNum) {
		/* Replace CharPage by a longer version */
		tLenOld = tLen;
		usCharFirstPage = usGetWord(0x13e, aucHeader); /* pnChrFirst */
		NO_DBG_DEC(usCharFirstPage);
		tLen += tCharPageNum - 1;
		tSize = tLen * sizeof(USHORT);
		ausCharPage = xrealloc(ausCharPage, tSize);
		/* Add new values */
		usCount = usCharFirstPage + 1;
		for (iIndex = (int)tLenOld; iIndex < (int)tLen; iIndex++) {
			ausCharPage[iIndex] = usCount;
			NO_DBG_DEC(ausCharPage[iIndex]);
			usCount++;
		}
	}

	for (iIndex = 0; iIndex < (int)tLen; iIndex++) {
		if (!bReadBytes(aucFpage, BIG_BLOCK_SIZE,
				(ULONG)ausCharPage[iIndex] * BIG_BLOCK_SIZE,
				pFile)) {
			break;
		}
		NO_DBG_PRINT_BLOCK(aucFpage, BIG_BLOCK_SIZE);
		iRun = (int)ucGetByte(0x1ff, aucFpage);
		NO_DBG_DEC(iRun);
		for (iIndex2 = 0; iIndex2 < iRun; iIndex2++) {
			if ((iRun + 1) * 4 + iIndex2 >= BIG_BLOCK_SIZE) {
				break;
			}
			ulCharPos = ulGetLong(iIndex2 * 4, aucFpage);
			ulFileOffset = ulCharPos;
			iFodo = 2 * (int)ucGetByte(
				(iRun + 1) * 4 + iIndex2, aucFpage);

			tChrLen = (size_t)ucGetByte(iFodo, aucFpage);

			usIstd = usGetIstd(ulFileOffset);
			vFillFontFromStylesheet(usIstd, &tFont);
			if (iFodo != 0) {
				if (iWordVersion == 1) {
					vGet1FontInfo(iFodo,
						aucFpage + 1, tChrLen, &tFont);
				} else if (iWordVersion == 2) {
					vGet2FontInfo(iFodo,
						aucFpage + 1, tChrLen, &tFont);
				}
			}
			tFont.ulFileOffset = ulFileOffset;
			vAdd2FontInfoList(&tFont);

			if (iFodo <= 0) {
				continue;
			}

			(void)memset(&tPicture, 0, sizeof(tPicture));
			bSuccess1 = iWordVersion == 1 &&
					bGet1PicInfo(iFodo, aucFpage + 1,
						tChrLen, &tPicture);
			bSuccess2 = iWordVersion == 2 &&
					bGet2PicInfo(iFodo, aucFpage + 1,
						tChrLen, &tPicture);
			if (bSuccess1 || bSuccess2) {
				tPicture.ulFileOffset = ulFileOffset;
				tPicture.ulFileOffsetPicture =
						tPicture.ulPictureOffset;
				vAdd2PictInfoList(&tPicture);
			}
		}
	}
	ausCharPage = xfree(ausCharPage);
} /* end of vGet2ChrInfo */
