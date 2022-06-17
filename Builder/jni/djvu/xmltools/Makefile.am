bin_PROGRAMS = djvutoxml djvuxmlparser

AM_CPPFLAGS = -I$(top_srcdir)/libdjvu -I$(top_srcdir)/tools
AM_CXXFLAGS = $(PTHREAD_CFLAGS)
DJLIB = $(top_builddir)/libdjvu/libdjvulibre.la

djvutoxml_SOURCES = djvutoxml.cpp
djvutoxml_LDADD = $(DJLIB) $(PTHREAD_LIBS)

djvuxmlparser_SOURCES = djvuxmlparser.cpp
djvuxmlparser_LDADD = $(DJLIB) $(PTHREAD_LIBS)

sed_process = \
$(SED) \
-e 's,DATADIR,$(datadir),g' \
< $< > $@ || rm $@

man_verbose = $(man_verbose_@AM_V@)
man_verbose_ = $(man_verbose_@AM_DEFAULT_V@)
man_verbose_0 = @echo "  MAN     " $@;

djvuxml.1: djvuxml.1.in Makefile
	$(AM_V_at)rm -f $@
	$(man_verbose)$(sed_process)

dist_man1_MANS = djvutoxml.1 djvuxmlparser.1
man1_MANS = djvuxml.1

EXTRA_DIST = djvuxml.1.in

MOSTLYCLEANFILES = djvuxml.1
