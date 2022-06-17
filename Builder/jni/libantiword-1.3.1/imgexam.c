/*
 * imgexam.c
 * Copyright (C) 2000-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Functions to examine image headers
 *
 *================================================================
 * Part of this software is based on:
 * jpeg2ps - convert JPEG compressed images to PostScript Level 2
 * Copyright (C) 1994-99 Thomas Merz (tm@muc.de)
 *================================================================
 * The credit should go to him, but all the bugs are mine.
 */

#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include "antiword.h"

/* BMP compression types */
#define BI_RGB		0
#define BI_RLE8		1
#define BI_RLE4		2

/* PNG colortype bits */
#define PNG_CB_PALETTE		0x01
#define PNG_CB_COLOR		0x02
#define PNG_CB_ALPHA		0x04

/* Instance signature */
#define MSOBI_WMF	0x0216
#define MSOBI_EMF	0x03d4
#define MSOBI_PICT	0x0542
#define MSOBI_PNG	0x06e0
#define MSOBI_JPEG	0x046a
#define MSOBI_DIB	0x07a8

/* The following enum is stolen from the IJG JPEG library */
typedef enum {		/* JPEG marker codes			*/
	M_SOF0	= 0xc0,	/* baseline DCT				*/
	M_SOF1	= 0xc1,	/* extended sequential DCT		*/
	M_SOF2	= 0xc2,	/* progressive DCT			*/
	M_SOF3	= 0xc3,	/* lossless (sequential)		*/

	M_SOF5	= 0xc5,	/* differential sequential DCT		*/
	M_SOF6	= 0xc6,	/* differential progressive DCT		*/
	M_SOF7	= 0xc7,	/* differential lossless		*/

	M_JPG	= 0xc8,	/* JPEG extensions			*/
	M_SOF9	= 0xc9,	/* extended sequential DCT		*/
	M_SOF10	= 0xca,	/* progressive DCT			*/
	M_SOF11	= 0xcb,	/* lossless (sequential)		*/

	M_SOF13	= 0xcd,	/* differential sequential DCT		*/
	M_SOF14	= 0xce,	/* differential progressive DCT		*/
	M_SOF15	= 0xcf,	/* differential lossless		*/

	M_DHT	= 0xc4,	/* define Huffman tables		*/

	M_DAC	= 0xcc,	/* define arithmetic conditioning table	*/

	M_RST0	= 0xd0,	/* restart				*/
	M_RST1	= 0xd1,	/* restart				*/
	M_RST2	= 0xd2,	/* restart				*/
	M_RST3	= 0xd3,	/* restart				*/
	M_RST4	= 0xd4,	/* restart				*/
	M_RST5	= 0xd5,	/* restart				*/
	M_RST6	= 0xd6,	/* restart				*/
	M_RST7	= 0xd7,	/* restart				*/

	M_SOI	= 0xd8,	/* start of image			*/
	M_EOI	= 0xd9,	/* end of image				*/
	M_SOS	= 0xda,	/* start of scan			*/
	M_DQT	= 0xdb,	/* define quantization tables		*/
	M_DNL	= 0xdc,	/* define number of lines		*/
	M_DRI	= 0xdd,	/* define restart interval		*/
	M_DHP	= 0xde,	/* define hierarchical progression	*/
	M_EXP	= 0xdf,	/* expand reference image(s)		*/

	M_APP0	= 0xe0,	/* application marker, used for JFIF	*/
	M_APP1	= 0xe1,	/* application marker			*/
	M_APP2	= 0xe2,	/* application marker			*/
	M_APP3	= 0xe3,	/* application marker			*/
	M_APP4	= 0xe4,	/* application marker			*/
	M_APP5	= 0xe5,	/* application marker			*/
	M_APP6	= 0xe6,	/* application marker			*/
	M_APP7	= 0xe7,	/* application marker			*/
	M_APP8	= 0xe8,	/* application marker			*/
	M_APP9	= 0xe9,	/* application marker			*/
	M_APP10	= 0xea,	/* application marker			*/
	M_APP11	= 0xeb,	/* application marker			*/
	M_APP12	= 0xec,	/* application marker			*/
	M_APP13	= 0xed,	/* application marker			*/
	M_APP14	= 0xee,	/* application marker, used by Adobe	*/
	M_APP15	= 0xef,	/* application marker			*/

	M_JPG0	= 0xf0,	/* reserved for JPEG extensions		*/
	M_JPG13	= 0xfd,	/* reserved for JPEG extensions		*/
	M_COM	= 0xfe,	/* comment				*/

	M_TEM	= 0x01	/* temporary use			*/
} JPEG_MARKER;


/*
 * bFillPaletteDIB - fill the palette part of the imagesdata
 *
 * returns TRUE if the images must be a color image, otherwise FALSE;
 */
static BOOL
bFillPaletteDIB(FILE *pFile, imagedata_type *pImg, BOOL bNewFormat)
{
	int	iIndex;
	BOOL	bIsColorPalette;

	fail(pFile == NULL);
	fail(pImg == NULL);

	if (pImg->uiBitsPerComponent > 8) {
		/* No palette, image uses more than 256 colors */
		return TRUE;
	}

	if (pImg->iColorsUsed <= 0) {
		/* Not specified, so compute the number of colors used */
		pImg->iColorsUsed = 1 << pImg->uiBitsPerComponent;
	}

	fail(pImg->iColorsUsed > 256);
	if (pImg->iColorsUsed > 256) {
		pImg->iColorsUsed = 256;
	}

	bIsColorPalette = FALSE;
	for (iIndex = 0; iIndex < pImg->iColorsUsed; iIndex++) {
		/* From BGR order to RGB order */
		pImg->aucPalette[iIndex][2] = (UCHAR)iNextByte(pFile);
		pImg->aucPalette[iIndex][1] = (UCHAR)iNextByte(pFile);
		pImg->aucPalette[iIndex][0] = (UCHAR)iNextByte(pFile);
		if (bNewFormat) {
			(void)iNextByte(pFile);
		}
		NO_DBG_PRINT_BLOCK(pImg->aucPalette[iIndex], 3);
		if (pImg->aucPalette[iIndex][0] !=
		     pImg->aucPalette[iIndex][1] ||
		    pImg->aucPalette[iIndex][1] !=
		     pImg->aucPalette[iIndex][2]) {
			bIsColorPalette = TRUE;
		}
	}

	return bIsColorPalette;
} /* end of bFillPaletteDIB */

/*
 * bExamineDIB - Examine a DIB header
 *
 * return TRUE if successful, otherwise FALSE
 */
static BOOL
bExamineDIB(FILE *pFile, imagedata_type *pImg)
{
	size_t	tHeaderSize;
	int	iPlanes, iCompression;

	tHeaderSize = (size_t)ulNextLong(pFile);
	switch (tHeaderSize) {
	case 12:
		pImg->iWidth = (int)usNextWord(pFile);
		pImg->iHeight = (int)usNextWord(pFile);
		iPlanes = (int)usNextWord(pFile);
		pImg->uiBitsPerComponent = (UINT)usNextWord(pFile);
		iCompression = BI_RGB;
		pImg->iColorsUsed = 0;
		break;
	case 40:
	case 64:
		pImg->iWidth = (int)ulNextLong(pFile);
		pImg->iHeight = (int)ulNextLong(pFile);
		iPlanes = (int)usNextWord(pFile);
		pImg->uiBitsPerComponent = (UINT)usNextWord(pFile);
		iCompression = (int)ulNextLong(pFile);
		(void)tSkipBytes(pFile, 12);
		pImg->iColorsUsed = (int)ulNextLong(pFile);
		(void)tSkipBytes(pFile, tHeaderSize - 36);
		break;
	default:
		DBG_DEC(tHeaderSize);
		return FALSE;
	}
	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);
	DBG_DEC(pImg->uiBitsPerComponent);
	DBG_DEC(iCompression);
	DBG_DEC(pImg->iColorsUsed);

	/* Do some sanity checks with the parameters */
	if (iPlanes != 1) {
		DBG_DEC(iPlanes);
		return FALSE;
	}
	if (pImg->iWidth <= 0 || pImg->iHeight <= 0) {
		DBG_DEC(pImg->iWidth);
		DBG_DEC(pImg->iHeight);
		return FALSE;
	}
	if (pImg->uiBitsPerComponent != 1 && pImg->uiBitsPerComponent != 4 &&
	    pImg->uiBitsPerComponent != 8 && pImg->uiBitsPerComponent != 24) {
		DBG_DEC(pImg->uiBitsPerComponent);
		return FALSE;
	}
	if (iCompression != BI_RGB &&
	    (pImg->uiBitsPerComponent == 1 || pImg->uiBitsPerComponent == 24)) {
		return FALSE;
	}
	if (iCompression == BI_RLE8 && pImg->uiBitsPerComponent == 4) {
		return FALSE;
	}
	if (iCompression == BI_RLE4 && pImg->uiBitsPerComponent == 8) {
		return FALSE;
	}

	switch (iCompression) {
	case BI_RGB:
		pImg->eCompression = compression_none;
		break;
	case BI_RLE4:
		pImg->eCompression = compression_rle4;
		break;
	case BI_RLE8:
		pImg->eCompression = compression_rle8;
		break;
	default:
		DBG_DEC(iCompression);
		return FALSE;
	}

	pImg->bColorImage = bFillPaletteDIB(pFile, pImg, tHeaderSize > 12);

	if (pImg->uiBitsPerComponent <= 8) {
		pImg->iComponents = 1;
	} else {
		pImg->iComponents = (int)(pImg->uiBitsPerComponent / 8);
	}

	return TRUE;
} /* end of bExamineDIB */

/*
 * iNextMarker - read the next JPEG marker
 */
static int
iNextMarker(FILE *pFile)
{
	int	iMarker;

	do {
		do {
			iMarker = iNextByte(pFile);
		} while (iMarker != 0xff && iMarker != EOF);
		if (iMarker == EOF) {
			return EOF;
		}
		do {
			iMarker = iNextByte(pFile);
		} while (iMarker == 0xff);
	} while (iMarker == 0x00);			/* repeat if ff/00 */

	return iMarker;
} /* end of iNextMarker */

/*
 * bExamineJPEG - Examine a JPEG header
 *
 * return TRUE if successful, otherwise FALSE
 */
static BOOL
bExamineJPEG(FILE *pFile, imagedata_type *pImg)
{
	size_t	tLength;
	int	iMarker, iIndex;
	char	appstring[10];
	BOOL	bSOFDone;

	tLength = 0;
	bSOFDone = FALSE;

	/* process JPEG markers */
	while (!bSOFDone && (iMarker = iNextMarker(pFile)) != (int)M_EOI) {
		switch (iMarker) {
		case EOF:
			DBG_MSG("Error: unexpected end of JPEG file");
			return FALSE;
	/* The following are not officially supported in PostScript level 2 */
		case M_SOF2:
		case M_SOF3:
		case M_SOF5:
		case M_SOF6:
		case M_SOF7:
		case M_SOF9:
		case M_SOF10:
		case M_SOF11:
		case M_SOF13:
		case M_SOF14:
		case M_SOF15:
			DBG_HEX(iMarker);
			return FALSE;
		case M_SOF0:
		case M_SOF1:
			tLength = (size_t)usNextWordBE(pFile);
			pImg->uiBitsPerComponent = (UINT)iNextByte(pFile);
			pImg->iHeight = (int)usNextWordBE(pFile);
			pImg->iWidth = (int)usNextWordBE(pFile);
			pImg->iComponents = iNextByte(pFile);
			bSOFDone = TRUE;
			break;
		case M_APP14:
		/*
		 * Check for Adobe application marker. It is known (per Adobe's
		 * TN5116) to contain the string "Adobe" at the start of the
		 * APP14 marker.
		 */
			tLength = (size_t)usNextWordBE(pFile);
			if (tLength < 12) {
				(void)tSkipBytes(pFile, tLength - 2);
			} else {
				for (iIndex = 0; iIndex < 5; iIndex++) {
					appstring[iIndex] =
							(char)iNextByte(pFile);
				}
				appstring[5] = '\0';
				if (STREQ(appstring, "Adobe")) {
					pImg->bAdobe = TRUE;
				}
				(void)tSkipBytes(pFile, tLength - 7);
			}
			break;
		case M_SOI:		/* ignore markers without parameters */
		case M_EOI:
		case M_TEM:
		case M_RST0:
		case M_RST1:
		case M_RST2:
		case M_RST3:
		case M_RST4:
		case M_RST5:
		case M_RST6:
		case M_RST7:
			break;
		default:		/* skip variable length markers */
			tLength = (size_t)usNextWordBE(pFile);
			(void)tSkipBytes(pFile, tLength - 2);
			break;
		}
	}

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);
	DBG_DEC(pImg->uiBitsPerComponent);
	DBG_DEC(pImg->iComponents);

	/* Do some sanity checks with the parameters */
	if (pImg->iHeight <= 0 ||
	    pImg->iWidth <= 0 ||
	    pImg->iComponents <= 0) {
		DBG_DEC(pImg->iHeight);
		DBG_DEC(pImg->iWidth);
		DBG_DEC(pImg->iComponents);
		return FALSE;
	}

	/* Some broken JPEG files have this but they print anyway... */
	if (pImg->iComponents * 3 + 8 != (int)tLength) {
		DBG_MSG("Warning: SOF marker has incorrect length - ignored");
	}

	if (pImg->uiBitsPerComponent != 8) {
		DBG_DEC(pImg->uiBitsPerComponent);
		DBG_MSG("Not supported in PostScript level 2");
		return FALSE;
	}

	if (pImg->iComponents != 1 &&
	    pImg->iComponents != 3 &&
	    pImg->iComponents != 4) {
		DBG_DEC(pImg->iComponents);
		return FALSE;
	}

	pImg->bColorImage = pImg->iComponents >= 3;
	pImg->iColorsUsed = 0;
	pImg->eCompression = compression_jpeg;

	return TRUE;
} /* end of bExamineJPEG */

/*
 * bFillPalettePNG - fill the palette part of the imagesdata
 *
 * returns TRUE if sucessful, otherwise FALSE;
 */
static BOOL
bFillPalettePNG(FILE *pFile, imagedata_type *pImg, size_t tLength)
{
	int	iIndex, iEntries;

	fail(pFile == NULL);
	fail(pImg == NULL);

	if (pImg->uiBitsPerComponent > 8) {
		/* No palette, image uses more than 256 colors */
		return TRUE;
	}

	if (!pImg->bColorImage) {
		/* Only color images can have a palette */
		return FALSE;
	}

	if (tLength % 3 != 0) {
		/* Each palette entry takes three bytes */
		DBG_DEC(tLength);
		return FALSE;
	}

	iEntries = (int)(tLength / 3);
	DBG_DEC(iEntries);
	pImg->iColorsUsed = 1 << pImg->uiBitsPerComponent;
	DBG_DEC(pImg->iColorsUsed);

	if (iEntries > 256) {
		DBG_DEC(iEntries);
		return FALSE;
	}

	for (iIndex = 0; iIndex < iEntries; iIndex++) {
		pImg->aucPalette[iIndex][0] = (UCHAR)iNextByte(pFile);
		pImg->aucPalette[iIndex][1] = (UCHAR)iNextByte(pFile);
		pImg->aucPalette[iIndex][2] = (UCHAR)iNextByte(pFile);
		NO_DBG_PRINT_BLOCK(pImg->aucPalette[iIndex], 3);
	}
	for (;iIndex < pImg->iColorsUsed; iIndex++) {
		pImg->aucPalette[iIndex][0] = 0;
		pImg->aucPalette[iIndex][1] = 0;
		pImg->aucPalette[iIndex][2] = 0;
	}

	return TRUE;
} /* end of bFillPalettePNG */

/*
 * bExaminePNG - Examine a PNG header
 *
 * return TRUE if successful, otherwise FALSE
 */
static BOOL
bExaminePNG(FILE *pFile, imagedata_type *pImg)
{
	size_t		tLength;
	ULONG		ulLong1, ulLong2, ulName;
	int		iIndex, iTmp;
	int		iCompressionMethod, iFilterMethod, iInterlaceMethod;
	int		iColor, iIncrement;
	BOOL		bHasPalette, bHasAlpha;
	UCHAR	aucBuf[4];

	/* Check signature */
	ulLong1 = ulNextLongBE(pFile);
	ulLong2 = ulNextLongBE(pFile);
	if (ulLong1 != 0x89504e47UL || ulLong2 != 0x0d0a1a0aUL) {
		DBG_HEX(ulLong1);
		DBG_HEX(ulLong2);
		return FALSE;
	}

	ulName = 0x00;
	bHasPalette = FALSE;

	/* Examine chunks */
	while (ulName != PNG_CN_IEND) {
		tLength = (size_t)ulNextLongBE(pFile);
		ulName = 0x00;
		for (iIndex = 0; iIndex < (int)elementsof(aucBuf); iIndex++) {
			aucBuf[iIndex] = (UCHAR)iNextByte(pFile);
			if (!isalpha(aucBuf[iIndex])) {
				DBG_HEX(aucBuf[iIndex]);
				return FALSE;
			}
			ulName <<= 8;
			ulName |= aucBuf[iIndex];
		}

		switch (ulName) {
		case PNG_CN_IHDR:
			/* Header chunck */
			if (tLength < 13) {
				DBG_DEC(tLength);
				return FALSE;
			}
			pImg->iWidth = (int)ulNextLongBE(pFile);
			pImg->iHeight = (int)ulNextLongBE(pFile);
			pImg->uiBitsPerComponent = (UINT)iNextByte(pFile);
			iTmp = iNextByte(pFile);
			NO_DBG_HEX(iTmp);
			pImg->bColorImage = (iTmp & PNG_CB_COLOR) != 0;
			bHasPalette = (iTmp & PNG_CB_PALETTE) != 0;
			bHasAlpha = (iTmp & PNG_CB_ALPHA) != 0;
			if (bHasPalette && pImg->uiBitsPerComponent > 8) {
				/* This should not happen */
				return FALSE;
			}
			pImg->iComponents =
				(bHasPalette || !pImg->bColorImage) ? 1 : 3;
			if (bHasAlpha) {
				pImg->iComponents++;
			}
			iCompressionMethod = iNextByte(pFile);
			if (iCompressionMethod != 0) {
				DBG_DEC(iCompressionMethod);
				return FALSE;
			}
			iFilterMethod = iNextByte(pFile);
			if (iFilterMethod != 0) {
				DBG_DEC(iFilterMethod);
				return FALSE;
			}
			iInterlaceMethod = iNextByte(pFile);
			if (iInterlaceMethod != 0) {
				DBG_DEC(iInterlaceMethod);
				return FALSE;
			}
			pImg->iColorsUsed = 0;
			(void)tSkipBytes(pFile, tLength - 13 + 4);
			break;
		case PNG_CN_PLTE:
			if (!bHasPalette) {
				return FALSE;
			}
			if (!bFillPalettePNG(pFile, pImg, tLength)) {
				return FALSE;
			}
			(void)tSkipBytes(pFile, 4);
			break;
		default:
			(void)tSkipBytes(pFile, tLength + 4);
			break;
		}
	}

	DBG_DEC(pImg->iWidth);
	DBG_DEC(pImg->iHeight);
	DBG_DEC(pImg->uiBitsPerComponent);
	DBG_DEC(pImg->iColorsUsed);
	DBG_DEC(pImg->iComponents);

	/* Do some sanity checks with the parameters */
	if (pImg->iWidth <= 0 || pImg->iHeight <= 0) {
		return FALSE;
	}

	if (pImg->uiBitsPerComponent != 1 && pImg->uiBitsPerComponent != 2 &&
	    pImg->uiBitsPerComponent != 4 && pImg->uiBitsPerComponent != 8 &&
	    pImg->uiBitsPerComponent != 16) {
		DBG_DEC(pImg->uiBitsPerComponent);
		return  FALSE;
	}

	if (pImg->iComponents != 1 && pImg->iComponents != 3) {
		/* Not supported */
		DBG_DEC(pImg->iComponents);
		return FALSE;
	}

	if (pImg->uiBitsPerComponent > 8) {
		/* Not supported */
		DBG_DEC(pImg->uiBitsPerComponent);
		return FALSE;
	}

	if (pImg->iColorsUsed == 0 &&
	    pImg->iComponents == 1 &&
	    pImg->uiBitsPerComponent <= 4) {
		/*
		 * No palette is supplied, but PostScript needs one in these
		 * cases, so we add a default palette here
		 */
		pImg->iColorsUsed = 1 << pImg->uiBitsPerComponent;
		iIncrement = 0xff / (pImg->iColorsUsed - 1);
		for (iIndex = 0, iColor = 0x00;
		     iIndex < pImg->iColorsUsed;
		     iIndex++, iColor += iIncrement) {
			pImg->aucPalette[iIndex][0] = (UCHAR)iColor;
			pImg->aucPalette[iIndex][1] = (UCHAR)iColor;
			pImg->aucPalette[iIndex][2] = (UCHAR)iColor;
		}
		/* Just to be sure */
		pImg->bColorImage = FALSE;
	}

	pImg->eCompression = compression_zlib;

	return TRUE;
} /* end of bExaminePNG */

/*
 * bExamineWMF - Examine a WMF header
 *
 * return TRUE if successful, otherwise FALSE
 */
static BOOL
bExamineWMF(FILE *pFile, imagedata_type *pImg)
{
	ULONG	ulFileSize, ulMaxRecord, ulMagic;
	USHORT	usType, usHeaderSize, usVersion, usNoObjects;

	usType = usNextWord(pFile);
	usHeaderSize = usNextWord(pFile);
	ulMagic = ((ULONG)usHeaderSize << 16) | (ULONG)usType;
	usVersion = usNextWord(pFile);
	ulFileSize = ulNextLong(pFile);
	usNoObjects = usNextWord(pFile);
	ulMaxRecord = ulNextLong(pFile);

	DBG_HEX(ulMagic);
	DBG_DEC(usType);
	DBG_DEC(usHeaderSize);
	DBG_HEX(usVersion);
	DBG_DEC(ulFileSize);
	DBG_DEC(usNoObjects);
	DBG_DEC(ulMaxRecord);

	return FALSE;
} /* end of bExamineWMF */

#if !defined(__riscos)
/*
 * vImage2Papersize - make sure the image fits on the paper
 *
 * This function should not be needed if Word would do a proper job
 */
static void
vImage2Papersize(imagedata_type *pImg)
{
	static int	iNetPageHeight = -1;
	static int	iNetPageWidth = -1;
	options_type	tOptions;
        double  dVerFactor, dHorFactor, dFactor;

	DBG_MSG("vImage2Papersize");

	fail(pImg == NULL);

	if (iNetPageHeight < 0 || iNetPageWidth < 0) {
		/* Get the page dimensions from the options */
		vGetOptions(&tOptions);
		/* Add 999 to err on the save side */
		iNetPageHeight = tOptions.iPageHeight -
				(lDrawUnits2MilliPoints(
					PS_TOP_MARGIN + PS_BOTTOM_MARGIN) +
					999) / 1000;
		iNetPageWidth = tOptions.iPageWidth -
				(lDrawUnits2MilliPoints(
					PS_LEFT_MARGIN + PS_RIGHT_MARGIN) +
					999) / 1000;
		DBG_DEC(iNetPageHeight);
		DBG_DEC(iNetPageWidth);
	}

	if (pImg->iVerSizeScaled < iNetPageHeight &&
	    pImg->iHorSizeScaled < iNetPageWidth) {
		/* The image fits on the paper */
		return;
	}

	dVerFactor = (double)iNetPageHeight / (double)pImg->iVerSizeScaled;
	dHorFactor = (double)iNetPageWidth / (double)pImg->iHorSizeScaled;
        dFactor = min(dVerFactor, dHorFactor);
        DBG_FLT(dFactor);
        /* Round down, just to be on the save side */
        pImg->iVerSizeScaled = (int)(pImg->iVerSizeScaled * dFactor);
        pImg->iHorSizeScaled = (int)(pImg->iHorSizeScaled * dFactor);
} /* end of vImage2Papersize */
#endif /* !__riscos */

/*
 * tFind6Image - skip until the image is found
 *
 * Find the image in Word 6/7 files
 *
 * returns the new position when a image is found, otherwise -1
 */
static size_t
tFind6Image(FILE *pFile, size_t tPosition, size_t tLength,
	imagetype_enum *peImageType)
{
	ULONG	ulMarker;
	size_t	tRecordLength, tToSkip;
	USHORT	usMarker;

	fail(pFile == NULL);
	fail(peImageType == NULL);

	*peImageType = imagetype_is_unknown;
	if (tPosition + 18 >= tLength) {
		return (size_t)-1;
	}

	ulMarker = ulNextLong(pFile);
	if (ulMarker != 0x00090001) {
		DBG_HEX(ulMarker);
		return (size_t)-1;
	}
	usMarker = usNextWord(pFile);
	if (usMarker != 0x0300) {
		DBG_HEX(usMarker);
		return (size_t)-1;
	}
	(void)tSkipBytes(pFile, 10);
	usMarker = usNextWord(pFile);
	if (usMarker != 0x0000) {
		DBG_HEX(usMarker);
		return (size_t)-1;
	}
	tPosition += 18;

	while (tPosition + 6 <= tLength) {
		tRecordLength = (size_t)ulNextLong(pFile);
		usMarker = usNextWord(pFile);
		tPosition += 6;
		NO_DBG_DEC(tRecordLength);
		NO_DBG_HEX(usMarker);
		switch (usMarker) {
		case 0x0000:
			DBG_HEX(ulGetDataOffset(pFile));
			return (size_t)-1;
		case 0x0b41:
			DBG_MSG("DIB");
			*peImageType = imagetype_is_dib;
			tPosition += tSkipBytes(pFile, 20);
			return tPosition;
		case 0x0f43:
			DBG_MSG("DIB");
			*peImageType = imagetype_is_dib;
			tPosition += tSkipBytes(pFile, 22);
			return tPosition;
		default:
			if (tRecordLength < 3) {
				break;
			}
			if (tRecordLength > SIZE_T_MAX / 2) {
				/*
				 * No need to compute the number of bytes
				 * to skip
				 */
				DBG_DEC(tRecordLength);
				DBG_HEX(tRecordLength);
				DBG_FIXME();
				return (size_t)-1;
			}
			tToSkip = tRecordLength * 2 - 6;
			if (tToSkip > tLength - tPosition) {
				/* You can't skip this number of bytes */
				DBG_DEC(tToSkip);
				DBG_DEC(tLength - tPosition);
				return (size_t)-1;
			}
			tPosition += tSkipBytes(pFile, tToSkip);
			break;
		}
	}

	return (size_t)-1;
} /* end of tFind6Image */

/*
 * tFind8Image - skip until the image is found
 *
 * Find the image in Word 8/9/10 files
 *
 * returns the new position when a image is found, otherwise -1
 */
static size_t
tFind8Image(FILE *pFile, size_t tPosition, size_t tLength,
	imagetype_enum *peImageType)
{
	size_t	tRecordLength, tNameLen;
	USHORT	usRecordVersion, usRecordType, usRecordInstance;
	USHORT	usTmp;

	fail(pFile == NULL);
	fail(peImageType == NULL);

	*peImageType = imagetype_is_unknown;
	while (tPosition + 8 <= tLength) {
		usTmp = usNextWord(pFile);
		usRecordVersion = usTmp & 0x000f;
		usRecordInstance = usTmp >> 4;
		usRecordType = usNextWord(pFile);
		tRecordLength = (size_t)ulNextLong(pFile);
		tPosition += 8;
		NO_DBG_HEX(usRecordVersion);
		NO_DBG_HEX(usRecordInstance);
		NO_DBG_HEX(usRecordType);
		NO_DBG_DEC(tRecordLength);
		switch (usRecordType) {
		case 0xf000: case 0xf001: case 0xf002: case 0xf003:
		case 0xf004: case 0xf005:
			break;
		case 0xf007:
			tPosition += tSkipBytes(pFile, 33);
			tNameLen = (size_t)iNextByte(pFile);
			tPosition++;
			DBG_DEC_C(tNameLen != 0, tNameLen);
			tPosition += tSkipBytes(pFile, 2 + tNameLen * 2);
			break;
		case 0xf008:
			tPosition += tSkipBytes(pFile, 8);
			break;
		case 0xf009:
			tPosition += tSkipBytes(pFile, 16);
			break;
		case 0xf006: case 0xf00a: case 0xf00b: case 0xf00d:
		case 0xf00e: case 0xf00f: case 0xf010: case 0xf011:
		case 0xf122:
			tPosition += tSkipBytes(pFile, tRecordLength);
			break;
		case 0xf01a:
			DBG_MSG("EMF");
			*peImageType = imagetype_is_emf;
			tPosition += tSkipBytes(pFile, 50);
			if ((usRecordInstance ^ MSOBI_EMF) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf01b:
			DBG_MSG("WMF");
			*peImageType = imagetype_is_wmf;
			tPosition += tSkipBytes(pFile, 50);
			if ((usRecordInstance ^ MSOBI_WMF) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf01c:
			DBG_MSG("PICT");
			*peImageType = imagetype_is_pict;
			tPosition += tSkipBytes(pFile, 50);
			if ((usRecordInstance ^ MSOBI_PICT) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf01d:
			DBG_MSG("JPEG");
			*peImageType = imagetype_is_jpeg;
			tPosition += tSkipBytes(pFile, 17);
			if ((usRecordInstance ^ MSOBI_JPEG) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf01e:
			DBG_MSG("PNG");
			*peImageType = imagetype_is_png;
			tPosition += tSkipBytes(pFile, 17);
			if ((usRecordInstance ^ MSOBI_PNG) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf01f:
			DBG_MSG("DIB");
			/* DIB is a BMP minus its 14 byte header */
			*peImageType = imagetype_is_dib;
			tPosition += tSkipBytes(pFile, 17);
			if ((usRecordInstance ^ MSOBI_DIB) == 1) {
				tPosition += tSkipBytes(pFile, 16);
			}
			return tPosition;
		case 0xf00c:
		default:
			DBG_HEX(usRecordType);
			DBG_DEC_C(tRecordLength % 4 != 0, tRecordLength);
			DBG_FIXME();
			return (size_t)-1;
		}
	}

	return (size_t)-1;
} /* end of tFind8Image */

/*
 * eExamineImage - Examine the image
 *
 * Returns an indication of the amount of information found
 */
image_info_enum
eExamineImage(FILE *pFile, ULONG ulFileOffsetImage, imagedata_type *pImg)
{
	long	lTmp;
	size_t	tWordHeaderLen, tLength, tPos;
	int	iType, iHorSize, iVerSize;
	USHORT	usHorScalingFactor, usVerScalingFactor;

	if (ulFileOffsetImage == FC_INVALID) {
		return image_no_information;
	}
	DBG_HEX(ulFileOffsetImage);

	if (!bSetDataOffset(pFile, ulFileOffsetImage)) {
		return image_no_information;
	}

	tLength = (size_t)ulNextLong(pFile);
	DBG_DEC(tLength);
	if (tLength < 46) {
		/* Smaller than the smallest known header */
		DBG_FIXME();
		return image_no_information;
	}
	tWordHeaderLen = (size_t)usNextWord(pFile);
	DBG_DEC(tWordHeaderLen);
	fail(tWordHeaderLen != 46 &&
		tWordHeaderLen != 58 &&
		tWordHeaderLen != 68);

	if (tLength < tWordHeaderLen) {
		/* Smaller than the current header */
		return image_no_information;
	}
	iType = (int)usNextWord(pFile);
	DBG_DEC(iType);
	(void)tSkipBytes(pFile, 28 - 8);

	lTmp = lTwips2MilliPoints(usNextWord(pFile));
	iHorSize = (int)(lTmp / 1000);
	if (lTmp % 1000 != 0) {
		iHorSize++;
	}
	DBG_DEC(iHorSize);
	lTmp = lTwips2MilliPoints(usNextWord(pFile));
	iVerSize = (int)(lTmp / 1000);
	if (lTmp % 1000 != 0) {
		iVerSize++;
	}
	DBG_DEC(iVerSize);

	usHorScalingFactor = usNextWord(pFile);
	DBG_DEC(usHorScalingFactor);
	usVerScalingFactor = usNextWord(pFile);
	DBG_DEC(usVerScalingFactor);

	/* Sanity checks */
	lTmp = (long)iHorSize * (long)usHorScalingFactor;
	if (lTmp < 2835) {
		/* This image would be less than 1 millimeter wide */
		DBG_DEC(lTmp);
		return image_no_information;
	}
	lTmp = (long)iVerSize * (long)usVerScalingFactor;
	if (lTmp < 2835) {
		/* This image would be less than 1 millimeter high */
		DBG_DEC(lTmp);
		return image_no_information;
	}

	/* Skip the rest of the header */
	(void)tSkipBytes(pFile, tWordHeaderLen - 36);
	tPos = tWordHeaderLen;

	(void)memset(pImg, 0, sizeof(*pImg));

	switch (iType) {
	case   7:
	case   8:
		tPos = tFind6Image(pFile, tPos, tLength, &pImg->eImageType);
		if (tPos == (size_t)-1) {
			/* No image found */
			return image_no_information;
		}
		DBG_HEX(tPos);
		break;
	case  94:	/* Word 6/7, no image just a pathname */
		pImg->eImageType = imagetype_is_external;
		DBG_HEX(ulFileOffsetImage + tPos);
		break;
	case 100:
		tPos = tFind8Image(pFile, tPos, tLength, &pImg->eImageType);
		if (tPos == (size_t)-1) {
			/* No image found */
			return image_no_information;
		}
		DBG_HEX(tPos);
		break;
	case 102:	/* Word 8/9/10, no image just a pathname or URL */
		pImg->eImageType = imagetype_is_external;
		DBG_HEX(ulFileOffsetImage + tPos);
		break;
	default:
		DBG_DEC(iType);
		DBG_HEX(ulFileOffsetImage + tPos);
		DBG_FIXME();
		return image_no_information;
	}

	/* Minimal information is now available */
	pImg->tLength = tLength;
	pImg->tPosition = tPos;
	pImg->iHorSizeScaled =
		(int)(((long)iHorSize * (long)usHorScalingFactor + 500) / 1000);
	pImg->iVerSizeScaled =
		(int)(((long)iVerSize * (long)usVerScalingFactor + 500) / 1000);
#if !defined(__riscos)
	vImage2Papersize(pImg);
#endif /* !__riscos */

	/* Image type specific examinations */
	switch (pImg->eImageType) {
	case imagetype_is_dib:
		if (bExamineDIB(pFile, pImg)) {
			return image_full_information;
		}
		return image_minimal_information;
	case imagetype_is_jpeg:
		if (bExamineJPEG(pFile, pImg)) {
			return image_full_information;
		}
		return image_minimal_information;
	case imagetype_is_png:
		if (bExaminePNG(pFile, pImg)) {
			return image_full_information;
		}
		return image_minimal_information;
	case imagetype_is_wmf:
		if (bExamineWMF(pFile, pImg)) {
			return image_full_information;
		}
		return image_minimal_information;
	case imagetype_is_emf:
	case imagetype_is_pict:
	case imagetype_is_external:
		return image_minimal_information;
	case imagetype_is_unknown:
	default:
		return image_no_information;
	}
} /* end of eExamineImage */
