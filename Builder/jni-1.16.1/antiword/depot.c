/*
 * depot.c
 * Copyright (C) 1998-2002 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to compute the depot offset
 */

#include "antiword.h"

#define SIZE_RATIO	(BIG_BLOCK_SIZE/SMALL_BLOCK_SIZE)

static ULONG	*aulSmallBlockList = NULL;
static size_t	tSmallBlockListLen = 0;


/*
 * vDestroySmallBlockList - destroy the small block list
 */
void
vDestroySmallBlockList(void)
{
	DBG_MSG("vDestroySmallBlockList");

	aulSmallBlockList = xfree(aulSmallBlockList);
	tSmallBlockListLen = 0;
} /* end of vDestroySmalBlockList */

/*
 * vCreateSmallBlockList - create the small block list
 *
 * returns: TRUE when successful, otherwise FALSE
 */
BOOL
bCreateSmallBlockList(ULONG ulStartblock, const ULONG *aulBBD, size_t tBBDLen)
{
	ULONG	ulTmp;
	size_t	tSize;
	int	iIndex;

	fail(aulSmallBlockList != NULL);
	fail(tSmallBlockListLen != 0);
	fail(ulStartblock > MAX_BLOCKNUMBER && ulStartblock != END_OF_CHAIN);
	fail(aulBBD == NULL);
	fail(tBBDLen == 0);

	/* Find the length of the small block list */
	for (tSmallBlockListLen = 0, ulTmp = ulStartblock;
	     tSmallBlockListLen < tBBDLen && ulTmp != END_OF_CHAIN;
	     tSmallBlockListLen++, ulTmp = aulBBD[ulTmp]) {
		if (ulTmp >= (ULONG)tBBDLen) {
			DBG_DEC(ulTmp);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
	}
	DBG_DEC(tSmallBlockListLen);

	if (tSmallBlockListLen == 0) {
		/* There is no small block list */
		fail(ulStartblock != END_OF_CHAIN);
		aulSmallBlockList = NULL;
		return TRUE;
	}

	/* Create the small block list */
	tSize = tSmallBlockListLen * sizeof(ULONG);
	aulSmallBlockList = xmalloc(tSize);
	for (iIndex = 0, ulTmp = ulStartblock;
	     iIndex < (int)tBBDLen && ulTmp != END_OF_CHAIN;
	     iIndex++, ulTmp = aulBBD[ulTmp]) {
		if (ulTmp >= (ULONG)tBBDLen) {
			DBG_DEC(ulTmp);
			DBG_DEC(tBBDLen);
			werr(1, "The Big Block Depot is damaged");
		}
		aulSmallBlockList[iIndex] = ulTmp;
		NO_DBG_DEC(aulSmallBlockList[iIndex]);
	}
	return TRUE;
} /* end of bCreateSmallBlockList */

/*
 * ulDepotOffset - get the depot offset the block list
 */
ULONG
ulDepotOffset(ULONG ulIndex, size_t tBlockSize)
{
	ULONG	ulTmp;
	size_t	tTmp;

	fail(ulIndex >= ULONG_MAX / BIG_BLOCK_SIZE);

	switch (tBlockSize) {
	case BIG_BLOCK_SIZE:
		return (ulIndex + 1) * BIG_BLOCK_SIZE;
	case SMALL_BLOCK_SIZE:
		tTmp = (size_t)(ulIndex / SIZE_RATIO);
		ulTmp = ulIndex % SIZE_RATIO;
		if (aulSmallBlockList == NULL ||
		    tTmp >= tSmallBlockListLen) {
			DBG_HEX(aulSmallBlockList);
			DBG_DEC(tSmallBlockListLen);
			DBG_DEC(tTmp);
			return 0;
		}
		return ((aulSmallBlockList[tTmp] + 1) * SIZE_RATIO +
				ulTmp) * SMALL_BLOCK_SIZE;
	default:
		DBG_DEC(tBlockSize);
		DBG_FIXME();
		return 0;
	}
} /* end of ulDepotOffset */
