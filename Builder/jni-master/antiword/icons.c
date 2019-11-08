/*
 * icons.c
 * Copyright (C) 1998-2001 A.J. van Os; Released under GPL
 *
 * Description:
 * Update window icons
 */

#include <string.h>
#include "DeskLib:Error.h"
#include "DeskLib:WimpSWIs.h"
#include "antiword.h"

void
vUpdateIcon(window_handle tWindow, icon_block *pIcon)
{
	window_redrawblock	tRedraw;
	BOOL		bMore;

	tRedraw.window = tWindow;
	tRedraw.rect = pIcon->workarearect;
	Error_CheckFatal(Wimp_UpdateWindow(&tRedraw, &bMore));
	while (bMore) {
		Error_CheckFatal(Wimp_PlotIcon(pIcon));
		Error_CheckFatal(Wimp_GetRectangle(&tRedraw, &bMore));
	}
} /* end of vUpdateIcon */

void
vUpdateRadioButton(window_handle tWindow, icon_handle tIconNumber,
	BOOL bSelected)
{
	icon_block	tIcon;

	Error_CheckFatal(Wimp_GetIconState(tWindow, tIconNumber, &tIcon));
	DBG_DEC(tIconNumber);
	DBG_HEX(tIcon.flags.data.selected);
	if (bSelected == (tIcon.flags.data.selected == 1)) {
		/* No update needed */
		return;
	}
	Error_CheckFatal(Wimp_SetIconState(tWindow, tIconNumber,
			bSelected ? 0x00200000 : 0, 0x00200000));
	vUpdateIcon(tWindow, &tIcon);
} /* end of vUpdateRadioButton */

/*
 * vUpdateWriteable - update a writeable icon with a string
 */
void
vUpdateWriteable(window_handle tWindow, icon_handle tIconNumber,
	const char *szString)
{
	icon_block	tIcon;
	caret_block	tCaret;
	int		iLen;

	fail(szString == NULL);

	NO_DBG_DEC(tIconNumber);
	NO_DBG_MSG(szString);

	Error_CheckFatal(Wimp_GetIconState(tWindow, tIconNumber, &tIcon));
	NO_DBG_HEX(tIcon.flags);
	if (!tIcon.flags.data.text || !tIcon.flags.data.indirected) {
		werr(1, "Icon %d must be indirected text", (int)tIconNumber);
		return;
	}
	strncpy(tIcon.data.indirecttext.buffer,
		szString,
		tIcon.data.indirecttext.bufflen - 1);
	/* Ensure the caret is behind the last character of the text */
	Error_CheckFatal(Wimp_GetCaretPosition(&tCaret));
	if (tCaret.window == tWindow && tCaret.icon == tIconNumber) {
		iLen = strlen(tIcon.data.indirecttext.buffer);
		if (tCaret.index != iLen) {
			tCaret.index = iLen;
			Error_CheckFatal(Wimp_SetCaretPosition(&tCaret));
		}
	}
	Error_CheckFatal(Wimp_SetIconState(tWindow, tIconNumber, 0, 0));
	vUpdateIcon(tWindow, &tIcon);
} /* end of vUpdateWriteable */

/*
 * vUpdateWriteableNumber - update a writeable icon with a number
 */
void
vUpdateWriteableNumber(window_handle tWindow, icon_handle tIconNumber,
	int iNumber)
{
	char	szTmp[1+3*sizeof(int)+1];

	(void)sprintf(szTmp, "%d", iNumber);
	vUpdateWriteable(tWindow, tIconNumber, szTmp);
} /* end of vUpdateWriteableNumber */
