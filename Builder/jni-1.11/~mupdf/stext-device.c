#include "mupdf/fitz.h"

/* Extract text into an unsorted span soup. */

#define LINE_DIST 0.9f
#define SPACE_DIST 0.15f
#define SPACE_MAX_DIST 0.8f
#define PARAGRAPH_DIST 0.5f

#undef DEBUG_SPANS
#undef DEBUG_INTERNALS
#undef DEBUG_LINE_HEIGHTS
#undef DEBUG_MASKS
#undef DEBUG_ALIGN
#undef DEBUG_INDENTS

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_ADVANCES_H

typedef struct fz_stext_device_s fz_stext_device;

typedef struct span_soup_s span_soup;

struct fz_stext_device_s
{
	fz_device super;
	fz_stext_sheet *sheet;
	fz_stext_page *page;
	span_soup *spans;
	fz_stext_span *cur_span;
	int lastchar;
	int flags;
};

const char *fz_stext_options_usage =
	"Structured text output options:\n"
	"\tpreserve-ligatures: do not expand all ligatures into constituent characters\n"
	"\tpreserve-whitespace: do not convert all whitespace characters into spaces\n"
	"\n";

static fz_rect *
add_point_to_rect(fz_rect *a, const fz_point *p)
{
	if (p->x < a->x0)
		a->x0 = p->x;
	if (p->x > a->x1)
		a->x1 = p->x;
	if (p->y < a->y0)
		a->y0 = p->y;
	if (p->y > a->y1)
		a->y1 = p->y;
	return a;
}

fz_rect *
fz_stext_char_bbox(fz_context *ctx, fz_rect *bbox, fz_stext_span *span, int i)
{
	fz_point a, d;
	const fz_point *max;
	fz_stext_char *ch;

	if (!span || i >= span->len)
	{
		*bbox = fz_empty_rect;
		return bbox;
	}
	ch = &span->text[i];
	if (i == span->len-1)
		max = &span->max;
	else
		max = &span->text[i+1].p;
	if (span->wmode == 0)
	{
		a.x = 0;
		a.y = span->ascender_max;
		d.x = 0;
		d.y = span->descender_min;
	}
	else
	{
		a.x = span->ascender_max;
		a.y = 0;
		d.x = span->descender_min;
		d.y = 0;
	}
	fz_transform_vector(&a, &span->transform);
	fz_transform_vector(&d, &span->transform);
	bbox->x0 = bbox->x1 = ch->p.x + a.x;
	bbox->y0 = bbox->y1 = ch->p.y + a.y;
	a.x += max->x;
	a.y += max->y;
	add_point_to_rect(bbox, &a);
	a.x = ch->p.x + d.x;
	a.y = ch->p.y + d.y;
	add_point_to_rect(bbox, &a);
	a.x = max->x + d.x;
	a.y = max->y + d.y;
	add_point_to_rect(bbox, &a);
	return bbox;
}

static void
add_bbox_to_span(fz_stext_span *span)
{
	fz_point a, d;
	fz_rect *bbox = &span->bbox;

	if (!span)
		return;
	if (span->wmode == 0)
	{
		a.x = 0;
		a.y = span->ascender_max;
		d.x = 0;
		d.y = span->descender_min;
	}
	else
	{
		a.x = span->ascender_max;
		a.y = 0;
		d.x = span->descender_min;
		d.y = 0;
	}
	fz_transform_vector(&a, &span->transform);
	fz_transform_vector(&d, &span->transform);
	bbox->x0 = bbox->x1 = span->min.x + a.x;
	bbox->y0 = bbox->y1 = span->min.y + a.y;
	a.x += span->max.x;
	a.y += span->max.y;
	add_point_to_rect(bbox, &a);
	a.x = span->min.x + d.x;
	a.y = span->min.y + d.y;
	add_point_to_rect(bbox, &a);
	a.x = span->max.x + d.x;
	a.y = span->max.y + d.y;
	add_point_to_rect(bbox, &a);
}

struct span_soup_s
{
	int len, cap;
	fz_stext_span **spans;
};

static span_soup *
new_span_soup(fz_context *ctx)
{
	span_soup *soup = fz_malloc_struct(ctx, span_soup);
	soup->len = 0;
	soup->cap = 0;
	soup->spans = NULL;
	return soup;
}

static void
free_span_soup(fz_context *ctx, span_soup *soup)
{
	int i;

	if (soup == NULL)
		return;
	for (i = 0; i < soup->len; i++)
	{
		fz_free(ctx, soup->spans[i]);
	}
	fz_free(ctx, soup->spans);
	fz_free(ctx, soup);
}

static void
add_span_to_soup(fz_context *ctx, span_soup *soup, fz_stext_span *span)
{
	if (span == NULL)
		return;
	if (soup->len == soup->cap)
	{
		int newcap = (soup->cap ? soup->cap * 2 : 16);
		soup->spans = fz_resize_array(ctx, soup->spans, newcap, sizeof(*soup->spans));
		soup->cap = newcap;
	}
	add_bbox_to_span(span);
	soup->spans[soup->len++] = span;
}

static fz_stext_line *
push_span(fz_context *ctx, fz_stext_device *tdev, fz_stext_span *span, int new_line, float distance)
{
	fz_stext_line *line;
	fz_stext_block *block;
	fz_stext_page *page = tdev->page;
	int prev_not_text = 0;

	if (page->len == 0 || page->blocks[page->len-1].type != FZ_PAGE_BLOCK_TEXT)
		prev_not_text = 1;

	if (new_line || prev_not_text)
	{
		float size = fz_matrix_expansion(&span->transform);
		/* So, a new line. Part of the same block or not? */
		if (distance == 0 || distance > size * 1.5 || distance < -size * PARAGRAPH_DIST || page->len == 0 || prev_not_text)
		{
			/* New block */
			if (page->len == page->cap)
			{
				int newcap = (page->cap ? page->cap*2 : 4);
				page->blocks = fz_resize_array(ctx, page->blocks, newcap, sizeof(*page->blocks));
				page->cap = newcap;
			}
			block = fz_malloc_struct(ctx, fz_stext_block);
			page->blocks[page->len].type = FZ_PAGE_BLOCK_TEXT;
			page->blocks[page->len].u.text = block;
			block->cap = 0;
			block->len = 0;
			block->lines = 0;
			block->bbox = fz_empty_rect;
			page->len++;
			distance = 0;
		}

		/* New line */
		block = page->blocks[page->len-1].u.text;
		if (block->len == block->cap)
		{
			int newcap = (block->cap ? block->cap*2 : 4);
			block->lines = fz_resize_array(ctx, block->lines, newcap, sizeof(*block->lines));
			block->cap = newcap;
		}
		block->lines[block->len].first_span = NULL;
		block->lines[block->len].last_span = NULL;
		block->lines[block->len].distance = distance;
		block->lines[block->len].bbox = fz_empty_rect;
		block->len++;
	}

	/* Find last line and append to it */
	block = page->blocks[page->len-1].u.text;
	line = &block->lines[block->len-1];

	fz_union_rect(&block->lines[block->len-1].bbox, &span->bbox);
	fz_union_rect(&block->bbox, &span->bbox);
	span->base_offset = (new_line ? 0 : distance);

	if (!line->first_span)
	{
		line->first_span = line->last_span = span;
		span->next = NULL;
	}
	else
	{
		line->last_span->next = span;
		line->last_span = span;
	}

	return line;
}

#if defined(DEBUG_SPANS) || defined(DEBUG_ALIGN) || defined(DEBUG_INDENTS)
static void
dump_span(fz_stext_span *s)
{
	int i;
	for (i=0; i < s->len; i++)
	{
		printf("%c", s->text[i].c);
	}
}
#endif

#ifdef DEBUG_ALIGN
static void
dump_line(fz_stext_line *line)
{
	int i;
	for (i=0; i < line->len; i++)
	{
		fz_stext_span *s = line->spans[i];
		if (s->spacing > 1)
			printf(" ");
		dump_span(s);
	}
	printf("\n");
}
#endif

static void
strain_soup(fz_context *ctx, fz_stext_device *tdev)
{
	span_soup *soup = tdev->spans;
	fz_stext_line *last_line = NULL;
	fz_stext_span *last_span = NULL;
	int span_num;

	if (soup == NULL)
		return;

	/* Really dumb implementation to match what we had before */
	for (span_num=0; span_num < soup->len; span_num++)
	{
		fz_stext_span *span = soup->spans[span_num];
		int new_line = 1;
		float distance = 0;
		float spacing = 0;
		soup->spans[span_num] = NULL;
		if (last_span)
		{
			/* If we have a last_span, we must have a last_line */
			/* Do span and last_line share the same baseline? */
			fz_point p, q, perp_r;
			float dot;
			float size = fz_matrix_expansion(&span->transform);

#ifdef DEBUG_SPANS
			{
				printf("Comparing: \"");
				dump_span(last_span);
				printf("\" and \"");
				dump_span(span);
				printf("\"\n");
			}
#endif

			p.x = last_line->first_span->max.x - last_line->first_span->min.x;
			p.y = last_line->first_span->max.y - last_line->first_span->min.y;
			fz_normalize_vector(&p);
			q.x = span->max.x - span->min.x;
			q.y = span->max.y - span->min.y;
			fz_normalize_vector(&q);
#ifdef DEBUG_SPANS
			printf("last_span=%g %g -> %g %g = %g %g\n", last_span->min.x, last_span->min.y, last_span->max.x, last_span->max.y, p.x, p.y);
			printf("span     =%g %g -> %g %g = %g %g\n", span->min.x, span->min.y, span->max.x, span->max.y, q.x, q.y);
#endif
			perp_r.y = last_line->first_span->min.x - span->min.x;
			perp_r.x = -(last_line->first_span->min.y - span->min.y);
			/* Check if p and q are parallel. If so, then this
			 * line is parallel with the last one. */
			dot = p.x * q.x + p.y * q.y;
			if (fabsf(dot) > 0.9995)
			{
				/* If we take the dot product of normalised(p) and
				 * perp(r), we get the perpendicular distance from
				 * one line to the next (assuming they are parallel). */
				distance = p.x * perp_r.x + p.y * perp_r.y;
				/* We allow 'small' distances of baseline changes
				 * to cope with super/subscript. FIXME: We should
				 * gather subscript/superscript information here. */
				new_line = (fabsf(distance) > size * LINE_DIST);
			}
			else
			{
				new_line = 1;
				distance = 0;
			}
			if (!new_line)
			{
				fz_point delta;

				delta.x = span->min.x - last_span->max.x;
				delta.y = span->min.y - last_span->max.y;

				spacing = (p.x * delta.x + p.y * delta.y);
				spacing = fabsf(spacing);
				/* Only allow changes in baseline (subscript/superscript etc)
				 * when the spacing is small. */
				if (spacing * fabsf(distance) > size * LINE_DIST && fabsf(distance) > size * 0.1f)
				{
					new_line = 1;
					distance = 0;
					spacing = 0;
				}
				else
				{
					spacing /= size * SPACE_DIST;
					/* Apply the same logic here as when we're adding chars to build spans. */
					if (spacing >= 1 && spacing < (SPACE_MAX_DIST/SPACE_DIST))
						spacing = 1;
				}
			}
#ifdef DEBUG_SPANS
			printf("dot=%g new_line=%d distance=%g size=%g spacing=%g\n", dot, new_line, distance, size, spacing);
#endif
		}
		span->spacing = spacing;
		last_line = push_span(ctx, tdev, span, new_line, distance);
		last_span = span;
	}
}

fz_stext_sheet *
fz_new_stext_sheet(fz_context *ctx)
{
	fz_stext_sheet *sheet = fz_malloc(ctx, sizeof *sheet);
	sheet->maxid = 0;
	sheet->style = NULL;
	return sheet;
}

void
fz_drop_stext_sheet(fz_context *ctx, fz_stext_sheet *sheet)
{
	fz_stext_style *style;

	if (sheet == NULL)
		return;

	style = sheet->style;
	while (style)
	{
		fz_stext_style *next = style->next;
		fz_drop_font(ctx, style->font);
		fz_free(ctx, style);
		style = next;
	}
	fz_free(ctx, sheet);
}

static fz_stext_style *
fz_lookup_stext_style_imp(fz_context *ctx, fz_stext_sheet *sheet,
	float size, fz_font *font, int wmode, int script)
{
	fz_stext_style *style;

	for (style = sheet->style; style; style = style->next)
	{
		if (style->font == font &&
			style->size == size &&
			style->wmode == wmode &&
			style->script == script) /* FIXME: others */
		{
			return style;
		}
	}

	/* Better make a new one and add it to our list */
	style = fz_malloc(ctx, sizeof *style);
	style->id = sheet->maxid++;
	style->font = fz_keep_font(ctx, font);
	style->size = size;
	style->wmode = wmode;
	style->script = script;
	style->next = sheet->style;
	sheet->style = style;
	return style;
}

static fz_stext_style *
fz_lookup_stext_style(fz_context *ctx, fz_stext_sheet *sheet, fz_text_span *span, const fz_matrix *ctm,
	fz_colorspace *colorspace, const float *color, float alpha, const fz_stroke_state *stroke)
{
	float size = 1.0f;
	fz_font *font = span ? span->font : NULL;
	int wmode = span ? span->wmode : 0;
	if (ctm && span)
	{
		fz_matrix tm = span->trm;
		fz_matrix trm;
		tm.e = 0;
		tm.f = 0;
		fz_concat(&trm, &tm, ctm);
		size = fz_matrix_expansion(&trm);
	}
	return fz_lookup_stext_style_imp(ctx, sheet, size, font, wmode, 0);
}

fz_stext_page *
fz_new_stext_page(fz_context *ctx, const fz_rect *mediabox)
{
	fz_stext_page *page = fz_malloc(ctx, sizeof(*page));
	page->mediabox = *mediabox;
	page->len = 0;
	page->cap = 0;
	page->blocks = NULL;
	page->next = NULL;
	return page;
}

static void
fz_drop_stext_line_contents(fz_context *ctx, fz_stext_line *line)
{
	fz_stext_span *span, *next;
	for (span = line->first_span; span; span=next)
	{
		next = span->next;
		fz_free(ctx, span->text);
		fz_free(ctx, span);
	}
}

static void
fz_drop_stext_block(fz_context *ctx, fz_stext_block *block)
{
	fz_stext_line *line;
	if (block == NULL)
		return;
	for (line = block->lines; line < block->lines + block->len; line++)
		fz_drop_stext_line_contents(ctx, line);
	fz_free(ctx, block->lines);
	fz_free(ctx, block);
}

static void
fz_drop_image_block(fz_context *ctx, fz_image_block *block)
{
	if (block == NULL)
		return;
	fz_drop_image(ctx, block->image);
	fz_drop_colorspace(ctx, block->cspace);
	fz_free(ctx, block);
}

void
fz_drop_stext_page(fz_context *ctx, fz_stext_page *page)
{
	fz_page_block *block;
	if (page == NULL)
		return;
	for (block = page->blocks; block < page->blocks + page->len; block++)
	{
		switch (block->type)
		{
		case FZ_PAGE_BLOCK_TEXT:
			fz_drop_stext_block(ctx, block->u.text);
			break;
		case FZ_PAGE_BLOCK_IMAGE:
			fz_drop_image_block(ctx, block->u.image);
			break;
		}
	}
	fz_free(ctx, page->blocks);
	fz_free(ctx, page);
}

static fz_stext_span *
fz_new_stext_span(fz_context *ctx, const fz_point *p, int wmode, const fz_matrix *trm)
{
	fz_stext_span *span = fz_malloc_struct(ctx, fz_stext_span);
	span->ascender_max = 0;
	span->descender_min = 0;
	span->cap = 0;
	span->len = 0;
	span->min = *p;
	span->max = *p;
	span->wmode = wmode;
	span->transform.a = trm->a;
	span->transform.b = trm->b;
	span->transform.c = trm->c;
	span->transform.d = trm->d;
	span->transform.e = 0;
	span->transform.f = 0;
	span->text = NULL;
	span->next = NULL;
	return span;
}

static void
add_char_to_span(fz_context *ctx, fz_stext_span *span, int c, fz_point *p, fz_point *max, fz_stext_style *style)
{
	if (span->len == span->cap)
	{
		int newcap = (span->cap ? span->cap * 2 : 16);
		span->text = fz_resize_array(ctx, span->text, newcap, sizeof(fz_stext_char));
		span->cap = newcap;
		span->bbox = fz_empty_rect;
	}
	span->max = *max;
	if (style->ascender > span->ascender_max)
		span->ascender_max = style->ascender;
	if (style->descender < span->descender_min)
		span->descender_min = style->descender;
	span->text[span->len].c = c;
	span->text[span->len].p = *p;
	span->text[span->len].style = style;
	span->len++;
}

static void
fz_add_stext_char_imp(fz_context *ctx, fz_stext_device *dev, fz_stext_style *style, int c, int glyph, fz_matrix *trm, float adv, int wmode)
{
	int can_append = 1;
	int add_space = 0;
	fz_point dir, ndir, p, q, r;
	float size;
	fz_point delta;
	float spacing = 0;
	float base_offset = 0;

	if (wmode == 0)
	{
		dir.x = 1;
		dir.y = 0;
	}
	else
	{
		dir.x = 0;
		dir.y = -1;
	}
	fz_transform_vector(&dir, trm);
	ndir = dir;
	fz_normalize_vector(&ndir);
	/* dir = direction vector for motion. ndir = normalised(dir) */

	size = fz_matrix_expansion(trm);

	/* We need to identify where glyphs 'start' (p) and 'stop' (q).
	 * Each glyph holds it's 'start' position, and the next glyph in the
	 * span (or span->max if there is no next glyph) holds it's 'end'
	 * position.
	 *
	 * For both horizontal and vertical motion, trm->{e,f} gives the
	 * bottom left corner of the glyph.
	 *
	 * In horizontal mode:
	 *   + p is bottom left.
	 *   + q is the bottom right
	 * In vertical mode:
	 *   + p is top left (where it advanced from)
	 *   + q is bottom left
	 */
	if (wmode == 0)
	{
		p.x = trm->e;
		p.y = trm->f;
		q.x = trm->e + adv * dir.x;
		q.y = trm->f + adv * dir.y;
	}
	else
	{
		p.x = trm->e - adv * dir.x;
		p.y = trm->f - adv * dir.y;
		q.x = trm->e;
		q.y = trm->f;
	}

	if (glyph < 0)
	{
		/* Don't reset 'pen' to start of no-glyph characters in cluster */
		if (dev->cur_span)
			q = dev->cur_span->max;
		goto no_glyph;
	}

	if (dev->cur_span == NULL ||
		trm->a != dev->cur_span->transform.a || trm->b != dev->cur_span->transform.b ||
		trm->c != dev->cur_span->transform.c || trm->d != dev->cur_span->transform.d ||
		dev->cur_span->wmode != wmode)
	{
		/* If the matrix has changed, or the wmode is different (or
		 * if we don't have a span at all), then we can't append. */
#ifdef DEBUG_SPANS
		printf("Transform/WMode changed\n");
#endif
		can_append = 0;
	}
	else
	{
		delta.x = q.x - dev->cur_span->max.x;
		delta.y = q.y - dev->cur_span->max.y;
		if (delta.x < FLT_EPSILON && delta.y < FLT_EPSILON && c == dev->lastchar)
			return;

		/* Calculate how far we've moved since the end of the current
		 * span. */
		delta.x = p.x - dev->cur_span->max.x;
		delta.y = p.y - dev->cur_span->max.y;

		/* The transform has not changed, so we know we're in the same
		 * direction. Calculate 2 distances; how far off the previous
		 * baseline we are, together with how far along the baseline
		 * we are from the expected position. */
		spacing = ndir.x * delta.x + ndir.y * delta.y;
		base_offset = -ndir.y * delta.x + ndir.x * delta.y;

		spacing /= size * SPACE_DIST;
		if (fabsf(base_offset) < size * 0.1)
		{
			/* Only a small amount off the baseline - we'll take this */
			if (fabsf(spacing) < 1.0)
			{
				/* Motion is in line, and small. */
			}
			else if (spacing >= 1 && spacing < (SPACE_MAX_DIST/SPACE_DIST))
			{
				/* Motion is in line, but large enough
				 * to warrant us adding a space */
				if (dev->lastchar != ' ' && wmode == 0)
					add_space = 1;
			}
			else
			{
				/* Motion is in line, but too large - split to a new span */
				can_append = 0;
			}
		}
		else
		{
			can_append = 0;
#ifdef DEBUG_SPANS
			spacing = 0;
#endif
		}
	}

#ifdef DEBUG_SPANS
	printf("%c%c append=%d space=%d size=%g spacing=%g base_offset=%g\n", dev->lastchar, c, can_append, add_space, size, spacing, base_offset);
#endif

	/* Start a new span */
	if (!can_append)
	{
		add_span_to_soup(ctx, dev->spans, dev->cur_span);
		dev->cur_span = NULL;
		dev->cur_span = fz_new_stext_span(ctx, &p, wmode, trm);
		dev->cur_span->spacing = 0;
	}

	/* Add synthetic space */
	if (add_space)
	{
		/* We know we always have a cur_span here */
		r = dev->cur_span->max;
		add_char_to_span(ctx, dev->cur_span, ' ', &r, &p, style);
	}

no_glyph:
	add_char_to_span(ctx, dev->cur_span, c, &p, &q, style);
	dev->lastchar = c;
}

static void
fz_add_stext_char(fz_context *ctx, fz_stext_device *dev, fz_stext_style *style, int c, int glyph, fz_matrix *trm, float adv, int wmode)
{
	/* ignore when one unicode character maps to multiple glyphs */
	if (c == -1)
		return;

	if (!(dev->flags & FZ_STEXT_PRESERVE_LIGATURES))
		switch (c)
		{
		case 0xFB00: /* ff */
			fz_add_stext_char_imp(ctx, dev, style, 'f', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'f', -1, trm, 0, wmode);
			return;
		case 0xFB01: /* fi */
			fz_add_stext_char_imp(ctx, dev, style, 'f', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'i', -1, trm, 0, wmode);
			return;
		case 0xFB02: /* fl */
			fz_add_stext_char_imp(ctx, dev, style, 'f', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'l', -1, trm, 0, wmode);
			return;
		case 0xFB03: /* ffi */
			fz_add_stext_char_imp(ctx, dev, style, 'f', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'f', -1, trm, 0, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'i', -1, trm, 0, wmode);
			return;
		case 0xFB04: /* ffl */
			fz_add_stext_char_imp(ctx, dev, style, 'f', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'f', -1, trm, 0, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 'l', -1, trm, 0, wmode);
			return;
		case 0xFB05: /* long st */
		case 0xFB06: /* st */
			fz_add_stext_char_imp(ctx, dev, style, 's', glyph, trm, adv, wmode);
			fz_add_stext_char_imp(ctx, dev, style, 't', -1, trm, 0, wmode);
			return;
		}

	if (!(dev->flags & FZ_STEXT_PRESERVE_WHITESPACE))
		switch (c)
		{
		case 0x0009: /* tab */
		case 0x0020: /* space */
		case 0x00A0: /* no-break space */
		case 0x1680: /* ogham space mark */
		case 0x180E: /* mongolian vowel separator */
		case 0x2000: /* en quad */
		case 0x2001: /* em quad */
		case 0x2002: /* en space */
		case 0x2003: /* em space */
		case 0x2004: /* three-per-em space */
		case 0x2005: /* four-per-em space */
		case 0x2006: /* six-per-em space */
		case 0x2007: /* figure space */
		case 0x2008: /* punctuation space */
		case 0x2009: /* thin space */
		case 0x200A: /* hair space */
		case 0x202F: /* narrow no-break space */
		case 0x205F: /* medium mathematical space */
		case 0x3000: /* ideographic space */
			c = ' ';
		}

	fz_add_stext_char_imp(ctx, dev, style, c, glyph, trm, adv, wmode);
}

static void
fz_stext_extract(fz_context *ctx, fz_stext_device *dev, fz_text_span *span, const fz_matrix *ctm, fz_stext_style *style)
{
	fz_font *font = span->font;
	FT_Face face = fz_font_ft_face(ctx, font);
	fz_buffer **t3procs = fz_font_t3_procs(ctx, font);
	fz_rect *bbox = fz_font_bbox(ctx, font);
	fz_matrix tm = span->trm;
	fz_matrix trm;
	float adv;
	float ascender = 1;
	float descender = 0;
	int i, err;

	if (span->len == 0)
		return;

	if (dev->spans == NULL)
		dev->spans = new_span_soup(ctx);

	if (style->wmode == 0)
	{
		if (face)
		{
			fz_lock(ctx, FZ_LOCK_FREETYPE);
			err = FT_Set_Char_Size(face, 64, 64, 72, 72);
			if (err)
				fz_warn(ctx, "freetype set character size: %s", ft_error_string(err));
			ascender = (float)face->ascender / face->units_per_EM;
			descender = (float)face->descender / face->units_per_EM;
			fz_unlock(ctx, FZ_LOCK_FREETYPE);
		}
		else if (t3procs && !fz_is_empty_rect(bbox))
		{
			ascender = bbox->y1;
			descender = bbox->y0;
		}
	}
	else
	{
		ascender = bbox->x1;
		descender = bbox->x0;
	}
	style->ascender = ascender;
	style->descender = descender;

	tm.e = 0;
	tm.f = 0;
	fz_concat(&trm, &tm, ctm);

	for (i = 0; i < span->len; i++)
	{
		/* Calculate new pen location and delta */
		tm.e = span->items[i].x;
		tm.f = span->items[i].y;
		fz_concat(&trm, &tm, ctm);

		/* Calculate bounding box and new pen position based on font metrics */
		if (span->items[i].gid >= 0)
			adv = fz_advance_glyph(ctx, font, span->items[i].gid, style->wmode);
		else
			adv = 0;

		fz_add_stext_char(ctx, dev, style, span->items[i].ucs, span->items[i].gid, &trm, adv, span->wmode);
	}
}

static void
fz_stext_fill_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm,
	fz_colorspace *colorspace, const float *color, float alpha)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_style *style;
	fz_text_span *span;
	for (span = text->head; span; span = span->next)
	{
		style = fz_lookup_stext_style(ctx, tdev->sheet, span, ctm, colorspace, color, alpha, NULL);
		fz_stext_extract(ctx, tdev, span, ctm, style);
	}
}

static void
fz_stext_stroke_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_stroke_state *stroke, const fz_matrix *ctm,
	fz_colorspace *colorspace, const float *color, float alpha)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_style *style;
	fz_text_span *span;
	for (span = text->head; span; span = span->next)
	{
		style = fz_lookup_stext_style(ctx, tdev->sheet, span, ctm, colorspace, color, alpha, stroke);
		fz_stext_extract(ctx, tdev, span, ctm, style);
	}
}

static void
fz_stext_clip_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_style *style;
	fz_text_span *span;
	for (span = text->head; span; span = span->next)
	{
		style = fz_lookup_stext_style(ctx, tdev->sheet, span, ctm, NULL, NULL, 0, NULL);
		fz_stext_extract(ctx, tdev, span, ctm, style);
	}
}

static void
fz_stext_clip_stroke_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_stroke_state *stroke, const fz_matrix *ctm, const fz_rect *scissor)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_style *style;
	fz_text_span *span;
	for (span = text->head; span; span = span->next)
	{
		style = fz_lookup_stext_style(ctx, tdev->sheet, span, ctm, NULL, NULL, 0, stroke);
		fz_stext_extract(ctx, tdev, span, ctm, style);
	}
}

static void
fz_stext_ignore_text(fz_context *ctx, fz_device *dev, const fz_text *text, const fz_matrix *ctm)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_style *style;
	fz_text_span *span;
	for (span = text->head; span; span = span->next)
	{
		style = fz_lookup_stext_style(ctx, tdev->sheet, span, ctm, NULL, NULL, 0, NULL);
		fz_stext_extract(ctx, tdev, span, ctm, style);
	}
}

static void
fz_stext_fill_image_mask(fz_context *ctx, fz_device *dev, fz_image *img, const fz_matrix *ctm,
		fz_colorspace *cspace, const float *color, float alpha)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	fz_stext_page *page = tdev->page;
	fz_image_block *block;

	/* If the alpha is less than 50% then it's probably a watermark or
	 * effect or something. Skip it */
	if (alpha < 0.5)
		return;

	/* New block */
	if (page->len == page->cap)
	{
		int newcap = (page->cap ? page->cap*2 : 4);
		page->blocks = fz_resize_array(ctx, page->blocks, newcap, sizeof(*page->blocks));
		page->cap = newcap;
	}
	block = fz_malloc_struct(ctx, fz_image_block);
	page->blocks[page->len].type = FZ_PAGE_BLOCK_IMAGE;
	page->blocks[page->len].u.image = block;
	block->image = fz_keep_image(ctx, img);
	block->cspace = fz_keep_colorspace(ctx, cspace);
	if (cspace)
		memcpy(block->colors, color, sizeof(block->colors[0])*fz_colorspace_n(ctx, cspace));
	block->mat = *ctm;
	block->bbox.x0 = 0;
	block->bbox.y0 = 0;
	block->bbox.x1 = 1;
	block->bbox.y1 = 1;
	fz_transform_rect(&block->bbox, ctm);
	page->len++;
}

static void
fz_stext_fill_image(fz_context *ctx, fz_device *dev, fz_image *img, const fz_matrix *ctm, float alpha)
{
	fz_stext_fill_image_mask(ctx, dev, img, ctm, NULL, NULL, alpha);
}

static int
direction_from_bidi_class(int bidiclass, int curdir)
{
	switch (bidiclass)
	{
	/* strong */
	case UCDN_BIDI_CLASS_L: return 1;
	case UCDN_BIDI_CLASS_R: return -1;
	case UCDN_BIDI_CLASS_AL: return -1;

	/* weak */
	case UCDN_BIDI_CLASS_EN:
	case UCDN_BIDI_CLASS_ES:
	case UCDN_BIDI_CLASS_ET:
	case UCDN_BIDI_CLASS_AN:
	case UCDN_BIDI_CLASS_CS:
	case UCDN_BIDI_CLASS_NSM:
	case UCDN_BIDI_CLASS_BN:
		return curdir;

	/* neutral */
	case UCDN_BIDI_CLASS_B:
	case UCDN_BIDI_CLASS_S:
	case UCDN_BIDI_CLASS_WS:
	case UCDN_BIDI_CLASS_ON:
		return curdir;

	/* embedding, override, pop ... we don't support them */
	default:
		return 0;
	}
}

static void
fz_bidi_reorder_run(fz_stext_span *span, int a, int b, int dir)
{
	if (a < b && dir == -1)
	{
		fz_stext_char c;
		int m = a + (b - a) / 2;
		while (a < m)
		{
			b--;
			c = span->text[a];
			span->text[a] = span->text[b];
			span->text[b] = c;
			a++;
		}
	}
}

static void
fz_bidi_reorder_span(fz_stext_span *span)
{
	int a, b, dir, curdir;

	a = 0;
	curdir = 1;
	for (b = 0; b < span->len; b++)
	{
		dir = direction_from_bidi_class(ucdn_get_bidi_class(span->text[b].c), curdir);
		if (dir != curdir)
		{
			fz_bidi_reorder_run(span, a, b, curdir);
			curdir = dir;
			a = b;
		}
	}
	fz_bidi_reorder_run(span, a, b, curdir);
}

static void
fz_bidi_reorder_stext_page(fz_context *ctx, fz_stext_page *page)
{
	fz_page_block *pageblock;
	fz_stext_block *block;
	fz_stext_line *line;
	fz_stext_span *span;

	for (pageblock = page->blocks; pageblock < page->blocks + page->len; pageblock++)
		if (pageblock->type == FZ_PAGE_BLOCK_TEXT)
			for (block = pageblock->u.text, line = block->lines; line < block->lines + block->len; line++)
				for (span = line->first_span; span; span = span->next)
					fz_bidi_reorder_span(span);
}

static void
fz_stext_close_device(fz_context *ctx, fz_device *dev)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;

	add_span_to_soup(ctx, tdev->spans, tdev->cur_span);
	tdev->cur_span = NULL;

	strain_soup(ctx, tdev);

	/* TODO: smart sorting of blocks in reading order */
	/* TODO: unicode NFC normalization */

	fz_bidi_reorder_stext_page(ctx, tdev->page);
}

static void
fz_stext_drop_device(fz_context *ctx, fz_device *dev)
{
	fz_stext_device *tdev = (fz_stext_device*)dev;
	free_span_soup(ctx, tdev->spans);
	tdev->spans = NULL;
}

fz_stext_options *
fz_parse_stext_options(fz_context *ctx, fz_stext_options *opts, const char *string)
{
	const char *val;

	memset(opts, 0, sizeof *opts);

	if (fz_has_option(ctx, string, "preserve-ligatures", &val) && fz_option_eq(val, "yes"))
		opts->flags |= FZ_STEXT_PRESERVE_LIGATURES;
	if (fz_has_option(ctx, string, "preserve-whitespace", &val) && fz_option_eq(val, "yes"))
		opts->flags |= FZ_STEXT_PRESERVE_WHITESPACE;

	return opts;
}

fz_device *
fz_new_stext_device(fz_context *ctx, fz_stext_sheet *sheet, fz_stext_page *page, const fz_stext_options *opts)
{
	fz_stext_device *dev = fz_new_derived_device(ctx, fz_stext_device);

	dev->super.hints = FZ_IGNORE_IMAGE | FZ_IGNORE_SHADE;

	dev->super.close_device = fz_stext_close_device;
	dev->super.drop_device = fz_stext_drop_device;

	dev->super.fill_text = fz_stext_fill_text;
	dev->super.stroke_text = fz_stext_stroke_text;
	dev->super.clip_text = fz_stext_clip_text;
	dev->super.clip_stroke_text = fz_stext_clip_stroke_text;
	dev->super.ignore_text = fz_stext_ignore_text;
	dev->super.fill_image = fz_stext_fill_image;
	dev->super.fill_image_mask = fz_stext_fill_image_mask;

	dev->sheet = sheet;
	dev->page = page;
	dev->spans = NULL;
	dev->cur_span = NULL;
	dev->lastchar = ' ';
	if (opts)
		dev->flags = opts->flags;

	return (fz_device*)dev;
}
