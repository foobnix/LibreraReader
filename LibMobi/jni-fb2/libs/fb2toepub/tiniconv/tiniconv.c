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

#include "tiniconv.h"
#include "tiniconv_int.h"

#include <string.h>
#include <assert.h>

int tiniconv_init(int in_charset_id, int out_charset_id, int options, struct tiniconv_ctx_s *ctx)
{
  assert(ctx != NULL);
  
  if (in_charset_id < 0 || in_charset_id >= TINICONV_CHARSETSIZE)
    return TINICONV_INIT_IN_CHARSET_NA;
  if (out_charset_id < 0 || out_charset_id >= TINICONV_CHARSETSIZE)
    return TINICONV_INIT_OUT_CHARSET_NA;
  
  memset(ctx, 0, sizeof(*ctx));
  ctx->mb2wc = tiniconv_charset_map[in_charset_id].mb2wc;
  ctx->flushwc = tiniconv_charset_map[in_charset_id].flushwc;
  ctx->wc2mb = tiniconv_charset_map[out_charset_id].wc2mb;
  ctx->reset = tiniconv_charset_map[out_charset_id].reset;
  ctx->options = options;
  if (!TINICONV_OPTION_GET_OUT_ILSEQ_CHAR(options))
    ctx->options = ctx->options | TINICONV_OPTION_OUT_ILSEQ_CHAR('?');
  
  return TINICONV_INIT_OK;
}

int tiniconv_convert(struct tiniconv_ctx_s *ctx,
  unsigned char const *in_buf, int in_size, int *p_in_size_consumed,
  unsigned char *out_buf, int out_size, int *p_out_size_consumed)
{
  ucs4_t wc;
  int in_idx, out_idx;
  int result, last_result;
  state_t last_istate;
  
  assert(ctx != NULL);
  assert(in_buf != NULL);
  assert(out_buf != NULL);
  
  for (in_idx = 0, out_idx = 0; in_idx < in_size && out_idx < out_size;)
  {
  	last_istate = ctx->istate;
    /* typedef int (*xxx_mb2wc_t) (conv_t conv, ucs4_t *pwc, unsigned char const *s, int n); */
    result = ctx->mb2wc(ctx, &wc, in_buf + in_idx, in_size - in_idx);
    assert(result <= in_size - in_idx);
    if (result < 0)
    {
      if (result == RET_ILSEQ)
      {
        if (ctx->options & TINICONV_OPTION_IGNORE_IN_ILSEQ)
        {
          ctx->istate = 0;
          in_idx ++;
          continue;
        }
        else
        {
          result = TINICONV_CONVERT_IN_ILSEQ;
          goto exit;
        }
      }
      else if (result == RET_TOOSMALL)
      {
        result = TINICONV_CONVERT_IN_TOO_SMALL;
        goto exit;
      }
      else
      {
      	in_idx += RET_TOOFEW(result);
      	continue;
      }
    }
    in_idx += last_result = result;
    
    /* typedef int (*xxx_wc2mb_t) (conv_t conv, unsigned char *r, ucs4_t wc, int n); */
    result = ctx->wc2mb(ctx, out_buf + out_idx, wc, out_size - out_idx);
    assert(result <= out_size - out_idx);
    if (result < 0)
    {
      if (result == RET_ILUNI)
      {
        if (ctx->options & TINICONV_OPTION_IGNORE_OUT_ILSEQ)
        {
          out_buf[out_idx ++] = TINICONV_OPTION_GET_OUT_ILSEQ_CHAR(ctx->options);
          ctx->ostate = 0;
          continue;
        }
        else
        {
          result = TINICONV_CONVERT_OUT_ILSEQ;
          in_idx -= last_result; /* discarding the last read sequence */
          ctx->istate = last_istate;
          goto exit;
        }
      }
      else if (result == RET_TOOSMALL)
      {
        result = TINICONV_CONVERT_OUT_TOO_SMALL;
        in_idx -= last_result; /* discarding the last read sequence */
        ctx->istate = last_istate;
        goto exit;
      }
    }
    out_idx += result;
  }
  result = TINICONV_CONVERT_OK;

exit:
  if (p_in_size_consumed)
    *p_in_size_consumed = in_idx;
  if (p_out_size_consumed)
    *p_out_size_consumed = out_idx;
  return result;
}
