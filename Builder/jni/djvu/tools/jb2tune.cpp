//C-  -*- C++ -*-
//C- -------------------------------------------------------------------
//C- DjVuLibre-3.5
//C- Copyright (c) 2002  Leon Bottou and Yann Le Cun.
//C- Copyright (c) 2001  AT&T
//C-
//C- This software is subject to, and may be distributed under, the
//C- GNU General Public License, either Version 2 of the license,
//C- or (at your option) any later version. The license should have
//C- accompanied the software or you may obtain a copy of the license
//C- from the Free Software Foundation at http://www.fsf.org .
//C-
//C- This program is distributed in the hope that it will be useful,
//C- but WITHOUT ANY WARRANTY; without even the implied warranty of
//C- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//C- GNU General Public License for more details.
//C- 
//C- DjVuLibre-3.5 is derived from the DjVu(r) Reference Library from
//C- Lizardtech Software.  Lizardtech Software has authorized us to
//C- replace the original DjVu(r) Reference Library notice by the following
//C- text (see doc/lizard2002.djvu and doc/lizardtech2007.djvu):
//C-
//C-  ------------------------------------------------------------------
//C- | DjVu (r) Reference Library (v. 3.5)
//C- | Copyright (c) 1999-2001 LizardTech, Inc. All Rights Reserved.
//C- | The DjVu Reference Library is protected by U.S. Pat. No.
//C- | 6,058,214 and patents pending.
//C- |
//C- | This software is subject to, and may be distributed under, the
//C- | GNU General Public License, either Version 2 of the license,
//C- | or (at your option) any later version. The license should have
//C- | accompanied the software or you may obtain a copy of the license
//C- | from the Free Software Foundation at http://www.fsf.org .
//C- |
//C- | The computer code originally released by LizardTech under this
//C- | license and unmodified by other parties is deemed "the LIZARDTECH
//C- | ORIGINAL CODE."  Subject to any third party intellectual property
//C- | claims, LizardTech grants recipient a worldwide, royalty-free, 
//C- | non-exclusive license to make, use, sell, or otherwise dispose of 
//C- | the LIZARDTECH ORIGINAL CODE or of programs derived from the 
//C- | LIZARDTECH ORIGINAL CODE in compliance with the terms of the GNU 
//C- | General Public License.   This grant only confers the right to 
//C- | infringe patent claims underlying the LIZARDTECH ORIGINAL CODE to 
//C- | the extent such infringement is reasonably necessary to enable 
//C- | recipient to make, have made, practice, sell, or otherwise dispose 
//C- | of the LIZARDTECH ORIGINAL CODE (or portions thereof) and not to 
//C- | any greater extent that may be necessary to utilize further 
//C- | modifications or combinations.
//C- |
//C- | The LIZARDTECH ORIGINAL CODE is provided "AS IS" WITHOUT WARRANTY
//C- | OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//C- | TO ANY WARRANTY OF NON-INFRINGEMENT, OR ANY IMPLIED WARRANTY OF
//C- | MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
//C- +------------------------------------------------------------------

#ifdef HAVE_CONFIG_H
# include "config.h"
#endif
#if NEED_GNUG_PRAGMAS
# pragma implementation
#endif

#include "DjVuGlobal.h"
#include "GException.h"
#include "GSmartPointer.h"
#include "GContainer.h"
#include "GRect.h"
#include "GBitmap.h"
#include "JB2Image.h"

#include "jb2tune.h"

#include "jb2cmp/minidjvu.h"
#include "jb2cmp/patterns.h"
#include "jb2cmp/classify.h"

#include <math.h>

#define REFINE_THRESHOLD 21



// ----------------------------------------
// UTILITIES


// Keep informations for pattern matching
struct MatchData 
{
  GP<GBitmap> bits;    // bitmap pointer
  int area;            // number of black pixels
  int match;           // jb2cmp pattern match
};


// Compute the number of black pixels.
static int 
compute_area(GBitmap *bits)
{
  GBitmap &bitmap = *bits;
  int w = bitmap.columns();
  int h = bitmap.rows();
  int black_pixels = 0;
  for (int i=0; i<h; i++)
    {
      unsigned char *row = bitmap[i];
      for (int j=0; j<w; j++)
        if (row[j])
          black_pixels++;
    }
  return black_pixels;
}


// Estimate position of baseline.
// The baseline position is measured in quarter pixels.
// Using fractional pixels makes a big improvement.
static int
compute_baseline(GBitmap *bits)
{
   int h = bits->rows();
   int w = bits->columns();
   GTArray<int> mass(h);
   int i, j, m;
   int tm = 0;
   for (i=0; i<h; i++)
     {
       unsigned char *row = (*bits)[i];
       for (j=0; j<w; j++)
         if (row[j])
           break;
       for (m = w-j; m>0; m--)
         if (row[j+m-1])
           break;
       mass[i] = m;
       tm += m;
     }
   m = 0;
   i = 0;
   while (m * 6 < tm * 4)
     {
       m += mass[i/4];
       i += 1;
     }
   return i;
}


// Fill the MatchData array for lossless compression
static void
compute_matchdata_lossless(JB2Image *jimg, MatchData *lib)
{
  int i;
  int nshapes = jimg->get_shape_count();
  for (i=0; i<nshapes; i++)
    {
      JB2Shape &jshp = jimg->get_shape(i);
      lib[i].bits = 0;
      lib[i].area = 0;
      lib[i].match = -1;
      if (! jshp.bits) continue;
      if (jshp.userdata & JB2SHAPE_SPECIAL) continue;
      lib[i].bits = jshp.bits;
      lib[i].area = compute_area(jshp.bits);
    }
}


// Interface with Ilya's data structures.
static mdjvu_pattern_t 
compute_comparable_image(GBitmap *bits)
{
  int w = bits->columns();
  int h = bits->rows();
  GTArray<unsigned char*> p(h);
  for (int i=0; i<h; i++) p[h-i-1] = (*bits)[i];
  return mdjvu_pattern_create_from_array(p, w, h);  
}


// Compute MatchData array for lossy compression.
static void
compute_matchdata_lossy(JB2Image *jimg, MatchData *lib,
                        int dpi, mdjvu_matcher_options_t options)
{
  int i;
  int nshapes = jimg->get_shape_count();
  // Prepare MatchData
  GTArray<mdjvu_pattern_t> handles(nshapes);
  for (i=0; i<nshapes; i++)
    {
      JB2Shape &jshp = jimg->get_shape(i);
      lib[i].bits = 0;
      lib[i].area = 0;
      lib[i].match = -1;
      handles[i] = 0;
      if (! jshp.bits) continue;
      if (jshp.userdata & JB2SHAPE_SPECIAL) continue;
      lib[i].bits = jshp.bits;
      lib[i].area = compute_area(jshp.bits);
      handles[i] = compute_comparable_image(jshp.bits);
    }
  // Run Ilya's pattern matcher.
  GTArray<int> tags(nshapes);  
  int maxtag = mdjvu_classify_patterns(handles, tags, nshapes, dpi, options);
  // Extract substitutions
  GTArray<int> reps(maxtag);
  for (i=0; i<=maxtag; i++)
    reps[i] = -1;
  for (i=0; i<nshapes; i++)
    if (handles[i])
      {
        int r = reps[tags[i]];
        lib[i].match = r;
        if (r < 0) 
          reps[tags[i]] = i;
      }
  // Free Ilya's data structures.
  for (i=0; i<nshapes; i++)
    if (handles[i])
      mdjvu_pattern_destroy(handles[i]);
}


// Reorganize jb2image on the basis of matchdata.
// Also locate cross-coding buddys.
// Flag lossy is not strictly necessary
// but speeds up things when it is false.
static void 
tune_jb2image(JB2Image *jimg, MatchData *lib, bool lossy)
{
  int nshapes = jimg->get_shape_count();
  // Loop on all shapes
  for (int current=0; current<nshapes; current++)
    {
      JB2Shape &jshp = jimg->get_shape(current);
      // Process substitutions.
      if (lossy && !(jshp.userdata & JB2SHAPE_LOSSLESS))
        {
          int substitute = lib[current].match;
          if (substitute >= 0)
            {
              jshp.parent = substitute;
              lib[current].bits = 0;
              continue;
            }
        }
      // Leave special shapes alone.
      if (! jshp.bits) continue;
      if (jshp.userdata & JB2SHAPE_SPECIAL) continue;
      // Compute matchdata info
      GBitmap &bitmap = *(jshp.bits);
      int rows = bitmap.rows();
      int cols = bitmap.columns();
      int best_score = (REFINE_THRESHOLD * rows * cols + 50) / 100;
      int black_pixels = lib[current].area;
      int closest = -1;
      // Search cross-coding buddy
      bitmap.minborder(2);
      if (best_score < 2) 
        best_score = 2;
      for (int candidate = 0; candidate < current; candidate++) 
        {
          int row, column;
          // Access candidate bitmap
          if (! lib[candidate].bits) 
            continue;
          GBitmap &cross_bitmap = *lib[candidate].bits;
          int cross_cols = cross_bitmap.columns();
          int cross_rows = cross_bitmap.rows();
          // Prune
          if (abs (lib[candidate].area - black_pixels) > best_score) 
            continue;
          if (abs (cross_rows - rows) > 2) 
            continue;
          if (abs (cross_cols - cols) > 2)
            continue;
          // Compute alignment (these are always +1, 0 or -1)
          int cross_col_adjust = (cross_cols-cross_cols/2)-(cols-cols/2);
          int cross_row_adjust = (cross_rows-cross_rows/2)-(rows-rows/2);
          // Ensure adequate borders
          cross_bitmap.minborder (2-cross_col_adjust);
          cross_bitmap.minborder (2+cols-cross_cols+cross_col_adjust);
          // Count pixel differences (including borders)
          int score = 0;
          unsigned char *p_row;
          unsigned char *p_cross_row;
          for (row = -1; row <= rows; row++) 
            {
              p_row = bitmap[row];
              p_cross_row = cross_bitmap[row+cross_row_adjust];
              p_cross_row += cross_col_adjust;
              for (column = -1; column <= cols; column++) 
                if (p_row[column] != p_cross_row[column])
                  score ++;
              if (score >= best_score)  // prune
                break;
            }
          if (score < best_score) 
            {
              best_score = score;
              closest = candidate;
            }
        }
      // Decide what to do with the match.
      if (closest >= 0)
        {
          // Mark the shape for cross-coding (``soft pattern matching'')
          jshp.parent = closest;
          // Exact match ==> Substitution
          if (best_score == 0)
            {
              lib[current].match = closest;
              lib[current].bits = 0;
            }
          // ISSUE: CROSS-IMPROVING.  When we decide not to do a substitution,
          // we can slightly modify the current shape in order to make it
          // closer to the matching shape, therefore improving the file size.
          // In fact there is a continuity between pure cross-coding and pure
          // substitution...
        }
    }
  
  // Process shape substitutions
  for (int blitno=0; blitno<jimg->get_blit_count(); blitno++)
    {
      JB2Blit *jblt = jimg->get_blit(blitno);
      JB2Shape &jshp = jimg->get_shape(jblt->shapeno);
      if (lib[jblt->shapeno].bits==0 && jshp.parent>=0)
        {
          // Locate parent
          int parent = jshp.parent;
          while (! lib[parent].bits)
            parent = lib[parent].match;
          // Compute coordinate adjustment.
          int cols = jshp.bits->columns();
          int rows = jshp.bits->rows();
          int cross_cols = lib[parent].bits->columns();
          int cross_rows = lib[parent].bits->rows();
          int cross_col_adjust = (cross_cols-cross_cols/2)-(cols-cols/2);
          int cross_row_adjust = (cross_rows-cross_rows/2)-(rows-rows/2);
           // Refine vertical adjustment
          if (lossy)
            {
              int adjust = compute_baseline(lib[parent].bits) 
                         - compute_baseline(jshp.bits);
              if (adjust < 0)
                adjust = - (2 - adjust) / 4;
              else
                adjust =   (2 + adjust) / 4;
              if (abs(adjust - cross_row_adjust) <= 1 + cols/16 )
                cross_row_adjust = adjust;
            }
          // Update blit record.
          jblt->bottom -= cross_row_adjust;
          jblt->left   -= cross_col_adjust;
          jblt->shapeno = parent;
          // Update shape record.
          jshp.bits = 0;
        }
    }
}




// ----------------------------------------
// LOSSLESS COMPRESSION


void 
tune_jb2image_lossless(JB2Image *jimg)
{
  int nshapes = jimg->get_shape_count();
  GArray<MatchData> lib(nshapes);
  compute_matchdata_lossless(jimg, lib);
  tune_jb2image(jimg, lib, false);
}


// ----------------------------------------
// LOSSY COMPRESSION
// Thanks to Ilya Mezhirov.

void 
tune_jb2image_lossy(JB2Image *jimg, int dpi, int aggression)
{
  int nshapes = jimg->get_shape_count();
  GArray<MatchData> lib(nshapes);

  mdjvu_matcher_options_t options = mdjvu_matcher_options_create();
  mdjvu_set_aggression(options, aggression);
  compute_matchdata_lossy(jimg, lib, dpi, options);
  mdjvu_matcher_options_destroy(options);

  tune_jb2image(jimg, lib, true);
}
