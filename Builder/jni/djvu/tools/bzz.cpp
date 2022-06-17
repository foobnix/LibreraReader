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

// BZZ -- a frontend for BSByteStream

/** @name bzz

    \begin{description}
    \item[Compression:]
    #bzz -e[<blocksize>] <infile> <outfile>#
    \item[Decompression:]
    #bzz -d <infile> <outfile>#
    \end{description}    

    Program bzz is a simple front-end for the Burrows Wheeler encoder
    implemented in \Ref{BSByteStream.h}.  Although this compression model is
    not currently used in DjVu files, it may be used in the future for
    encoding textual data chunks.  Argument #blocksize# is expressed in
    kilobytes and must be in range 200 to 4096.  The default value is 2048.
    Arguments #infile# and #outfile# are the input and output filenames. A
    single dash (#"-"#) can be used to represent the standard input or output.

    @memo
    General purpose compression/decompression program
    @author
    L\'eon Bottou <leonb@research.att.com> -- initial implementation
*/
//@{
//@}

#include "GException.h"
#include "ByteStream.h"
#include "BSByteStream.h"
#include "GOS.h"
#include "GURL.h"
#include "DjVuMessage.h"
#include "common.h"

static const char *program = "(unknown)";

void
usage(void)
{
  DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
          "BZZ --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
          "Compress/decompress <infile> using the Burrows Wheeler\n"
          "transform and the ZP adaptive binary coder.\n\n"
          "Usage [encoding]: %s -e[<blocksize>] <infile> <outfile>\n"
          "Usage [decoding]: %s -d <infile> <outfile>\n"
          "  Argument <blocksize> must be in range [900..4096] (default 1100).\n"
          "  Arguments <infile> and <outfile> can be '-' for stdin/stdout.\n"
          , program, program);
  exit(1);
}

int 
main(int argc, char **argv)
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);
  G_TRY
    {
      if(argc < 2)
        usage();
      // Get program name
      program=dargv[0]=GOS::basename(dargv[0]);
      // Obtain default mode from program name
      int blocksize = -1;
      if (dargv[0] == "bzz")
        blocksize = 1100;
      else if (dargv[0] == "unbzz")
        blocksize = 0;
      // Parse arguments
      if (argc>=2 && dargv[1][0]=='-')
        {
          if (dargv[1][1]=='d' && dargv[1][2]==0)
            {
              blocksize = 0;
            }
          else if (dargv[1][1]=='e')
            {
              blocksize = 2048;
              if (dargv[1][2])
                 blocksize = dargv[1].substr(2, dargv[1].length()).toInt(); //atoi(2+(const char *)dargv[1]);
            }
          else 
            usage();
          dargv.shift(-1);
          argc--;
        }
      if (blocksize < 0)
        usage();
      // Obtain filenames
      const GURL::Filename::UTF8 inurl((argc>=2)?dargv[1]:GUTF8String("-"));
      const GURL::Filename::UTF8 outurl((argc>=3)?dargv[2]:GUTF8String("-"));
      if (argc >= 4)
        usage();
      // Action
      GP<ByteStream> in=ByteStream::create(inurl,"rb");
      GP<ByteStream> out=ByteStream::create(outurl,"wb");
      if (blocksize)
        {
          GP<ByteStream> gbsb=BSByteStream::create(out, blocksize);
          gbsb->copy(*in);
        }
      else 
        {
          GP<ByteStream> gbsb=BSByteStream::create(in);
          out->copy(*gbsb);
        }
    }
  G_CATCH(ex)
    {
      ex.perror();
      exit(1);
    }
  G_ENDCATCH;
  return 0;
}

