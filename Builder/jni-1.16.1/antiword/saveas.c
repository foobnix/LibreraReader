/*
 * saveas.c
 * Copyright (C) 1998-2001 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to save the results as a textfile or a drawfile
 */

#include <stdio.h>
#include <string.h>
#include "DeskLib:Menu.h"
#include "DeskLib:Save.h"
#include "DeskLib:Template.h"
#include "DeskLib:Window.h"
#include "drawfile.h"
#include "antiword.h"

/* The window handle of the save window */
static window_handle	tSaveWindow = 0;

/* Xfer_send box fields */
#define DRAG_SPRITE	3
#define OK_BUTTON	0
#define CANCEL_BUTTON	(-1)
#define FILENAME_ICON	2


/*
 * saveas - a wrapper around Save_InitSaveWindowhandler
 */
static void
saveas(int iFileType, char *szOutfile, size_t tEstSize,
	save_filesaver save_function, void *pvReference)
{
	TRACE_MSG("saveas");

	if (tSaveWindow == 0) {
		tSaveWindow = Window_Create("xfer_send", template_TITLEMIN);
	}
	Icon_SetText(tSaveWindow, FILENAME_ICON, szOutfile);
	Window_Show(tSaveWindow, open_UNDERPOINTER);
	(void)Save_InitSaveWindowHandler(tSaveWindow, FALSE, TRUE, TRUE,
		DRAG_SPRITE, OK_BUTTON, CANCEL_BUTTON, FILENAME_ICON,
		save_function, NULL, NULL, tEstSize, iFileType, pvReference);
} /* end of saveas */

static BOOL
bWrite2File(void *pvBytes, size_t tSize, FILE *pFile, const char *szFilename)
{
	if (fwrite(pvBytes, sizeof(char), tSize, pFile) != tSize) {
		werr(0, "I can't write to '%s'", szFilename);
		return FALSE;
	}
	return TRUE;
} /* end of bWrite2File */

/*
 * bText2File - Save the generated draw file to a Text file
 */
static BOOL
bText2File(char *szFilename, void *pvHandle)
{
	FILE	*pFile;
	diagram_type	*pDiag;
	drawfile_object	*pObj;
	drawfile_text	*pText;
	const char	*pcTmp;
	int	iToGo, iX, iYtopPrev, iHeight, iLines;
	BOOL	bFirst, bIndent, bSuccess;

	TRACE_MSG("bText2File");

	fail(szFilename == NULL || szFilename[0] == '\0');
	fail(pvHandle == NULL);

	DBG_MSG(szFilename);

	pDiag = (diagram_type *)pvHandle;
	pFile = fopen(szFilename, "w");
	if (pFile == NULL) {
		werr(0, "I can't open '%s' for writing", szFilename);
		return FALSE;
	}
	bFirst = TRUE;
	iYtopPrev = 0;
	iHeight = (int)lWord2DrawUnits20(DEFAULT_FONT_SIZE);
	bSuccess = TRUE;
	fail(pDiag->tInfo.length < offsetof(drawfile_diagram, objects));
	iToGo = pDiag->tInfo.length - offsetof(drawfile_diagram, objects);
	DBG_DEC(iToGo);
	pcTmp = (const char *)pDiag->tInfo.data +
				offsetof(drawfile_diagram, objects);
	while (iToGo > 0 && bSuccess) {
		pObj = (drawfile_object *)pcTmp;
		switch (pObj->type) {
		case drawfile_TYPE_TEXT:
			pText = &pObj->data.text;
			/* Compute the number of lines */
			iLines = (iYtopPrev - pText->bbox.max.y +
					iHeight / 2) / iHeight;
			DBG_DEC_C(iLines < 0, iYtopPrev);
			DBG_DEC_C(iLines < 0, pText->bbox.max.y);
			fail(iLines < 0);
			bIndent = iLines > 0 || bFirst;
			bFirst = FALSE;
			/* Print the newlines */
			while (iLines > 0 && bSuccess) {
				bSuccess = bWrite2File("\n",
					1, pFile, szFilename);
				iLines--;
			}
			/* Print the indentation */
			if (bIndent && bSuccess) {
				for (iX = Drawfile_ScreenToDraw(8);
				     iX <= pText->bbox.min.x && bSuccess;
				     iX += Drawfile_ScreenToDraw(16)) {
					bSuccess = bWrite2File(" ",
						1, pFile, szFilename);
				}
			}
			if (!bSuccess) {
				break;
			}
			/* Print the text object */
			bSuccess = bWrite2File(pText->text,
				strlen(pText->text), pFile, szFilename);
			/* Setup for the next object */
			iYtopPrev = pText->bbox.max.y;
			iHeight = pText->bbox.max.y - pText->bbox.min.y;
			break;
		case drawfile_TYPE_FONT_TABLE:
		case drawfile_TYPE_PATH:
		case drawfile_TYPE_SPRITE:
		case drawfile_TYPE_JPEG:
			/* These are not relevant in a textfile */
			break;
		default:
			DBG_DEC(pObj->type);
			bSuccess = FALSE;
			break;
		}
		pcTmp += pObj->size;
		iToGo -= pObj->size;
	}
	DBG_DEC_C(iToGo != 0, iToGo);
	if (bSuccess) {
		bSuccess = bWrite2File("\n", 1, pFile, szFilename);
	}
	(void)fclose(pFile);
	if (bSuccess) {
		vSetFiletype(szFilename, FILETYPE_TEXT);
	} else {
		(void)remove(szFilename);
		werr(0, "Unable to save textfile '%s'", szFilename);
	}
	return bSuccess;
} /* end of bText2File */

/*
 * bSaveTextfile - save the diagram as a text file
 */
BOOL
bSaveTextfile(event_pollblock *pEvent, void *pvReference)
{
	diagram_type	*pDiag;
	size_t	tRecLen, tNbrRecs, tEstSize;

	TRACE_MSG("bSaveTextfile");

	fail(pEvent == NULL);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	switch (pEvent->type) {
	case event_SEND:	/* From a menu */
		fail(pEvent->data.message.header.action != message_MENUWARN);
		if (menu_currentopen != pDiag->pSaveMenu ||
		    pEvent->data.message.data.menuwarn.selection[0] !=
							SAVEMENU_SAVETEXT) {
			return FALSE;
		}
		break;
	case event_KEY:		/* From a key short cut */
		if (pEvent->data.key.caret.window != pDiag->tMainWindow) {
			return FALSE;
		}
		break;
	default:
		DBG_DEC(pEvent->type);
		return FALSE;
	}

	tRecLen = sizeof(drawfile_text) + DEFAULT_SCREEN_WIDTH * 2 / 3;
	tNbrRecs = pDiag->tInfo.length / tRecLen + 1;
	tEstSize = tNbrRecs * DEFAULT_SCREEN_WIDTH * 2 / 3;
	DBG_DEC(tEstSize);

	saveas(FILETYPE_TEXT, "WordText", tEstSize, bText2File, pDiag);
	return TRUE;
} /* end of bSaveTextfile */

/*
 * bDraw2File - Save the generated draw file to a Draw file
 *
 * Remark: This is not a simple copy action. The origin of the
 * coordinates (0,0) must move from the top-left corner to the
 * bottom-left corner.
 */
static BOOL
bDraw2File(char *szFilename, void *pvHandle)
{
	FILE		*pFile;
	diagram_type	*pDiagram;
	wimp_box	*pBbox;
	drawfile_object	*pObj;
	drawfile_text	*pText;
	drawfile_path	*pPath;
	drawfile_sprite	*pSprite;
	drawfile_jpeg	*pJpeg;
	int	*piPath;
	char	*pcTmp;
	int	iYadd, iToGo, iSize;
	BOOL	bSuccess;

	TRACE_MSG("bDraw2File");

	fail(szFilename == NULL || szFilename[0] == '\0');
	fail(pvHandle == NULL);

	NO_DBG_MSG(szFilename);

	pDiagram = (diagram_type *)pvHandle;
	pFile = fopen(szFilename, "wb");
	if (pFile == NULL) {
		werr(0, "I can't open '%s' for writing", szFilename);
		return FALSE;
	}
	iToGo = pDiagram->tInfo.length;
	DBG_DEC(iToGo);
	pcTmp = pDiagram->tInfo.data;
	bSuccess = bWrite2File(pcTmp,
			offsetof(drawfile_diagram, bbox), pFile, szFilename);
	if (bSuccess) {
	  	pcTmp += offsetof(drawfile_diagram, bbox);
		iToGo -= offsetof(drawfile_diagram, bbox);
		pBbox = (wimp_box *)pcTmp;
		iYadd = -pBbox->min.y;
		pBbox->min.y += iYadd;
		pBbox->max.y += iYadd;
		bSuccess = bWrite2File(pcTmp,
				sizeof(*pBbox), pFile, szFilename);
		iToGo -= sizeof(*pBbox);
		DBG_DEC(iToGo);
		pcTmp += sizeof(*pBbox);
	} else {
		iYadd = 0;
	}
	while (iToGo > 0 && bSuccess) {
		pObj = (drawfile_object *)pcTmp;
		iSize = pObj->size;
		switch (pObj->type) {
		case drawfile_TYPE_FONT_TABLE:
			bSuccess = bWrite2File(pcTmp,
					iSize, pFile, szFilename);
			pcTmp += iSize;
			iToGo -= iSize;
			break;
		case drawfile_TYPE_TEXT:
			pText = &pObj->data.text;
			/* First correct the coordinates */
			pText->bbox.min.y += iYadd;
			pText->bbox.max.y += iYadd;
			pText->base.y += iYadd;
			/* Now write the information to file */
			bSuccess = bWrite2File(pcTmp,
					iSize, pFile, szFilename);
			pcTmp += pObj->size;
			iToGo -= pObj->size;
			break;
		case drawfile_TYPE_PATH:
			pPath = &pObj->data.path;
			/* First correct the coordinates */
			pPath->bbox.min.y += iYadd;
			pPath->bbox.max.y += iYadd;
			/* Now write the information to file */
			bSuccess = bWrite2File(pPath,
				sizeof(*pPath), pFile, szFilename);
			pcTmp += sizeof(*pPath);
			iSize = pObj->size - sizeof(*pPath);
			fail(iSize < 14 * sizeof(int));
			/* Second correct the path coordinates */
			piPath = xmalloc(iSize);
			memcpy(piPath, pcTmp, iSize);
			piPath[ 2] += iYadd;
			piPath[ 5] += iYadd;
			piPath[ 8] += iYadd;
			piPath[11] += iYadd;
			if (bSuccess) {
				bSuccess = bWrite2File(piPath,
					iSize, pFile, szFilename);
				pcTmp += iSize;
			}
			piPath = xfree(piPath);
			iToGo -= pObj->size;
			break;
		case drawfile_TYPE_SPRITE:
			pSprite = &pObj->data.sprite;
			/* First correct the coordinates */
			pSprite->bbox.min.y += iYadd;
			pSprite->bbox.max.y += iYadd;
			/* Now write the information to file */
			bSuccess = bWrite2File(pcTmp,
					iSize, pFile, szFilename);
			pcTmp += pObj->size;
			iToGo -= pObj->size;
			break;
		case drawfile_TYPE_JPEG:
			pJpeg = &pObj->data.jpeg;
			/* First correct the coordinates */
			pJpeg->bbox.min.y += iYadd;
			pJpeg->bbox.max.y += iYadd;
			pJpeg->trfm.entries[2][1] += iYadd;
			/* Now write the information to file */
			bSuccess = bWrite2File(pcTmp,
					iSize, pFile, szFilename);
			pcTmp += pObj->size;
			iToGo -= pObj->size;
			break;
		default:
			DBG_DEC(pObj->type);
			bSuccess = FALSE;
			break;
		}
	}
	DBG_DEC_C(iToGo != 0, iToGo);
	(void)fclose(pFile);
	if (bSuccess) {
		vSetFiletype(szFilename, FILETYPE_DRAW);
	} else {
		(void)remove(szFilename);
		werr(0, "Unable to save drawfile '%s'", szFilename);
	}
	return bSuccess;
} /* end of bDraw2File */

/*
 * bSaveDrawfile - save the diagram as a draw file
 */
BOOL
bSaveDrawfile(event_pollblock *pEvent, void *pvReference)
{
	diagram_type	*pDiag;
	size_t		tEstSize;

	TRACE_MSG("bSaveDrawfile");

	fail(pEvent == NULL);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	switch (pEvent->type) {
	case event_SEND:	/* From a menu */
		fail(pEvent->data.message.header.action != message_MENUWARN);
		if (menu_currentopen != pDiag->pSaveMenu ||
		    pEvent->data.message.data.menuwarn.selection[0] !=
							SAVEMENU_SAVEDRAW) {
			return FALSE;
		}
		break;
	case event_KEY:		/* From a key short cut */
		if (pEvent->data.key.caret.window != pDiag->tMainWindow) {
			return FALSE;
		}
		break;
	default:
		DBG_DEC(pEvent->type);
		return FALSE;
	}

	tEstSize = pDiag->tInfo.length;
	DBG_DEC(tEstSize);

	saveas(FILETYPE_DRAW, "WordDraw", tEstSize, bDraw2File, pDiag);
	return TRUE;
} /* end of bSaveDrawfile */
