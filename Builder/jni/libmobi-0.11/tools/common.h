/** @file common.h
 *
 * Copyright (c) 2020 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef common_h
#define common_h

#include <stdio.h>
#include <sys/stat.h>

#ifdef HAVE_CONFIG_H
# include "../config.h"
#endif

#ifdef HAVE_GETOPT
# include <unistd.h>
#else
# include "win32/getopt.h"
#endif

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
extern bool outdir_opt;
extern char outdir[FILENAME_MAX];

const char * libmobi_msg(const MOBI_RET ret);
void split_fullpath(const char *fullpath, char *dirname, char *basename, const size_t buf_len);
int make_directory(const char *path);
int create_subdir(char *newdir, const size_t buf_len, const char *parent_dir, const char *subdir_name);
int write_file(const unsigned char *buffer, const size_t len, const char *path);
int write_to_dir(const char *dir, const char *name, const unsigned char *buffer, const size_t len);
bool dir_exists(const char *path);
void normalize_path(char *path);
void print_summary(const MOBIData *m);
void print_exth(const MOBIData *m);
int set_decryption_key(MOBIData *m, const char *serial, const char *pid);
int set_decryption_pid(MOBIData *m, const char *pid);
int set_decryption_serial(MOBIData *m, const char *serial);
int save_mobi(MOBIData *m, const char *fullpath, const char *suffix);
#endif /* common_h */
