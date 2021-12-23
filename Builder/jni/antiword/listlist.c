/*
 * listlist.c
 * Copyright (C) 2002,2003 A.J. van Os; Released under GPL
 *
 * Description:
 * Build, read and destroy a list of Word list information
 *
 * Note:
 * This list only exists when the Word document is saved by Word 8 or later
 */

#include "antiword.h"

/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct list_desc_tag {
	list_block_type		tInfo;
	ULONG			ulListID;
	USHORT			usIstd;
	UCHAR			ucListLevel;
	struct list_desc_tag	*pNext;
} list_desc_type;

typedef struct list_value_tag {
	USHORT			usValue;
	USHORT			usListIndex;
	UCHAR			ucListLevel;
	struct list_value_tag	*pNext;
} list_value_type;

/* Variables needed to describe the LFO list (pllfo) */
static ULONG		*aulLfoList = NULL;
static USHORT		usLfoLen = 0;
/* Variables needed to write the List Information List */
static list_desc_type	*pAnchor = NULL;
static list_desc_type	*pBlockLast = NULL;
/* Variable needed for numbering new lists */
static list_value_type	*pValues = NULL;
/* Variables needed for numbering old lists */
static int	iOldListSeqNumber = 0;
static USHORT	usOldListValue = 0;


/*
 * vDestroyListInfoList - destroy the List Information List
 */
void
vDestroyListInfoList(void)
{
	list_desc_type	*pCurr, *pNext;
	list_value_type	*pValueCurr, *pValueNext;

	DBG_MSG("vDestroyListInfoList");

	/* Free the LFO list */
	usLfoLen = 0;
	aulLfoList = xfree(aulLfoList);

	/* Free the List Information List */
	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all control variables */
	pBlockLast = NULL;

	/* Free the values list */
	pValueCurr = pValues;
	while (pValueCurr != NULL) {
		pValueNext = pValueCurr->pNext;
		pValueCurr = xfree(pValueCurr);
		pValueCurr = pValueNext;
	}
	pValues = NULL;
	/* Reset the values for the old lists */
	iOldListSeqNumber = 0;
	usOldListValue = 0;
} /* end of vDestroyListInfoList */

/*
 * vBuildLfoList - build the LFO list (pllfo)
 */
void
vBuildLfoList(const UCHAR *aucBuffer, size_t tBufLen)
{
	size_t	tRecords;
	int	iIndex;

	fail(aucBuffer == NULL);

	if (tBufLen < 4) {
		return;
	}
	tRecords = (size_t)ulGetLong(0, aucBuffer);
	NO_DBG_DEC(tRecords);
	if (4 + 16 * tRecords > tBufLen || tRecords >= 0x7fff) {
		/* Just a sanity check */
		DBG_DEC(tRecords);
		DBG_DEC(4 + 16 * tRecords);
		DBG_DEC(tBufLen);
		return;
	}
	aulLfoList = xcalloc(tRecords, sizeof(ULONG));
	for (iIndex = 0; iIndex < (int)tRecords; iIndex++) {
		aulLfoList[iIndex] = ulGetLong(4 + 16 * iIndex, aucBuffer);
		NO_DBG_HEX(aulLfoList[iIndex]);
	}
	usLfoLen = (USHORT)tRecords;
} /* end of vBuildLfoList */

/*
 * vAdd2ListInfoList - add an element to the List Information list
 */
void
vAdd2ListInfoList(ULONG ulListID, USHORT usIstd, UCHAR ucListLevel,
	const list_block_type *pListBlock)
{
	list_desc_type	*pListMember;

	fail(pListBlock == NULL);

	NO_DBG_HEX(ulListID);
	NO_DBG_DEC(usIstd);
	NO_DBG_DEC(ucListLevel);
	NO_DBG_DEC(pListBlock->ulStartAt);
	NO_DBG_DEC(pListBlock->bNoRestart);
	NO_DBG_DEC(pListBlock->sLeftIndent);
	NO_DBG_HEX(pListBlock->ucNFC);
	NO_DBG_HEX(pListBlock->usListChar);

	/* Create list member */
	pListMember = xmalloc(sizeof(list_desc_type));
	/* Fill the list member */
	pListMember->tInfo = *pListBlock;
	pListMember->ulListID = ulListID;
	pListMember->usIstd = usIstd;
	pListMember->ucListLevel = ucListLevel;
	pListMember->pNext = NULL;
	/* Correct the values where needed */
	if (pListMember->tInfo.ulStartAt > 0xffff) {
		DBG_DEC(pListMember->tInfo.ulStartAt);
		pListMember->tInfo.ulStartAt = 1;
	}
	/* Add the new member to the list */
	if (pAnchor == NULL) {
		pAnchor = pListMember;
	} else {
		fail(pBlockLast == NULL);
		pBlockLast->pNext = pListMember;
	}
	pBlockLast = pListMember;
} /* end of vAdd2ListInfoList */

/*
 * Get a matching record from the List Information List
 *
 * Returns NULL if no matching records is found
 */
const list_block_type *
pGetListInfo(USHORT usListIndex, UCHAR ucListLevel)
{
	list_desc_type	*pCurr;
	list_block_type	*pNearMatch;
	ULONG	ulListID;

	if (usListIndex == 0) {
		return NULL;
	}
	if (usListIndex - 1 >= usLfoLen || ucListLevel > 8) {
		DBG_DEC(usListIndex);
		DBG_DEC(ucListLevel);
		return NULL;
	}
	fail(aulLfoList == NULL);
	ulListID = aulLfoList[usListIndex - 1];
	NO_DBG_HEX(ulListID);

	pNearMatch = NULL;
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (pCurr->ulListID != ulListID) {
			/* No match */
			continue;
		}
		if (pCurr->ucListLevel == ucListLevel) {
			/* Exact match */
			return &pCurr->tInfo;
		}
		if (pCurr->ucListLevel == 0) {
			/* Near match */
			pNearMatch = &pCurr->tInfo;
		}
	}
	/* No exact match, use a near match if any */
	return pNearMatch;
} /* end of pGetListInfo */

/*
 * Get a matching record from the List Information List
 *
 * Returns NULL if no matching records is found
 */
const list_block_type *
pGetListInfoByIstd(USHORT usIstd)
{
	list_desc_type	*pCurr;

	if (usIstd == ISTD_INVALID || usIstd == STI_NIL || usIstd == STI_USER) {
		return NULL;
	}

	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (pCurr->usIstd == usIstd) {
			return &pCurr->tInfo;
		}
	}
	return NULL;
} /* end of pGetListInfoByIstd */

/*
 * vRestartListValues - reset the less significant list levels
 */
static void
vRestartListValues(USHORT usListIndex, UCHAR ucListLevel)
{
	list_value_type	*pPrev, *pCurr, *pNext;
	int		iCounter;

	iCounter = 0;
	pPrev = NULL;
	pCurr = pValues;

	while (pCurr != NULL) {
		if (pCurr->usListIndex != usListIndex ||
		    pCurr->ucListLevel <= ucListLevel) {
			pPrev = pCurr;
			pCurr = pCurr->pNext;
			continue;
		}
		/* Reset the level by deleting the record */
		pNext = pCurr->pNext;
		if (pPrev == NULL) {
			pValues = pNext;
		} else {
			pPrev->pNext = pNext;
		}
		DBG_DEC(pCurr->usListIndex);
		DBG_DEC(pCurr->ucListLevel);
		pCurr = xfree(pCurr);
		pCurr = pNext;
		iCounter++;
	}
	DBG_DEC_C(iCounter > 0, iCounter);
} /* end of vRestartListValues */

/*
 * usGetListValue - Get the current value of the given list
 *
 * Returns the value of the given list
 */
USHORT
usGetListValue(int iListSeqNumber, int iWordVersion,
	const style_block_type *pStyle)
{
	list_value_type	*pCurr;
	USHORT		usValue;

	fail(iListSeqNumber < 0);
	fail(iListSeqNumber < iOldListSeqNumber);
	fail(iWordVersion < 0);
	fail(pStyle == NULL);

	if (iListSeqNumber <= 0) {
		return 0;
	}

	if (iWordVersion < 8) {
		/* Old style list */
		if (iListSeqNumber == iOldListSeqNumber ||
		    (iListSeqNumber == iOldListSeqNumber + 1 &&
		     eGetNumType(pStyle->ucNumLevel) == level_type_sequence)) {
			if (!pStyle->bNumPause) {
				usOldListValue++;
			}
		} else {
			usOldListValue = pStyle->usStartAt;
		}
		iOldListSeqNumber = iListSeqNumber;
		return usOldListValue;
	}

	/* New style list */
	if (pStyle->usListIndex == 0 ||
	    pStyle->usListIndex - 1 >= usLfoLen ||
	    pStyle->ucListLevel > 8) {
		/* Out of range; no need to search */
		return 0;
	}

	for (pCurr = pValues; pCurr != NULL; pCurr = pCurr->pNext) {
		if (pCurr->usListIndex == pStyle->usListIndex &&
		    pCurr->ucListLevel == pStyle->ucListLevel) {
			/* Record found; increment and return the value */
			pCurr->usValue++;
			usValue = pCurr->usValue;
			if (!pStyle->bNoRestart) {
				vRestartListValues(pStyle->usListIndex,
						pStyle->ucListLevel);
			}
			return usValue;
		}
	}

	/* Record not found; create it and add it to the front of the list */
	pCurr = xmalloc(sizeof(list_value_type));
	pCurr->usValue = pStyle->usStartAt;
	pCurr->usListIndex = pStyle->usListIndex;
	pCurr->ucListLevel = pStyle->ucListLevel;
	pCurr->pNext = pValues;
	pValues = pCurr;
	usValue = pCurr->usValue;
	if (!pStyle->bNoRestart) {
		vRestartListValues(pStyle->usListIndex, pStyle->ucListLevel);
	}
	return usValue;
} /* end of usGetListValue */
