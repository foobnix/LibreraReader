/* minidjvu - library for handling bilevel images with DjVuBitonal support
 *
 * patterns.h - matching patterns
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

#ifndef MDJVU_PATTERNS_H
#define MDJVU_PATTERNS_H


/* To get an image ready for comparisons, one have to `prepare' it.
 * A prepared image is called a `pattern' here.
 */

/* the struct itself is not defined in this header */
typedef struct MinidjvuPattern *mdjvu_pattern_t;


/* Allocate a pattern and calculate all necessary information.
 * Memory consumption is byte per pixel + constant.
 * The pattern would be completely independent on the bitmap given.
 *     (that is, you can destroy the bitmap immediately)
 */
#ifndef NO_MINIDJVU
MDJVU_FUNCTION mdjvu_pattern_t mdjvu_pattern_create(mdjvu_bitmap_t);
#endif

/* Same, but create from two-dimensional array.
 */

MDJVU_FUNCTION mdjvu_pattern_t mdjvu_pattern_create_from_array
    (unsigned char **, int32 w, int32 h);


/* Destroy the pattern. */

MDJVU_FUNCTION void mdjvu_pattern_destroy(mdjvu_pattern_t);


typedef struct MinidjvuMatcherOptions *mdjvu_matcher_options_t;

MDJVU_FUNCTION mdjvu_matcher_options_t mdjvu_matcher_options_create(void);
MDJVU_FUNCTION void mdjvu_set_aggression(mdjvu_matcher_options_t, int level);
MDJVU_FUNCTION void mdjvu_matcher_options_destroy(mdjvu_matcher_options_t);


/* Compare patterns.
 * Returns
 * +1 if images are considered equivalent,
 * -1 if they are considered totally different (just to speed up things),
 *  0 if unknown, but probably different.
 * Exchanging the order of arguments should not change the outcome.
 * If you have found that A ~ B and B ~ C,
 *     then you may assume A ~ C regardless of this function's result.
 *
 * Options may be NULL.
 */

MDJVU_FUNCTION int mdjvu_match_patterns(mdjvu_pattern_t, mdjvu_pattern_t,
                                        int32 dpi,
                                        mdjvu_matcher_options_t);


/* Auxiliary functions used in pattern matcher (TODO: comment them) */

/* `result' and `pixels' may be the same array */
MDJVU_FUNCTION void mdjvu_soften_pattern(unsigned char **result,
    unsigned char **pixels, int32 w, int32 h);

MDJVU_FUNCTION void mdjvu_get_gray_signature(
    unsigned char **data, int32 w, int32 h,
    unsigned char *result, int32 size);

MDJVU_FUNCTION void mdjvu_get_black_and_white_signature(
    unsigned char **data, int32 w, int32 h,
    unsigned char *result, int32 size);

#endif /* MDJVU_PATTERNS_H */
