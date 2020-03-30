/*
 * sectlist.c
 * Copyright (C) 2001-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Build, read and destroy list(s) of Word section information
 */

#include <stddef.h>
#include <string.h>
#include "antiword.h"


/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct section_mem_tag {
	section_block_type	tInfo;
	ULONG			ulCharPos;
	struct section_mem_tag	*pNext;
} section_mem_type;

/* Variables needed to write the Section Information List */
static section_mem_type	*pAnchor = NULL;
static section_mem_type	*pSectionLast = NULL;


/*
 * vDestroySectionInfoList - destroy the Section Information List
 */
void
vDestroySectionInfoList(void)
{
	section_mem_type	*pCurr, *pNext;

	DBG_MSG("vDestroySectionInfoList");

	/* Free the Section Information List */
	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all control variables */
	pSectionLast = NULL;
} /* end of vDestroySectionInfoList */

/*
 * vAdd2SectionInfoList - Add an element to the Section Information List
 */
void
vAdd2SectionInfoList(const section_block_type *pSection, ULONG ulCharPos)
{
	section_mem_type	*pListMember;

	fail(pSection == NULL);

	/* Create list member */
	pListMember = xmalloc(sizeof(section_mem_type));
	/* Fill the list member */
	pListMember->tInfo = *pSection;
	pListMember->ulCharPos = ulCharPos;
	pListMember->pNext = NULL;
	/* Add the new member to the list */
	if (pAnchor == NULL) {
		pAnchor = pListMember;
	} else {
		fail(pSectionLast == NULL);
		pSectionLast->pNext = pListMember;
	}
	pSectionLast = pListMember;
} /* vAdd2SectionInfoList */

/*
 * vGetDefaultSection - fill the section struct with default values
 */
void
vGetDefaultSection(section_block_type *pSection)
{
	(void)memset(pSection, 0, sizeof(*pSection));
	pSection->bNewPage = TRUE;
} /* end of vGetDefaultSection */

/*
 * vDefault2SectionInfoList - Add a default to the Section Information List
 */
void
vDefault2SectionInfoList(ULONG ulCharPos)
{
	section_block_type	tSection;

	vGetDefaultSection(&tSection);
	vAdd2SectionInfoList(&tSection, ulCharPos);
} /* end of vDefault2SectionInfoList */

/*
 * pGetSectionInfo - get the section information
 */
const section_block_type *
pGetSectionInfo(const section_block_type *pOld, ULONG ulCharPos)
{
	const section_mem_type	*pCurr;

	if (pOld == NULL || ulCharPos == 0) {
		if (pAnchor == NULL) {
			/* There are no records, make one */
			vDefault2SectionInfoList(0);
			fail(pAnchor == NULL);
		}
		/* The first record */
		NO_DBG_MSG("First record");
		return &pAnchor->tInfo;
	}

	NO_DBG_HEX(ulCharPos);
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		NO_DBG_HEX(pCurr->ulCharPos);
		if (ulCharPos == pCurr->ulCharPos ||
		    ulCharPos + 1 == pCurr->ulCharPos) {
			NO_DBG_HEX(pCurr->ulCharPos);
			return &pCurr->tInfo;
		}
	}
	return pOld;
} /* end of pGetSectionInfo */

/*
 * tGetNumberOfSections - get the number of sections
 */
size_t
tGetNumberOfSections(void)
{
	const section_mem_type	*pCurr;
	size_t	tCounter;

	for (tCounter = 0, pCurr = pAnchor;
	     pCurr != NULL;
	     tCounter++, pCurr = pCurr->pNext)
		;	/* Empty */
	return tCounter;
} /* end of tGetNumberOfSections */

/*
 * ucGetSepHdrFtrSpecification - get the Heder/footer specification
 */
UCHAR
ucGetSepHdrFtrSpecification(size_t tSectionNumber)
{
	const section_mem_type	*pCurr;
	size_t	tIndex;

	for (tIndex = 0, pCurr = pAnchor;
	     tIndex < tSectionNumber && pCurr != NULL;
	     tIndex++, pCurr = pCurr->pNext)
		;	/* Empty */
	if (pCurr == NULL) {
		DBG_DEC(tSectionNumber);
		DBG_FIXME();
		return 0x00;
	}
	return pCurr->tInfo.ucHdrFtrSpecification;
} /* end of ucGetSepHdrFtrSpecification */
