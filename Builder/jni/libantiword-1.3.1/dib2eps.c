/*
 * dib2eps.c
 * Copyright (C) 2000-2003 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to translate dib pictures into eps
 *
 *================================================================
 * This part of the software is based on:
 * The Windows Bitmap Decoder Class part of paintlib
 * Paintlib is copyright (c) 1996-2000 Ulrich von Zadow
 *================================================================
 * The credit should go to him, but all the bugs are mine.
 */

#include <stdio.h>
#include "antiword.h"


/*
 * vDecode1bpp - decode an uncompressed 1 bit per pixel image
 */
static void
vDecode1bpp(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	size_t	tPadding;
	int	iX, iY, iN, iByte, iTmp, iEighthWidth, iUse;

	DBG_MSG("vDecode1bpp");

	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 2);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	iEighthWidth = (pImg->iWidth + 7) / 8;
	tPadding = (size_t)(ROUND4(iEighthWidth) - iEighthWidth);

	for (iY = 0; iY < pImg->iHeight; iY++) {
		for (iX = 0; iX < iEighthWidth; iX++) {
			iByte = iNextByte(pInFile);
			if (iByte == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iX == iEighthWidth - 1 && pImg->iWidth % 8 != 0) {
				iUse = pImg->iWidth % 8;
			} else {
				iUse = 8;
			}
			for (iN = 0; iN < iUse; iN++) {
				switch (iN) {
				case 0: iTmp = (iByte & 0x80) / 128; break;
				case 1: iTmp = (iByte & 0x40) / 64; break;
				case 2: iTmp = (iByte & 0x20) / 32; break;
				case 3: iTmp = (iByte & 0x10) / 16; break;
				case 4: iTmp = (iByte & 0x08) / 8; break;
				case 5: iTmp = (iByte & 0x04) / 4; break;
				case 6: iTmp = (iByte & 0x02) / 2; break;
				case 7: iTmp = (iByte & 0x01); break;
				default: iTmp = 0; break;
				}
				vASCII85EncodeByte(pOutFile, iTmp);
			}
		}
		(void)tSkipBytes(pInFile, tPadding);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecode1bpp */

/*
 * vDecode4bpp - decode an uncompressed 4 bits per pixel image
 */
static void
vDecode4bpp(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	size_t	tPadding;
	int	iX, iY, iN, iByte, iTmp, iHalfWidth, iUse;

	DBG_MSG("vDecode4bpp");

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 16);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	iHalfWidth = (pImg->iWidth + 1) / 2;
	tPadding = (size_t)(ROUND4(iHalfWidth) - iHalfWidth);

	for (iY = 0; iY < pImg->iHeight; iY++) {
		for (iX = 0; iX < iHalfWidth; iX++) {
			iByte = iNextByte(pInFile);
			if (iByte == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iX == iHalfWidth - 1 && odd(pImg->iWidth)) {
				iUse = 1;
			} else {
				iUse = 2;
			}
			for (iN = 0; iN < iUse; iN++) {
				if (odd(iN)) {
					iTmp = iByte & 0x0f;
				} else {
					iTmp = (iByte & 0xf0) / 16;
				}
				vASCII85EncodeByte(pOutFile, iTmp);
			}
		}
		(void)tSkipBytes(pInFile, tPadding);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecode4bpp */

/*
 * vDecode8bpp - decode an uncompressed 8 bits per pixel image
 */
static void
vDecode8bpp(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	size_t	tPadding;
	int	iX, iY, iByte;

	DBG_MSG("vDecode8bpp");

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 256);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	tPadding = (size_t)(ROUND4(pImg->iWidth) - pImg->iWidth);

	for (iY = 0; iY < pImg->iHeight; iY++) {
		for (iX = 0; iX < pImg->iWidth; iX++) {
			iByte = iNextByte(pInFile);
			if (iByte == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			vASCII85EncodeByte(pOutFile, iByte);
		}
		(void)tSkipBytes(pInFile, tPadding);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecode8bpp */

/*
 * vDecode24bpp - decode an uncompressed 24 bits per pixel image
 */
static void
vDecode24bpp(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	size_t	tPadding;
	int	iX, iY, iBlue, iGreen, iRed, iTripleWidth;

	DBG_MSG("vDecode24bpp");

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(!pImg->bColorImage);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	iTripleWidth = pImg->iWidth * 3;
	tPadding = (size_t)(ROUND4(iTripleWidth) - iTripleWidth);

	for (iY = 0; iY < pImg->iHeight; iY++) {
		for (iX = 0; iX < pImg->iWidth; iX++) {
			/* Change from BGR order to RGB order */
			iBlue = iNextByte(pInFile);
			if (iBlue == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			iGreen = iNextByte(pInFile);
			if (iGreen == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			iRed = iNextByte(pInFile);
			if (iRed == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			vASCII85EncodeByte(pOutFile, iRed);
			vASCII85EncodeByte(pOutFile, iGreen);
			vASCII85EncodeByte(pOutFile, iBlue);
		}
		(void)tSkipBytes(pInFile, tPadding);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecode24bpp */

/*
 * vDecodeRle4 - decode a RLE compressed 4 bits per pixel image
 */
static void
vDecodeRle4(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	int	iX, iY, iByte, iTmp, iRunLength, iRun;
	BOOL	bEOF, bEOL;

	DBG_MSG("vDecodeRle4");

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 16);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	bEOF = FALSE;

	for (iY =  0; iY < pImg->iHeight && !bEOF; iY++) {
		bEOL = FALSE;
		iX = 0;
		while (!bEOL) {
			iRunLength = iNextByte(pInFile);
			if (iRunLength == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iRunLength != 0) {
				/*
				 * Encoded packet:
				 * RunLength pixels, all the "same" value
				 */
				iByte = iNextByte(pInFile);
				if (iByte == EOF) {
					vASCII85EncodeByte(pOutFile, EOF);
					return;
				}
				for (iRun = 0; iRun < iRunLength; iRun++) {
					if (odd(iRun)) {
						iTmp = iByte & 0x0f;
					} else {
						iTmp = (iByte & 0xf0) / 16;
					}
					if (iX < pImg->iWidth) {
						vASCII85EncodeByte(pOutFile, iTmp);
					}
					iX++;
				}
				continue;
			}
			/* Literal or escape */
			iRunLength = iNextByte(pInFile);
			if (iRunLength == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iRunLength == 0) {		/* End of line escape */
				bEOL = TRUE;
			} else if (iRunLength == 1) {	/* End of file escape */
				bEOF = TRUE;
				bEOL = TRUE;
			} else if (iRunLength == 2) {	/* Delta escape */
				DBG_MSG("RLE4: encountered delta escape");
				bEOF = TRUE;
				bEOL = TRUE;
			} else {			/* Literal packet */
				iByte = 0;
				for (iRun = 0; iRun < iRunLength; iRun++) {
					if (odd(iRun)) {
						iTmp = iByte & 0x0f;
					} else {
						iByte = iNextByte(pInFile);
						if (iByte == EOF) {
							vASCII85EncodeByte(pOutFile, EOF);
							return;
						}
						iTmp = (iByte & 0xf0) / 16;
					}
					if (iX < pImg->iWidth) {
						vASCII85EncodeByte(pOutFile, iTmp);
					}
					iX++;
				}
				/* Padding if the number of bytes is odd */
				if (odd((iRunLength + 1) / 2)) {
					(void)tSkipBytes(pInFile, 1);
				}
			}
		}
		DBG_DEC_C(iX != pImg->iWidth, iX);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecodeRle4 */

/*
 * vDecodeRle8 - decode a RLE compressed 8 bits per pixel image
 */
static void
vDecodeRle8(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	int	iX, iY, iByte, iRunLength, iRun;
	BOOL	bEOF, bEOL;

	DBG_MSG("vDecodeRle8");

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);
	fail(pImg->iColorsUsed < 1 || pImg->iColorsUsed > 256);

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);

	bEOF = FALSE;

	for (iY = 0; iY < pImg->iHeight && !bEOF; iY++) {
		bEOL = FALSE;
		iX = 0;
		while (!bEOL) {
			iRunLength = iNextByte(pInFile);
			if (iRunLength == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iRunLength != 0) {
				/*
				 * Encoded packet:
				 * RunLength pixels, all the same value
				 */
				iByte = iNextByte(pInFile);
				if (iByte == EOF) {
					vASCII85EncodeByte(pOutFile, EOF);
					return;
				}
				for (iRun = 0; iRun < iRunLength; iRun++) {
					if (iX < pImg->iWidth) {
						vASCII85EncodeByte(pOutFile, iByte);
					}
					iX++;
				}
				continue;
			}
			/* Literal or escape */
			iRunLength = iNextByte(pInFile);
			if (iRunLength == EOF) {
				vASCII85EncodeByte(pOutFile, EOF);
				return;
			}
			if (iRunLength == 0) {		/* End of line escape */
				bEOL = TRUE;
			} else if (iRunLength == 1) {	/* End of file escape */
				bEOF = TRUE;
				bEOL = TRUE;
			} else if (iRunLength == 2) {	/* Delta escape */
				DBG_MSG("RLE8: encountered delta escape");
				bEOF = TRUE;
				bEOL = TRUE;
			} else {			/* Literal packet */
				for (iRun = 0; iRun < iRunLength; iRun++) {
					iByte = iNextByte(pInFile);
					if (iByte == EOF) {
						vASCII85EncodeByte(pOutFile, EOF);
						return;
					}
					if (iX < pImg->iWidth) {
						vASCII85EncodeByte(pOutFile, iByte);
					}
					iX++;
				}
				/* Padding if the number of bytes is odd */
				if (odd(iRunLength)) {
					(void)tSkipBytes(pInFile, 1);
				}
			}
		}
		DBG_DEC_C(iX != pImg->iWidth, iX);
	}
	vASCII85EncodeByte(pOutFile, EOF);
} /* end of vDecodeRle8 */

/*
 * vDecodeDIB - decode a dib picture
 */
static void
vDecodeDIB(FILE *pInFile, FILE *pOutFile, const imagedata_type *pImg)
{
	size_t	tHeaderSize;

	fail(pInFile == NULL);
	fail(pOutFile == NULL);
	fail(pImg == NULL);

	/* Skip the bitmap info header */
	tHeaderSize = (size_t)ulNextLong(pInFile);
	(void)tSkipBytes(pInFile, tHeaderSize - 4);
	/* Skip the colortable */
	if (pImg->uiBitsPerComponent <= 8) {
		(void)tSkipBytes(pInFile,
			(size_t)(pImg->iColorsUsed *
			 ((tHeaderSize > 12) ? 4 : 3)));
	}

	switch (pImg->uiBitsPerComponent) {
	case 1:
		fail(pImg->eCompression != compression_none);
		vDecode1bpp(pInFile, pOutFile, pImg);
		break;
	case 4:
		fail(pImg->eCompression != compression_none &&
				pImg->eCompression != compression_rle4);
		if (pImg->eCompression == compression_rle4) {
			vDecodeRle4(pInFile, pOutFile, pImg);
		} else {
			vDecode4bpp(pInFile, pOutFile, pImg);
		}
		break;
	case 8:
		fail(pImg->eCompression != compression_none &&
				pImg->eCompression != compression_rle8);
		if (pImg->eCompression == compression_rle8) {
			vDecodeRle8(pInFile, pOutFile, pImg);
		} else {
			vDecode8bpp(pInFile, pOutFile, pImg);
		}
		break;
	case 24:
		fail(pImg->eCompression != compression_none);
		vDecode24bpp(pInFile, pOutFile, pImg);
		break;
	default:
		DBG_DEC(pImg->uiBitsPerComponent);
		break;
	}
} /* end of vDecodeDIB */

#if defined(DEBUG)
/*
 * vCopy2File
 */
static void
vCopy2File(FILE *pInFile, ULONG ulFileOffset, size_t tPictureLen)
{
	static int	iPicCounter = 0;
	FILE	*pOutFile;
	size_t	tIndex;
	int	iTmp;
	char	szFilename[30];

	if (!bSetDataOffset(pInFile, ulFileOffset)) {
		return;
	}

	sprintf(szFilename, "/tmp/pic/pic%04d.bmp", ++iPicCounter);
	pOutFile = fopen(szFilename, "wb");
	if (pOutFile == NULL) {
		return;
	}
	/* Turn a dib into a bmp by adding a fake 14 byte header */
	(void)putc('B', pOutFile);
	(void)putc('M', pOutFile);
	for (iTmp = 0; iTmp < 12; iTmp++) {
		if (putc(0, pOutFile) == EOF) {
			break;
		}
	}
	for (tIndex = 0; tIndex < tPictureLen; tIndex++) {
		iTmp = iNextByte(pInFile);
		if (putc(iTmp, pOutFile) == EOF) {
			break;
		}
	}
	(void)fclose(pOutFile);
} /* end of vCopy2File */
#endif /* DEBUG */

/*
 * bTranslateDIB - translate a DIB picture
 *
 * This function translates a picture from dib to eps
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bTranslateDIB(diagram_type *pDiag, FILE *pInFile,
		ULONG ulFileOffset, const imagedata_type *pImg)
{
#if defined(DEBUG)
	fail(pImg->tPosition > pImg->tLength);
	vCopy2File(pInFile, ulFileOffset, pImg->tLength - pImg->tPosition);
#endif /* DEBUG */

	/* Seek to start position of DIB data */
	if (!bSetDataOffset(pInFile, ulFileOffset)) {
		return FALSE;
	}

	vImagePrologue(pDiag, pImg);
	vDecodeDIB(pInFile, pDiag->pOutFile, pImg);
	vImageEpilogue(pDiag);

	return TRUE;
} /* end of bTranslateDIB */
