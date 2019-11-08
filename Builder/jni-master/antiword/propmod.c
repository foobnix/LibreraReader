/*
 * propmod.c
 * Copyright (C) 2001-2003 A.J. van Os; Released under GPL
 *
 * Description:
 * Build, read and destroy a list (array) of Word property modifiers
 */

#include <stdio.h>
#include <string.h>
#include "antiword.h"

#if defined(DEBUG)
#define ELEMENTS_TO_ADD	 3
#else
#define ELEMENTS_TO_ADD	30
#endif /* DEBUG */

/* Variables needed to write the property modifier list */
static UCHAR	**ppAnchor = NULL;
static size_t	tNextFree = 0;
static size_t	tMaxElements = 0;


/*
 * vDestroyPropModList - destroy the property modifier list
 */
void
vDestroyPropModList(void)
{
	size_t	tIndex;

	DBG_MSG("vDestroyPropModList");

	/* Free all the elements of the list */
	for (tIndex = 0; tIndex < tNextFree; tIndex++) {
		ppAnchor[tIndex] = xfree(ppAnchor[tIndex]);
	}
	/* Free the list itself */
	ppAnchor = xfree(ppAnchor);
	/* Reset all control variables */
	tNextFree = 0;
	tMaxElements = 0;
} /* end of vDestroyPropModList */

/*
 * vAdd2PropModList - add an element to the property modifier list
 */
void
vAdd2PropModList(const UCHAR *aucPropMod)
{
	size_t	tSize, tLen;

	fail(aucPropMod == NULL);

	NO_DBG_MSG("vAdd2PropModList");

	if (tNextFree >= tMaxElements) {
		tMaxElements += ELEMENTS_TO_ADD;
		tSize = tMaxElements * sizeof(UCHAR **);
		ppAnchor = xrealloc(ppAnchor, tSize);
	}
	NO_DBG_DEC(tNextFree);

	tLen = 2 + (size_t)usGetWord(0, aucPropMod);
	NO_DBG_HEX(tLen);
	NO_DBG_PRINT_BLOCK(pucPropMod, tLen);
	ppAnchor[tNextFree] = xmalloc(tLen);
	memcpy(ppAnchor[tNextFree], aucPropMod, tLen);
	tNextFree++;
} /* end of vAdd2PropModList */

/*
 * aucReadPropModListItem - get an item of the property modifier list
 */
const UCHAR *
aucReadPropModListItem(USHORT usPropMod)
{
	static UCHAR	aucBuffer[4];
	size_t	tIndex;

	if (usPropMod == IGNORE_PROPMOD) {
		/* This Properties Modifier must be ignored */
		return NULL;
	}

	if (!odd(usPropMod)) {
		/* Variant 1: The information is in the input ifself */
		aucBuffer[0] = 2;
		aucBuffer[1] = 0;
		aucBuffer[2] = (UCHAR)((usPropMod & 0x00fe) >> 1);
		aucBuffer[3] = (UCHAR)((usPropMod & 0xff00) >> 8);
		return aucBuffer;
	}

	if (ppAnchor == NULL) {
		/* No information available */
		return NULL;
	}

	/* Variant 2: The input contains an index */
	tIndex = (size_t)(usPropMod >> 1);
	if (tIndex >= tNextFree) {
		DBG_HEX(usPropMod);
		DBG_DEC(tIndex);
		DBG_DEC(tNextFree);
		return NULL;
	}
	return ppAnchor[tIndex];
} /* end of aucGetPropModListItem */
