/*
 * draw.c
 * Copyright (C) 1998-2005 A.J. van Os; Released under GPL
 *
 * Description:
 * Functions to deal with the Draw format
 */

#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include "DeskLib:KeyCodes.h"
#include "DeskLib:Error.h"
#include "DeskLib:Menu.h"
#include "DeskLib:Template.h"
#include "DeskLib:Window.h"
#include "DeskLib:EventMsg.h"
#include "flexlib:flex.h"
#include "drawfile.h"
#include "antiword.h"

/* The work area must be a little bit larger than the diagram */
#define WORKAREA_EXTENSION	    5
/* Diagram memory */
#define INITIAL_SIZE		32768	/* 32k */
#define EXTENSION_SIZE		 4096	/*  4k */
/* Main window title */
#define WINDOW_TITLE_LEN	   28
#define FILENAME_TITLE_LEN	(WINDOW_TITLE_LEN - 10)


#if !defined(__GNUC__)
int
flex_alloc(flex_ptr anchor, int n)
{
	void	*pvTmp;

	TRACE_MSG("flex_alloc");

	if (anchor == NULL || n < 0) {
		return 0;
	}
	if (n == 0) {
		n = 1;
	}
	pvTmp = malloc(n);
	if (pvTmp == NULL) {
		return 0;
	}
	*anchor = pvTmp;
	return 1;
} /* end of flex_alloc */

void
flex_free(flex_ptr anchor)
{
	TRACE_MSG("flex_free");

	if (anchor == NULL || *anchor == NULL) {
		return;
	}
	free(*anchor);
	*anchor = NULL;
} /* end of flex_free */

int
flex_extend(flex_ptr anchor, int newsize)
{
	void	*pvTmp;

	TRACE_MSG("flex_extend");

	if (anchor == NULL || newsize < 0) {
		return 0;
	}
	if (newsize == 0) {
		newsize = 1;
	}
	pvTmp = realloc(*anchor, newsize);
	if (pvTmp == NULL) {
		return 0;
	}
	*anchor = pvTmp;
	return 1;
} /* end of flex_extend */
#endif /* !__GNUC__ */

/*
 * vCreateMainWindow - create the Main window
 *
 * remark: does not return if the Main window can't be created
 */
static window_handle
tCreateMainWindow(void)
{
	window_handle	tMainWindow;

	TRACE_MSG("tCreateMainWindow");

	tMainWindow = Window_Create("MainWindow", template_TITLEMIN);
	if (tMainWindow == 0) {
		werr(1, "I can't find the 'MainWindow' template");
	}
	return tMainWindow;
} /* end of tCreateMainWindow */

/*
 * vCreateScaleWindow - create the Scale view window
 *
 * remark: does not return if the Scale view window can't be created
 */
static window_handle
tCreateScaleWindow(void)
{
	window_handle	tScaleWindow;

	TRACE_MSG("tCreateScaleWindow");

	tScaleWindow = Window_Create("ScaleView", template_TITLEMIN);
	if (tScaleWindow == 0) {
		werr(1, "I can't find the 'ScaleView' template");
	}
	return tScaleWindow;
} /* end of tCreateScaleWindow */

/*
 * pCreateDiagram - create and initialize a diagram
 *
 * remark: does not return if the diagram can't be created
 */
diagram_type *
pCreateDiagram(const char *szTask, const char *szFilename)
{
	diagram_type	*pDiag;
	options_type	tOptions;
	window_handle	tMainWindow, tScaleWindow;
	wimp_box	tBox;

	TRACE_MSG("pCreateDiagram");

	fail(szTask == NULL || szTask[0] == '\0');

	/* Create the main window */
	tMainWindow = tCreateMainWindow();

	/* Create the scale view window */
	tScaleWindow = tCreateScaleWindow();

	/* Get the necessary memory */
	pDiag = xmalloc(sizeof(diagram_type));
	if (flex_alloc((flex_ptr)&pDiag->tInfo.data, INITIAL_SIZE) != 1) {
		werr(1, "Memory allocation failed, unable to continue");
	}

	/* Initialize the diagram */
	vGetOptions(&tOptions);
	pDiag->tMainWindow = tMainWindow;
	pDiag->tScaleWindow = tScaleWindow;
	pDiag->iScaleFactorCurr = tOptions.iScaleFactor;
	pDiag->iScaleFactorTemp = tOptions.iScaleFactor;
	pDiag->tMemorySize = INITIAL_SIZE;
	tBox.min.x = 0;
	tBox.min.y = -(Drawfile_ScreenToDraw(32 + 3) * 8 + 1);
	tBox.max.x = Drawfile_ScreenToDraw(16) * MIN_SCREEN_WIDTH + 1;
	tBox.max.y = 0;
	Error_CheckFatal(Drawfile_CreateDiagram(&pDiag->tInfo,
					pDiag->tMemorySize, szTask, tBox));
	DBG_DEC(pDiag->tInfo.length);
	pDiag->lXleft = 0;
	pDiag->lYtop = 0;
	strncpy(pDiag->szFilename,
			szBasename(szFilename), sizeof(pDiag->szFilename) - 1);
	pDiag->szFilename[sizeof(pDiag->szFilename) - 1] = '\0';
	/* Return success */
	return pDiag;
} /* end of pCreateDiagram */

/*
 * bDestroyDiagram - remove a diagram by freeing the memory it uses
 */
BOOL
bDestroyDiagram(event_pollblock *pEvent, void *pvReference)
{
	diagram_type	*pDiag;
	window_handle	tWindow;

	TRACE_MSG("bDestroyDiagram");

	fail(pEvent == NULL);
	fail(pvReference == NULL);

	if (pEvent == NULL || pvReference == NULL) {
		return FALSE;
	}

	pDiag = (diagram_type *)pvReference;

	switch (pEvent->type) {
	case event_CLOSE:
		tWindow = pEvent->data.openblock.window;
		break;
	case event_KEY:
		tWindow = pEvent->data.key.caret.window;
		break;
	default:
		DBG_DEC(pEvent->type);
		return FALSE;
	}
	if (tWindow != pDiag->tMainWindow) {
		return FALSE;
	}

	/* Delete the main window */
	Window_Delete(pDiag->tMainWindow);
	pDiag->tMainWindow = 0;

	/* Delete the scale window */
	Window_Delete(pDiag->tScaleWindow);
	pDiag->tScaleWindow = 0;

#if defined(__GNUC__)
	/*
	 * Remove all references to the diagram that will be free-ed
	 * by undoing the EventMsg_Claim's from within the Menu_Warn's
	 */
	while (EventMsg_ReleaseSpecific(message_MENUWARNING, window_ANY,
					bSaveTextfile, pDiag))
		; /* EMPTY */
	while (EventMsg_ReleaseSpecific(message_MENUWARNING, window_ANY,
					bSaveDrawfile, pDiag))
		; /* EMPTY */
	while (EventMsg_ReleaseSpecific(message_MENUWARNING, window_ANY,
					bScaleOpenAction, pDiag))
		; /* EMPTY */
#endif /* __GNUC__ */

	/* Free the memory */
	if (pDiag->tInfo.data != NULL && pDiag->tMemorySize != 0) {
		flex_free((flex_ptr)&pDiag->tInfo.data);
	}
	/* Just to be on the save side */
	pDiag->tInfo.data = NULL;
	pDiag->tInfo.length = 0;
	pDiag->tMemorySize = 0;

	/* Destroy the diagram itself */
	pDiag = xfree(pDiag);
	return TRUE;
} /* end of bDestroyDiagram */

/*
 * vExtendDiagramSize - make sure the diagram is big enough
 */
static void
vExtendDiagramSize(diagram_type *pDiag, size_t tSize)
{
	TRACE_MSG("vExtendDiagramSize");

	fail(pDiag == NULL || tSize % 4 != 0);

	while (pDiag->tInfo.length + tSize > pDiag->tMemorySize) {
		if (flex_extend((flex_ptr)&pDiag->tInfo.data,
				pDiag->tMemorySize + EXTENSION_SIZE) != 1) {
			werr(1, "Memory extend failed, unable to continue");
		}
		pDiag->tMemorySize += EXTENSION_SIZE;
		NO_DBG_DEC(pDiag->tMemorySize);
	}
	TRACE_MSG("end of vExtendDiagramSize");
} /* end of vExtendDiagramSize */

/*
 * vPrologue2 - prologue part 2; add a font list to a diagram
 */
void
vPrologue2(diagram_type *pDiag, int iWordVersion)
{
	drawfile_object	*pNew;
	const font_table_type	*pTmp;
	char	*pcTmp;
	size_t	tRealSize, tSize;
	int	iCount;

	TRACE_MSG("vPrologue2");

	fail(pDiag == NULL);

	if (tGetFontTableLength() == 0) {
		return;
	}
	tRealSize = offsetof(drawfile_object, data);
	pTmp = NULL;
	while ((pTmp = pGetNextFontTableRecord(pTmp)) != NULL) {
		tRealSize += 2 + strlen(pTmp->szOurFontname);
	}
	DBG_DEC(tRealSize);
	tSize = ROUND4(tRealSize);
	vExtendDiagramSize(pDiag, tSize);
	pNew = xmalloc(tSize);
	memset(pNew, 0, tSize);
	pNew->type = drawfile_TYPE_FONT_TABLE;
	pNew->size = tSize;
	pcTmp = (char *)&pNew->data.font_table.font_def[0].font_ref;
	iCount = 0;
	pTmp = NULL;
	while ((pTmp = pGetNextFontTableRecord(pTmp)) != NULL) {
		*pcTmp = ++iCount;
		pcTmp++;
		strcpy(pcTmp, pTmp->szOurFontname);
		pcTmp += 1 + strlen(pTmp->szOurFontname);
	}
	Error_CheckFatal(Drawfile_AppendObject(&pDiag->tInfo,
			pDiag->tMemorySize, pNew, TRUE));
	pNew = xfree(pNew);
} /* end of vPrologue2 */

/*
 * vSubstring2Diagram - put a sub string into a diagram
 */
void
vSubstring2Diagram(diagram_type *pDiag,
	char *szString, size_t tStringLength, long lStringWidth,
	UCHAR ucFontColor, USHORT usFontstyle, drawfile_fontref tFontRef,
	USHORT usFontSize, USHORT usMaxFontSize)
{
	drawfile_object	*pNew;
	long	lSizeX, lSizeY, lOffset, l20, lYMove;
	size_t	tRealSize, tSize;

	TRACE_MSG("vSubstring2Diagram");

	fail(pDiag == NULL || szString == NULL);
	fail(pDiag->lXleft < 0);
	fail(tStringLength != strlen(szString));
	fail(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);
	fail(usMaxFontSize < MIN_FONT_SIZE || usMaxFontSize > MAX_FONT_SIZE);
	fail(usFontSize > usMaxFontSize);

	if (szString[0] == '\0' || tStringLength == 0) {
		return;
	}

	if (tFontRef == 0) {
		lOffset = Drawfile_ScreenToDraw(2);
		l20 = Drawfile_ScreenToDraw(32 + 3);
		lSizeX = Drawfile_ScreenToDraw(16);
		lSizeY = Drawfile_ScreenToDraw(32);
	} else {
		lOffset = lToBaseLine(usMaxFontSize);
		l20 = lWord2DrawUnits20(usMaxFontSize);
		lSizeX = lWord2DrawUnits00(usFontSize);
		lSizeY = lWord2DrawUnits00(usFontSize);
	}

	lYMove = 0;

	/* Up for superscript */
	if (bIsSuperscript(usFontstyle)) {
		lYMove = lMilliPoints2DrawUnits((((long)usFontSize + 1) / 2) * 375);
	}
	/* Down for subscript */
	if (bIsSubscript(usFontstyle)) {
		lYMove = -lMilliPoints2DrawUnits((long)usFontSize * 125);
	}

	tRealSize = offsetof(drawfile_object, data);
	tRealSize += sizeof(drawfile_text) + tStringLength;
	tSize = ROUND4(tRealSize);
	vExtendDiagramSize(pDiag, tSize);
	pNew = xmalloc(tSize);
	memset(pNew, 0, tSize);
	pNew->type = drawfile_TYPE_TEXT;
	pNew->size = tSize;
	pNew->data.text.bbox.min.x = (int)pDiag->lXleft;
	pNew->data.text.bbox.min.y = (int)(pDiag->lYtop + lYMove);
	pNew->data.text.bbox.max.x = (int)(pDiag->lXleft + lStringWidth);
	pNew->data.text.bbox.max.y = (int)(pDiag->lYtop + l20 + lYMove);
	pNew->data.text.fill.value = (int)ulColor2Color(ucFontColor);
	pNew->data.text.bg_hint.value = 0xffffff00;	/* White */
	pNew->data.text.style.font_ref = tFontRef;
	pNew->data.text.style.reserved[0] = 0;
	pNew->data.text.style.reserved[1] = 0;
	pNew->data.text.style.reserved[2] = 0;
	pNew->data.text.xsize = (int)lSizeX;
	pNew->data.text.ysize = (int)lSizeY;
	pNew->data.text.base.x = (int)pDiag->lXleft;
	pNew->data.text.base.y = (int)(pDiag->lYtop + lOffset + lYMove);
	strncpy(pNew->data.text.text, szString, tStringLength);
	pNew->data.text.text[tStringLength] = '\0';
	Error_CheckFatal(Drawfile_AppendObject(&pDiag->tInfo,
			pDiag->tMemorySize, pNew, TRUE));
	pNew = xfree(pNew);
	/*draw_translateText(&pDiag->tInfo);*/
	pDiag->lXleft += lStringWidth;
	TRACE_MSG("leaving vSubstring2Diagram");
} /* end of vSubstring2Diagram */

/*
 * vImage2Diagram - put an image into a diagram
 */
void
vImage2Diagram(diagram_type *pDiag, const imagedata_type *pImg,
	UCHAR *pucImage, size_t tImageSize)
{
  	drawfile_object	*pNew;
	long	lWidth, lHeight;
	size_t	tRealSize, tSize;

	TRACE_MSG("vImage2Diagram");

	fail(pDiag == NULL);
	fail(pImg == NULL);
	fail(pDiag->lXleft < 0);
	fail(pImg->eImageType != imagetype_is_dib &&
	     pImg->eImageType != imagetype_is_jpeg);

	DBG_DEC_C(pDiag->lXleft != 0, pDiag->lXleft);

	lWidth = lPoints2DrawUnits(pImg->iHorSizeScaled);
	lHeight = lPoints2DrawUnits(pImg->iVerSizeScaled);
	DBG_DEC(lWidth);
	DBG_DEC(lHeight);

	pDiag->lYtop -= lHeight;

	tRealSize = offsetof(drawfile_object, data);
	switch (pImg->eImageType) {
	case imagetype_is_dib:
		tRealSize += sizeof(drawfile_sprite) + tImageSize;
		tSize = ROUND4(tRealSize);
		vExtendDiagramSize(pDiag, tSize);
		pNew = xmalloc(tSize);
		memset(pNew, 0, tSize);
		pNew->type = drawfile_TYPE_SPRITE;
		pNew->size = tSize;
		pNew->data.sprite.bbox.min.x = (int)pDiag->lXleft;
		pNew->data.sprite.bbox.min.y = (int)pDiag->lYtop;
		pNew->data.sprite.bbox.max.x = (int)(pDiag->lXleft + lWidth);
		pNew->data.sprite.bbox.max.y = (int)(pDiag->lYtop + lHeight);
		memcpy(&pNew->data.sprite.header, pucImage, tImageSize);
		break;
	case imagetype_is_jpeg:
#if defined(DEBUG)
		(void)bGetJpegInfo(pucImage, tImageSize);
#endif /* DEBUG */
		tRealSize += sizeof(drawfile_jpeg) + tImageSize;
		tSize = ROUND4(tRealSize);
		vExtendDiagramSize(pDiag, tSize);
		pNew = xmalloc(tSize);
		memset(pNew, 0, tSize);
		pNew->type = drawfile_TYPE_JPEG;
		pNew->size = tSize;
		pNew->data.jpeg.bbox.min.x = (int)pDiag->lXleft;
		pNew->data.jpeg.bbox.min.y = (int)pDiag->lYtop;
		pNew->data.jpeg.bbox.max.x = (int)(pDiag->lXleft + lWidth);
		pNew->data.jpeg.bbox.max.y = (int)(pDiag->lYtop + lHeight);
		pNew->data.jpeg.width = (int)lWidth;
		pNew->data.jpeg.height = (int)lHeight;
		pNew->data.jpeg.xdpi = 90;
		pNew->data.jpeg.ydpi = 90;
		pNew->data.jpeg.trfm.entries[0][0] = 0x10000;
		pNew->data.jpeg.trfm.entries[0][1] = 0;
		pNew->data.jpeg.trfm.entries[1][0] = 0;
		pNew->data.jpeg.trfm.entries[1][1] = 0x10000;
		pNew->data.jpeg.trfm.entries[2][0] = (int)pDiag->lXleft;
		pNew->data.jpeg.trfm.entries[2][1] = (int)pDiag->lYtop;
		pNew->data.jpeg.len = tImageSize;
		memcpy(pNew->data.jpeg.data, pucImage, tImageSize);
		break;
	default:
		DBG_DEC(pImg->eImageType);
		pNew = NULL;
		break;
	}

	Error_CheckFatal(Drawfile_AppendObject(&pDiag->tInfo,
					pDiag->tMemorySize, pNew, TRUE));
	pNew = xfree(pNew);
	pDiag->lXleft = 0;
} /* end of vImage2Diagram */

/*
 * bAddDummyImage - add a dummy image
 *
 * return TRUE when successful, otherwise FALSE
 */
BOOL
bAddDummyImage(diagram_type *pDiag, const imagedata_type *pImg)
{
  	drawfile_object	*pNew;
	int	*piTmp;
	long	lWidth, lHeight;
	size_t	tRealSize, tSize;

	TRACE_MSG("bAddDummyImage");

	fail(pDiag == NULL);
	fail(pImg == NULL);
	fail(pDiag->lXleft < 0);

	if (pImg->iVerSizeScaled <= 0 || pImg->iHorSizeScaled <= 0) {
		return FALSE;
	}

	DBG_DEC_C(pDiag->lXleft != 0, pDiag->lXleft);

	lWidth = lPoints2DrawUnits(pImg->iHorSizeScaled);
	lHeight = lPoints2DrawUnits(pImg->iVerSizeScaled);

	pDiag->lYtop -= lHeight;

	tRealSize = offsetof(drawfile_object, data);
	tRealSize += sizeof(drawfile_path) + (14 - 1) * sizeof(int);
	tSize = ROUND4(tRealSize);
	vExtendDiagramSize(pDiag, tSize);
	pNew = xmalloc(tSize);
	memset(pNew, 0, tSize);
	pNew->type = drawfile_TYPE_PATH;
	pNew->size = tSize;
	pNew->data.path.bbox.min.x = (int)pDiag->lXleft;
	pNew->data.path.bbox.min.y = (int)pDiag->lYtop;
	pNew->data.path.bbox.max.x = (int)(pDiag->lXleft + lWidth);
	pNew->data.path.bbox.max.y = (int)(pDiag->lYtop + lHeight);
	pNew->data.path.fill.value = -1;
	pNew->data.path.outline.value = 0x4d4d4d00;	/* Gray 70 percent */
	pNew->data.path.width = (int)lMilliPoints2DrawUnits(500);
	pNew->data.path.style.flags = 0;
	pNew->data.path.style.reserved = 0;
	pNew->data.path.style.cap_width = 0;
	pNew->data.path.style.cap_length = 0;
	piTmp = pNew->data.path.path;
	*piTmp++ = drawfile_PATH_MOVE_TO;
	*piTmp++ = pNew->data.path.bbox.min.x;
	*piTmp++ = pNew->data.path.bbox.min.y;
	*piTmp++ = drawfile_PATH_LINE_TO;
	*piTmp++ = pNew->data.path.bbox.min.x;
	*piTmp++ = pNew->data.path.bbox.max.y;
	*piTmp++ = drawfile_PATH_LINE_TO;
	*piTmp++ = pNew->data.path.bbox.max.x;
	*piTmp++ = pNew->data.path.bbox.max.y;
	*piTmp++ = drawfile_PATH_LINE_TO;
	*piTmp++ = pNew->data.path.bbox.max.x;
	*piTmp++ = pNew->data.path.bbox.min.y;
	*piTmp++ = drawfile_PATH_CLOSE_LINE;
	*piTmp++ = drawfile_PATH_END_PATH;

	Error_CheckFatal(Drawfile_AppendObject(&pDiag->tInfo,
					pDiag->tMemorySize, pNew, TRUE));
	pNew = xfree(pNew);
	pDiag->lXleft = 0;
	return TRUE;
} /* end of bAddDummyImage */

/*
 * vMove2NextLine - move to the next line
 */
void
vMove2NextLine(diagram_type *pDiag, drawfile_fontref tFontRef,
	USHORT usFontSize)
{
	long	l20;

	TRACE_MSG("vMove2NextLine");

	fail(pDiag == NULL);
	fail(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);

	if (tFontRef == 0) {
		l20 = Drawfile_ScreenToDraw(32 + 3);
	} else {
		l20 = lWord2DrawUnits20(usFontSize);
	}
	pDiag->lYtop -= l20;
} /* end of vMove2NextLine */

/*
 * Create an start of paragraph (Phase 1)
 */
void
vStartOfParagraph1(diagram_type *pDiag, long lBeforeIndentation)
{
	TRACE_MSG("vStartOfParagraph1");

	fail(pDiag == NULL);
	fail(lBeforeIndentation < 0);

	pDiag->lXleft = 0;
	pDiag->lYtop -= lMilliPoints2DrawUnits(lBeforeIndentation);
} /* end of vStartOfParagraph1 */

/*
 * Create an start of paragraph (Phase 2)
 * DUMMY function
 */
void
vStartOfParagraph2(diagram_type *pDiag)
{
	TRACE_MSG("vStartOfParagraph2");
} /* end of vStartOfParagraph2 */

/*
 * Create an end of paragraph
 */
void
vEndOfParagraph(diagram_type *pDiag,
	drawfile_fontref tFontRef, USHORT usFontSize, long lAfterIndentation)
{
	TRACE_MSG("vEndOfParagraph");

	fail(pDiag == NULL);
	fail(usFontSize < MIN_FONT_SIZE || usFontSize > MAX_FONT_SIZE);
	fail(lAfterIndentation < 0);

	pDiag->lXleft = 0;
	pDiag->lYtop -= lMilliPoints2DrawUnits(lAfterIndentation);
} /* end of vEndOfParagraph */

/*
 * Create an end of page
 */
void
vEndOfPage(diagram_type *pDiag, long lAfterIndentation, BOOL bNewSection)
{
	TRACE_MSG("vEndOfPage");

	fail(pDiag == NULL);
	fail(lAfterIndentation < 0);

	pDiag->lXleft = 0;
	pDiag->lYtop -= lMilliPoints2DrawUnits(lAfterIndentation);
} /* end of vEndOfPage */

/*
 * vSetHeaders - set the headers
 * DUMMY function
 */
void
vSetHeaders(diagram_type *pDiag, USHORT usIstd)
{
	TRACE_MSG("vSetHeaders");
} /* end of vSetHeaders */

/*
 * Create a start of list
 * DUMMY function
 */
void
vStartOfList(diagram_type *pDiag, UCHAR ucNFC, BOOL bIsEndOfTable)
{
	TRACE_MSG("vStartOfList");
} /* end of vStartOfList */

/*
 * Create an end of list
 * DUMMY function
 */
void
vEndOfList(diagram_type *pDiag)
{
	TRACE_MSG("vEndOfList");
} /* end of vEndOfList */

/*
 * Create a start of a list item
 * DUMMY function
 */
void
vStartOfListItem(diagram_type *pDiag, BOOL bNoMarks)
{
	TRACE_MSG("vStartOfListItem");
} /* end of vStartOfListItem */

/*
 * Create an end of a table
 * DUMMY function
 */
void
vEndOfTable(diagram_type *pDiag)
{
	TRACE_MSG("vEndOfTable");
} /* end of vEndTable */

/*
 * Add a table row
 * DUMMY function
 *
 * Returns TRUE when conversion type is XML
 */
BOOL
bAddTableRow(diagram_type *pDiag, char **aszColTxt,
	int iNbrOfColumns, const short *asColumnWidth, UCHAR ucBorderInfo)
{
	TRACE_MSG("bAddTableRow");

	return FALSE;
} /* end of bAddTableRow */

/*
 * vForceRedraw - force a redraw of the main window
 */
static void
vForceRedraw(diagram_type *pDiag)
{
	window_state		tWindowState;
	window_redrawblock	tRedraw;
	int	x0, y0, x1, y1;

	TRACE_MSG("vForceRedraw");

	fail(pDiag == NULL);

	DBG_DEC(pDiag->iScaleFactorCurr);

	/* Read the size of the current diagram */
	Drawfile_QueryBox(&pDiag->tInfo, &tRedraw.rect, TRUE);
	/* Adjust the size of the work area */
	x0 = tRedraw.rect.min.x * pDiag->iScaleFactorCurr / 100 - 1;
	y0 = tRedraw.rect.min.y * pDiag->iScaleFactorCurr / 100 - 1;
	x1 = tRedraw.rect.max.x * pDiag->iScaleFactorCurr / 100 + 1;
	y1 = tRedraw.rect.max.y * pDiag->iScaleFactorCurr / 100 + 1;
	/* Work area extension */
	x0 -= WORKAREA_EXTENSION;
	y0 -= WORKAREA_EXTENSION;
	x1 += WORKAREA_EXTENSION;
	y1 += WORKAREA_EXTENSION;
	Window_SetExtent(pDiag->tMainWindow, x0, y0, x1, y1);
	/* Widen the box slightly to be sure all the edges are drawn */
	x0 -= 5;
	y0 -= 5;
	x1 += 5;
	y1 += 5;
	/* Force the redraw */
	Window_ForceRedraw(pDiag->tMainWindow, x0, y0, x1, y1);
	/* Reopen the window to show the correct size */
	Error_CheckFatal(Wimp_GetWindowState(pDiag->tMainWindow, &tWindowState));
	tWindowState.openblock.behind = -1;
	Error_CheckFatal(Wimp_OpenWindow(&tWindowState.openblock));
} /* end of vForceRedraw */

/*
 * vShowDiagram - put the diagram on the screen
 */
void
vShowDiagram(diagram_type *pDiag)
{
	wimp_box	tRect;
	int	x0, y0, x1, y1;

	TRACE_MSG("vShowDiagram");

	fail(pDiag == NULL);

	Window_Show(pDiag->tMainWindow, open_NEARLAST);
	Drawfile_QueryBox(&pDiag->tInfo, &tRect, TRUE);
	/* Work area extension */
	x0 = tRect.min.x - WORKAREA_EXTENSION;
	y0 = tRect.min.y - WORKAREA_EXTENSION;
	x1 = tRect.max.x + WORKAREA_EXTENSION;
	y1 = tRect.max.y + WORKAREA_EXTENSION;
	Window_SetExtent(pDiag->tMainWindow, x0, y0, x1, y1);
	vForceRedraw(pDiag);
} /* end of vShowDiagram */

/*
 * vMainButtonClick - handle mouse buttons clicks for the main screen
 */
void
vMainButtonClick(mouse_block *pMouse)
{
	caret_block	tCaret;
	window_state	ws;

	TRACE_MSG("vMainButtonClick");

	fail(pMouse == NULL);

	DBG_DEC(pMouse->button.data.select);
	DBG_DEC(pMouse->button.data.adjust);
	DBG_DEC(pMouse->window);
	DBG_DEC(pMouse->icon);

	if (pMouse->window >= 0 &&
	    pMouse->icon == -1 &&
	    (pMouse->button.data.select || pMouse->button.data.adjust)) {
		/* Get the input focus */
		Error_CheckFatal(Wimp_GetWindowState(pMouse->window, &ws));
		tCaret.window = pMouse->window;
		tCaret.icon = -1;
		tCaret.offset.x = pMouse->pos.x - ws.openblock.screenrect.min.x;
		tCaret.offset.y = pMouse->pos.y - ws.openblock.screenrect.max.y;
		tCaret.height = (int)BIT(25);
		tCaret.index = 0;
		Error_CheckFatal(Wimp_SetCaretPosition(&tCaret));
	}
} /* end of vMainButtonClick */

/*
 * bMainKeyPressed - handle pressed keys for the main window
 */
BOOL
bMainKeyPressed(event_pollblock *pEvent, void *pvReference)
{
	diagram_type 	*pDiag;

	TRACE_MSG("bMainKeyPressed");

	fail(pEvent == NULL);
	fail(pEvent->type != event_KEY);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	fail(pEvent->data.key.caret.window != pDiag->tMainWindow);


	switch (pEvent->data.key.code) {
	case keycode_CTRL_F2:		/* Ctrl F2 */
		bDestroyDiagram(pEvent, pvReference);
		break;
	case keycode_F3:		/* F3 */
		bSaveDrawfile(pEvent, pvReference);
		break;
	case keycode_SHIFT_F3:		/* Shift F3 */
		bSaveTextfile(pEvent, pvReference);
		break;
	default:
		DBG_DEC(pEvent->data.key.code);
		Error_CheckFatal(Wimp_ProcessKey(pEvent->data.key.code));
	}
	return TRUE;
} /* end of bMainKeyPressed */

/*
 * bRedrawMainWindow - redraw the main window
 */
BOOL
bRedrawMainWindow(event_pollblock *pEvent, void *pvReference)
{
	window_redrawblock	tBlock;
	diagram_type	*pDiag;
	drawfile_info	*pInfo;
	double		dScaleFactor;
	BOOL		bMore;

	TRACE_MSG("bRedrawMainWindow");

	fail(pEvent == NULL);
	fail(pEvent->type != event_REDRAW);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	fail(pDiag->tMainWindow != pEvent->data.openblock.window);
	fail(pDiag->iScaleFactorCurr < MIN_SCALE_FACTOR);
	fail(pDiag->iScaleFactorCurr > MAX_SCALE_FACTOR);

	dScaleFactor = (double)pDiag->iScaleFactorCurr / 100.0;
	pInfo = &pDiag->tInfo;

	tBlock.window = pEvent->data.openblock.window;
	Error_CheckFatal(Wimp_RedrawWindow(&tBlock, &bMore));

	/* If there is no real diagram just go thru the motions */
	while (bMore) {
		if (pInfo->data != NULL && pInfo->length != 0) {
			Error_CheckFatal(Drawfile_RenderDiagram(pInfo,
						&tBlock, dScaleFactor));
		}
		Error_CheckFatal(Wimp_GetRectangle(&tBlock, &bMore));
	}
	return TRUE;
} /* end of bRedrawMainWindow */

/*
 * bScaleOpenAction - action to be taken when the Scale view window opens
 */
BOOL
bScaleOpenAction(event_pollblock *pEvent, void *pvReference)
{
	window_state	tWindowState;
	diagram_type	*pDiag;

	TRACE_MSG("bScaleOpenAction");

	fail(pEvent == NULL);
	fail(pEvent->type != event_SEND);
	fail(pEvent->data.message.header.action != message_MENUWARN);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	if (menu_currentopen != pDiag->pSaveMenu ||
	    pEvent->data.message.data.menuwarn.selection[0] != SAVEMENU_SCALEVIEW) {
		return FALSE;
	}

	Error_CheckFatal(Wimp_GetWindowState(pDiag->tScaleWindow,
						&tWindowState));
	if (tWindowState.flags.data.open) {
		/* The window is already open */
		return TRUE;
	}

	DBG_MSG("vScaleOpenAction for real");

	pDiag->iScaleFactorTemp = pDiag->iScaleFactorCurr;
	vUpdateWriteableNumber(pDiag->tScaleWindow,
			SCALE_SCALE_WRITEABLE, pDiag->iScaleFactorTemp);
	Window_Show(pDiag->tScaleWindow, open_UNDERPOINTER);
	return TRUE;
} /* end of bScaleOpenAction */

/*
 * vSetTitle - set the title of a window
 */
void
vSetTitle(diagram_type *pDiag)
{
	char	szTitle[WINDOW_TITLE_LEN];

	TRACE_MSG("vSetTitle");

	fail(pDiag == NULL);
	fail(pDiag->szFilename[0] == '\0');

	(void)sprintf(szTitle, "%.*s at %d%%",
				FILENAME_TITLE_LEN,
				pDiag->szFilename,
				pDiag->iScaleFactorCurr % 1000);
	if (strlen(pDiag->szFilename) > FILENAME_TITLE_LEN) {
		szTitle[FILENAME_TITLE_LEN - 1] = OUR_ELLIPSIS;
	}

	Window_SetTitle(pDiag->tMainWindow, szTitle);
} /* end of vSetTitle */

/*
 * vScaleButtonClick - handle a mouse button click in the Scale view window
 */
void
vScaleButtonClick(mouse_block *pMouse, diagram_type *pDiag)
{
	BOOL	bCloseWindow, bRedraw;

	TRACE_MSG("vScaleButtonClick");

	fail(pMouse == NULL || pDiag == NULL);
	fail(pMouse->window != pDiag->tScaleWindow);

	bCloseWindow = FALSE;
	bRedraw = FALSE;
	switch (pMouse->icon) {
	case SCALE_CANCEL_BUTTON:
		bCloseWindow = TRUE;
		pDiag->iScaleFactorTemp = pDiag->iScaleFactorCurr;
		break;
	case SCALE_SCALE_BUTTON:
		bCloseWindow = TRUE;
		bRedraw = pDiag->iScaleFactorCurr != pDiag->iScaleFactorTemp;
		pDiag->iScaleFactorCurr = pDiag->iScaleFactorTemp;
		break;
	case SCALE_50_PCT:
		pDiag->iScaleFactorTemp = 50;
		break;
	case SCALE_75_PCT:
		pDiag->iScaleFactorTemp = 75;
		break;
	case SCALE_100_PCT:
		pDiag->iScaleFactorTemp = 100;
		break;
	case SCALE_150_PCT:
		pDiag->iScaleFactorTemp = 150;
		break;
	default:
		DBG_DEC(pMouse->icon);
		break;
	}
	if (bCloseWindow) {
		/* Close the scale window */
		Error_CheckFatal(Wimp_CloseWindow(pMouse->window));
		if (bRedraw) {
			/* Redraw the main window */
			vSetTitle(pDiag);
			vForceRedraw(pDiag);
		}
	} else {
		vUpdateWriteableNumber(pMouse->window,
				SCALE_SCALE_WRITEABLE,
				pDiag->iScaleFactorTemp);
	}
} /* end of vScaleButtonClick */

/*
 * bScaleKeyPressed - handle pressed keys for the scale window
 */
BOOL
bScaleKeyPressed(event_pollblock *pEvent, void *pvReference)
{
	icon_block	tIcon;
	diagram_type	*pDiag;
	caret_block	*pCaret;
	char		*pcChar;
	int		iTmp;

	TRACE_MSG("bScaleKeyPressed");

        fail(pEvent == NULL);
        fail(pEvent->type != event_KEY);
        fail(pvReference == NULL);

	pCaret = &pEvent->data.key.caret;
	pDiag = (diagram_type *)pvReference;

        fail(pEvent->data.key.caret.window != pDiag->tScaleWindow);

	DBG_DEC_C(pCaret->icon != SCALE_SCALE_WRITEABLE, pCaret->icon);
	DBG_DEC_C(pCaret->icon == SCALE_SCALE_WRITEABLE, pEvent->data.key.code);

	if (pEvent->data.key.code != '\r' ||
	    pCaret->icon != SCALE_SCALE_WRITEABLE) {
		Error_CheckFatal(Wimp_ProcessKey(pEvent->data.key.code));
		return TRUE;
	}

	Error_CheckFatal(Wimp_GetIconState(pCaret->window, pCaret->icon, &tIcon));
	if (!tIcon.flags.data.text || !tIcon.flags.data.indirected) {
		werr(1, "Icon %d must be indirected text", (int)pCaret->icon);
	}
	iTmp = (int)strtol(tIcon.data.indirecttext.buffer, &pcChar, 10);
	if (*pcChar != '\0' && *pcChar != '\r') {
		DBG_DEC(*pcChar);
	} else if (iTmp < MIN_SCALE_FACTOR) {
		pDiag->iScaleFactorTemp = MIN_SCALE_FACTOR;
	} else if (iTmp > MAX_SCALE_FACTOR) {
		pDiag->iScaleFactorTemp = MAX_SCALE_FACTOR;
	} else {
		pDiag->iScaleFactorTemp = iTmp;
	}
	pDiag->iScaleFactorCurr = pDiag->iScaleFactorTemp;
	/* Close the scale window */
	Error_CheckFatal(Wimp_CloseWindow(pCaret->window));
	/* Redraw the main window */
	vSetTitle(pDiag);
	vForceRedraw(pDiag);
	return TRUE;
} /* end of bScaleKeyPressed */

