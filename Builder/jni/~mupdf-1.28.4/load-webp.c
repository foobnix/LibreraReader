// Copyright (C) 2004-2022 Artifex Software, Inc.
//
// This file is part of MuPDF.
//
// MuPDF is free software: you can redistribute it and/or modify it under the
// terms of the GNU Affero General Public License as published by the Free
// Software Foundation, either version 3 of the License, or (at your option)
// any later version.
//
// MuPDF is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
// details.
//
// You should have received a copy of the GNU Affero General Public License
// along with MuPDF. If not, see <https://www.gnu.org/licenses/agpl-3.0.en.html>
//
// Alternative licensing terms are available from the licensor.
// For commercial licensing, see <https://www.artifex.com/> or contact
// Artifex Software, Inc., 1305 Grant Avenue - Suite 200, Novato,
// CA 94945, U.S.A., +1(415)492-9861, for further information.

#include "mupdf/fitz.h"

#include "image-imp.h"
#include "pixmap-imp.h"

#include <math.h>
#include <string.h>
#include <limits.h>

#include <webp/decode.h>
#include <webp/demux.h>
#include <webp/types.h>

struct info
{
	int width, height;
	int xres, yres;
	uint8_t orientation;
	int pages;
	fz_colorspace *cs;
};

/* Returns true if <x> can be represented as an integer without overflow.
 *
 * We can't use comparisons such as 'return x < INT_MAX' because INT_MAX is
 * not safely convertible to float - it ends up as INT_MAX+1 so the comparison
 * doesn't do what we want.
 *
 * Instead we do a round-trip conversion and return true if this differs by
 * less than 1. This relies on high adjacent float values that differ by more
 * than 1, actually being exact integers, so the round-trip doesn't change the
 * value.
 */
static int float_can_be_int(float x)
{
	return fabsf(x - (float)(int) x) < 1;
}

static uint8_t exif_orientation_to_mupdf[9] = { 0, 1, 5, 3, 7, 6, 4, 8, 2 };

static inline int read_value(const unsigned char *data, int bytes, int is_big_endian)
{
	int value = 0;
	if (!is_big_endian)
		data += bytes;
	for (; bytes > 0; bytes--)
		value = (value << 8) | (is_big_endian ? *data++ : *--data);
	return value;
}

static int extract_exif_resolution(WebPData* chunk,
	int *xres, int *yres, uint8_t *orientation)
{
	int is_big_endian, orient;
	const unsigned char *data;
	unsigned int offset, ifd_len, res_type = 0;
	float x_res = 0, y_res = 0;

	if (!chunk || chunk->size < 14)
		return 0;
	data = (const unsigned char *)chunk->bytes;
	if (read_value(data, 4, 1) != 0x45786966 /* Exif */ || read_value(data + 4, 2, 1) != 0x0000)
		return 0;
	if (read_value(data + 6, 4, 1) == 0x49492A00)
		is_big_endian = 0;
	else if (read_value(data + 6, 4, 1) == 0x4D4D002A)
		is_big_endian = 1;
	else
		return 0;

	offset = read_value(data + 10, 4, is_big_endian) + 6;
	if (offset < 14 || offset > chunk->size - 2)
		return 0;
	ifd_len = read_value(data + offset, 2, is_big_endian);
	for (offset += 2; ifd_len > 0 && offset + 12 < chunk->size; ifd_len--, offset += 12)
	{
		int tag = read_value(data + offset, 2, is_big_endian);
		int type = read_value(data + offset + 2, 2, is_big_endian);
		int count = read_value(data + offset + 4, 4, is_big_endian);
		unsigned int value_off = read_value(data + offset + 8, 4, is_big_endian) + 6;
		switch (tag)
		{
		case 0x112:
			if (type == 3 && count == 1) {
				orient = read_value(data + offset + 8, 2, is_big_endian);
				if (orient >= 1 && orient <= 8 && orientation)
					*orientation = exif_orientation_to_mupdf[orient];
			}
			break;
		case 0x11A:
			if (type == 5 && value_off > offset && value_off <= chunk->size - 8)
				x_res = 1.0f * read_value(data + value_off, 4, is_big_endian) / read_value(data + value_off + 4, 4, is_big_endian);
			break;
		case 0x11B:
			if (type == 5 && value_off > offset && value_off <= chunk->size - 8)
				y_res = 1.0f * read_value(data + value_off, 4, is_big_endian) / read_value(data + value_off + 4, 4, is_big_endian);
			break;
		case 0x128:
			if (type == 3 && count == 1)
				res_type = read_value(data + offset + 8, 2, is_big_endian);
			break;
		}
	}

	if (x_res <= 0 || !float_can_be_int(x_res) || y_res <= 0 || !float_can_be_int(y_res))
		return 0;
	if (res_type == 2)
	{
		*xres = (int)x_res;
		*yres = (int)y_res;
	}
	else if (res_type == 3)
	{
		*xres = (int)(x_res * 254 / 100);
		*yres = (int)(y_res * 254 / 100);
	}
	else
	{
		*xres = 0;
		*yres = 0;
	}
	return 1;
}

static fz_colorspace *extract_icc_profile(fz_context *ctx, WebPData* chunk, fz_colorspace *colorspace)
{
#if FZ_ENABLE_ICC
	fz_buffer *buf = NULL;

	fz_var(buf);

	if (!chunk || !chunk->bytes || chunk->size == 0)
		return colorspace;

	fz_try(ctx)
	{
		buf = fz_new_buffer_from_copied_data(ctx, chunk->bytes, chunk->size);
		if (buf)
		{
			fz_colorspace *icc = fz_new_icc_colorspace(ctx, FZ_COLORSPACE_NONE, 0, NULL, buf);
			fz_drop_colorspace(ctx, colorspace);
			colorspace = icc;
		}
	}
	fz_always(ctx)
		fz_drop_buffer(ctx, buf);
	fz_catch(ctx)
		fz_warn(ctx, "ignoring embedded ICC profile in JPEG");

	return colorspace;
#else
	return colorspace;
#endif
}

static fz_pixmap *
webp_read_image(fz_context *ctx, struct info *info, const unsigned char *p, size_t total, int only_metadata)
{
	fz_pixmap *image = NULL;

	fz_try(ctx)
	{
		struct WebPBitstreamFeatures features;
		WebPData webp_data;
		WebPDemuxer* demux = NULL;

		if (WebPGetFeatures(p, total, &features) != VP8_STATUS_OK)
			fz_throw(ctx, FZ_ERROR_GENERIC, "unable to extract webp features");

		info->width = features.width;
		info->height = features.height;
		info->xres = 72;
		info->yres = 72;
		info->cs = fz_keep_colorspace(ctx, fz_device_rgb(ctx));

		webp_data.bytes = p;
		webp_data.size = total;
		demux = WebPDemux(&webp_data);

		if (demux != NULL)
		{
			if (WebPDemuxGetI(demux, WEBP_FF_FORMAT_FLAGS) & EXIF_FLAG)
			{
				WebPChunkIterator chunk_iter;
				if (WebPDemuxGetChunk(demux, "EXIF", 1, &chunk_iter))
				{
					extract_exif_resolution(&chunk_iter.chunk, &info->xres, &info->yres, &info->orientation);
				}
				WebPDemuxReleaseChunkIterator(&chunk_iter);
			}
			if (WebPDemuxGetI(demux, WEBP_FF_FORMAT_FLAGS) & ICCP_FLAG)
			{
				WebPChunkIterator chunk_iter;
				if (WebPDemuxGetChunk(demux, "ICCP", 1, &chunk_iter))
				{
					info->cs = extract_icc_profile(ctx, &chunk_iter.chunk, info->cs);
				}
				WebPDemuxReleaseChunkIterator(&chunk_iter);
			}

			WebPDemuxDelete(demux);
		}

		if (!only_metadata)
		{
			uint8_t* rgba = features.has_alpha ? WebPDecodeRGBA(p, total, NULL, NULL) : WebPDecodeRGB(p, total, NULL, NULL);

			if (rgba == NULL)
				fz_throw(ctx, FZ_ERROR_GENERIC, "failed decoding webp image");

			image = fz_new_pixmap(ctx, info->cs, info->width, info->height, NULL, features.has_alpha);
			image->xres = info->xres;
			image->yres = info->yres;

			fz_clear_pixmap(ctx, image);
			fz_unpack_tile(ctx, image, rgba, image->n, 8, image->stride, 1);

			WebPFree(rgba);
		}
	}
	fz_catch(ctx)
		fz_rethrow(ctx);

	return image;
}

fz_pixmap *
fz_load_webp(fz_context *ctx, const unsigned char *p, size_t total)
{
	fz_pixmap *image;
	struct info info;

	fz_try(ctx)
	{
		image = webp_read_image(ctx, &info, p, total, 0);
	}
	fz_always(ctx)
		fz_drop_colorspace(ctx, info.cs);
	fz_catch(ctx)
		fz_rethrow(ctx);

	return image;
}

void
fz_load_webp_info(fz_context *ctx, const unsigned char *p, size_t total, int *wp, int *hp, int *xresp, int *yresp, fz_colorspace **cspacep)
{
	struct info info;

	fz_try(ctx)
		webp_read_image(ctx, &info, p, total, 1);
	fz_catch(ctx)
	{
		fz_drop_colorspace(ctx, info.cs);
		fz_rethrow(ctx);
	}

	*cspacep = info.cs;
	*wp = info.width;
	*hp = info.height;
	*xresp = info.xres;
	*yresp = info.xres;
}