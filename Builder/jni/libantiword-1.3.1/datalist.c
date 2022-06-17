/*
 * datalist.c
 * Copyright (C) 2000-2002 A.J. van Os; Released under GPL
 *
 * Description:
 * Build, read and destroy a list of Word data blocks
 */

#include <stdlib.h>
#include <errno.h>
#include "antiword.h"

#if defined(__riscos)
#define EIO		42
#endif /* __riscos */


/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct data_mem_tag {
	data_block_type		tInfo;
	struct data_mem_tag	*pNext;
} data_mem_type;

/* Variable to describe the start of the data block list */
static data_mem_type	*pAnchor = NULL;
/* Variable needed to read the data block list */
static data_mem_type	*pBlockLast = NULL;
/* Variable needed to read the data block list */
static data_mem_type	*pBlockCurrent = NULL;
static ULONG	ulBlockOffset = 0;
static size_t	tByteNext = 0;
/* Last block read */
static UCHAR	aucBlock[BIG_BLOCK_SIZE];


/*
 * vDestroyDataBlockList - destroy the data block list
 */
void
vDestroyDataBlockList(void)
{
	data_mem_type	*pCurr, *pNext;

	DBG_MSG("vDestroyDataBlockList");

	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all the control variables */
	pBlockLast = NULL;
	pBlockCurrent = NULL;
	ulBlockOffset = 0;
	tByteNext = 0;
} /* end of vDestroyDataBlockList */

/*
 * bAdd2DataBlockList - add an element to the data block list
 *
 * Returns TRUE when successful, otherwise FALSE
 */
BOOL
bAdd2DataBlockList(const data_block_type *pDataBlock)
{
	data_mem_type	*pListMember;

	fail(pDataBlock == NULL);
	fail(pDataBlock->ulFileOffset == FC_INVALID);
	fail(pDataBlock->ulDataPos == CP_INVALID);
	fail(pDataBlock->ulLength == 0);

	NO_DBG_MSG("bAdd2DataBlockList");
	NO_DBG_HEX(pDataBlock->ulFileOffset);
	NO_DBG_HEX(pDataBlock->ulDataPos);
	NO_DBG_HEX(pDataBlock->ulLength);

	if (pDataBlock->ulFileOffset == FC_INVALID ||
	    pDataBlock->ulDataPos == CP_INVALID ||
	    pDataBlock->ulLength == 0) {
		werr(0, "Software (datablock) error");
		return FALSE;
	}
	/* Check for continuous blocks */
	if (pBlockLast != NULL &&
	    pBlockLast->tInfo.ulFileOffset +
	     pBlockLast->tInfo.ulLength == pDataBlock->ulFileOffset &&
	    pBlockLast->tInfo.ulDataPos +
	     pBlockLast->tInfo.ulLength == pDataBlock->ulDataPos) {
		/* These are continous blocks */
		pBlockLast->tInfo.ulLength += pDataBlock->ulLength;
		return TRUE;
	}
	/* Make a new block */
	pListMember = xmalloc(sizeof(data_mem_type));
	/* Add the block to the data list */
	pListMember->tInfo = *pDataBlock;
	pListMember->pNext = NULL;
	if (pAnchor == NULL) {
		pAnchor = pListMember;
	} else {
		fail(pBlockLast == NULL);
		pBlockLast->pNext = pListMember;
	}
	pBlockLast = pListMember;
	return TRUE;
} /* end of bAdd2DataBlockList */

/*
 * ulGetDataOffset - get the offset in the data block list
 *
 * Get the fileoffset the current position in the data block list
 */
ULONG
ulGetDataOffset(FILE *pFile)
{
	return pBlockCurrent->tInfo.ulFileOffset + ulBlockOffset + tByteNext;
} /* end of ulGetDataOffset */

/*
 * bSetDataOffset - set the offset in the data block list
 *
 * Make the given fileoffset the current position in the data block list
 */
BOOL
bSetDataOffset(FILE *pFile, ULONG ulFileOffset)
{
	data_mem_type	*pCurr;
	size_t	tReadLen;

	DBG_HEX(ulFileOffset);

	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (ulFileOffset < pCurr->tInfo.ulFileOffset ||
		    ulFileOffset >= pCurr->tInfo.ulFileOffset +
		     pCurr->tInfo.ulLength) {
			/* The file offset is not in this block */
			continue;
		}
		/* Compute the maximum number of bytes to read */
		tReadLen = (size_t)(pCurr->tInfo.ulFileOffset +
				pCurr->tInfo.ulLength -
				ulFileOffset);
		/* Compute the real number of bytes to read */
		if (tReadLen > sizeof(aucBlock)) {
			tReadLen = sizeof(aucBlock);
		}
		/* Read the bytes */
		if (!bReadBytes(aucBlock, tReadLen, ulFileOffset, pFile)) {
			return FALSE;
		}
		/* Set the control variables */
		pBlockCurrent = pCurr;
		ulBlockOffset = ulFileOffset - pCurr->tInfo.ulFileOffset;
		tByteNext = 0;
		return TRUE;
	}
	return FALSE;
} /* end of bSetDataOffset */

/*
 * iNextByte - get the next byte from the data block list
 */
int
iNextByte(FILE *pFile)
{
	ULONG	ulReadOff;
	size_t	tReadLen;

	fail(pBlockCurrent == NULL);

	if (tByteNext >= sizeof(aucBlock) ||
	    ulBlockOffset + tByteNext >= pBlockCurrent->tInfo.ulLength) {
		if (ulBlockOffset + sizeof(aucBlock) <
					pBlockCurrent->tInfo.ulLength) {
			/* Same block, next part */
			ulBlockOffset += sizeof(aucBlock);
		} else {
			/* Next block, first part */
			pBlockCurrent = pBlockCurrent->pNext;
			ulBlockOffset = 0;
		}
		if (pBlockCurrent == NULL) {
			/* Past the last part of the last block */
			errno = EIO;
			return EOF;
		}
		tReadLen = (size_t)
				(pBlockCurrent->tInfo.ulLength - ulBlockOffset);
		if (tReadLen > sizeof(aucBlock)) {
			tReadLen = sizeof(aucBlock);
		}
		ulReadOff = pBlockCurrent->tInfo.ulFileOffset + ulBlockOffset;
		if (!bReadBytes(aucBlock, tReadLen, ulReadOff, pFile)) {
			errno = EIO;
			return EOF;
		}
		tByteNext = 0;
	}
	return (int)aucBlock[tByteNext++];
} /* end of iNextByte */

/*
 * usNextWord - get the next word from the data block list
 *
 * Read a two byte value in Little Endian order, that means MSB last
 *
 * All return values can be valid so errno is set in case of error
 */
USHORT
usNextWord(FILE *pFile)
{
	USHORT	usLSB, usMSB;

	usLSB = (USHORT)iNextByte(pFile);
	if (usLSB == (USHORT)EOF) {
		errno = EIO;
		return (USHORT)EOF;
	}
	usMSB = (USHORT)iNextByte(pFile);
	if (usMSB == (USHORT)EOF) {
		DBG_MSG("usNextWord: Unexpected EOF");
		errno = EIO;
		return (USHORT)EOF;
	}
	return (usMSB << 8) | usLSB;
} /* end of usNextWord */

/*
 * ulNextLong - get the next long from the data block list
 *
 * Read a four byte value in Little Endian order, that means MSW last
 *
 * All return values can be valid so errno is set in case of error
 */
ULONG
ulNextLong(FILE *pFile)
{
	ULONG	ulLSW, ulMSW;

	ulLSW = (ULONG)usNextWord(pFile);
	if (ulLSW == (ULONG)EOF) {
		errno = EIO;
		return (ULONG)EOF;
	}
	ulMSW = (ULONG)usNextWord(pFile);
	if (ulMSW == (ULONG)EOF) {
		DBG_MSG("ulNextLong: Unexpected EOF");
		errno = EIO;
		return (ULONG)EOF;
	}
	return (ulMSW << 16) | ulLSW;
} /* end of ulNextLong */

/*
 * usNextWordBE - get the next two byte value
 *
 * Read a two byte value in Big Endian order, that means MSB first
 *
 * All return values can be valid so errno is set in case of error
 */
USHORT
usNextWordBE(FILE *pFile)
{
	USHORT usLSB, usMSB;

	usMSB = (USHORT)iNextByte(pFile);
	if (usMSB == (USHORT)EOF) {
		errno = EIO;
		return (USHORT)EOF;
	}
	usLSB = (USHORT)iNextByte(pFile);
	if (usLSB == (USHORT)EOF) {
		DBG_MSG("usNextWordBE: Unexpected EOF");
		errno = EIO;
		return (USHORT)EOF;
	}
	return (usMSB << 8) | usLSB;
} /* end of usNextWordBE */

/*
 * ulNextLongBE - get the next four byte value
 *
 * Read a four byte value in Big Endian order, that means MSW first
 *
 * All return values can be valid so errno is set in case of error
 */
ULONG
ulNextLongBE(FILE *pFile)
{
	ULONG	ulLSW, ulMSW;

	ulMSW = (ULONG)usNextWordBE(pFile);
	if (ulMSW == (ULONG)EOF) {
		errno = EIO;
		return (ULONG)EOF;
	}
	ulLSW = (ULONG)usNextWordBE(pFile);
	if (ulLSW == (ULONG)EOF) {
		DBG_MSG("ulNextLongBE: Unexpected EOF");
		errno = EIO;
		return (ULONG)EOF;
	}
	return (ulMSW << 16) | ulLSW;
} /* end of ulNextLongBE */

/*
 * tSkipBytes - skip over the given number of bytes
 *
 * Returns the number of skipped bytes
 */
size_t
tSkipBytes(FILE *pFile, size_t tToSkip)
{
	size_t	tToGo, tMaxMove, tMove;

	fail(pFile == NULL);
	fail(pBlockCurrent == NULL);

	tToGo = tToSkip;
	while (tToGo != 0) {
		/* Goto the end of the current block */
		tMaxMove = min(sizeof(aucBlock) - tByteNext,
				(size_t)(pBlockCurrent->tInfo.ulLength -
				ulBlockOffset - tByteNext));
		tMove = min(tMaxMove, tToGo);
		tByteNext += tMove;
		tToGo -= tMove;
		if (tToGo != 0) {
			/* Goto the next block */
			if (iNextByte(pFile) == EOF) {
				return tToSkip - tToGo;
			}
			tToGo--;
		}
	}
	return tToSkip;
} /* end of tSkipBytes */

/*
 * Translate  a data position to an offset in the file.
 * Logical to physical offset.
 *
 * Returns:	FC_INVALID: in case of error
 *		otherwise: the computed file offset
 */
ULONG
ulDataPos2FileOffset(ULONG ulDataPos)
{
	data_mem_type	*pCurr;

	fail(ulDataPos == CP_INVALID);

	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (ulDataPos < pCurr->tInfo.ulDataPos ||
		    ulDataPos >= pCurr->tInfo.ulDataPos +
		     pCurr->tInfo.ulLength) {
			/* The data offset is not in this block, try the next */
			continue;
		}
		/* The data offset is in the current block */
		return pCurr->tInfo.ulFileOffset +
				ulDataPos -
				pCurr->tInfo.ulDataPos;
	}
	/* Passed beyond the end of the list */
	DBG_HEX_C(ulDataPos != 0, ulDataPos);
	return FC_INVALID;
} /* end of ulDataPos2FileOffset */
