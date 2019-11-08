/*
 * fontlist.c
 * Copyright (C) 1998-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Build, read and destroy a list of Word font information
 */

#include <stdlib.h>
#include <stddef.h>
#include "antiword.h"


/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct font_desc_tag {
	font_block_type tInfo;
	struct font_desc_tag    *pNext;
} font_mem_type;

/* Variables needed to write the Font Information List */
static font_mem_type	*pAnchor = NULL;
static font_mem_type	*pFontLast = NULL;


/*
 * vDestroyFontInfoList - destroy the Font Information List
 */
void
vDestroyFontInfoList(void)
{
	font_mem_type	*pCurr, *pNext;

	DBG_MSG("vDestroyFontInfoList");

	/* Free the Font Information List */
	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all control variables */
	pFontLast = NULL;
} /* end of vDestroyFontInfoList */

/*
 * vCorrectFontValues - correct font values to values Antiword can use
 */
void
vCorrectFontValues(font_block_type *pFontBlock)
{
	UINT	uiRealSize;
	USHORT	usRealStyle;

	uiRealSize = pFontBlock->usFontSize;
	usRealStyle = pFontBlock->usFontStyle;
	if (bIsSmallCapitals(pFontBlock->usFontStyle)) {
		/* Small capitals become normal capitals in a smaller font */
		uiRealSize = (uiRealSize * 4 + 2) / 5;
		usRealStyle &= ~FONT_SMALL_CAPITALS;
		usRealStyle |= FONT_CAPITALS;
	}
	if (bIsSuperscript(pFontBlock->usFontStyle) ||
	    bIsSubscript(pFontBlock->usFontStyle)) {
		/* Superscript and subscript use a smaller fontsize */
		uiRealSize = (uiRealSize * 2 + 1) / 3;
	}

	if (uiRealSize < MIN_FONT_SIZE) {
		DBG_DEC(uiRealSize);
		uiRealSize = MIN_FONT_SIZE;
	} else if (uiRealSize > MAX_FONT_SIZE) {
		DBG_DEC(uiRealSize);
		uiRealSize = MAX_FONT_SIZE;
	}

	pFontBlock->usFontSize = (USHORT)uiRealSize;
	if (pFontBlock->ucFontColor == 8) {
		/* White text to light gray text */
		pFontBlock->ucFontColor = 16;
	}
	pFontBlock->usFontStyle = usRealStyle;
} /* end of vCorrectFontValues */

/*
 * vAdd2FontInfoList - Add an element to the Font Information List
 */
void
vAdd2FontInfoList(const font_block_type *pFontBlock)
{
	font_mem_type	*pListMember;

	fail(pFontBlock == NULL);

	NO_DBG_MSG("bAdd2FontInfoList");

	if (pFontBlock->ulFileOffset == FC_INVALID) {
		/*
		 * This offset is really past the end of the file,
		 * so don't waste any memory by storing it.
		 */
		return;
	}

	NO_DBG_HEX(pFontBlock->ulFileOffset);
	NO_DBG_DEC_C(pFontBlock->ucFontNumber != 0,
					pFontBlock->ucFontNumber);
	NO_DBG_DEC_C(pFontBlock->usFontSize != DEFAULT_FONT_SIZE,
					pFontBlock->usFontSize);
	NO_DBG_DEC_C(pFontBlock->ucFontColor != 0,
					pFontBlock->ucFontColor);
	NO_DBG_HEX_C(pFontBlock->usFontStyle != 0x00,
					pFontBlock->usFontStyle);

	if (pFontLast != NULL &&
	    pFontLast->tInfo.ulFileOffset == pFontBlock->ulFileOffset) {
		/*
		 * If two consecutive fonts share the same
		 * offset, remember only the last font
		 */
		fail(pFontLast->pNext != NULL);
		pFontLast->tInfo = *pFontBlock;
		return;
	}

	/* Create list member */
	pListMember = xmalloc(sizeof(font_mem_type));
	/* Fill the list member */
	pListMember->tInfo = *pFontBlock;
	pListMember->pNext = NULL;
	/* Correct the values where needed */
	vCorrectFontValues(&pListMember->tInfo);
	/* Add the new member to the list */
	if (pAnchor == NULL) {
		pAnchor = pListMember;
	} else {
		fail(pFontLast == NULL);
		pFontLast->pNext = pListMember;
	}
	pFontLast = pListMember;
} /* end of vAdd2FontInfoList */

/*
 * Get the record that follows the given recored in the Font Information List
 */
const font_block_type *
pGetNextFontInfoListItem(const font_block_type *pCurr)
{
	const font_mem_type	*pRecord;
	size_t	tOffset;

	if (pCurr == NULL) {
		if (pAnchor == NULL) {
			/* There are no records */
			return NULL;
		}
		/* The first record is the only one without a predecessor */
		return &pAnchor->tInfo;
	}
	tOffset = offsetof(font_mem_type, tInfo);
	/* Many casts to prevent alignment warnings */
	pRecord = (font_mem_type *)(void *)((char *)pCurr - tOffset);
	fail(pCurr != &pRecord->tInfo);
	if (pRecord->pNext == NULL) {
		/* The last record has no successor */
		return NULL;
	}
	return &pRecord->pNext->tInfo;
} /* end of pGetNextFontInfoListItem */
