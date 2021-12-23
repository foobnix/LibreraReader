/*
 * finddata.c
 * Copyright (C) 2000-2002 A.J. van Os; Released under GPL
 *
 * Description:
 * Find the blocks that contain the data of MS Word files
 */

#include <stdio.h>
#include <stdlib.h>
#include "antiword.h"


/*
 * bAddDataBlocks - Add the blocks to the data block list
 *
 * Returns TRUE when successful, otherwise FALSE
 */
BOOL
bAddDataBlocks(ULONG ulDataPosFirst, ULONG ulTotalLength,
	ULONG ulStartBlock, const ULONG *aulBBD, size_t tBBDLen)
{
	data_block_type	tDataBlock;
	ULONG	ulDataPos, ulOffset, ulIndex;
	long	lToGo;
	BOOL	bSuccess;

	fail(ulTotalLength > (ULONG)LONG_MAX);
	fail(ulStartBlock > MAX_BLOCKNUMBER && ulStartBlock != END_OF_CHAIN);
	fail(aulBBD == NULL);

	NO_DBG_HEX(ulDataPosFirst);
	NO_DBG_DEC(ulTotalLength);

	lToGo = (long)ulTotalLength;

	ulDataPos = ulDataPosFirst;
	ulOffset = ulDataPosFirst;
	for (ulIndex = ulStartBlock;
	     ulIndex != END_OF_CHAIN && lToGo > 0;
	     ulIndex = aulBBD[ulIndex]) {
		if (ulIndex == UNUSED_BLOCK || ulIndex >= (ULONG)tBBDLen) {
			DBG_DEC(ulIndex);
			DBG_DEC(tBBDLen);
			return FALSE;
		}
		if (ulOffset >= BIG_BLOCK_SIZE) {
			ulOffset -= BIG_BLOCK_SIZE;
			continue;
		}
		tDataBlock.ulFileOffset =
			(ulIndex + 1) * BIG_BLOCK_SIZE + ulOffset;
		tDataBlock.ulDataPos = ulDataPos;
		tDataBlock.ulLength = min(BIG_BLOCK_SIZE - ulOffset,
						(ULONG)lToGo);
		fail(tDataBlock.ulLength > BIG_BLOCK_SIZE);
		ulOffset = 0;
		if (!bAdd2DataBlockList(&tDataBlock)) {
			DBG_HEX(tDataBlock.ulFileOffset);
			DBG_HEX(tDataBlock.ulDataPos);
			DBG_DEC(tDataBlock.ulLength);
			return FALSE;
		}
		ulDataPos += tDataBlock.ulLength;
		lToGo -= (long)tDataBlock.ulLength;
	}
	bSuccess = lToGo == 0 ||
		(ulTotalLength == (ULONG)LONG_MAX && ulIndex == END_OF_CHAIN);
	DBG_DEC_C(!bSuccess, lToGo);
	DBG_DEC_C(!bSuccess, ulTotalLength);
	DBG_DEC_C(!bSuccess, ulIndex);
	return bSuccess;
} /* end of bAddDataBlocks */

/*
 * bGet6DocumentData - make a list of the data blocks of Word 6/7 files
 *
 * Code for "fast saved" files.
 *
 * Returns TRUE when successful, otherwise FALSE
 */
BOOL
bGet6DocumentData(FILE *pFile, ULONG ulStartBlock,
	const ULONG *aulBBD, size_t tBBDLen, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginTextInfo, ulOffset, ulTotLength;
	size_t	tTextInfoLen;
	int	iIndex, iOff, iType, iLen, iPieces;

	DBG_MSG("bGet6DocumentData");

	fail(pFile == NULL);
	fail(aulBBD == NULL);
	fail(aucHeader == NULL);

	ulBeginTextInfo = ulGetLong(0x160, aucHeader);
	DBG_HEX(ulBeginTextInfo);
	tTextInfoLen = (size_t)ulGetLong(0x164, aucHeader);
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
	while (iOff < (int)tTextInfoLen) {
		iType = (int)ucGetByte(iOff, aucBuffer);
		iOff++;
		if (iType == 0) {
			iOff++;
			continue;
		}
		iLen = (int)usGetWord(iOff, aucBuffer);
		iOff += 2;
		if (iType == 1) {
			iOff += iLen;
			continue;
		}
		if (iType != 2) {
			werr(0, "Unknown type of 'fastsaved' format");
			aucBuffer = xfree(aucBuffer);
			return FALSE;
		}
		/* Type 2 */
		NO_DBG_DEC(iLen);
		iOff += 2;
		iPieces = (iLen - 4) / 12;
		DBG_DEC(iPieces);
		for (iIndex = 0; iIndex < iPieces; iIndex++) {
			ulOffset = ulGetLong(
				iOff + (iPieces + 1) * 4 + iIndex * 8 + 2,
				aucBuffer);
			ulTotLength = ulGetLong(iOff + (iIndex + 1) * 4,
						aucBuffer) -
					ulGetLong(iOff + iIndex * 4,
						aucBuffer);
			if (!bAddDataBlocks(ulOffset, ulTotLength,
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
} /* end of bGet6DocumentData */
