/*
 * main_ros.c
 *
 * Released under GPL
 *
 * Copyright (C) 1998-2005 A.J. van Os
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Description:
 * The main program of !Antiword (RISC OS version)
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "DeskLib:Dialog2.h"
#include "DeskLib:Error.h"
#include "DeskLib:Event.h"
#include "DeskLib:EventMsg.h"
#include "DeskLib:Handler.h"
#include "DeskLib:Menu.h"
#include "DeskLib:Resource.h"
#include "DeskLib:Screen.h"
#include "DeskLib:Template.h"
#include "DeskLib:Window.h"
#if defined(__GNUC__)
#include "flexlib:flex.h"
#endif /* __GNUC__ */
#include "version.h"
#include "antiword.h"


/* The name of this program */
static char	*szTask = "!Antiword";

/* The window handle of the choices window */
static window_handle	tChoicesWindow = 0;

/* Dummy diagram with the iconbar menu pointer */
static diagram_type	tDummyDiagram;

/* Program information Box */
static dialog2_block	*pInfoBox = NULL;

/* Info box fields */
#define PURPOSE_INFO_FIELD	2
#define AUTHOR_INFO_FIELD	3
#define VERSION_INFO_FIELD	4
#define STATUS_INFO_FIELD	5

/* Iconbar menu fields */
#define ICONBAR_INFO_FIELD	0
#define ICONBAR_CHOICES_FIELD	1
#define ICONBAR_QUIT_FIELD	2


/*
 * bBarInfo - Show iconbar information
 */
static BOOL
bBarInfo(event_pollblock *pEvent, void *pvReference)
{
	diagram_type	*pDiag;

	TRACE_MSG("bBarInfo");

	fail(pEvent == NULL);
	fail(pEvent->type != event_SEND);
	fail(pEvent->data.message.header.action != message_MENUWARN);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	if (menu_currentopen != pDiag->pSaveMenu ||
	    pEvent->data.message.data.menuwarn.selection[0] != ICONBAR_INFO_FIELD) {
		return FALSE;
	}

	Dialog2_OpenDialogMenuLeaf(pEvent, pInfoBox);
	return TRUE;
} /* end of bBarInfo */

/*
 * vBarInfoSetText - Set the iconbar infobox text
 */
static void
vBarInfoSetText(dialog2_block *pBox)
{
	TRACE_MSG("vBarInfoSetText");

	fail(pBox == NULL);
	fail(pBox != pInfoBox);

	Icon_SetText(pBox->window, PURPOSE_INFO_FIELD, PURPOSESTRING);
	Icon_SetText(pBox->window, AUTHOR_INFO_FIELD, AUTHORSTRING);
	Icon_SetText(pBox->window, VERSION_INFO_FIELD, VERSIONSTRING);
	Icon_SetText(pBox->window, STATUS_INFO_FIELD, STATUSSTRING);
} /* end of vBarInfoSetText */

/*
 * bMouseButtonClick - respond to mouse button click
 */
static BOOL
bMouseButtonClick(event_pollblock *pEvent, void *pvReference)
{
	diagram_type	*pDiag;
	menu_ptr	pMenu;
	int		iPosY;

	TRACE_MSG("bMouseButtonClick");

	fail(pEvent == NULL);
	fail(pEvent->type != event_CLICK);
	fail(pvReference == NULL);

	pDiag = (diagram_type *)pvReference;

	if (pEvent->data.mouse.button.data.menu) {
		pMenu = pDiag->pSaveMenu;
		iPosY = (pMenu == tDummyDiagram.pSaveMenu) ?
					-1 : pEvent->data.mouse.pos.y;
		Menu_Show(pMenu, pEvent->data.mouse.pos.x, iPosY);
		return TRUE;
	}
	if (pEvent->data.mouse.window == pDiag->tMainWindow &&
	    pEvent->data.mouse.icon == -1) {
		vMainButtonClick(&pEvent->data.mouse);
		return TRUE;
	}
	if (pEvent->data.mouse.window == pDiag->tScaleWindow &&
	    pEvent->data.mouse.icon >= 0) {
		vScaleButtonClick(&pEvent->data.mouse, pDiag);
		return TRUE;
	}
	return FALSE;
} /* end of bMouseButtonClick */

/*
 * bAutoRedrawWindow - the redraw is handled by the WIMP
 */
static BOOL
bAutoRedrawWindow(event_pollblock *pEvent, void *pvReference)
{
	return TRUE;
} /* end of bAutoRedrawWindow */

static BOOL
bSaveSelect(event_pollblock *pEvent, void *pvReference)
{
	TRACE_MSG("bSaveSelect");

	fail(pEvent == NULL);
	fail(pEvent->type != event_MENU);
	fail(pvReference == NULL);

	DBG_DEC(pEvent->data.selection[0]);

	switch (pEvent->data.selection[0]) {
	case SAVEMENU_SCALEVIEW:
		return bScaleOpenAction(pEvent, pvReference);
	case SAVEMENU_SAVEDRAW:
		return bSaveDrawfile(pEvent, pvReference);
	case SAVEMENU_SAVETEXT:
		return bSaveTextfile(pEvent, pvReference);
	default:
		DBG_DEC(pEvent->data.selection[0]);
		return FALSE;
	}
} /* end of bSaveSelect */

/*
 * Create the window for the text from the given file
 */
static diagram_type *
pCreateTextWindow(const char *szFilename)
{
	diagram_type	*pDiag;

	TRACE_MSG("pCreateTextWindow");

	fail(szFilename == NULL || szFilename[0] == '\0');

	/* Create the diagram */
	pDiag = pCreateDiagram(szTask+1, szFilename);
	if (pDiag == NULL) {
		werr(0, "Sorry, no new diagram object");
		return NULL;
	}

	/* Prepare a save menu for this diagram */
	pDiag->pSaveMenu = Menu_New(szTask+1,
		">Scale view,"
		">Save (Drawfile)   F3,"
		">Save (Text only) \213F3");
	if (pDiag->pSaveMenu == NULL) {
		werr(1, "Sorry, no Savemenu object");
	}
	Menu_Warn(pDiag->pSaveMenu, SAVEMENU_SCALEVIEW,
					TRUE, bScaleOpenAction, pDiag);
	Menu_Warn(pDiag->pSaveMenu, SAVEMENU_SAVEDRAW,
					TRUE, bSaveDrawfile, pDiag);
	Menu_Warn(pDiag->pSaveMenu, SAVEMENU_SAVETEXT,
					TRUE, bSaveTextfile, pDiag);

	/* Claim events for the main window */
        Event_Claim(event_REDRAW, pDiag->tMainWindow, icon_ANY,
                                        bRedrawMainWindow, pDiag);
        Event_Claim(event_CLOSE, pDiag->tMainWindow, icon_ANY,
                                        bDestroyDiagram, pDiag);
        Event_Claim(event_CLICK, pDiag->tMainWindow, icon_ANY,
                                        bMouseButtonClick, pDiag);
        Event_Claim(event_KEY, pDiag->tMainWindow, icon_ANY,
                                        bMainKeyPressed, pDiag);

	/* Claim events for the scale window */
	Event_Claim(event_REDRAW, pDiag->tScaleWindow, icon_ANY,
					bAutoRedrawWindow, NULL);
        Event_Claim(event_CLICK, pDiag->tScaleWindow, icon_ANY,
                                        bMouseButtonClick, pDiag);
        Event_Claim(event_KEY, pDiag->tScaleWindow, icon_ANY,
                                        bScaleKeyPressed, pDiag);

	/* Set the window title */
	vSetTitle(pDiag);
	return pDiag;
} /* end of pCreateTextWindow */

/*
 * vProcessFile - process one file
 */
static void
vProcessFile(const char *szFilename, int iFiletype)
{
	options_type	tOptions;
	FILE		*pFile;
	diagram_type	*pDiag;
	long		lFilesize;
	int		iWordVersion;

	TRACE_MSG("vProcessFile");

	fail(szFilename == NULL || szFilename[0] == '\0');

	DBG_MSG(szFilename);

	pFile = fopen(szFilename, "rb");
	if (pFile == NULL) {
		werr(0, "I can't open '%s' for reading", szFilename);
		return;
	}

	lFilesize = lGetFilesize(szFilename);
	if (lFilesize < 0) {
		(void)fclose(pFile);
		werr(0, "I can't get the size of '%s'", szFilename);
		return;
	}

	iWordVersion = iGuessVersionNumber(pFile, lFilesize);
	if (iWordVersion < 0 || iWordVersion == 3) {
		if (bIsRtfFile(pFile)) {
			werr(0, "%s is not a Word Document."
				" It is probably a Rich Text Format file",
				szFilename);
		} if (bIsWordPerfectFile(pFile)) {
			werr(0, "%s is not a Word Document."
				" It is probably a Word Perfect file",
				szFilename);
		} else {
			werr(0, "%s is not a Word Document.", szFilename);
		}
		(void)fclose(pFile);
		return;
	}
	/* Reset any reading done during file-testing */
	rewind(pFile);

	if (iFiletype != FILETYPE_MSWORD) {
		vGetOptions(&tOptions);
		if (tOptions.bAutofiletypeAllowed) {
			vSetFiletype(szFilename, FILETYPE_MSWORD);
		}
	}

	pDiag = pCreateTextWindow(szFilename);
	if (pDiag == NULL) {
		(void)fclose(pFile);
		return;
	}

	(void)bWordDecryptor(pFile, lFilesize, pDiag);
	Error_CheckFatal(Drawfile_VerifyDiagram(&pDiag->tInfo));
	vShowDiagram(pDiag);
	TRACE_MSG("After vShowDiagram");

	TRACE_MSG("before debug print");
	DBG_HEX(pFile);
	TRACE_MSG("before fclose");
	(void)fclose(pFile);
	TRACE_MSG("after fclose");
} /* end of vProcessFile */

/*
 * vSendAck - send an acknowledge
 */
static void
vSendAck(event_pollblock *pEvent)
{
	message_block	tMessage;

	TRACE_MSG("vSendAck");

	fail(pEvent == NULL);
	fail(pEvent->type != event_SEND && pEvent->type != event_SENDWANTACK);
	fail(pEvent->data.message.header.action != message_DATALOAD &&
		pEvent->data.message.header.action != message_DATAOPEN);

	tMessage.header.action = message_DATALOADACK;
	tMessage.header.size = sizeof(tMessage);
	tMessage.header.yourref = pEvent->data.message.header.myref;
	Error_CheckFatal(Wimp_SendMessage(event_SEND, &tMessage,
				pEvent->data.message.header.sender, 0));
} /* end of vSendAck */

static BOOL
bEventMsgHandler(event_pollblock *pEvent, void *pvReference)
{
	TRACE_MSG("bEventMsgHandler");

	fail(pEvent == NULL);

	switch (pEvent->type) {
	case event_SEND:
	case event_SENDWANTACK:
		switch (pEvent->data.message.header.action) {
		case message_CLOSEDOWN:
			exit(EXIT_SUCCESS);
			break;
		case message_DATALOAD:
		case message_DATAOPEN:
			vProcessFile(
				pEvent->data.message.data.dataload.filename,
				pEvent->data.message.data.dataload.filetype);
			vSendAck(pEvent);
			break;
		default:
			DBG_DEC(pEvent->data.message.header.action);
			break;
		}
		return TRUE;
	default:
		DBG_DEC(pEvent->type);
		return FALSE;
	}
} /* end of bEventMsgHandler */

/*
 * bMenuSelect - select from the iconbar menu
 */
static BOOL
bMenuSelect(event_pollblock *pEvent, void *pvReference)
{
	TRACE_MSG("bMenuSelect");

	fail(pEvent == NULL);
	fail(pEvent->type != event_MENU);

	DBG_DEC(pEvent->data.selection[0]);

	switch (pEvent->data.selection[0]) {
	case ICONBAR_INFO_FIELD:
		return bBarInfo(pEvent, pvReference);
	case ICONBAR_CHOICES_FIELD:
		vChoicesOpenAction(tChoicesWindow);
		Window_BringToFront(tChoicesWindow);
		break;
	case ICONBAR_QUIT_FIELD:
		TRACE_MSG("before exit");
		exit(EXIT_SUCCESS);
		break;
	default:
		DBG_DEC(pEvent->data.selection[0]);
		break;
	}
	return TRUE;
} /* end of bMenuSelect */

/*
 * bMenuClick - respond to an menu click
 */
static BOOL
bMenuClick(event_pollblock *pEvent, void *pvReference)
{
	TRACE_MSG("bMenuClick");

	fail(pEvent == NULL);
	fail(pEvent->type != event_MENU);

	if (menu_currentopen == tDummyDiagram.pSaveMenu) {
		return bMenuSelect(pEvent, pvReference);
	} else if (pvReference == NULL) {
		return FALSE;
	}
	return bSaveSelect(pEvent, pvReference);
} /* end of bMenuClick */

static void
vTemplates(void)
{
	TRACE_MSG("vTemplates");

	Template_Initialise();
	Template_LoadFile("Templates");

	tChoicesWindow = Window_Create("Choices", template_TITLEMIN);
	if (tChoicesWindow == 0) {
		werr(1, "I can't find the 'Choices' template");
	}

	/* Claim events for the choices window */
	Event_Claim(event_REDRAW, tChoicesWindow, icon_ANY,
					bAutoRedrawWindow, NULL);
	Event_Claim(event_CLICK, tChoicesWindow, icon_ANY,
					bChoicesMouseClick, NULL);
	Event_Claim(event_KEY, tChoicesWindow, icon_ANY,
					bChoicesKeyPressed, NULL);
} /* end of vTemplates */

static void
vInitialise(void)
{
	int	aiMessages[] = {0};
	icon_handle	tBarIcon;


	TRACE_MSG("vInitialise");

	Resource_Initialise(szTask+1);
	Event_Initialise3(szTask+1, 310, aiMessages);
	EventMsg_Initialise();
	Screen_CacheModeInfo();
#if defined(__GNUC__)
	flex_init(szTask+1, 0, 0);
	flex_set_budge(1);
#endif /* __GNUC__ */
	vTemplates();

	/* Prepare iconbar menu */
	tDummyDiagram.tInfo.data = NULL;
	tDummyDiagram.tInfo.length = 0;
	tDummyDiagram.pSaveMenu = Menu_New(szTask+1, ">Info,Choices...,Quit");
	if (tDummyDiagram.pSaveMenu == NULL) {
		werr(1, "Sorry, no Barmenu object");
	}
	pInfoBox = Dialog2_CreateDialogBlock("ProgInfo", -1, -1,
					vBarInfoSetText, NULL, NULL);

	if (pInfoBox == NULL) {
		werr(1, "Sorry, no Infobox object");
	}
	Menu_Warn(tDummyDiagram.pSaveMenu, ICONBAR_INFO_FIELD,
					TRUE, bBarInfo, &tDummyDiagram);

	/* Create an icon on the icon bar */
	tBarIcon = Icon_BarIcon(szTask, iconbar_RIGHT);
	Event_Claim(event_CLICK, window_ICONBAR, tBarIcon,
					bMouseButtonClick, &tDummyDiagram);

	/* Generic claims */
	Event_Claim(event_OPEN, window_ANY, icon_ANY,
					Handler_OpenWindow, NULL);
	Event_Claim(event_CLOSE, window_ANY, icon_ANY,
					Handler_CloseWindow, NULL);
	Event_Claim(event_MENU, window_ANY, icon_ANY,
					bMenuClick, NULL);
	EventMsg_Claim(message_DATALOAD, window_ICONBAR,
					bEventMsgHandler, NULL);
	EventMsg_Claim(message_MODECHANGE, window_ANY,
					Handler_ModeChange, NULL);
} /* end of vInitialise */

int
main(int argc, char **argv)
{
	int	iFirst, iFiletype;

	TRACE_MSG("main");

	vInitialise();
	iFirst = iReadOptions(argc, argv);
	if (iFirst != 1) {
		return EXIT_FAILURE;
	}

	if (argc > 1) {
		iFiletype = iGetFiletype(argv[1]);
		if (iFiletype < 0) {
			return EXIT_FAILURE;
		}
		vProcessFile(argv[1], iFiletype);
		TRACE_MSG("main after vProcessFile");
	}

	for (;;) {
		Event_Poll();
	}
} /* end of main */
