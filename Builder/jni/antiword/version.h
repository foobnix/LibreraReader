/*
 * version.h
 * Copyright (C) 1998-2005 A.J. van Os; Released under GNU GPL
 *
 * Description:
 * Version and release information
 */

#if !defined(__version_h)
#define __version_h 1

/* Strings for the info box */
#define PURPOSESTRING	"Display MS-Word files"

#if defined(__riscos)
#define AUTHORSTRING	"© 1998-2005 Adri van Os"
#else
#define AUTHORSTRING	"(C) 1998-2005 Adri van Os"
#endif /* __riscos */

#define VERSIONSTRING	"0.37  (21 Oct 2005)"

#if defined(__dos)
#if defined(__DJGPP__)
#define VERSIONSTRING2	" # 32-bit Protected Mode"
#else
#define VERSIONSTRING2	" # 16-bit Real Mode"
#endif /* __DJGPP__ */
#endif /* __dos */

#if defined(DEBUG)
#define STATUSSTRING	"DEBUG version"
#else
#define STATUSSTRING	"GNU General Public License"
#endif /* DEBUG */

#endif /* __version_h */
