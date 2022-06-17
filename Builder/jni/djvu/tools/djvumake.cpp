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

/** @name djvumake

    {\bf Synopsis}
    \begin{verbatim}
       % djvumake <djvufile> [...<arguments>...]
    \end{verbatim}

    {\bf Description}

    This command assembles a single-page DjVu file by copying or creating
    chunks according to the provided <arguments>. Supported syntaxes for
    <arguments> are as follows:
    \begin{tabular}{ll}
    {#INFO=<w>,<h>,<dpi>#} &
      Creates the initial ``INFO'' chunk.  Arguments #w#, #h# and #dpi#
      describe the width, height and resolution of the image.  All arguments
      may be omitted.  The default resolution is 300 dpi.  The default width
      and height will be retrieved from the first mask chunk specified in the
      command line options.\\
      {#Sjbz=<jb2file>#} &
      Creates a JB2 mask chunk.  File #jb2file# may
      contain raw JB2 data or be a DjVu file containing JB2 data, such as the
      files produced by \Ref{cjb2}.\\
      {#Smmr=<mmrfile>] #} &
      Creates a mask chunk containing MMR/G4 data.  File #mmrfile#
      may contain raw MMR data or be a DjVu file containing MMR data.\\
      {#BG44=<iw44file>:<n>#} &
      Creates one or more IW44 background chunks.  File #iw44file# must be an
      IW44 file such as the files created by \Ref{c44}.  The optional argument
      #n# indicates the number of chunks to copy from the IW44 file.\\
      {#BGjp=<jpegfile>#} &
      Creates a JPEG background chunk.\\
      {#BG2k=<jpegfile>#} &
      Creates a JPEG-2000 background chunk.\\
      {#FG44=<iw44file>#} &
      Creates one IW44 foreround chunks.  File #iw44file# must be an
      IW44 file such as the files created by \Ref{c44}.  Only the first
      chunk will be copied.\\
      {FGbz=(bzzfile|\{#color1[:x,y,w,h]\})}
      Creates a chunk containing colors for each JB2 encoded object.
      Such chunks are created using class \Ref{DjVuPalette}.
      See program \Ref{cpaldjvu} for an example.\\
      {#FGjp=<jpegfile>#} &
      Creates a JPEG foreground chunk.\\
      {#FG2k=<jpegfile>#} &
      Creates a JPEG-2000 foreground chunk.\\
      {#INCL=<fileid>#} &
      Creates an include chunk pointing to <fileid>.
      The resulting file should then be included into a 
      multipage document.\\
      {#PPM=<ppmfile>#} (psuedo-chunk) &
      Create IW44 foreground and background chunks
      by masking and subsampling PPM file #ppmfile#.
      This is used by program \Ref{cdjvu}.\\
      {#chunk=<rawdatafile>#} &
      Creates the specified chunk with the specified raw data.
    \end{tabular}

    Let us assume now that you have a PPM image #"myimage.ppm"# and a PBM
    bitonal image #"mymask.pbm"# whose black pixels indicate which pixels
    belong to the foreground.  Such a bitonal file may be obtained by
    thresholding, although more sophisticated techniques can give better
    results.  You can then generate a Compound DjVu File by typing:
    \begin{verbatim}
       % cjb2 mymask.pbm mymask.djvu
       % djvumake mycompound.djvu Sjbz=mymask.djvu PPM=myimage.ppm
    \end{verbatim}

    @memo
    Assemble DjVu files.
    @author
    L\'eon Bottou <leonb@research.att.com> \\
    Patrick Haffner <haffner@research.att.com>
*/
//@{
//@}

#include "GString.h"
#include "GException.h"
#include "DjVuImage.h"
#include "MMRDecoder.h"
#include "IFFByteStream.h"
#include "JB2Image.h"
#include "IW44Image.h"

#include "GPixmap.h"
#include "GBitmap.h"
#include "GContainer.h"
#include "GRect.h"
#include "DjVuMessage.h"

#include "common.h"

int flag_contains_fg      = 0;
int flag_contains_bg      = 0;
int flag_contains_stencil = 0;
int flag_contains_incl    = 0;
int flag_fg_needs_palette = 0;

struct DJVUMAKEGlobal 
{
  // Globals that need static initialization
  // are grouped here to work around broken compilers.
  GP<ByteStream> jb2stencil;
  GP<ByteStream> mmrstencil;
  GP<JB2Image> stencil;
  GP<JB2Dict> dictionary;
  GTArray<GRect> colorzones;
  GP<ByteStream> colorpalette;
};

static DJVUMAKEGlobal& g(void)
{
  static DJVUMAKEGlobal g;
  return g;
}

int w = -1;
int h = -1;
int dpi = 300;
int blit_count = -1;

// -- Display brief usage information

void 
usage()
{
  DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
         "DJVUMAKE --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
         "Utility for manually assembling DjVu files\n\n"
         "Usage: djvumake djvufile ...arguments...\n"
         "\n"
         "The arguments describe the successive chunks of the DJVU file.\n"
         "Possible arguments are:\n"
         "\n"
         "   INFO=w[,[h[,[dpi]]]]     --  Create the initial information chunk\n"
         "   Sjbz=jb2file             --  Create a JB2 mask chunk\n"
         "   Djbz=jb2file             --  Create a JB2 shape dictionary\n"
         "   Smmr=mmrfile             --  Create a MMR mask chunk\n"
         "   BG44=[iw4file][:nchunks] --  Create one or more IW44 background chunks\n"
         "   BGjp=jpegfile            --  Create a JPEG background chunk\n"
         "   BG2k=jpeg2000file        --  Create a JP2K background chunk\n"
         "   FG44=iw4file             --  Create an IW44 foreground chunk\n"
         "   FGbz=bzzfile             --  Create a foreground color chunk from a file\n"
         "   FGbz={#color:x,y,w,h}    --  Create a foreground color chunk from zones\n"
         "   FGjp=jpegfile            --  Create a JPEG foreground image chunk\n"
         "   FG2k=jpeg2000file        --  Create a JP2K foreground image chunk\n"
         "   INCL=fileid              --  Create an INCL chunk\n"
         "   chunk=rawdatafile        --  Create the specified chunk from the raw data file\n"
         "   PPM=ppmfile              --  Create IW44 foreground and background chunks\n"
         "                                by masking and subsampling a PPM file.\n"
         "\n"
         "You may omit the specification of the information chunk. An information\n"
         "chunk will be created using the image size of the first mask chunk\n"
         "This program is sometimes able  to issue a warning when you are building an\n"
         "incorrect djvu file.\n"
         "\n");
  exit(1);
}

// -- Obtain image size from mmr chunk

void
analyze_mmr_chunk(const GURL &url)
{
  if (!g().mmrstencil || !g().mmrstencil->size())
    {
      GP<ByteStream> gbs=ByteStream::create(url,"rb");
      ByteStream &bs=*gbs;
      g().mmrstencil = ByteStream::create();
      // Check if file is an IFF file
      char magic[4];
      memset(magic,0,sizeof(magic));
      bs.readall(magic,sizeof(magic));
      if (!GStringRep::cmp(magic,"AT&T",4))
        bs.readall(magic,sizeof(magic));
      if (GStringRep::cmp(magic,"FORM",4))
        {
          // Must be a raw file
          bs.seek(0);
          g().mmrstencil->copy(bs);
        }
      else
        {
          // Search Smmr chunk
          bs.seek(0);
          GUTF8String chkid;
          GP<IFFByteStream> giff=IFFByteStream::create(gbs);
          IFFByteStream &iff=*giff;
          if (iff.get_chunk(chkid)==0 || chkid!="FORM:DJVU")
            G_THROW("Expecting a DjVu file!");
          for(; iff.get_chunk(chkid); iff.close_chunk())
            if (chkid=="Smmr") { g().mmrstencil->copy(bs); break; }
        }
      // Check result
      g().mmrstencil->seek(0);
      if (!g().mmrstencil->size())
        G_THROW("Could not find MMR data");
      // Decode
      g().stencil = MMRDecoder::decode(g().mmrstencil);
      int jw = g().stencil->get_width();
      int jh = g().stencil->get_height();
      if (w < 0) w = jw;
      if (h < 0) h = jh;
      if (jw!=w || jh!=h)
        DjVuPrintErrorUTF8("djvumake: mask size (%s) does not match info size\n", (const char *)url);
    }
}


// -- Obtain shape dictionary

void 
analyze_djbz_chunk(GP<ByteStream> gbs)
{
  if (g().dictionary)
    G_THROW("Duplicate Djbz dictionary");
  g().dictionary = JB2Dict::create();
  g().dictionary->decode(gbs);
}

void 
analyze_djbz_chunk(const GURL &url)
{
  GP<ByteStream> gbs = ByteStream::create(url, "rb");
  analyze_djbz_chunk(gbs);
}


// -- Obtain image size and blit count from jb2 chunk

GP<JB2Dict> 
provide_shared_dict( void* )
{
  return(g().dictionary);
}

void 
analyze_jb2_chunk(const GURL &url)
{
  if (!g().jb2stencil || !g().jb2stencil->size())
    {
      GP<ByteStream> gbs=ByteStream::create(url,"rb");
      ByteStream &bs=*gbs;
      g().jb2stencil = ByteStream::create();
      // Check if file is an IFF file
      char magic[4];
      memset(magic,0,sizeof(magic));
      bs.readall(magic,sizeof(magic));
      if (!GStringRep::cmp(magic,"AT&T",4))
        bs.readall(magic,sizeof(magic));
      if (GStringRep::cmp(magic,"FORM",4))
        {
          // Must be a raw file
          bs.seek(0);
          g().jb2stencil->copy(bs);
        }
      else
        {
          // Search Sjbz chunk
          bs.seek(0);
          GUTF8String chkid;
          GP<IFFByteStream> giff=IFFByteStream::create(gbs);
          IFFByteStream &iff=*giff;
          if (iff.get_chunk(chkid)==0 || chkid!="FORM:DJVU")
            G_THROW("Expecting a DjVu file!");
          for(; iff.get_chunk(chkid); iff.close_chunk())
            if (chkid=="Sjbz") { g().jb2stencil->copy(bs); break; }
        }
      // Check result
      g().jb2stencil->seek(0);
      if (!g().jb2stencil->size())
        G_THROW("Could not find JB2 data");
      // Decode
      g().stencil=JB2Image::create();
      g().stencil->decode(g().jb2stencil,&provide_shared_dict,NULL);
      int jw = g().stencil->get_width();
      int jh = g().stencil->get_height();
      if (w < 0) 
        w = jw;
      if (h < 0) 
        h = jh;
      if (blit_count < 0) 
        blit_count = g().stencil->get_blit_count();
      if (jw!=w || jh!=h)
        DjVuPrintErrorUTF8("djvumake: mask size (%s) does not match info size\n", (const char *)url);
    }
}

// -- Load dictionary from an INCL chunk

void
analyze_incl_chunk(const GURL &url)
{
  if (! url.is_file())
    return;
  GP<ByteStream> gbs = ByteStream::create(url,"rb");
  char buffer[24];
  memset(buffer, 0, sizeof(buffer));
  gbs->read(buffer,sizeof(buffer));
  char *s = buffer;
  if (!strncmp(s, "AT&T", 4))
    s += 4;
  if (strncmp(s, "FORM", 4) || strncmp(s+8, "DJVI", 4))
    G_THROW("Expecting a valid FORM:DJVI chunk in the included file");
  gbs->seek(0);
  GP<IFFByteStream> giff=IFFByteStream::create(gbs);
  GUTF8String chkid;
  giff->get_chunk(chkid); // FORM:DJVI
  for(; giff->get_chunk(chkid); giff->close_chunk())
    if (chkid=="Djbz") 
      analyze_djbz_chunk(giff->get_bytestream());
}

void
check_for_shared_dict(GArray<GUTF8String> &argv)
{
  const int argc=argv.hbound()+1;
  for (int i=2; i<argc; i++)
    if (!argv[i].cmp("INCL=",5))
      analyze_incl_chunk(GURL::Filename::UTF8(5+(const char *)argv[i]));
    else if (!argv[i].cmp("Djbz=", 5))
      analyze_djbz_chunk(GURL::Filename::UTF8(5+(const char *)argv[i]));
}



// -- Create info chunk from specification or mask

void
create_info_chunk(IFFByteStream &iff, GArray<GUTF8String> &argv)
{
  const int argc=argv.hbound()+1;
  // Process info specification
  for (int i=2; i<argc; i++)
    if (!argv[i].cmp("INFO=",5))
      {
        int   narg = 0;
        const char *ptr = 5+(const char *)argv[i];
        while (*ptr)
          {
            if (*ptr != ',')
              {
                int x = strtol((char *)ptr, (char **)&ptr, 10);
                switch(narg)
                  {
                  case 0: 
                    w = x; break;
                  case 1: 
                    h = x; break;
                  case 2: 
                    dpi = x; break;
                  default:  
                    G_THROW("djvumake: incorrect 'INFO' chunk specification\n");
                  }
              }
            narg++;
            if (*ptr && *ptr++!=',')
              G_THROW("djvumake: comma expected in 'INFO' chunk specification\n");
          }
          break;
      }
  if (w>0 && (w<=0 || w>=32768))
    G_THROW("djvumake: incorrect width in 'INFO' chunk specification\n");
  if (h>0 && (h<=0 || h>=32768))
    G_THROW("djvumake: incorrect height in 'INFO' chunk specification\n");
  if (dpi>0 && (dpi<25 || dpi>6000))
    G_THROW("djvumake: incorrect dpi in 'INFO' chunk specification\n");
  // Search first mask chunks if size is still unknown
  if (h<0 || w<0)
    {
      for (int i=2; i<argc; i++)
        if (!argv[i].cmp("Sjbz=",5))
          {
            analyze_jb2_chunk(GURL::Filename::UTF8(5+(const char *)argv[i]));
            break;
          }
        else if (!argv[i].cmp("Smmr=",5))
          {
            analyze_mmr_chunk(GURL::Filename::UTF8(5+(const char *)argv[i]));
            break;
          }
    }
  
  // Check that we have everything
  if (w<0 || h<0)
    G_THROW("djvumake: cannot determine image size\n");
  // write info chunk
  GP<DjVuInfo> ginfo=DjVuInfo::create();
  DjVuInfo &info=*ginfo;
  info.width = w;
  info.height = h;
  info.dpi = dpi;
  iff.put_chunk("INFO");
  info.encode(*iff.get_bytestream());
  iff.close_chunk();
}


// -- Create MMR mask chunk

void 
create_mmr_chunk(IFFByteStream &iff, const char *chkid, const GURL &url)
{
  analyze_mmr_chunk(url);
  g().mmrstencil->seek(0);
  iff.put_chunk(chkid);
  iff.copy(*g().mmrstencil);
  iff.close_chunk();
}


// -- Create FGbz palette chunk

void 
create_fgbz_chunk(IFFByteStream &iff)
{
  int nzones = g().colorzones.size();
  int npalette = g().colorpalette->size() / 3;
  GP<DjVuPalette> pal = DjVuPalette::create();
  g().colorpalette->seek(0);
  pal->decode_rgb_entries(*g().colorpalette, npalette);
  pal->colordata.resize(0,blit_count-1);
  for (int d=0; d<blit_count; d++)
    {
      JB2Blit *blit = g().stencil->get_blit(d);
      const JB2Shape &shape = g().stencil->get_shape(blit->shapeno);
      GRect brect(blit->left, blit->bottom, shape.bits->columns(), shape.bits->rows());
      int index = nzones;
      for (int i=0; i<nzones; i++)
        {
          GRect zrect = g().colorzones[i];
          if (zrect.isempty() || zrect.intersect(brect, zrect))
            index = i;
        }
      if (index >= npalette)
        G_THROW("create_fgbz_chunk: internal error");
      pal->colordata[d] = index;
    }
  iff.put_chunk("FGbz");
  pal->encode(iff.get_bytestream());
  iff.close_chunk();
}

// -- Create JB2 mask chunk

void 
create_jb2_chunk(IFFByteStream &iff, const char *chkid, const GURL &url)
{
  analyze_jb2_chunk(url);
  g().jb2stencil->seek(0);
  iff.put_chunk(chkid);
  iff.copy(*g().jb2stencil);
  iff.close_chunk();
}


// -- Create inclusion chunk

void 
create_incl_chunk(IFFByteStream &iff, const char *chkid, const char *fileid)
{
  if (strchr(fileid, '/') || strchr(fileid, '\\') || strchr(fileid, ':'))
    G_THROW( ERR_MSG("DjVuFile.malformed") );
  iff.put_chunk("INCL");
  iff.write(fileid, strlen(fileid));
  iff.close_chunk();
}


// -- Create chunk by copying file contents

void 
create_raw_chunk(IFFByteStream &iff, const GUTF8String &chkid, const GURL &url)
{
  iff.put_chunk(chkid);
  GP<ByteStream> ibs=ByteStream::create(url,"rb");
  iff.copy(*ibs);
  iff.close_chunk();
}


// -- Internal headers for IW44

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

// -- Create and check FG44 chunk

void 
create_fg44_chunk(IFFByteStream &iff, const char *ckid, const GURL &url)
{
  GP<ByteStream> gbs=ByteStream::create(url,"rb");
  GP<IFFByteStream> gbsi=IFFByteStream::create(gbs);
  IFFByteStream &bsi=*gbsi;
  GUTF8String chkid;
  bsi.get_chunk(chkid);
  if (chkid != "FORM:PM44" && chkid != "FORM:BM44")
    G_THROW("djvumake: FG44 file has incorrect format (wrong IFF header)");
  bsi.get_chunk(chkid);
  if (chkid!="PM44" && chkid!="BM44")
    G_THROW("djvumake: FG44 file has incorrect format (wring IFF header)");
  GP<ByteStream> gmbs=ByteStream::create();
  ByteStream &mbs=*gmbs;
  mbs.copy(*bsi.get_bytestream());
  bsi.close_chunk();  
  if (bsi.get_chunk(chkid))
    DjVuPrintErrorUTF8("%s","djvumake: FG44 file contains more than one chunk\n");
  bsi.close_chunk();  
  mbs.seek(0);
  if (mbs.readall((void*)&primary, sizeof(primary)) != sizeof(primary))
    G_THROW("djvumake: FG44 file is corrupted (cannot read primary header)");    
  if (primary.serial != 0)
    G_THROW("djvumake: FG44 file is corrupted (wrong serial number)");
  if (mbs.readall((void*)&secondary, sizeof(secondary)) != sizeof(secondary))
    G_THROW("djvumake: FG44 file is corrupted (cannot read secondary header)");    
  int iw = (secondary.xhi<<8) + secondary.xlo;
  int ih = (secondary.yhi<<8) + secondary.ylo;
  int red;
  for (red=1; red<=12; red++)
    if (iw==(w+red-1)/red && ih==(h+red-1)/red)
      break;
  flag_contains_fg = red;
  if (red>12)
    DjVuPrintErrorUTF8("%s","djvumake: FG44 subsampling is not in [1..12] range\n");
  mbs.seek(0);
  iff.put_chunk(ckid);
  iff.copy(mbs);
  iff.close_chunk();
}



// -- Create and check BG44 chunk

void 
create_bg44_chunk(IFFByteStream &iff, const char *ckid, GUTF8String filespec)
{
  static GP<IFFByteStream> bg44iff;
  if (! bg44iff)
    {
      if (flag_contains_bg)
        DjVuPrintErrorUTF8("%s","djvumake: Duplicate BGxx chunk\n");
      int i=filespec.rsearch(':');
      for (int j=i+1; i>0 && j<(int)filespec.length(); j++)
        if (filespec[j] < '0' || filespec[j] > '9')
          i = -1;
      if (!i)
        G_THROW("djvumake: no filename specified in first BG44 specification");
      GUTF8String filename=(i<0)?filespec:GUTF8String(filespec, i);
      const GURL::Filename::UTF8 url(filename);
      const GP<ByteStream> gbs(ByteStream::create(url,"rb"));
      if(!gbs)
      {
        G_THROW("djvumake: no such file as"+filename);
      }
      bg44iff = IFFByteStream::create(gbs);
      GUTF8String chkid;
      bg44iff->get_chunk(chkid);
      if (chkid != "FORM:PM44" && chkid != "FORM:BM44")
        G_THROW("djvumake: BG44 file has incorrect format (wrong IFF header)");        
      if (i>=0)
        filespec = i+1+(const char *)filespec;
      else 
        filespec = "99";
    }
  else
    {
      if (filespec.length() && filespec[0]!=':')
        G_THROW("djvumake: filename specified in BG44 refinement");
      filespec = 1+(const char *)filespec;
    }
  const char *s=filespec;
  int nchunks = strtol((char *)s, (char **)&s, 10);
  if (nchunks<1 || nchunks>99)
    G_THROW("djvumake: invalid number of chunks in BG44 specification");    
  if (*s)
    G_THROW("djvumake: invalid BG44 specification (syntax error)");
  
  int flag = (nchunks>=99);
  GUTF8String chkid;
  while (nchunks-->0 && bg44iff->get_chunk(chkid))
    {
      if (chkid!="PM44" && chkid!="BM44")
        {
          DjVuPrintErrorUTF8("%s","djvumake: BG44 file contains unrecognized chunks (fixed)\n");
          nchunks += 1;
          bg44iff->close_chunk();
          continue;
        }
      GP<ByteStream> gmbs=ByteStream::create();
      ByteStream &mbs=*gmbs;
      mbs.copy(*(bg44iff->get_bytestream()));
      bg44iff->close_chunk();  
      mbs.seek(0);
      if (mbs.readall((void*)&primary, sizeof(primary)) != sizeof(primary))
        G_THROW("djvumake: BG44 file is corrupted (cannot read primary header)\n");    
      if (primary.serial == 0)
        {
          if (mbs.readall((void*)&secondary, sizeof(secondary)) != sizeof(secondary))
            G_THROW("djvumake: BG44 file is corrupted (cannot read secondary header)\n");    
          int iw = (secondary.xhi<<8) + secondary.xlo;
          int ih = (secondary.yhi<<8) + secondary.ylo;
          int red;
          for (red=1; red<=12; red++)
            if (iw==(w+red-1)/red && ih==(h+red-1)/red)
              break;
          flag_contains_bg = red;
          if (red>12)
            DjVuPrintErrorUTF8("%s","djvumake: BG44 subsampling is not in [1..12] range\n");
        }
      mbs.seek(0);
      iff.put_chunk(ckid);
      iff.copy(mbs);
      iff.close_chunk();
      flag = 1;
    }
  if (!flag)
    DjVuPrintErrorUTF8("%s","djvumake: no more chunks in BG44 file\n");
}



// -- Forward declarations

void processForeground(const GPixmap* image, const JB2Image *mask,
                       GPixmap& subsampled_image, GBitmap& subsampled_mask);
void processBackground(const GPixmap* image, const JB2Image *mask,
                       GPixmap& subsampled_image, GBitmap& subsampled_mask);


// -- Create both foreground and background by masking and subsampling

void 
create_masksub_chunks(IFFByteStream &iff, const GURL &url)
{
  // Check and load pixmap file
  if (!g().stencil)
    G_THROW("The use of a raw ppm image requires a stencil");
  GP<ByteStream> gibs=ByteStream::create(url, "rb");
  ByteStream &ibs=*gibs;
  GP<GPixmap> graw_pm=GPixmap::create(ibs);
  GPixmap &raw_pm=*graw_pm;
  if ((int) g().stencil->get_width() != (int) raw_pm.columns())
    G_THROW("Stencil and raw image have different widths!");
  if ((int) g().stencil->get_height() != (int) raw_pm.rows())
    G_THROW("Stencil and raw image have different heights!");
  // Encode foreground
  {
    GP<GPixmap> gfg_img=GPixmap::create();
    GPixmap &fg_img=*gfg_img;
    GP<GBitmap> fg_mask=GBitmap::create();
    processForeground(&raw_pm, g().stencil, fg_img, *fg_mask);
    GP<IW44Image> fg_pm = IW44Image::create_encode(fg_img, fg_mask, IW44Image::CRCBfull);
    IWEncoderParms parms[8];
    iff.put_chunk("FG44");
    parms[0].slices = 100;
    fg_pm->encode_chunk(iff.get_bytestream(), parms[0]);
    iff.close_chunk();
  }
  // Encode backgound 
  {
    GP<GPixmap> gbg_img=GPixmap::create();
    GPixmap &bg_img=*gbg_img;
    GP<GBitmap> bg_mask=GBitmap::create();
    processBackground(&raw_pm, g().stencil, bg_img, *bg_mask);
    GP<IW44Image> bg_pm = IW44Image::create_encode(bg_img, bg_mask, IW44Image::CRCBnormal);
    IWEncoderParms parms[4];
    parms[0].bytes = 10000;
    parms[0].slices = 74;
    iff.put_chunk("BG44");
    bg_pm->encode_chunk(iff.get_bytestream(), parms[0]);
    iff.close_chunk();
    parms[1].slices = 84;
    iff.put_chunk("BG44");
    bg_pm->encode_chunk(iff.get_bytestream(), parms[1]);
    iff.close_chunk();
    parms[2].slices = 90;
    iff.put_chunk("BG44");
    bg_pm->encode_chunk(iff.get_bytestream(), parms[2]);
    iff.close_chunk();
    parms[3].slices = 97;
    iff.put_chunk("BG44");
    bg_pm->encode_chunk(iff.get_bytestream(), parms[3]);
    iff.close_chunk();
  }
}



const char *
parse_color_name(const char *s, char *rgb)
{
  static struct { 
    const char *name; 
    unsigned char r, g, b; 
  } stdcols[] = { 
    {"aqua",    0x00, 0xFF, 0xFF},
    {"black",   0x00, 0x00, 0x00},
    {"blue",    0x00, 0x00, 0xFF},
    {"fuchsia", 0xFF, 0x00, 0xFF},
    {"gray",    0x80, 0x80, 0x80},
    {"green",   0x00, 0x80, 0x00},
    {"lime",    0x00, 0xFF, 0x00},
    {"maroon",  0x80, 0x00, 0x00},
    {"navy",    0x00, 0x00, 0x80},
    {"olive",   0x80, 0x80, 0x00},
    {"purple",  0x80, 0x00, 0x80},
    {"red",     0xFF, 0x00, 0x00},
    {"silver",  0xC0, 0xC0, 0xC0},
    {"teal",    0x00, 0x80, 0x80},
    {"white",   0xFF, 0xFF, 0xFF},
    {"yellow",  0xFF, 0xFF, 0x00},
    {0}
  };
  // potential color names
  int len = 0;
  while (s[len] && s[len]!=':' && s[len]!='#')
    len += 1;
  GUTF8String name(s, len);
  name = name.downcase();
  for (int i=0; stdcols[i].name; i++)
    if (name == stdcols[i].name)
      {
        rgb[0] = stdcols[i].r;
        rgb[1] = stdcols[i].g;
        rgb[2] = stdcols[i].b;
        return s+len;
      }
  // potential hex specifications
  unsigned int r,g,b;
  if (sscanf(s,"%2x%2x%2x",&r,&g,&b) == 3)
    {
      rgb[0] = r;
      rgb[1] = g;
      rgb[2] = b;
      return s+6;
    }
  G_THROW("Unrecognized color name in FGbz chunk specification");
  return 0; // win
}


void
parse_color_zones(const char *s)
{
  bool fullpage = false;
  int zones = 0;
  g().colorzones.empty();
  g().colorpalette = ByteStream::create();
  // zones
  while (s[0] == '#')
    {
      char rgb[3];
      GRect rect;
      s = parse_color_name(s+1, rgb);
      if (s[0] == ':')
        {
          int c[4];
          for (int i=0; i<4; i++)
            {
              char *e = 0;
              c[i] = strtol(s+1, &e, 10);
              if (e <= s || (i>=2 && c[i]<0) || (i<3 && e[0]!=','))
                G_THROW("Invalid coordinates in FGbz chunk specification");
              s = e;
            }
          rect = GRect(c[0],c[1],c[2],c[3]);
        }
      if (rect.isempty())
        fullpage = true;
      g().colorpalette->writall(rgb, 3);
      g().colorzones.touch(zones);
      g().colorzones[zones] = rect;
      zones++;
    }
  if (s[0])
    G_THROW("Syntax error in FGbz chunk specification");
  // add extra black palette entry
  if (! fullpage)
    {
      char rgb[3] = {0,0,0};
      g().colorpalette->writall(rgb, 3);
    }
}



// -- Main

int
main(int argc, char **argv)
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);
  G_TRY
    {
      // Print usage when called without enough arguments
      if (argc <= 2)
        usage();
      // Open djvu file
      remove(dargv[1]);
      GP<IFFByteStream> giff = 
        IFFByteStream::create(ByteStream::create(GURL::Filename::UTF8(dargv[1]),"wb"));
      IFFByteStream &iff=*giff;
      // Create header
      iff.put_chunk("FORM:DJVU", 1);
      // Check if shared dicts are present
      check_for_shared_dict(dargv);
      // Create information chunk
      create_info_chunk(iff, dargv);
      // Parse all arguments
      for (int i=2; i<argc; i++)
        {
          if (!dargv[i].cmp("INFO=",5))
            {
              if (i>2)
                DjVuPrintErrorUTF8("%s","djvumake: 'INFO' chunk should appear first (ignored)\n");
            }
          else if (!dargv[i].cmp("Sjbz=",5))
            {
              if (flag_contains_stencil)
                DjVuPrintErrorUTF8("%s","djvumake: duplicate stencil chunk\n");
              create_jb2_chunk(iff, "Sjbz", GURL::Filename::UTF8(5+(const char *)dargv[i]));
              flag_contains_stencil = 1;
              if (flag_fg_needs_palette && blit_count >= 0)
                create_fgbz_chunk(iff);
              flag_fg_needs_palette = 0;
            }
          else if (!dargv[i].cmp("Smmr=",5))
            {
              create_mmr_chunk(iff, "Smmr", 
			       GURL::Filename::UTF8(5+(const char *)dargv[i]));
              if (flag_contains_stencil)
                DjVuPrintErrorUTF8("%s","djvumake: duplicate stencil chunk\n");
              flag_contains_stencil = 1;
            }
          else if (!dargv[i].cmp("FGbz=",5))
            {
              const char *c = 5 + (const char*)dargv[i];
              if (flag_contains_fg)
                DjVuPrintErrorUTF8("%s","djvumake: duplicate 'FGxx' chunk\n");
              if (c[0] != '#')
                {
                  create_raw_chunk(iff, "FGbz", GURL::Filename::UTF8(c));
                }
              else
                {
                  parse_color_zones(c);
                  if (flag_contains_stencil && blit_count >= 0)
                    create_fgbz_chunk(iff);
                  else
                    flag_fg_needs_palette = 1;
                }
              flag_contains_fg = 1;
            }
          else if (!dargv[i].cmp("FG44=",5))
            {
              if (flag_contains_fg)
                DjVuPrintErrorUTF8("%s","djvumake: duplicate 'FGxx' chunk\n");
              create_fg44_chunk(iff, "FG44", 
				GURL::Filename::UTF8(5+(const char *)dargv[i]));
            }
          else if (!dargv[i].cmp("BG44=",5))
            {
              create_bg44_chunk(iff, "BG44", 5+(const char *)dargv[i]);
            }
          else if (!dargv[i].cmp("BGjp=",5) ||
                   !dargv[i].cmp("BG2k=",5)  )
            {
              if (flag_contains_bg)
                DjVuPrintErrorUTF8("%s","djvumake: Duplicate BGxx chunk\n");
              GUTF8String chkid = dargv[i].substr(0,4);
              create_raw_chunk(iff, chkid, GURL::Filename::UTF8(5+(const char *)dargv[i]));
              flag_contains_bg = 1;
            }
          else if (!dargv[i].cmp("FGjp=",5) ||  !dargv[i].cmp("FG2k=",5))
            {
              if (flag_contains_fg)
                DjVuPrintErrorUTF8("%s","djvumake: duplicate 'FGxx' chunk\n");
              GUTF8String chkid = dargv[i].substr(0,4);
              create_raw_chunk(iff, chkid, GURL::Filename::UTF8(5+(const char *)dargv[i]));
              flag_contains_fg = 1;
            }
          else if (!dargv[i].cmp("INCL=",5))
            {
              create_incl_chunk(iff, "INCL", (const char *)GUTF8String(dargv[i].substr(5,-1)));
              flag_contains_incl = 1;
            }
          else if (!dargv[i].cmp("PPM=",4))
            {
              if (flag_contains_bg || flag_contains_fg)
                DjVuPrintErrorUTF8("%s","djvumake: Duplicate 'FGxx' or 'BGxx' chunk\n");
              create_masksub_chunks(iff, GURL::Filename::UTF8(4+(const char *)dargv[i]));
              flag_contains_bg = 1;
              flag_contains_fg = 1;
            }
          else if (dargv[i].length() > 4 && dargv[i][4] == '=')
            {
              GNativeString chkid = dargv[i].substr(0,4);
              if (chkid != "TXTz" && chkid != "TXTa" 
                  && chkid != "ANTz" && chkid != "ANTa"
                  && chkid != "Djbz" )
                DjVuPrintErrorUTF8("djvumake: creating chunk of unknown type ``%s''.\n",
                                   (const char*)chkid);
              create_raw_chunk(iff, chkid, GURL::Filename::UTF8(5+(const char *)dargv[i]));
            }
          else 
            {
              DjVuPrintErrorUTF8("djvumake: illegal argument : ``%s'' (ignored)\n", 
                                 (const char *)dargv[i]);
            }
        }
      // Common cases for missing chunks
      if (flag_contains_stencil)
        {
          if (flag_contains_bg && ! flag_contains_fg)
            {  
              DjVuPrintErrorUTF8("%s","djvumake: generating black FGbz chunk\n");
              g().colorzones.empty();
              g().colorpalette = ByteStream::create();
              char rgb[3] = {0,0,0};
              g().colorpalette->writall(rgb, 3);
              create_fgbz_chunk(iff);
              flag_contains_fg = 1;
            }
          if (flag_contains_fg && !flag_contains_bg)
            {
              DjVuPrintErrorUTF8("%s","djvumake: generating white BG44 chunk\n");
              GPixel bgcolor = GPixel::WHITE;
              GP<GPixmap> inputsub=GPixmap::create((h+11)/12, (w+11)/12, &bgcolor);
              GP<IW44Image> iw = IW44Image::create_encode(*inputsub, 0, IW44Image::CRCBnone);
              IWEncoderParms iwparms;
              iff.put_chunk("BG44");
              iwparms.slices = 97;
              iw->encode_chunk(iff.get_bytestream(), iwparms);
              iff.close_chunk();
              flag_contains_bg = 1;
            }
        }
      // Close
      iff.close_chunk();
      // Sanity checks
      if (flag_contains_stencil)
        {
          // Compound or Bilevel
          if (flag_contains_bg && ! flag_contains_fg)
            DjVuPrintErrorUTF8("%s","djvumake: djvu file contains a BGxx chunk but no FGxx chunk\n");
          if (flag_contains_fg && ! flag_contains_bg)
            DjVuPrintErrorUTF8("%s","djvumake: djvu file contains a FGxx chunk but no BGxx chunk\n");
        }
      else if (flag_contains_bg)
        {
          // Photo DjVu Image
          if (flag_contains_bg!=1)
            DjVuPrintErrorUTF8("%s","djvumake: photo djvu image has subsampled BGxx chunk\n"); 
          if (flag_fg_needs_palette)
            DjVuPrintErrorUTF8("%s","djvumake: could not generate FGbz chunk, as stencil is not available\n");
          else if (flag_contains_fg)
            DjVuPrintErrorUTF8("%s","djvumake: photo djvu file contains FGxx chunk\n");            
        }
      else
        DjVuPrintErrorUTF8("%s","djvumake: djvu file contains neither Sxxx nor BGxx chunks\n");
    }
  G_CATCH(ex)
    {
      remove(dargv[1]);
      ex.perror();
      exit(1);
    }
  G_ENDCATCH;
  return 0;
}


////////////////////////////////////////
// MASKING AND SUBSAMPLING
////////////////////////////////////////


// -- Returns a dilated version of a bitmap with the same size

static GP<GBitmap> 
dilate8(const GBitmap *p_bm)
{
  const GBitmap& bm = *p_bm;
  GP<GBitmap> p_newbm = GBitmap::create(bm.rows(),bm.columns(),1);
  GBitmap& newbm = *p_newbm;
  int nrows = bm.rows();
  int ncols = bm.columns();
  for(int y=0; y<nrows; y++)
    {
      const unsigned char *bmrow = bm[y];
      unsigned char *nbmprow = (y-1>=0) ? newbm[y-1] : 0;
      unsigned char *nbmrow = newbm[y];
      unsigned char *nbmnrow = (y+1<nrows) ? newbm[y+1] : 0;
      for(int x=0; x<ncols; x++)
        {
          if(bmrow[x]) 
            {
              // Set all the 8-neighborhood to black
              if (nbmprow)
                {
                  nbmprow[x-1]=1;
                  nbmprow[x]=1;
                  nbmprow[x+1]=1;
                }
              nbmrow[x-1]=1;
              nbmrow[x]=1;
              nbmrow[x+1]=1;
              if (nbmnrow)
                {
                  nbmnrow[x-1]=1;
                  nbmnrow[x]=1;
                  nbmnrow[x+1]=1;
                }
            }
        }
    }
  return p_newbm;
}

// -- Returns a smaller eroded version of a bitmap

static GP<GBitmap> 
erode8(const GBitmap *p_bm)
{
  const GBitmap& bm = *p_bm;
  int newnrows = bm.rows()-2;
  int newncolumns = bm.columns()-2;
  if(newnrows<=0 || newncolumns<=0) // then return an empty GBitmap 
    return GBitmap::create();
  GP<GBitmap> p_newbm = GBitmap::create(newnrows,newncolumns); 
  GBitmap& newbm = *p_newbm;
  for(int y=0; y<newnrows; y++)
    {
      for(int x=0; x<newncolumns; x++)
        {
          // Check if there's a white pixel in the 8-neighborhood
          if(   !( bm[y  ][x] && bm[y  ][x+1] && bm[y  ][x+2]
                && bm[y+1][x] && bm[y+1][x+1] && bm[y+1][x+2]
                && bm[y+2][x] && bm[y+2][x+1] && bm[y+2][x+2]))
            newbm[y][x] = 0; // then set current to white
          else
            newbm[y][x] = 1; // else set current to black
        }
    }
  return p_newbm;
}


// -- Returns a smaller eroded version of a jb2image

GP<JB2Image> 
erode8(const JB2Image *im)
{
  int i;
  GP<JB2Image> newim = JB2Image::create();
  newim->set_dimension(im->get_width(),im->get_height());
  for(i=0; i<im->get_shape_count(); i++)
    {
      const JB2Shape &shape = im->get_shape(i);
      JB2Shape newshape;
      newshape.parent = shape.parent;
      if (shape.bits) 
        newshape.bits = erode8(shape.bits);
      else
        newshape.bits = 0;
      newim->add_shape(newshape);
    }
  for(i=0; i<im->get_blit_count(); i++)
    {
      const JB2Blit* blit = im->get_blit(i);
      JB2Blit newblit;
      newblit.bottom = blit->bottom + 1;
      newblit.left = blit->left + 1;
      newblit.shapeno = blit->shapeno;
      newim->add_blit(newblit);
    }
  return newim;
}


// Subsamples only the pixels of <image> that are not masked (<mask>).  This
// call resizes and fills the resulting <subsampled_image> and
// <subsampled_mask>.  Their dimension is the dimension of the original
// <image> divided by <gridwidth> and rounded to the superior integer.  For
// each square grid (gridwidth times gridwidth) of the subsampling mesh that
// contains at least <minpixels> non-masked pixels, their value is averaged to
// give the value of the corresponding <subsampled_image> pixel, and the
// <subsampled_mask> is cleared at this position.  If <inverted_mask> is true,
// then pixels are considered to be masked when mask==0
 

static void
maskedSubsample(const GPixmap* img,
                const GBitmap *p_mask,
                GPixmap& subsampled_image,
                GBitmap& subsampled_mask,
                int gridwidth, int inverted_mask, 
                int minpixels=1
                )
{
  const GPixmap& image= *img;
  const GBitmap& mask = *p_mask;
  int imageheight = image.rows();
  int imagewidth = image.columns();
  // compute the size of the resulting subsampled image
  int subheight = imageheight/gridwidth;
  if(imageheight%gridwidth)
    subheight++;
  int subwidth = imagewidth/gridwidth;
  if(imagewidth%gridwidth)
    subwidth++;
  // set the sizes unless in incremental mode
  subsampled_image.init(subheight, subwidth);
  subsampled_mask.init(subheight, subwidth);
  // go subsampling
  int row, col;  // row and col in the subsampled image
  int posx, posxend, posy, posyend; // corresponding square in the original image
  for(row=0, posy=0; row<subheight; row++, posy+=gridwidth)
    {
      GPixel* subsampled_image_row = subsampled_image[row]; // row row of subsampled image
      unsigned char* subsampled_mask_row = subsampled_mask[row]; // row row of subsampled mask
      posyend = posy+gridwidth;
      if(posyend>imageheight)
        posyend = imageheight;
      for(col=0, posx=0; col<subwidth; col++, posx+=gridwidth) 
        {
          posxend = posx+gridwidth;
          if(posxend>imagewidth)
            posxend = imagewidth;
          int count = 0;
          int r = 0;
          int g = 0;
          int b = 0;
          for(int y=posy; y<posyend; y++)
            {
              const unsigned char* mask_y = mask[y]; // Row y of the mask
              for(int x=posx; x<posxend; x++)
                {
                  unsigned char masked = (inverted_mask ? !mask_y[x] :mask_y[x]);
                  if(!masked)
                    {
                      GPixel p = image[y][x];
                      r += p.r;
                      g += p.g;
                      b += p.b;
                      count ++;
                    }
                }
            }
          /* minpixels pixels are enough to give the color */
          /* so set it, and do not mask this point */
          if(count >= minpixels)   
            {
              GPixel p;
              p.r = r/count;
              p.g = g/count;
              p.b = b/count;
              subsampled_image_row[col] = p;
              subsampled_mask_row[col] = 0;
            } 
          else /* make it bright red and masked */ 
            {
              subsampled_image_row[col] = GPixel::RED;
              subsampled_mask_row[col] = 1;
            }
        }
    }
}


// -- Computes foreground image and mask

void 
processForeground(const GPixmap* image, const JB2Image *mask,
                  GPixmap& subsampled_image, GBitmap& subsampled_mask)
{
  GP<JB2Image> eroded_mask = erode8(mask);
  maskedSubsample(image, eroded_mask->get_bitmap(), 
                  subsampled_image, subsampled_mask, 
                  6, 1);   // foreground subsample is 6 (300dpi->50dpi)
}


// -- Computes background image and mask

void 
processBackground(const GPixmap* image, const JB2Image *mask,
                  GPixmap& subsampled_image, GBitmap& subsampled_mask)
{
  GP<GBitmap> b = mask->get_bitmap();
  b = dilate8(b);
  b = dilate8(b);
  maskedSubsample(image, b, subsampled_image, subsampled_mask, 3, 0);
}


