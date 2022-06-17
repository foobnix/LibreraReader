/*
 * fail.h
 * Copyright (C) 1998-2000 A.J. van Os; Released under GPL
 *
 * Description:
 * Support for an alternative form of assert()
 */

#if !defined(__fail_h)
#define __fail_h 1

#undef fail

#if defined(NDEBUG)
#define fail(e)	((void)0)
#else
#define fail(e)	((e) ? __fail(#e, __FILE__, __LINE__) : (void)0)
#endif /* NDEBUG */

extern void	__fail(char *, char *, int);

#endif /* __fail_h */
