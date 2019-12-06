/*
 * rowlist.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GPL
 *
 * Description:
 * Build, read and destroy a list of Word table-row information
 */

#include <stdlib.h>
#include <string.h>
#include "antiword.h"

/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct row_desc_tag {
	row_block_type		tInfo;
	struct row_desc_tag	*pNext;
} row_desc_type;

/* Variables needed to write the Row Information List */
static row_desc_type	*pAnchor = NULL;
static row_desc_type	*pRowLast = NULL;
/* Variable needed to read the Row Information List */
static row_desc_type	*pRowCurrent = NULL;


/*
 * vDestroyRowInfoList - destroy the Row Information List
 */
void
vDestroyRowInfoList(void)
{
	row_desc_type	*pCurr, *pNext;

	DBG_MSG("vDestroyRowInfoList");

	/* Free the Row Information List */
	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all control variables */
	pRowLast = NULL;
	pRowCurrent = NULL;
} /* end of vDestroyRowInfoList */

/*
 * vAdd2RowInfoList - Add an element to the Row Information List
 */
void
vAdd2RowInfoList(const row_block_type *pRowBlock)
{
	row_desc_type	*pListMember;
	short		*psTmp;
	int		iIndex;

	fail(pRowBlock == NULL);

	if (pRowBlock->ulFileOffsetStart == FC_INVALID ||
	    pRowBlock->ulFileOffsetEnd == FC_INVALID ||
	    pRowBlock->ulFileOffsetStart == pRowBlock->ulFileOffsetEnd) {
		DBG_HEX_C(pRowBlock->ulFileOffsetStart != FC_INVALID,
			pRowBlock->ulFileOffsetStart);
		DBG_HEX_C(pRowBlock->ulFileOffsetEnd != FC_INVALID,
			pRowBlock->ulFileOffsetEnd);
		return;
	}

	NO_DBG_HEX(pRowBlock->ulFileOffsetStart);
	NO_DBG_HEX(pRowBlock->ulFileOffsetEnd);
	NO_DBG_DEC(pRowBlock->ucNumberOfColumns);

	/* Create the new list member */
	pListMember = xmalloc(sizeof(row_desc_type));
	/* Fill the new list member */
	pListMember->tInfo = *pRowBlock;
	pListMember->pNext = NULL;
	/* Correct the values where needed */
	for (iIndex = 0, psTmp = pListMember->tInfo.asColumnWidth;
	     iIndex < (int)pListMember->tInfo.ucNumberOfColumns;
	     iIndex++, psTmp++) {
		if (*psTmp < 0) {
			*psTmp = 0;
			DBG_MSG("The column width was negative");
		}
	}
	/* Add the new member to the list */
	if (pAnchor == NULL) {
		pAnchor = pListMember;
		pRowCurrent = pListMember;
	} else {
		fail(pRowLast == NULL);
		pRowLast->pNext = pListMember;
	}
	pRowLast = pListMember;
} /* end of vAdd2RowInfoList */

/*
 * Get the next item in the Row Information List
 */
const row_block_type *
pGetNextRowInfoListItem(void)
{
	const row_block_type	*pItem;

	if (pRowCurrent == NULL) {
		return NULL;
	}
	pItem = &pRowCurrent->tInfo;
	pRowCurrent = pRowCurrent->pNext;
	return pItem;
} /* end of pGetNextRowInfoListItem */
