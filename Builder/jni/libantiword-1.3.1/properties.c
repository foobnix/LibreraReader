/*
 * properties.c
 * Copyright (C) 1998-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Read the properties information from a MS Word file
 */

#include <stdlib.h>
#include <string.h>
#include "antiword.h"


/*
 * Build the lists with Property Information
 */
void
vGetPropertyInfo(FILE *pFile, const pps_info_type *pPPS,
	const ULONG *aulBBD, size_t tBBDLen,
	const ULONG *aulSBD, size_t tSBDLen,
	const UCHAR *aucHeader, int iWordVersion)
{
	options_type	tOptions;

	TRACE_MSG("vGetPropertyInfo");

	fail(pFile == NULL);
	fail(pPPS == NULL && iWordVersion >= 6);
	fail(aulBBD == NULL && tBBDLen != 0);
	fail(aulSBD == NULL && tSBDLen != 0);
	fail(aucHeader == NULL);

	/* Get the options */
	vGetOptions(&tOptions);

	/* Get the property information per Word version */
	switch (iWordVersion) {
	case 0:
		vGet0DopInfo(pFile, aucHeader);
		vGet0SepInfo(pFile, aucHeader);
		vGet0PapInfo(pFile, aucHeader);
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_xml ||
		    tOptions.eConversionType == conversion_fmt_text ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet0ChrInfo(pFile, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vCreate0FontTable();
		}
		vSet0SummaryInfo(pFile, aucHeader);
		break;
	case 1:
	case 2:
		vGet2Stylesheet(pFile, iWordVersion, aucHeader);
		vGet2DopInfo(pFile, aucHeader);
		vGet2SepInfo(pFile, aucHeader);
		vGet2PapInfo(pFile, aucHeader);
		if (tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet2HdrFtrInfo(pFile, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_xml ||
		    tOptions.eConversionType == conversion_fmt_text ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet2ChrInfo(pFile, iWordVersion, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vCreate2FontTable(pFile, iWordVersion, aucHeader);
		}
		vSet2SummaryInfo(pFile, iWordVersion, aucHeader);
		break;
	case 4:
	case 5:
		break;
	case 6:
	case 7:
		vGet6Stylesheet(pFile, pPPS->tWordDocument.ulSB,
			aulBBD, tBBDLen, aucHeader);
		vGet6DopInfo(pFile, pPPS->tWordDocument.ulSB,
			aulBBD, tBBDLen, aucHeader);
		vGet6SepInfo(pFile, pPPS->tWordDocument.ulSB,
			aulBBD, tBBDLen, aucHeader);
		vGet6PapInfo(pFile, pPPS->tWordDocument.ulSB,
			aulBBD, tBBDLen, aucHeader);
		if (tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet6HdrFtrInfo(pFile, pPPS->tWordDocument.ulSB,
				aulBBD, tBBDLen, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_xml ||
		    tOptions.eConversionType == conversion_fmt_text ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet6ChrInfo(pFile, pPPS->tWordDocument.ulSB,
				aulBBD, tBBDLen, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vCreate6FontTable(pFile, pPPS->tWordDocument.ulSB,
				aulBBD, tBBDLen, aucHeader);
		}
		vSet6SummaryInfo(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		break;
	case 8:
		vGet8LstInfo(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		vGet8Stylesheet(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		vGet8DopInfo(pFile, &pPPS->tTable,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		vGet8SepInfo(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		vGet8PapInfo(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		if (tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet8HdrFtrInfo(pFile, &pPPS->tTable,
				aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_xml ||
		    tOptions.eConversionType == conversion_fmt_text ||
		    tOptions.eConversionType == conversion_pdf) {
			vGet8ChrInfo(pFile, pPPS,
				aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		}
		if (tOptions.eConversionType == conversion_draw ||
		    tOptions.eConversionType == conversion_ps ||
		    tOptions.eConversionType == conversion_pdf) {
			vCreate8FontTable(pFile, pPPS,
				aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		}
		vSet8SummaryInfo(pFile, pPPS,
			aulBBD, tBBDLen, aulSBD, tSBDLen, aucHeader);
		break;
	default:
		DBG_DEC(iWordVersion);
		DBG_FIXME();
		werr(0, "Sorry, no property information");
		break;
	}

	/* Temporarily: Correct the font table */
	vCorrectFontTable(tOptions.eConversionType, tOptions.eEncoding);
} /* end of vGetPropertyInfo */

/*
 * ePropMod2RowInfo - Turn the Property Modifier into row information
 *
 * Returns: the row information
 */
row_info_enum
ePropMod2RowInfo(USHORT usPropMod, int iWordVersion)
{
	row_block_type	tRow;
	const UCHAR	*aucPropMod;
	int	iLen;

	TRACE_MSG("ePropMod2RowInfo");

	aucPropMod = aucReadPropModListItem(usPropMod);
	if (aucPropMod == NULL) {
		return found_nothing;
	}
	iLen = (int)usGetWord(0, aucPropMod);

	switch (iWordVersion) {
	case 0:
		return found_nothing;
	case 1:
	case 2:
		return eGet2RowInfo(0, aucPropMod + 2, iLen, &tRow);
	case 4:
	case 5:
		return found_nothing;
	case 6:
	case 7:
		return eGet6RowInfo(0, aucPropMod + 2, iLen, &tRow);
	case 8:
		return eGet8RowInfo(0, aucPropMod + 2, iLen, &tRow);
	default:
		DBG_DEC(iWordVersion);
		DBG_FIXME();
		return found_nothing;
	}
} /* end of ePropMod2RowInfo */
