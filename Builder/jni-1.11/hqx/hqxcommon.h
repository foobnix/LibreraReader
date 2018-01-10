/*
 * Copyright (C) 2003 Maxim Stepin ( maxst@hiend3d.com )
 *
 * Copyright (C) 2010 Cameron Zemek ( grom@zeminvaders.net)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

#ifndef __HQX_COMMON_H_
#define __HQX_COMMON_H_

#include <stdlib.h>
#include <stdint.h>

#define MASK_2 0x00FF00
#define MASK_13 0xFF00FF

#define Ymask 0x00FF0000
#define Umask 0x0000FF00
#define Vmask 0x000000FF
#define trY   0x00300000
#define trU   0x00000700
#define trV   0x00000006

static inline uint32_t rgb2yuv(uint32_t c)
{
	int r, g, b, y, u, v;

    r = (c & 0xFF0000) >> 16;
    g = (c & 0x00FF00) >> 8;
    b = c & 0x0000FF;
    y = (uint32_t)(0.299*r + 0.587*g + 0.114*b);
    u = (uint32_t)(-0.169*r - 0.331*g + 0.5*b) + 128;
    v = (uint32_t)(0.5*r - 0.419*g - 0.081*b) + 128;

    return (y << 16) + (u << 8) + v;
}

/* Test if there is difference in color */
static inline int Diff(uint32_t w1, uint32_t w2)
{
    // Mask against RGB_MASK to discard the alpha channel
    uint32_t YUV1 = rgb2yuv(w1);
    uint32_t YUV2 = rgb2yuv(w2);
    return ( ( abs((YUV1 & Ymask) - (YUV2 & Ymask)) > trY ) ||
            ( abs((YUV1 & Umask) - (YUV2 & Umask)) > trU ) ||
            ( abs((YUV1 & Vmask) - (YUV2 & Vmask)) > trV ) );
}

/* Interpolate functions */

static inline void Interp1(uint32_t * pc, uint32_t c1, uint32_t c2)
{
    //*pc = (c1*3+c2) >> 2;
    if (c1 == c2) {
        *pc = c1;
        return;
    }
    *pc = ((((c1 & MASK_2) * 3 + (c2 & MASK_2)) >> 2) & MASK_2) +
        ((((c1 & MASK_13) * 3 + (c2 & MASK_13)) >> 2) & MASK_13);
}

static inline void Interp2(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*2+c2+c3) >> 2;
    *pc = ((((c1 & MASK_2) * 2 + (c2 & MASK_2) + (c3 & MASK_2)) >> 2) & MASK_2) +
          ((((c1 & MASK_13) * 2 + (c2 & MASK_13) + (c3 & MASK_13)) >> 2) & MASK_13);
}

static inline void Interp3(uint32_t * pc, uint32_t c1, uint32_t c2)
{
    //*pc = (c1*7+c2)/8;
    if (c1 == c2) {
        *pc = c1;
        return;
    }
    *pc = ((((c1 & MASK_2) * 7 + (c2 & MASK_2)) >> 3) & MASK_2) +
        ((((c1 & MASK_13) * 7 + (c2 & MASK_13)) >> 3) & MASK_13);
}

static inline void Interp4(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*2+(c2+c3)*7)/16;
    *pc = ((((c1 & MASK_2) * 2 + (c2 & MASK_2) * 7 + (c3 & MASK_2) * 7) >> 4) & MASK_2) +
          ((((c1 & MASK_13) * 2 + (c2 & MASK_13) * 7 + (c3 & MASK_13) * 7) >> 4) & MASK_13);
}

static inline void Interp5(uint32_t * pc, uint32_t c1, uint32_t c2)
{
    //*pc = (c1+c2) >> 1;
    if (c1 == c2) {
        *pc = c1;
        return;
    }
    *pc = ((((c1 & MASK_2) + (c2 & MASK_2)) >> 1) & MASK_2) +
        ((((c1 & MASK_13) + (c2 & MASK_13)) >> 1) & MASK_13);
}

static inline void Interp6(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*5+c2*2+c3)/8;
    *pc = ((((c1 & MASK_2) * 5 + (c2 & MASK_2) * 2 + (c3 & MASK_2)) >> 3) & MASK_2) +
          ((((c1 & MASK_13) * 5 + (c2 & MASK_13) * 2 + (c3 & MASK_13)) >> 3) & MASK_13);
}

static inline void Interp7(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*6+c2+c3)/8;
    *pc = ((((c1 & MASK_2) * 6 + (c2 & MASK_2) + (c3 & MASK_2)) >> 3) & MASK_2) +
          ((((c1 & MASK_13) * 6 + (c2 & MASK_13) + (c3 & MASK_13)) >> 3) & MASK_13);
}

static inline void Interp8(uint32_t * pc, uint32_t c1, uint32_t c2)
{
    //*pc = (c1*5+c2*3)/8;
    if (c1 == c2) {
        *pc = c1;
        return;
    }
    *pc = ((((c1 & MASK_2) * 5 + (c2 & MASK_2) * 3) >> 3) & MASK_2) +
          ((((c1 & MASK_13) * 5 + (c2 & MASK_13) * 3) >> 3) & MASK_13);
}

static inline void Interp9(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*2+(c2+c3)*3)/8;
    *pc = ((((c1 & MASK_2) * 2 + (c2 & MASK_2) * 3 + (c3 & MASK_2) * 3) >> 3) & MASK_2) +
          ((((c1 & MASK_13) * 2 + (c2 & MASK_13) * 3 + (c3 & MASK_13) * 3) >> 3) & MASK_13);
}

static inline void Interp10(uint32_t * pc, uint32_t c1, uint32_t c2, uint32_t c3)
{
    //*pc = (c1*14+c2+c3)/16;
    *pc = ((((c1 & MASK_2) * 14 + (c2 & MASK_2) + (c3 & MASK_2)) >> 4) & MASK_2) +
          ((((c1 & MASK_13) * 14 + (c2 & MASK_13) + (c3 & MASK_13)) >> 4) & MASK_13);
}

#endif
