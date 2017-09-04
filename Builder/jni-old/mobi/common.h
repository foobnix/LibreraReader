/** @file common.h
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef common_h
#define common_h

#include <stdio.h>
#include <sys/stat.h>

/* return codes */
#define ERROR 1
#define SUCCESS 0

#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

#define ARRAYSIZE(arr) (sizeof(arr) / sizeof(arr[0]))

#if defined(__clang__)
# define COMPILER "clang " __VERSION__
#elif defined(__SUNPRO_C)
# define COMPILER "suncc " STR(__SUNPRO_C)
#elif defined(__GNUC__)
# if (defined(__MINGW32__) || defined(__MINGW64__))
#  define COMPILER "gcc (MinGW) " __VERSION__
# else
#  define COMPILER "gcc " __VERSION__
# endif
#elif defined(_MSC_VER)
# define COMPILER "MSVC++ " STR(_MSC_VER)
#else
# define COMPILER "unknown"
#endif

#if !defined S_ISDIR && defined S_IFDIR
# define S_ISDIR(m) (((m) & S_IFMT) == S_IFDIR)
#endif
#ifndef S_ISDIR
# error "At least one of S_ISDIR or S_IFDIR macros is required"
#endif

#define FULLNAME_MAX 1024

extern const char separator;
const char * libmobi_msg(const MOBI_RET ret);
int mt_mkdir(const char *filename);
void split_fullpath(const char *fullpath, char *dirname, char *basename);
bool dir_exists(const char *path);
void print_summary(const MOBIData *m);
void print_exth(const MOBIData *m);
int set_decryption_key(MOBIData *m, const char *serial, const char *pid);

#endif /* common_h */
