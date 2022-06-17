/* minidjvu - library for handling bilevel images with DjVuBitonal support
 *
 * patterns.c - pattern matching algorithm
 *
 * Copyright (C) 2005  Ilya Mezhirov
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * 
 * minidjvu is derived from DjVuLibre (http://djvu.sourceforge.net)
 * All over DjVuLibre there is a patent alert from LizardTech
 * which I guess I should reproduce (don't ask me what does this mean):
 * 
 *  ------------------------------------------------------------------
 * | DjVu (r) Reference Library (v. 3.5)
 * | Copyright (c) 1999-2001 LizardTech, Inc. All Rights Reserved.
 * | The DjVu Reference Library is protected by U.S. Pat. No.
 * | 6,058,214 and patents pending.
 * |
 * | This software is subject to, and may be distributed under, the
 * | GNU General Public License, either Version 2 of the license,
 * | or (at your option) any later version. The license should have
 * | accompanied the software or you may obtain a copy of the license
 * | from the Free Software Foundation at http://www.fsf.org .
 * |
 * | The computer code originally released by LizardTech under this
 * | license and unmodified by other parties is deemed "the LIZARDTECH
 * | ORIGINAL CODE."  Subject to any third party intellectual property
 * | claims, LizardTech grants recipient a worldwide, royalty-free, 
 * | non-exclusive license to make, use, sell, or otherwise dispose of 
 * | the LIZARDTECH ORIGINAL CODE or of programs derived from the 
 * | LIZARDTECH ORIGINAL CODE in compliance with the terms of the GNU 
 * | General Public License.   This grant only confers the right to 
 * | infringe patent claims underlying the LIZARDTECH ORIGINAL CODE to 
 * | the extent such infringement is reasonably necessary to enable 
 * | recipient to make, have made, practice, sell, or otherwise dispose 
 * | of the LIZARDTECH ORIGINAL CODE (or portions thereof) and not to 
 * | any greater extent that may be necessary to utilize further 
 * | modifications or combinations.
 * |
 * | The LIZARDTECH ORIGINAL CODE is provided "AS IS" WITHOUT WARRANTY
 * | OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * | TO ANY WARRANTY OF NON-INFRINGEMENT, OR ANY IMPLIED WARRANTY OF
 * | MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * +------------------------------------------------------------------
 */

/* This is `patterns.c', the unit that handles pattern matching.
 * Its task is only to compare pairs of images, not to classify a set of them.
 * And this has absolutely nothing to do with choosing a cross-coding prototype.
 */

#include "mdjvucfg.h"
#include "minidjvu.h"
#include <stdlib.h>
#include <string.h>
#include <assert.h>


/* Stuff for not using malloc in C++
 * (made by Leon Bottou; has no use in minidjvu,
 * but left here for potential DjVuLibre compatibility)
 */
#ifdef __cplusplus
# define MALLOC(Type)    new Type
# define FREE(p)         delete p
# define MALLOCV(Type,n) new Type[n]
# define FREEV(p)        delete [] p
#else
# define MALLOC(Type)    ((Type*)malloc(sizeof(Type)))
# define FREE(p)         do{if(p)free(p);}while(0)
# define MALLOCV(Type,n) ((Type*)malloc(sizeof(Type)*(n)))
# define FREEV(p)        do{if(p)free(p);}while(0)
#endif


#define SIGNATURE_SIZE 32

/* Mass center coordinates are stored in (1/MASS_CENTER_QUANT) pixels.
 * This leads to more precise alignment then using whole pixels.
 */
#define MASS_CENTER_QUANT 8


/* These are hand-tweaked parameters of this classifier. */

typedef struct
{
    double pithdiff_threshold;
    double softdiff_threshold;
    double shiftdiff1_threshold;
    double shiftdiff2_threshold;
    double shiftdiff3_threshold;
} Options;

static const double pithdiff_veto_threshold        = 23;
static const double softdiff_veto_threshold        = 100; /* that means off */
static const double shiftdiff1_veto_threshold      = 1000;
static const double shiftdiff2_veto_threshold      = 1500;
static const double shiftdiff3_veto_threshold      = 2000;

static const double size_difference_threshold = 10;
static const double mass_difference_threshold = 15;

static const double shiftdiff1_falloff        = .9;
static const double shiftdiff2_falloff        = 1;
static const double shiftdiff3_falloff        = 1.15;

static void interpolate(Options *opt, const double *v1, const double *v2,
                        int l, int r, int x)
{
    double w1 = ((double)(r - x)) / (r - l); /* weights */
    double w2 = 1 - w1;
    opt->pithdiff_threshold   = v1[0] * w1 + v2[0] * w2;
    opt->softdiff_threshold   = v1[1] * w1 + v2[1] * w2;
    opt->shiftdiff1_threshold = v1[2] * w1 + v2[2] * w2;
    opt->shiftdiff2_threshold = v1[3] * w1 + v2[3] * w2;
    opt->shiftdiff3_threshold = v1[4] * w1 + v2[4] * w2;
}


/* Sets `aggression' for pattern matching.
 * Lower values are safer, bigger values produce smaller files.
 */

MDJVU_IMPLEMENT void mdjvu_set_aggression(mdjvu_matcher_options_t opt, int level)
{
    const double set200[5] = {7, 15, 60,  80, 170};
    const double set150[5] = {5, 13, 50,  70, 160};
    const double   set0[5] = {0,  0,  0,   0,   0};

    if (level < 0) level = 0;

    if (level > 150)
        interpolate((Options *) opt, set150, set200, 150, 200, level);
    else
        interpolate((Options *) opt, set0, set150, 0, 150, level);
}

/* ========================================================================== */

MDJVU_IMPLEMENT mdjvu_matcher_options_t mdjvu_matcher_options_create(void)
{
    mdjvu_matcher_options_t options = (mdjvu_matcher_options_t) MALLOC(Options);
    mdjvu_set_aggression(options, 100);
    return options;
}

MDJVU_IMPLEMENT void mdjvu_matcher_options_destroy(mdjvu_matcher_options_t opt)
{
    FREE((Options *) opt);
}


/* FIXME: maxint is maxint32 */
static const int32 maxint = ~(1 << (sizeof(int32) * 8 - 1));
typedef unsigned char byte;

typedef struct ComparableImageData
{
    byte **pixels; /* 0 - purely white, 255 - purely black (inverse to PGM!) */
    int32 width, height, mass;
    int32 mass_center_x, mass_center_y;
    byte signature[SIGNATURE_SIZE];  /* for shiftdiff 1 and 3 tests */
    byte signature2[SIGNATURE_SIZE]; /* for shiftdiff 2 test */
} Image;



/* Each image pair undergoes simple tests (dimensions and mass)
 * and at most five more advanced tests.
 * Each test may end up with three outcomes: veto (-1), doubt (0) and match(1).
 * Images are equivalent if and only if
 *     there was no `veto'
 *     and there was at least one `match'.
 */


/* We check whether images' dimensions are different
 *     no more than by size_difference_threshold percent.
 * Return value is usual: veto (-1) or doubt (0).
 * Mass checking was introduced by Leon Bottou.
 */

static int simple_tests(Image *i1, Image *i2)
{
    int32 w1 = i1->width, h1 = i1->height, m1 = i1->mass;
    int32 w2 = i2->width, h2 = i2->height, m2 = i2->mass;

    if (100.* w1 > (100.+ size_difference_threshold) * w2) return -1;
    if (100.* w2 > (100.+ size_difference_threshold) * w1) return -1;
    if (100.* h1 > (100.+ size_difference_threshold) * h2) return -1;
    if (100.* h2 > (100.+ size_difference_threshold) * h1) return -1;
    if (100.* m1 > (100.+ mass_difference_threshold) * m2) return -1;
    if (100.* m2 > (100.+ mass_difference_threshold) * m1) return -1;

    return 0;
}


#define USE_PITHDIFF 1
#define USE_SOFTDIFF 1
#define USE_SHIFTDIFF_1 1
#define USE_SHIFTDIFF_2 1
#define USE_SHIFTDIFF_3 1


/* Computing distance by comparing pixels {{{ */

#if USE_PITHDIFF || USE_SOFTDIFF

/* This function compares two images pixel by pixel.
 * The exact way to compare pixels is defined by two functions,
 *     compare_row and compare_with_white.
 * Both functions take pointers to byte rows and their length.
 *
 * Now images are aligned by mass centers.
 * Code needs some clarification, yes...
 */
static int32 distance_by_pixeldiff_functions_by_shift(Image *i1, Image *i2,
    int32 (*compare_row)(byte *, byte *, int32),
    int32 (*compare_with_white)(byte *, int32), int32 ceiling,
    int32 shift_x, int32 shift_y) /* of i1's coordinate system with respect to i2 */
{
    int32 w1 = i1->width, w2 = i2->width, h1 = i1->height, h2 = i2->height;
    int32 min_y = shift_y < 0 ? shift_y : 0;
    int32 right1 = shift_x + w1;
    int32 max_y_plus_1 = h2 > shift_y + h1 ? h2 : shift_y + h1;
    int32 i;
    int32 min_overlap_x = shift_x > 0 ? shift_x : 0;
    int32 max_overlap_x_plus_1 = w2 < right1 ? w2 : right1;
    int32 min_overlap_x_for_i1 = min_overlap_x - shift_x;
    int32 max_overlap_x_plus_1_for_i1 = max_overlap_x_plus_1 - shift_x;
    int32 overlap_length = max_overlap_x_plus_1 - min_overlap_x;
    int32 score = 0;

    if (overlap_length <= 0) return maxint;

    for (i = min_y; i < max_y_plus_1; i++)
    {
        int32 y1 = i - shift_y;

        /* calculate difference in the i-th line */

        if (i < 0 || i >= h2)
        {
            /* calculate difference of i1 with white */
            score += compare_with_white(i1->pixels[y1], w1);
        }
        else if (i < shift_y || i >= shift_y + h1)
        {
            /* calculate difference of i2 with white */
            score += compare_with_white(i2->pixels[i], w2);
        }
        else
        {
            /* calculate difference in a line where the bitmaps overlap */
            score += compare_row(i1->pixels[y1] + min_overlap_x_for_i1,
                                 i2->pixels[i] + min_overlap_x,
                                 overlap_length);


            /* calculate penalty for the left margin */
            if (min_overlap_x > 0)
                score += compare_with_white(i2->pixels[i], min_overlap_x);
            else
                score += compare_with_white(i1->pixels[y1], min_overlap_x_for_i1);

            /* calculate penalty for the right margin */
            if (max_overlap_x_plus_1 < w2)
            {
                score += compare_with_white(
                    i2->pixels[i] + max_overlap_x_plus_1,
                    w2 - max_overlap_x_plus_1);
            }
            else
            {
                score += compare_with_white(
                     i1->pixels[y1] + max_overlap_x_plus_1_for_i1,
                     w1 - max_overlap_x_plus_1_for_i1);

            }
        }

        if (score >= ceiling) return maxint;
    }
    return score;
}

static int32 distance_by_pixeldiff_functions(Image *i1, Image *i2,
    int32 (*compare_row)(byte *, byte *, int32),
    int32 (*compare_with_white)(byte *, int32), int32 ceiling)
{
    int32 w1, w2, h1, h2;
    int32 shift_x, shift_y; /* of i1's coordinate system with respect to i2 */

    /* make i1 to be narrower than i2 */
    if (i1->width > i2->width)
    {
        Image *img = i1;
        i1 = i2;
        i2 = img;
    }

    w1 = i1->width; h1 = i1->height;
    w2 = i2->width; h2 = i2->height; 

    /* (shift_x, shift_y) */
    /*     is what should be added to i1's coordinates to get i2's coordinates. */
    shift_x = (w2 - w2/2) - (w1 - w1/2); /* center favors right */
    shift_y = h2/2 - h1/2;               /* center favors top */

    shift_x = i2->mass_center_x - i1->mass_center_x;
    if (shift_x < 0)
        shift_x = (shift_x - MASS_CENTER_QUANT / 2) / MASS_CENTER_QUANT;
    else
        shift_x = (shift_x + MASS_CENTER_QUANT / 2) / MASS_CENTER_QUANT;

    shift_y = i2->mass_center_y - i1->mass_center_y;
    if (shift_y < 0)
        shift_y = (shift_y - MASS_CENTER_QUANT / 2) / MASS_CENTER_QUANT;
    else
        shift_y = (shift_y + MASS_CENTER_QUANT / 2) / MASS_CENTER_QUANT;

    return distance_by_pixeldiff_functions_by_shift(
        i1, i2, compare_row, compare_with_white, ceiling, shift_x, shift_y);
}

#endif

/* Computing distance by comparing pixels }}} */
/* inscribed framework penalty counting {{{ */

/* (Look at `frames.c' to see what it's all about) */

#if USE_PITHDIFF

/* If the framework of one letter is inscribed into another and vice versa,
 *     then those letters are probably equivalent.
 * That's the idea...
 * Counting penalty points here for any pixel
 *     that's framework in one image and white in the other.
 */

static int32 pithdiff_compare_row(byte *row1, byte *row2, int32 n)
{
    int32 i, s = 0;
    for (i = 0; i < n; i++)
    {
        int32 k = row1[i], l = row2[i];
        if (k == 255)
            s += 255 - l;
        else if (l == 255)
            s += 255 - k;
    }
    return s;
}

static int32 pithdiff_compare_with_white(byte *row, int32 n)
{
    int32 i, s = 0;
    for (i = 0; i < n; i++) if (row[i] == 255) s += 255;
    return s;
}

static int32 pithdiff_distance(Image *i1, Image *i2, int32 ceiling)
{
    return distance_by_pixeldiff_functions(i1, i2,
            &pithdiff_compare_row, &pithdiff_compare_with_white, ceiling);
}

static int pithdiff_equivalence(Image *i1, Image *i2, double threshold, int32 dpi)
{
    int32 perimeter = i1->width + i1->height + i2->width + i2->height;
    double ceiling = pithdiff_veto_threshold * dpi * perimeter / 100;
    int32 d = pithdiff_distance(i1, i2, (int32) ceiling);
    if (d == maxint) return -1;
    if (d < threshold * dpi * perimeter / 100) return 1;
    return 0;
}

#endif /* if USE_PITHDIFF */

/* inscribed framework penalty counting }}} */
/* soft penalty counting {{{ */

#if USE_SOFTDIFF

/* This test scores penalty points for pixels that are different in both images.
 * Since every black pixel has a rating of importance,
 *     the penalty for a pair of corresponding pixels, one black, one white,
 *         is equal to the rating of the black pixel.
 */

static int32 softdiff_compare_row(byte *row1, byte *row2, int32 n)
{
    int32 i, s = 0;
    for (i = 0; i < n; i++)
    {
        if (!row1[i])
            s += row2[i];
        else if (!row2[i])
            s += row1[i];
    }
    return s;
}

static int32 softdiff_compare_with_white(byte *row, int32 n)
{
    int32 i, s = 0;
    for (i = 0; i < n; i++) s += row[i];
    return s;
}

static int32 softdiff_distance(Image *i1, Image *i2, int32 ceiling)
{
    return distance_by_pixeldiff_functions(i1, i2,
            &softdiff_compare_row, &softdiff_compare_with_white, ceiling);
}

static int softdiff_equivalence(Image *i1, Image *i2, double threshold, int32 dpi)
{
    int32 perimeter = i1->width + i1->height + i2->width + i2->height;
    double ceiling = softdiff_veto_threshold * dpi * perimeter / 100;
    int32 d = softdiff_distance(i1, i2, (int32) ceiling);
    if (d == maxint) return -1;
    if (d < threshold * dpi * perimeter / 100) return 1;
    return 0;
}

#endif /* if USE_SOFTDIFF */

/* soft penalty counting }}} */
/* shift signature comparison {{{ */

/* Just finding the square of a normal Euclidean distance between vectors
 * (but with falloff)
 */

#if USE_SHIFTDIFF_1 || USE_SHIFTDIFF_2 || USE_SHIFTDIFF_3
static int shiftdiff_equivalence(byte *s1, byte *s2, double falloff, double veto, double threshold)
{
    int i, delay_before_falloff = 1, delay_counter = 1;
    double penalty = 0;
    double weight = 1;

    for (i = 1; i < SIGNATURE_SIZE; i++) /* kluge: ignores the first byte */
    {
        int difference = s1[i] - s2[i];
        penalty += difference * difference * weight;
        if (!--delay_counter)
        {
            weight *= falloff;
            delay_counter = delay_before_falloff <<= 1;
        }
    }

    if (penalty >= veto * SIGNATURE_SIZE) return -1;
    if (penalty <= threshold * SIGNATURE_SIZE) return 1;
    return 0;
}
#endif
/* shift signature comparison }}} */

#ifndef NO_MINIDJVU
mdjvu_pattern_t mdjvu_pattern_create(mdjvu_bitmap_t bitmap)
{
    int32 w = mdjvu_bitmap_get_width(bitmap);
    int32 h = mdjvu_bitmap_get_height(bitmap);
    mdjvu_pattern_t pattern;
    byte **pixels = mdjvu_create_2d_array(w, h);
    mdjvu_bitmap_unpack_all(bitmap, pixels);
    pattern = mdjvu_pattern_create_from_array(pixels, w, h);
    mdjvu_destroy_2d_array(pixels);
    return pattern;
}
#endif

/* Finding mass center {{{ */

static void get_mass_center(unsigned char **pixels, int32 w, int32 h,
                     int32 *pmass_center_x, int32 *pmass_center_y)
{
    double x_sum = 0, y_sum = 0, mass = 0;
    int32 i, j;

    for (i = 0; i < h; i++)
    {
        unsigned char *row = pixels[i];
        for (j = 0; j < w; j++)
        {
            unsigned char pixel = row[j];
            x_sum += pixel * j;
            y_sum += pixel * i;
            mass  += pixel;
        }
    }

    *pmass_center_x = (int32) (x_sum * MASS_CENTER_QUANT / mass);
    *pmass_center_y = (int32) (y_sum * MASS_CENTER_QUANT / mass);
}

/* Finding mass center }}} */


MDJVU_IMPLEMENT mdjvu_pattern_t mdjvu_pattern_create_from_array(byte **pixels, int32 w, int32 h)/*{{{*/
{
    int32 i, mass;
    Image *img = MALLOC(Image);
    byte *pool = MALLOCV(byte, w * h);
    memset(pool, 0, w * h);

    img->width = w;
    img->height = h;

    img->pixels = MALLOCV(byte *, h);
    for (i = 0; i < h; i++)
        img->pixels[i] = pool + i * w;

    mass = 0;
    for (i = 0; i < h; i++)
    {
        int32 j;
        for (j = 0; j < w; j++)
            if (pixels[i][j])
            {
                img->pixels[i][j] = 255; /* i don't remember what for */
                mass += 1;
            }
    }
    img->mass = mass;

    mdjvu_soften_pattern(img->pixels, img->pixels, w, h);
    get_mass_center(img->pixels, w, h,
                    &img->mass_center_x, &img->mass_center_y);
    mdjvu_get_gray_signature(img->pixels, w, h,
                             img->signature, SIGNATURE_SIZE);
    mdjvu_get_black_and_white_signature(img->pixels, w, h,
                                        img->signature2, SIGNATURE_SIZE);
    return (mdjvu_pattern_t) img;
}/*}}}*/

/* Requires `opt' to be non-NULL */
static int compare_patterns(mdjvu_pattern_t ptr1, mdjvu_pattern_t ptr2,/*{{{*/
                            int32 dpi, Options *opt)

{
    Image *i1 = (Image *) ptr1, *i2 = (Image *) ptr2;
    int i, state = 0; /* 0 - unsure, 1 - equal unless veto */

    if (simple_tests(i1, i2)) return -1;

    #if USE_SHIFTDIFF_1
        i = shiftdiff_equivalence(i1->signature, i2->signature,
            shiftdiff1_falloff, shiftdiff1_veto_threshold, opt->shiftdiff1_threshold);
        if (i == -1) return -1;
        state |= i;
    #endif

    #if USE_SHIFTDIFF_2
        i = shiftdiff_equivalence(i1->signature2, i2->signature2,
            shiftdiff2_falloff, shiftdiff2_veto_threshold, opt->shiftdiff2_threshold);
        if (i == -1) return -1;
        state |= i;
    #endif

    #if USE_SHIFTDIFF_3
        i = shiftdiff_equivalence(i1->signature, i2->signature,
            shiftdiff3_falloff, shiftdiff3_veto_threshold, opt->shiftdiff3_threshold);
        if (i == -1) return -1;
        state |= i;
    #endif

    #if USE_PITHDIFF
        i = pithdiff_equivalence(i1, i2, opt->pithdiff_threshold, dpi);
        if (i == -1) return 0; /* pithdiff has no right to veto at upper level */
        state |= i;
    #endif

    #if USE_SOFTDIFF
        i = softdiff_equivalence(i1, i2, opt->softdiff_threshold, dpi);
        if (i == -1) return 0;  /* softdiff has no right to veto at upper level */
        state |= i;
    #endif

    return state;
}/*}}}*/

MDJVU_IMPLEMENT int mdjvu_match_patterns(mdjvu_pattern_t ptr1, mdjvu_pattern_t ptr2,
                    int32 dpi, mdjvu_matcher_options_t options)
{
    Options *opt;
    int result;
    if (options)
        opt = (Options *) options;
    else
        opt = (Options *) mdjvu_matcher_options_create();

    result = compare_patterns(ptr1, ptr2, dpi, opt);

    if (!options)
        mdjvu_matcher_options_destroy((mdjvu_matcher_options_t) opt);

    return result;
}


MDJVU_IMPLEMENT void mdjvu_pattern_destroy(mdjvu_pattern_t p)/*{{{*/
{
    Image *img = (Image *) p;
    FREEV(img->pixels[0]);
    FREEV(img->pixels);
    FREE(img);
}/*}}}*/
