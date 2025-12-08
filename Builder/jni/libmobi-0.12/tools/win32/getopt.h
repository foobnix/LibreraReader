/** @file getopt.h
*
* Copyright (c) 2016 Bartek Fabiszewski
* http://www.fabiszewski.net
*
* Licensed under LGPL, either version 3, or any later.
* See <http://www.gnu.org/licenses/>
*/

/** Supply getopt header for Win32 builds */

#ifndef libmobi_getopt_h
#define libmobi_getopt_h

extern int opterr, optind, optopt, optreset;
extern char *optarg;

int getopt(int nargc, char *const nargv[], const char *ostr);

#endif
