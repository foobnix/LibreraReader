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

/** @name djvuextract

    {\bf Synopsis}
    \begin{verbatim}
    djvuextract <djvufile> [-page=<page>] [Sjbz=<maskout>] [FG44=<fgout>] [BG44=<bgout>]
    \end{verbatim}
    
    {\bf Description}\\
    Program #djvuextract# analyzes the DjVu file
    #<djvufile># and saves the various layers into the specified files.
    The reverse operation can be achieved using program \Ref{djvumake}.
    \begin{itemize}
    \item When option #Sjbz=<maskout># is specified, the foreground mask is
      saved into file #<maskout># as JB2 data. This data file can be read
      using function \Ref{JB2Image::decode} in class \Ref{JB2Image}.
    \item When option #FG44=<fgout># is specified, the foreground color image
      is saved into file #<fgout># as IW44 data.  This data file can be processed
      using programs \Ref{d44}.
    \item When option #BG44=<bgout># is specified, the background color image
      is saved into file #<bgout># as IW44 data.  This data file can be processed
      using programs \Ref{d44}.
    \item Optionally one can provide a #-page# option to select a given
      page from the document, if it's a multipage document. The page numbers
      start from #1#.
    \end{itemize}
    This commands also supports #"Smmr"# chunks for G4/MMR encoded masks,
    #"FGjp"# and #"BGjp"# for JPEG encoded color layers, and finally #"FG2k"#
    and #"BG2k"# for JPEG-2000 encoded color layers.

    @memo
    Extract components from DjVu files.
    @author
    L\'eon Bottou <leonb@research.att.com> - Initial implementation\\
    Andrei Erofeev <eaf@geocities.com> - Multipage support */
//@{
//@}

#include "GException.h"
#include "ByteStream.h"
#include "IFFByteStream.h"
#include "DjVuDocument.h"
#include "DjVuFile.h"
#include "GOS.h"
#include "DjVuMessage.h"
#include "common.h"

struct DejaVuInfo
{
  unsigned char width_hi, width_lo;
  unsigned char height_hi, height_lo;
  char version;
} djvuinfo;

struct PrimaryHeader {
  unsigned char serial;
  unsigned char slices;
} primary;

struct SecondaryHeader {
  unsigned char major;
  unsigned char minor;
  unsigned char xhi, xlo;
  unsigned char yhi, ylo;
} secondary;


static void
extract_chunk(GP<ByteStream> ibs, const GUTF8String &id, GP<ByteStream> out)
{
  ibs->seek(0);
  GP<IFFByteStream> giff=IFFByteStream::create(ibs);
  IFFByteStream &iff=*giff;
  GUTF8String chkid;
  if (! iff.get_chunk(chkid))
    G_THROW("Malformed DJVU file");
  if (chkid != "FORM:DJVU" && chkid != "FORM:DJVI" )
    G_THROW("This is not a layered DJVU file");
  

  // Special case for FG44 and BG44
  if (id == "BG44" || id == "FG44")
    {
      // Rebuild IW44 file
      GP<IFFByteStream> giffout=IFFByteStream::create(out);
      IFFByteStream &iffout=*giffout;
      int color_bg = -1;
      while (iff.get_chunk(chkid))
        {
          if (chkid == id)
            {
              GP<ByteStream> gtemp=ByteStream::create();
              ByteStream &temp=*gtemp;
              temp.copy(*iff.get_bytestream());
              temp.seek(0);
              if (temp.readall((void*)&primary, sizeof(primary))<sizeof(primary))
                G_THROW("Cannot read primary header in BG44 chunk");
              if (primary.serial == 0)
                {
                  if (temp.readall((void*)&secondary, sizeof(secondary))<sizeof(secondary))
                    G_THROW("Cannot read secondary header in BG44 chunk");
                  color_bg = ! (secondary.major & 0x80);
                  iffout.put_chunk(color_bg ? "FORM:PM44" : "FORM:BM44");
                }
              if (color_bg < 0)
                G_THROW("IW44 chunks are not in proper order");
              temp.seek(0);
              iffout.put_chunk(color_bg ? "PM44" : "BM44");
              iffout.copy(temp);
              iffout.close_chunk();
            }
          iff.close_chunk();
        }
    }
  else
    {
      // Just concatenates all matching chunks
      while (iff.get_chunk(chkid))
        {
          if (chkid == id)
            out->copy(*iff.get_bytestream());
          iff.close_chunk();
        }
    }
}


void 
usage()
{
  DjVuPrintErrorUTF8("%s",
#ifdef DJVULIBRE_VERSION
          "DJVUEXTRACT --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
          "Extracts components of a DjVu file\n"
          "\n"
          "Usage:\n"
	  "   djvuextract <djvufile> [-page=<num>] {...<chunkid>=<file>...} \n");
  exit(1);
}


int
main(int argc, char **argv)
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);
  int retcode = 0;
  G_TRY
    {
      int i;

      // Process page number
      int page_num=0;
      for(i=1;i<argc;i++)
	 if (!dargv[i].cmp("-page=", 6))
           {
	     page_num = dargv[i].substr(6,dargv[i].length()).toInt() - 1;
             for(int j=i;j<argc-1;j++) 
               dargv[j]=dargv[j+1];
             argc--;
             break;
           } 
      if (page_num<0)
        {
          DjVuPrintErrorUTF8("%s", "Invalid page number\n");
          usage();
        }
      
      // Check that chunk names are legal
      if (argc<=2)
        usage();
      for (i=2; i<argc; i++)
        if (IFFByteStream::check_id(dargv[i]) || dargv[i][4]!='=' || dargv[i][5]==0)
          usage();

      // Decode
      const GURL::Filename::UTF8 url1(dargv[1]);
      GP<DjVuDocument> doc=DjVuDocument::create_wait(url1);
      if (! doc->wait_for_complete_init() || ! doc->is_init_ok())
        G_THROW("Decoding failed. Nothing can be done.");        
      GP<DjVuFile> file=doc->get_djvu_file(page_num);
      GP<ByteStream> pibs = file->get_djvu_bytestream(false, false);
      // Extract required chunks
      for (i=2; i<argc; i++)
        {
          GP<ByteStream> gmbs=ByteStream::create();
          const GUTF8String chunkid=dargv[i].substr(0,4);
          extract_chunk(pibs, chunkid, gmbs);
          ByteStream &mbs=*gmbs;
          if (mbs.size() == 0)
            {
              DjVuPrintErrorUTF8("  %s --> not found!\n", (const char *)dargv[i]);
	      retcode = 64;
            }
          else
            {
              const GURL::Filename::UTF8 url((const char *)dargv[i].substr(5,-1));
              GP<ByteStream> obs=ByteStream::create(url,"wb");
              mbs.seek(0);
              obs->copy(mbs);
              DjVuPrintErrorUTF8("  %s --> \"%s\" (%d bytes)\n", 
                      (const char *)dargv[i], (const char *)dargv[i]+5, mbs.size());
            }
        }
    }
  G_CATCH(ex)
    {
      ex.perror();
      exit(1);
    }
  G_ENDCATCH;
  return retcode;
}
