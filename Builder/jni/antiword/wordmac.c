/*
 * wordmac.c
 * Copyright (C) 2002-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Deal with the MAC internals of a MS Word file
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
	ULONG	ulBeginOfText, ulEndOfText;
	ULONG	ulTextLen;
	UCHAR	ucDocStatus;
	BOOL    bFastSaved;

	fail(pFile == NULL);
	fail(aucHeader == NULL);

	DBG_MSG("bGetDocumentText");

	NO_DBG_PRINT_BLOCK(aucHeader, 0x20);

	/* Get the status flags from the header */
	ucDocStatus = ucGetByte(0x0a, aucHeader);
	DBG_HEX(ucDocStatus);
	bFastSaved = (ucDocStatus & BIT(5)) != 0;
	DBG_MSG_C(bFastSaved, "This document is Fast Saved");
	if (bFastSaved) {
		werr(0, "MacWord: fast saved documents are not supported yet");
		return FALSE;
	}

	/* Get length information */
	ulBeginOfText = ulGetLongBE(0x14, aucHeader);
	DBG_HEX(ulBeginOfText);
	ulEndOfText = ulGetLongBE(0x18, aucHeader);
	DBG_HEX(ulEndOfText);
	ulTextLen = ulEndOfText - ulBeginOfText;
	DBG_DEC(ulTextLen);
	tTextBlock.ulFileOffset = ulBeginOfText;
	tTextBlock.ulCharPos = ulBeginOfText;
	tTextBlock.ulLength = ulTextLen;
	tTextBlock.bUsesUnicode = FALSE;
	tTextBlock.usPropMod = IGNORE_PROPMOD;
	if (!bAdd2TextBlockList(&tTextBlock)) {
		DBG_HEX(tTextBlock.ulFileOffset);
		DBG_HEX(tTextBlock.ulCharPos);
		DBG_DEC(tTextBlock.ulLength);
		DBG_DEC(tTextBlock.bUsesUnicode);
		DBG_DEC(tTextBlock.usPropMod);
		return FALSE;
	}
	return TRUE;
} /* end of bGetDocumentText */

/*
 * iInitDocumentMAC - initialize an MAC document
 *
 * Returns the version of Word that made the document or -1
 */
int
iInitDocumentMAC(FILE *pFile, long lFilesize)
{
	int	iWordVersion;
	BOOL	bSuccess;
	USHORT	usIdent;
	UCHAR	aucHeader[256];

	fail(pFile == NULL);

	if (lFilesize < 256) {
		return -1;
	}

	/* Read the headerblock */
	if (!bReadBytes(aucHeader, 256, 0x00, pFile)) {
		return -1;
	}
	/* Get the "magic number" from the header */
	usIdent = usGetWord(0x00, aucHeader);
	DBG_HEX(usIdent);
	fail(usIdent != 0x37fe);	/* MacWord 4 and 5 */
	iWordVersion = iGetVersionNumber(aucHeader);
	if (iWordVersion != 4 && iWordVersion != 5) {
		werr(0, "This file is not from ''Mac Word 4 or 5'.");
		return -1;
	}
	bSuccess = bGetDocumentText(pFile, aucHeader);
	if (bSuccess) {
		vGetPropertyInfo(pFile, NULL,
				NULL, 0, NULL, 0,
				aucHeader, iWordVersion);
		vSetDefaultTabWidth(pFile, NULL,
				NULL, 0, NULL, 0,
				aucHeader, iWordVersion);
	}
	return bSuccess ? iWordVersion : -1;
} /* end of iInitDocumentMAC */
