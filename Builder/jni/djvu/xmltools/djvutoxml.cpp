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

#include "DjVuDocument.h"
#include "GOS.h"
#include "DjVuMessage.h"
#include "ByteStream.h"
#include "DjVuAnno.h"
#include "DjVuText.h"
#include "DjVuImage.h"
#include "debug.h"

#include "common.h"
#include <sys/stat.h>
#include <assert.h>
#include <ctype.h>

static void
usage(void)
{
  DjVuPrintErrorUTF8("Usage: %s [options] <inputfile> <outputfile>\n"
                     "Options:\n"
                     "  --with[out]-anno\n"
                     "  --with[out]-text\n"
                     "  --page p\n",
                     (const char *)GOS::basename(DjVuMessage::programname()));
}

//------------------------ implementation ------------------------
int
main(int argc, char * argv[], char *env[])
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);

  G_TRY
  {
      GUTF8String name_in, name_out;
      int notext = -1;
      int noanno = -1;
      int page_num = -1;
      
      for(int i=1;i<argc;i++)
      {
        GUTF8String arg(dargv[i]);
        if (arg == "-" || arg[0] != '-' || arg[1] != '-')
        {
          if (!name_in.length())
          {
            if (arg == "-")
            {
              DjVuMessage::perror( ERR_MSG("DjVuToXML.std_read") );
              usage();
              exit(1);
            }
            name_in=arg;
          } 
          else if (!name_out.length())
          {
            name_out=arg;
          }
          else
          {
            usage();
            exit(1);
          }
        }
        else if (arg == "--page")
        {
          if (i+1>=argc)
            {
              DjVuMessage::perror( ERR_MSG("DjVuToXML.no_num") );
              usage();
              exit(1);
            }
          i++;
          page_num=dargv[i].toInt() - 1;
          if (page_num<0)
            {
              DjVuMessage::perror( ERR_MSG("DjVuToXML.negative_num") );
              usage();
              exit(1);
            }
        }
        else if(arg == "--with-text")
        {
          notext=0;
          if(noanno<0)
            noanno=1;
        }
        else if(arg == "--without-text")
        {
          notext=1;
          if(noanno<0)
            noanno=0;
        }
        else if(arg == "--with-anno")
        {
          noanno=0;
          if(notext<0)
            notext=1;
        }
        else if(arg == "--without-anno")
        {
          noanno=1;
          if(notext<0)
            notext=0;
        }
        else if (arg == "--help")
        {
          usage();
          exit(1);
        } 
        else
        {
          DjVuMessage::perror( ERR_MSG("DjVuToXML.unrecog_opt") "\t"+arg);
        }
      }
      
      if (!name_in.length())
      {
        DjVuMessage::perror( ERR_MSG("DjVuToXML.no_name") );
        usage();
        exit(1);
      }
      if (!name_out.length())
      {
        name_out="-";
      }
      
      GP<DjVuDocument> doc = 
        DjVuDocument::create_wait(GURL::Filename::UTF8(name_in));

      GP<ByteStream> gstr_out = 
        ByteStream::create(GURL::Filename::UTF8(name_out), "w");

      int flags=0;
      if(noanno > 0)
        flags |= DjVuImage::NOMAP;
      if(notext > 0)
        flags |= DjVuImage::NOTEXT;
      doc->writeDjVuXML(gstr_out,flags, page_num);
  }
  G_CATCH(exc)
  {
    exc.perror();
    exit(1);
  }
  G_ENDCATCH;
  return 0;
}

