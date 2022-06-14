# tools

# this lists the binaries to produce, the (non-PHONY, binary) targets in
# the previous manual Makefile
AM_CPPFLAGS = -I$(top_srcdir)/src -I$(top_builddir)/src

bin_PROGRAMS = mobitool mobimeta
man_MANS = mobitool.1 mobimeta.1

noinst_LIBRARIES = libcommon.a
libcommon_a_SOURCES = common.c common.h

if USE_INTERNAL_GETOPT
libcommon_a_SOURCES += win32/getopt.c win32/getopt.h
endif

mobitool_SOURCES = mobitool.c
mobitool_DEPENDENCIES = $(top_builddir)/src/libmobi.la libcommon.a
mobitool_LDADD = libcommon.a $(top_builddir)/src/libmobi.la
mobitool_CFLAGS = $(ISO99_SOURCE) $(DEBUG_CFLAGS) -D_POSIX_C_SOURCE=200112L
mobitool_LDFLAGS = $(TOOLS_STATIC)

if USE_XMLWRITER
# miniz.c zip functions are needed for epub creation
noinst_LIBRARIES += libminiz.a
libminiz_a_SOURCES = ../src/miniz.c ../src/miniz.h
libminiz_a_CFLAGS = $(MINIZ_CFLAGS) -DMINIZ_NO_ZLIB_COMPATIBLE_NAMES
mobitool_DEPENDENCIES += libminiz.a
mobitool_LDADD += libminiz.a
if USE_STATIC
if USE_MINIZ
mobitool_LDFLAGS += $(MOBI_ALLOW_MULTIPLE)
endif
endif
endif

mobimeta_SOURCES = mobimeta.c
mobimeta_DEPENDENCIES = $(top_builddir)/src/libmobi.la libcommon.a
mobimeta_LDADD = libcommon.a $(top_builddir)/src/libmobi.la
mobimeta_CFLAGS = $(ISO99_SOURCE) $(DEBUG_CFLAGS) -D_POSIX_C_SOURCE=200112L
mobimeta_LDFLAGS = $(TOOLS_STATIC)

if USE_ENCRYPTION
bin_PROGRAMS += mobidrm
man_MANS += mobidrm.1
mobidrm_SOURCES = mobidrm.c
mobidrm_DEPENDENCIES = $(top_builddir)/src/libmobi.la libcommon.a
mobidrm_LDADD = libcommon.a $(top_builddir)/src/libmobi.la
mobidrm_CFLAGS = $(ISO99_SOURCE) $(DEBUG_CFLAGS) -D_POSIX_C_SOURCE=200112L
mobidrm_LDFLAGS = $(TOOLS_STATIC)
endif
