/*
 * wordwin.c
 * Copyright (C) 2002-2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Deal with the WIN internals of a MS Word file
 */

#include "antiword.h"


/*
 * bGetDocumentText - make a list of the text blocks of a Word document
 *
 * Return TRUE when succesful, otherwise FALSE
 */
static BOOL
bGetDocumentText(FILE *pFile, const UCHAR *aucHeader)
{
	text_block_type	tTextBlock;
	ULONG	ulBeginOfText;
	ULONG	ulTextLen, ulFootnoteLen;
	ULONG	ulHdrFtrLen, ulMacroLen, ulAnnotationLen;
	UINT	uiQuickSaves;
	USHORT	usDocStatus;
	BOOL	bTemplate, bFastSaved, bEncrypted, bSuccess;

	fail(pFile == NULL);
	fail(aucHeader == NULL);

	DBG_MSG("bGetDocumentText");

	/* Get the status flags from the header */
	usDocStatus = usGetWord(0x0a, aucHeader);
	DBG_HEX(usDocStatus);
	bTemplate = (usDocStatus & BIT(0)) != 0;
	DBG_MSG_C(bTemplate, "This document is a Template");
	bFastSaved = (usDocStatus & BIT(2)) != 0;
	uiQuickSaves = (UINT)(usDocStatus & 0x00f0) >> 4;
	DBG_MSG_C(bFastSaved, "This document is Fast Saved");
	DBG_DEC_C(bFastSaved, uiQuickSaves);
	if (bFastSaved) {
		werr(0, "Word2: fast saved documents are not supported yet");
		return FALSE;
	}
	bEncrypted = (usDocStatus & BIT(8)) != 0;
	if (bEncrypted) {
		werr(0, "Encrypted documents are not supported");
		return FALSE;
	}

	/* Get length information */
	ulBeginOfText = ulGetLong(0x18, aucHeader);
	DBG_HEX(ulBeginOfText);
	ulTextLen = ulGetLong(0x34, aucHeader);
	ulFootnoteLen = ulGetLong(0x38, aucHeader);
	ulHdrFtrLen = ulGetLong(0x3c, aucHeader);
	ulMacroLen = ulGetLong(0x40, aucHeader);
	ulAnnotationLen = ulGetLong(0x44, aucHeader);
	DBG_DEC(ulTextLen);
	DBG_DEC(ulFootnoteLen);
	DBG_DEC(ulHdrFtrLen);
	DBG_DEC(ulMacroLen);
	DBG_DEC(ulAnnotationLen);
	if (bFastSaved) {
		bSuccess = FALSE;
	} else {
		tTextBlock.ulFileOffset = ulBeginOfText;
		tTextBlock.ulCharPos = ulBeginOfText;
		tTextBlock.ulLength = ulTextLen +
				ulFootnoteLen +
				ulHdrFtrLen + ulMacroLen + ulAnnotationLen;
		tTextBlock.bUsesUnicode = FALSE;
		tTextBlock.usPropMod = IGNORE_PROPMOD;
		bSuccess = bAdd2TextBlockList(&tTextBlock);
		DBG_HEX_C(!bSuccess, tTextBlock.ulFileOffset);
		DBG_HEX_C(!bSuccess, tTextBlock.ulCharPos);
		DBG_DEC_C(!bSuccess, tTextBlock.ulLength);
		DBG_DEC_C(!bSuccess, tTextBlock.bUsesUnicode);
		DBG_DEC_C(!bSuccess, tTextBlock.usPropMod);
	}

	if (bSuccess) {
		vSplitBlockList(pFile,
				ulTextLen,
				ulFootnoteLen,
				ulHdrFtrLen,
				ulMacroLen,
				ulAnnotationLen,
				0,
				0,
				0,
				FALSE);
	} else {
		vDestroyTextBlockList();
		werr(0, "I can't find the text of this document");
	}
	return bSuccess;
} /* end of bGetDocumentText */

/*
 * vGetDocumentData - make a list of the data blocks of a Word document
 */
static void
vGetDocumentData(FILE *pFile, const UCHAR *aucHeader)
{
	data_block_type	tDataBlock;
	options_type	tOptions;
	ULONG	ulEndOfText, ulBeginCharInfo;
	BOOL	bFastSaved, bHasImages, bSuccess;
	USHORT	usDocStatus;

	/* Get the options */
	vGetOptions(&tOptions);

	/* Get the status flags from the header */
	usDocStatus = usGetWord(0x0a, aucHeader);
	DBG_HEX(usDocStatus);
	bFastSaved = (usDocStatus & BIT(2)) != 0;
	bHasImages = (usDocStatus & BIT(3)) != 0;

	if (!bHasImages ||
	    tOptions.eConversionType == conversion_text ||
	    tOptions.eConversionType == conversion_fmt_text ||
	    tOptions.eConversionType == conversion_xml ||
	    tOptions.eImageLevel == level_no_images) {
		/*
		 * No images in the document or text-only output or
		 * no images wanted, so no data blocks will be needed
		 */
		vDestroyDataBlockList();
		return;
	}

	if (bFastSaved) {
		bSuccess = FALSE;
	} else {
		/* This datablock is too big, but it contains all images */
		ulEndOfText = ulGetLong(0x1c, aucHeader);
		DBG_HEX(ulEndOfText);
		ulBeginCharInfo = ulGetLong(0xa0, aucHeader);
		DBG_HEX(ulBeginCharInfo);
		if (ulBeginCharInfo > ulEndOfText) {
			tDataBlock.ulFileOffset = ulEndOfText;
			tDataBlock.ulDataPos = ulEndOfText;
			tDataBlock.ulLength = ulBeginCharInfo - ulEndOfText;
			bSuccess = bAdd2DataBlockList(&tDataBlock);
			DBG_HEX_C(!bSuccess, tDataBlock.ulFileOffset);
			DBG_HEX_C(!bSuccess, tDataBlock.ulDataPos);
			DBG_DEC_C(!bSuccess, tDataBlock.ulLength);
		} else {
			bSuccess = ulBeginCharInfo == ulEndOfText;
		}
	}

	if (!bSuccess) {
		vDestroyDataBlockList();
		werr(0, "I can't find the data of this document");
	}
} /* end of vGetDocumentData */

/*
 * iInitDocumentWIN - initialize an WIN document
 *
 * Returns the version of Word that made the document or -1
 */
int
iInitDocumentWIN(FILE *pFile, long lFilesize)
{
	int	iWordVersion;
	BOOL	bSuccess;
	USHORT	usIdent;
	UCHAR	aucHeader[384];

	fail(pFile == NULL);

	if (lFilesize < 384) {
		return -1;
	}

	/* Read the headerblock */
	if (!bReadBytes(aucHeader, 384, 0x00, pFile)) {
		return -1;
	}
	/* Get the "magic number" from the header */
	usIdent = usGetWord(0x00, aucHeader);
	DBG_HEX(usIdent);
	fail(usIdent != 0xa59b &&	/* WinWord 1.x */
		usIdent != 0xa5db);	/* WinWord 2.0 */
	iWordVersion = iGetVersionNumber(aucHeader);
	if (iWordVersion != 1 && iWordVersion != 2) {
		werr(0, "This file is not from ''Win Word 1 or 2'.");
		return -1;
	}
	bSuccess = bGetDocumentText(pFile, aucHeader);
	if (bSuccess) {
		vGetDocumentData(pFile, aucHeader);
		vGetPropertyInfo(pFile, NULL,
				NULL, 0, NULL, 0,
				aucHeader, iWordVersion);
		vSetDefaultTabWidth(pFile, NULL,
				NULL, 0, NULL, 0,
				aucHeader, iWordVersion);
		vGetNotesInfo(pFile, NULL,
				NULL, 0, NULL, 0,
				aucHeader, iWordVersion);
	}
	return bSuccess ? iWordVersion : -1;
} /* end of iInitDocumentWIN */
