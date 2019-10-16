/*
 * riscos.c
 * Copyright (C) 2001,2002 A.J. van Os; Released under GPL
 *
 * Description:
 * RISC OS only functions
 */

#include <string.h>
#include <stdlib.h>
#include <stdarg.h>
#include "DeskLib:Error.h"
#include "DeskLib:SWI.h"
#include "antiword.h"

#if !defined(DrawFile_Render)
#define DrawFile_Render		0x045540
#endif /* !DrawFile_Render */
#if !defined(JPEG_Info)
#define JPEG_Info		0x049980
#endif /* !JPEG_Info */

/*
 * werr - write an error message and exit if needed
 */
void
werr(int iFatal, const char *szFormat, ...)
{
	va_list tArg;

	va_start(tArg, szFormat);
	Error_Report(iFatal, (char *)szFormat, tArg);
	va_end(tArg);
	switch (iFatal) {
	case 0:		/* The message is just a warning, so no exit */
		return;
	case 1:		/* Fatal error with a standard exit */
		exit(EXIT_FAILURE);
	default:	/* Fatal error with a non-standard exit */
		exit(iFatal);
	}
} /* end of werr */

/*
 * iGetFiletype
 * This function will get the filetype of the given file.
 * returns the filetype.
 */
int
iGetFiletype(const char *szFilename)
{
	os_error	*e;
	int		iType;

	fail(szFilename == NULL || szFilename[0] == '\0');

	e = SWI(2, 7, SWI_OS_File | XOS_Bit,
		23, szFilename,
		NULL, NULL, NULL, NULL, NULL, NULL, &iType);
	if (e == NULL) {
		return iType;
	}
	werr(0, "Get Filetype error %d: %s", e->errnum, e->errmess);
	return -1;
} /* end of iGetFiletype */

/*
 * vSetFiletype
 * This procedure will set the filetype of the given file to the given
 * type.
 */
void
vSetFiletype(const char *szFilename, int iFiletype)
{
	os_error	*e;

	fail(szFilename == NULL || szFilename[0] == '\0');

	if (iFiletype < 0x000 || iFiletype > 0xfff) {
		return;
	}
	e = SWI(3, 0, SWI_OS_File | XOS_Bit,
		18, szFilename, iFiletype);
	if (e != NULL) {
		switch (e->errnum) {
		case 0x000113:	/* ROM */
		case 0x0104e1:	/* Read-only floppy DOSFS */
		case 0x0108c9:	/* Read-only floppy ADFS */
		case 0x013803:	/* Read-only ArcFS */
		case 0x80344a:	/* CD-ROM */
			break;
		default:
			werr(0, "Set Filetype error %d: %s",
				e->errnum, e->errmess);
			break;
		}
	}
} /* end of vSetFileType */

/*
 * Check if the directory part of the given file exists, make the directory
 * if it does not exist yet.
 * Returns TRUE in case of success, otherwise FALSE.
 */
BOOL
bMakeDirectory(const char *szFilename)
{
	os_error	*e;
	char	*pcLastDot;
	int	iObjectType;
	char	szDirectory[PATH_MAX+1];

	DBG_MSG("bMakeDirectory");
	fail(szFilename == NULL || szFilename[0] == '\0');
	DBG_MSG(szFilename);

	if (strlen(szFilename) >= sizeof(szDirectory)) {
		DBG_DEC(strlen(szFilename));
		return FALSE;
	}
	strcpy(szDirectory, szFilename);
	pcLastDot = strrchr(szDirectory, '.');
	if (pcLastDot == NULL) {
		/* No directory equals current directory */
		DBG_MSG("No directory part given");
		return TRUE;
	}
	*pcLastDot = '\0';
	DBG_MSG(szDirectory);
	/* Check if the name exists */
	e = SWI(2, 1, SWI_OS_File | XOS_Bit,
		17, szDirectory,
		&iObjectType);
	if (e != NULL) {
		werr(0, "Directory check %d: %s", e->errnum, e->errmess);
		return FALSE;
	}
	if (iObjectType == 2) {
		/* The name exists and it is a directory */
		DBG_MSG("The directory already exists");
		return TRUE;
	}
	if (iObjectType != 0) {
		/* The name exists and it is not a directory */
		DBG_DEC(iObjectType);
		return FALSE;
	}
	/* The name does not exist, make the directory */
	e = SWI(5, 0, SWI_OS_File | XOS_Bit,
		8, szDirectory, 0, 0, 0);
	if (e != NULL) {
		werr(0, "I can't make a directory %d: %s",
			e->errnum, e->errmess);
		return FALSE;
	}
	return TRUE;
} /* end of bMakeDirectory */

/*
 * iReadCurrentAlphabetNumber
 * This function reads the current Alphabet number.
 * Returns the current Alphabet number when successful, otherwise -1
 */
int
iReadCurrentAlphabetNumber(void)
{
	os_error	*e;
	int		iAlphabetNumber;

	e = SWI(2, 2, SWI_OS_Byte | XOS_Bit,
		71, 127,
		NULL, &iAlphabetNumber);
	if (e == NULL) {
		return iAlphabetNumber;
	}
	werr(0, "Read alphabet error %d: %s", e->errnum, e->errmess);
	return -1;
} /* end of iReadCurrentAlphabetNumber */

/*
 * iGetRiscOsVersion - get the RISC OS version number
 *
 * returns the RISC OS version * 100
 */
int
iGetRiscOsVersion(void)
{
	os_error	*e;
	int		iVersion;

	e = SWI(3, 2, SWI_OS_Byte | XOS_Bit,
		129, 0, 0xff,
		NULL, &iVersion);
	if (e != NULL) {
		werr(0, "Read RISC OS version error %d: %s",
			e->errnum, e->errmess);
		return 0;
	}
	switch (iVersion) {
	case 0xa0:	/* Arthur 1.20 */
		return 120;
	case 0xa1:	/* RISC OS 2.00 */
		return 200;
	case 0xa2:	/* RISC OS 2.01 */
		return 201;
	case 0xa3:	/* RISC OS 3.00 */
		return 300;
	case 0xa4:	/* RISC OS 3.1x */
		return 310;
	case 0xa5:	/* RISC OS 3.50 */
		return 350;
	case 0xa6:	/* RISC OS 3.60 */
		return 360;
	case 0xa7:	/* RISC OS 3.7x */
		return 370;
	case 0xa8:	/* RISC OS 4.0x */
		return 400;
	default:
		if (iVersion >= 0xa9 && iVersion <= 0xaf) {
			/* RISC OS 4.10 and up */
			return 410;
		}
		/* Unknown version */
		return 0;
	}
} /* end of iGetRiscOsVersion */

#if defined(DEBUG)
BOOL
bGetJpegInfo(UCHAR *pucJpeg, size_t tJpegSize)
{
	os_error	*e;
	int	iReg0, iReg4, iReg5;

	e = SWI(3, 6, JPEG_Info | XOS_Bit,
		0x00, pucJpeg, tJpegSize,
		&iReg0, NULL, NULL, NULL, &iReg4, &iReg5);
	if (e == NULL) {
		if (iReg0 & BIT(2)) {
			DBG_MSG("Pixel density is a simple ratio");
		} else {
			DBG_MSG("Pixel density is in dpi");
		}
		DBG_DEC(iReg4);
		DBG_DEC(iReg5);
		return TRUE;
	}
	werr(0, "JPEG Info error %d: %s", e->errnum, e->errmess);
	return FALSE;
} /* end of bGetJpegInfo */
#endif /* DEBUG */
