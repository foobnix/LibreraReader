/*
 * blocklist.c
 * Copyright (C) 1998-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Build, read and destroy the lists of Word "text" blocks
 */

#include <stdlib.h>
#include "antiword.h"


/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct list_mem_tag {
	text_block_type		tInfo;
	struct list_mem_tag	*pNext;
} list_mem_type;

typedef struct readinfo_tag {
	list_mem_type		*pBlockCurrent;
	ULONG			ulBlockOffset;
	size_t			tByteNext;
	UCHAR			aucBlock[BIG_BLOCK_SIZE];
} readinfo_type;

/* Variables to describe the start of the block lists */
static list_mem_type	*pTextAnchor = NULL;
static list_mem_type	*pFootnoteAnchor = NULL;
static list_mem_type	*pHdrFtrAnchor = NULL;
static list_mem_type	*pMacroAnchor = NULL;
static list_mem_type	*pAnnotationAnchor = NULL;
static list_mem_type	*pEndnoteAnchor = NULL;
static list_mem_type	*pTextBoxAnchor = NULL;
static list_mem_type	*pHdrTextBoxAnchor = NULL;
/* Variable needed to build the block list */
static list_mem_type	*pBlockLast = NULL;
/* Variable needed to read the block lists */
static readinfo_type	tOthers = { NULL, 0, 0, };
static readinfo_type	tHdrFtr = { NULL, 0, 0, };
static readinfo_type	tFootnote = { NULL, 0, 0, };


/*
 * pFreeOneList - free a text block list
 *
 * Will always return NULL
 */
static list_mem_type *
pFreeOneList(list_mem_type *pAnchor)
{
	list_mem_type	*pCurr, *pNext;

	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	return NULL;
} /* end of pFreeOneList */

/*
 * vDestroyTextBlockList - destroy the text block lists
 */
void
vDestroyTextBlockList(void)
{
	DBG_MSG("vDestroyTextBlockList");

	/* Free the lists one by one */
	pTextAnchor = pFreeOneList(pTextAnchor);
	pFootnoteAnchor = pFreeOneList(pFootnoteAnchor);
	pHdrFtrAnchor = pFreeOneList(pHdrFtrAnchor);
	pMacroAnchor = pFreeOneList(pMacroAnchor);
	pAnnotationAnchor = pFreeOneList(pAnnotationAnchor);
	pEndnoteAnchor = pFreeOneList(pEndnoteAnchor);
	pTextBoxAnchor = pFreeOneList(pTextBoxAnchor);
	pHdrTextBoxAnchor = pFreeOneList(pHdrTextBoxAnchor);
	/* Reset all the controle variables */
	pBlockLast = NULL;
	tOthers.pBlockCurrent = NULL;
	tHdrFtr.pBlockCurrent = NULL;
	tFootnote.pBlockCurrent = NULL;
} /* end of vDestroyTextBlockList */

/*
 * bAdd2TextBlockList - add an element to the text block list
 *
 * returns: TRUE when successful, otherwise FALSE
 */
BOOL
bAdd2TextBlockList(const text_block_type *pTextBlock)
{
	list_mem_type	*pListMember;

	fail(pTextBlock == NULL);
	fail(pTextBlock->ulFileOffset == FC_INVALID);
	fail(pTextBlock->ulCharPos == CP_INVALID);
	fail(pTextBlock->ulLength == 0);
	fail(pTextBlock->bUsesUnicode && odd(pTextBlock->ulLength));

	NO_DBG_MSG("bAdd2TextBlockList");
	NO_DBG_HEX(pTextBlock->ulFileOffset);
	NO_DBG_HEX(pTextBlock->ulCharPos);
	NO_DBG_HEX(pTextBlock->ulLength);
	NO_DBG_DEC(pTextBlock->bUsesUnicode);
	NO_DBG_DEC(pTextBlock->usPropMod);

	if (pTextBlock->ulFileOffset == FC_INVALID ||
	    pTextBlock->ulCharPos == CP_INVALID ||
	    pTextBlock->ulLength == 0 ||
	    (pTextBlock->bUsesUnicode && odd(pTextBlock->ulLength))) {
		werr(0, "Software (textblock) error");
		return FALSE;
	}
	/*
	 * Check for continuous blocks of the same character size and
	 * the same properties modifier
	 */
	if (pBlockLast != NULL &&
	    pBlockLast->tInfo.ulFileOffset +
	     pBlockLast->tInfo.ulLength == pTextBlock->ulFileOffset &&
	    pBlockLast->tInfo.ulCharPos +
	     pBlockLast->tInfo.ulLength == pTextBlock->ulCharPos &&
	    pBlockLast->tInfo.bUsesUnicode == pTextBlock->bUsesUnicode &&
	    pBlockLast->tInfo.usPropMod == pTextBlock->usPropMod) {
		/* These are continous blocks */
		pBlockLast->tInfo.ulLength += pTextBlock->ulLength;
		return TRUE;
	}
	/* Make a new block */
	pListMember = xmalloc(sizeof(list_mem_type));
	/* Add the block to the list */
	pListMember->tInfo = *pTextBlock;
	pListMember->pNext = NULL;
	if (pTextAnchor == NULL) {
		pTextAnchor = pListMember;
	} else {
		fail(pBlockLast == NULL);
		pBlockLast->pNext = pListMember;
	}
	pBlockLast = pListMember;
	return TRUE;
} /* end of bAdd2TextBlockList */

/*
 * vSpitList - Split the list in two
 */
static void
vSpitList(list_mem_type **ppAnchorCurr, list_mem_type **ppAnchorNext,
	ULONG ulListLen)
{
	list_mem_type	*pCurr;
	long		lCharsToGo, lBytesTooFar;

	fail(ppAnchorCurr == NULL);
	fail(ppAnchorNext == NULL);
	fail(ulListLen > (ULONG)LONG_MAX);

	pCurr = NULL;
	lCharsToGo = (long)ulListLen;
	lBytesTooFar = -1;
	if (ulListLen != 0) {
		DBG_DEC(ulListLen);
		for (pCurr = *ppAnchorCurr;
		     pCurr != NULL;
		     pCurr = pCurr->pNext) {
			NO_DBG_DEC(pCurr->tInfo.ulLength);
			fail(pCurr->tInfo.ulLength == 0);
			fail(pCurr->tInfo.ulLength > (ULONG)LONG_MAX);
			if (pCurr->tInfo.bUsesUnicode) {
				fail(odd(pCurr->tInfo.ulLength));
				lCharsToGo -= (long)(pCurr->tInfo.ulLength / 2);
				if (lCharsToGo < 0) {
					lBytesTooFar = -2 * lCharsToGo;
				}
			} else {
				lCharsToGo -= (long)pCurr->tInfo.ulLength;
				if (lCharsToGo < 0) {
					lBytesTooFar = -lCharsToGo;
				}
			}
			if (lCharsToGo <= 0) {
				break;
			}
		}
	}
/* Split the list */
	if (ulListLen == 0) {
		/* Current blocklist is empty */
		*ppAnchorNext = *ppAnchorCurr;
		*ppAnchorCurr = NULL;
	} else if (pCurr == NULL) {
		/* No blocks for the next list */
		*ppAnchorNext = NULL;
	} else if (lCharsToGo == 0) {
		/* Move the integral number of blocks to the next list */
		*ppAnchorNext = pCurr->pNext;
		pCurr->pNext = NULL;
	} else {
		/* Split the part current block list, part next block list */
		DBG_DEC(lBytesTooFar);
		fail(lBytesTooFar <= 0);
		*ppAnchorNext = xmalloc(sizeof(list_mem_type));
		DBG_HEX(pCurr->tInfo.ulFileOffset);
		(*ppAnchorNext)->tInfo.ulFileOffset =
				pCurr->tInfo.ulFileOffset +
				pCurr->tInfo.ulLength -
				lBytesTooFar;
		DBG_HEX((*ppAnchorNext)->tInfo.ulFileOffset);
		DBG_HEX(pCurr->tInfo.ulCharPos);
		(*ppAnchorNext)->tInfo.ulCharPos =
				pCurr->tInfo.ulCharPos +
				pCurr->tInfo.ulLength -
				lBytesTooFar;
		DBG_HEX((*ppAnchorNext)->tInfo.ulCharPos);
		(*ppAnchorNext)->tInfo.ulLength = (ULONG)lBytesTooFar;
		pCurr->tInfo.ulLength -= (ULONG)lBytesTooFar;
		(*ppAnchorNext)->tInfo.bUsesUnicode = pCurr->tInfo.bUsesUnicode;
		(*ppAnchorNext)->tInfo.usPropMod = pCurr->tInfo.usPropMod;
		/* Move the integral number of blocks to the next list */
		(*ppAnchorNext)->pNext = pCurr->pNext;
		pCurr->pNext = NULL;
	}
} /* end of vSpitList */

#if defined(DEBUG) || defined(__riscos)
/*
 * ulComputeListLength - compute the length of a list
 *
 * returns the list length in characters
 */
static ULONG
ulComputeListLength(const list_mem_type *pAnchor)
{
	const list_mem_type	*pCurr;
	ULONG		ulTotal;

	ulTotal = 0;
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		fail(pCurr->tInfo.ulLength == 0);
		if (pCurr->tInfo.bUsesUnicode) {
			fail(odd(pCurr->tInfo.ulLength));
			ulTotal += pCurr->tInfo.ulLength / 2;
		} else {
			ulTotal += pCurr->tInfo.ulLength;
		}
	}
	return ulTotal;
} /* end of ulComputeListLength */
#endif /* DEBUG || __riscos */

#if defined(DEBUG)
/*
 * vCheckList - check the number of bytes in a block list
 */
static void
vCheckList(const list_mem_type *pAnchor, ULONG ulListLen, char *szMsg)
{
	ULONG		ulTotal;

	ulTotal = ulComputeListLength(pAnchor);
	DBG_DEC(ulTotal);
	if (ulTotal != ulListLen) {
		DBG_DEC(ulListLen);
		werr(1, szMsg);
	}
} /* end of vCheckList */
#endif /* DEBUG */

/*
 * bIsEmptyBox - check to see if the given text box is empty
 */
static BOOL
bIsEmptyBox(FILE *pFile, const list_mem_type *pAnchor)
{
	const list_mem_type	*pCurr;
	size_t	tIndex, tSize;
	UCHAR	*aucBuffer;
	char	cChar;

	fail(pFile == NULL);

	if (pAnchor == NULL) {
		return TRUE;
	}

	aucBuffer = NULL;
	for (pCurr = pAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		fail(pCurr->tInfo.ulLength == 0);
		tSize = (size_t)pCurr->tInfo.ulLength;
#if defined(__dos) && !defined(__DJGPP__)
		if (pCurr->tInfo.ulLength > 0xffffUL) {
			tSize = 0xffff;
		}
#endif /* __dos && !__DJGPP__ */
		fail(aucBuffer != NULL);
		aucBuffer = xmalloc(tSize);
		if (!bReadBytes(aucBuffer, tSize,
				pCurr->tInfo.ulFileOffset, pFile)) {
			aucBuffer = xfree(aucBuffer);
			return FALSE;
		}
		for (tIndex = 0; tIndex < tSize; tIndex++) {
			cChar = (char)aucBuffer[tIndex];
			switch (cChar) {
			case '\0': case '\r': case '\n':
			case '\f': case '\t': case '\v':
			case ' ':
				break;
			default:
				aucBuffer = xfree(aucBuffer);
				return FALSE;
			}
		}
		aucBuffer = xfree(aucBuffer);
	}
	fail(aucBuffer != NULL);
	return TRUE;
} /* end of bIsEmptyBox */

/*
 * vSplitBlockList - split the block list in the various parts
 *
 * Split the blocklist in a Text block list, a Footnote block list, a
 * HeaderFooter block list, a Macro block list, an Annotation block list,
 * an Endnote block list, a TextBox list and a HeaderTextBox list.
 *
 * NOTE:
 * The various ul*Len input parameters are given in characters, but the
 * length of the blocks are in bytes.
 */
void
vSplitBlockList(FILE *pFile, ULONG ulTextLen, ULONG ulFootnoteLen,
	ULONG ulHdrFtrLen, ULONG ulMacroLen, ULONG ulAnnotationLen,
	ULONG ulEndnoteLen, ULONG ulTextBoxLen, ULONG ulHdrTextBoxLen,
	BOOL bMustExtend)
{
	list_mem_type	*apAnchors[8];
	list_mem_type	*pGarbageAnchor, *pCurr;
	size_t		tIndex;

	DBG_MSG("vSplitBlockList");

	pGarbageAnchor = NULL;

	DBG_MSG_C(ulTextLen != 0, "Text block list");
	vSpitList(&pTextAnchor, &pFootnoteAnchor, ulTextLen);
	DBG_MSG_C(ulFootnoteLen != 0, "Footnote block list");
	vSpitList(&pFootnoteAnchor, &pHdrFtrAnchor, ulFootnoteLen);
	DBG_MSG_C(ulHdrFtrLen != 0, "Header/Footer block list");
	vSpitList(&pHdrFtrAnchor, &pMacroAnchor, ulHdrFtrLen);
	DBG_MSG_C(ulMacroLen != 0, "Macro block list");
	vSpitList(&pMacroAnchor, &pAnnotationAnchor, ulMacroLen);
	DBG_MSG_C(ulAnnotationLen != 0, "Annotation block list");
	vSpitList(&pAnnotationAnchor, &pEndnoteAnchor, ulAnnotationLen);
	DBG_MSG_C(ulEndnoteLen != 0, "Endnote block list");
	vSpitList(&pEndnoteAnchor, &pTextBoxAnchor, ulEndnoteLen);
	DBG_MSG_C(ulTextBoxLen != 0, "Textbox block list");
	vSpitList(&pTextBoxAnchor, &pHdrTextBoxAnchor, ulTextBoxLen);
	DBG_MSG_C(ulHdrTextBoxLen != 0, "HeaderTextbox block list");
	vSpitList(&pHdrTextBoxAnchor, &pGarbageAnchor, ulHdrTextBoxLen);

	/* Free the garbage block list, this should not be needed */
	DBG_DEC_C(pGarbageAnchor != NULL, pGarbageAnchor->tInfo.ulLength);
	pGarbageAnchor = pFreeOneList(pGarbageAnchor);

#if defined(DEBUG)
	vCheckList(pTextAnchor, ulTextLen, "Software error (Text)");
	vCheckList(pFootnoteAnchor, ulFootnoteLen, "Software error (Footnote)");
	vCheckList(pHdrFtrAnchor, ulHdrFtrLen, "Software error (Hdr/Ftr)");
	vCheckList(pMacroAnchor, ulMacroLen, "Software error (Macro)");
	vCheckList(pAnnotationAnchor, ulAnnotationLen,
						"Software error (Annotation)");
	vCheckList(pEndnoteAnchor, ulEndnoteLen, "Software error (Endnote)");
	vCheckList(pTextBoxAnchor, ulTextBoxLen, "Software error (TextBox)");
	vCheckList(pHdrTextBoxAnchor, ulHdrTextBoxLen,
						"Software error (HdrTextBox)");
#endif /* DEBUG */

	/* Remove the list if the text box is empty */
	if (bIsEmptyBox(pFile, pTextBoxAnchor)) {
		pTextBoxAnchor = pFreeOneList(pTextBoxAnchor);
	}
	if (bIsEmptyBox(pFile, pHdrTextBoxAnchor)) {
		pHdrTextBoxAnchor = pFreeOneList(pHdrTextBoxAnchor);
	}

	if (!bMustExtend) {
		return;
	}
	/*
	 * All blocks (except the last one) must have a length that
	 * is a multiple of the Big Block Size
	 */

	apAnchors[0] = pTextAnchor;
	apAnchors[1] = pFootnoteAnchor;
	apAnchors[2] = pHdrFtrAnchor;
	apAnchors[3] = pMacroAnchor;
	apAnchors[4] = pAnnotationAnchor;
	apAnchors[5] = pEndnoteAnchor;
	apAnchors[6] = pTextBoxAnchor;
	apAnchors[7] = pHdrTextBoxAnchor;

	for (tIndex = 0; tIndex < elementsof(apAnchors); tIndex++) {
		for (pCurr = apAnchors[tIndex];
		     pCurr != NULL;
		     pCurr = pCurr->pNext) {
			if (pCurr->pNext != NULL &&
			    pCurr->tInfo.ulLength % BIG_BLOCK_SIZE != 0) {
				DBG_DEC(tIndex);
				DBG_HEX(pCurr->tInfo.ulFileOffset);
				DBG_HEX(pCurr->tInfo.ulCharPos);
				DBG_DEC(pCurr->tInfo.ulLength);
				pCurr->tInfo.ulLength /= BIG_BLOCK_SIZE;
				pCurr->tInfo.ulLength++;
				pCurr->tInfo.ulLength *= BIG_BLOCK_SIZE;
				DBG_DEC(pCurr->tInfo.ulLength);
			}
		}
	}
} /* end of vSplitBlockList */

#if defined(__riscos)
/*
 * ulGetDocumentLength - get the total character length of the printable lists
 *
 * returns: The total number of characters
 */
ULONG
ulGetDocumentLength(void)
{
	long		ulTotal;

	DBG_MSG("ulGetDocumentLength");

	ulTotal = ulComputeListLength(pTextAnchor);
	ulTotal += ulComputeListLength(pFootnoteAnchor);
	ulTotal += ulComputeListLength(pEndnoteAnchor);
	ulTotal += ulComputeListLength(pTextBoxAnchor);
	ulTotal += ulComputeListLength(pHdrTextBoxAnchor);
	DBG_DEC(ulTotal);
	return ulTotal;
} /* end of ulGetDocumentLength */
#endif /* __riscos */

#if 0
/*
 * bExistsHdrFtr - are there headers and/or footers?
 */
BOOL
bExistsHdrFtr(void)
{
	return pHdrFtrAnchor != NULL &&
		pHdrFtrAnchor->tInfo.ulLength != 0;
} /* end of bExistsHdrFtr */
#endif

/*
 * bExistsTextBox - is there a text box?
 */
BOOL
bExistsTextBox(void)
{
	return pTextBoxAnchor != NULL &&
		pTextBoxAnchor->tInfo.ulLength != 0;
} /* end of bExistsTextBox */

/*
 * bExistsHdrTextBox - is there a header text box?
 */
BOOL
bExistsHdrTextBox(void)
{
	return pHdrTextBoxAnchor != NULL &&
		pHdrTextBoxAnchor->tInfo.ulLength != 0;
} /* end of bExistsHdrTextBox */

/*
 * usGetNextByte - get the next byte from the specified block list
 */
static USHORT
usGetNextByte(FILE *pFile, readinfo_type *pInfoCurrent, list_mem_type *pAnchor,
	ULONG *pulFileOffset, ULONG *pulCharPos, USHORT *pusPropMod)
{
	ULONG	ulReadOff;
	size_t	tReadLen;

	fail(pInfoCurrent == NULL);

	if (pInfoCurrent->pBlockCurrent == NULL ||
	    pInfoCurrent->tByteNext >= sizeof(pInfoCurrent->aucBlock) ||
	    pInfoCurrent->ulBlockOffset + pInfoCurrent->tByteNext >=
				pInfoCurrent->pBlockCurrent->tInfo.ulLength) {
		if (pInfoCurrent->pBlockCurrent == NULL) {
			/* First block, first part */
			pInfoCurrent->pBlockCurrent = pAnchor;
			pInfoCurrent->ulBlockOffset = 0;
		} else if (pInfoCurrent->ulBlockOffset +
				sizeof(pInfoCurrent->aucBlock) <
				pInfoCurrent->pBlockCurrent->tInfo.ulLength) {
			/* Same block, next part */
			pInfoCurrent->ulBlockOffset +=
					sizeof(pInfoCurrent->aucBlock);
		} else {
			/* Next block, first part */
			pInfoCurrent->pBlockCurrent =
					pInfoCurrent->pBlockCurrent->pNext;
			pInfoCurrent->ulBlockOffset = 0;
		}
		if (pInfoCurrent->pBlockCurrent == NULL) {
			/* Past the last part of the last block */
			return (USHORT)EOF;
		}
		tReadLen = (size_t)
			(pInfoCurrent->pBlockCurrent->tInfo.ulLength -
			 pInfoCurrent->ulBlockOffset);
		if (tReadLen > sizeof(pInfoCurrent->aucBlock)) {
			tReadLen = sizeof(pInfoCurrent->aucBlock);
		}
		ulReadOff = pInfoCurrent->pBlockCurrent->tInfo.ulFileOffset +
				pInfoCurrent->ulBlockOffset;
		if (!bReadBytes(pInfoCurrent->aucBlock,
						tReadLen, ulReadOff, pFile)) {
			/* Don't read from this list any longer */
			pInfoCurrent->pBlockCurrent = NULL;
			return (USHORT)EOF;
		}
		pInfoCurrent->tByteNext = 0;
	}
	if (pulFileOffset != NULL) {
		*pulFileOffset =
			pInfoCurrent->pBlockCurrent->tInfo.ulFileOffset +
			pInfoCurrent->ulBlockOffset +
			pInfoCurrent->tByteNext;
	}
	if (pulCharPos != NULL) {
		*pulCharPos =
			pInfoCurrent->pBlockCurrent->tInfo.ulCharPos +
			pInfoCurrent->ulBlockOffset +
			pInfoCurrent->tByteNext;
	}
	if (pusPropMod != NULL) {
		*pusPropMod = pInfoCurrent->pBlockCurrent->tInfo.usPropMod;
	}
	return (USHORT)pInfoCurrent->aucBlock[pInfoCurrent->tByteNext++];
} /* end of usGetNextByte */


/*
 * usGetNextChar - get the next character from the specified block list
 */
static USHORT
usGetNextChar(FILE *pFile, list_id_enum eListID,
	ULONG *pulFileOffset, ULONG *pulCharPos, USHORT *pusPropMod)
{
	readinfo_type	*pReadinfo;
	list_mem_type	*pAnchor;
	USHORT	usLSB, usMSB;

	switch (eListID) {
	case text_list:
		pReadinfo = &tOthers;
		pAnchor = pTextAnchor;
		break;
	case footnote_list:
		pReadinfo = &tFootnote;
		pAnchor = pFootnoteAnchor;
		break;
	case hdrftr_list:
		pReadinfo = &tHdrFtr;
		pAnchor = pHdrFtrAnchor;
		break;
	case endnote_list:
		pReadinfo = &tOthers;
		pAnchor = pEndnoteAnchor;
		break;
	case textbox_list:
		pReadinfo = &tOthers;
		pAnchor = pTextBoxAnchor;
		break;
	case hdrtextbox_list:
		pReadinfo = &tOthers;
		pAnchor = pHdrTextBoxAnchor;
		break;
	default:
		DBG_DEC(eListID);
		return (USHORT)EOF;
	}

	usLSB = usGetNextByte(pFile, pReadinfo, pAnchor,
				pulFileOffset, pulCharPos, pusPropMod);
	if (usLSB == (USHORT)EOF) {
		return (USHORT)EOF;
	}
	fail(pReadinfo->pBlockCurrent == NULL);

	if (pReadinfo->pBlockCurrent->tInfo.bUsesUnicode) {
		usMSB = usGetNextByte(pFile,
				pReadinfo, pAnchor, NULL, NULL, NULL);
	} else {
		usMSB = 0x00;
	}
	if (usMSB == (USHORT)EOF) {
		DBG_MSG("usGetNextChar: Unexpected EOF");
		DBG_HEX_C(pulFileOffset != NULL, *pulFileOffset);
		DBG_HEX_C(pulCharPos != NULL, *pulCharPos);
		return (USHORT)EOF;
	}
	return (usMSB << 8) | usLSB;
} /* end of usGetNextChar */

/*
 * usNextChar - get the next character from the given block list
 */
USHORT
usNextChar(FILE *pFile, list_id_enum eListID,
	ULONG *pulFileOffset, ULONG *pulCharPos, USHORT *pusPropMod)
{
	USHORT	usRetVal;

	fail(pFile == NULL);

	usRetVal = usGetNextChar(pFile, eListID,
				pulFileOffset, pulCharPos, pusPropMod);
	if (usRetVal == (USHORT)EOF) {
		if (pulFileOffset != NULL) {
			*pulFileOffset = FC_INVALID;
		}
		if (pulCharPos != NULL) {
			*pulCharPos = CP_INVALID;
		}
		if (pusPropMod != NULL) {
			*pusPropMod = IGNORE_PROPMOD;
		}
	}
	return usRetVal;
} /* end of usNextChar */

/*
 * usToHdrFtrPosition - Go to a character position in header/foorter list
 *
 * Returns the character found on the specified character position
 */
USHORT
usToHdrFtrPosition(FILE *pFile, ULONG ulCharPos)
{
	ULONG	ulCharPosCurr;
	USHORT	usChar;

	tHdrFtr.pBlockCurrent = NULL;	/* To reset the header/footer list */
	do {
		usChar = usNextChar(pFile,
				hdrftr_list, NULL, &ulCharPosCurr, NULL);
	} while (usChar != (USHORT)EOF && ulCharPosCurr != ulCharPos);
	return usChar;
} /* end of usToHdrFtrPosition */

/*
 * usToFootnotePosition - Go to a character position in footnote list
 *
 * Returns the character found on the specified character position
 */
USHORT
usToFootnotePosition(FILE *pFile, ULONG ulCharPos)
{
	ULONG	ulCharPosCurr;
	USHORT	usChar;

	tFootnote.pBlockCurrent = NULL;	/* To reset the footnote list */
	do {
		usChar = usNextChar(pFile,
				footnote_list, NULL, &ulCharPosCurr, NULL);
	} while (usChar != (USHORT)EOF && ulCharPosCurr != ulCharPos);
	return usChar;
} /* end of usToFootnotePosition */

/*
 * Convert a character position to an offset in the file.
 * Logical to physical offset.
 *
 * Returns:	FC_INVALID: in case of error
 *		otherwise: the computed file offset
 */
ULONG
ulCharPos2FileOffsetX(ULONG ulCharPos, list_id_enum *peListID)
{
	static list_id_enum	eListIDs[8] = {
		text_list,	footnote_list,		hdrftr_list,
		macro_list,	annotation_list,	endnote_list,
		textbox_list,	hdrtextbox_list,
	};
	list_mem_type	*apAnchors[8];
	list_mem_type	*pCurr;
	list_id_enum	eListGuess;
	ULONG		ulBestGuess;
	size_t		tIndex;

	fail(peListID == NULL);

	if (ulCharPos == CP_INVALID) {
		*peListID = no_list;
		return FC_INVALID;
	}

	apAnchors[0] = pTextAnchor;
	apAnchors[1] = pFootnoteAnchor;
	apAnchors[2] = pHdrFtrAnchor;
	apAnchors[3] = pMacroAnchor;
	apAnchors[4] = pAnnotationAnchor;
	apAnchors[5] = pEndnoteAnchor;
	apAnchors[6] = pTextBoxAnchor;
	apAnchors[7] = pHdrTextBoxAnchor;

	eListGuess = no_list;	  /* Best guess is no list */
	ulBestGuess = FC_INVALID; /* Best guess is "file offset not found" */

	for (tIndex = 0; tIndex < elementsof(apAnchors); tIndex++) {
		for (pCurr = apAnchors[tIndex];
		     pCurr != NULL;
		     pCurr = pCurr->pNext) {
			if (ulCharPos == pCurr->tInfo.ulCharPos +
			     pCurr->tInfo.ulLength &&
			    pCurr->pNext != NULL) {
				/*
				 * The character position is one beyond this
				 * block, so we guess it's the first byte of
				 * the next block (if there is a next block)
				 */
				eListGuess= eListIDs[tIndex];
				ulBestGuess = pCurr->pNext->tInfo.ulFileOffset;
			}

			if (ulCharPos < pCurr->tInfo.ulCharPos ||
			    ulCharPos >= pCurr->tInfo.ulCharPos +
			     pCurr->tInfo.ulLength) {
				/* Character position is not in this block */
				continue;
			}

			/* The character position is in the current block */
			*peListID = eListIDs[tIndex];
			return pCurr->tInfo.ulFileOffset +
				ulCharPos - pCurr->tInfo.ulCharPos;
		}
	}
	/* Passed beyond the end of the last list */
	NO_DBG_HEX(ulCharPos);
	NO_DBG_HEX(ulBestGuess);
	*peListID = eListGuess;
	return ulBestGuess;
} /* end of ulCharPos2FileOffsetX */

/*
 * Convert a character position to an offset in the file.
 * Logical to physical offset.
 *
 * Returns:	FC_INVALID: in case of error
 *		otherwise: the computed file offset
 */
ULONG
ulCharPos2FileOffset(ULONG ulCharPos)
{
	list_id_enum	eListID;

	return ulCharPos2FileOffsetX(ulCharPos, &eListID);
} /* end of ulCharPos2FileOffset */

/*
 * Convert an offset in the header/footer list to a character position.
 *
 * Returns:	CP_INVALID: in case of error
 *		otherwise: the computed character position
 */
ULONG
ulHdrFtrOffset2CharPos(ULONG ulHdrFtrOffset)
{
	list_mem_type	*pCurr;
	ULONG		ulOffset;

	ulOffset = ulHdrFtrOffset;
	for (pCurr = pHdrFtrAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (ulOffset >= pCurr->tInfo.ulLength) {
			/* The offset is not in this block */
			ulOffset -= pCurr->tInfo.ulLength;
			continue;
		}
		return pCurr->tInfo.ulCharPos + ulOffset;
	}
	return CP_INVALID;
} /* end of ulHdrFtrOffset2CharPos */

/*
 * Get the sequence number beloning to the given file offset
 *
 * Returns the sequence number
 */
ULONG
ulGetSeqNumber(ULONG ulFileOffset)
{
	list_mem_type	*pCurr;
	ULONG		ulSeq;

	if (ulFileOffset == FC_INVALID) {
		return FC_INVALID;
	}

	ulSeq = 0;
	for (pCurr = pTextAnchor; pCurr != NULL; pCurr = pCurr->pNext) {
		if (ulFileOffset >= pCurr->tInfo.ulFileOffset &&
		    ulFileOffset < pCurr->tInfo.ulFileOffset +
		     pCurr->tInfo.ulLength) {
			/* The file offset is within the current textblock */
			return ulSeq + ulFileOffset - pCurr->tInfo.ulFileOffset;
		}
		ulSeq += pCurr->tInfo.ulLength;
	}
	return FC_INVALID;
} /* end of ulGetSeqNumber */
