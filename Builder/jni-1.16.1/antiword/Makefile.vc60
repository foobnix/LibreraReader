#
# Makefile for antiword (Windows version)
#
# Visual C++ 6.0 (use nmake)
#
# needs getopt.c 
#
# Original version by: Frank M. Siegert <frank@wizards.de>
#

.SUFFIXES: .exe .obj .c .asm

CC	= cl
LD	= link
CP	= copy
RM	= del /f

# Debug flag must be equal to DEBUG or NDEBUG

LDLIBS	= kernel32.lib user32.lib

# Ox is full optimization

CFLAGS	= /Ox /c /D__dos /DNDEBUG
LDFLAGS	= 

OBJS	=\
	main_u.obj asc85enc.obj blocklist.obj chartrans.obj datalist.obj\
	depot.obj dib2eps.obj doclist.obj fail.obj finddata.obj findtext.obj\
	fmt_text.obj fontlist.obj fonts.obj fonts_u.obj drftrlist.obj\
	imgexam.obj imgtrans.obj jpeg2eps.obj listlist.obj misc.obj notes.obj\
	options.obj out2window.obj output.obj pdf.obj pictlist.obj png2eps.obj\
	postscript.obj prop0.obj\ prop2.obj prop6.obj prop8.obj properties.obj\
	propmod.obj rowlist.obj sectlist.obj stylelist.obj stylesheet.obj\
	summary.obj tabstop.obj text.obj unix.obj utf8.obj word2text.obj\
	worddos.obj wordlib.obj wordmac.obj wordole.obj wordwin.obj xmalloc.obj\
	xml.obj getopt.obj

PROG	= antiword.exe

INSTALL_DIR = $(HOME)\bin

all:		$(PROG)

install:	all
	$(CP) $(PROG) $(INSTALL_DIR)

clean:
	$(RM) $(OBJS)
	$(RM) antiword.exe

$(PROG):	$(OBJS)
	-$(RM) $@
	$(LD) /OUT:$@ $(LDFLAGS) $(OBJS) $(LDLIBS)

.c.obj:
	$(CC) $(CFLAGS) -c $<

main_u.obj:	version.h
postscri.obj:	version.h
pdf.obj:	version.h
fonts_u.obj:	fontinfo.h
