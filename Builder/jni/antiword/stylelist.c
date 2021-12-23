/*
 * stylelist.c
 * Copyright (C) 1998-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Build, read and destroy a list of Word style information
 */

#include <stdlib.h>
#include <stddef.h>
#include <ctype.h>
#include "antiword.h"


/*
 * Private structure to hide the way the information
 * is stored from the rest of the program
 */
typedef struct style_mem_tag {
	style_block_type	tInfo;
	ULONG			ulSequenceNumber;
	struct style_mem_tag	*pNext;
} style_mem_type;

/* Variables needed to write the Style Information List */
static style_mem_type	*pAnchor = NULL;
static style_mem_type	*pStyleLast = NULL;
/* The type of conversion */
static conversion_type	eConversionType = conversion_unknown;
/* The character set encoding */
static encoding_type	eEncoding = encoding_neutral;
/* Values for efficiency reasons */
static const style_mem_type	*pMidPtr = NULL;
static BOOL		bMoveMidPtr = FALSE;
static BOOL		bInSequence = TRUE;


/*
 * vDestroyStyleInfoList - destroy the Style Information List
 */
void
vDestroyStyleInfoList(void)
{
	style_mem_type	*pCurr, *pNext;

	DBG_MSG("vDestroyStyleInfoList");

	/* Free the Style Information List */
	pCurr = pAnchor;
	while (pCurr != NULL) {
		pNext = pCurr->pNext;
		pCurr = xfree(pCurr);
		pCurr = pNext;
	}
	pAnchor = NULL;
	/* Reset all control variables */
	pStyleLast = NULL;
	pMidPtr = NULL;
	bMoveMidPtr = FALSE;
	bInSequence = TRUE;
} /* end of vDestroyStyleInfoList */

/*
 * vConvertListCharacter - convert the list character
 */
static void
vConvertListCharacter(UCHAR ucNFC, USHORT usListChar, char *szListChar)
{
	options_type	tOptions;
	size_t	tLen;

	fail(szListChar == NULL);
	fail(szListChar[0] != '\0');

	if (usListChar < 0x80 && isprint((int)usListChar)) {
		DBG_CHR_C(isalnum((int)usListChar), usListChar);
		szListChar[0] = (char)usListChar;
		szListChar[1] = '\0';
		return;
	}

	if (ucNFC != LIST_SPECIAL &&
	    ucNFC != LIST_SPECIAL2 &&
	    ucNFC != LIST_BULLETS) {
		szListChar[0] = '.';
		szListChar[1] = '\0';
		return;
	}

	if (eConversionType == conversion_unknown ||
	    eEncoding == encoding_neutral) {
		vGetOptions(&tOptions);
		eConversionType = tOptions.eConversionType;
		eEncoding = tOptions.eEncoding;
	}

	switch (usListChar) {
	case 0x0000: case 0x00b7: case 0x00fe: case  0xf021: case 0xf043:
	case 0xf06c: case 0xf093: case 0xf0b7:
		usListChar = 0x2022;	/* BULLET */
		break;
	case 0x0096: case 0xf02d:
		usListChar = 0x2013;	/* EN DASH */
		break;
	case 0x00a8:
		usListChar = 0x2666;	/* BLACK DIAMOND SUIT */
		break;
	case 0x00de:
		usListChar = 0x21d2;	/* RIGHTWARDS DOUBLE ARROW */
		break;
	case 0x00e0: case 0xf074:
		usListChar = 0x25ca;	/* LOZENGE */
		break;
	case 0x00e1:
		usListChar = 0x2329;	/* LEFT ANGLE BRACKET */
		break;
	case 0xf020:
		usListChar = 0x0020;	/* SPACE */
		break;
	case 0xf041:
		usListChar = 0x270c;	/* VICTORY HAND */
		break;
	case 0xf066:
		usListChar = 0x03d5;	/* GREEK PHI SYMBOL */
		break;
	case 0xf06e:
		usListChar = 0x25a0;	/* BLACK SQUARE */
		break;
	case 0xf06f: case 0xf070: case 0xf0a8:
		usListChar = 0x25a1;	/* WHITE SQUARE */
		break;
	case 0xf071:
		usListChar = 0x2751;	/* LOWER RIGHT SHADOWED WHITE SQUARE */
		break;
	case 0xf075: case 0xf077:
		usListChar = 0x25c6;	/* BLACK DIAMOND */
		break;
	case 0xf076:
		usListChar = 0x2756;	/* BLACK DIAMOND MINUS WHITE X */
		break;
	case 0xf0a7:
		usListChar = 0x25aa;	/* BLACK SMALL SQUARE */
		break;
	case 0xf0d8:
		usListChar = 0x27a2;	/* RIGHTWARDS ARROWHEAD */
		break;
	case 0xf0e5:
		usListChar = 0x2199;	/* SOUTH WEST ARROW */
		break;
	case 0xf0f0:
		usListChar = 0x21e8;	/* RIGHTWARDS WHITE ARROW */
		break;
	case 0xf0fc:
		usListChar = 0x2713;	/* CHECK MARK */
		break;
	default:
		if ((usListChar >= 0xe000 && usListChar < 0xf900) ||
		    (usListChar < 0x80 && !isprint((int)usListChar))) {
			/*
			 * All remaining private area characters and all
			 * remaining non-printable ASCII characters to their
			 * default bullet character
			 */
			DBG_HEX(usListChar);
			DBG_FIXME();
			if (ucNFC == LIST_SPECIAL || ucNFC == LIST_SPECIAL2) {
				usListChar = 0x2190;	/* LEFTWARDS ARROW */
			} else {
				usListChar = 0x2022;	/* BULLET */
			}
		}
		break;
	}

	if (eEncoding == encoding_utf_8) {
		tLen = tUcs2Utf8(usListChar, szListChar, 4);
		szListChar[tLen] = '\0';
	} else {
		switch (usListChar) {
		case 0x03d5: case 0x25a1: case 0x25c6: case 0x25ca:
		case 0x2751:
			szListChar[0] = 'o';
			break;
		case 0x2013: case 0x2500:
			szListChar[0] = '-';
			break;
		case 0x2190: case 0x2199: case 0x2329:
			szListChar[0] = '<';
			break;
		case 0x21d2:
			szListChar[0] = '=';
			break;
		case 0x21e8: case 0x27a2:
			szListChar[0] = '>';
			break;
		case 0x25a0: case 0x25aa:
			szListChar[0] = '.';
			break;
		case 0x2666:
			szListChar[0] = OUR_DIAMOND;
			break;
		case 0x270c:
			szListChar[0] = 'x';
			break;
		case 0x2713:
			szListChar[0] = 'V';
			break;
		case 0x2756:
			szListChar[0] = '*';
			break;
		case 0x2022:
		default:
			vGetBulletValue(eConversionType, eEncoding,
					szListChar, 2);
			break;
		}
		tLen = 1;
	}
	szListChar[tLen] = '\0';
} /* end of vConvertListCharacter */

/*
 * eGetNumType - get the level type from the given level number
 *
 * Returns the level type
 */
level_type_enum
eGetNumType(UCHAR ucNumLevel)
{
	switch (ucNumLevel) {
	case  1: case  2: case  3: case  4: case  5:
	case  6: case  7: case  8: case  9:
		return level_type_outline;
	case 10:
		return level_type_numbering;
	case 11:
		return level_type_sequence;
	case 12:
		return level_type_pause;
	default:
		return level_type_none;
	}
} /* end of eGetNumType */

/*
 * vCorrectStyleValues - correct style values that Antiword can't use
 */
void
vCorrectStyleValues(style_block_type *pStyleBlock)
{
	if (pStyleBlock->usBeforeIndent > 0x7fff) {
		pStyleBlock->usBeforeIndent = 0;
	} else if (pStyleBlock->usBeforeIndent > 2160) {
		/* 2160 twips = 1.5 inches or 38.1 mm */
		DBG_DEC(pStyleBlock->usBeforeIndent);
		pStyleBlock->usBeforeIndent = 2160;
	}
	if (pStyleBlock->usIstd >= 1 &&
	    pStyleBlock->usIstd <= 9 &&
	    pStyleBlock->usBeforeIndent < HEADING_GAP) {
		NO_DBG_DEC(pStyleBlock->usBeforeIndent);
		pStyleBlock->usBeforeIndent = HEADING_GAP;
	}

	if (pStyleBlock->usAfterIndent > 0x7fff) {
		pStyleBlock->usAfterIndent = 0;
	} else if (pStyleBlock->usAfterIndent > 2160) {
		/* 2160 twips = 1.5 inches or 38.1 mm */
		DBG_DEC(pStyleBlock->usAfterIndent);
		pStyleBlock->usAfterIndent = 2160;
	}
	if (pStyleBlock->usIstd >= 1 &&
	    pStyleBlock->usIstd <= 9 &&
	    pStyleBlock->usAfterIndent < HEADING_GAP) {
		NO_DBG_DEC(pStyleBlock->usAfterIndent);
		pStyleBlock->usAfterIndent = HEADING_GAP;
	}

	if (pStyleBlock->sLeftIndent < 0) {
		pStyleBlock->sLeftIndent = 0;
	}
	if (pStyleBlock->sRightIndent > 0) {
		pStyleBlock->sRightIndent = 0;
	}
	vConvertListCharacter(pStyleBlock->ucNFC,
			pStyleBlock->usListChar,
			pStyleBlock->szListChar);
} /* end of vCorrectStyleValues */

/*
 * vAdd2StyleInfoList - Add an element to the Style Information List
 */
void
vAdd2StyleInfoList(const style_block_type *pStyleBlock)
{
	style_mem_type	*pListMember;

	fail(pStyleBlock == NULL);

	NO_DBG_MSG("bAdd2StyleInfoList");

	if (pStyleBlock->ulFileOffset == FC_INVALID) {
		NO_DBG_DEC(pStyleBlock->usIstd);
		return;
	}

	NO_DBG_HEX(pStyleBlock->ulFileOffset);
	NO_DBG_DEC_C(pStyleBlock->sLeftIndent != 0,
					pStyleBlock->sLeftIndent);
	NO_DBG_DEC_C(pStyleBlock->sRightIndent != 0,
					pStyleBlock->sRightIndent);
	NO_DBG_DEC_C(pStyleBlock->bNumPause, pStyleBlock->bNumPause);
	NO_DBG_DEC_C(pStyleBlock->usIstd != 0, pStyleBlock->usIstd);
	NO_DBG_DEC_C(pStyleBlock->usStartAt != 1, pStyleBlock->usStartAt);
	NO_DBG_DEC_C(pStyleBlock->usAfterIndent != 0,
					pStyleBlock->usAfterIndent);
	NO_DBG_DEC_C(pStyleBlock->ucAlignment != 0, pStyleBlock->ucAlignment);
	NO_DBG_DEC(pStyleBlock->ucNFC);
	NO_DBG_HEX(pStyleBlock->usListChar);

	if (pStyleLast != NULL &&
	    pStyleLast->tInfo.ulFileOffset == pStyleBlock->ulFileOffset) {
		/*
		 * If two consecutive styles share the same
		 * offset, remember only the last style
		 */
		fail(pStyleLast->pNext != NULL);
		pStyleLast->tInfo = *pStyleBlock;
		/* Correct the values where needed */
		vCorrectStyleValues(&pStyleLast->tInfo);
		return;
	}

	/* Create list member */
	pListMember = xmalloc(sizeof(style_mem_type));
	/* Fill the list member */
	pListMember->tInfo = *pStyleBlock;
	pListMember->pNext = NULL;
	/* Add the sequence number */
	pListMember->ulSequenceNumber =
			ulGetSeqNumber(pListMember->tInfo.ulFileOffset);
	/* Correct the values where needed */
	vCorrectStyleValues(&pListMember->tInfo);
	/* Add the new member to the list */
	if (pAnchor == NULL) {
		pAnchor = pListMember;
		/* For efficiency */
		pMidPtr = pAnchor;
		bMoveMidPtr = FALSE;
		bInSequence = TRUE;
	} else {
		fail(pStyleLast == NULL);
		pStyleLast->pNext = pListMember;
		/* For efficiency */
		if (bMoveMidPtr) {
			pMidPtr = pMidPtr->pNext;
			bMoveMidPtr = FALSE;
		} else {
			bMoveMidPtr = TRUE;
		}
		if (bInSequence) {
			bInSequence = pListMember->ulSequenceNumber >
					pStyleLast->ulSequenceNumber;
		}
	}
	pStyleLast = pListMember;
} /* end of vAdd2StyleInfoList */

/*
 * Get the record that follows the given recored in the Style Information List
 */
const style_block_type *
pGetNextStyleInfoListItem(const style_block_type *pCurr)
{
	const style_mem_type	*pRecord;
	size_t	tOffset;

	if (pCurr == NULL) {
		if (pAnchor == NULL) {
			/* There are no records */
			return NULL;
		}
		/* The first record is the only one without a predecessor */
		return &pAnchor->tInfo;
	}
	tOffset = offsetof(style_mem_type, tInfo);
	/* Many casts to prevent alignment warnings */
	pRecord = (style_mem_type *)(void *)((char *)pCurr - tOffset);
	fail(pCurr != &pRecord->tInfo);
	if (pRecord->pNext == NULL) {
		/* The last record has no successor */
		return NULL;
	}
	return &pRecord->pNext->tInfo;
} /* end of pGetNextStyleInfoListItem */

/*
 * Get the next text style
 */
const style_block_type *
pGetNextTextStyle(const style_block_type *pCurr)
{
	const style_block_type	*pRecord;

	pRecord = pCurr;
	do {
		pRecord = pGetNextStyleInfoListItem(pRecord);
	} while (pRecord != NULL &&
		 (pRecord->eListID == hdrftr_list ||
		  pRecord->eListID == macro_list ||
		  pRecord->eListID == annotation_list));
	return pRecord;
} /* end of pGetNextTextStyle */

/*
 * usGetIstd - get the istd that belongs to the given file offset
 */
USHORT
usGetIstd(ULONG ulFileOffset)
{
	const style_mem_type	*pCurr, *pBest, *pStart;
	ULONG	ulSeq, ulBest;

	ulSeq = ulGetSeqNumber(ulFileOffset);
	if (ulSeq == FC_INVALID) {
		return ISTD_NORMAL;
	}
	NO_DBG_HEX(ulFileOffset);
	NO_DBG_DEC(ulSeq);

	if (bInSequence &&
	    pMidPtr != NULL &&
	    ulSeq > pMidPtr->ulSequenceNumber) {
		/* The istd is in the second half of the chained list */
		pStart = pMidPtr;
	} else {
		pStart = pAnchor;
	}

	pBest = NULL;
	ulBest = 0;
	for (pCurr = pStart; pCurr != NULL; pCurr = pCurr->pNext) {
		if (pCurr->ulSequenceNumber != FC_INVALID &&
		    (pBest == NULL || pCurr->ulSequenceNumber > ulBest) &&
		    pCurr->ulSequenceNumber <= ulSeq) {
			pBest = pCurr;
			ulBest = pCurr->ulSequenceNumber;
		}
		if (bInSequence && pCurr->ulSequenceNumber > ulSeq) {
			break;
		}
	}
	NO_DBG_DEC(ulBest);

	if (pBest == NULL) {
		return ISTD_NORMAL;
	}

	NO_DBG_DEC(pBest->tInfo.usIstd);
	return pBest->tInfo.usIstd;
} /* end of usGetIstd */

/*
 * bStyleImpliesList - does style info implies being part of a list
 *
 * Decide whether the style information implies that the given paragraph is
 * part of a list
 *
 * Returns TRUE when the paragraph is part of a list, otherwise FALSE
 */
BOOL
bStyleImpliesList(const style_block_type *pStyle, int iWordVersion)
{
	fail(pStyle == NULL);
	fail(iWordVersion < 0);

	if (pStyle->usIstd >= 1 && pStyle->usIstd <= 9) {
		/* These are heading levels */
		return FALSE;
	}
	if (iWordVersion < 8) {
		/* Check for old style lists */
		return pStyle->ucNumLevel != 0;
	}
	/* Check for new style lists */
	return pStyle->usListIndex != 0;
} /* end of bStyleImpliesList */
