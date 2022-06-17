/*
 * fail.c
 * Copyright (C) 1998 A.J. van Os
 *
 * Description:
 * An alternative form of assert()
 */

#include <stdlib.h>
#include "antiword.h"

#if !defined(NDEBUG)
void
__fail(char *szExpression, char *szFilename, int iLineNumber)
{
	if (szExpression == NULL || szFilename == NULL) {
		werr(1, "Internal error: no expression");
	}
#if defined(DEBUG)
	fprintf(stderr, "%s[%3d]: Internal error in '%s'\n",
		szFilename, iLineNumber, szExpression);
#endif /* DEBUG */
	werr(1, "Internal error in '%s' in file %s at line %d",
		szExpression, szFilename, iLineNumber);
} /* end of __fail */
#endif /* !NDEBUG */
