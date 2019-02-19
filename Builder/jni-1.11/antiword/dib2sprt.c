/*
 * dib2sprt.c
 * Copyright (C) 2000-2003 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to translate dib pictures into sprites
 */

#include <stdio.h>
#include <string.h>
#include "DeskLib:Error.h"
#include "DeskLib:Sprite.h"
#include "antiword.h"

#if 0 /* defined(DEBUG) */
static int iPicCounter = 0;
#endif /* DEBUG */


/*
 * iGetByteWidth - compute the number of bytes needed for a row of pixels
 */
static int
iGetByteWidth(const imagedata_type *pImg)
{
	switch (pImg->uiBitsPerComponent) {
	case  1:
		return (pImg->iWidth + 31) / 32 * sizeof(int);
	case  4:
		return (pImg->iWidth + 7) / 8 * sizeof(int);
	case  8:
	case 24:
		return (pImg->iWidth + 3) / 4 * sizeof(int);
	default:
		DBG_DEC(pImg->uiBitsPerComponent);
		return 0;
	}
} /* end of iGetByteWidth */

/*
 * pCreateBlankSprite - Create a blank sprite.
 *
 * Create a blank sprite and add a palette if needed
 *
 * returns a pointer to the sprite when successful, otherwise NULL
 */
static sprite_areainfo *
pCreateBlankSprite(const imagedata_type *pImg, size_t *pSize)
{
	sprite_areainfo	*pArea;
	UCHAR	*pucTmp;
	size_t	tSize;
	screen_modeval	uMode;
	int	iIndex, iPaletteEntries;

	TRACE_MSG("pCreateBlankSprite");

	fail(pImg == NULL);
	fail(pSize == NULL);

	switch (pImg->uiBitsPerComponent) {
	case  1:
		uMode.screen_mode = 18;
		iPaletteEntries = 2;
		break;
	case  4:
		uMode.screen_mode = 20;
		iPaletteEntries = 16;
		break;
	case  8:
	case 24:
		uMode.screen_mode = 21;
		iPaletteEntries = 0;
		break;
	default:
		DBG_DEC(pImg->uiBitsPerComponent);
		return NULL;
	}
	fail(iPaletteEntries < 0 || iPaletteEntries > 16);

	/* Get memory for the sprite */
	tSize = sizeof(sprite_areainfo) +
		Sprite_MemorySize(pImg->iWidth, pImg->iHeight, uMode,
		iPaletteEntries > 0 ? sprite_HASPAL : sprite_HASNOMASKPAL);
	DBG_DEC(tSize);
	pArea = xmalloc(tSize);

	/* Initialise sprite area */
	pArea->areasize = tSize;
	pArea->numsprites = 0;
	pArea->firstoffset = sizeof(sprite_areainfo);
	pArea->freeoffset = sizeof(sprite_areainfo);

	/* Create a blank sprite */
	Error_CheckFatal(Sprite_Create(pArea, "wordimage",
		iPaletteEntries > 0 ? 1 : 0,
		pImg->iWidth, pImg->iHeight, uMode));

	/* Add the palette */
	pucTmp = (UCHAR *)pArea + pArea->firstoffset + sizeof(sprite_header);
	for (iIndex = 0; iIndex < iPaletteEntries; iIndex++) {
		/* First color */
		*pucTmp++ = 0;
		*pucTmp++ = pImg->aucPalette[iIndex][0];
		*pucTmp++ = pImg->aucPalette[iIndex][1];
		*pucTmp++ = pImg->aucPalette[iIndex][2];
		/* Second color */
		*pucTmp++ = 0;
		*pucTmp++ = pImg->aucPalette[iIndex][0];
		*pucTmp++ = pImg->aucPalette[iIndex][1];
		*pucTmp++ = pImg->aucPalette[iIndex][2];
	}

	*pSize = tSize;
	return pArea;
} /* end of pCreateBlankSprite */

/*
 * iReduceColor - reduce from 24 bit to 8 bit color
 *
 * Reduce 24 bit true colors to RISC OS default 256 color palette
 *
 * returns the resulting color
 */
static int
iReduceColor(int iRed, int iGreen, int iBlue)
{
	int	iResult;

	iResult = (iBlue & 0x80) ? 0x80 : 0;
	iResult |= (iGreen & 0x80) ? 0x40 : 0;
	iResult |= (iGreen & 0x40) ? 0x20 : 0;
	iResult |= (iRed & 0x80) ? 0x10 : 0;
	iResult |= (iBlue & 0x40) ? 0x08 : 0;
	iResult |= (iRed & 0x40) ? 0x04 : 0;
	iResult |= ((iRed | iGreen | iBlue) & 0x20) ? 0x02 : 0;
	iResult |= ((iRed | iGreen | iBlue) & 0x10) ? 0x01 : 0;
	return iResult;
} /* end of iReduceColor */

/*
 * vDecode1bpp - decode an uncompressed 1 bit per pixel image
 */
static void
vDecode1bpp(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iByteWidth, iOffset, iTmp, iEighthWidth, iPadding;
	UCHAR	ucTmp;

	DBG_MSG("vDecode1bpp");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 2);

	iByteWidth = iGetByteWidth(pImg);

	iEighthWidth = (pImg->iWidth + 7) / 8;
	iPadding = ROUND4(iEighthWidth) - iEighthWidth;

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		for (iX = 0; iX < iEighthWidth; iX++) {
			iTmp = iNextByte(pFile);
			if (iTmp == EOF) {
				return;
			}
			/* Reverse the bit order */
			ucTmp  = (iTmp & BIT(0)) ? (UCHAR)BIT(7) : 0;
			ucTmp |= (iTmp & BIT(1)) ? (UCHAR)BIT(6) : 0;
			ucTmp |= (iTmp & BIT(2)) ? (UCHAR)BIT(5) : 0;
			ucTmp |= (iTmp & BIT(3)) ? (UCHAR)BIT(4) : 0;
			ucTmp |= (iTmp & BIT(4)) ? (UCHAR)BIT(3) : 0;
			ucTmp |= (iTmp & BIT(5)) ? (UCHAR)BIT(2) : 0;
			ucTmp |= (iTmp & BIT(6)) ? (UCHAR)BIT(1) : 0;
			ucTmp |= (iTmp & BIT(7)) ? (UCHAR)BIT(0) : 0;
			iOffset = iY * iByteWidth + iX;
			*(pucData + iOffset) = ucTmp;
		}
		(void)tSkipBytes(pFile, iPadding);
	}
} /* end of vDecode1bpp */

/*
 * vDecode4bpp - decode an uncompressed 4 bits per pixel image
 */
static void
vDecode4bpp(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iByteWidth, iOffset, iTmp, iHalfWidth, iPadding;
	UCHAR	ucTmp;

	DBG_MSG("vDecode4bpp");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 16);

	iByteWidth = iGetByteWidth(pImg);

	iHalfWidth = (pImg->iWidth + 1) / 2;
	iPadding = ROUND4(iHalfWidth) - iHalfWidth;

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		for (iX = 0; iX < iHalfWidth; iX++) {
			iTmp = iNextByte(pFile);
			if (iTmp == EOF) {
				return;
			}
			/* Reverse the nibble order */
			ucTmp = (iTmp & 0xf0) >> 4;
			ucTmp |= (iTmp & 0x0f) << 4;
			iOffset = iY * iByteWidth + iX;
			*(pucData + iOffset) = ucTmp;
		}
		(void)tSkipBytes(pFile, iPadding);
	}
} /* end of vDecode4bpp */

/*
 * vDecode8bpp - decode an uncompressed 8 bits per pixel image
 */
static void
vDecode8bpp(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iByteWidth, iOffset, iIndex, iPadding;

	DBG_MSG("vDecode8bpp");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 256);

	iByteWidth = iGetByteWidth(pImg);

	iPadding = ROUND4(pImg->iWidth) - pImg->iWidth;

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		for (iX = 0; iX < pImg->iWidth; iX++) {
			iIndex = iNextByte(pFile);
			if (iIndex == EOF) {
				return;
			}
			iOffset = iY * iByteWidth + iX;
			*(pucData + iOffset) = iReduceColor(
				pImg->aucPalette[iIndex][0],
				pImg->aucPalette[iIndex][1],
				pImg->aucPalette[iIndex][2]);
		}
		(void)tSkipBytes(pFile, iPadding);
	}
} /* end of vDecode8bpp */

/*
 * vDecode24bpp - decode an uncompressed 24 bits per pixel image
 */
static void
vDecode24bpp(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iTripleWidth, iByteWidth, iOffset, iPadding;
	int	iRed, iGreen, iBlue;

	DBG_MSG("vDecode24bpp");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);

	iByteWidth = iGetByteWidth(pImg);

	iTripleWidth = pImg->iWidth * 3;
	iPadding = ROUND4(iTripleWidth) - iTripleWidth;

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		for (iX = 0; iX < pImg->iWidth; iX++) {
			iBlue = iNextByte(pFile);
			if (iBlue == EOF) {
				return;
			}
			iGreen = iNextByte(pFile);
			if (iGreen == EOF) {
				return;
			}
			iRed = iNextByte(pFile);
			if (iRed == EOF) {
				return;
			}
			iOffset = iY * iByteWidth + iX;
			*(pucData + iOffset) =
					iReduceColor(iRed, iGreen, iBlue);
		}
		(void)tSkipBytes(pFile, iPadding);
	}
} /* end of vDecode24bpp */

/*
 * vDecodeRle4 - decode a RLE compressed 4 bits per pixel image
 */
static void
vDecodeRle4(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iByteWidth, iOffset, iTmp, iHalfWidth;
	int	iRun, iRunLength, iHalfRun;
	BOOL	bEOL;
	UCHAR	ucTmp;

	DBG_MSG("vDecodeRle4");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 16);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	iByteWidth = iGetByteWidth(pImg);
	iHalfWidth = (pImg->iWidth + 1) / 2;

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		bEOL = FALSE;
		iX = 0;
		while (!bEOL) {
			iRunLength = iNextByte(pFile);
			if (iRunLength == EOF) {
				return;
			}
			if (iRunLength != 0) {
			  	/*
				 * Encoded packet:
				 * RunLength pixels, all the "same" value
				 */
				iTmp = iNextByte(pFile);
				if (iTmp == EOF) {
					return;
				}
				/* Reverse the nibble order */
				ucTmp = (iTmp & 0xf0) >> 4;
				ucTmp |= (iTmp & 0x0f) << 4;
				iHalfRun = (iRunLength + 1) / 2;
				for (iRun = 0; iRun < iHalfRun; iRun++) {
					if (iX < iHalfWidth) {
						iOffset = iY * iByteWidth + iX;
						*(pucData + iOffset) = ucTmp;
					}
					iX++;
				}
				continue;
			}
			/* Literal or escape */
			iRunLength = iNextByte(pFile);
			if (iRunLength == EOF) {
				return;
			}
			if (iRunLength == 0) {		/* End of line escape */
				bEOL = TRUE;
			} else if (iRunLength == 1) {	/* End of file escape */
				return;
			} else if (iRunLength == 2) {	/* Delta escape */
				DBG_MSG("RLE4: encountered delta escape");
				return;
			} else {			/* Literal packet */
				iHalfRun = (iRunLength + 1) / 2;
				for (iRun = 0; iRun < iHalfRun; iRun++) {
					iTmp = iNextByte(pFile);
					if (iTmp == EOF) {
						return;
					}
					/* Reverse the nibble order */
					ucTmp = (iTmp & 0xf0) >> 4;
					ucTmp |= (iTmp & 0x0f) << 4;
					if (iX < iHalfWidth) {
						iOffset = iY * iByteWidth + iX;
						*(pucData + iOffset) = ucTmp;
					}
					iX++;
				}
				/* Padding if the number of bytes is odd */
				if (odd(iHalfRun)) {
					(void)tSkipBytes(pFile, 1);
				}
			}
		}
		DBG_DEC_C(iX != iHalfWidth, iX);
	}
} /* end of vDecodeRle4 */

/*
 * vDecodeRle8 - decode a RLE compressed 8 bits per pixel image
 */
static void
vDecodeRle8(FILE *pFile, UCHAR *pucData, const imagedata_type *pImg)
{
	int	iX, iY, iRun, iRunLength, iOffset, iIndex, iByteWidth;
	BOOL	bEOL;

	DBG_MSG("vDecodeRle8");

	fail(pFile == NULL);
	fail(pucData == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 256);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	iByteWidth = iGetByteWidth(pImg);

	for (iY = pImg->iHeight - 1; iY >= 0; iY--) {
		bEOL = FALSE;
		iX = 0;
		while (!bEOL) {
			iRunLength = iNextByte(pFile);
			if (iRunLength == EOF) {
				return;
			}
			if (iRunLength != 0) {
			  	/*
				 * Encoded packet:
				 * RunLength pixels, all the same value
				 */
				iIndex = iNextByte(pFile);
				if (iIndex == EOF) {
					return;
				}
				for (iRun = 0; iRun < iRunLength; iRun++) {
					if (iX < pImg->iWidth) {
						iOffset = iY * iByteWidth + iX;
						*(pucData + iOffset) =
							iReduceColor(
							pImg->aucPalette[iIndex][0],
							pImg->aucPalette[iIndex][1],
							pImg->aucPalette[iIndex][2]);
					}
					iX++;
				}
				continue;
			}
			/* Literal or escape */
			iRunLength = iNextByte(pFile);
			if (iRunLength == EOF) {
				return;
			}
			if (iRunLength == 0) {		/* End of line escape */
				bEOL = TRUE;
			} else if (iRunLength == 1) {	/* End of file escape */
				return;
			} else if (iRunLength == 2) {	/* Delta escape */
				DBG_MSG("RLE8: encountered delta escape");
				return;
			} else {			/* Literal packet */
				for (iRun = 0; iRun < iRunLength; iRun++) {
					iIndex = iNextByte(pFile);
					if (iIndex == EOF) {
						return;
					}
					if (iX < pImg->iWidth) {
						iOffset = iY * iByteWidth + iX;
						*(pucData + iOffset) =
							iReduceColor(
							pImg->aucPalette[iIndex][0],
							pImg->aucPalette[iIndex][1],
							pImg->aucPalette[iIndex][2]);
					}
					iX++;
				}
				/* Padding if the number of bytes is odd */
				if (odd(iRunLength)) {
					(void)tSkipBytes(pFile, 1);
				}
			}
		}
		DBG_DEC_C(iX != pImg->iWidth, iX);
	}
} /* end of vDecodeRle8 */

#if 0 /* defined(DEBUG) */
static void
vCopy2File(UCHAR *pucSprite, size_t tSpriteSize)
{
	FILE	*pOutFile;
	int	iIndex;
	char	szFilename[30];

	sprintf(szFilename, "<Wimp$ScrapDir>.sprt%04d", ++iPicCounter);
	pOutFile = fopen(szFilename, "wb");
	if (pOutFile == NULL) {
		return;
	}
	DBG_MSG(szFilename);
	for (iIndex = 4; iIndex < (int)tSpriteSize; iIndex++) {
		if (putc(pucSprite[iIndex], pOutFile) == EOF) {
			break;
		}
	}
	(void)fclose(pOutFile);
	vSetFiletype(szFilename, FILETYPE_SPRITE);
} /* end of vCopy2File */
#endif /* DEBUG */

/*
 * vDecodeDIB - decode a dib picture
 */
static void
vDecodeDIB(diagram_type *pDiag, FILE *pFile, const imagedata_type *pImg)
{
	sprite_areainfo	*pSprite;
	UCHAR	*pucPalette, *pucData;
	size_t	tSpriteSize;
	int	iHeaderSize;

	/* Skip the bitmap info header */
	iHeaderSize = (int)ulNextLong(pFile);
	(void)tSkipBytes(pFile, iHeaderSize - 4);
	/* Skip the colortable */
	if (pImg->uiBitsPerComponent <= 8) {
		(void)tSkipBytes(pFile,
			pImg->iColorsUsed * ((iHeaderSize > 12) ? 4 : 3));
	}

	/* Create an blank sprite */
	pSprite = pCreateBlankSprite(pImg, &tSpriteSize);
	pucPalette = (UCHAR *)pSprite +
			pSprite->firstoffset + sizeof(sprite_header);

	/* Add the pixel information */
	switch (pImg->uiBitsPerComponent) {
	case  1:
		fail(pImg->eCompression != compression_none);
		pucData = pucPalette + 2 * 8;
		vDecode1bpp(pFile, pucData, pImg);
		break;
	case  4:
		fail(pImg->eCompression != compression_none &&
				pImg->eCompression != compression_rle4);
		pucData = pucPalette + 16 * 8;
		if (pImg->eCompression == compression_rle4) {
			vDecodeRle4(pFile, pucData, pImg);
		} else {
			vDecode4bpp(pFile, pucData, pImg);
		}
		break;
	case  8:
		fail(pImg->eCompression != compression_none &&
				pImg->eCompression != compression_rle8);
		pucData = pucPalette + 0 * 8;
		if (pImg->eCompression == compression_rle8) {
			vDecodeRle8(pFile, pucData, pImg);
		} else {
			vDecode8bpp(pFile, pucData, pImg);
		}
		break;
	case 24:
		fail(pImg->eCompression != compression_none);
		pucData = pucPalette + 0 * 8;
		vDecode24bpp(pFile, pucData, pImg);
		break;
	default:
		DBG_DEC(pImg->uiBitsPerComponent);
		break;
	}

#if 0 /* defined(DEBUG) */
	vCopy2File((UCHAR *)pSprite, tSpriteSize);
#endif /* DEBUG */

	/* Add the sprite to the Draw file */
	vImage2Diagram(pDiag, pImg,
		(UCHAR *)pSprite + pSprite->firstoffset,
		tSpriteSize - pSprite->firstoffset);

	/* Clean up before you leave */
	pSprite = xfree(pSprite);
} /* end of vDecodeDIB */

/*
 * bTranslateDIB - translate a DIB picture
 *
 * This function translates a picture from dib to sprite
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bTranslateDIB(diagram_type *pDiag, FILE *pFile,
	ULONG ulFileOffset, const imagedata_type *pImg)
{
	/* Seek to start position of DIB data */
	if (!bSetDataOffset(pFile, ulFileOffset)) {
		return FALSE;
	}

	vDecodeDIB(pDiag, pFile, pImg);

	return TRUE;
} /* end of bTranslateDIB */
