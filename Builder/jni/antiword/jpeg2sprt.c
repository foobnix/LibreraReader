/*
 * jpeg2sprt.c
 * Copyright (C) 2000-2002 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to translate jpeg pictures into sprites
 */

#include <stdio.h>
#include "antiword.h"

#if 0 /* defined(DEBUG) */
static int iPicCounter = 0;
#endif /* DEBUG */


#if 0 /* defined(DEBUG) */
static void
vCopy2File(UCHAR *pucJpeg, size_t tJpegSize)
{
	FILE	*pOutFile;
	size_t	tIndex;
	char	szFilename[30];

	sprintf(szFilename, "<Wimp$ScrapDir>.jpeg%04d", ++iPicCounter);
	pOutFile = fopen(szFilename, "wb");
	if (pOutFile == NULL) {
		return;
	}
	DBG_MSG(szFilename);
	for (tIndex = 0; tIndex < tJpegSize; tIndex++) {
		if (putc(pucJpeg[tIndex], pOutFile) == EOF) {
			break;
		}
	}
	(void)fclose(pOutFile);
	vSetFiletype(szFilename, FILETYPE_JPEG);
} /* end of vCopy2File */
#endif /* DEBUG */

/*
 * bSave2Draw - save the JPEG picture to the Draw file
 *
 * This function puts a JPEG picture in a Draw file
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bSave2Draw(diagram_type *pDiag, FILE *pFile,
	size_t tJpegSize, const imagedata_type *pImg)
{
	UCHAR	*pucJpeg, *pucTmp;
	size_t	tLen;
	int	iByte;

	pucJpeg = xmalloc(tJpegSize);
	for (pucTmp = pucJpeg, tLen = 0; tLen < tJpegSize; pucTmp++, tLen++) {
		iByte = iNextByte(pFile);
		if (iByte == EOF) {
			return FALSE;
		}
		*pucTmp = (UCHAR)iByte;
	}

#if 0 /* defined(DEBUG) */
	vCopy2File(pucJpeg, tJpegSize);
#endif /* DEBUG */

	/* Add the JPEG to the Draw file */
	vImage2Diagram(pDiag, pImg, pucJpeg, tJpegSize);

	xfree(pucJpeg);
	return TRUE;
} /* end of bSave2Draw */

/*
 * bTranslateJPEG - translate a JPEG picture
 *
 * This function translates a picture from jpeg to sprite
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bTranslateJPEG(diagram_type *pDiag, FILE *pFile,
	ULONG ulFileOffset, size_t tPictureLen, const imagedata_type *pImg)
{
  	/* Seek to start position of JPEG data */
	if (!bSetDataOffset(pFile, ulFileOffset)) {
		return FALSE;
	}

	if (iGetRiscOsVersion() >= 360) {
		return bSave2Draw(pDiag, pFile, tPictureLen, pImg);
	}
  	/* JPEG is not supported until RISC OS 3.6 */
	return bAddDummyImage(pDiag, pImg);
} /* end of bTranslateJPEG */
