//C-  -*- C -*-
//C- -------------------------------------------------------------------
//C- DjView4
//C- Copyright (c) 2006  Leon Bottou
//C-
//C- This software is subject to, and may be distributed under, the
//C- GNU General Public License, either version 2 of the license,
//C- or (at your option) any later version. The license should have
//C- accompanied the software or you may obtain a copy of the license
//C- from the Free Software Foundation at http://www.fsf.org .
//C-
//C- This program is distributed in the hope that it will be useful,
//C- but WITHOUT ANY WARRANTY; without even the implied warranty of
//C- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//C- GNU General Public License for more details.
//C-  ------------------------------------------------------------------

#ifndef TIFF2PDF_H
# define TIFF2PDF_H
# if HAVE_CONFIG_H
#  include "config.h"
# endif
# if HAVE_TIFF
#  include <stdio.h>
#  include <stddef.h>
#  include <stdlib.h>
#  include <tiff.h>
#  include <tiffio.h>
#  include <tiffconf.h>
#  ifdef TIFFLIB_VERSION
#   if TIFFLIB_VERSION > 20041104
#    define HAVE_TIFF2PDF 1
#   endif
#  endif
# endif
# if HAVE_TIFF2PDF
#  ifdef __cplusplus
extern "C" {
#  endif
int tiff2pdf(TIFF *input, FILE *output, int argc, const char **argv);
#  ifdef __cplusplus
}
#  endif
# endif  /* HAVE_TIFF2PDF */
#endif /* TIFF2PDF_H */
