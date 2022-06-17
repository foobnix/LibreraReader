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

#include "XMLParser.h"
#include "XMLTags.h"
#include "GOS.h"
#include "GURL.h"
#include "DjVuDocument.h"
#include "ByteStream.h"
#include "DjVuMessage.h"
#include "common.h"

static void 
usage(char *argv0)
{
  DjVuPrintErrorUTF8("Usage: %s [-o <djvufile>] <xmlfile> ...\n", argv0);
  exit(1);
}

static void 
nofile(char *s)
{
  DjVuPrintErrorUTF8("Error: File '%s' does not exist.\n",s);
  exit(1);
}

int 
main(int argc,char *argv[],char *[])
{
  DJVU_LOCALE;
  G_TRY
    {
      int i;
      if (argc < 2)
        usage(argv[0]);
      for (i=1; i<argc; i++)
        {
          GURL djvufile;
          GURL *pdjvufile = 0;
          if (! strcmp(argv[i], "-o"))
            {
              if (++i >= argc) 
                usage(argv[0]);
              djvufile = GURL::Filename::Native(argv[i]);
              pdjvufile = &djvufile;
              if (! djvufile.is_file())
                nofile(argv[i]);
              if (++i >= argc) 
                usage(argv[0]);
            }
          GURL xmlfile = GURL::Filename::Native(argv[i]);
          if (! xmlfile.is_file())
            nofile(argv[i]);
          GP<lt_XMLParser> parser(lt_XMLParser::create());
          GP<lt_XMLTags> tag(lt_XMLTags::create(xmlfile));
          parser->parse(*tag, pdjvufile);
          parser->save();
        }
    }
  G_CATCH(ex)
    {
      ex.perror();
      exit(1);
    }
  G_ENDCATCH;
  exit(0);
#ifdef _WIN32
  return 0;
#endif
}

