#include "fitz-imp.h"

#define SUBSCRIPT_OFFSET 0.2f
#define SUPERSCRIPT_OFFSET -0.2f

#include <ft2build.h>
#include FT_FREETYPE_H

/* HTML output (visual formatting with preserved layout) */

static int
detect_super_script(fz_stext_line *line, fz_stext_char *ch)
{
	if (line->wmode == 0 && line->dir.x == 1 && line->dir.y == 0)
		return ch->origin.y < line->first_char->origin.y - ch->size * 0.1f;
	return 0;
}

static const char *
font_full_name(fz_context *ctx, fz_font *font)
{
	const char *name = fz_font_name(ctx, font);
	const char *s = strchr(name, '+');
	return s ? s + 1 : name;
}

static void
font_family_name(fz_context *ctx, fz_font *font, char *buf, int size, int is_mono, int is_serif)
{
	const char *name = font_full_name(ctx, font);
	char *s;
	fz_strlcpy(buf, name, size);
	s = strrchr(buf, '-');
	if (s)
		*s = 0;
	if (is_mono)
		fz_strlcat(buf, ",monospace", size);
	else
		fz_strlcat(buf, is_serif ? ",serif" : ",sans-serif", size);
}

static void
fz_print_style_begin_html(fz_context *ctx, fz_output *out, fz_font *font, float size, int sup)
{
	char family[80];

	int is_bold = fz_font_is_bold(ctx, font);
	int is_italic = fz_font_is_italic(ctx, font);
	int is_serif = fz_font_is_serif(ctx, font);
	int is_mono = fz_font_is_monospaced(ctx, font);

	font_family_name(ctx, font, family, sizeof family, is_mono, is_serif);

	if (sup) fz_write_string(ctx, out, "<sup>");
	if (is_mono) fz_write_string(ctx, out, "<tt>");
	if (is_bold) fz_write_string(ctx, out, "<b>");
	if (is_italic) fz_write_string(ctx, out, "<i>");
	fz_write_printf(ctx, out, "<span style=\"font-family:%s;font-size:%gpt;\">", family, size);
}

static void
fz_print_style_end_html(fz_context *ctx, fz_output *out, fz_font *font, float size, int sup)
{
	int is_mono = fz_font_is_monospaced(ctx, font);
	int is_bold = fz_font_is_bold(ctx,font);
	int is_italic = fz_font_is_italic(ctx, font);

	fz_write_string(ctx, out, "</span>");
	if (is_italic) fz_write_string(ctx, out, "</i>");
	if (is_bold) fz_write_string(ctx, out, "</b>");
	if (is_mono) fz_write_string(ctx, out, "</tt>");
	if (sup) fz_write_string(ctx, out, "</sup>");
}

static void
fz_print_stext_image_as_html(fz_context *ctx, fz_output *out, fz_stext_block *block)
{
	int x = block->bbox.x0;
	int y = block->bbox.y0;
	int w = block->bbox.x1 - block->bbox.x0;
	int h = block->bbox.y1 - block->bbox.y0;

	fz_write_printf(ctx, out, "<img style=\"top:%dpt;left:%dpt;width:%dpt;height:%dpt\" src=\"data:", y, x, w, h);
	fz_write_image_as_data_uri(ctx, out, block->u.i.image);
	fz_write_string(ctx, out, "\">\n");
}

void
fz_print_stext_block_as_html(fz_context *ctx, fz_output *out, fz_stext_block *block)
{
	fz_stext_line *line;
	fz_stext_char *ch;
	int x, y;

	fz_font *font = NULL;
	float size = 0;
	int sup = 0;

	for (line = block->u.t.first_line; line; line = line->next)
	{
		x = line->bbox.x0;
		y = line->bbox.y0;

		fz_write_printf(ctx, out, "<p style=\"top:%dpt;left:%dpt;\">", y, x);
		font = NULL;

		for (ch = line->first_char; ch; ch = ch->next)
		{
			int ch_sup = detect_super_script(line, ch);
			if (ch->font != font || ch->size != size)
			{
				if (font)
					fz_print_style_end_html(ctx, out, font, size, sup);
				font = ch->font;
				size = ch->size;
				sup = ch_sup;
				fz_print_style_begin_html(ctx, out, font, size, sup);
			}

			switch (ch->c)
			{
			default:
				if (ch->c >= 32 && ch->c <= 127)
					fz_write_byte(ctx, out, ch->c);
				else
					fz_write_printf(ctx, out, "&#x%x;", ch->c);
				break;
			case '<': fz_write_string(ctx, out, "&lt;"); break;
			case '>': fz_write_string(ctx, out, "&gt;"); break;
			case '&': fz_write_string(ctx, out, "&amp;"); break;
			case '"': fz_write_string(ctx, out, "&quot;"); break;
			case '\'': fz_write_string(ctx, out, "&apos;"); break;
			}
		}

		if (font)
			fz_print_style_end_html(ctx, out, font, size, sup);

		fz_write_string(ctx, out, "</p>\n");
	}
}

void
fz_print_stext_page_as_html(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;

	int w = page->mediabox.x1 - page->mediabox.x0;
	int h = page->mediabox.y1 - page->mediabox.y0;

	fz_write_printf(ctx, out, "<div style=\"width:%dpt;height:%dpt\">\n", w, h);

	for (block = page->first_block; block; block = block->next)
	{
		if (block->type == FZ_STEXT_BLOCK_IMAGE)
			fz_print_stext_image_as_html(ctx, out, block);
		else if (block->type == FZ_STEXT_BLOCK_TEXT)
			fz_print_stext_block_as_html(ctx, out, block);
	}

	fz_write_string(ctx, out, "</div>\n");
}

void
fz_print_stext_header_as_html(fz_context *ctx, fz_output *out)
{
	fz_write_string(ctx, out, "<!DOCTYPE html>\n");
	fz_write_string(ctx, out, "<html>\n");
	fz_write_string(ctx, out, "<head>\n");
	fz_write_string(ctx, out, "<style>\n");
	fz_write_string(ctx, out, "body{background-color:gray}\n");
	fz_write_string(ctx, out, "div{position:relative;background-color:white;margin:1em auto}\n");
	fz_write_string(ctx, out, "p{position:absolute;margin:0}\n");
	fz_write_string(ctx, out, "img{position:absolute}\n");
	fz_write_string(ctx, out, "</style>\n");
	fz_write_string(ctx, out, "</head>\n");
	fz_write_string(ctx, out, "<body>\n");
}

void
fz_print_stext_trailer_as_html(fz_context *ctx, fz_output *out)
{
	fz_write_string(ctx, out, "</body>\n");
	fz_write_string(ctx, out, "</html>\n");
}

/* XHTML output (semantic, little layout, suitable for reflow) */

static void
fz_print_stext_image_as_xhtml(fz_context *ctx, fz_output *out, fz_stext_block *block)
{
	int w = block->bbox.x1 - block->bbox.x0;
	int h = block->bbox.y1 - block->bbox.y0;

	fz_write_printf(ctx, out, "<p><img width=\"%d\" height=\"%d\" src=\"data:", w, h);
	fz_write_image_as_data_uri(ctx, out, block->u.i.image);
	fz_write_string(ctx, out, "\"/></p>\n");
}

static void
fz_print_style_begin_xhtml(fz_context *ctx, fz_output *out, fz_font *font, float size, int sup)
{
	int is_mono = fz_font_is_monospaced(ctx, font);
	int is_bold = fz_font_is_bold(ctx, font);
	int is_italic = fz_font_is_italic(ctx, font);

	if (sup)
		fz_write_string(ctx, out, "<sup>");
	if (is_mono)
		fz_write_string(ctx, out, "<tt>");
	if (is_bold)
		fz_write_string(ctx, out, "<b>");
	if (is_italic)
		fz_write_string(ctx, out, "<i>");
}

static void
fz_print_style_end_xhtml(fz_context *ctx, fz_output *out, fz_font *font, float size, int sup)
{
	int is_mono = fz_font_is_monospaced(ctx, font);
	int is_bold = fz_font_is_bold(ctx, font);
	int is_italic = fz_font_is_italic(ctx, font);

	if (is_italic)
		fz_write_string(ctx, out, "</i>");
	if (is_bold)
		fz_write_string(ctx, out, "</b>");
	if (is_mono)
		fz_write_string(ctx, out, "</tt>");
	if (sup)
		fz_write_string(ctx, out, "</sup>");
}

static void fz_print_stext_block_as_xhtml(fz_context *ctx, fz_output *out, fz_stext_block *block)
{
	fz_stext_line *line;
	fz_stext_char *ch;

	fz_font *font = NULL;
	float size = 0;
	int sup = 0;

	fz_write_string(ctx, out, "<p>");

	for (line = block->u.t.first_line; line; line = line->next)
	{
		for (ch = line->first_char; ch; ch = ch->next)
		{
			int ch_sup = detect_super_script(line, ch);
			if (ch->font != font || ch->size != size || ch_sup != sup)
			{
				if (font)
					fz_print_style_end_xhtml(ctx, out, font, size, sup);
				font = ch->font;
				size = ch->size;
				sup = ch_sup;
				fz_print_style_begin_xhtml(ctx, out, font, size, sup);
			}

			switch (ch->c)
			{
			default:
				if (ch->c >= 32 && ch->c <= 127)
					fz_write_byte(ctx, out, ch->c);
				else
					fz_write_printf(ctx, out, "&#x%x;", ch->c);
				break;
			case '<': fz_write_string(ctx, out, "&lt;"); break;
			case '>': fz_write_string(ctx, out, "&gt;"); break;
			case '&': fz_write_string(ctx, out, "&amp;"); break;
			case '"': fz_write_string(ctx, out, "&quot;"); break;
			case '\'': fz_write_string(ctx, out, "&apos;"); break;
			}
		}
	}

	if (font)
		fz_print_style_end_xhtml(ctx, out, font, size, sup);
	fz_write_string(ctx, out, "</p>\n");
}

void
fz_print_stext_page_as_xhtml(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;

	fz_write_string(ctx, out, "<div>\n");

	for (block = page->first_block; block; block = block->next)
	{
		if (block->type == FZ_STEXT_BLOCK_IMAGE)
			fz_print_stext_image_as_xhtml(ctx, out, block);
		else if (block->type == FZ_STEXT_BLOCK_TEXT)
			fz_print_stext_block_as_xhtml(ctx, out, block);
	}

	fz_write_string(ctx, out, "</div>\n");
}

void
fz_print_stext_header_as_xhtml(fz_context *ctx, fz_output *out)
{
	fz_write_string(ctx, out, "<?xml version=\"1.0\"?>\n");
	fz_write_string(ctx, out, "<!DOCTYPE html");
	fz_write_string(ctx, out, " PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"");
	fz_write_string(ctx, out, " \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
	fz_write_string(ctx, out, "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
	fz_write_string(ctx, out, "<head>\n");
	fz_write_string(ctx, out, "<style>\n");
	fz_write_string(ctx, out, "body{background-color:gray}\n");
	fz_write_string(ctx, out, "div{background-color:white;margin:1em;padding:1em}\n");
	fz_write_string(ctx, out, "p{white-space:pre-wrap}\n");
	fz_write_string(ctx, out, "</style>\n");
	fz_write_string(ctx, out, "</head>\n");
	fz_write_string(ctx, out, "<body>\n");
}

void
fz_print_stext_trailer_as_xhtml(fz_context *ctx, fz_output *out)
{
	fz_write_string(ctx, out, "</body>\n");
	fz_write_string(ctx, out, "</html>\n");
}

/* Detailed XML dump of the entire structured text data */

void
fz_print_stext_page_as_xml(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;
	fz_stext_line *line;
	fz_stext_char *ch;

	fz_write_printf(ctx, out, "<page width=\"%g\" height=\"%g\">\n",
		page->mediabox.x1 - page->mediabox.x0,
		page->mediabox.y1 - page->mediabox.y0);

	for (block = page->first_block; block; block = block->next)
	{
		switch (block->type)
		{
		case FZ_STEXT_BLOCK_TEXT:
			fz_write_printf(ctx, out, "<block bbox=\"%g %g %g %g\">\n",
					block->bbox.x0, block->bbox.y0, block->bbox.x1, block->bbox.y1);
			for (line = block->u.t.first_line; line; line = line->next)
			{
				fz_font *font = NULL;
				float size = 0;
				const char *name = NULL;

				fz_write_printf(ctx, out, "<line bbox=\"%g %g %g %g\" wmode=\"%d\" dir=\"%g %g\">\n",
						line->bbox.x0, line->bbox.y0, line->bbox.x1, line->bbox.y1,
						line->wmode,
						line->dir.x, line->dir.y);

				for (ch = line->first_char; ch; ch = ch->next)
				{
					if (ch->font != font || ch->size != size)
					{
						if (font)
							fz_write_string(ctx, out, "</font>\n");
						font = ch->font;
						size = ch->size;
						name = font_full_name(ctx, font);
						fz_write_printf(ctx, out, "<font name=\"%s\" size=\"%g\">\n", name, size);
					}
					fz_write_printf(ctx, out, "<char bbox=\"%g %g %g %g\" x=\"%g\" y=\"%g\" c=\"",
							ch->bbox.x0, ch->bbox.y0, ch->bbox.x1, ch->bbox.y1, ch->origin.x, ch->origin.y);
					switch (ch->c)
					{
					case '<': fz_write_string(ctx, out, "&lt;"); break;
					case '>': fz_write_string(ctx, out, "&gt;"); break;
					case '&': fz_write_string(ctx, out, "&amp;"); break;
					case '"': fz_write_string(ctx, out, "&quot;"); break;
					case '\'': fz_write_string(ctx, out, "&apos;"); break;
					default:
						   if (ch->c >= 32 && ch->c <= 127)
							   fz_write_printf(ctx, out, "%c", ch->c);
						   else
							   fz_write_printf(ctx, out, "&#x%x;", ch->c);
						   break;
					}
					fz_write_string(ctx, out, "\"/>\n");
				}

				if (font)
					fz_write_string(ctx, out, "</font>\n");

				fz_write_string(ctx, out, "</line>\n");
			}
			fz_write_string(ctx, out, "</block>\n");
			break;

		case FZ_STEXT_BLOCK_IMAGE:
			fz_write_printf(ctx, out, "<image bbox=\"%g %g %g %g\" />\n",
					block->bbox.x0, block->bbox.y0, block->bbox.x1, block->bbox.y1);
			break;
		}
	}
	fz_write_string(ctx, out, "</page>\n");
}

/* Plain text */

void
fz_print_stext_page_as_text(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;
	fz_stext_line *line;
	fz_stext_char *ch;
	char utf[10];
	int i, n;

	for (block = page->first_block; block; block = block->next)
	{
		if (block->type == FZ_STEXT_BLOCK_TEXT)
		{
			for (line = block->u.t.first_line; line; line = line->next)
			{
					int is_bold = fz_font_is_bold(ctx, line->first_char->font);
					int is_italic = fz_font_is_italic(ctx, line->first_char->font);

					if (is_bold){						
						fz_write_printf(ctx,out,"<b>");
					}

					if (is_italic){
						fz_write_printf(ctx,out,"<i>");
					}

				for (ch = line->first_char; ch; ch = ch->next)
				{
					n = fz_runetochar(utf, ch->c);
					for (i = 0; i < n; i++)
						fz_write_byte(ctx, out, utf[i]);
				}

				    if(is_bold){
						fz_write_printf(ctx,out,"</b>&nbsp; ");
					}

					if(is_italic){
						fz_write_printf(ctx,out,"</i>&nbsp; ");
					}

				fz_write_string(ctx, out, "&nbsp; ");
				//fz_write_printf(ctx, out, "<br/>");
			}
			//fz_write_string(ctx, out, "\n");
			fz_write_printf(ctx, out, "<br/>");
		}
	}
}

/* Text output writer */

enum {
	FZ_FORMAT_TEXT,
	FZ_FORMAT_HTML,
	FZ_FORMAT_XHTML,
	FZ_FORMAT_STEXT,
};

typedef struct fz_text_writer_s fz_text_writer;

struct fz_text_writer_s
{
	fz_document_writer super;
	int format;
	fz_stext_options opts;
	fz_stext_page *page;
	fz_output *out;
};

static fz_device *
text_begin_page(fz_context *ctx, fz_document_writer *wri_, const fz_rect *mediabox)
{
	fz_text_writer *wri = (fz_text_writer*)wri_;

	if (wri->page)
	{
		fz_drop_stext_page(ctx, wri->page);
		wri->page = NULL;
	}

	wri->page = fz_new_stext_page(ctx, mediabox);
	return fz_new_stext_device(ctx, wri->page, &wri->opts);
}

static void
text_end_page(fz_context *ctx, fz_document_writer *wri_, fz_device *dev)
{
	fz_text_writer *wri = (fz_text_writer*)wri_;

	fz_try(ctx)
		fz_close_device(ctx, dev);
	fz_always(ctx)
		fz_drop_device(ctx, dev);
	fz_catch(ctx)
		fz_rethrow(ctx);

	switch (wri->format)
	{
	default:
	case FZ_FORMAT_TEXT:
		fz_print_stext_page_as_text(ctx, wri->out, wri->page);
		break;
	case FZ_FORMAT_HTML:
		fz_print_stext_page_as_html(ctx, wri->out, wri->page);
		break;
	case FZ_FORMAT_XHTML:
		fz_print_stext_page_as_xhtml(ctx, wri->out, wri->page);
		break;
	case FZ_FORMAT_STEXT:
		fz_print_stext_page_as_xml(ctx, wri->out, wri->page);
		break;
	}

	fz_drop_stext_page(ctx, wri->page);
	wri->page = NULL;
}

static void
text_close_writer(fz_context *ctx, fz_document_writer *wri_)
{
	fz_text_writer *wri = (fz_text_writer*)wri_;
	switch (wri->format)
	{
	case FZ_FORMAT_HTML:
		fz_print_stext_trailer_as_html(ctx, wri->out);
		break;
	case FZ_FORMAT_XHTML:
		fz_print_stext_trailer_as_xhtml(ctx, wri->out);
		break;
	case FZ_FORMAT_STEXT:
		fz_write_string(ctx, wri->out, "</document>\n");
		break;
	}
	fz_close_output(ctx, wri->out);
}

static void
text_drop_writer(fz_context *ctx, fz_document_writer *wri_)
{
	fz_text_writer *wri = (fz_text_writer*)wri_;
	fz_drop_stext_page(ctx, wri->page);
	fz_drop_output(ctx, wri->out);
}

fz_document_writer *
fz_new_text_writer(fz_context *ctx, const char *format, const char *path, const char *args)
{
	fz_text_writer *wri;

	wri = fz_new_derived_document_writer(ctx, fz_text_writer, text_begin_page, text_end_page, text_close_writer, text_drop_writer);
	fz_try(ctx)
	{
		fz_parse_stext_options(ctx, &wri->opts, args);

		wri->format = FZ_FORMAT_TEXT;
		if (!strcmp(format, "text"))
			wri->format = FZ_FORMAT_TEXT;
		else if (!strcmp(format, "html"))
			wri->format = FZ_FORMAT_HTML;
		else if (!strcmp(format, "xhtml"))
			wri->format = FZ_FORMAT_XHTML;
		else if (!strcmp(format, "stext"))
			wri->format = FZ_FORMAT_STEXT;

		wri->out = fz_new_output_with_path(ctx, path ? path : "out.txt", 0);

		switch (wri->format)
		{
		case FZ_FORMAT_HTML:
			fz_print_stext_header_as_html(ctx, wri->out);
			break;
		case FZ_FORMAT_XHTML:
			fz_print_stext_header_as_xhtml(ctx, wri->out);
			break;
		case FZ_FORMAT_STEXT:
			fz_write_string(ctx, wri->out, "<?xml version=\"1.0\"?>\n");
			fz_write_string(ctx, wri->out, "<document>\n");
			break;
		}
	}
	fz_catch(ctx)
	{
		fz_drop_output(ctx, wri->out);
		fz_free(ctx, wri);
		fz_rethrow(ctx);
	}

	return (fz_document_writer*)wri;
}

void
fz_print_stext_page_as_text_my(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	fz_stext_block *block;
	fz_stext_line *line;
	fz_stext_char *ch;
	char utf[10];
	int i, n;

	for (block = page->first_block; block; block = block->next)
	{

		if (block->type == FZ_STEXT_BLOCK_IMAGE){
			//fz_write_printf(ctx,out,"<b> FZ_STEXT_BLOCK_IMAGE !!!!!!!!!!!</b>");
			fz_print_stext_image_as_html(ctx, out, block);
		}else if (block->type == FZ_STEXT_BLOCK_TEXT)
		{
			for (line = block->u.t.first_line; line; line = line->next)
			{
					int is_bold = fz_font_is_bold(ctx, line->first_char->font);
					int is_italic = fz_font_is_italic(ctx, line->first_char->font);

					if (is_bold){
						fz_write_printf(ctx,out,"<b>");
					}

					if (is_italic){
						fz_write_printf(ctx,out,"<i>");
					}

				for (ch = line->first_char; ch; ch = ch->next)
				{
					n = fz_runetochar(utf, ch->c);
					for (i = 0; i < n; i++)
						fz_write_byte(ctx, out, utf[i]);
				}

				    if(is_bold){
						fz_write_printf(ctx,out,"</b>&nbsp; ");
					}

					if(is_italic){
						fz_write_printf(ctx,out,"</i>&nbsp; ");
					}

				fz_write_string(ctx, out, "&nbsp; ");
				//fz_write_printf(ctx, out, "<br/>");
			}
			//fz_write_string(ctx, out, "\n");
			fz_write_printf(ctx, out, "<br/>");
		}
	}
}
