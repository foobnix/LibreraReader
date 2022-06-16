bin_PROGRAMS = bzz c44 cjb2 cpaldjvu csepdjvu ddjvu djvm djvmcvt	\
 djvudump djvups djvuextract djvumake djvused djvutxt djvuserve

jb2cmp_SOURCES = jb2cmp/classify.cpp jb2cmp/cuts.cpp		\
 jb2cmp/frames.cpp jb2cmp/patterns.cpp jb2cmp/classify.h	\
 jb2cmp/mdjvucfg.h jb2cmp/minidjvu.h jb2cmp/patterns.h

AM_CPPFLAGS = -I$(top_srcdir)/libdjvu
AM_CXXFLAGS = $(PTHREAD_CFLAGS)

DJLIB = $(top_builddir)/libdjvu/libdjvulibre.la

bzz_SOURCES = bzz.cpp common.h
bzz_LDADD = $(DJLIB) $(PTHREAD_LIBS)

c44_SOURCES = c44.cpp common.h
c44_LDADD = $(DJLIB) $(PTHREAD_LIBS)

cjb2_SOURCES = cjb2.cpp jb2tune.cpp common.h jb2tune.h $(jb2cmp_SOURCES)
cjb2_CPPFLAGS = $(AM_CPPFLAGS) $(TIFF_CFLAGS)
cjb2_LDADD = $(DJLIB) $(TIFF_LIBS) $(PTHREAD_LIBS)

cpaldjvu_SOURCES = cpaldjvu.cpp jb2tune.cpp common.h jb2tune.h $(jb2cmp_SOURCES)
cpaldjvu_LDADD = $(DJLIB) $(PTHREAD_LIBS)

csepdjvu_SOURCES = csepdjvu.cpp jb2tune.cpp common.h jb2tune.h $(jb2cmp_SOURCES)
csepdjvu_LDADD = $(DJLIB) $(PTHREAD_LIBS)

ddjvu_SOURCES = ddjvu.cpp tiff2pdf.c tiff2pdf.h
ddjvu_CPPFLAGS = -I$(top_srcdir)  $(AM_CPPFLAGS) $(TIFF_CFLAGS)
if HAVE_OS_WIN32
ddjvu_CPPFLAGS += -D__USE_MINGW_ANSI_STDIO
endif
ddjvu_CFLAGS = $(THREAD_CFLAGS)
ddjvu_LDADD = $(DJLIB) $(TIFF_LIBS) $(PTHREAD_LIBS)

djvm_SOURCES = djvm.cpp common.h
djvm_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvmcvt_SOURCES = djvmcvt.cpp common.h
djvmcvt_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvudump_SOURCES = djvudump.cpp common.h
djvudump_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvuextract_SOURCES = djvuextract.cpp common.h
djvuextract_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvumake_SOURCES = djvumake.cpp common.h
djvumake_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvups_SOURCES = djvups.cpp
djvups_CPPFLAGS = -I$(top_srcdir) $(AM_CPPFLAGS)
djvups_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvused_SOURCES = djvused.cpp common.h
djvused_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvuserve_SOURCES = djvuserve.cpp common.h
djvuserve_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvutxt_SOURCES = djvutxt.cpp
djvutxt_CPPFLAGS = -I$(top_srcdir) $(AM_CPPFLAGS)
djvutxt_LDADD = $(DJLIB) $(PTHREAD_LIBS)

dist_bin_SCRIPTS = any2djvu djvudigital

dist_man1_MANS = any2djvu.1 bzz.1 c44.1 cjb2.1 cpaldjvu.1 csepdjvu.1	\
 ddjvu.1 djvm.1 djvmcvt.1 djvu.1 djvudigital.1 djvudump.1		\
 djvuextract.1 djvumake.1 djvups.1 djvused.1 djvuserve.1 djvutxt.1

EXTRA_DIST = jb2cmp/README tiff2pdf.c.diff
