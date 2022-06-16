# -*-Shell-script-*-
#
# DjVuLibre-3.5
# Copyright (c) 2002  Leon Bottou and Yann Le Cun.
# Copyright (c) 2001  AT&T
#
# This software is subject to, and may be distributed under, the
# GNU General Public License, either Version 2 of the license,
# or (at your option) any later version. The license should have
# accompanied the software or you may obtain a copy of the license
# from the Free Software Foundation at http://www.fsf.org .
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# DjVuLibre-3.5 is derived from the DjVu(r) Reference Library
# distribued by Lizardtech Software.
#
# ----------------------------------------
# Initialization
# ----------------------------------------

m4_define([v_maj], [3])
m4_define([v_min], [5])
m4_define([v_mic], [28])
m4_define([v_ver], [v_maj.v_min.v_mic])

m4_define([lt_compat], [21])
m4_define([lt_cur], [v_mic])
m4_define([lt_rev], [0])
m4_define([lt_age], [m4_eval(v_mic - lt_compat)])

AC_PREREQ([2.65])
AC_INIT([djvulibre],[v_ver])

AC_CONFIG_AUX_DIR(config)
AC_CONFIG_MACRO_DIR(config)
AC_CONFIG_SRCDIR(libdjvu/ddjvuapi.cpp)
AC_CONFIG_HEADER(config.h:config/config.h.in)
AC_CANONICAL_HOST

AM_INIT_AUTOMAKE([1.6 subdir-objects dist-bzip2 dist-xz -Wall])
m4_ifdef([AM_SILENT_RULES], [AM_SILENT_RULES([yes])])
m4_ifdef([AM_PROG_AR], [AM_PROG_AR])

LT_INIT([win32-dll disable-static])

# Library version
version_info="lt_cur:lt_rev:lt_age"
AC_SUBST([version_info])

# Additional libraries
AC_ARG_WITH(extra-includes,
            AS_HELP_STRING([--with-extra-includes=DIR],[Define an additional directory for include files]),
        [ if test -d "$withval" ; then
            CPPFLAGS="$CPPFLAGS -I$withval" 
          else
            AC_MSG_ERROR([Cannot stat directory $withval])
          fi ] )

AC_ARG_WITH(extra-libraries,
            AS_HELP_STRING([--with-extra-libraries=DIR],[Define an additional directory for library files]),
        [ if test -d "$withval" ; then
           LDFLAGS="$LDFLAGS -L$withval"
          else
            AC_MSG_ERROR([Cannot stat directory $withval])
          fi ] )

# Always look into a "gnu" directory.
curwd=`pwd`
if test -d $curwd/gnu/include ; then
   CPPFLAGS="$CPPFLAGS -I$curwd/gnu/include"
fi
if test -d $curwd/gnu/lib ; then
   LDFLAGS="$LDFLAGS -L$curwd/gnu/lib"
fi

# Special cases

AC_CHECK_DECL([_WIN32],[have_os_win32=yes],[have_os_win32=no])
AM_CONDITIONAL([HAVE_OS_WIN32], [test "x${have_os_win32}" = "xyes"])

AC_CHECK_DECL([__APPLE__],[have_os_apple=yes],[have_os_apple=no])
AM_CONDITIONAL([HAVE_OS_APPLE], [test "x${have_os_apple}" = "xyes"])

if test x$have_os_apple = xyes && test -d /opt/local ; then
    CPPFLAGS="$CPPFLAGS -I/opt/local/include"
    LDFLAGS="$LDFLAGS -L/opt/local/lib"
elif test x$have_os_apple = xyes && test -d /sw ; then
    CPPFLAGS="$CPPFLAGS -I/sw/include"
    LDFLAGS="$LDFLAGS -L/sw/lib"
fi



# ----------------------------------------
# Programs
# ----------------------------------------

# Does make set variable $(MAKE)
AC_PROG_MAKE_SET

# Programs
AC_PROG_INSTALL
AC_PROG_LN_S
AC_PROG_MKDIR_P
AC_PROG_SED
AC_PATH_PROG([TOUCH], [touch])
AC_PATH_PROG([RSVG], [rsvg-convert], [false])
AC_PATH_PROG([INKSCAPE], [inkscape], [false])
AC_PATH_PROG([CONVERT], [convert], [false])
conversion_tool=no
if test "x${RSVG}" != xfalse ; then
  conversion_tool=rsvg
elif test "x${INKSCAPE}" != xfalse ; then
  conversion_tool=inkscape
elif test "x${CONVERT}" != xfalse ; then
  ( ${CONVERT} --version 2>&1 | grep -q ImageMagick ) && conversion_tool=convert
fi
AM_CONDITIONAL([HAVE_CONVERSION_RSVG], [test x${conversion_tool} = xrsvg])
AM_CONDITIONAL([HAVE_CONVERSION_INKSCAPE], [test x${conversion_tool} = xinkscape])
AM_CONDITIONAL([HAVE_CONVERSION_CONVERT], [test x${conversion_tool} = xconvert])
AM_CONDITIONAL([HAVE_CONVERSION_TOOL], [test x${conversion_tool} != xno])

# tweak rm to make libtool happy
AC_PATH_PROG(RM, rm) 
RM="$RM -f"

# Libtool & Compilers
AC_PROG_CC
AC_PROG_CXX
AC_OPTIMIZE

# C++
AC_LANG(C++)
AC_CXX_BOOL
AC_CXX_EXCEPTIONS
AC_CXX_TYPENAME
AC_CXX_STDINCLUDES
AC_CXX_NAMESPACES
AC_CXX_MEMBER_TEMPLATES
AC_CXX_INTEL_ATOMIC_BUILTINS
AC_CXX_GCCTLS

# ----------------------------------------
# Libraries
# ----------------------------------------

AC_CHECK_LIB(m,sqrt)

# AC_CHECK_LIB(iconv,libiconv_open) # not really used.


# ----------------------------------------
# Header Files
# ----------------------------------------

AC_HEADER_DIRENT
AC_HEADER_TIME
AC_HEADER_SYS_WAIT     
AC_CHECK_HEADERS(wchar.h wctype.h sys/mman.h iconv.h getopt.h)
AC_CHECK_HEADERS(langinfo.h sched.h new.h)
AC_CHECK_HEADERS(stdint.h inttypes.h sys/ipc.h sys/shm.h)
AC_CHECK_HEADERS(cpuid.h)

# ----------------------------------------
# Types
# ----------------------------------------

AC_TYPE_INT16_T
AC_TYPE_INT32_T
AC_TYPE_UINT16_T
AC_TYPE_UINT32_T
AC_CHECK_TYPES(wchar_t)
AC_CHECK_TYPES(long long int)
AC_CHECK_TYPES(mbstate_t,,,[#include "wchar.h"])
AC_SYS_LARGEFILE

# ----------------------------------------
# Functions
# ----------------------------------------

AC_FUNC_MMAP
AC_FUNC_FORK
AC_FUNC_FSEEKO
AC_CHECK_FUNCS(wcrtomb iswspace setenv nl_langinfo)
AC_CHECK_FUNCS(putc_unlocked strerror vsnprintf)
AC_CHECK_FUNCS(gethostname strftime getpwuid)
AC_CHECK_FUNCS(sigaction mkstemp sched_yield)

# iconv function might be defined as libiconv in iconv.h
AC_MSG_CHECKING([for iconv])
ac_has_iconv=no
AC_LANG_PUSH(C)
AC_LINK_IFELSE([AC_LANG_PROGRAM([[#include "iconv.h"]], 
  [[iconv((iconv_t)0, (char**)0, (size_t*)0, (char**)0, (size_t*)0);]])],
  [ac_has_iconv=yes],[])
AC_MSG_RESULT($ac_has_iconv)
AC_LANG_POP(C)
if test x$ac_has_iconv = xyes ; then
    AC_DEFINE(HAVE_ICONV,1,
        [Define to 1 if you have the iconv function.])
fi


# ----------------------------------------
# Test auxilliary packages
# ----------------------------------------

# Search for PTHREADS (when not on windows)
have_pthread=no
AM_COND_IF([HAVE_OS_WIN32],,[AC_PATH_PTHREAD([have_pthread=yes])])

# Search JPEG library
AC_PATH_JPEG([have_jpeg="yes"], [have_jpeg="no"])
AC_MSG_CHECKING([for libjpeg])
AC_MSG_RESULT([${have_jpeg}])

# Search TIFF library
AC_PATH_TIFF([have_tiff="yes"], [have_tiff="no"])
AC_MSG_CHECKING([for libtiff])
AC_MSG_RESULT([${have_tiff}])


# ----------------------------------------
# What to compile
# ----------------------------------------

# Check compilation of xmltools
want_xmltools="yes"
AC_ARG_ENABLE([xmltools],
   [AS_HELP_STRING([--disable-xmltools],
           [Disable djvu xml tools. @<:@default=enabled@:>@])],
   [want_xmltools=${enableval}])

AM_CONDITIONAL([WANT_XMLTOOLS], [test "x${want_xmltools}" = "xyes"])

# ----------------------------------------
# Desktop files
# ----------------------------------------

ac_desktopfiles="yes"
AC_ARG_ENABLE(desktopfiles,
    AS_HELP_STRING([--disable-desktopfiles], 
        [Do not install icons and mime types. @<:@default=enabled@:>@]),
    [ac_desktopfiles=$enableval])

AM_CONDITIONAL([WANT_DESKTOPFILES], [test "x${ac_desktopfiles}" = "xyes"])

# ----------------------------------------
# End
# ----------------------------------------

# Define installation path for .pc files
PKG_INSTALLDIR

# Define installation paths and version string
AC_DEFINE_UNQUOTED(DJVULIBRE_VERSION,["${PACKAGE_VERSION}"],[version string])

# Output files
AC_CONFIG_FILES([
Makefile
libdjvu/Makefile
tools/Makefile
xmltools/Makefile
share/Makefile
desktopfiles/Makefile
])

# Go
AC_OUTPUT

# Final message

echo
echo "$PACKAGE $VERSION"
echo
echo "Configuration Options Summary:"
echo
echo "  OS.................: ${host_os}"
echo "  Win32 support......: ${have_os_win32}"
echo "  Pthread support....: ${have_pthread}"
echo "  Iconv support......: ${ac_has_iconv}"
echo "  JPEG support.......: ${have_jpeg}"
echo "  TIFF support.......: ${have_tiff}"
echo "  XML tools..........: ${want_xmltools}"
echo "  Desktop files......: ${ac_desktopfiles}"
echo "  Icon generation....: ${conversion_tool}"
echo
echo "Compilation..........: make (or gmake)"
echo "  CPPFLAGS...........: $CPPFLAGS"
echo "  CFLAGS.............: $CFLAGS"
echo "  CXXFLAGS...........: $CXXFLAGS"
echo "  LDFLAGS............: $LDFLAGS"
echo
echo "Installation...........: make install (as root if needed, with 'su' or 'sudo')"
echo "  prefix...............: $prefix"
echo

# ----------------------------------------
# Stuff added to config.h
# ----------------------------------------

# Fence
AH_TOP([
#ifndef CONFIG_H
#define CONFIG_H
/* config.h: begin */
])

# L18N Macros
AH_BOTTOM([

/* - Miscellaneous */
#define AUTOCONF 1
#if defined(__CYGWIN32__) || !defined(_WIN32)
# define UNIX 1
#endif

/* - BOOL */
#if !defined(HAVE_BOOL) && !defined(bool)
#define bool  char
#define true  1
#define false 0
#endif

/* - WCHAR etc.*/
#if ! defined(HAVE_WCHAR_T)
#define HAS_WCHAR 0
#define HAS_WCTYPE 0
#define HAS_MBSTATE 0
#else
#define HAS_WCHAR 1
#if defined(HAVE_WCTYPE_H) && defined(HAVE_ISWSPACE)
#define HAS_WCTYPE 1
#endif
#if defined(HAVE_MBSTATE_T) && defined(HAVE_WCRTOMB)
#define HAS_MBSTATE 1
#endif
#endif
#if defined(HAVE_ICONV_H) && defined(HAVE_ICONV)
#define HAS_ICONV 1
#else
#define HAS_ICONV 0
#endif

/* - I18N MESSAGES HELL */
#define HAS_CTRL_C_IN_ERR_MSG 1

/* - CONTAINERS */
#ifndef HAVE_MEMBER_TEMPLATES
#define GCONTAINER_NO_MEMBER_TEMPLATES 1
#endif
#ifndef HAVE_TYPENAME
#define GCONTAINER_NO_TYPENAME 1
#endif

/* - JPEG */
#ifdef HAVE_JPEG
#define NEED_JPEG_DECODER 1
#endif

/* - MMAP */
#if defined(HAVE_MMAP) && defined(HAVE_SYS_MMAN_H)
#define HAS_MEMMAP 1
#else
#define HAS_MEMMAP 0
#endif

/* config.h: end */
#endif
])
