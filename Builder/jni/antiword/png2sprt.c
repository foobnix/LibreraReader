/*
 * png2sprt.c
 * Copyright (C) 2000 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to translate png pictures into sprites
 */

#include <stdio.h>
#include "antiword.h"


/*
 * bTranslatePNG - translate a PNG picture
 *
 * This function translates a picture from png to sprite
 *
 * return TRUE when sucessful, otherwise FALSE
 */
BOOL
bTranslatePNG(diagram_type *pDiag, FILE *pFile,
	ULONG ulFileOffset, size_t tPictureLen, const imagedata_type *pImg)
{
	/* PNG is not supported yet */
	return bAddDummyImage(pDiag, pImg);
} /* end of bTranslatePNG */
