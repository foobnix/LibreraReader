//
// Created by dev on 03.06.22.
//

#ifndef LIBRERAREADER_LOAD_WEBP_H
#define LIBRERAREADER_LOAD_WEBP_H

fz_pixmap *fz_load_webp(fz_context *ctx, unsigned char *data, size_t size);
void fz_load_webp_info(fz_context *ctx, unsigned char *data, size_t size, int *w, int *h, int *xres, int *yres, fz_colorspace **cspace);


#endif //LIBRERAREADER_LOAD_WEBP_H
