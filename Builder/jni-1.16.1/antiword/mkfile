</$objtype/mkfile

TARG=antiword

CFLAGS=-Bp -I/sys/include/ape -I/$objtype/include/ape -DNDEBUG -D_POSIX_SOURCE -D__Plan9__

TARG=antiword
OFILES= main_u.$O asc85enc.$O blocklist.$O chartrans.$O datalist.$O depot.$O\
	dib2eps.$O fail.$O finddata.$O findtext.$O fmt_text.$O fontlist.$O\
	fonts.$O fonts_u.$O imgexam.$O imgtrans.$O jpeg2eps.$O listlist.$O\
	misc.$O notes.$O options.$O out2window.$O output.$O pdf.$O pictlist.$O\
	png2eps.$O postscript.$O prop0.$O prop2.$O prop6.$O prop8.$O\
	properties.$O propmod.$O rowlist.$O sectlist.$O stylelist.$O\
	stylesheet.$O summary.$O tabstop.$O text.$O unix.$O utf8.$O\
	word2text.$O worddos.$O wordlib.$O wordmac.$O wordole.$O wordwin.$O\
	xmalloc.$O xml.$O

HFILES=antiword.h debug.h draw.h fail.h fontinfo.h version.h wordconst.h wordtypes.h

BIN=/$objtype/bin/aux

</sys/src/cmd/mkone

main_u.$O: version.h
postscript.$O: version.h
pdf.$O: version.h
fonts_u.$O: fontinfo.h

# fontinfo.h: Unix-fontinfo.pl
# 	fontinfo.pl > fontinfo.h
