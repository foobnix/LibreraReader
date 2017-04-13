/*
 * This file is part of the TINICONV Library.
 *
 * The TINICONV Library is free software; you can redistribute it
 * and/or modify it under the terms of the Library General Public
 * License version 2 as published by the Free Software Foundation.
 *
 * The TINICONV Library is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the Library General Public
 * License along with the TINICONV Library; see the file COPYING.LIB.
 * If not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301, USA.
 */

#ifndef TINICONV_INT_H_
#define TINICONV_INT_H_

#include "tiniconv.h"

#define RET_ILSEQ      -1
#define RET_TOOFEW(n)  (-2-(n))
#define RET_ILUNI      -1
#define RET_TOOSMALL   -2

extern const struct tiniconv_charset_map_entry_s {
  xxx_mb2wc_t mb2wc;
  xxx_flushwc_t flushwc;
  xxx_wc2mb_t wc2mb;
  xxx_reset_t reset;
} tiniconv_charset_map[];

typedef struct {
  unsigned short indx; /* index into big table */
  unsigned short used; /* bitmask of used entries */
} Summary16;

#define TINICONV_OPTION_GET_OUT_ILSEQ_CHAR(options) ((options >> 8) & 0xFF)

#endif /*TINICONV_INT_H_*/
