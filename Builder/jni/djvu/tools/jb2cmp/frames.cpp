/* minidjvu - library for handling bilevel images with DjVuBitonal support
 *
 * frames.c - extracting frameworks and calculating "importance rating"
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

/* Frameworks are funny things...
 * The algorithm is to be commented yet.
 * Here's a picture illustrating what is a frame
 * (view with monospace font):
 *
 *  Original letter:        Its framework:
 *
  .....@@@@@@@@........ .....................
  ...@@@@@@@@@@@@...... ......@@@@@..........
  ..@@@@@@@@@@@@@@..... .....@@...@@@........
  ..@@@@@...@@@@@@@.... ....@@......@@.......
  ..@@@@.....@@@@@@.... ....@........@.......
  .@@@@@.....@@@@@@.... ....@........@.......
  .@@@@@.....@@@@@@.... ....@........@.......
  ..@@@@.....@@@@@@.... ....@........@.......
  ..........@@@@@@@.... .............@.......
  .......@@@@@@@@@@.... .............@.......
  .....@@@@@@@@@@@@.... ........@@@@@@.......
  ...@@@@@@@@@@@@@@.... ......@@@....@.......
  ..@@@@@@@..@@@@@@.... .....@@......@.......
  .@@@@@@....@@@@@@.... ...@@@.......@.......
  .@@@@@.....@@@@@@.... ...@.........@.......
  @@@@@......@@@@@@.... ..@@.........@.......
  @@@@@......@@@@@@.... ..@..........@.......
  @@@@@.....@@@@@@@.... ..@..........@.......
  @@@@@@....@@@@@@@.@@@ ..@@.........@.......
  .@@@@@@@@@@@@@@@@@@@@ ...@@.....@@@@@......
  .@@@@@@@@@@@@@@@@@@@@ ....@@..@@@...@@@@@..
  ..@@@@@@@@@.@@@@@@@@. .....@@@@............
  ....@@@@@....@@@@@... .....................

 * A letter is converted into grayshades,
 *   and a frame is the set of its purely black pixels after the transformation.
 * In the grayshade version of a letter,
 *   all pixels that were white remain absolutely white,
 *   the frame is black and the blackness falls down from it to the border.
 */

/* Offtopic: I wonder if this thing could help OCRing because frameworks
 * perfectly retain readability while becoming essentially 1-dimensional.
 */

#include "mdjvucfg.h"
#include "minidjvu.h"
#include <stdlib.h>
#include <assert.h>
#include <string.h>
#include <math.h>

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


/* This determines the gray level of the border (ratio of black).
 * Setting it to 1 will effectively eliminate grayshading.
 */
#define BORDER_FALLOFF .7 /* this is the main constant in all the matcher... */

typedef unsigned char byte;

static int donut_connectivity_test(byte *upper, byte *row, byte *lower)/*{{{*/
{
    /*(on the pictures below 0 is white, 1 is black or gray)
     *
     * 01.
     * 1 . -> 1
     * ...
     *
     * .0.
     * 1 1 ->  1
     * .0.
     *
     * all others -> 0
     */

    int sum, l, u, d, r;

    sum = (u = *upper ? 1 : 0) + (d = *lower ? 1 : 0) +
          (l = row[-1] ? 1 : 0) + (r = row[1] ? 1 : 0);

    switch(sum)
    {
        case 3:/*{{{*/
        {
            int x = 6 - (u + (l << 1) + d + (d << 1));
            switch(x)
            {
                case 0: /* l */
                    return upper[-1] && lower[-1] ? 0 : 1;
                case 1: /* d */
                    return lower[-1] && lower[1] ? 0 : 1;
                case 2: /* r */
                    return upper[1] && lower[1] ? 0 : 1;
                case 3: /* u */
                    return upper[-1] && upper[1] ? 0 : 1;
                default: assert(0); return 0;
            }
        }
        break;/*}}}*/
        case 2:/*{{{*/
        {
            int s = l + r;
            if (s & 1)
            {
                /*   A1.
                 *   1 0 - should be !A (2x2 square extermination)
                 *   .0.
                 */
                if (l)
                {
                    if (u)
                        return upper[-1] ? 0 : 1;
                    else
                        return lower[-1] ? 0 : 1;
                }
                else /* r */
                {
                    if (u)
                        return upper[1] ? 0 : 1;
                    else
                        return lower[1] ? 0 : 1;
                }
            }
            else
            {
                /*   .0.
                 *   1 1 - surely should be 1 to preserve connection
                 *   .0.
                 */
                return 1;
            }
        }
        break;/*}}}*/
        case 0: case 4:
            return 1;
        case 1:
            return 0;
        default: assert(0); return 0;
    }
    assert(0); return 0;
}/*}}}*/
static byte donut_transform_pixel(byte *upper, byte *row, byte *lower)/*{{{*/
{
    /* (center pixel should be gray in order for this to work)
     * (on the pictures below 0 is white, 1 is black or gray)
     *
     * 01.
     * 1 . -> center will become 1
     * ...
     *
     * .0.
     * 1 1 -> center will become 1
     * .0.
     *
     * 00.
     * 1 0 -> center will become 1
     * .0.
     *
     * 1..
     * 1 0 -> center will become 0
     * 1..
     *
     * 11.
     * 1 0 -> center will become 0
     * .0.
     *
     * .A.
     * A A -> center will become 1
     * .A.
     */

    int sum, l, u, d, r;
    if (!*row) return 0;

    sum = (u = *upper ? 1 : 0) + (d = *lower ? 1 : 0) +
          (l = row[-1] ? 1 : 0) + (r = row[1] ? 1 : 0);

    switch(sum)
    {
        case 1: case 3:/*{{{*/
        {
            int x = u + (l << 1) + d + (d << 1);
            if (sum == 3) x = (6 - x) ^ 2;
            switch(x)
            {
                case 0: /* r */
                    return upper[1] && lower[1] ? 0 : 1;
                case 1: /* u */
                    return upper[-1] && upper[1] ? 0 : 1;
                case 2: /* l */
                    return upper[-1] && lower[-1] ? 0 : 1;
                case 3: /* d */
                    return lower[-1] && lower[1] ? 0 : 1;
                default: assert(0); return 0;
            }
        }
        break;/*}}}*/
        case 2:/*{{{*/
        {
            int s = l + r;
            if (s & 1)
            {
                /*   A1.
                 *   1 0 - should be !A (2x2 square extermination)
                 *   .0.
                 */
                if (l)
                {
                    if (u)
                        return upper[-1] ? 0 : 1;
                    else
                        return lower[-1] ? 0 : 1;
                }
                else /* r */
                {
                    if (u)
                        return upper[1] ? 0 : 1;
                    else
                        return lower[1] ? 0 : 1;
                }
            }
            else
            {
                /*   .0.
                 *   1 1 - surely should be 1 to preserve connection
                 *   .0.
                 */
                return 1;
            }
        }
        break;/*}}}*/
        case 0: case 4:
            return 1; /* lone pixels are NOT omitted */
        default: assert(0); return 0;
    }
    assert(0); return 0;
}/*}}}*/

/* `pixels' should have a margin of 1 pixel at each side
 * returns true if the image was changed
 */
static int flay(byte **pixels, int w, int h, int rank, int **ranks)
{
    int i, j, result = 0;

    byte *buf = MALLOCV(byte, w * h);

    assert(pixels);
    assert(w);
    assert(h);

    for (i = 0; i < h; i++) for (j = 0; j < w; j++)
    {
        buf[w * i + j] =
            donut_transform_pixel(pixels[i-1] + j, pixels[i] + j, pixels[i+1] + j);
    }

    for (i = 0; i < h; i++)
    {
        byte *up = pixels[i-1], *row = pixels[i], *dn = pixels[i+1];
        byte *buf_row = buf + w * i;
        int *rank_row = NULL;
        if (ranks) rank_row = ranks[i];
        for (j = 0; j < w; j++)
        {
            if (row[j] && !buf_row[j])
            {
                if (!donut_connectivity_test(up + j, row + j, dn + j))
                {
                    row[j] = buf_row[j];
                    if (rank) rank_row[j] = rank;
                    result = 1;
                }
            }
            else
                row[j] = buf_row[j];
        }
    }

    FREEV(buf);
    return result;
}

/* TODO: use less temporary buffers and silly copyings */
MDJVU_IMPLEMENT void mdjvu_soften_pattern(byte **result, byte **pixels, int32 w, int32 h)/*{{{*/
{
    byte *r = MALLOCV(byte, (w + 2) * (h + 2));
    byte **pointers = MALLOCV(byte *, h + 2);
    int *ranks_buf = MALLOCV(int, w * h);
    int **ranks = MALLOCV(int *, h);

    int i, j, passes = 1;
    double level = 1, falloff;
    byte *colors;

    memset(r, 0, (w + 2) * (h + 2));
    memset(ranks_buf, 0, w * h * sizeof(int));

    for (i = 0; i < h + 2; i++)
        pointers[i] = r + (w + 2) * i + 1;

    for (i = 0; i < h; i++)
        memcpy(pointers[i+1], pixels[i], w);

    for (i = 0; i < h; i++)
        ranks[i] = ranks_buf + w * i;

    while(flay(pointers + 1, w, h, passes, ranks)) passes++;

    colors = MALLOCV(byte, passes + 1);

    falloff = pow(BORDER_FALLOFF, 1./passes);

    for (i = 0; i < passes; i++)
    {
        colors[i] = (byte) (level * 255);
        level *= falloff;
    }
    /* TODO: test the next line */
    /* colors[passes - 1] = 50; pay less attention to border pixels */
    colors[passes] = 0;

    pointers++;
    for (i = 0; i < h; i++)
    {
        for (j = 0; j < w; j++)
        {
            if (pointers[i][j])
            {
                result[i][j] = 255;
            }
            else
            {
                result[i][j] = colors[passes - ranks[i][j]];
            }
        }
    }
    pointers--;

    FREEV(colors);
    FREEV(ranks);
    FREEV(ranks_buf);
    FREEV(r);
    FREEV(pointers);
}/*}}}*/
