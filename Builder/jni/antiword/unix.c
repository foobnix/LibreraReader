/*
 * unix.c
 * Copyright (C) 1998-2000 A.J. van Os; Released under GPL
 *
 * Description:
 * Unix approximations of RISC-OS functions
 */

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include "antiword.h"


/*
 * werr - write an error message and exit if needed
 */
void
werr(int iFatal, const char *szFormat, ...)
{
	va_list tArg;

	va_start(tArg, szFormat);
	(void)vfprintf(stderr, szFormat, tArg);
	va_end(tArg);
	fprintf(stderr, "\n");
	switch (iFatal) {
	case 0:		/* The message is just a warning, so no exit */
		return;
	case 1:		/* Fatal error with a standard exit */
		exit(EXIT_FAILURE);
	default:	/* Fatal error with a non-standard exit */
		exit(iFatal);
	}
} /* end of werr */

void
Hourglass_On(void)
{
} /* end of Hourglass_On */

void
Hourglass_Off(void)
{
} /* end of Hourglass_Off */
