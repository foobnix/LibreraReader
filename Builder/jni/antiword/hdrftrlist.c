/*
 * hdrftrlist.c
 * Copyright (C) 2004,2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Build, read and destroy list(s) of Word Header/footer information
 */

#include <string.h>
#include "antiword.h"


#define HDR_EVEN_PAGES	0
#define HDR_ODD_PAGES	1
#define FTR_EVEN_PAGES	2
#define FTR_ODD_PAGES	3
#define HDR_FIRST_PAGE	4
#define FTR_FIRST_PAGE	5

/*
 * Private structures to hide the way the information
 * is stored from the rest of the program
 */
typedef struct hdrftr_local_tag {
	hdrftr_block_type	tInfo;
	ULONG			ulCharPosStart;
	ULONG			ulCharPosNext;
	BOOL			bUseful;
	BOOL			bTextOriginal;
} hdrftr_local_type;
typedef struct hdrftr_mem_tag {
	hdrftr_local_type	atElement[6];
} hdrftr_mem_type;

/* Variables needed to write the Header/footer Information List */
static hdrftr_mem_type	*pHdrFtrList = NULL;
static size_t		tHdrFtrLen = 0;


/*
 * vDestroyHdrFtrInfoList - destroy the Header/footer Information List
 */
void
vDestroyHdrFtrInfoList(void)
{
	hdrftr_mem_type *pRecord;
	output_type	*pCurr, *pNext;
	size_t		tHdrFtr, tIndex;

	DBG_MSG("vDestroyHdrFtrInfoList");

	/* Free the Header/footer Information List */
	for (tHdrFtr = 0; tHdrFtr < tHdrFtrLen; tHdrFtr++) {
		pRecord = pHdrFtrList + tHdrFtr;
		for (tIndex = 0;
		     tIndex < elementsof(pRecord->atElement);
		     tIndex++) {
			if (!pRecord->atElement[tIndex].bTextOriginal) {
				continue;
			}
			pCurr = pRecord->atElement[tIndex].tInfo.pText;
			while (pCurr != NULL) {
				pCurr->szStorage = xfree(pCurr->szStorage);
				pNext = pCurr->pNext;
				pCurr = xfree(pCurr);
				pCurr = pNext;
			}
		}
	}
	pHdrFtrList = xfree(pHdrFtrList);
	/* Reset all control variables */
	tHdrFtrLen = 0;
} /* end of vDestroyHdrFtrInfoList */

/*
 * vCreat8HdrFtrInfoList - Create the Header/footer Information List
 */
void
vCreat8HdrFtrInfoList(const ULONG *aulCharPos, size_t tLength)
{
	hdrftr_mem_type	*pListMember;
	size_t	tHdrFtr, tIndex, tMainIndex;

	fail(aulCharPos == NULL);

	DBG_DEC(tLength);
	if (tLength <= 1) {
		return;
	}
	tHdrFtrLen = tLength / 12;
	if (tLength % 12 != 0 && tLength % 12 != 1) {
		tHdrFtrLen++;
	}
	DBG_DEC(tHdrFtrLen);

	pHdrFtrList = xcalloc(tHdrFtrLen, sizeof(hdrftr_mem_type));

	for (tHdrFtr = 0; tHdrFtr < tHdrFtrLen; tHdrFtr++) {
		pListMember = pHdrFtrList + tHdrFtr;
		for (tIndex = 0, tMainIndex = tHdrFtr * 12;
		     tIndex < 6 && tMainIndex < tLength;
		     tIndex++, tMainIndex++) {
			pListMember->atElement[tIndex].tInfo.pText = NULL;
			pListMember->atElement[tIndex].ulCharPosStart =
						aulCharPos[tMainIndex];
			if (tMainIndex + 1 < tLength) {
				pListMember->atElement[tIndex].ulCharPosNext =
					aulCharPos[tMainIndex + 1];
			} else {
				pListMember->atElement[tIndex].ulCharPosNext =
					aulCharPos[tMainIndex];
			}
		}
	}
} /* end of vCreat8HdrFtrInfoList */

/*
 * vCreat6HdrFtrInfoList - Create the Header/footer Information List
 */
void
vCreat6HdrFtrInfoList(const ULONG *aulCharPos, size_t tLength)
{
	static const size_t	atIndex[] =
		{ SIZE_T_MAX, SIZE_T_MAX, FTR_FIRST_PAGE, HDR_FIRST_PAGE,
		  FTR_ODD_PAGES, FTR_EVEN_PAGES, HDR_ODD_PAGES, HDR_EVEN_PAGES,
		};
	hdrftr_mem_type	*pListMember;
	size_t	tHdrFtr, tTmp, tIndex, tMainIndex, tBit;
	UCHAR	ucDopSpecification, ucSepSpecification;

	fail(aulCharPos == NULL);

	DBG_DEC(tLength);
	if (tLength <= 1) {
		return;
	}
	tHdrFtrLen = tGetNumberOfSections();
	if (tHdrFtrLen == 0) {
		tHdrFtrLen = 1;
	}
	DBG_DEC(tHdrFtrLen);

	pHdrFtrList = xcalloc(tHdrFtrLen, sizeof(hdrftr_mem_type));

	/* Get the start index in aulCharPos */
	ucDopSpecification = ucGetDopHdrFtrSpecification();
	DBG_HEX(ucDopSpecification & 0xe0);
	tMainIndex = 0;
	for (tBit = 7; tBit >= 5; tBit--) {
		if ((ucDopSpecification & BIT(tBit)) != 0) {
			tMainIndex++;
		}
	}
	DBG_DEC(tMainIndex);

	for (tHdrFtr = 0; tHdrFtr < tHdrFtrLen; tHdrFtr++) {
		ucSepSpecification = ucGetSepHdrFtrSpecification(tHdrFtr);
		DBG_HEX(ucSepSpecification & 0xfc);
		pListMember = pHdrFtrList + tHdrFtr;
		for (tTmp = 0;
		     tTmp < elementsof(pListMember->atElement);
		     tTmp++) {
			pListMember->atElement[tTmp].tInfo.pText = NULL;
		}
		for (tBit = 7; tBit >= 2; tBit--) {
			if (tMainIndex >= tLength) {
				break;
			}
			if ((ucSepSpecification & BIT(tBit)) == 0) {
				continue;
			}
			tIndex = atIndex[tBit];
			fail(tIndex >= 6);
			pListMember->atElement[tIndex].ulCharPosStart =
				aulCharPos[tMainIndex];
			if (tMainIndex + 1 < tLength) {
				pListMember->atElement[tIndex].ulCharPosNext =
					aulCharPos[tMainIndex + 1];
			} else {
				pListMember->atElement[tIndex].ulCharPosNext =
					aulCharPos[tMainIndex];
			}
			tMainIndex++;
		}
	}
} /* end of vCreat6HdrFtrInfoList */

/*
 * vCreat2HdrFtrInfoList - Create the Header/footer Information List
 */
void
vCreat2HdrFtrInfoList(const ULONG *aulCharPos, size_t tLength)
{
	vCreat6HdrFtrInfoList(aulCharPos, tLength);
} /* end of vCreat2HdrFtrInfoList */

/*
 * pGetHdrFtrInfo - get the Header/footer information
 */
const hdrftr_block_type *
pGetHdrFtrInfo(int iSectionIndex,
	BOOL bWantHeader, BOOL bOddPage, BOOL bFirstInSection)
{
	hdrftr_mem_type	*pCurr;

	fail(iSectionIndex < 0);
	fail(pHdrFtrList == NULL && tHdrFtrLen != 0);

	if (pHdrFtrList == NULL || tHdrFtrLen == 0) {
		/* No information */
		return NULL;
	}

	if (iSectionIndex < 0) {
		iSectionIndex = 0;
	} else if (iSectionIndex >= (int)tHdrFtrLen) {
		iSectionIndex = (int)(tHdrFtrLen - 1);
	}

	pCurr = pHdrFtrList + iSectionIndex;

	if (bFirstInSection) {
		if (bWantHeader) {
			return &pCurr->atElement[HDR_FIRST_PAGE].tInfo;
		} else {
			return &pCurr->atElement[FTR_FIRST_PAGE].tInfo;
		}
	} else {
		if (bWantHeader) {
			if (bOddPage) {
				return &pCurr->atElement[HDR_ODD_PAGES].tInfo;
			} else {
				return &pCurr->atElement[HDR_EVEN_PAGES].tInfo;
			}
		} else {
			if (bOddPage) {
				return &pCurr->atElement[FTR_ODD_PAGES].tInfo;
			} else {
				return &pCurr->atElement[FTR_EVEN_PAGES].tInfo;
			}
		}
	}
} /* end of pGetHdrFtrInfo */

/*
 * lComputeHdrFtrHeight - compute the height of a header or footer
 *
 * Returns the height in DrawUnits
 */
static long
lComputeHdrFtrHeight(const output_type *pAnchor)
{
	const output_type *pCurr;
	long	lTotal;
	USHORT	usFontSizeMax;

	lTotal = 0;
	usFontSizeMax = 0;
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (pCurr->tNextFree == 1) {
			if (pCurr->szStorage[0] == PAR_END) {
				/* End of a paragraph */
				lTotal += lComputeLeading(usFontSizeMax);
				lTotal += lMilliPoints2DrawUnits(
						(long)pCurr->usFontSize * 200);
				usFontSizeMax = 0;
				continue;
			}
			if (pCurr->szStorage[0] == HARD_RETURN) {
				/* End of a line */
				lTotal += lComputeLeading(usFontSizeMax);
				usFontSizeMax = 0;
				continue;
			}
		}
		if (pCurr->usFontSize > usFontSizeMax) {
			usFontSizeMax = pCurr->usFontSize;
		}
	}
	if (usFontSizeMax != 0) {
		/* Height of the last paragraph */
		lTotal += lComputeLeading(usFontSizeMax);
	}
	return lTotal;
} /* end of lComputeHdrFtrHeight */

/*
 * vPrepareHdrFtrText - prepare the header/footer text
 */
void
vPrepareHdrFtrText(FILE *pFile)
{
	hdrftr_mem_type		*pCurr, *pPrev;
	hdrftr_local_type	*pTmp;
	output_type		*pText;
	size_t		tHdrFtr, tIndex;

	fail(pFile == NULL);
	fail(pHdrFtrList == NULL && tHdrFtrLen != 0);

	if (pHdrFtrList == NULL || tHdrFtrLen == 0) {
		/* No information */
		return;
	}

	/* Fill text, text height and useful-ness */
	for (tHdrFtr = 0; tHdrFtr < tHdrFtrLen; tHdrFtr++) {
		pCurr = pHdrFtrList + tHdrFtr;
		for (tIndex = 0;
		     tIndex < elementsof(pHdrFtrList->atElement);
		     tIndex++) {
			pTmp = &pCurr->atElement[tIndex];
			pTmp->bUseful =
				pTmp->ulCharPosStart != pTmp->ulCharPosNext;
			if (pTmp->bUseful) {
				pText = pHdrFtrDecryptor(pFile,
						pTmp->ulCharPosStart,
						pTmp->ulCharPosNext);
				pTmp->tInfo.pText = pText;
				pTmp->tInfo.lHeight =
						lComputeHdrFtrHeight(pText);
				pTmp->bTextOriginal = pText != NULL;
			} else {
				pTmp->tInfo.pText = NULL;
				pTmp->tInfo.lHeight = 0;
				pTmp->bTextOriginal = FALSE;
			}
		}
	}

	/* Replace not-useful records by using inheritance */
	if (pHdrFtrList->atElement[HDR_FIRST_PAGE].bUseful) {
		pTmp = &pHdrFtrList->atElement[HDR_ODD_PAGES];
		if (!pTmp->bUseful) {
			*pTmp = pHdrFtrList->atElement[HDR_FIRST_PAGE];
			pTmp->bTextOriginal = FALSE;
		}
		pTmp = &pHdrFtrList->atElement[HDR_EVEN_PAGES];
		if (!pTmp->bUseful) {
			*pTmp = pHdrFtrList->atElement[HDR_FIRST_PAGE];
			pTmp->bTextOriginal = FALSE;
		}
	}
	if (pHdrFtrList->atElement[FTR_FIRST_PAGE].bUseful) {
		pTmp = &pHdrFtrList->atElement[FTR_ODD_PAGES];
		if (!pTmp->bUseful) {
			*pTmp = pHdrFtrList->atElement[FTR_FIRST_PAGE];
			pTmp->bTextOriginal = FALSE;
		}
		pTmp = &pHdrFtrList->atElement[FTR_EVEN_PAGES];
		if (!pTmp->bUseful) {
			*pTmp = pHdrFtrList->atElement[FTR_FIRST_PAGE];
			pTmp->bTextOriginal = FALSE;
		}
	}
	for (tHdrFtr = 1, pCurr = &pHdrFtrList[1];
	     tHdrFtr < tHdrFtrLen;
	     tHdrFtr++, pCurr++) {
		pPrev = pCurr - 1;
		for (tIndex = 0;
		     tIndex < elementsof(pHdrFtrList->atElement);
		     tIndex++) {
			if (!pCurr->atElement[tIndex].bUseful &&
			    pPrev->atElement[tIndex].bUseful) {
				pCurr->atElement[tIndex] =
						pPrev->atElement[tIndex];
				pCurr->atElement[tIndex].bTextOriginal = FALSE;
			}
		}
	}
} /* end of vPrepareHdrFtrText */
