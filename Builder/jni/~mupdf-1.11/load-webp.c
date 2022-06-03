#include "mupdf/fitz.h"

#include <webp/decode.h>
#include <webp/demux.h>
#include <webp/types.h>

fz_pixmap *
fz_load_webp(fz_context *ctx, const unsigned char *p, size_t total)
{

    struct WebPBitstreamFeatures features;
    WebPData webp_data;
    WebPDemuxer* demux = NULL;

    if (WebPGetFeatures(p, total, &features) != VP8_STATUS_OK)
    fz_throw(ctx, FZ_ERROR_GENERIC, "unable to extract webp features");

    int width = features.width;
    int height = features.height;

    webp_data.bytes = p;
    webp_data.size = total;

    demux = WebPDemux(&webp_data);

    uint8_t* rgba = features.has_alpha ? WebPDecodeRGBA(p, total, NULL, NULL) : WebPDecodeRGB(p, total, NULL, NULL);

    if (rgba == NULL)
        fz_throw(ctx, FZ_ERROR_GENERIC, "failed decoding webp image");

    fz_pixmap *image;
    image = fz_new_pixmap(ctx, fz_device_rgb(ctx), width, height, features.has_alpha ? 1:0);

    fz_clear_pixmap(ctx, image);
    fz_unpack_tile(ctx, image, rgba, image->n, 8, image->stride, 1);

    WebPFree(rgba);


return image;
}

void
fz_load_webp_info(fz_context *ctx, const unsigned char *p, size_t total, int *wp, int *hp, int *xresp, int *yresp, fz_colorspace **cspacep)
{
    struct WebPBitstreamFeatures features;

    if (WebPGetFeatures(p, total, &features) != VP8_STATUS_OK)
        fz_throw(ctx, FZ_ERROR_GENERIC, "unable to extract webp features");

    int width = features.width;
    int height = features.height;

    *wp = width;
    *hp = height;
    *xresp = 72;
    *yresp = 72;
}

