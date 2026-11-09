/*
 * TIFF images read with libtiff, for com.foobnix.ext.NativeTiff: the pages of a comic archive
 * that are TIFF - a scanned comic, often black and white fax-coded - which Android's own image
 * decoder cannot read, and a .tif or .tiff book, every image of it a page. Built into libtiff.so
 * (libtiff-4.7.2/Android.mk), in every flavour; MuPDF does not use it.
 *
 * A comic's page comes as the bytes of the archive entry and is read from memory; a book is read
 * from its file, which libtiff maps, and stays open while it is read. An image is drawn into a
 * bitmap the Java side makes, shrunk by a whole factor as it goes: the image is read through
 * libtiff's RGBA interface one band of rows at a time - whole strips or rows of tiles, so none is
 * decoded twice - and each pixel of the bitmap is the average of the square of pixels it stands
 * for, so a scan is never held whole at full size and the thin lines of a black and white page
 * grey out rather than vanish.
 *
 * libtiff's RGBA pixels are R, G, B, A bytes with the alpha premultiplied, which is how an
 * ARGB_8888 bitmap holds them, so they go in as they are.
 */

#include <jni.h>
#include <stdarg.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <android/bitmap.h>
#include <android/log.h>

#include "tiffio.h"

#define TAG "NativeTiff"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

/* The most one allocation of libtiff's may take, and all of them together: a damaged or hostile
   file asks for whatever its header says, and is refused past these */
#define MAX_SINGLE_ALLOC (256 * 1024 * 1024)
#define MAX_TOTAL_ALLOC (512 * 1024 * 1024)

/* The most a band of rows may take, as RGBA, before it is cut down to fewer rows */
#define MAX_BAND_BYTES (64 * 1024 * 1024)
/* The fewest rows a band of short strips is made up to, so a strip of one row is not a call */
#define MIN_BAND_ROWS 64

typedef struct
{
    unsigned char *data;
    toff_t size;
    toff_t pos;
} memory_file;

static tmsize_t
mf_read(thandle_t h, void *buf, tmsize_t n)
{
    memory_file *mf = (memory_file *)h;
    if (n <= 0 || mf->pos >= mf->size)
        return 0;
    if ((toff_t)n > mf->size - mf->pos)
        n = (tmsize_t)(mf->size - mf->pos);
    memcpy(buf, mf->data + mf->pos, (size_t)n);
    mf->pos += (toff_t)n;
    return n;
}

static tmsize_t
mf_write(thandle_t h, void *buf, tmsize_t n)
{
    return 0;
}

static toff_t
mf_seek(thandle_t h, toff_t off, int whence)
{
    memory_file *mf = (memory_file *)h;
    int64_t pos;
    /* relative to the place or the end, the offset may be back, a negative number */
    switch (whence)
    {
        case SEEK_SET:
            pos = (int64_t)off;
            break;
        case SEEK_CUR:
            pos = (int64_t)mf->pos + (int64_t)off;
            break;
        case SEEK_END:
            pos = (int64_t)mf->size + (int64_t)off;
            break;
        default:
            return (toff_t)-1;
    }
    if (pos < 0)
        return (toff_t)-1;
    mf->pos = (toff_t)pos;
    return mf->pos;
}

static int
mf_close(thandle_t h)
{
    return 0;
}

static toff_t
mf_size(thandle_t h)
{
    return ((memory_file *)h)->size;
}

/* The file is in memory already: libtiff reads its strips straight out of it */
static int
mf_map(thandle_t h, void **base, toff_t *size)
{
    memory_file *mf = (memory_file *)h;
    *base = mf->data;
    *size = mf->size;
    return 1;
}

static void
mf_unmap(thandle_t h, void *base, toff_t size)
{
}

static int
on_error(TIFF *tif, void *user_data, const char *module, const char *fmt, va_list ap)
{
    __android_log_vprint(ANDROID_LOG_ERROR, TAG, fmt, ap);
    /* handled: libtiff's own handler, which writes to stderr, is not called */
    return 1;
}

static int
on_warning(TIFF *tif, void *user_data, const char *module, const char *fmt, va_list ap)
{
    /* unknown tags and the like, of which scans are full */
    return 1;
}

static TIFFOpenOptions *
new_options(void)
{
    TIFFOpenOptions *opts = TIFFOpenOptionsAlloc();
    if (!opts)
        return NULL;
    TIFFOpenOptionsSetMaxSingleMemAlloc(opts, MAX_SINGLE_ALLOC);
    TIFFOpenOptionsSetMaxCumulatedMemAlloc(opts, MAX_TOTAL_ALLOC);
    TIFFOpenOptionsSetErrorHandlerExtR(opts, on_error, NULL);
    TIFFOpenOptionsSetWarningHandlerExtR(opts, on_warning, NULL);
    return opts;
}

/* Copies the Java bytes and opens them; NULL when they are not a TIFF libtiff reads */
static TIFF *
open_tiff(JNIEnv *env, jbyteArray bytes, memory_file *mf)
{
    TIFFOpenOptions *opts;
    TIFF *tif;
    jsize n = (*env)->GetArrayLength(env, bytes);

    mf->data = malloc(n > 0 ? (size_t)n : 1);
    mf->size = (toff_t)n;
    mf->pos = 0;
    if (!mf->data)
        return NULL;
    (*env)->GetByteArrayRegion(env, bytes, 0, n, (jbyte *)mf->data);

    opts = new_options();
    if (!opts)
        return NULL;
    tif = TIFFClientOpenExt("page", "r", (thandle_t)mf, mf_read, mf_write, mf_seek, mf_close,
                            mf_size, mf_map, mf_unmap, opts);
    TIFFOpenOptionsFree(opts);
    return tif;
}

static void
close_tiff(TIFF *tif, memory_file *mf)
{
    if (tif)
        TIFFClose(tif);
    free(mf->data);
    mf->data = NULL;
}

/* The width and height of the current image, or null */
static jintArray
image_size(JNIEnv *env, TIFF *tif)
{
    uint32_t w = 0, h = 0;
    jintArray result = NULL;

    if (tif && TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &w) && TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &h)
        && w > 0 && h > 0 && w <= INT32_MAX && h <= INT32_MAX)
    {
        jint size[2] = {(jint)w, (jint)h};
        result = (*env)->NewIntArray(env, 2);
        if (result)
            (*env)->SetIntArrayRegion(env, result, 0, 2, size);
    }
    return result;
}

/* The width and height of the first image, or null */
JNIEXPORT jintArray JNICALL
Java_com_foobnix_ext_NativeTiff_nativeSize(JNIEnv *env, jclass clazz, jbyteArray bytes)
{
    memory_file mf = {0};
    TIFF *tif = open_tiff(env, bytes, &mf);
    jintArray result = image_size(env, tif);
    close_tiff(tif, &mf);
    return result;
}

/* The rows of the band each strip or row of tiles is read in */
static uint32_t
band_rows(TIFF *tif, uint32_t w, uint32_t h)
{
    uint32_t piece = 0;
    uint32_t band;
    uint32_t most = MAX_BAND_BYTES / 4 / (w > 0 ? w : 1);

    if (TIFFIsTiled(tif))
        TIFFGetField(tif, TIFFTAG_TILELENGTH, &piece);
    else
        TIFFGetFieldDefaulted(tif, TIFFTAG_ROWSPERSTRIP, &piece);
    if (piece == 0 || piece > h)
        piece = h;

    /* Whole pieces, enough of them to make a band worth a call */
    band = piece;
    if (band < MIN_BAND_ROWS)
        band = (MIN_BAND_ROWS + piece - 1) / piece * piece;
    if (band > most)
        /* too wide for whole pieces: a piece is read again for each band it is cut into */
        band = most >= piece ? most / piece * piece : (most > 0 ? most : 1);
    if (band > h)
        band = h;
    return band;
}

/*
 * Draws the current image into the bitmap, shrunk by sample each way: the bitmap is
 * ceil(width / sample) by ceil(height / sample), ARGB_8888. False when it cannot be read at
 * all; an image damaged part way is drawn as far as it can be, white beyond.
 */
static jboolean
decode_into(JNIEnv *env, TIFF *tif, jint sample, jobject bitmap)
{
    TIFFRGBAImage img;
    int img_ok = 0;
    char emsg[1024] = "";
    AndroidBitmapInfo info;
    unsigned char *pixels = NULL;
    uint32_t *raster = NULL;
    uint32_t *sums = NULL;
    uint32_t w, h, out_w, out_h, s, band, y0;
    jboolean ok = JNI_FALSE;

    s = sample > 1 ? (uint32_t)sample : 1;

    if (!tif)
        goto done;
    if (!TIFFRGBAImageOK(tif, emsg) || !TIFFRGBAImageBegin(&img, tif, 0, emsg))
    {
        LOGE("cannot read: %s", emsg);
        goto done;
    }
    img_ok = 1;
    /* rows from the top down, as a bitmap holds them, whatever corner the file starts in */
    img.req_orientation = ORIENTATION_TOPLEFT;

    w = img.width;
    h = img.height;
    out_w = (w + s - 1) / s;
    out_h = (h + s - 1) / s;

    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS
        || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 || info.width != out_w || info.height != out_h)
    {
        LOGE("bitmap is not %ux%u RGBA_8888", out_w, out_h);
        goto done;
    }

    band = band_rows(tif, w, h);
    raster = malloc((size_t)w * band * 4);
    sums = s > 1 ? calloc((size_t)out_w * 4, sizeof(uint32_t)) : NULL;
    if (!raster || (s > 1 && !sums))
    {
        LOGE("no memory for %u rows of %u", band, w);
        goto done;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, (void **)&pixels) != ANDROID_BITMAP_RESULT_SUCCESS)
        goto done;

    for (y0 = 0; y0 < h; y0 += band)
    {
        uint32_t rows = h - y0 < band ? h - y0 : band;
        uint32_t r;

        /* white where a damaged piece leaves rows undrawn */
        memset(raster, 0xFF, (size_t)w * rows * 4);
        img.row_offset = (int)y0;
        img.col_offset = 0;
        TIFFRGBAImageGet(&img, raster, w, rows);

        for (r = 0; r < rows; r++)
        {
            const unsigned char *src = (const unsigned char *)(raster + (size_t)r * w);
            uint32_t y = y0 + r;
            uint32_t x;

            if (s == 1)
            {
                memcpy(pixels + (size_t)y * info.stride, src, (size_t)w * 4);
                continue;
            }

            for (x = 0; x < w; x++)
            {
                uint32_t *sum = sums + (x / s) * 4;
                sum[0] += src[x * 4];
                sum[1] += src[x * 4 + 1];
                sum[2] += src[x * 4 + 2];
                sum[3] += src[x * 4 + 3];
            }

            if ((y + 1) % s == 0 || y == h - 1)
            {
                uint32_t oy = y / s;
                uint32_t n_rows = y - oy * s + 1;
                unsigned char *dst = pixels + (size_t)oy * info.stride;
                uint32_t ox;
                for (ox = 0; ox < out_w; ox++)
                {
                    uint32_t cols = w - ox * s < s ? w - ox * s : s;
                    uint32_t n = cols * n_rows;
                    uint32_t *sum = sums + ox * 4;
                    dst[ox * 4] = (unsigned char)(sum[0] / n);
                    dst[ox * 4 + 1] = (unsigned char)(sum[1] / n);
                    dst[ox * 4 + 2] = (unsigned char)(sum[2] / n);
                    dst[ox * 4 + 3] = (unsigned char)(sum[3] / n);
                    sum[0] = sum[1] = sum[2] = sum[3] = 0;
                }
            }
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    ok = JNI_TRUE;

done:
    free(raster);
    free(sums);
    if (img_ok)
        TIFFRGBAImageEnd(&img);
    return ok;
}

/* Draws the first image of the bytes into the bitmap - see decode_into */
JNIEXPORT jboolean JNICALL
Java_com_foobnix_ext_NativeTiff_nativeDecode(JNIEnv *env, jclass clazz, jbyteArray bytes, jint sample,
                                            jobject bitmap)
{
    memory_file mf = {0};
    TIFF *tif = open_tiff(env, bytes, &mf);
    jboolean ok = decode_into(env, tif, sample, bitmap);
    close_tiff(tif, &mf);
    return ok;
}

/* ------------------------------------------------------------------ a book of TIFF pages */

/*
 * A .tif or .tiff opened as a book: its pages are its images, but for the reduced copies a file
 * may carry of them - a thumbnail is not a page. One handle is not thread-safe; the Java side
 * synchronizes on it.
 */
typedef struct
{
    TIFF *tif;
    int count;
    tdir_t *pages; /* the directory each page is */
} tiff_book;

static void
book_close(tiff_book *book)
{
    if (!book)
        return;
    if (book->tif)
        TIFFClose(book->tif);
    free(book->pages);
    free(book);
}

/* Opens the file and finds its pages; 0 when it is not a TIFF libtiff reads, or has no page */
JNIEXPORT jlong JNICALL
Java_com_foobnix_ext_NativeTiff_nativeOpenFile(JNIEnv *env, jclass clazz, jbyteArray jpath)
{
    jsize len = (*env)->GetArrayLength(env, jpath);
    char *path = malloc((size_t)len + 1);
    TIFFOpenOptions *opts = new_options();
    tiff_book *book = calloc(1, sizeof(tiff_book));
    int capacity = 0;

    if (!path || !opts || !book)
        goto fail;
    (*env)->GetByteArrayRegion(env, jpath, 0, len, (jbyte *)path);
    path[len] = 0;

    book->tif = TIFFOpenExt(path, "r", opts);
    if (!book->tif)
        goto fail;

    do
    {
        uint32_t kind = 0;
        TIFFGetField(book->tif, TIFFTAG_SUBFILETYPE, &kind);
        if (kind & FILETYPE_REDUCEDIMAGE)
            continue;
        if (book->count == capacity)
        {
            int more = capacity ? capacity * 2 : 16;
            tdir_t *grown = realloc(book->pages, (size_t)more * sizeof(tdir_t));
            if (!grown)
                goto fail;
            book->pages = grown;
            capacity = more;
        }
        book->pages[book->count++] = TIFFCurrentDirectory(book->tif);
    } while (TIFFReadDirectory(book->tif));

    if (book->count == 0 || !TIFFSetDirectory(book->tif, book->pages[0]))
        goto fail;

    free(path);
    TIFFOpenOptionsFree(opts);
    return (jlong)(intptr_t)book;

fail:
    free(path);
    if (opts)
        TIFFOpenOptionsFree(opts);
    book_close(book);
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_foobnix_ext_NativeTiff_nativePageCount(JNIEnv *env, jclass clazz, jlong handle)
{
    tiff_book *book = (tiff_book *)(intptr_t)handle;
    return book ? book->count : 0;
}

/* Makes a page the current image; false when it cannot be read */
static int
book_page(tiff_book *book, jint page)
{
    if (!book || page < 0 || page >= book->count)
        return 0;
    return TIFFCurrentDirectory(book->tif) == book->pages[page] || TIFFSetDirectory(book->tif, book->pages[page]);
}

JNIEXPORT jintArray JNICALL
Java_com_foobnix_ext_NativeTiff_nativePageSize(JNIEnv *env, jclass clazz, jlong handle, jint page)
{
    tiff_book *book = (tiff_book *)(intptr_t)handle;
    return book_page(book, page) ? image_size(env, book->tif) : NULL;
}

JNIEXPORT jboolean JNICALL
Java_com_foobnix_ext_NativeTiff_nativeDecodePage(JNIEnv *env, jclass clazz, jlong handle, jint page, jint sample,
                                                jobject bitmap)
{
    tiff_book *book = (tiff_book *)(intptr_t)handle;
    return book_page(book, page) ? decode_into(env, book->tif, sample, bitmap) : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_foobnix_ext_NativeTiff_nativeClose(JNIEnv *env, jclass clazz, jlong handle)
{
    book_close((tiff_book *)(intptr_t)handle);
}
