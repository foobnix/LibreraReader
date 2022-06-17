dnl Copyright (c) 2002  Leon Bottou and Yann Le Cun.
dnl Copyright (c) 2001  AT&T
dnl
dnl Most of these macros are derived from macros listed
dnl at the GNU Autoconf Macro Archive
dnl http://www.gnu.org/software/ac-archive/
dnl
dnl This program is free software; you can redistribute it and/or modify
dnl it under the terms of the GNU General Public License as published by
dnl the Free Software Foundation; either version 2 of the License, or
dnl (at your option) any later version.
dnl
dnl This program is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
dnl GNU General Public License for more details.
dnl
dnl You should have received a copy of the GNU General Public License
dnl along with this program.  If not, see <http://www.gnu.org/licenses/>.
dnl

dnl -------------------------------------------------------
dnl @synopsis AC_CHECK_CXX_OPT(OPTION,
dnl               ACTION-IF-OKAY,ACTION-IF-NOT-OKAY)
dnl Check if compiler accepts option OPTION.
dnl Default action is to add option to CXXFLAGS.
dnl -------------------------------------------------------
AC_DEFUN([AC_CHECK_CXX_OPT],[
 opt="$1"
 AC_MSG_CHECKING([if $CXX accepts $opt])
 echo 'void f(){}' > conftest.cc
 if test -z "`${CXX} ${CXXFLAGS} $opt -c conftest.cc 2>&1`"; then
    AC_MSG_RESULT(yes)
    rm conftest.* 
    ifelse($2,,[CXXFLAGS="$CXXFLAGS $opt"],$2)
 else
    AC_MSG_RESULT(no)
    rm conftest.*
    ifelse($3,,:,$3)
 fi
])

dnl -------------------------------------------------------
dnl @synopsis AC_CHECK_CC_OPT(OPTION,
dnl               ACTION-IF-OKAY,ACTION-IF-NOT-OKAY)
dnl Check if compiler accepts option OPTION.
dnl Default action is to add option to CFLAGS.
dnl -------------------------------------------------------
AC_DEFUN([AC_CHECK_CC_OPT],[
 opt="$1"
 AC_MSG_CHECKING([if $CXX accepts $opt])
 echo 'void f(){}' > conftest.c
 if test -z "`${CC} ${CFLAGS} $opt -c conftest.c 2>&1`"; then
    AC_MSG_RESULT(yes)
    rm conftest.* 
    ifelse($2,,[CFLAGS="$CFLAGS $opt"],$2)
 else
    AC_MSG_RESULT(no)
    rm conftest.*
    ifelse($3,,:,$3)
 fi
])

dnl ------------------------------------------------------
dnl @synopsis AC_REMOVE_OPTIONS(VAR,PATTERN)
dnl ------------------------------------------------------
AC_DEFUN([AC_REMOVE_OPTIONS],[
   saved_var=${$1}
   $1=
   for opt in ${saved_var} ; do
     case "$opt" in 
      $2) ;;
      *) $1="${$1} $opt" ;;
     esac
   done
])

dnl -------------------------------------------------------
dnl @synopsis AC_OPTIMIZE
dnl Setup option --enable-debug
dnl Determine optimization options
dnl Add them to CFLAGS and CXXFLAGS
dnl -------------------------------------------------------
AC_DEFUN([AC_OPTIMIZE],[
   AC_REQUIRE([AC_CANONICAL_HOST])
   AC_ARG_ENABLE(debug,
        AS_HELP_STRING([--enable-debug],
                       [Compile with debugging options (default: no)]),
        [ac_debug=$enableval],[ac_debug=no])
   defines=
   if test x$ac_debug = xno ; then
     AC_REMOVE_OPTIONS([CFLAGS],[-O*])
     AC_REMOVE_OPTIONS([CXXFLAGS],[-O*])
     if test x$GCC != xyes ; then
       AC_REMOVE_OPTIONS([CFLAGS],[-g*])
       AC_REMOVE_OPTIONS([CXXFLAGS],[-g*])
     fi
     defines="-DNDEBUG"
     AC_CHECK_CC_OPT([-O3],,[AC_CHECK_CC_OPT([-O2])])
     AC_CHECK_CXX_OPT([-O3],,[AC_CHECK_CXX_OPT([-O2])])
     cpu=`uname -m 2>/dev/null`
     test -z "$cpu" && cpu=${host_cpu}
     case "${host_cpu}" in
        i?86)
           opt="-mtune=${host_cpu}"
           AC_CHECK_CXX_OPT([-mtune=${host_cpu}],,
                [AC_CHECK_CXX_OPT([-mcpu=${host_cpu}])])
           AC_CHECK_CC_OPT([-mtune=${host_cpu}],,
                [AC_CHECK_CC_OPT([-mcpu=${host_cpu}])])
           ;;
      esac
   else
     AC_REMOVE_OPTIONS([CFLAGS],[-O*|-g*])
     AC_REMOVE_OPTIONS([CXXFLAGS],[-O*|-g*])
     AC_CHECK_CC_OPT([-g])
     AC_CHECK_CXX_OPT([-g])
     AC_CHECK_CXX_OPT([-Wno-non-virtual-dtor])
   fi
   AC_CHECK_CC_OPT([-Wall])
   AC_CHECK_CXX_OPT([-Wall])
   case x"$ac_debug" in
changequote(<<, >>)dnl
     x[0-9]) defines="-DDEBUGLVL=$ac_debug" ;;
     xr*)    defines="-DRUNTIME_DEBUG_ONLY" ;;
changequote([, ])dnl 
   esac
   CFLAGS="$CFLAGS $defines"
   CXXFLAGS="$CXXFLAGS $defines"
])

dnl -------------------------------------------------------
dnl @synopsis AC_CXX_INTEL_ATOMIC_BUILTINS
dnl If the compiler supports intel atomic builtins.
dnl define HAVE_INTEL_ATOMIC_BUILTINS
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_INTEL_ATOMIC_BUILTINS],
[AC_CACHE_CHECK(whether the compiler supports intel atomic builtins,
ac_cv_cxx_intel_atomic_builtins,
[AC_LANG_PUSH([C++])
 AC_LINK_IFELSE(
 [AC_LANG_PROGRAM(
  [[
static int volatile l;
  ]],
  [
__sync_lock_test_and_set(&l,1);
__sync_lock_release(&l);
__sync_add_and_fetch(&l,1);
__sync_bool_compare_and_swap(&l,&l,1);
__sync_synchronize();
return 0;
  ])],
 [ac_cv_cxx_intel_atomic_builtins=yes],
 [ac_cv_cxx_intel_atomic_builtins=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_intel_atomic_builtins" = yes; then
  AC_DEFINE(HAVE_INTEL_ATOMIC_BUILTINS,1,
        [define if the compiler supports intel atomic builtins])
fi
])

dnl -------------------------------------------------------
dnl @synopsis AC_CXX_MEMBER_TEMPLATES
dnl If the compiler supports member templates, 
dnl define HAVE_MEMBER_TEMPLATES.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_MEMBER_TEMPLATES],
[AC_CACHE_CHECK(whether the compiler supports member templates,
ac_cv_cxx_member_templates,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
template<class T, int N> class A
{ public:
  template<int N2> A<T,N> operator=(const A<T,N2>& z) { return A<T,N>(); }
};
  ]],
  [[
A<double,4> x; A<double,7> y;
x = y;
return 0;
  ]])],
 [ac_cv_cxx_member_templates=yes],
 [ac_cv_cxx_member_templates=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_member_templates" = yes; then
  AC_DEFINE(HAVE_MEMBER_TEMPLATES,1,
        [define if the compiler supports member templates])
fi
])


dnl -------------------------------------------------------
dnl @synopsis AC_CXX_NAMESPACES
dnl Define HAVE_NAMESPACES if the compiler supports
dnl namespaces.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_NAMESPACES],
[AC_CACHE_CHECK(whether the compiler implements namespaces,
ac_cv_cxx_namespaces,
[ AC_LANG_PUSH([C++])
  AC_COMPILE_IFELSE(
  [AC_LANG_PROGRAM(
   [[
namespace Outer { namespace Inner { int i = 0; }}
   ]],
   [[
using namespace Outer::Inner;
return i;
   ]])],
  [ac_cv_cxx_namespaces=yes],
  [ac_cv_cxx_namespaces=no])
  AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_namespaces" = yes && test "$ac_debug" = no; then
  AC_DEFINE(HAVE_NAMESPACES,1,
             [define if the compiler implements namespaces])
fi
])



dnl -------------------------------------------------------
dnl @synopsis AC_CXX_TYPENAME
dnl Define HAVE_TYPENAME if the compiler recognizes 
dnl keyword typename.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_TYPENAME],
[AC_CACHE_CHECK(whether the compiler recognizes typename,
ac_cv_cxx_typename,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
template<typename T>class X {public:X(){}};
  ]],
  [[
X<float> z;
return 0;
  ]])],
 [ac_cv_cxx_typename=yes],
 [ac_cv_cxx_typename=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_typename" = yes; then
  AC_DEFINE(HAVE_TYPENAME,1,[define if the compiler recognizes typename])
fi
])


dnl -------------------------------------------------------
dnl @synopsis AC_CXX_STDINCLUDES
dnl Define HAVE_STDINCLUDES if the compiler has the
dnl new style include files (without the .h)
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_STDINCLUDES],
[AC_CACHE_CHECK(whether the compiler comes with standard includes,
ac_cv_cxx_stdincludes,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
#include <new>
struct X { int a; X(int a):a(a){} };
X* foo(void *x) { return new(x) X(2); }
  ]],
  [[
  ]])],
 [ac_cv_cxx_stdincludes=yes],
 [ac_cv_cxx_stdincludes=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_stdincludes" = yes; then
  AC_DEFINE(HAVE_STDINCLUDES,1,
    [define if the compiler comes with standard includes])
fi
])


dnl -------------------------------------------------------
dnl @synopsis AC_CXX_BOOL
dnl If the compiler recognizes bool as a separate built-in type,
dnl define HAVE_BOOL. Note that a typedef is not a separate
dnl type since you cannot overload a function such that it 
dnl accepts either the basic type or the typedef.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_BOOL],
[AC_CACHE_CHECK(whether the compiler recognizes bool as a built-in type,
ac_cv_cxx_bool,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
int f(int  x){return 1;}
int f(char x){return 1;}
int f(bool x){return 1;}
  ]],
  [[
bool b = true;
return f(b);
  ]])],
 [ac_cv_cxx_bool=yes],
 [ac_cv_cxx_bool=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_bool" = yes; then
  AC_DEFINE(HAVE_BOOL,1,[define if bool is a built-in type])
fi
])

dnl -------------------------------------------------------
dnl @synopsis AC_CXX_EXCEPTIONS
dnl If the C++ compiler supports exceptions handling (try,
dnl throw and catch), define HAVE_EXCEPTIONS.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_EXCEPTIONS],
[AC_CACHE_CHECK(whether the compiler supports exceptions,
ac_cv_cxx_exceptions,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
  ]],
  [[
try { throw  1; } catch (int i) { return i; }
  ]])],
 [ac_cv_cxx_exceptions=yes],
 [ac_cv_cxx_exceptions=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_exceptions" = yes; then
  AC_DEFINE(HAVE_EXCEPTIONS,1,[define if the compiler supports exceptions])
fi
])


dnl -------------------------------------------------------
dnl @synopsis AC_CXX_GCCTLS
dnl Define HAVE_GCCTLS if the compiler recognizes 
dnl keyword __thread for TLS variables.
dnl -------------------------------------------------------
AC_DEFUN([AC_CXX_GCCTLS],
[AC_CACHE_CHECK(whether the compiler supports keyword __thread,
ac_cv_cxx_gcctls,
[AC_LANG_PUSH([C++])
 AC_COMPILE_IFELSE(
 [AC_LANG_PROGRAM(
  [[
__thread int i;
  ]],
  [[
return i;
  ]])],
 [ac_cv_cxx_gcctls=yes],
 [ac_cv_cxx_gcctls=no])
 AC_LANG_POP([C++])
])
if test "$ac_cv_cxx_gcctls" = yes; then
  AC_DEFINE(HAVE_GCCTLS,1,[define if the compiler supports keyword __thread])
fi
])


dnl ------------------------------------------------------------------
dnl @synopsis AC_PATH_PTHREAD([ACTION-IF-FOUND[, ACTION-IF-NOT-FOUND]])
dnl This macro figures out how to build C programs using POSIX
dnl threads.  It sets the PTHREAD_LIBS output variable to the threads
dnl library and linker flags, and the PTHREAD_CFLAGS output variable
dnl to any special C compiler flags that are needed.  (The user can also
dnl force certain compiler flags/libs to be tested by setting these
dnl environment variables.).  
dnl ------------------------------------------------------------------
AC_DEFUN([AC_PATH_PTHREAD], [
AC_REQUIRE([AC_CANONICAL_HOST])
acx_pthread_ok=no
# First, check if the POSIX threads header, pthread.h, is available.
# If it isn't, don't bother looking for the threads libraries.
AC_CHECK_HEADER(pthread.h, , acx_pthread_ok=noheader)
# We must check for the threads library under a number of different
# names; the ordering is very important because some systems
# (e.g. DEC) have both -lpthread and -lpthreads, where one of the
# libraries is broken (non-POSIX).
# First of all, check if the user has set any of the PTHREAD_LIBS,
# etcetera environment variables, and if threads linking works.
if test x${PTHREAD_LIBS+set} = xset ||
   test x${PTHREAD_CFLAGS+set} = xset ; then
        save_CFLAGS="$CFLAGS"
        CFLAGS="$CFLAGS $PTHREAD_CFLAGS"
        save_CXXFLAGS="$CXXFLAGS"
        CXXFLAGS="$CXXFLAGS $PTHREAD_CFLAGS"
        save_LIBS="$LIBS"
        LIBS="$PTHREAD_LIBS $LIBS"
        AC_MSG_CHECKING([provided PTHREAD_LIBS/PTHREAD_CFLAGS.])
        AC_TRY_LINK_FUNC(pthread_join, acx_pthread_ok=yes)
        AC_MSG_RESULT($acx_pthread_ok)
        if test x"$acx_pthread_ok" = xno; then
                PTHREAD_LIBS=""
                PTHREAD_CFLAGS=""
        fi
        LIBS="$save_LIBS"
        CFLAGS="$save_CFLAGS"
        CXXFLAGS="$save_CXXFLAGS"
fi
# Create a list of thread flags to try.  Items starting with a "-" are
# C compiler flags, and other items are library names, except for "none"
# which indicates that we try without any flags at all. Also, combinations
# of items (for instance, both a compiler flag and a library name) can be 
# specified using a colon separator.
acx_pthread_flags="pthreads none -Kthread -kthread lthread 
                   -pthread -pthreads -mt -mthreads pthread
                   --thread-safe"
# The ordering *is* (sometimes) important.  
# Some notes on the individual items follow:
# pthreads: AIX (must check this before -lpthread)
# none: in case threads are in libc; should be tried before -Kthread and
#       other compiler flags to prevent continual compiler warnings
# -Kthread: Sequent (threads in libc, but -Kthread needed for pthread.h)
# -kthread: FreeBSD kernel threads (preferred to -pthread since SMP-able)
# lthread: LinuxThreads port on FreeBSD (also preferred to -pthread)
# -pthread: Linux/gcc (kernel threads), BSD/gcc (userland threads)
# -pthreads: Solaris/gcc
# -mt: HP aCC (check before -mthreads)
# -mt: Sun Workshop C (may only link SunOS threads [-lthread], but it
#      doesn't hurt to check since this sometimes defines pthreads too;
#      also defines -D_REENTRANT)
# -mthreads: Mingw32/gcc, Lynx/gcc
# pthread: Linux, etcetera
# --thread-safe: KAI C++
case "${host_cpu}-${host_os}" in
        *solaris*)
        # On Solaris (at least, for some versions), libc contains stubbed
        # (non-functional) versions of the pthreads routines, so link-based
        # tests will erroneously succeed.  (We need to link with -pthread or
        # -lpthread.)  (The stubs are missing pthread_cleanup_push, or rather
        # a function called by this macro, so we could check for that, but
        # who knows whether they'll stub that too in a future libc.)  So,
        # we'll just look for -pthreads and -lpthread first:
        acx_pthread_flags="-pthread -pthreads pthread -mt $acx_pthread_flags"
        ;;
esac
case "${host_os}-${GCC}" in
        *linux*-yes | *kfreebsd*-yes )
        # On Linux/GCC, libtool uses -nostdlib for linking, which cancel part
        # of the -pthread flag effect (libpthread is not automatically linked).
        # So we'll try to link with both -pthread and -lpthread first:
        acx_pthread_flags="-pthread:pthread $acx_pthread_flags"
        ;;
esac
if test x"$acx_pthread_ok" = xno; then
for flag in $acx_pthread_flags; do
        case $flag in
                none)
                AC_MSG_CHECKING([whether pthreads work without any flags])
                ;;
                *:*)
                PTHREAD_CFLAGS=""
                PTHREAD_LIBS=""
                message="whether pthreads work with"
                while test x"$flag" != x; do
                        subflag=`echo $flag | cut -d: -f1`
                        case $subflag in
                                -*)
                                PTHREAD_CFLAGS="$PTHREAD_CFLAGS $subflag"
                                message="$message $subflag"
                                ;;
                                *)
                                PTHREAD_LIBS="$PTHREAD_LIBS -l$subflag"
                                message="$message -l$subflag"
                                ;;
                        esac
                        flag=`echo $flag | cut -s -d: -f2-`
                done
                AC_MSG_CHECKING([$message])
                ;;
                -*)
                AC_MSG_CHECKING([whether pthreads work with $flag])
                PTHREAD_CFLAGS="$flag"
                ;;
                *)
                AC_MSG_CHECKING([for the pthreads library -l$flag])
                PTHREAD_LIBS="-l$flag"
                ;;
        esac
        save_LIBS="$LIBS"
        save_CFLAGS="$CFLAGS"
        save_CXXFLAGS="$CXXFLAGS"
        LIBS="$PTHREAD_LIBS $LIBS"
        CFLAGS="$CFLAGS $PTHREAD_CFLAGS"
        CXXFLAGS="$CXXFLAGS $PTHREAD_CFLAGS"
        # Check for various functions.  We must include pthread.h,
        # since some functions may be macros.  (On the Sequent, we
        # need a special flag -Kthread to make this header compile.)
        # We check for pthread_join because it is in -lpthread on IRIX
        # while pthread_create is in libc.  We check for pthread_attr_init
        # due to DEC craziness with -lpthreads.  We check for
        # pthread_cleanup_push because it is one of the few pthread
        # functions on Solaris that doesn't have a non-functional libc stub.
        # We try pthread_create on general principles.
        AC_LINK_IFELSE(
        [AC_LANG_PROGRAM(
         [[
#include <pthread.h>
         ]],
         [
pthread_t th; pthread_join(th, 0);
pthread_attr_init(0); pthread_cleanup_push(0, 0);
pthread_create(0,0,0,0); pthread_cleanup_pop(0);
         ])],
         [acx_pthread_ok=yes],
         [acx_pthread_ok=no])
        LIBS="$save_LIBS"
        CFLAGS="$save_CFLAGS"
        CXXFLAGS="$save_CXXFLAGS"
        AC_MSG_RESULT($acx_pthread_ok)
        if test "x$acx_pthread_ok" = xyes; then
                break;
        fi
        PTHREAD_LIBS=""
        PTHREAD_CFLAGS=""
done
fi
# Various other checks:
if test "x$acx_pthread_ok" = xyes; then
        save_LIBS="$LIBS"
        LIBS="$PTHREAD_LIBS $LIBS"
        save_CFLAGS="$CFLAGS"
        CFLAGS="$CFLAGS $PTHREAD_CFLAGS"
        save_CXXFLAGS="$CXXFLAGS"
        CXXFLAGS="$CXXFLAGS $PTHREAD_CFLAGS"
        AC_MSG_CHECKING([if more special flags are required for pthreads])
        flag=no
        case "${host_cpu}-${host_os}" in
                *-aix* | *-freebsd*)     flag="-D_THREAD_SAFE";;
                *solaris* | alpha*-osf*) flag="-D_REENTRANT";;
        esac
        AC_MSG_RESULT(${flag})
        if test "x$flag" != xno; then
                PTHREAD_CFLAGS="$flag $PTHREAD_CFLAGS"
        fi
        LIBS="$save_LIBS"
        CFLAGS="$save_CFLAGS"
        CXXFLAGS="$save_CXXFLAGS"
fi
AC_ARG_VAR(PTHREAD_LIBS, [Flags for linking pthread programs.])
AC_ARG_VAR(PTHREAD_CFLAGS, [Flags for compiling pthread programs.])
# execute ACTION-IF-FOUND/ACTION-IF-NOT-FOUND:
if test x"$acx_pthread_ok" = xyes; then
        AC_DEFINE(HAVE_PTHREAD,1,[Define if pthreads are available])
        ifelse([$1],,:,[$1])
else
        ifelse([$2],,:,[$2])
fi
])

dnl ------------------------------------------------------------------
dnl @synopsis AC_PATH_JPEG([ACTION-IF-FOUND[, ACTION-IF-NOT-FOUND]])
dnl Process option --with-jpeg.
dnl Search JPEG. Define HAVE_JPEG.
dnl Set output variable JPEG_CFLAGS and JPEG_LIBS
dnl ------------------------------------------------------------------

AC_DEFUN([AC_PATH_JPEG],
[
  AC_ARG_VAR(JPEG_LIBS)
  AC_ARG_VAR(JPEG_CFLAGS)
  ac_jpeg=no
  AC_ARG_WITH(jpeg,
     AS_HELP_STRING([--with-jpeg=DIR],
                    [where the IJG jpeg library is located]),
     [ac_jpeg=$withval], [ac_jpeg=yes] )
  # Process specification
  if test x$ac_jpeg = xyes ; then
     test x${JPEG_LIBS+set} != xset && JPEG_LIBS="-ljpeg"
  elif test x$ac_jpeg != xno ; then
     test x${JPEG_LIBS+set} != xset && JPEG_LIBS="-L$ac_jpeg/lib -ljpeg"
     test x${JPEG_CFLAGS+set} != xset && JPEG_CFLAGS="-I$ac_jpeg/include"
  fi
  # Try linking
  if test x$ac_jpeg != xno ; then
     AC_MSG_CHECKING([for jpeg library])
     save_CFLAGS="$CFLAGS"
     save_CXXFLAGS="$CXXFLAGS"
     save_LIBS="$LIBS"
     CFLAGS="$CFLAGS $JPEG_CFLAGS"
     CXXFLAGS="$CXXFLAGS $JPEG_CFLAGS"
     LIBS="$LIBS $JPEG_LIBS"
     AC_LINK_IFELSE(
     [AC_LANG_PROGRAM(
      [[
#ifdef __cplusplus
extern "C" {
#endif
#include <stdio.h>
#include <jpeglib.h>
#ifdef __cplusplus
}
#endif
      ]],
      [
jpeg_CreateDecompress(0,0,0);
      ])],
      [ac_jpeg=yes],
      [ac_jpeg=no])
     CFLAGS="$save_CFLAGS"
     CXXFLAGS="$save_CXXFLAGS"
     LIBS="$save_LIBS"
     AC_MSG_RESULT($ac_jpeg)
   fi
   # Finish
   if test x$ac_jpeg = xno; then
      JPEG_CFLAGS= ; JPEG_LIBS=
      ifelse([$2],,:,[$2])
   else
      AC_DEFINE(HAVE_JPEG,1,[Define if you have the IJG JPEG library.])
      dnl AC_MSG_RESULT([setting JPEG_CFLAGS=$JPEG_CFLAGS])
      dnl AC_MSG_RESULT([setting JPEG_LIBS=$JPEG_LIBS])
      ifelse([$1],,:,[$1])
   fi
])

dnl ------------------------------------------------------------------
dnl @synopsis AC_PATH_TIFF([ACTION-IF-FOUND[, ACTION-IF-NOT-FOUND]])
dnl Process option --with-tiff
dnl Search LIBTIFF. Define HAVE_TIFF.
dnl Set output variable TIFF_CFLAGS and TIFF_LIBS
dnl ------------------------------------------------------------------

AC_DEFUN([AC_PATH_TIFF],
[
  AC_ARG_VAR(TIFF_LIBS)
  AC_ARG_VAR(TIFF_CFLAGS)
  ac_tiff=no
  AC_ARG_WITH(tiff,
     AS_HELP_STRING([--with-tiff=DIR],
                    [where libtiff is located]),
     [ac_tiff=$withval], [ac_tiff=yes] )
  # Process specification
  if test x$ac_tiff = xyes ; then
     test x${TIFF_LIBS+set} != xset && TIFF_LIBS="-ltiff"
  elif test x$ac_tiff != xno ; then
     test x${TIFF_LIBS+set} != xset && TIFF_LIBS="-L$ac_tiff/lib -ltiff"
     test x${TIFF_CFLAGS+set} != xset && TIFF_CFLAGS="-I$ac_tiff/include"
  fi
  # Try linking
  if test x$ac_tiff != xno ; then
     AC_MSG_CHECKING([for the libtiff library])
     save_CFLAGS="$CFLAGS"
     save_CXXFLAGS="$CXXFLAGS"
     save_LIBS="$LIBS"
     CFLAGS="$CFLAGS $TIFF_CFLAGS"
     CXXFLAGS="$CXXFLAGS $TIFF_CFLAGS"
     LIBS="$LIBS $TIFF_LIBS"
     AC_LINK_IFELSE(
     [AC_LANG_PROGRAM(
      [[
#ifdef __cplusplus
extern "C" {
#endif
#include <stdio.h>
#include <tiffio.h>
#ifdef __cplusplus
}
#endif
      ]],
      [
TIFFOpen(0,0);
      ])],
      [ac_tiff=yes],
      [ac_tiff=no])
     CFLAGS="$save_CFLAGS"
     CXXFLAGS="$save_CXXFLAGS"
     LIBS="$save_LIBS"
     AC_MSG_RESULT($ac_tiff)
   fi
   # Finish
   if test x$ac_tiff = xno; then
      TIFF_CFLAGS= ; TIFF_LIBS=
      ifelse([$2],,:,[$2])
   else
      AC_DEFINE(HAVE_TIFF,1,[Define if you have libtiff.])
      dnl AC_MSG_RESULT([setting TIFF_CFLAGS=$TIFF_CFLAGS])
      dnl AC_MSG_RESULT([setting TIFF_LIBS=$TIFF_LIBS])
      ifelse([$1],,:,[$1])
   fi
])


dnl ------------------------------------------------------------------
dnl @synopsis AC_PROG_PKG_CONFIG([ACTION-IF-FOUND[, ACTION-IF-NOT-FOUND]])
dnl Sets output variables PKG_CONFIG
dnl ------------------------------------------------------------------


AC_DEFUN([AC_PROG_PKG_CONFIG], 
[
  AC_ARG_VAR(PKG_CONFIG,[Location of the pkg-config program.])
  AC_ARG_VAR(PKG_CONFIG_PATH, [Path for pkg-config descriptors.])
  AC_PATH_PROG(PKG_CONFIG, pkg-config)
  if test -z "$PKG_CONFIG" ; then
      ifelse([$2],,:,[$2])
  else
      ifelse([$1],,:,[$1])
  fi
])





