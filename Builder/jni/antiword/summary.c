/*
 * summary.c
 * Copyright (C) 2002-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Read the summary information of a Word document
 */

#include <time.h>
#include <string.h>
#include "antiword.h"

#define P_HEADER_SZ		28
#define P_SECTIONLIST_SZ	20
#define P_LENGTH_SZ		 4
#define P_SECTION_MAX_SZ	(2 * P_SECTIONLIST_SZ + P_LENGTH_SZ)
#define P_SECTION_SZ(x)		((x) * P_SECTIONLIST_SZ + P_LENGTH_SZ)

#define PID_TITLE		 2
#define PID_SUBJECT		 3
#define PID_AUTHOR		 4
#define PID_CREATE_DTM		12
#define PID_LASTSAVE_DTM	13
#define PID_APPNAME		18

#define PIDD_MANAGER		14
#define PIDD_COMPANY		15

#define VT_LPSTR		30
#define VT_FILETIME		64

#define TIME_OFFSET_HI		0x019db1de
#define TIME_OFFSET_LO		0xd53e8000

static char	*szTitle = NULL;
static char	*szSubject = NULL;
static char	*szAuthor = NULL;
static time_t	tCreateDtm = (time_t)-1;
static time_t	tLastSaveDtm= (time_t)-1;
static char	*szAppName = NULL;
static char	*szManager = NULL;
static char	*szCompany = NULL;
static USHORT	usLid = (USHORT)-1;


/*
 * vDestroySummaryInfo - destroy the summary information
 */
void
vDestroySummaryInfo(void)
{
	TRACE_MSG("vDestroySummaryInfo");

	szTitle = xfree(szTitle);
	szSubject = xfree(szSubject);
	szAuthor = xfree(szAuthor);
	tCreateDtm = (time_t)-1;
	tLastSaveDtm = (time_t)-1;
	szAppName = xfree(szAppName);
	szManager = xfree(szManager);
	szCompany = xfree(szCompany);
	usLid = (USHORT)-1;
} /* end of vDestroySummaryInfo */

/*
 * tConvertDosDate - convert DOS date format
 *
 * returns Unix time_t or -1
 */
static time_t
tConvertDosDate(const char *szDosDate)
{
	struct tm	tTime;
	const char	*pcTmp;
	time_t		tResult;

	memset(&tTime, 0, sizeof(tTime));
	pcTmp = szDosDate;
	/* Get the month */
	if (!isdigit(*pcTmp)) {
		return (time_t)-1;
	}
	tTime.tm_mon = (int)(*pcTmp - '0');
	pcTmp++;
	if (isdigit(*pcTmp)) {
		tTime.tm_mon *= 10;
		tTime.tm_mon += (int)(*pcTmp - '0');
		pcTmp++;
	}
	/* Get the first separater */
	if (isalnum(*pcTmp)) {
		return (time_t)-1;
	}
	pcTmp++;
	/* Get the day */
	if (!isdigit(*pcTmp)) {
		return (time_t)-1;
	}
	tTime.tm_mday = (int)(*pcTmp - '0');
	pcTmp++;
	if (isdigit(*pcTmp)) {
		tTime.tm_mday *= 10;
		tTime.tm_mday += (int)(*pcTmp - '0');
		pcTmp++;
	}
	/* Get the second separater */
	if (isalnum(*pcTmp)) {
		return (time_t)-1;
	}
	pcTmp++;
	/* Get the year */
	if (!isdigit(*pcTmp)) {
		return (time_t)-1;
	}
	tTime.tm_year = (int)(*pcTmp - '0');
	pcTmp++;
	if (isdigit(*pcTmp)) {
		tTime.tm_year *= 10;
		tTime.tm_year += (int)(*pcTmp - '0');
		pcTmp++;
	}
	/* Check the values */
	if (tTime.tm_mon == 0 || tTime.tm_mday == 0 || tTime.tm_mday > 31) {
		return (time_t)-1;
	}
	/* Correct the values */
	tTime.tm_mon--;		/* From 01-12 to 00-11 */
	if (tTime.tm_year < 80) {
		tTime.tm_year += 100;	/* 00 means 2000 is 100 */
	}
	tTime.tm_isdst = -1;
	tResult = mktime(&tTime);
	NO_DBG_MSG(ctime(&tResult));
	return tResult;
} /* end of tConvertDosDate */

/*
 * szLpstr - get a zero terminate string property
 */
static char *
szLpstr(ULONG ulOffset, const UCHAR *aucBuffer)
{
	char	*szStart, *szResult, *szTmp;
	size_t	tSize;

	tSize = (size_t)ulGetLong(ulOffset + 4, aucBuffer);
	NO_DBG_DEC(tSize);
	if (tSize == 0) {
		return NULL;
	}
	/* Remove white space from the start of the string */
	szStart = (char *)aucBuffer + ulOffset + 8;
	NO_DBG_MSG(szStart);
	fail(strlen(szStart) >= tSize);
	while (isspace(*szStart)) {
		szStart++;
	}
	if (szStart[0] == '\0') {
		return NULL;
	}
	szResult = xstrdup(szStart);
	/* Remove white space from the end of the string */
	szTmp = szResult + strlen(szResult) - 1;
	while (isspace(*szTmp)) {
		*szTmp = '\0';
		szTmp--;
	}
	NO_DBG_MSG(szResult);
	return szResult;
} /* end of szLpstr */

/*
 * tFiletime - get a filetime property
 */
static time_t
tFiletime(ULONG ulOffset, const UCHAR *aucBuffer)
{
	double	dHi, dLo, dTmp;
	ULONG	ulHi, ulLo;
	time_t	tResult;

	ulLo = ulGetLong(ulOffset + 4, aucBuffer);
	ulHi = ulGetLong(ulOffset + 8, aucBuffer);
	NO_DBG_HEX(ulHi);
	NO_DBG_HEX(ulLo);

	/* Move the starting point from 01 Jan 1601 to 01 Jan 1970 */
	dHi = (double)ulHi - (double)TIME_OFFSET_HI;
	dLo = (double)ulLo - (double)TIME_OFFSET_LO;
	NO_DBG_FLT(dHi);
	NO_DBG_FLT(dLo);

	/* Combine the values and divide by 10^7 to get seconds */
	dTmp  = dLo / 10000000.0;	/* 10^7 */
	dTmp += dHi * 429.4967926;	/* 2^32 / 10^7 */
	NO_DBG_FLT(dTmp);

	/* Make a time_t */
	if (dTmp - 0.5 < TIME_T_MIN || dTmp + 0.5 > TIME_T_MAX) {
		return (time_t)-1;
	}
	tResult = dTmp < 0.0 ? (time_t)(dTmp - 0.5) : (time_t)(dTmp + 0.5);
	NO_DBG_MSG(ctime(&tResult));
	return tResult;
} /* end of tFiletime */

/*
 * vAnalyseSummaryInfo - analyse the summary information
 */
static void
vAnalyseSummaryInfo(const UCHAR *aucBuffer)
{
	ULONG	ulOffset;
	size_t	tIndex, tCount, tPropID, tPropType;

	tCount = (size_t)ulGetLong(4, aucBuffer);
	DBG_DEC(tCount);
	for (tIndex = 0; tIndex < tCount; tIndex++) {
		tPropID = (size_t)ulGetLong(8 + tIndex * 8, aucBuffer);
		ulOffset = ulGetLong(12 + tIndex * 8, aucBuffer);
		NO_DBG_DEC(tPropID);
		NO_DBG_HEX(ulOffset);
		tPropType = (size_t)ulGetLong(ulOffset, aucBuffer);
		NO_DBG_DEC(tPropType);
		switch (tPropID) {
		case PID_TITLE:
			if (tPropType == VT_LPSTR && szTitle == NULL) {
				szTitle = szLpstr(ulOffset, aucBuffer);
			}
			break;
		case PID_SUBJECT:
			if (tPropType == VT_LPSTR && szSubject == NULL) {
				szSubject = szLpstr(ulOffset, aucBuffer);
			}
			break;
		case PID_AUTHOR:
			if (tPropType == VT_LPSTR && szAuthor == NULL) {
				szAuthor = szLpstr(ulOffset, aucBuffer);
			}
			break;
		case PID_CREATE_DTM:
			if (tPropType == VT_FILETIME &&
			    tCreateDtm == (time_t)-1) {
				tCreateDtm = tFiletime(ulOffset, aucBuffer);
			}
			break;
		case PID_LASTSAVE_DTM:
			if (tPropType == VT_FILETIME &&
			    tLastSaveDtm == (time_t)-1) {
				tLastSaveDtm = tFiletime(ulOffset, aucBuffer);
			}
			break;
		case PID_APPNAME:
			if (tPropType == VT_LPSTR && szAppName == NULL) {
				szAppName = szLpstr(ulOffset, aucBuffer);
			}
			break;
		default:
			break;
		}
	}
} /* end of vAnalyseSummaryInfo */

/*
 * vAnalyseDocumentSummaryInfo - analyse the document summary information
 */
static void
vAnalyseDocumentSummaryInfo(const UCHAR *aucBuffer)
{
	ULONG	ulOffset;
	size_t	tIndex, tCount, tPropID, tPropType;

	tCount = (size_t)ulGetLong(4, aucBuffer);
	DBG_DEC(tCount);
	for (tIndex = 0; tIndex < tCount; tIndex++) {
		tPropID = (size_t)ulGetLong(8 + tIndex * 8, aucBuffer);
		ulOffset = ulGetLong(12 + tIndex * 8, aucBuffer);
		NO_DBG_DEC(tPropID);
		NO_DBG_HEX(ulOffset);
		tPropType = (size_t)ulGetLong(ulOffset, aucBuffer);
		NO_DBG_DEC(tPropType);
		switch (tPropID) {
		case PIDD_MANAGER:
			if (tPropType == VT_LPSTR && szManager == NULL) {
				szManager = szLpstr(ulOffset, aucBuffer);
			}
			break;
		case PIDD_COMPANY:
			if (tPropType == VT_LPSTR && szCompany == NULL) {
				szCompany = szLpstr(ulOffset, aucBuffer);
			}
			break;
		default:
			break;
		}
	}
} /* end of vAnalyseDocumentSummaryInfo */

/*
 * pucAnalyseSummaryInfoHeader-
 */
static UCHAR *
pucAnalyseSummaryInfoHeader(FILE *pFile,
	ULONG ulStartBlock, ULONG ulSize,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen)
{
	const ULONG	*aulBlockDepot;
	UCHAR	*aucBuffer;
	size_t	tBlockDepotLen, tBlockSize, tSectionCount, tLength;
	ULONG	ulTmp, ulOffset;
	USHORT	usLittleEndian, usEmpty, usOS, usVersion;
	UCHAR	aucHdr[P_HEADER_SZ], aucSecLst[P_SECTION_MAX_SZ];

	if (ulSize < MIN_SIZE_FOR_BBD_USE) {
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

	if (tBlockDepotLen == 0) {
		DBG_MSG("The Block Depot length is zero");
		return NULL;
	}

	/* Read the Summery Information header */
	if (!bReadBuffer(pFile, ulStartBlock,
			aulBlockDepot, tBlockDepotLen, tBlockSize,
			aucHdr, 0, P_HEADER_SZ)) {
		return NULL;
	}
	NO_DBG_PRINT_BLOCK(aucHdr, P_HEADER_SZ);

	/* Analyse the Summery Information header */
	usLittleEndian =  usGetWord(0, aucHdr);
	if (usLittleEndian != 0xfffe) {
		DBG_HEX(usLittleEndian);
		DBG_MSG_C(usLittleEndian == 0xfeff, "Big endian");
		return NULL;
	}
	usEmpty =  usGetWord(2, aucHdr);
	if (usEmpty != 0x0000) {
		DBG_DEC(usEmpty);
		return NULL;
	}
	ulTmp = ulGetLong(4, aucHdr);
	DBG_HEX(ulTmp);
	usOS = (USHORT)(ulTmp >> 16);
	usVersion = (USHORT)(ulTmp & 0xffff);
	switch (usOS) {
	case 0:
		DBG_MSG("Win16");
		DBG_HEX(usVersion);
		break;
	case 1:
		DBG_MSG("MacOS");
		DBG_HEX(usVersion);
		break;
	case 2:
		DBG_MSG("Win32");
		DBG_HEX(usVersion);
		break;
	default:
		DBG_DEC(usOS);
		DBG_HEX(usVersion);
		break;
	}
	tSectionCount = (size_t)ulGetLong(24, aucHdr);
	DBG_DEC_C(tSectionCount != 1 && tSectionCount != 2, tSectionCount);
	if (tSectionCount != 1 && tSectionCount != 2) {
		return NULL;
	}

	/* Read the Summery Information Section Lists */
	if (!bReadBuffer(pFile, ulStartBlock,
			aulBlockDepot, tBlockDepotLen, tBlockSize,
			aucSecLst, P_HEADER_SZ, P_SECTION_SZ(tSectionCount))) {
		return NULL;
	}
	NO_DBG_PRINT_BLOCK(aucSecLst, P_SECTION_SZ(tSectionCount));

	ulTmp = ulGetLong(0, aucSecLst);
	DBG_HEX(ulTmp);
	ulTmp = ulGetLong(4, aucSecLst);
	DBG_HEX(ulTmp);
	ulTmp = ulGetLong(8, aucSecLst);
	DBG_HEX(ulTmp);
	ulTmp = ulGetLong(12, aucSecLst);
	DBG_HEX(ulTmp);
	ulOffset = ulGetLong(16, aucSecLst);
	DBG_DEC_C(ulOffset != P_HEADER_SZ + P_SECTIONLIST_SZ &&
		ulOffset != P_HEADER_SZ + 2 * P_SECTIONLIST_SZ,
		ulOffset);
	fail(ulOffset != P_HEADER_SZ + P_SECTIONLIST_SZ &&
		ulOffset != P_HEADER_SZ + 2 * P_SECTIONLIST_SZ);
	tLength =
		(size_t)ulGetLong(tSectionCount * P_SECTIONLIST_SZ, aucSecLst);
	NO_DBG_HEX(tLength);
	fail(ulOffset + tLength > ulSize);

	/* Read the Summery Information */
	aucBuffer = xmalloc(tLength);
	if (!bReadBuffer(pFile, ulStartBlock,
			aulBlockDepot, tBlockDepotLen, tBlockSize,
			aucBuffer, ulOffset, tLength)) {
		aucBuffer = xfree(aucBuffer);
		return NULL;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tLength);
	return aucBuffer;
} /* end of pucAnalyseSummaryInfoHeader */

/*
 * vSet0SummaryInfo - set summary information from a Word for DOS file
 */
void
vSet0SummaryInfo(FILE *pFile, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginSumdInfo, ulBeginNextBlock;
	size_t	tLen;
	USHORT	usCodepage, usOffset;

	TRACE_MSG("vSet0SummaryInfo");

	fail(pFile == NULL || aucHeader == NULL);

	/* First check the header */
	usCodepage = usGetWord(0x7e, aucHeader);
	DBG_DEC(usCodepage);
	switch (usCodepage) {
	case 850: usLid = 0x0809; break; /* Latin1 -> British English */
	case 862: usLid = 0x040d; break; /* Hebrew */
	case 866: usLid = 0x0419; break; /* Russian */
	case 0:
	case 437:
	default: usLid = 0x0409; break; /* ASCII -> American English */
	}

	/* Second check the summary information block */
	ulBeginSumdInfo = 128 * (ULONG)usGetWord(0x1c, aucHeader);
	DBG_HEX(ulBeginSumdInfo);
	ulBeginNextBlock = 128 * (ULONG)usGetWord(0x6a, aucHeader);
	DBG_HEX(ulBeginNextBlock);

	if (ulBeginSumdInfo >= ulBeginNextBlock || ulBeginNextBlock == 0) {
		/* There is no summary information block */
		return;
	}
	tLen = (size_t)(ulBeginNextBlock - ulBeginSumdInfo);
	aucBuffer = xmalloc(tLen);
	/* Read the summary information block */
	if (!bReadBytes(aucBuffer, tLen, ulBeginSumdInfo, pFile)) {
		return;
	}
	usOffset = usGetWord(0, aucBuffer);
	if (aucBuffer[usOffset] != 0) {
		NO_DBG_MSG(aucBuffer + usOffset);
		szTitle = xstrdup((char *)aucBuffer + usOffset);
	}
	usOffset = usGetWord(2, aucBuffer);
	if (aucBuffer[usOffset] != 0) {
		NO_DBG_MSG(aucBuffer + usOffset);
		szAuthor = xstrdup((char *)aucBuffer + usOffset);
	}
	usOffset = usGetWord(12, aucBuffer);
	if (aucBuffer[usOffset] != 0) {
		NO_DBG_STRN(aucBuffer + usOffset, 8);
		tLastSaveDtm = tConvertDosDate((char *)aucBuffer + usOffset);
	}
	usOffset = usGetWord(14, aucBuffer);
	if (aucBuffer[usOffset] != 0) {
		NO_DBG_STRN(aucBuffer + usOffset, 8);
		tCreateDtm = tConvertDosDate((char *)aucBuffer + usOffset);
	}
	aucBuffer = xfree(aucBuffer);
} /* end of vSet0SummaryInfo */

/*
 * vSet2SummaryInfo - set summary information from a WinWord 1/2 file
 */
void
vSet2SummaryInfo(FILE *pFile, int iWordVersion, const UCHAR *aucHeader)
{
	UCHAR	*aucBuffer;
	ULONG	ulBeginSumdInfo, ulBeginDocpInfo, ulTmp;
	size_t	tSumdInfoLen, tDocpInfoLen, tLen, tCounter, tStart;

	TRACE_MSG("vSet2SummaryInfo");

	fail(pFile == NULL || aucHeader == NULL);
	fail(iWordVersion != 1 && iWordVersion != 2);

	/* First check the header */
	usLid = usGetWord(0x06, aucHeader); /* Language IDentification */
	DBG_HEX(usLid);
	if (usLid < 999 && iWordVersion == 1) {
		switch (usLid) {
		case   1: usLid = 0x0409; break;	/* American English */
		case   2: usLid = 0x0c0c; break;	/* Canadian French */
		case  31: usLid = 0x0413; break;	/* Dutch */
		case  33: usLid = 0x040c; break;	/* French */
		case  34: usLid = 0x040a; break;	/* Spanish */
		case  36: usLid = 0x040e; break;	/* Hungarian */
		case  39: usLid = 0x0410; break;	/* Italian */
		case  44: usLid = 0x0809; break;	/* British English */
		case  45: usLid = 0x0406; break;	/* Danish */
		case  46: usLid = 0x041f; break;	/* Swedish */
		case  47: usLid = 0x0414; break;	/* Norwegian */
		case  48: usLid = 0x0415; break;	/* Polish */
		case  49: usLid = 0x0407; break;	/* German */
		case 351: usLid = 0x0816; break;	/* Portuguese */
		case 358: usLid = 0x040b; break;	/* Finnish */
		default:
			DBG_DEC(usLid);
			DBG_FIXME();
			usLid = 0x0409;		/* American English */
			break;
		}
	}

	if (iWordVersion != 2) {
		/* Unknown where to find the associated strings */
		return;
	}

	/* Second check the associated strings */
	ulBeginSumdInfo = ulGetLong(0x118, aucHeader); /* fcSttbfAssoc */
	DBG_HEX(ulBeginSumdInfo);
	tSumdInfoLen = (size_t)usGetWord(0x11c, aucHeader); /* cbSttbfAssoc */
	DBG_DEC(tSumdInfoLen);

	if (tSumdInfoLen == 0) {
		/* There is no summary information */
		return;
	}

	aucBuffer = xmalloc(tSumdInfoLen);
	if (!bReadBytes(aucBuffer, tSumdInfoLen, ulBeginSumdInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
	NO_DBG_PRINT_BLOCK(aucBuffer, tSumdInfoLen);
	tLen = (size_t)ucGetByte(0, aucBuffer);
	DBG_DEC_C(tSumdInfoLen != tLen, tSumdInfoLen);
	DBG_DEC_C(tSumdInfoLen != tLen, tLen);
	tStart = 1;
	for (tCounter = 0; tCounter < 17; tCounter++) {
		if (tStart >= tSumdInfoLen) {
			break;
		}
		tLen = (size_t)ucGetByte(tStart, aucBuffer);
		if (tLen != 0) {
			NO_DBG_DEC(tCounter);
			NO_DBG_STRN(aucBuffer + tStart + 1, tLen);
			switch (tCounter) {
			case 3:
				szTitle = xmalloc(tLen + 1);
				strncpy(szTitle,
					(char *)aucBuffer + tStart + 1, tLen);
				szTitle[tLen] = '\0';
				break;
			case 4:
				szSubject = xmalloc(tLen + 1);
				strncpy(szSubject,
					(char *)aucBuffer + tStart + 1, tLen);
				szSubject[tLen] = '\0';
				break;
			case 7:
				szAuthor = xmalloc(tLen + 1);
				strncpy(szAuthor,
					(char *)aucBuffer + tStart + 1, tLen);
				szAuthor[tLen] = '\0';
				break;
			default:
				break;
			}
		}
		tStart += tLen + 1;
	}
	aucBuffer = xfree(aucBuffer);

	/* Third check the document properties */
	ulBeginDocpInfo = ulGetLong(0x112, aucHeader); /* fcDop */
	DBG_HEX(ulBeginDocpInfo);
	tDocpInfoLen = (size_t)usGetWord(0x116, aucHeader); /* cbDop */
	DBG_DEC(tDocpInfoLen);
	if (tDocpInfoLen < 12) {
		return;
	}

	aucBuffer = xmalloc(tDocpInfoLen);
	if (!bReadBytes(aucBuffer, tDocpInfoLen, ulBeginDocpInfo, pFile)) {
		aucBuffer = xfree(aucBuffer);
		return;
	}
        ulTmp = ulGetLong(0x14, aucBuffer); /* dttmCreated */
	tCreateDtm = tConvertDTTM(ulTmp);
        ulTmp = ulGetLong(0x18, aucBuffer); /* dttmRevised */
	tLastSaveDtm = tConvertDTTM(ulTmp);
	aucBuffer = xfree(aucBuffer);
} /* end of vSet2SummaryInfo */

/*
 * vSetSummaryInfoOLE - set summary information from a Word 6+ file
 */
static void
vSetSummaryInfoOLE(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen)
{
	UCHAR	*pucBuffer;

	fail(pFile == NULL || pPPS == NULL);
	fail(aulBBD == NULL || aulSBD == NULL);

	/* Summary Information */
	pucBuffer = pucAnalyseSummaryInfoHeader(pFile,
		pPPS->tSummaryInfo.ulSB, pPPS->tSummaryInfo.ulSize,
		aulBBD, tBBDLen, aulSBD, tSBDLen);
	if (pucBuffer != NULL) {
		vAnalyseSummaryInfo(pucBuffer);
		pucBuffer = xfree(pucBuffer);
	}

	/* Document Summary Information */
	pucBuffer = pucAnalyseSummaryInfoHeader(pFile,
		pPPS->tDocSummaryInfo.ulSB, pPPS->tDocSummaryInfo.ulSize,
		aulBBD, tBBDLen, aulSBD, tSBDLen);
	if (pucBuffer != NULL) {
		vAnalyseDocumentSummaryInfo(pucBuffer);
		pucBuffer = xfree(pucBuffer);
	}
} /* end of vSetSummaryInfoOLE */

/*
 * vSet6SummaryInfo - set summary information from a Word 6/7 file
 */
void
vSet6SummaryInfo(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader)
{
	TRACE_MSG("vSet6SummaryInfo");

	/* Header Information */
	usLid = usGetWord(0x06, aucHeader); /* Language IDentification */
	DBG_HEX(usLid);

	/* Summery Information */
	vSetSummaryInfoOLE(pFile, pPPS, aulBBD, tBBDLen, aulSBD, tSBDLen);
} /* end of vSet6SummaryInfo */

/*
 * vSet8SummaryInfo - set summary information a Word 8/9/10 file
 */
void
vSet8SummaryInfo(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader)
{
	USHORT	usTmp;

	TRACE_MSG("vSet8SummaryInfo");

	/* Header Information */
	usTmp = usGetWord(0x0a, aucHeader);
	if (usTmp & BIT(14)) {
		/* Language IDentification Far East */
		usLid = usGetWord(0x3c, aucHeader);
	} else {
		/* Language IDentification */
		usLid = usGetWord(0x06, aucHeader);
	}
	DBG_HEX(usLid);

	/* Summery Information */
	vSetSummaryInfoOLE(pFile, pPPS, aulBBD, tBBDLen, aulSBD, tSBDLen);
} /* end of vSet8SummaryInfo */

/*
 * szGetTitle - get the title field
 */
const char *
szGetTitle(void)
{
	return szTitle;
} /* end of szGetTitle */

/*
 * szGetSubject - get the subject field
 */
const char *
szGetSubject(void)
{
	return szSubject;
} /* end of szGetSubject */

/*
 * szGetAuthor - get the author field
 */
const char *
szGetAuthor(void)
{
	return szAuthor;
} /* end of szGetAuthor */

/*
 * szGetLastSaveDtm - get the last save date field
 */
const char *
szGetLastSaveDtm(void)
{
	static char	szTime[12];
	struct tm	*pTime;

	if (tLastSaveDtm == (time_t)-1) {
		return NULL;
	}
	pTime = localtime(&tLastSaveDtm);
	if (pTime == NULL) {
		return NULL;
	}
	sprintf(szTime, "%04d-%02d-%02d",
		pTime->tm_year + 1900, pTime->tm_mon + 1, pTime->tm_mday);
	return szTime;
} /* end of szGetLastSaveDtm */

/*
 * szGetModDate - get the last save date field
 */
const char *
szGetModDate(void)
{
	static char	szTime[20];
	struct tm	*pTime;

	if (tLastSaveDtm == (time_t)-1) {
		return NULL;
	}
	pTime = localtime(&tLastSaveDtm);
	if (pTime == NULL) {
		return NULL;
	}
	sprintf(szTime, "D:%04d%02d%02d%02d%02d",
		pTime->tm_year + 1900, pTime->tm_mon + 1, pTime->tm_mday,
		pTime->tm_hour, pTime->tm_min);
	return szTime;
} /* end of szGetModDate */

/*
 * szGetCreationDate - get the last save date field
 */
const char *
szGetCreationDate(void)
{
	static char	szTime[20];
	struct tm	*pTime;

	if (tCreateDtm == (time_t)-1) {
		return NULL;
	}
	pTime = localtime(&tCreateDtm);
	if (pTime == NULL) {
		return NULL;
	}
	sprintf(szTime, "D:%04d%02d%02d%02d%02d",
		pTime->tm_year + 1900, pTime->tm_mon + 1, pTime->tm_mday,
		pTime->tm_hour, pTime->tm_min);
	return szTime;
} /* end of szGetCreationDate */

/*
 * szGetCompany - get the company field
 */
const char *
szGetCompany(void)
{
	return szCompany;
} /* end of szGetCompany */

/*
 * szGetLanguage - get de language field
 */
const char *
szGetLanguage(void)
{
	if (usLid == (USHORT)-1) {
		/* No Language IDentification */
		return NULL;
	}
	if (usLid < 999) {
		/* This is a Locale, not a Language IDentification */
		DBG_DEC(usLid);
		return NULL;
	}

	/* Exceptions to the general rule */
	switch (usLid) {
	case 0x0404: return "zh_TW"; /* Traditional Chinese */
	case 0x0804: return "zh_CN"; /* Simplified Chinese */
	case 0x0c04: return "zh_HK"; /* Hong Kong Chinese */
	case 0x1004: return "zh_SG"; /* Singapore Chinese */
	case 0x0807: return "de_CH"; /* Swiss German */
	case 0x0409: return "en_US"; /* American English */
	case 0x0809: return "en_GB"; /* British English */
	case 0x0c09: return "en_AU"; /* Australian English */
	case 0x080a: return "es_MX"; /* Mexican Spanish */
	case 0x080c: return "fr_BE"; /* Belgian French */
	case 0x0c0c: return "fr_CA"; /* Canadian French */
	case 0x100c: return "fr_CH"; /* Swiss French */
	case 0x0810: return "it_CH"; /* Swiss Italian */
	case 0x0813: return "nl_BE"; /* Belgian Dutch */
	case 0x0416: return "pt_BR"; /* Brazilian Portuguese */
	case 0x081a:
	case 0x0c1a: return "sr";    /* Serbian */
	case 0x081d: return "sv_FI"; /* Finland Swedish */
	default:
		break;
	}

	/* The general rule */
	switch (usLid & 0x00ff) {
	case 0x01: return "ar";	/* Arabic */
	case 0x02: return "bg";	/* Bulgarian */
	case 0x03: return "ca";	/* Catalan */
	case 0x04: return "zh";	/* Chinese */
	case 0x05: return "cs";	/* Czech */
	case 0x06: return "da";	/* Danish */
	case 0x07: return "de";	/* German */
	case 0x08: return "el";	/* Greek */
	case 0x09: return "en";	/* English */
	case 0x0a: return "es";	/* Spanish */
	case 0x0b: return "fi";	/* Finnish */
	case 0x0c: return "fr";	/* French */
	case 0x0d: return "he";	/* Hebrew */
	case 0x0e: return "hu";	/* Hungarian */
	case 0x0f: return "is";	/* Icelandic */
	case 0x10: return "it";	/* Italian */
	case 0x11: return "ja";	/* Japanese */
	case 0x12: return "ko";	/* Korean */
	case 0x13: return "nl";	/* Dutch */
	case 0x14: return "no";	/* Norwegian */
	case 0x15: return "pl";	/* Polish */
	case 0x16: return "pt";	/* Portuguese */
	case 0x17: return "rm";	/* Rhaeto-Romance */
	case 0x18: return "ro";	/* Romanian */
	case 0x19: return "ru";	/* Russian */
	case 0x1a: return "hr";	/* Croatian */
	case 0x1b: return "sk";	/* Slovak */
	case 0x1c: return "sq";	/* Albanian */
	case 0x1d: return "sv";	/* Swedish */
	case 0x1e: return "th";	/* Thai */
	case 0x1f: return "tr";	/* Turkish */
	case 0x20: return "ur";	/* Urdu */
	case 0x21: return "id";	/* Indonesian */
	case 0x22: return "uk";	/* Ukrainian */
	case 0x23: return "be";	/* Belarusian */
	case 0x24: return "sl";	/* Slovenian */
	case 0x25: return "et";	/* Estonian */
	case 0x26: return "lv";	/* Latvian */
	case 0x27: return "lt";	/* Lithuanian */
	case 0x29: return "fa";	/* Farsi */
	case 0x2a: return "vi";	/* Viet Nam */
	case 0x2b: return "hy";	/* Armenian */
	case 0x2c: return "az";	/* Azeri */
	case 0x2d: return "eu";	/* Basque */
	case 0x2f: return "mk";	/* Macedonian */
	case 0x36: return "af";	/* Afrikaans */
	case 0x37: return "ka";	/* Georgian */
	case 0x38: return "fo";	/* Faeroese */
	case 0x39: return "hi";	/* Hindi */
	case 0x3e: return "ms";	/* Malay */
	case 0x3f: return "kk";	/* Kazakh */
	default:
		DBG_HEX(usLid);
		DBG_FIXME();
		return NULL;
	}
} /* end of szGetLanguage */
