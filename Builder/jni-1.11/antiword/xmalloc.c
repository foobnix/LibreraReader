/*
 * xmalloc.c
 * Copyright (C) 1998-2005 A.J. van Os
 *
 * Description:
 * Extended malloc and friends
 */

#include <stdlib.h>
#include <string.h>
#include "antiword.h"

static char *szMessage =
	"Memory allocation failed, unable to continue";
#if defined(__dos) && !defined(__DJGPP__)
static char *szDosMessage =
	"DOS can't allocate this kind of memory, unable to continue";
#endif /* __dos && !__DJGPP__ */


/*
 * xmalloc - Allocates dynamic memory
 *
 * See malloc(3), but unlike malloc(3) xmalloc does not return in case
 * of error.
 */
void *
xmalloc(size_t tSize)
{
	void	*pvTmp;

	TRACE_MSG("xmalloc");

	if (tSize == 0) {
		tSize = 1;
	}
	pvTmp = malloc(tSize);
	if (pvTmp == NULL) {
		DBG_MSG("xmalloc returned NULL");
		DBG_DEC(tSize);
		werr(1, szMessage);
	}
	return pvTmp;
} /* end of xmalloc */

/*
 * xcalloc - Allocates and zeros dynamic memory
 *
 * See calloc(3), but unlike calloc(3) xcalloc does not return in case of error
 */
void *
xcalloc(size_t tNmemb, size_t tSize)
{
	void	*pvTmp;

	TRACE_MSG("xcalloc");

#if defined(__dos) && !defined(__DJGPP__)
	if ((ULONG)tNmemb * (ULONG)tSize > 0xffffUL) {
		DBG_DEC((ULONG)tNmemb * (ULONG)tSize);
		werr(1, szDosMessage);
	}
#endif /* __dos && !__DJGPP__ */

	if (tNmemb == 0 || tSize == 0) {
		tNmemb = 1;
		tSize = 1;
	}
	pvTmp = calloc(tNmemb, tSize);
	if (pvTmp == NULL) {
		DBG_MSG("xcalloc returned NULL");
		werr(1, szMessage);
	}
	return pvTmp;
} /* end of xcalloc */

/*
 * xrealloc - Changes the size of a memory object
 *
 * See realloc(3), but unlike realloc(3) xrealloc does not return in case
 * of error.
 */
void *
xrealloc(void *pvArg, size_t tSize)
{
	void	*pvTmp;

	TRACE_MSG("xrealloc");

	pvTmp = realloc(pvArg, tSize);
	if (pvTmp == NULL) {
		DBG_MSG("realloc returned NULL");
		werr(1, szMessage);
	}
	return pvTmp;
} /* end of xrealloc */

/*
 * xstrdup - Duplicate a string
 *
 * See strdup(3), but unlike strdup(3) xstrdup does not return in case
 * of error.
 *
 * NOTE:
 * Does not use strdup(3), because some systems don't have it.
 */
char *
xstrdup(const char *szArg)
{
	char	*szTmp;

	TRACE_MSG("xstrdup");

	szTmp = xmalloc(strlen(szArg) + 1);
	strcpy(szTmp, szArg);
	return szTmp;
} /* end of xstrdup */

/*
 * xfree - Deallocates dynamic memory
 *
 * See free(3).
 *
 * returns NULL;
 * This makes p=xfree(p) possible, free memory and overwrite the pointer to it.
 */
void *
xfree(void *pvArg)
{
	TRACE_MSG("xfree");

	if (pvArg != NULL) {
		free(pvArg);
	}
	return NULL;
} /* end of xfree */
