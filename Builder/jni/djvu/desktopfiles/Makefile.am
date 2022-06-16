all: icons

#icons

SVGICONS = \
scalable/mimetypes/image-vnd.djvu.svgz

PNGICONS = \
16x16/mimetypes/image-vnd.djvu.png \
20x20/mimetypes/image-vnd.djvu.png \
22x22/mimetypes/image-vnd.djvu.png \
24x24/mimetypes/image-vnd.djvu.png \
32x32/mimetypes/image-vnd.djvu.png \
48x48/mimetypes/image-vnd.djvu.png \
64x64/mimetypes/image-vnd.djvu.png \
72x72/mimetypes/image-vnd.djvu.png \
96x96/mimetypes/image-vnd.djvu.png \
128x128/mimetypes/image-vnd.djvu.png \
256x256/mimetypes/image-vnd.djvu.png 

icons: $(PNGICONS) $(SVGICONS)

if HAVE_CONVERSION_RSVG
convert_icons_process = \
s=`echo $@ | sed -e 's/[a-z]*\([0-9]*\).*/\1/'`; \
${RSVG} \
-w $${s} \
-h $${s} -o $@ $<
endif

if HAVE_CONVERSION_INKSCAPE
convert_icons_process = \
s=`echo $@ | sed -e 's/[a-z]*\([0-9]*\).*/\1/'`; \
${INKSCAPE} \
--without-gui \
--export-width=$${s} \
--export-height=$${s} \
--export-png=$@ $<
endif

if HAVE_CONVERSION_CONVERT
convert_icons_process = \
s=`echo $@ | sed -e 's/[a-z]*\([0-9]*\).*/\1/'`; \
${CONVERT} \
-geometry $${s}x$${s} \
-depth 8 \
-background none $< $@
endif

png_verbose = $(png_verbose_@AM_V@)
png_verbose_ = $(png_verbose_@AM_DEFAULT_V@)
png_verbose_0 = @echo "  PNG     " $@;

${PNGICONS}: djvu.svg Makefile
	$(AM_V_at)$(MKDIR_P) `dirname $@`
if HAVE_CONVERSION_TOOL
	$(png_verbose)$(convert_icons_process)
else
	$(AM_V_at)s=`echo $@ | sed -e 's/[a-z]*\([0-9]*\).*/\1/'`; \
	  cp ${top_srcdir}/desktopfiles/prebuilt-hi$${s}-djvu.png $@
endif

svg_process =\
cat $< | gzip -n >$@

svg_verbose = $(svg_verbose_@AM_V@)
svg_verbose_ = $(svg_verbose_@AM_DEFAULT_V@)
svg_verbose_0 = @echo "  SVG     " $@;

$(SVGICONS): djvu.svg Makefile
	$(AM_V_at)$(MKDIR_P) `dirname $@`
	$(AM_V_at)rm -f $@
	$(svg_verbose)$(svg_process)

iconsdir = $(datadir)/icons/hicolor
nobase_icons_DATA = $(PNGICONS) $(SVGICONS)

# mime
# | No longer install mime type description
# | because it conflicts with the defaults provided
# | by shared-mime-info.
# mimedir = $(datadir)/mime/packages
# dist_mime_DATA = djvulibre-mime.xml

# hooks

install-data-hook: update-mime
uninstall-hook: update-mime

update-mime:
	echo "*** You must still update various caches with:" ; \
	echo "*** # update-mime-database $(datadir)/mime" ; \
	echo "*** # update-icon-caches $(iconsdir)" 

# extra

EXTRA_DIST = \
djvu.svg \
prebuilt-hi128-djvu.png \
prebuilt-hi16-djvu.png \
prebuilt-hi20-djvu.png \
prebuilt-hi22-djvu.png \
prebuilt-hi24-djvu.png \
prebuilt-hi256-djvu.png \
prebuilt-hi32-djvu.png \
prebuilt-hi48-djvu.png \
prebuilt-hi64-djvu.png \
prebuilt-hi72-djvu.png \
prebuilt-hi96-djvu.png

# clean

MOSTLYCLEANFILES = $(PNGICONS) $(SVGICONS) 

mostlyclean-local:
	$(AM_V_at)for n in $(PNGICONS) $(SVGICONS) ; do m=`dirname "$$n"` ; \
	    while test "$$m" != "." ; do rmdir $$m ; m=`dirname "$$m"` ; done ; \
	done

