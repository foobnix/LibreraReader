/*
 * tabstops.c
 * Copyright (C) 1999-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Read the tab stop information from a MS Word file
 */

#include <stdio.h>
#include "antiword.h"

#define HALF_INCH	36000L	/* In millipoints */

static long	lDefaultTabWidth = HALF_INCH;


/*
 * vSet0DefaultTabWidth -
 */
static void
vSet0DefaultTabWidth(const UCHAR *aucHeader)
{
	USHORT	usTmp;

	fail(aucHeader == NULL);

	usTmp = usGetWord(0x70, aucHeader); /* dxaTab */
	DBG_DEC(usTmp);
	lDefaultTabWidth = usTmp == 0 ? HALF_INCH : lTwips2MilliPoints(usTmp);
	DBG_DEC(lDefaultTabWidth);
} /* end of vSet0DefaultTabWidth */

/*
 * vSet2DefaultTabWidth -
 */
static void
vSet2DefaultTabWidth(FILE *pFile, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginDocpInfo;
	size_t	tDocpInfoLen;
	USHORT	usTmp;

	fail(pFile == NULL || aucHeader == NULL);

	ulBeginDocpInfo = ulGetLong(0x112, aucHeader); /* fcDop */
	DBG_HEX(ulBeginDocpInfo);
	tDocpInfoLen = (size_t)usGetWord(0x116, aucHeader); /* cbDop */
	DBG_DEC(tDocpInfoLen);
	if (tDocpInfoLen < 12) {
		DBG_MSG("No TAB information");
		return;
	}

	aucBuffer = xmalloc(tDocpInfoLen);
	if (!bReadBytes(aucBuffer, tDocpInfoLen, ulBeginDocpInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	usTmp = usGetWord(0x0a, aucBuffer); /* dxaTab */
	lDefaultTabWidth = usTmp == 0 ? HALF_INCH : lTwips2MilliPoints(usTmp);
	DBG_DEC(lDefaultTabWidth);
	aucBuffer = xfree(aucBuffer);
} /* end of vSet2DefaultTabWidth */

/*
 * vSet6DefaultTabWidth -
 */
static void
vSet6DefaultTabWidth(FILE *pFile, ULONG ulStartBlock,
	const ULONG *aulBBD, size_t tBBDLen, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginDocpInfo;
	size_t	tDocpInfoLen;
	USHORT	usTmp;

	ulBeginDocpInfo = ulGetLong(0x150, aucHeader); /* fcDop */
	DBG_HEX(ulBeginDocpInfo);
	tDocpInfoLen = (size_t)ulGetLong(0x154, aucHeader); /* lcbDop */
	DBG_DEC(tDocpInfoLen);
	if (tDocpInfoLen < 12) {
		DBG_MSG("No TAB information");
		return;
	}

	aucBuffer = xmalloc(tDocpInfoLen);
	if (!bReadBuffer(pFile, ulStartBlock,
			aulBBD, tBBDLen, BIG_BLOCK_SIZE,
			aucBuffer, ulBeginDocpInfo, tDocpInfoLen)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	usTmp = usGetWord(0x0a, aucBuffer); /* dxaTab */
	lDefaultTabWidth = usTmp == 0 ? HALF_INCH : lTwips2MilliPoints(usTmp);
	DBG_DEC(lDefaultTabWidth);
	aucBuffer = xfree(aucBuffer);
} /* end of vSet6DefaultTabWidth */

/*
 * vSet8DefaultTabWidth -
 */
static void
vSet8DefaultTabWidth(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader)
{
        const ULONG	*aulBlockDepot;
	UCHAR	*aucBuffer;
	ULONG	ulBeginDocpInfo;
	size_t	tDocpInfoLen, tBlockDepotLen, tBlockSize;
	USHORT	usTmp;

	ulBeginDocpInfo = ulGetLong(0x192, aucHeader); /* fcDop */
	DBG_HEX(ulBeginDocpInfo);
	tDocpInfoLen = (size_t)ulGetLong(0x196, aucHeader); /* lcbDop */
	DBG_DEC(tDocpInfoLen);
	if (tDocpInfoLen < 12) {
		DBG_MSG("No TAB information");
		return;
	}

	DBG_DEC(pPPS->tTable.ulSB);
	DBG_HEX(pPPS->tTable.ulSize);
	if (pPPS->tTable.ulSize == 0) {
		DBG_MSG("No TAB information");
		return;
	}

	if (pPPS->tTable.ulSize < MIN_SIZE_FOR_BBD_USE) {
		/* Use the Small Block Depot */
		aulBlockDepot = aulSBD;
		tBlockDepotLen = tSBDLen;
		tBlockSize = SMALL_BLOCK_SIZE;
	} else {
		/* Use the Big Block Depot */
		aulBlockDepot = aulBBD;
		tBlockDepotLen = tBBDLen;
		tBlockSize = BIG_BLOCK_SIZE;
	}
	aucBuffer = xmalloc(tDocpInfoLen);
	if (!bReadBuffer(pFile, pPPS->tTable.ulSB,
			aulBlockDepot, tBlockDepotLen, tBlockSize,
			aucBuffer, ulBeginDocpInfo, tDocpInfoLen)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	usTmp = usGetWord(0x0a, aucBuffer); /* dxaTab */
	lDefaultTabWidth = usTmp == 0 ? HALF_INCH : lTwips2MilliPoints(usTmp);
	DBG_DEC(lDefaultTabWidth);
	aucBuffer = xfree(aucBuffer);
} /* end of vSet8DefaultTabWidth */

/*
 * vSetDefaultTabWidth -
 */
void
vSetDefaultTabWidth(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader, int iWordVersion)
{
	fail(pFile == NULL && iWordVersion >= 1);
	fail(pPPS == NULL && iWordVersion >= 6);
	fail(aulBBD == NULL && tBBDLen != 0);
	fail(aulSBD == NULL && tSBDLen != 0);
	fail(aucHeader == NULL);

	/* Reset to the default default value */
	lDefaultTabWidth = HALF_INCH;

	switch (iWordVersion) {
	case 0:
		vSet0DefaultTabWidth(aucHeader);
		break;
	case 1:
	case 2:
		vSet2DefaultTabWidth(pFile, aucHeader);
		break;
	case 4:
	case 5:
		break;
	case 6:
	case 7:
		vSet6DefaultTabWidth(pFile, pPPS->tWordDocument.ulSB,
				aulBBD, tBBDLen, aucHeader);
		break;
	case 8:
		vSet8DefaultTabWidth(pFile, pPPS,
				aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		break;
	default:
		werr(0, "Sorry, no TAB information");
		break;
	}
} /* end of vSetDefaultTabWidth */

#if 0
/*
 * lGetDefaultTabWidth - Get the default tabwidth in millipoints
 */
long
lGetDefaultTabWidth(void)
{
	if (lDefaultTabWidth <= 0) {
		DBG_DEC(lDefaultTabWidth);
		return lTwips2MilliPoints(1);
	}
	return lDefaultTabWidth;
} /* end of lGetDefaultTabWidth */
#endif
