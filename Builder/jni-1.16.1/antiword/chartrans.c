/*
 * chartrans.c
 * Copyright (C) 1999-2004 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Translate Word characters to local representation
 */

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#if defined(__STDC_ISO_10646__)
#include <wctype.h>
#endif /* __STDC_ISO_10646__ */
#include "antiword.h"

static const USHORT usCp850[] = {	/* DOS implementation of Latin1 */
	0x00c7, 0x00fc, 0x00e9, 0x00e2, 0x00e4, 0x00e0, 0x00e5, 0x00e7,
	0x00ea, 0x00eb, 0x00e8, 0x00ef, 0x00ee, 0x00ec, 0x00c4, 0x00c5,
	0x00c9, 0x00e6, 0x00c6, 0x00f4, 0x00f6, 0x00f2, 0x00fb, 0x00f9,
	0x00ff, 0x00d6, 0x00dc, 0x00f8, 0x00a3, 0x00d8, 0x00d7, 0x0192,
	0x00e1, 0x00ed, 0x00f3, 0x00fa, 0x00f1, 0x00d1, 0x00aa, 0x00ba,
	0x00bf, 0x00ae, 0x00ac, 0x00bd, 0x00bc, 0x00a1, 0x00ab, 0x00bb,
	0x2591, 0x2592, 0x2593, 0x2502, 0x2524, 0x00c1, 0x00c2, 0x00c0,
	0x00a9, 0x2563, 0x2551, 0x2557, 0x255d, 0x00a2, 0x00a5, 0x2510,
	0x2514, 0x2534, 0x252c, 0x251c, 0x2500, 0x253c, 0x00e3, 0x00c3,
	0x255a, 0x2554, 0x2569, 0x2566, 0x2560, 0x2550, 0x256c, 0x00a4,
	0x00f0, 0x00d0, 0x00ca, 0x00cb, 0x00c8, 0x0131, 0x00cd, 0x00ce,
	0x00cf, 0x2518, 0x250c, 0x2588, 0x2584, 0x00a6, 0x00cc, 0x2580,
	0x00d3, 0x00df, 0x00d4, 0x00d2, 0x00f5, 0x00d5, 0x00b5, 0x00fe,
	0x00de, 0x00da, 0x00db, 0x00d9, 0x00fd, 0x00dd, 0x00af, 0x00b4,
	0x00ad, 0x00b1, 0x2017, 0x00be, 0x00b6, 0x00a7, 0x00f7, 0x00b8,
	0x00b0, 0x00a8, 0x00b7, 0x00b9, 0x00b3, 0x00b2, 0x25a0, 0x00a0,
};

static const USHORT usCp1250[] = {	/* Windows implementation of Latin2 */
	0x20ac, 0x003f, 0x201a, 0x003f, 0x201e, 0x2026, 0x2020, 0x2021,
	0x003f, 0x2030, 0x0160, 0x2039, 0x015a, 0x0164, 0x017d, 0x0179,
	0x003f, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
	0x003f, 0x2122, 0x0161, 0x203a, 0x015b, 0x0165, 0x017e, 0x017a,
	0x00a0, 0x02c7, 0x02d8, 0x0141, 0x00a4, 0x0104, 0x00a6, 0x00a7,
	0x00a8, 0x00a9, 0x015e, 0x00ab, 0x00ac, 0x00ad, 0x00ae, 0x017b,
	0x00b0, 0x00b1, 0x02db, 0x0142, 0x00b4, 0x00b5, 0x00b6, 0x00b7,
	0x00b8, 0x0105, 0x015f, 0x00bb, 0x013d, 0x02dd, 0x013e, 0x017c,
	0x0154, 0x00c1, 0x00c2, 0x0102, 0x00c4, 0x0139, 0x0106, 0x00c7,
	0x010c, 0x00c9, 0x0118, 0x00cb, 0x011a, 0x00cd, 0x00ce, 0x010e,
	0x0110, 0x0143, 0x0147, 0x00d3, 0x00d4, 0x0150, 0x00d6, 0x00d7,
	0x0158, 0x016e, 0x00da, 0x0170, 0x00dc, 0x00dd, 0x0162, 0x00df,
	0x0155, 0x00e1, 0x00e2, 0x0103, 0x00e4, 0x013a, 0x0107, 0x00e7,
	0x010d, 0x00e9, 0x0119, 0x00eb, 0x011b, 0x00ed, 0x00ee, 0x010f,
	0x0111, 0x0144, 0x0148, 0x00f3, 0x00f4, 0x0151, 0x00f6, 0x00f7,
	0x0159, 0x016f, 0x00fa, 0x0171, 0x00fc, 0x00fd, 0x0163, 0x02d9,
};

static const USHORT usCp1251[] = {	/* Windows implementation of Cyrillic */
	0x0402, 0x0403, 0x201a, 0x0453, 0x201e, 0x2026, 0x2020, 0x2021,
	0x20ac, 0x2030, 0x0409, 0x2039, 0x040a, 0x040c, 0x040b, 0x040f,
	0x0452, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
	0x00f3, 0x2122, 0x0459, 0x203a, 0x045a, 0x045c, 0x045b, 0x045f,
	0x00a0, 0x040e, 0x045e, 0x0408, 0x00a4, 0x0490, 0x00a6, 0x00a7,
	0x0401, 0x00a9, 0x0404, 0x00ab, 0x00ac, 0x00ad, 0x00ae, 0x0407,
	0x00b0, 0x00b1, 0x0406, 0x0456, 0x0491, 0x00b5, 0x00b6, 0x00b7,
	0x0451, 0x2116, 0x0454, 0x00bb, 0x0458, 0x0405, 0x0455, 0x0457,
	0x0410, 0x0411, 0x0412, 0x0413, 0x0414, 0x0415, 0x0416, 0x0417,
	0x0418, 0x0419, 0x041a, 0x041b, 0x041c, 0x041d, 0x041e, 0x041f,
	0x0420, 0x0421, 0x0422, 0x0423, 0x0424, 0x0425, 0x0426, 0x0427,
	0x0428, 0x0429, 0x042a, 0x042b, 0x042c, 0x042d, 0x042e, 0x042f,
	0x0430, 0x0431, 0x0432, 0x0433, 0x0434, 0x0435, 0x0436, 0x0437,
	0x0438, 0x0439, 0x043a, 0x043b, 0x043c, 0x043d, 0x043e, 0x043f,
	0x0440, 0x0441, 0x0442, 0x0443, 0x0444, 0x0445, 0x0446, 0x0447,
	0x0448, 0x0449, 0x044a, 0x044b, 0x044c, 0x044d, 0x044e, 0x044f,
};

static const USHORT usCp1252[] = {	/* Windows implementation of Latin1 */
	0x20ac, 0x003f, 0x201a, 0x0192, 0x201e, 0x2026, 0x2020, 0x2021,
	0x02c6, 0x2030, 0x0160, 0x2039, 0x0152, 0x003f, 0x017d, 0x003f,
	0x003f, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
	0x02dc, 0x2122, 0x0161, 0x203a, 0x0153, 0x003f, 0x017e, 0x0178,
	0x00a0, 0x00a1, 0x00a2, 0x00a3, 0x00a4, 0x00a5, 0x00a6, 0x00a7,
	0x00a8, 0x00a9, 0x00aa, 0x00ab, 0x00ac, 0x00ad, 0x00ae, 0x00af,
	0x00b0, 0x00b1, 0x00b2, 0x00b3, 0x00b4, 0x00b5, 0x00b6, 0x00b7,
	0x00b8, 0x00b9, 0x00ba, 0x00bb, 0x00bc, 0x00bd, 0x00be, 0x00bf,
	0x00c0, 0x00c1, 0x00c2, 0x00c3, 0x00c4, 0x00c5, 0x00c6, 0x00c7,
	0x00c8, 0x00c9, 0x00ca, 0x00cb, 0x00cc, 0x00cd, 0x00ce, 0x00cf,
	0x00d0, 0x00d1, 0x00d2, 0x00d3, 0x00d4, 0x00d5, 0x00d6, 0x00d7,
	0x00d8, 0x00d9, 0x00da, 0x00db, 0x00dc, 0x00dd, 0x00de, 0x00df,
	0x00e0, 0x00e1, 0x00e2, 0x00e3, 0x00e4, 0x00e5, 0x00e6, 0x00e7,
	0x00e8, 0x00e9, 0x00ea, 0x00eb, 0x00ec, 0x00ed, 0x00ee, 0x00ef,
	0x00f0, 0x00f1, 0x00f2, 0x00f3, 0x00f4, 0x00f5, 0x00f6, 0x00f7,
	0x00f8, 0x00f9, 0x00fa, 0x00fb, 0x00fc, 0x00fd, 0x00fe, 0x00ff,
};

static const USHORT usMacRoman[] = {	/* Apple implementation of Latin1 */
	0x00c4, 0x00c5, 0x00c7, 0x00c9, 0x00d1, 0x00d6, 0x00dc, 0x00e1,
	0x00e0, 0x00e2, 0x00e4, 0x00e3, 0x00e5, 0x00e7, 0x00e9, 0x00e8,
	0x00ea, 0x00eb, 0x00ed, 0x00ec, 0x00ee, 0x00ef, 0x00f1, 0x00f3,
	0x00f2, 0x00f4, 0x00f6, 0x00f5, 0x00fa, 0x00f9, 0x00fb, 0x00fc,
	0x2020, 0x00b0, 0x00a2, 0x00a3, 0x00a7, 0x2022, 0x00b6, 0x00df,
	0x00ae, 0x00a9, 0x2122, 0x00b4, 0x00a8, 0x2260, 0x00c6, 0x00d8,
	0x221e, 0x00b1, 0x2264, 0x2265, 0x00a5, 0x00b5, 0x2202, 0x2211,
	0x220f, 0x03c0, 0x222b, 0x00aa, 0x00ba, 0x2126, 0x00e6, 0x00f8,
	0x00bf, 0x00a1, 0x00ac, 0x221a, 0x0192, 0x2248, 0x2206, 0x00ab,
	0x00bb, 0x2026, 0x00a0, 0x00c0, 0x00c3, 0x00d5, 0x0152, 0x0153,
	0x2013, 0x2014, 0x201c, 0x201d, 0x2018, 0x2019, 0x00f7, 0x25ca,
	0x00ff, 0x0178, 0x2044, 0x00a4, 0x2039, 0x203a, 0xfb01, 0xfb02,
	0x2021, 0x00b7, 0x201a, 0x201e, 0x2030, 0x00c2, 0x00ca, 0x00c1,
	0x00cb, 0x00c8, 0x00cd, 0x00ce, 0x00cf, 0x00cc, 0x00d3, 0x00d4,
	0x003f, 0x00d2, 0x00da, 0x00db, 0x00d9, 0x0131, 0x02c6, 0x02dc,
	0x00af, 0x02d8, 0x02d9, 0x02da, 0x00b8, 0x02dd, 0x02db, 0x02c7,
};

static const USHORT usPrivateArea[] = {
	0x0020, 0x0021, 0x2200, 0x0023, 0x2203, 0x0025, 0x0026, 0x220d,
	0x0028, 0x0029, 0x2217, 0x002b, 0x002c, 0x2212, 0x002e, 0x002f,
	0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
	0x0038, 0x0039, 0x003a, 0x003b, 0x003c, 0x2019, 0x003e, 0x003f,
	0x201d, 0x201c, 0x0392, 0x03a7, 0x0394, 0x0395, 0x03a6, 0x0393,
	0x0397, 0x0399, 0x03d1, 0x039a, 0x039b, 0x039c, 0x039d, 0x039f,
	0x03a0, 0x0398, 0x03a1, 0x03a3, 0x03a4, 0x03a5, 0x03c2, 0x03a9,
	0x039e, 0x03a8, 0x0396, 0x005b, 0x2234, 0x005d, 0x22a5, 0x005f,
	0x003f, 0x03b1, 0x03b2, 0x03c7, 0x03b4, 0x03b5, 0x03c6, 0x03b3,
	0x03b7, 0x03b9, 0x03d5, 0x03ba, 0x03bb, 0x03bc, 0x03bd, 0x03bf,
	0x03c0, 0x03b8, 0x03c1, 0x03c3, 0x03c4, 0x03c5, 0x03d6, 0x03c9,
	0x03be, 0x03c8, 0x03b6, 0x007b, 0x007c, 0x007d, 0x223c, 0x003f,
	0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f,
	0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f,
	0x003f, 0x003f, 0x003f, 0x2022, 0x003f, 0x003f, 0x003f, 0x003f,
	0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f, 0x003f,
	0x20ac, 0x03d2, 0x2032, 0x2264, 0x2044, 0x221e, 0x0192, 0x2663,
	0x2666, 0x2665, 0x2660, 0x2194, 0x2190, 0x2191, 0x2192, 0x2193,
	0x00b0, 0x00b1, 0x2033, 0x2265, 0x00d7, 0x221d, 0x2202, 0x2022,
	0x00f7, 0x2260, 0x2261, 0x2248, 0x2026, 0x007c, 0x23af, 0x21b5,
	0x2135, 0x2111, 0x211c, 0x2118, 0x2297, 0x2295, 0x2205, 0x2229,
	0x222a, 0x2283, 0x2287, 0x2284, 0x2282, 0x2286, 0x2208, 0x2209,
	0x2220, 0x2207, 0x00ae, 0x00a9, 0x2122, 0x220f, 0x221a, 0x22c5,
	0x00ac, 0x2227, 0x2228, 0x21d4, 0x21d0, 0x21d1, 0x21d2, 0x21d3,
	0x22c4, 0x3008, 0x00ae, 0x00a9, 0x2122, 0x2211, 0x239b, 0x239c,
	0x239d, 0x23a1, 0x23a2, 0x23a3, 0x23a7, 0x23a8, 0x23a9, 0x23aa,
	0x003f, 0x3009, 0x222b, 0x2320, 0x23ae, 0x2321, 0x239e, 0x239f,
	0x23a0, 0x23a4, 0x23a5, 0x23a6, 0x23ab, 0x23ac, 0x23ad, 0x003f,
};

typedef struct char_table_tag {
	UCHAR	ucLocal;
	USHORT	usUnicode;
} char_table_type;

static char_table_type	atCharTable[256];
static size_t		tNextPosFree = 0;


/*
 * iCompare - compare two records
 *
 * Compares two records. For use by qsort(3C) and bsearch(3C).
 *
 * returns -1 if rec1 < rec2, 0 if rec1 == rec2, 1 if rec1 > rec2
 */
static int
iCompare(const void *pvRecord1, const void *pvRecord2)
{
	USHORT	usUnicode1, usUnicode2;

	usUnicode1 = ((char_table_type *)pvRecord1)->usUnicode;
	usUnicode2 = ((char_table_type *)pvRecord2)->usUnicode;

	if (usUnicode1 < usUnicode2) {
		return -1;
	}
	if (usUnicode1 > usUnicode2) {
		return 1;
	}
	return 0;
} /* end of iCompare */

/*
 * pGetCharTableRecord - get the character table record
 *
 * returns a pointer to the record when found, otherwise NULL
 */
static const char_table_type *
pGetCharTableRecord(USHORT usUnicode)
{
	char_table_type	tKey;

	if (tNextPosFree == 0) {
		return NULL;
	}
	tKey.usUnicode = usUnicode;
	tKey.ucLocal = 0;
	return (char_table_type *)bsearch(&tKey,
			atCharTable,
			tNextPosFree, sizeof(atCharTable[0]),
			iCompare);
} /* end of pGetCharTableRecord */

/*
 * ucGetBulletCharacter - get the local representation of the bullet
 */
UCHAR
ucGetBulletCharacter(conversion_type eConversionType, encoding_type eEncoding)
{
#if defined(__riscos)
	return 0x8f;
#else
	const char_table_type	*pRec;

	fail(eEncoding == encoding_utf_8);

	if (eEncoding == encoding_latin_1 &&
	    (eConversionType == conversion_ps ||
	     eConversionType == conversion_pdf)) {
		/* Ugly, but it makes the PostScript and PDF look better */
		return (UCHAR)143;
	}
	if (eConversionType != conversion_text &&
	    eConversionType != conversion_fmt_text) {
		pRec = pGetCharTableRecord(UNICODE_BULLET);
		if (pRec != NULL) {
			return pRec->ucLocal;
		}
		pRec = pGetCharTableRecord(UNICODE_BULLET_OPERATOR);
		if (pRec != NULL) {
			return pRec->ucLocal;
		}
		pRec = pGetCharTableRecord(UNICODE_MIDDLE_DOT);
		if (pRec != NULL) {
			return pRec->ucLocal;
		}
	}
	return (UCHAR)'.';
#endif /* __riscos */
} /* end of ucGetBulletCharacter */

/*
 * ucGetNbspCharacter - get the local representation of the non-breaking space
 */
UCHAR
ucGetNbspCharacter(void)
{
	const char_table_type	*pRec;

	pRec = pGetCharTableRecord(0x00a0);	/* Unicode non-breaking space */
	if (pRec == NULL) {
		DBG_MSG("Non-breaking space record not found");
		/* No value found, use the best guess */
		return (UCHAR)0xa0;
	}
	return pRec->ucLocal;
} /* end of ucGetNbspCharacter */

/*
 * bReadCharacterMappingTable - read the mapping table
 *
 * Read the character mapping table from file and have the contents sorted
 *
 * returns TRUE if successful, otherwise FALSE
 */
BOOL
bReadCharacterMappingTable(FILE *pFile)
{
	char	*pcTmp;
	ULONG	ulUnicode;
	UINT	uiLocal;
	int	iFields;
	char	szLine[81];

	if (pFile == NULL) {
		return FALSE;
	}

	/* Clean the table first */
	(void)memset(atCharTable, 0, sizeof(atCharTable));

	/* Fill the table */
	while (fgets(szLine, (int)sizeof(szLine), pFile)) {
		if (szLine[0] == '#' ||
		    szLine[0] == '\r' ||
		    szLine[0] == '\n') {
			/* Comment or empty line */
			continue;
		}
		iFields = sscanf(szLine, "%x %lx %*s", &uiLocal, &ulUnicode);
		if (iFields != 2) {
			pcTmp = strchr(szLine, '\r');
			if (pcTmp != NULL) {
				*pcTmp = '\0';
			}
			pcTmp = strchr(szLine, '\n');
			if (pcTmp != NULL) {
				*pcTmp = '\0';
			}
			werr(0, "Syntax error in: '%s'", szLine);
			continue;
		}
		if (uiLocal > 0xff || ulUnicode > 0xffff) {
			werr(0, "Syntax error in: '%02x %04lx'",
					uiLocal, ulUnicode);
			continue;
		}
		/* Store only the relevant entries */
		if (uiLocal != ulUnicode || uiLocal >= 0x80) {
			atCharTable[tNextPosFree].ucLocal = (UCHAR)uiLocal;
			atCharTable[tNextPosFree].usUnicode = (USHORT)ulUnicode;
			tNextPosFree++;
		}
		if (tNextPosFree >= elementsof(atCharTable)) {
			werr(0, "Too many entries in the character mapping "
				"file. Ignoring the rest.");
			break;
		}
	}

	if (tNextPosFree != 0) {
		DBG_HEX(atCharTable[0].usUnicode);
		DBG_HEX(atCharTable[tNextPosFree - 1].usUnicode);

		qsort(atCharTable,
			tNextPosFree, sizeof(atCharTable[0]),
			iCompare);

		DBG_HEX(atCharTable[0].usUnicode);
		DBG_HEX(atCharTable[tNextPosFree - 1].usUnicode);
	}

	return TRUE;
} /* end of bReadCharacterMappingTable */

/*
 * ulTranslateCharacters - Translate characters to local representation
 *
 * Translate all characters to local representation
 *
 * returns the translated character
 */
ULONG
ulTranslateCharacters(USHORT usChar, ULONG ulFileOffset, int iWordVersion,
	conversion_type eConversionType, encoding_type eEncoding,
	BOOL bUseMacCharSet)
{
	const char_table_type	*pTmp;
	const USHORT	*usCharSet;

	usCharSet = NULL;
	if (bUseMacCharSet) {
		/* Macintosh character set */
		usCharSet = usMacRoman;
	} else if (iWordVersion == 0) {
		/* DOS character set */
		usCharSet = usCp850;
	} else {
		/* Windows character set */
		switch (eEncoding) {
		case encoding_latin_2:
			usCharSet = usCp1250;
			break;
		case encoding_cyrillic:
			usCharSet = usCp1251;
			break;
		case encoding_latin_1:
		default:
			usCharSet = usCp1252;
			break;
		}
	}
	fail(usCharSet == NULL);
	if (usChar >= 0x80 && usChar <= 0x9f) {
		/* Translate implementation defined characters */
		usChar = usCharSet[usChar - 0x80];
	} else if (iWordVersion < 8 && usChar >= 0xa0 && usChar <= 0xff) {
		/* Translate old character set to Unixcode */
		usChar = usCharSet[usChar - 0x80];
	}

	/* Microsoft Unicode to real Unicode */
	if (usChar >= 0xf020 && usChar <= 0xf0ff) {
		DBG_HEX_C(usPrivateArea[usChar - 0xf020] == 0x003f, usChar);
		usChar = usPrivateArea[usChar - 0xf020];
	}

	/* Characters with a special meaning in Word */
	switch (usChar) {
	case IGNORE_CHARACTER:
	case FOOTNOTE_SEPARATOR:
	case FOOTNOTE_CONTINUATION:
	case ANNOTATION:
	case FRAME:
	case LINE_FEED:
	case WORD_SOFT_HYPHEN:
	case UNICODE_HYPHENATION_POINT:
		return IGNORE_CHARACTER;
	case PICTURE:
	case TABLE_SEPARATOR:
	case TAB:
	case HARD_RETURN:
	case PAGE_BREAK:
	case PAR_END:
	case COLUMN_FEED:
		return (ULONG)usChar;
	case FOOTNOTE_OR_ENDNOTE:
		NO_DBG_HEX(ulFileOffset);
		switch (eGetNotetype(ulFileOffset)) {
		case notetype_is_footnote:
			return FOOTNOTE_CHAR;
		case notetype_is_endnote:
			return ENDNOTE_CHAR;
		default:
			return UNKNOWN_NOTE_CHAR;
		}
	case WORD_UNBREAKABLE_JOIN:
		return (ULONG)OUR_UNBREAKABLE_JOIN;
	default:
		break;
	}

	if (eEncoding != encoding_utf_8) {
		/* Latin characters in an oriental text */
		if (usChar >= 0xff01 && usChar <= 0xff5e) {
			usChar -= 0xfee0;
		}
	}

	if (eEncoding == encoding_latin_1 &&
	    (eConversionType == conversion_ps ||
	     eConversionType == conversion_pdf)) {
		/* Ugly, but it makes the PostScript and PDF look better */
		switch (usChar) {
		case UNICODE_ELLIPSIS:
			return 140;
		case UNICODE_TRADEMARK_SIGN:
			return 141;
		case UNICODE_PER_MILLE_SIGN:
			return 142;
		case UNICODE_BULLET:
		case UNICODE_BULLET_OPERATOR:
		case UNICODE_BLACK_CLUB_SUIT:
			return 143;
		case UNICODE_LEFT_SINGLE_QMARK:
			return 144;
		case UNICODE_RIGHT_SINGLE_QMARK:
			return 145;
		case UNICODE_SINGLE_LEFT_ANGLE_QMARK:
			return 146;
		case UNICODE_SINGLE_RIGHT_ANGLE_QMARK:
			return 147;
		case UNICODE_LEFT_DOUBLE_QMARK:
			return 148;
		case UNICODE_RIGHT_DOUBLE_QMARK:
			return 149;
		case UNICODE_DOUBLE_LOW_9_QMARK:
			return 150;
		case UNICODE_EN_DASH:
			return 151;
		case UNICODE_EM_DASH:
			return 152;
		case UNICODE_MINUS_SIGN:
			return 153;
		case UNICODE_CAPITAL_LIGATURE_OE:
			return 154;
		case UNICODE_SMALL_LIGATURE_OE:
			return 155;
		case UNICODE_DAGGER:
			return 156;
		case UNICODE_DOUBLE_DAGGER:
			return 157;
		case UNICODE_SMALL_LIGATURE_FI:
			return 158;
		case UNICODE_SMALL_LIGATURE_FL:
			return 159;
		default:
			break;
		}
	}

	if (eConversionType == conversion_pdf) {
		if (eEncoding == encoding_latin_1) {
			switch (usChar) {
			case UNICODE_EURO_SIGN:
				return 128;
			default:
				break;
			}
		} else if (eEncoding == encoding_latin_2) {
			switch (usChar) {
			case UNICODE_CAPITAL_D_WITH_STROKE:
			case UNICODE_SMALL_D_WITH_STROKE:
				return 0x3f;
			default:
				break;
			}
		}
	}

	if (usChar < 0x80) {
		/* US ASCII */
		if (usChar < 0x20 || usChar == 0x7f) {
			/* Ignore control characters */
			DBG_HEX(usChar);
			DBG_FIXME();
			return IGNORE_CHARACTER;
		}
		return (ULONG)usChar;
	}

	if (eEncoding == encoding_utf_8) {
		/* No need to convert Unicode characters */
		return (ULONG)usChar;
	}

	/* Unicode to local representation */
	pTmp = pGetCharTableRecord(usChar);
	if (pTmp != NULL) {
		DBG_HEX_C(usChar >= 0x7f && usChar <= 0x9f, usChar);
		return (ULONG)pTmp->ucLocal;
	}

	/* Fancy characters to simple US ASCII */
	switch (usChar) {
	case UNICODE_SMALL_F_HOOK:
		return (ULONG)'f';
	case UNICODE_GREEK_CAPITAL_CHI:
		return (ULONG)'X';
	case UNICODE_GREEK_SMALL_UPSILON:
		return (ULONG)'v';
	case UNICODE_MODIFIER_CIRCUMFLEX:
	case UNICODE_UPWARDS_ARROW:
		return (ULONG)'^';
	case UNICODE_SMALL_TILDE:
	case UNICODE_TILDE_OPERATOR:
		return (ULONG)'~';
	case UNICODE_EN_QUAD:
	case UNICODE_EM_QUAD:
	case UNICODE_EN_SPACE:
	case UNICODE_EM_SPACE:
	case UNICODE_THREE_PER_EM_SPACE:
	case UNICODE_FOUR_PER_EM_SPACE:
	case UNICODE_SIX_PER_EM_SPACE:
	case UNICODE_FIGURE_SPACE:
	case UNICODE_PUNCTUATION_SPACE:
	case UNICODE_THIN_SPACE:
	case UNICODE_NARROW_NO_BREAK_SPACE:
	case UNICODE_LIGHT_SHADE:
	case UNICODE_MEDIUM_SHADE:
	case UNICODE_DARK_SHADE:
		return (ULONG)' ';
	case UNICODE_LEFT_DOUBLE_QMARK:
	case UNICODE_RIGHT_DOUBLE_QMARK:
	case UNICODE_DOUBLE_LOW_9_QMARK:
	case UNICODE_DOUBLE_HIGH_REV_9_QMARK:
	case UNICODE_DOUBLE_PRIME:
		return (ULONG)'"';
	case UNICODE_LEFT_SINGLE_QMARK:
	case UNICODE_RIGHT_SINGLE_QMARK:
	case UNICODE_SINGLE_LOW_9_QMARK:
	case UNICODE_SINGLE_HIGH_REV_9_QMARK:
	case UNICODE_PRIME:
		return (ULONG)'\'';
	case UNICODE_HYPHEN:
	case UNICODE_NON_BREAKING_HYPHEN:
	case UNICODE_FIGURE_DASH:
	case UNICODE_EN_DASH:
	case UNICODE_EM_DASH:
	case UNICODE_HORIZONTAL_BAR:
	case UNICODE_MINUS_SIGN:
	case UNICODE_BD_LIGHT_HORIZONTAL:
	case UNICODE_BD_DOUBLE_HORIZONTAL:
		return (ULONG)'-';
	case UNICODE_DOUBLE_VERTICAL_LINE:
	case UNICODE_BD_LIGHT_VERTICAL:
	case UNICODE_BD_DOUBLE_VERTICAL:
		return (ULONG)'|';
	case UNICODE_DOUBLE_LOW_LINE:
		return (ULONG)'_';
	case UNICODE_DAGGER:
		return (ULONG)'+';
	case UNICODE_DOUBLE_DAGGER:
		return (ULONG)'#';
	case UNICODE_BULLET:
	case UNICODE_BULLET_OPERATOR:
	case UNICODE_BLACK_CLUB_SUIT:
		return (ULONG)ucGetBulletCharacter(eConversionType, eEncoding);
	case UNICODE_ONE_DOT_LEADER:
	case UNICODE_TWO_DOT_LEADER:
		return (ULONG)'.';
	case UNICODE_ELLIPSIS:
#if defined(__riscos)
		return (ULONG)OUR_ELLIPSIS;
#else
		if (ulFileOffset == 0) {
			return (ULONG)OUR_ELLIPSIS;
		}
		return UNICODE_ELLIPSIS;
#endif /* __riscos */
	case UNICODE_DOUBLE_LEFT_ANGLE_QMARK:
	case UNICODE_TRIANGULAR_BULLET:
	case UNICODE_SINGLE_LEFT_ANGLE_QMARK:
	case UNICODE_LEFTWARDS_ARROW:
		return (ULONG)'<';
	case UNICODE_DOUBLE_RIGHT_ANGLE_QMARK:
	case UNICODE_SINGLE_RIGHT_ANGLE_QMARK:
	case UNICODE_RIGHTWARDS_ARROW:
		return (ULONG)'>';
	case UNICODE_UNDERTIE:
		return (ULONG)'-';
	case UNICODE_N_ARY_SUMMATION:
		return (ULONG)'S';
	case UNICODE_EURO_SIGN:
		return (ULONG)'E';
	case UNICODE_CIRCLE:
	case UNICODE_SQUARE:
		return (ULONG)'O';
	case UNICODE_DIAMOND:
		return (ULONG)OUR_DIAMOND;
	case UNICODE_NUMERO_SIGN:
		return (ULONG)'N';
	case UNICODE_KELVIN_SIGN:
		return (ULONG)'K';
	case UNICODE_DOWNWARDS_ARROW:
		return (ULONG)'v';
	case UNICODE_FRACTION_SLASH:
	case UNICODE_DIVISION_SLASH:
		return (ULONG)'/';
	case UNICODE_ASTERISK_OPERATOR:
		return (ULONG)'*';
	case UNICODE_RATIO:
		return (ULONG)':';
	case UNICODE_BD_LIGHT_DOWN_RIGHT:
	case UNICODE_BD_LIGHT_DOWN_AND_LEFT:
	case UNICODE_BD_LIGHT_UP_AND_RIGHT:
	case UNICODE_BD_LIGHT_UP_AND_LEFT:
	case UNICODE_BD_LIGHT_VERTICAL_AND_RIGHT:
	case UNICODE_BD_LIGHT_VERTICAL_AND_LEFT:
	case UNICODE_BD_LIGHT_DOWN_AND_HORIZONTAL:
	case UNICODE_BD_LIGHT_UP_AND_HORIZONTAL:
	case UNICODE_BD_LIGHT_VERTICAL_AND_HORIZONTAL:
	case UNICODE_BD_DOUBLE_DOWN_AND_RIGHT:
	case UNICODE_BD_DOUBLE_DOWN_AND_LEFT:
	case UNICODE_BD_DOUBLE_UP_AND_RIGHT:
	case UNICODE_BD_DOUBLE_UP_AND_LEFT:
	case UNICODE_BD_DOUBLE_VERTICAL_AND_RIGHT:
	case UNICODE_BD_DOUBLE_VERTICAL_AND_LEFT:
	case UNICODE_BD_DOUBLE_DOWN_AND_HORIZONTAL:
	case UNICODE_BD_DOUBLE_UP_AND_HORIZONTAL:
	case UNICODE_BD_DOUBLE_VERTICAL_AND_HORIZONTAL:
	case UNICODE_BLACK_SQUARE:
		return (ULONG)'+';
	case UNICODE_HAIR_SPACE:
	case UNICODE_ZERO_WIDTH_SPACE:
	case UNICODE_ZERO_WIDTH_NON_JOINER:
	case UNICODE_ZERO_WIDTH_JOINER:
	case UNICODE_LEFT_TO_RIGHT_MARK:
	case UNICODE_RIGHT_TO_LEFT_MARK:
	case UNICODE_LEFT_TO_RIGHT_EMBEDDING:
	case UNICODE_RIGHT_TO_LEFT_EMBEDDING:
	case UNICODE_POP_DIRECTIONAL_FORMATTING:
	case UNICODE_LEFT_TO_RIGHT_OVERRIDE:
	case UNICODE_RIGHT_TO_LEFT_OVERRIDE:
	case UNICODE_ZERO_WIDTH_NO_BREAK_SPACE:
		return IGNORE_CHARACTER;
	default:
		break;
	}

	if (usChar == UNICODE_TRADEMARK_SIGN) {
		/*
		 * No local representation, it doesn't look like anything in
		 * US-ASCII and a question mark does more harm than good.
		 */
		return IGNORE_CHARACTER;
	}

	if (usChar >= 0xa0 && usChar <= 0xff) {
		/* Before Word 97, Word did't use Unicode */
		return (ULONG)usChar;
	}

	DBG_HEX_C(usChar < 0x3000 || usChar >= 0xd800, ulFileOffset);
	DBG_HEX_C(usChar < 0x3000 || usChar >= 0xd800, usChar);
	DBG_MSG_C(usChar >= 0xe000 && usChar < 0xf900, "Private Use Area");

	/* Untranslated Unicode character */
	return 0x3f;
} /* end of ulTranslateCharacters */

/*
 * ulToUpper - convert letter to upper case
 *
 * This function converts a letter to upper case. Unlike toupper(3) this
 * function is independent from the settings of locale. This comes in handy
 * for people who have to read Word documents in more than one language or
 * contain more than one language.
 *
 * returns the converted letter, or ulChar if the conversion was not possible.
 */
ULONG
ulToUpper(ULONG ulChar)
{
	if (ulChar < 0x80) {
		/* US ASCII: use standard function */
		return (ULONG)toupper((int)ulChar);
	}
	if (ulChar >= 0xe0 && ulChar <= 0xfe && ulChar != 0xf7) {
		/*
		 * Lower case accented characters
		 * 0xf7 is Division sign; 0xd7 is Multiplication sign
		 * 0xff is y with diaeresis; 0xdf is Sharp s
		 */
		return ulChar & ~0x20;
	}
#if defined(__STDC_ISO_10646__)
	/*
	 * If this is ISO C99 and all locales have wchar_t = ISO 10646
	 * (e.g., glibc 2.2 or newer), then use standard function
	 */
	if (ulChar > 0xff) {
		return (ULONG)towupper((wint_t)ulChar);
	}
#endif /* __STDC_ISO_10646__ */
	return ulChar;
} /* end of ulToUpper */
