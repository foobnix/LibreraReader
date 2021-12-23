#include "fitz-imp.h"

#define SUBSCRIPT_OFFSET 0.2f
#define SUPERSCRIPT_OFFSET -0.2f

#include <ft2build.h>
#include FT_FREETYPE_H

/* XML, HTML and plain-text output */

static void
fz_print_style_begin(fz_context *ctx, fz_output *out, fz_stext_style *style)
{
	int script = style->script;
	fz_write_printf(ctx, out, "<span class=\"s%d\">", style->id);
	while (script-- > 0)
		fz_write_printf(ctx, out, "<sup>");
	while (++script < 0)
		fz_write_printf(ctx, out, "<sub>");
}

static void
fz_print_style_end(fz_context *ctx, fz_output *out, fz_stext_style *style)
{
	int script = style->script;
	while (script-- > 0)
		fz_write_printf(ctx, out, "</sup>");
	while (++script < 0)
		fz_write_printf(ctx, out, "</sub>");
	fz_write_printf(ctx, out, "</span>");
}

static void
fz_print_style(fz_context *ctx, fz_output *out, fz_stext_style *style)
{
	const char *name = fz_font_name(ctx, style->font);
	const char *s = strchr(name, '+');
	s = s ? s + 1 : name;
	fz_write_printf(ctx, out, "span.s%d{font-family:\"%s\";font-size:%gpt;",
		style->id, s, style->size);
	if (fz_font_is_italic(ctx, style->font))
		fz_write_printf(ctx, out, "font-style:italic;");
	if (fz_font_is_bold(ctx, style->font))
		fz_write_printf(ctx, out, "font-weight:bold;");
	fz_write_printf(ctx, out, "}\n");
}

void
fz_print_stext_sheet(fz_context *ctx, fz_output *out, fz_stext_sheet *sheet)
{
	fz_stext_style *style;
	for (style = sheet->style; style; style = style->next)
		fz_print_style(ctx, out, style);
}

void
send_data_base64_stext(fz_context *ctx, fz_output *out, fz_buffer *buffer)
{
	size_t i, len;
	static const char set[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	len = buffer->len/3;
	for (i = 0; i < len; i++)
	{
		int c = buffer->data[3*i];
		int d = buffer->data[3*i+1];
		int e = buffer->data[3*i+2];
		if ((i & 15) == 0)
			fz_write_printf(ctx, out, "\n");
		fz_write_printf(ctx, out, "%c%c%c%c", set[c>>2], set[((c&3)<<4)|(d>>4)], set[((d&15)<<2)|(e>>6)], set[e & 63]);
	}
	i *= 3;
	switch (buffer->len-i)
	{
		case 2:
		{
			int c = buffer->data[i];
			int d = buffer->data[i+1];
			fz_write_printf(ctx, out, "%c%c%c=", set[c>>2], set[((c&3)<<4)|(d>>4)], set[((d&15)<<2)]);
			break;
		}
	case 1:
		{
			int c = buffer->data[i];
			fz_write_printf(ctx, out, "%c%c==", set[c>>2], set[(c&3)<<4]);
			break;
		}
	default:
	case 0:
		break;
	}
}

void
fz_print_stext_page_html(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	int block_n, line_n, ch_n;
	fz_stext_style *style = NULL;
	fz_stext_line *line;
	fz_stext_span *span;
	void *last_region = NULL;

	fz_write_printf(ctx, out, "<div class=\"page\">\n");

	for (block_n = 0; block_n < page->len; block_n++)
	{
		switch (page->blocks[block_n].type)
		{
		case FZ_PAGE_BLOCK_TEXT:
		{
			fz_stext_block * block = page->blocks[block_n].u.text;
			fz_write_printf(ctx, out, "<div class=\"block\"><p>\n");
			for (line_n = 0; line_n < block->len; line_n++)
			{
				int lastcol=-1;
				line = &block->lines[line_n];
				style = NULL;

				if (line->region != last_region)
				{
					if (last_region)
						fz_write_printf(ctx, out, "</div>");
					fz_write_printf(ctx, out, "<div class=\"metaline\">");
					last_region = line->region;
				}
				fz_write_printf(ctx, out, "<div class=\"line\"");
#ifdef DEBUG_INTERNALS
				if (line->region)
					fz_write_printf(ctx, out, " region=\"%x\"", line->region);
#endif
				fz_write_printf(ctx, out, ">");
				for (span = line->first_span; span; span = span->next)
				{
					float size = fz_matrix_expansion(&span->transform);
					float base_offset = span->base_offset / size;

					if (lastcol != span->column)
					{
						if (lastcol >= 0)
						{
							fz_write_printf(ctx, out, "</div>");
						}
						/* If we skipped any columns then output some spacer spans */
						while (lastcol < span->column-1)
						{
							fz_write_printf(ctx, out, "<div class=\"cell\"></div>");
							lastcol++;
						}
						lastcol++;
						/* Now output the span to contain this entire column */
						fz_write_printf(ctx, out, "<div class=\"cell\" style=\"");
						{
							fz_stext_span *sn;
							for (sn = span->next; sn; sn = sn->next)
							{
								if (sn->column != lastcol)
									break;
							}
							fz_write_printf(ctx, out, "width:%g%%;align:%s", span->column_width, (span->align == 0 ? "left" : (span->align == 1 ? "center" : "right")));
						}
						if (span->indent > 1)
							fz_write_printf(ctx, out, ";padding-left:1em;text-indent:-1em");
						if (span->indent < -1)
							fz_write_printf(ctx, out, ";text-indent:1em");
						fz_write_printf(ctx, out, "\">");
					}
#ifdef DEBUG_INTERNALS
					fz_write_printf(ctx, out, "<span class=\"internal_span\"");
					if (span->column)
						fz_write_printf(ctx, out, " col=\"%x\"", span->column);
					fz_write_printf(ctx, out, ">");
#endif
					if (span->spacing >= 1)
						fz_write_printf(ctx, out, " ");
					if (base_offset > SUBSCRIPT_OFFSET)
						fz_write_printf(ctx, out, "<sub>");
					else if (base_offset < SUPERSCRIPT_OFFSET)
						fz_write_printf(ctx, out, "<sup>");
					for (ch_n = 0; ch_n < span->len; ch_n++)
					{
						fz_stext_char *ch = &span->text[ch_n];
						if (style != ch->style)
						{
							if (style)
								fz_print_style_end(ctx, out, style);
							fz_print_style_begin(ctx, out, ch->style);
							style = ch->style;
						}

						if (ch->c == '<')
							fz_write_printf(ctx, out, "&lt;");
						else if (ch->c == '>')
							fz_write_printf(ctx, out, "&gt;");
						else if (ch->c == '&')
							fz_write_printf(ctx, out, "&amp;");
						else if (ch->c >= 32 && ch->c <= 127)
							fz_write_printf(ctx, out, "%c", ch->c);
						else
							fz_write_printf(ctx, out, "&#x%x;", ch->c);
					}
					if (style)
					{
						fz_print_style_end(ctx, out, style);
						style = NULL;
					}
					if (base_offset > SUBSCRIPT_OFFSET)
						fz_write_printf(ctx, out, "</sub>");
					else if (base_offset < SUPERSCRIPT_OFFSET)
						fz_write_printf(ctx, out, "</sup>");
#ifdef DEBUG_INTERNALS
					fz_write_printf(ctx, out, "</span>");
#endif
				}
				/* Close our floating span */
				fz_write_printf(ctx, out, "</div>");
				/* Close the line */
				fz_write_printf(ctx, out, "</div>");
				fz_write_printf(ctx, out, "\n");
			}
			/* Close the metaline */
			fz_write_printf(ctx, out, "</div>");
			last_region = NULL;
			fz_write_printf(ctx, out, "</p></div>\n");
			break;
		}
		case FZ_PAGE_BLOCK_IMAGE:
		{
			fz_image_block *image = page->blocks[block_n].u.image;
			fz_compressed_buffer *buffer = fz_compressed_image_buffer(ctx, image->image);
			fz_write_printf(ctx, out, "<img width=%d height=%d src=\"data:", image->image->w, image->image->h);
			switch (buffer == NULL ? FZ_IMAGE_JPX : buffer->params.type)
			{
			case FZ_IMAGE_JPEG:
				fz_write_printf(ctx, out, "image/jpeg;base64,");
				send_data_base64_stext(ctx, out, buffer->buffer);
				break;
			case FZ_IMAGE_PNG:
				fz_write_printf(ctx, out, "image/png;base64,");
				send_data_base64_stext(ctx, out, buffer->buffer);
				break;
			default:
				{
					fz_buffer *buf = fz_new_buffer_from_image_as_png(ctx, image->image);
					fz_write_printf(ctx, out, "image/png;base64,");
					send_data_base64_stext(ctx, out, buf);
					fz_drop_buffer(ctx, buf);
					break;
				}
			}
			fz_write_printf(ctx, out, "\">\n");
			break;
		}
		}
	}

	fz_write_printf(ctx, out, "</div>\n");
}

void
fz_print_stext_page_xml(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	int block_n;

	fz_write_printf(ctx, out, "<page width=\"%g\" height=\"%g\">\n",
		page->mediabox.x1 - page->mediabox.x0,
		page->mediabox.y1 - page->mediabox.y0);

	for (block_n = 0; block_n < page->len; block_n++)
	{
		switch (page->blocks[block_n].type)
		{
		case FZ_PAGE_BLOCK_TEXT:
		{
			fz_stext_block *block = page->blocks[block_n].u.text;
			fz_stext_line *line;
			const char *s;

			fz_write_printf(ctx, out, "<block bbox=\"%g %g %g %g\">\n",
				block->bbox.x0, block->bbox.y0, block->bbox.x1, block->bbox.y1);
			for (line = block->lines; line < block->lines + block->len; line++)
			{
				fz_stext_span *span;
				fz_write_printf(ctx, out, "<line bbox=\"%g %g %g %g\">\n",
					line->bbox.x0, line->bbox.y0, line->bbox.x1, line->bbox.y1);
				for (span = line->first_span; span; span = span->next)
				{
					fz_stext_style *style = NULL;
					const char *name = NULL;
					int char_num;
					for (char_num = 0; char_num < span->len; char_num++)
					{
						fz_stext_char *ch = &span->text[char_num];
						if (ch->style != style)
						{
							if (style)
							{
								fz_write_printf(ctx, out, "</span>\n");
							}
							style = ch->style;
							name = fz_font_name(ctx, style->font);
							s = strchr(name, '+');
							s = s ? s + 1 : name;
							fz_write_printf(ctx, out, "<span bbox=\"%g %g %g %g\" font=\"%s\" size=\"%g\">\n",
								span->bbox.x0, span->bbox.y0, span->bbox.x1, span->bbox.y1,
								s, style->size);
						}
						{
							fz_rect rect;
							fz_stext_char_bbox(ctx, &rect, span, char_num);
							fz_write_printf(ctx, out, "<char bbox=\"%g %g %g %g\" x=\"%g\" y=\"%g\" c=\"",
								rect.x0, rect.y0, rect.x1, rect.y1, ch->p.x, ch->p.y);
						}
						switch (ch->c)
						{
						case '<': fz_write_printf(ctx, out, "&lt;"); break;
						case '>': fz_write_printf(ctx, out, "&gt;"); break;
						case '&': fz_write_printf(ctx, out, "&amp;"); break;
						case '"': fz_write_printf(ctx, out, "&quot;"); break;
						case '\'': fz_write_printf(ctx, out, "&apos;"); break;
						default:
							if (ch->c >= 32 && ch->c <= 127)
								fz_write_printf(ctx, out, "%c", ch->c);
							else
								fz_write_printf(ctx, out, "&#x%x;", ch->c);
							break;
						}
						fz_write_printf(ctx, out, "\"/>\n");
					}
					if (style)
						fz_write_printf(ctx, out, "</span>\n");
				}
				fz_write_printf(ctx, out, "</line>\n");
			}
			fz_write_printf(ctx, out, "</block>\n");
			break;
		}
		case FZ_PAGE_BLOCK_IMAGE:
		{
			break;
		}
	}
	}
	fz_write_printf(ctx, out, "</page>\n");
}

void
fz_print_stext_page(fz_context *ctx, fz_output *out, fz_stext_page *page)
{
	int block_n;

	for (block_n = 0; block_n < page->len; block_n++)
	{
		switch (page->blocks[block_n].type)
		{
		case FZ_PAGE_BLOCK_TEXT:
		{
			fz_stext_block *block = page->blocks[block_n].u.text;
			fz_stext_line *line;
			fz_stext_char *ch;
			char utf[10];
			int i, n;

			for (line = block->lines; line < block->lines + block->len; line++)
			{
				fz_stext_span *span;
				for (span = line->first_span; span; span = span->next)
				{
					for (ch = span->text; ch < span->text + span->len; ch++)
					{
						n = fz_runetochar(utf, ch->c);
						for (i = 0; i < n; i++)
							fz_write_printf(ctx, out, "%c", utf[i]);
					}
				}
				fz_write_printf(ctx, out, "\n");
			}
			fz_write_printf(ctx, out, "\n");
			break;
		}
		case FZ_PAGE_BLOCK_IMAGE:
			break;
		}
	}
}
