# libmobi 

lib_LTLIBRARIES = libmobi.la
libmobi_la_SOURCES = buffer.c buffer.h compression.c compression.h debug.c debug.h index.c index.h memory.c memory.h \
meta.c meta.h parse_rawml.c parse_rawml.h read.c read.h structure.c structure.h util.c util.h write.c write.h

if USE_XMLWRITER
libmobi_la_SOURCES += opf.c opf.h
if !USE_LIBXML2
libmobi_la_SOURCES += xmlwriter.c xmlwriter.h
endif
endif
if USE_ENCRYPTION
libmobi_la_SOURCES += encryption.c encryption.h sha1.c sha1.h
endif
EXTRA_LTLIBRARIES = libminiz.la
libminiz_la_SOURCES = miniz.c miniz.h
libminiz_la_CFLAGS = $(VISIBILITY_HIDDEN) $(MINIZ_CFLAGS) \
-DMINIZ_NO_STDIO -DMINIZ_NO_ZLIB_COMPATIBLE_NAMES \
-DMINIZ_NO_TIME -DMINIZ_NO_ARCHIVE_APIS -DMINIZ_NO_ARCHIVE_WRITING_APIS
libminiz_la_LDFLAGS =
if USE_MINIZ
libmobi_la_LIBADD = libminiz.la
endif
include_HEADERS = mobi.h
libmobi_la_LDFLAGS = $(AVOID_VERSION) $(NO_UNDEFINED) $(DARWIN_LDFLAGS) $(LIBZ_LDFLAGS) $(LIBXML2_LDFLAGS)
libmobi_la_CFLAGS = $(VISIBILITY_HIDDEN) $(ISO99_SOURCE) $(DEBUG_CFLAGS) $(LIBXML2_CFLAGS)
