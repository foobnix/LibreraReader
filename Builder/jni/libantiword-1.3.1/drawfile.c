/*
 * drawfile.c
 * Copyright (C) 2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to process with the Draw diagram
 */

#include <string.h>
#include "DeskLib:Error.h"
#include "DeskLib:SWI.h"
#include "drawfile.h"
#include "antiword.h"

#define DRAWFILE_OBJECT_TOO_SMALL	200
#define DRAWFILE_NO_TEXT		201
#define DRAWFILE_BAD_CHARACTER		202
#define DRAWFILE_SMALL_MEMORY		203
#define DRAWFILE_PATH_WITHOUT_LINES	204
#define DRAWFILE_BAD_PATH_TYPE		205
#define DRAWFILE_PATH_WITHOUT_END	206
#define DRAWFILE_BAD_SPRITE_SIZE	207
#define DRAWFILE_BAD_JPEG_SIZE		208
#define DRAWFILE_TOO_SMALL		209
#define DRAWFILE_NOT_A_DRAWFILE		210
#define DRAWFILE_OBJECT_SIZE		211
#define DRAWFILE_MANY_FONTTABLES	212
#define DRAWFILE_TEXT_NO_FONT		213
#define DRAWFILE_OBJECT_UNEXPECTED	214
#define DRAWFILE_SIZE_ERROR		215

typedef struct drawfile_error_tag {
	int		iErrorNumber;
	const char	*szErrorText;
} drawfile_error_type;

static const drawfile_error_type atErrors[] = {
	{ DRAWFILE_OBJECT_TOO_SMALL, "Object too small"},
	{ DRAWFILE_NO_TEXT, "Text object without text"},
	{ DRAWFILE_BAD_CHARACTER, "Bad character in string"},
	{ DRAWFILE_SMALL_MEMORY, "Not enough memory reserved"},
	{ DRAWFILE_PATH_WITHOUT_LINES, "This path has no lines"},
	{ DRAWFILE_BAD_PATH_TYPE, "Bad path-type in path"},
	{ DRAWFILE_PATH_WITHOUT_END, "No end of path seen"},
	{ DRAWFILE_BAD_SPRITE_SIZE, "Bad sprite size"},
	{ DRAWFILE_BAD_JPEG_SIZE, "Bad jpeg size"},
	{ DRAWFILE_TOO_SMALL, "Too small to be a drawfile"},
	{ DRAWFILE_NOT_A_DRAWFILE, "Not a drawfile"},
	{ DRAWFILE_OBJECT_SIZE, "Object with incorrect size"},
	{ DRAWFILE_MANY_FONTTABLES, "More than one font table"},
	{ DRAWFILE_TEXT_NO_FONT, "Text, but no font table seen"},
	{ DRAWFILE_OBJECT_UNEXPECTED, "Unexpected object type"},
	{ DRAWFILE_SIZE_ERROR, "Sizes don't match"},
};


/*
 * pFillError - error number to error struct
 */
static os_error *
pFillError(int iErrorNumber)
{
	static os_error		tError;
	const drawfile_error_type	*pTmp;
	const char	*szErrorText;

	szErrorText = "Unknown error";
	for (pTmp = atErrors; pTmp < atErrors + elementsof(atErrors); pTmp++) {
		if (iErrorNumber == pTmp->iErrorNumber) {
			szErrorText = pTmp->szErrorText;
			break;
		}
	}
	tError.errnum = iErrorNumber;
	strncpy(tError.errmess, szErrorText, sizeof(tError.errmess) - 1);
	tError.errmess[sizeof(tError.errmess) - 1] = '\0';
	DBG_DEC(tError.errnum);
	DBG_MSG(tError.errmess);
	return &tError;
} /* end of pFillError */

/*
 * Drawfile_BBox - Find the bounding box of a diagram
 */
os_error *
Drawfile_Bbox(drawfile_bbox_flags flags,
	drawfile_diagram const *diagram,
	int size,
	os_trfm const *trfm,
	wimp_box *bbox)
{
	return SWI(5, 0, DrawFile_BBox | XOS_Bit,
		flags, diagram, size, trfm, bbox);
} /* end of Drawfile_Bbox */

/*
 * Drawfile_CreateDiagram - create an empty drawfile diagram
 */
os_error *
Drawfile_CreateDiagram(drawfile_info *pInfo, size_t tMemorySize,
	const char *szCreator, wimp_box tBbox)
{
	drawfile_diagram	*pDiag;

	if (tMemorySize < offsetof(drawfile_diagram, objects)) {
		return pFillError(DRAWFILE_SMALL_MEMORY);
	}
	pDiag = (drawfile_diagram *)pInfo->data;
	strncpy(pDiag->tag, "Draw", 4);
	pDiag->major_version = 201;
	pDiag->minor_version = 0;
	strncpy(pDiag->source, szCreator, sizeof(pDiag->source));
	pDiag->bbox = tBbox;
	/* Memory in use */
	pInfo->length = offsetof(drawfile_diagram, objects);
	return NULL;
} /* end of Drawfile_CreateDiagram */

/*
 * Drawfile_AppendObject - append an object to a diagram
 */
os_error *
Drawfile_AppendObject(drawfile_info *pInfo, size_t tMemorySize,
	const drawfile_object *pObject, BOOL bRebind)
{
	wimp_box	*pMainBbox;
	const wimp_box	*pBbox;
	byte		*pAfter;

	if (tMemorySize < pInfo->length + pObject->size) {
		return pFillError(DRAWFILE_OBJECT_TOO_SMALL);
	}
	/* After the last object */
	pAfter = (byte *)pInfo->data + pInfo->length;
	/* Copy in the new data */
	memcpy(pAfter, pObject, pObject->size);
	/* Rebind if needed */
	if (bRebind) {
		pMainBbox = &((drawfile_diagram *)pInfo->data)->bbox;
		switch (pObject->type) {
		case drawfile_TYPE_FONT_TABLE:
			pBbox = NULL;
			break;
		case drawfile_TYPE_TEXT:
			pBbox = &pObject->data.text.bbox;
			break;
		case drawfile_TYPE_PATH:
			pBbox = &pObject->data.path.bbox;
			break;
		case drawfile_TYPE_SPRITE:
			pBbox = &pObject->data.sprite.bbox;
			break;
		case drawfile_TYPE_GROUP:
			pBbox = &pObject->data.group.bbox;
			break;
		case drawfile_TYPE_TAGGED:
			pBbox = &pObject->data.tagged.bbox;
			break;
		case drawfile_TYPE_TEXT_AREA:
			pBbox = &pObject->data.text_area.bbox;
			break;
		case drawfile_TYPE_TEXT_COLUMN:
			pBbox = NULL;
			break;
		case drawfile_TYPE_OPTIONS:
			pBbox = &pObject->data.options.bbox;
			break;
		case drawfile_TYPE_TRFM_TEXT:
			pBbox = &pObject->data.trfm_text.bbox;
			break;
		case drawfile_TYPE_TRFM_SPRITE:
			pBbox = &pObject->data.trfm_sprite.bbox;
			break;
		case drawfile_TYPE_JPEG:
			pBbox = &pObject->data.jpeg.bbox;
			break;
		default:
			pBbox = NULL;
			break;
		}
		if (pBbox != NULL) {
			if (pBbox->min.x < pMainBbox->min.x) {
				pMainBbox->min.x = pBbox->min.x;
			}
			if (pBbox->min.y < pMainBbox->min.y) {
				pMainBbox->min.y = pBbox->min.y;
			}
			if (pBbox->max.x > pMainBbox->max.x) {
				pMainBbox->max.x = pBbox->max.x;
			}
			if (pBbox->max.y > pMainBbox->max.y) {
				pMainBbox->max.y = pBbox->max.y;
			}
		}
	}
	/* Memory in use */
	pInfo->length += pObject->size;
	return NULL;
} /* end of Drawfile_AppendObject */

/*
 * Replaces the draw_render_diag function from RISC_OSLib
 */
os_error *
Drawfile_RenderDiagram(drawfile_info *pInfo, window_redrawblock *pRedraw,
	double dScale)
{
	int	aiTransform[6];

	fail(pInfo == NULL);
	fail(pInfo->data == NULL);
	fail(pRedraw == NULL);
	fail(dScale < 0.01);

	aiTransform[0] = (int)(dScale * 0x10000);
	aiTransform[1] = 0;
	aiTransform[2] = 0;
	aiTransform[3] = (int)(dScale * 0x10000);
	aiTransform[4] = (pRedraw->rect.min.x - pRedraw->scroll.x) * 256;
	aiTransform[5] = (pRedraw->rect.max.y - pRedraw->scroll.y) * 256;

	return SWI(6, 0, DrawFile_Render | XOS_Bit,
		0, pInfo->data, pInfo->length, aiTransform, &pRedraw->rect, 0);
} /* end of Drawfile_RenderDiagram */

/*
 * pVerifyText - verify a text object
 */
static os_error *
pVerifyText(const drawfile_text *pText)
{
	const unsigned char	*pucTmp;

	if (pText->text[0] == '\0') {
		return pFillError(DRAWFILE_NO_TEXT);
	}
	pucTmp = (const unsigned char *)pText->text;
	while (*pucTmp != '\0') {
		if (*pucTmp < 0x20 || *pucTmp == 0x7f) {
			return pFillError(DRAWFILE_BAD_CHARACTER);
		}
		pucTmp++;
	}
	return NULL;
} /* end of pVerifyText */

/*
 * pVerifyPath - verify a path object
 */
static os_error *
pVerifyPath(const drawfile_path *pPath, int iSize)
{
	const int	*piTmp;
	int	iElements;
	BOOL	bLine;

	bLine = FALSE;
	iElements = (iSize - offsetof(drawfile_path, path)) / 4;

	for (piTmp = pPath->path; piTmp < pPath->path + iElements; piTmp++) {
		switch(*piTmp) {
		case drawfile_PATH_END_PATH:
			if (bLine) {
				return NULL;
			}
			return pFillError(DRAWFILE_PATH_WITHOUT_LINES);
		case drawfile_PATH_LINE_TO:
			bLine = TRUE;
			piTmp += 2;
			break;
		case drawfile_PATH_MOVE_TO:
			piTmp += 2;
			break;
		case drawfile_PATH_CLOSE_LINE:
			bLine = TRUE;
			break;
		default:
			return pFillError(DRAWFILE_BAD_PATH_TYPE);
		}
	}
	return pFillError(DRAWFILE_PATH_WITHOUT_END);
} /* end of pVerifyPath */

/*
 * pVerifySprite - verify a sprite object
 */
static os_error *
pVerifySprite(const drawfile_sprite *pSprite, int iSize)
{
	iSize -= offsetof(drawfile_sprite, header);
	if (iSize < pSprite->header.offset_next) {
		DBG_DEC(iSize);
		DBG_DEC(pSprite->header.offset_next);
		return pFillError(DRAWFILE_BAD_SPRITE_SIZE);
	}
	return NULL;
} /* end of pVerifySprite */

/*
 * pVerifyJpeg - verify a jpeg object
 */
static os_error *
pVerifyJpeg(const drawfile_jpeg *pJpeg, int iSize)
{
	iSize -= offsetof(drawfile_jpeg, data);
	if (iSize < pJpeg->len) {
		DBG_DEC(iSize);
		DBG_DEC(pJpeg->len);
		return pFillError(DRAWFILE_BAD_JPEG_SIZE);
	}
	return NULL;
} /* end of pVerifyJpeg */

/*
 * Drawfile_VerifyDiagram - Verify the diagram generated from the Word file
 *
 * returns NULL if the diagram is correct
 */
os_error *
Drawfile_VerifyDiagram(drawfile_info *pInfo)
{
	drawfile_diagram	*pDiag;
	drawfile_object	*pObj;
	os_error	*pError;
	const char	*pcTmp;
	int		iToGo, iFontTables;
	BOOL		bTypeFontTable;

	TRACE_MSG("Drawfile_VerifyDiagram");

	fail(pInfo == NULL);

  	if (pInfo->length < offsetof(drawfile_diagram, objects)) {
		return pFillError(DRAWFILE_TOO_SMALL);
  	}

	pDiag = (drawfile_diagram *)pInfo->data;
	if (strncmp(pDiag->tag, "Draw", 4) != 0 ||
	    pDiag->major_version != 201 ||
	    pDiag->minor_version != 0) {
		return pFillError(DRAWFILE_NOT_A_DRAWFILE);
	}

	iToGo = pInfo->length - offsetof(drawfile_diagram, objects);
	pcTmp = (const char *)pInfo->data + offsetof(drawfile_diagram, objects);
	iFontTables = 0;
	bTypeFontTable = FALSE;

	while (iToGo > 0) {
		pObj = (drawfile_object *)pcTmp;
		if (pObj->size < 0 || pObj->size % 4 != 0) {
			return pFillError(DRAWFILE_OBJECT_SIZE);
		}
		switch (pObj->type) {
		case drawfile_TYPE_FONT_TABLE:
			if (bTypeFontTable) {
				return pFillError(DRAWFILE_MANY_FONTTABLES);
			}
			bTypeFontTable = TRUE;
			break;
		case drawfile_TYPE_TEXT:
			if (pObj->data.text.style.font_ref != 0 &&
			    !bTypeFontTable) {
				return pFillError(DRAWFILE_TEXT_NO_FONT);
			}
			pError = pVerifyText(&pObj->data.text);
			if (pError != NULL) {
				return pError;
			}
			break;
		case drawfile_TYPE_PATH:
			pError = pVerifyPath(&pObj->data.path,
				pObj->size - offsetof(drawfile_object, data));
			if (pError != NULL) {
				return pError;
			}
			break;
		case drawfile_TYPE_SPRITE:
			pError = pVerifySprite(&pObj->data.sprite,
				pObj->size - offsetof(drawfile_object, data));
			if (pError != NULL) {
				return pError;
			}
			break;
		case drawfile_TYPE_JPEG:
			pError = pVerifyJpeg(&pObj->data.jpeg,
				pObj->size - offsetof(drawfile_object, data));
			if (pError != NULL) {
				return pError;
			}
			break;
		default:
			DBG_DEC(pObj->type);
			return pFillError(DRAWFILE_OBJECT_UNEXPECTED);
		}
		pcTmp += pObj->size;
		iToGo -= pObj->size;
	}
	if (iToGo < 0) {
		return pFillError(DRAWFILE_SIZE_ERROR);
	}
	return NULL;
} /* end of Drawfile_VerifyDiagram */

/*
 * Drawfile_QueryBox - Find the bounding box of a diagram
 */
void
Drawfile_QueryBox(drawfile_info *pInfo, wimp_box *pRect, BOOL bScreenUnits)
{
	fail(pInfo == NULL);
	fail(pRect == NULL);

	Error_CheckFatal(Drawfile_Bbox(0,
		pInfo->data, pInfo->length, NULL, pRect));
	if (bScreenUnits) {
		pRect->min.x = Drawfile_DrawToScreen(pRect->min.x);
		pRect->min.y = Drawfile_DrawToScreen(pRect->min.y);
		pRect->max.x = Drawfile_DrawToScreen(pRect->max.x);
		pRect->max.y = Drawfile_DrawToScreen(pRect->max.y);
	}
} /* end of Drawfile_QueryBox */
