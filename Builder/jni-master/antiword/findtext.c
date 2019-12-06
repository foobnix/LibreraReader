/*
 * findtext.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Find the blocks that contain the text of MS Word files
 */

#include <stdio.h>
#include <stdlib.h>
#include "antiword.h"


/*
 * bAddTextBlocks - Add the blocks to the text block list
 *
 * Returns TRUE when successful, FALSE if not
 */
BOOL
bAddTextBlocks(ULONG ulCharPosFirst, ULONG ulTotalLength,
	BOOL bUsesUnicode, USHORT usPropMod,
	ULONG ulStartBlock, const ULONG *aulBBD, size_t tBBDLen)
{
	text_block_type	tTextBlock;
	ULONG	ulCharPos, ulOffset, ulIndex;
	long	lToGo;

	fail(ulTotalLength > (ULONG)LONG_MAX / 2);
	fail(ulStartBlock > MAX_BLOCKNUMBER && ulStartBlock != END_OF_CHAIN);
	fail(aulBBD == NULL);

	NO_DBG_HEX(ulCharPosFirst);
	NO_DBG_DEC(ulTotalLength);

	if (bUsesUnicode) {
		/* One character equals two bytes */
		NO_DBG_MSG("Uses Unicode");
		lToGo = (long)ulTotalLength * 2;
	} else {
		/* One character equals one byte */
		NO_DBG_MSG("Uses ASCII");
		lToGo = (long)ulTotalLength;
	}

	ulCharPos = ulCharPosFirst;
	ulOffset = ulCharPosFirst;
	for (ulIndex = ulStartBlock;
	     ulIndex != END_OF_CHAIN && lToGo > 0;
	     ulIndex = aulBBD[ulIndex]) {
		if (ulIndex >= (ULONG)tBBDLen) {
			DBG_DEC(ulIndex);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
		if (ulOffset >= BIG_BLOCK_SIZE) {
			ulOffset -= BIG_BLOCK_SIZE;
			continue;
		}
		tTextBlock.ulFileOffset =
			(ulIndex + 1) * BIG_BLOCK_SIZE + ulOffset;
		tTextBlock.ulCharPos = ulCharPos;
		tTextBlock.ulLength = min(BIG_BLOCK_SIZE - ulOffset,
						(ULONG)lToGo);
		tTextBlock.bUsesUnicode = bUsesUnicode;
		tTextBlock.usPropMod = usPropMod;
		ulOffset = 0;
		if (!bAdd2TextBlockList(&tTextBlock)) {
			DBG_HEX(tTextBlock.ulFileOffset);
			DBG_HEX(tTextBlock.ulCharPos);
			DBG_DEC(tTextBlock.ulLength);
			DBG_DEC(tTextBlock.bUsesUnicode);
			DBG_DEC(tTextBlock.usPropMod);
			return FALSE;
		}
		ulCharPos += tTextBlock.ulLength;
		lToGo -= (long)tTextBlock.ulLength;
	}
	DBG_DEC_C(lToGo != 0, lToGo);
	return lToGo == 0;
} /* end of bAddTextBlocks */

/*
 * bGet6DocumentText - make a list of the text blocks of Word 6/7 files
 *
 * Code for "fast saved" files.
 *
 * Returns TRUE when successful, FALSE if not
 */
BOOL
bGet6DocumentText(FILE *pFile, BOOL bUsesUnicode, ULONG ulStartBlock,
	const ULONG *aulBBD, size_t tBBDLen, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginTextInfo, ulTextOffset, ulTotLength;
	size_t	tTextInfoLen;
	int	iIndex, iType, iOff, iLen, iPieces;
	USHORT	usPropMod;

	DBG_MSG("bGet6DocumentText");

	fail(pFile == NULL);
	fail(aulBBD == NULL);
	fail(aucHeader == NULL);

	ulBeginTextInfo = ulGetLong(0x160, aucHeader);	/* fcClx */
	DBG_HEX(ulBeginTextInfo);
	tTextInfoLen = (size_t)ulGetLong(0x164, aucHeader);	/* lcbClx */
	DBG_DEC(tTextInfoLen);

	aucBuffer = xmalloc(tTextInfoLen);
	if (!bReadBuffer(pFile, ulStartBlock,
			aulBBD, tBBDLen, BIG_BLOCK_SIZE,
			aucBuffer, ulBeginTextInfo, tTextInfoLen)) {
		aucBuffer = xfree(aucBuffer);
		return FALSE;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tTextInfoLen);

	iOff = 0;
	while ((size_t)iOff < tTextInfoLen) {
		iType = (int)ucGetByte(iOff, aucBuffer);
		iOff++;
		if (iType == 0) {
			DBG_FIXME();
			iOff++;
			continue;
		}
		if (iType == 1) {
			iLen = (int)usGetWord(iOff, aucBuffer);
			vAdd2PropModList(aucBuffer + iOff);
			iOff += iLen + 2;
			continue;
		}
		if (iType != 2) {
			werr(0, "Unknown type of 'fastsaved' format");
			aucBuffer = xfree(aucBuffer);
			return FALSE;
		}
		/* Type 2 */
		iLen = (int)usGetWord(iOff, aucBuffer);
		NO_DBG_DEC(iLen);
		iOff += 4;
		iPieces = (iLen - 4) / 12;
		DBG_DEC(iPieces);
		for (iIndex = 0; iIndex < iPieces; iIndex++) {
			ulTextOffset = ulGetLong(
				iOff + (iPieces + 1) * 4 + iIndex * 8 + 2,
				aucBuffer);
			usPropMod = usGetWord(
				iOff + (iPieces + 1) * 4 + iIndex * 8 + 6,
				aucBuffer);
			ulTotLength = ulGetLong(iOff + (iIndex + 1) * 4,
						aucBuffer) -
					ulGetLong(iOff + iIndex * 4,
						aucBuffer);
			NO_DBG_HEX_C(usPropMod != 0, usPropMod);
			if (!bAddTextBlocks(ulTextOffset, ulTotLength,
					bUsesUnicode, usPropMod,
					ulStartBlock,
					aulBBD, tBBDLen)) {
				aucBuffer = xfree(aucBuffer);
				return FALSE;
			}
		}
		break;
	}
	aucBuffer = xfree(aucBuffer);
	return TRUE;
} /* end of bGet6DocumentText */

/*
 * bGet8DocumentText - make a list of the text blocks of Word 8/97 files
 *
 * Returns TRUE when successful, FALSE if not
 */
BOOL
bGet8DocumentText(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader)
{
	const ULONG	*aulBlockDepot;
	UCHAR	*aucBuffer;
	ULONG	ulTextOffset, ulBeginTextInfo;
	ULONG	ulTotLength, ulLen;
	long	lIndex, lPieces, lOff;
	size_t	tTextInfoLen, tBlockDepotLen, tBlockSize;
	int	iType, iLen;
	BOOL	bUsesUnicode;
	USHORT	usPropMod;

	DBG_MSG("bGet8DocumentText");

	fail(pFile == NULL || pPPS == NULL);
	fail(aulBBD == NULL || aulSBD == NULL);
	fail(aucHeader == NULL);

  	ulBeginTextInfo = ulGetLong(0x1a2, aucHeader);	/* fcClx */
	DBG_HEX(ulBeginTextInfo);
	tTextInfoLen = (size_t)ulGetLong(0x1a6, aucHeader);	/* lcbClx */
	DBG_DEC(tTextInfoLen);

	DBG_DEC(pPPS->tTable.ulSB);
	DBG_HEX(pPPS->tTable.ulSize);
	if (pPPS->tTable.ulSize == 0) {
		return FALSE;
	}

	if (pPPS->tTable.ulSize < MIN_SIZE_FOR_BBD_USE) {
	  	/* Use the Small Block Depot */
		aulBlockDepot = aulSBD;
		tBlockDepotLen = tSBDLen;
		tBlockSize = SMALL_BLOCK_SIZE;
	} else {
	  	/* Use the Big Block Depot */
		aulBlockDepot = aulBBD;
		tBlockDepotLen = tBBDLen;
		tBlockSize = BIG_BLOCK_SIZE;
	}
	aucBuffer = xmalloc(tTextInfoLen);
	if (!bReadBuffer(pFile, pPPS->tTable.ulSB,
			aulBlockDepot, tBlockDepotLen, tBlockSize,
			aucBuffer, ulBeginTextInfo, tTextInfoLen)) {
		aucBuffer = xfree(aucBuffer);
		return FALSE;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tTextInfoLen);

	lOff = 0;
	while (lOff < (long)tTextInfoLen) {
		iType = (int)ucGetByte(lOff, aucBuffer);
		lOff++;
		if (iType == 0) {
			DBG_FIXME();
			lOff++;
			continue;
		}
		if (iType == 1) {
			iLen = (int)usGetWord(lOff, aucBuffer);
			vAdd2PropModList(aucBuffer + lOff);
			lOff += (long)iLen + 2;
			continue;
		}
		if (iType != 2) {
			werr(0, "Unknown type of 'fastsaved' format");
			aucBuffer = xfree(aucBuffer);
			return FALSE;
		}
		/* Type 2 */
		ulLen = ulGetLong(lOff, aucBuffer);
		if (ulLen < 4) {
			DBG_DEC(ulLen);
			return FALSE;
		}
		lOff += 4;
		lPieces = (long)((ulLen - 4) / 12);
		DBG_DEC(lPieces);
		for (lIndex = 0; lIndex < lPieces; lIndex++) {
			ulTextOffset = ulGetLong(
				lOff + (lPieces + 1) * 4 + lIndex * 8 + 2,
				aucBuffer);
			usPropMod = usGetWord(
				lOff + (lPieces + 1) * 4 + lIndex * 8 + 6,
				aucBuffer);
			ulTotLength = ulGetLong(lOff + (lIndex + 1) * 4,
						aucBuffer) -
					ulGetLong(lOff + lIndex * 4,
						aucBuffer);
			if ((ulTextOffset & BIT(30)) == 0) {
				bUsesUnicode = TRUE;
			} else {
				bUsesUnicode = FALSE;
				ulTextOffset &= ~BIT(30);
				ulTextOffset /= 2;
			}
			NO_DBG_HEX_C(usPropMod != 0, usPropMod);
			if (!bAddTextBlocks(ulTextOffset, ulTotLength,
					bUsesUnicode, usPropMod,
					pPPS->tWordDocument.ulSB,
					aulBBD, tBBDLen)) {
				aucBuffer = xfree(aucBuffer);
				return FALSE;
			}
		}
		break;
	}
	aucBuffer = xfree(aucBuffer);
	return TRUE;
} /* end of bGet8DocumentText */
