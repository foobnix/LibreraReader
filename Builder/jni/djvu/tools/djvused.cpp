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


#include "IW44Image.h"
#include "GOS.h"
#include "GString.h"
#include "DjVuDocEditor.h"
#include "DjVuDumpHelper.h"
#include "DjVuMessageLite.h"
#include "BSByteStream.h"
#include "DjVuText.h"
#include "DjVuAnno.h"
#include "DjVuInfo.h"
#include "IFFByteStream.h"
#include "DataPool.h"
#include "DjVuPort.h"
#include "DjVuFile.h"
#include "DjVmNav.h"
#include "common.h"

static bool modified = false;
static bool verbose = false;
static bool save = false;
static bool nosave = false;
static bool utf8 = false;

static unsigned char utf8bom[] = { 0xef, 0xbb, 0xbf };

struct DJVUSEDGlobal 
{
  // Globals that need static initialization
  // are grouped here to work around broken compilers.
  GUTF8String djvufile;
  GP<ByteStream> cmdbs;
  GP<DjVuDocEditor> doc;
  GPList<DjVmDir::File> selected;
  GP<DjVuFile> file;
  GUTF8String fileid;
};

static DJVUSEDGlobal& g(void)
{
  static DJVUSEDGlobal g;
  return g;
}

static GUTF8String 
ToNative(GUTF8String s)
{
  if (utf8)
    return s;
  // fake the damn GUTF8/GNative type check
  GNativeString n = s;
  return GUTF8String((const char*)n);
}



// --------------------------------------------------
// PARSING BYTESTREAM
// --------------------------------------------------

// -- A bytestream that performs buffering and 
//    offers a stdio-like interface for reading files.

class ParsingByteStream : public ByteStream 
{
private:
  enum { bufsize=512 };
  const GP<ByteStream> &gbs;
  ByteStream &bs;
  unsigned char buffer[bufsize];
  int  bufpos;
  int  bufend;
  bool goteof;
  ParsingByteStream(const GP<ByteStream> &gbs);
  int getbom(int c);
public:
  static GP<ParsingByteStream> create(const GP<ByteStream> &gbs) 
  { return new ParsingByteStream(gbs); }
  size_t read(void *buffer, size_t size);
  size_t write(const void *buffer, size_t size);
  long int tell() const;
  int eof();
  int unget(int c);
  inline int get();
  int get_spaces(bool skipseparator=false);
  GUTF8String get_token(bool skipseparator=false, bool compat=false);
  const char *get_error_context(int c=EOF);
};

ParsingByteStream::ParsingByteStream(const GP<ByteStream> &xgbs)
  : gbs(xgbs),bs(*gbs), bufpos(1), bufend(1), goteof(false)
{ 
}

int 
ParsingByteStream::eof() // aka. feof
{
  if (bufpos < bufend) 
    return false;
  if (goteof)
    return true;
  bufend = bufpos = 1;
  while (bs.read(buffer+bufend,1) && ++bufend<(int)bufsize)
    if (buffer[bufend-1]=='\r' || buffer[bufend-1]=='\n')
      break;
  if (bufend == bufpos)
    goteof = true;
  return goteof;
}

size_t 
ParsingByteStream::read(void *buf, size_t size)
{
  if (size < 1)
    return 0;
  if (bufend == bufpos) 
    {
      if (size >= bufsize)
        return bs.read(buf, size);
      if (eof())
        return 0;
    }
  if (bufpos + (int)size > bufend)
    size = bufend - bufpos;
  memcpy(buf, buffer+bufpos, size);
  bufpos += size;
  return size;
}

size_t 
ParsingByteStream::write(const void *, size_t )
{
  G_THROW("Cannot write() into a ParsingByteStream");
  return 0;
}

long int
ParsingByteStream::tell() const
{ 
  G_THROW("Cannot tell() a ParsingByteStream");
  return 0;
}

int 
ParsingByteStream::getbom(int c)
{
  int i = 0;
  while (c == utf8bom[i++])
    {
      if (i >= 3)
        i = 0;
      if (bufpos < bufend || !eof())
        c = buffer[bufpos++];
    }
  while (--i > 0)
    {
      unget(c);
      c = utf8bom[i-1];
    }
  return c;
}

inline int 
ParsingByteStream::get() // like getc() skipping bom.
{
  int c = EOF;
  if (bufpos < bufend || !eof())
    c = buffer[bufpos++];
  if (c == utf8bom[0])
    return getbom(c);
  return c;
}


int  
ParsingByteStream::unget(int c) // like ungetc()
{
  if (bufpos > 0 && c != EOF) 
    return buffer[--bufpos] = (unsigned char)c;
  return EOF;
}

int
ParsingByteStream::get_spaces(bool skipseparator)
{
   int c = get();
   while (c==' ' || c=='\t' || c=='\r' 
          || c=='\n' || c=='#' || c==';' )
     {
       if (c == '#')
         do { c=get(); } while (c!=EOF && c!='\n' && c!='\r');
       if (!skipseparator && (c=='\n' || c=='\r' || c==';'))
         break;
       c = get();
     }
   return c;
}
  
const char *
ParsingByteStream::get_error_context(int c)
{
  static char buffer[22];
  unget(c);
  int len = read((void*)buffer, sizeof(buffer)-1);
  buffer[(len>0)?len:0] = 0;
  for (int i=0; i<len; i++)
    if (buffer[i]=='\n')
      buffer[i] = 0;
  return buffer;
}

GUTF8String
ParsingByteStream::get_token(bool skipseparator, bool compat)
{
  GUTF8String str;
  int c = get_spaces(skipseparator);
  if (c == EOF)
    {
      return str;
    }
  if (!skipseparator && (c=='\n' || c=='\r' || c==';'))
    {
      unget(c);
      return str;
    }
  if (c != '\"' && c != '\'') 
    {
      while (c!=' ' && c!='\t' && c!='\r' && c!=';'
             && c!='\n' && c!='#' && c!=EOF)
        {
          str += c;
          c = get();
        }
      unget(c);
    }
  else 
    {
      int delim = c;
      c = get();
      while (c != delim && c!=EOF) 
        {
          if (c == '\\') 
            {
              c = get();
              if (compat && c!='\"')
                {
                  str += '\\';
                }
              else if (c>='0' && c<='7')
                {
                  int x = 0;
                  { // extra nesting for windows
                    for (int i=0; i<3 && c>='0' && c<='7'; i++) 
                    {
                      x = x * 8 + c - '0';
                      c = get();
                    }
                  }
                  unget(c);
                  c = x;
                }
              else 
                {
                  const char *tr1 = "tnrbfva";
                  const char *tr2 = "\t\n\r\b\f\013\007";
                  { // extra nesting for windows
                    for (int i=0; tr1[i]; i++)
                    {
                      if (c == tr1[i])
                        c = tr2[i];
                    }
                  }
                }
            }
          if (c != EOF)
            str += c;
          c = get();
        }
    }
  return str;
}


// --------------------------------------------------
// COMMANDS
// --------------------------------------------------


static void
vprint(const char *fmt, ... )
#ifdef __GNUC__
  __attribute__ ((format (printf, 1, 2)));
static void
vprint(const char *fmt, ... )
#endif
{
  if (verbose)
    {
      GUTF8String msg("");
      va_list args;
      va_start(args, fmt);
      msg.vformat(fmt, args);
      fprintf(stderr,"djvused: %s\n", (const char*)ToNative(msg));
    }
}

static void
verror(const char *fmt, ... )
#ifdef __GNUC__
  __attribute__ ((format (printf, 1, 2)));
static void
verror(const char *fmt, ... )
#endif
{
  GUTF8String msg;
  va_list args;
  va_start(args, fmt);
  msg.vformat(fmt, args);
  G_THROW((const char*)ToNative(msg));
}

static void
get_data_from_file(const char *cmd, ParsingByteStream &pbs, ByteStream &out)
{
  GUTF8String fname = pbs.get_token();

  if (! fname)
    {
      vprint("%s: enter data and terminate with a period on a single line", cmd);
      int c = pbs.get_spaces(true);
      pbs.unget(c);
      char skip[4];
      char term0[4] = "\n.\n";
      char term1[4] = "\r.\r";
      char *s = skip;
      int state = 1;
      while (state < 3) 
        {
          c = pbs.get();
          if (c == EOF)
            break;
          if ( c == term0[state] || c == term1[state] )
            {
              state += 1;
              *s++ = c;
            }
          else
            {
              { // extra nesting for windows
                for (char *m=skip; m<s; m++)
                  out.write8(*m);
              }
              s = skip;
              state = 0;
              if (c == '\n')
                pbs.unget(c);
              else if (c != EOF)
                out.write8(c);
            }
        }
      pbs.unget(c);
    }
  else
    {
      GP<ByteStream> in=ByteStream::create(GURL::Filename::UTF8(fname),"rb");
      out.copy(*in);
    }
}

static bool
char_unquoted(unsigned char c, bool eightbit)
{
  if (eightbit && c>=0x80)
    return true;
  if (c==0x7f || c=='\"' || c=='\\')
    return false;
  if (c>=0x20 && c<0x7f)
    return true;
  return false;
}

static void
print_c_string(const char *data, int length, ByteStream &out, bool eightbit)
{
  out.write("\"",1);
  while (*data && length>0) 
    {
      int span = 0;
      while (span<length && char_unquoted(data[span],eightbit))
        span++;
      if (span > 0) 
        {
          out.write(data, span);
          data += span;
          length -= span;
        }
      else
        {
          char buf[5];
          static const char *tr1 = "\"\\tnrbf";
          static const char *tr2 = "\"\\\t\n\r\b\f";
          sprintf(buf,"\\%03o", (int)(((unsigned char*)data)[0]));
          { // extra nesting for windows
            for (int i=0; tr2[i]; i++)
              if (*(char*)data == tr2[i])
                buf[1] = tr1[i];
          }
          if (buf[1]<'0' || buf[1]>'3')
            buf[2] = 0;
          out.write(buf, ((buf[2]) ? 4 : 2));
          data += 1;
          length -= 1;
        }
    }
  out.write("\"",1);
}

void
command_ls(ParsingByteStream &)
{
  int pagenum = 0;
  GPList<DjVmDir::File> lst = g().doc->get_djvm_dir()->get_files_list();
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p) 
    {
      GP<DjVmDir::File> f = lst[p];
      if (f->is_page())
        fprintf(stdout,"%4d P ", ++pagenum);
      else if (f->is_include())
        fprintf(stdout,"     I ");
      else if (f->is_thumbnails())
        continue;
      else if (f->is_shared_anno())
        fprintf(stdout,"     A ");
      else
        fprintf(stdout,"     ? ");
      GUTF8String id = f->get_load_name();
      fprintf(stdout,"%8d  %s", f->size, (const char*)ToNative(id));
      GUTF8String name = f->get_save_name();
      if (name != id)
        fprintf(stdout," F=%s", (const char*)ToNative(name));
      GUTF8String title = f->get_title();
      if (title != id && f->is_page())
        fprintf(stdout," T=%s", (const char*)ToNative(title));
      fprintf(stdout,"\n");
    }
  }
  if (g().doc->get_thumbnails_num() == g().doc->get_pages_num())
    fprintf(stdout,"     T %8s  %s\n", "", "<thumbnails>");
}

void
command_n(ParsingByteStream &)
{
  int pagenum = 0;
  GPList<DjVmDir::File> lst = g().doc->get_djvm_dir()->get_files_list();
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p) 
    {
      GP<DjVmDir::File> f = lst[p];
      if (f->is_page())
        ++pagenum;
    }
  }
  fprintf(stdout,"%d\n", pagenum); 
}

void
command_dump(ParsingByteStream &)
{
  GP<DataPool> pool;
  // Need to be modified to handle "selected" list.
  if (g().file)
    pool = g().file->get_djvu_data(false, false);
  else
    pool = g().doc->get_init_data_pool();
  DjVuDumpHelper helper;
  GP<ByteStream> bs = helper.dump(pool);
  size_t size = bs->size();
  GUTF8String str;
  char *buf = str.getbuf(size);
  bs->seek(0);
  bs->readall(buf, size);
  GUTF8String ns = ToNative(str);
  GP<ByteStream> obs=ByteStream::create("w");
  obs->writall((const char*)ns, ns.length());
}

static void
print_size(const GP<DjVuFile> &file)
{
  GP<DjVuInfo> info = file->info;
  if (! info)
    {
      const GP<ByteStream> pbs(file->get_djvu_bytestream(false, false));
      const GP<IFFByteStream> iff(IFFByteStream::create(pbs));
      GUTF8String chkid;
      if (! iff->get_chunk(chkid))
        verror("Selected file contains no data");
      if (chkid == "FORM:DJVU")
        {
          while (iff->get_chunk(chkid) && chkid!="INFO")
            iff->close_chunk();
          if (chkid == "INFO")
            { 
              info = DjVuInfo::create();
              info->decode(*iff->get_bytestream());
            }
        }
      else if (chkid == "FORM:BM44" || chkid == "FORM:PM44")
        {
          while (iff->get_chunk(chkid) && chkid!="BM44" && chkid!="PM44")
            iff->close_chunk();
          if (chkid=="BM44" || chkid=="PM44")
            {
              GP<IW44Image> junk=IW44Image::create_decode(IW44Image::COLOR);
              junk->decode_chunk(iff->get_bytestream());
              fprintf(stdout,"width=%d height=%d\n", 
                      junk->get_width(), junk->get_height());
            }
        }
    }
  if (info)
    {
      fprintf(stdout,"width=%d height=%d", info->width, info->height);
      if (info->orientation)
        fprintf(stdout, " rotation=%d", info->orientation);
      fprintf(stdout,"\n");
    }
}


void
command_size(ParsingByteStream &)
{
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
    {
      if (lst[p]->is_page())
        {
          GUTF8String fileid = g().doc->page_to_id(lst[p]->get_page_num());
          const GP<DjVuFile> f = g().doc->get_djvu_file(fileid);
          print_size(f);
        }
    }
  }
}

static void
select_all(void)
{
  g().file = 0;
  g().fileid = "";
  g().selected = g().doc->get_djvm_dir()->get_files_list();
}

static void
select_clear(void)
{
  g().file = 0;
  g().fileid = "<all>";
  g().selected.empty();
}

static void
select_add(GP<DjVmDir::File> frec)
{
  GPosition selp = g().selected;
  GPList<DjVmDir::File> all = g().doc->get_djvm_dir()->get_files_list();
  GPosition allp = all;
  while (allp)
    {
      if (all[allp] == frec)
        break;
      if ( selp && all[allp] == g().selected[selp])
        ++ selp;
      ++ allp;
    }
  if (allp && (!selp || all[allp] != g().selected[selp]))
    {
      g().selected.insert_before(selp, frec);
      if (! g().file)
        {
          g().fileid = frec->get_load_name();
          g().file = g().doc->get_djvu_file(g().fileid);
        }
      else
        {
          g().fileid = "<multiple>";
          g().file = 0;
        }
    }
}

void
command_select(ParsingByteStream &pbs)
{
  GUTF8String pagid = pbs.get_token();
  // Case of NULL
  if (pagid == "") 
    {
      select_all();
      vprint("select: selecting entire document");
      return;
    } 
  // Case of a single page number
  if (pagid.is_int())
    {
      int pageno = atoi(pagid);
      GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->page_to_file(pageno-1);
      if (!frec)
        verror("page \"%d\" not found", pageno);
      select_clear();
      select_add(frec);
      vprint("select: selecting \"%s\"", (const char*)ToNative(g().fileid));
      return;
    }
  // Case of a single file id
  GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->id_to_file(pagid);
  if (!frec)
    frec = g().doc->get_djvm_dir()->name_to_file(pagid);
  if (!frec)
    frec = g().doc->get_djvm_dir()->title_to_file(pagid);
  if (!frec)
    verror("page \"%s\" not found", (const char*)ToNative(pagid));
  select_clear();
  select_add(frec);
  vprint("select: selecting \"%s\"", (const char*)ToNative(g().fileid));
}  

void
command_select_shared_ant(ParsingByteStream &)
{
  GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
  if (! frec)
    verror("select-shared-ant: no shared annotation file"); 
  select_clear();
  select_add(frec);
  vprint("select-shared-ant: selecting shared annotation");
}

void
command_create_shared_ant(ParsingByteStream &)
{
  GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
  if (! frec)
    {
      vprint("create-shared-ant: creating shared annotation file");
      g().doc->create_shared_anno_file();
      frec = g().doc->get_djvm_dir()->get_shared_anno_file();
      if (!frec) G_THROW("internal error");
    }
  select_clear();
  select_add(frec);
  vprint("select-shared-ant: selecting shared annotation");
}

void
command_showsel(ParsingByteStream &)
{
  int pagenum = 0;
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p) 
    {
      GP<DjVmDir::File> f = lst[p];
      if (f->is_page())
        fprintf(stdout,"%4d P ", ++pagenum);
      else if (f->is_include())
        fprintf(stdout,"     I ");
      else if (f->is_thumbnails())
        fprintf(stdout,"     T ");
      else if (f->is_shared_anno())
        fprintf(stdout,"     A ");
      else
        fprintf(stdout,"     ? ");
      GUTF8String id = f->get_load_name();
      fprintf(stdout,"%8d  %s", f->size, (const char*)ToNative(id));
      GUTF8String name = f->get_save_name();
      if (name != id)
        fprintf(stdout," F=%s", (const char*)ToNative(name));
      GUTF8String title = f->get_title();
      if (title != id && f->is_page())
        fprintf(stdout," T=%s", (const char*)ToNative(title));
      fprintf(stdout,"\n");
    }
  }
  if (g().doc->get_thumbnails_num() == g().doc->get_pages_num())
    fprintf(stdout,"     T %8s  %s\n", "", "<thumbnails>");
}

void
command_set_page_title(ParsingByteStream &pbs)
{
  if (! g().file)
    verror("must select a single page first");
  GUTF8String fname = pbs.get_token();
  if (! fname)
    verror("must provide a name");
  GPList<DjVmDir::File> &lst = g().selected;
  GPosition pos = lst;
  if (! lst[pos]->is_page())
    verror("component file is not a page");
  g().doc->set_file_title(g().fileid, fname);
  vprint("set-page-title: modified \"%s\"", (const char*)ToNative(g().fileid));
  modified = true;
}

GP<DjVuInfo> decode_info(GP<DjVuFile> file)
{
  GP<DjVuInfo> info = file->info;
  if (! info)
    {
      const GP<ByteStream> pbs(file->get_djvu_bytestream(false, false));
      const GP<IFFByteStream> iff(IFFByteStream::create(pbs));
      GUTF8String chkid;
      if (! iff->get_chunk(chkid))
        return 0;
      if (chkid == "FORM:DJVU")
        {
          while (iff->get_chunk(chkid) && chkid!="INFO")
            iff->close_chunk();
          if (chkid == "INFO")
            {
              info = DjVuInfo::create();
              info->decode(*iff->get_bytestream());
            }
        }
      file->info = info;
    }
  return info;
}

bool
set_rotation(GP<DjVuFile> file, int rot, bool relative)
{
  // decode info
  GP<DjVuInfo> info = decode_info(file);
  if (! info)
    return false;
  if (relative)
    rot += info->orientation;
  info->orientation = rot & 3;
  file->set_modified(true);
  modified = true;
  return true;
}

void
command_set_rotation(ParsingByteStream &pbs)
{
  GUTF8String rot = pbs.get_token();
  if (! rot.is_int())
    verror("usage: set-rotation [+-]<rot>");
  int rotation = rot.toInt();
  bool relative = (rot[0]=='+' || rot[0]=='-');
  if (! relative)
    if (rotation < 0 || rotation > 3)
      verror("absolute rotation must be in range 0..3");
  int rcount = 0;
  if (g().file)
    {
      GUTF8String id = g().fileid;
      if (set_rotation(g().file, rotation, relative))
        rcount += 1;
    }
  else
    {
      GPList<DjVmDir::File> &lst = g().selected;
      for (GPosition p=lst; p; ++p)
        {
          GUTF8String id = lst[p]->get_load_name();
          const GP<DjVuFile> f(g().doc->get_djvu_file(id));
          if (set_rotation(f, rotation, relative))
            rcount += 1;
        }
    }
  vprint("rotated %d pages", rcount);
}

bool
set_dpi(GP<DjVuFile> file, int dpi)
{
  // decode info
  GP<DjVuInfo> info = decode_info(file);
  if (! info)
    return false;
  info->dpi = dpi;
  file->set_modified(true);
  modified = true;
  return true;
}

void
command_set_dpi(ParsingByteStream &pbs)
{
  GUTF8String sdpi = pbs.get_token();
  if (! sdpi.is_int())
    verror("usage: set-dpi <dpi>");
  int dpi = sdpi.toInt();
  if (dpi < 25 || dpi > 6000)
    verror("resolution should be in range 25..6000dpi");
  int rcount = 0;
  if (g().file)
    {
      GUTF8String id = g().fileid;
      if (set_dpi(g().file, dpi))
        rcount += 1;
    }
  else
    {
      GPList<DjVmDir::File> &lst = g().selected;
      for (GPosition p=lst; p; ++p)
        {
          GUTF8String id = lst[p]->get_load_name();
          const GP<DjVuFile> f(g().doc->get_djvu_file(id));
          if (set_dpi(f, dpi))
            rcount += 1;
        }
    }
  vprint("set dpi on %d pages", rcount);
}


#define DELMETA     1
#define DELXMP      8
#define CHKCOMPAT   2
#define EIGHTBIT    4

static bool
filter_ant(GP<ByteStream> in, 
           GP<ByteStream> out, 
           int flags)
{
  int c;
  int plevel = 0;
  bool copy = true;
  bool unchanged = true;
  bool compat = false;
  GP<ByteStream> mem;
  GP<ParsingByteStream> inp;

  if (flags & CHKCOMPAT)
    {
      mem = ByteStream::create();
      mem->copy(*in);
      mem->seek(0);
      char c;
      int state = 0;
      while (!compat && mem->read(&c,1)>0)
          {
            switch(state)
              {
              case 0:
                if (c == '\"')
                  state = '\"';
                break;
              case '\"':
                if (c == '\"')
                  state = 0;
                else if (c == '\\')
                  state = '\\';
                else if ((unsigned char)c<0x20 || c==0x7f)
                  compat = true;
                break;
              case '\\':
                if (!strchr("01234567tnrbfva\"\\",c))
                  compat = true;
                state = '\"';
                break;
              }
          }
      mem->seek(0);
      inp = ParsingByteStream::create(mem);
    }
  else
    {
      inp = ParsingByteStream::create(in);
    }
  
  while ((c = inp->get()) != EOF)
    if (c!=' ' && c!='\t' && c!='\r' && c!='\n')
      break;
  inp->unget(c);
  while ((c = inp->get()) != EOF)
    {
      if (plevel == 0)
        if (c !=' ' && c!='\t' && c!='\r' && c!='\n')
          copy = true;
      if (c == '\"')
        {
          inp->unget(c);
          GUTF8String token = inp->get_token(false, compat);
          if (copy)
	    print_c_string(token, token.length(), *out, !!(flags & EIGHTBIT));
          if (compat)
            unchanged = false;
        }
      else if (c == '(')
        {
          while ((c = inp->get()) != EOF)
            if (c!=' ' && c!='\t' && c!='\r' && c!='\n')
              break;
          inp->unget(c);
          if ((flags & DELMETA) && plevel==0 && c=='m')
            {
              GUTF8String token = inp->get_token();
              if (token == "metadata")
                copy = unchanged = false;
              if (copy) {
                out->write8('(');
                out->write((const char*)token, token.length());
              }
            }
          if ((flags & DELXMP) && plevel==0 && c=='x')
            {
              GUTF8String token = inp->get_token();
              if (token == "xmp")
                copy = unchanged = false;
              if (copy) {
                out->write8('(');
                out->write((const char*)token, token.length());
              }
            }
          else if (copy) 
            out->write8('(');
          plevel += 1;
        }
      else if (c == ')')
        {
          if (copy) 
            out->write8(c);
          if ( --plevel < 0)
            plevel = 0;
        }
      else if (copy)
        out->write8(c);
    }
  return !unchanged;
}

static bool
print_ant(GP<IFFByteStream> iff, 
          GP<ByteStream> out, 
          int flags=CHKCOMPAT)
{
  GUTF8String chkid;
  bool changed = false;
  if (utf8)
    flags |= EIGHTBIT;
  while (iff->get_chunk(chkid))
    {
      if (chkid == "ANTa") 
        {
          changed = filter_ant(iff->get_bytestream(), out, flags);
        }
      else if (chkid == "ANTz") 
        {
          GP<ByteStream> bsiff = 
	    BSByteStream::create(iff->get_bytestream());
          changed = filter_ant(bsiff, out, flags);
        }
      iff->close_chunk();
    }
  return changed;
}

void
command_print_ant(ParsingByteStream &)
{
  if (!g().file)
    verror("you must first select a single page");
  GP<ByteStream> out=ByteStream::create("w");
  GP<ByteStream> anno = g().file->get_anno();
  if (! (anno && anno->size())) return;
  GP<IFFByteStream> iff=IFFByteStream::create(anno);
  print_ant(iff, out);
  out->write8('\n');
}

void
command_print_merged_ant(ParsingByteStream &)
{
  if (!g().file)
    verror("you must first select a single page");
  GP<ByteStream> out=ByteStream::create("w");
  GP<ByteStream> anno = g().file->get_merged_anno();
  if (! (anno && anno->size())) return;
  GP<IFFByteStream> iff=IFFByteStream::create(anno);
  print_ant(iff, out);
  out->write8('\n');
}


static void
modify_ant(const GP<DjVuFile> &f, 
           const char *newchunkid,
           const GP<ByteStream> newchunk )
{
  const GP<ByteStream> anno(ByteStream::create());
  if (newchunkid && newchunk && newchunk->size())
    {
      const GP<IFFByteStream> out(IFFByteStream::create(anno));
      newchunk->seek(0);
      out->put_chunk(newchunkid);
      out->copy(*newchunk);
      out->close_chunk();
    }
  f->anno = anno;
  if (! anno->size())
    f->remove_anno();
  f->set_modified(true);
  modified = true;
}

void
file_remove_ant(const GP<DjVuFile> &f, const char *id)
{
  if (!f) return;
  modify_ant(f, 0, 0);
  vprint("remove_ant: modified \"%s\"", id);
}

void
command_remove_ant(ParsingByteStream &)
{
  GPList<DjVmDir::File> & lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
    {
      GUTF8String id = lst[p]->get_load_name();
      const GP<DjVuFile> f(g().doc->get_djvu_file(id));
      file_remove_ant(f, id);
    }
  }
}

void
command_set_ant(ParsingByteStream &pbs)
{
  if (! g().file)
    verror("must select a single page first");
  const GP<ByteStream> ant = ByteStream::create();
  {
    const GP<ByteStream> dsedant = ByteStream::create();
    get_data_from_file("set-ant", pbs, *dsedant);
    dsedant->seek(0);
    GP<ByteStream> bsant = BSByteStream::create(ant,100);
    filter_ant(dsedant, bsant, EIGHTBIT);
    bsant = 0;
  }
  modify_ant(g().file, "ANTz", ant);
  vprint("set-ant: modified \"%s\"", (const char*)ToNative(g().fileid));
}

static void
print_meta(IFFByteStream &iff, ByteStream &out)
{
  GUTF8String chkid;  
  while (iff.get_chunk(chkid))
    {
      bool ok = false;
      GP<DjVuANT> ant=DjVuANT::create();
      if (chkid == "ANTz") {
          GP<ByteStream> bsiff=BSByteStream::create(iff.get_bytestream());
          ant->decode(*bsiff);
          ok = true;
      } else if (chkid == "ANTa") {
        ant->decode(*iff.get_bytestream());
        ok = true;
      }
      if (ok)
        {
          { // extra nesting for windows
            for (GPosition pos=ant->metadata; pos; ++pos)
            { 
              GUTF8String tmp;
              tmp=ant->metadata.key(pos);
              out.writestring(tmp); 
              out.write8('\t');
              tmp=ant->metadata[pos];
              print_c_string((const char*)tmp, tmp.length(), out, utf8);
              out.write8('\n');
            }
          }
        }
      iff.close_chunk();
    }
}

void 
command_print_meta(ParsingByteStream &)
{
  if (! g().file )
    {
      GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
      if (frec)
        {
          vprint("print-meta: implicitly selecting shared annotations");
          select_clear();
          select_add(frec);
        }
    }
  if ( g().file )
    {
      GP<ByteStream> out=ByteStream::create("w");
      GP<ByteStream> anno = g().file->get_anno();
      if (! (anno && anno->size())) return;
      GP<IFFByteStream> iff=IFFByteStream::create(anno); 
      print_meta(*iff,*out);
    }
}


static bool
modify_meta(const GP<DjVuFile> &f,
            GMap<GUTF8String,GUTF8String> *newmeta)
{
  bool changed = false;
  GP<ByteStream> newant = ByteStream::create();
  if (newmeta && !newmeta->isempty())
    {
      newant->writestring(GUTF8String("(metadata"));
      { // extra nesting for windows
        for (GPosition pos=newmeta->firstpos(); pos; ++pos)
        {
          GUTF8String key = newmeta->key(pos); 
          GUTF8String val = (*newmeta)[pos];
          newant->write("\n\t(",3);
          newant->writestring(key);
          newant->write(" ",1);
          print_c_string((const char*)val, val.length(),
                         *newant, true);
          newant->write(")",1);
        }
      }
      newant->write(" )\n",3);
      changed = true;
    }
  GP<ByteStream> anno = f->get_anno();
  if (anno && anno->size()) 
    {
      GP<IFFByteStream> iff=IFFByteStream::create(anno);
      if (print_ant(iff, newant, DELMETA|CHKCOMPAT|EIGHTBIT))
        changed = true;
    }
  const GP<ByteStream> newantz=ByteStream::create();
  if (changed)
    {
      newant->seek(0);
      { 
        GP<ByteStream> bzz = BSByteStream::create(newantz,100); 
        bzz->copy(*newant); 
        bzz = 0;
      }
      newantz->seek(0);
      modify_ant(f, "ANTz", newantz);
    }
  return changed;
}

void
file_remove_meta(const GP<DjVuFile> &f, const char *id)
{
  if (modify_meta(f, 0))
    vprint("remove_meta: modified \"%s\"", id);
}

void 
command_remove_meta(ParsingByteStream &)
{
   GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
    {
      GUTF8String id = lst[p]->get_load_name();
      const GP<DjVuFile> f(g().doc->get_djvu_file(id));
      file_remove_meta(f, id);
    }
  }
}

void
command_set_meta(ParsingByteStream &pbs)
{
  // get metadata
  GP<ByteStream> metastream = ByteStream::create();
  get_data_from_file("set-meta", pbs, *metastream);
  metastream->seek(0);
  // parse metadata
  GMap<GUTF8String,GUTF8String> metadata;
  GP<ParsingByteStream> inp = ParsingByteStream::create(metastream);
  int c;
  while ( (c = inp->get_spaces(true)) != EOF )
    {
      GUTF8String key, val;
      inp->unget(c);
      key = inp->get_token();
      c = inp->get_spaces(false);
      if (c == '\"') {
        inp->unget(c);
        val = inp->get_token();
      } else {
        while (c!='\n' && c!='\r' && c!=EOF) {
          val += c;
          c = inp->get();
        }
      }
      if (key.length()>0 && val.length()>0)
        metadata[key] = val;
    }
  // possibly select shared annotations.
  if (! g().file)
    {
      GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
      if (frec)
        {
          vprint("set-meta: implicitly selecting shared annotations.");
        }
      else if (metadata.size() > 0)
        {
          vprint("set-meta: implicitly creating and selecting shared annotations.");
          g().doc->create_shared_anno_file();
          frec = g().doc->get_djvm_dir()->get_shared_anno_file();
        }
      if (frec)
        {
          select_clear();
          select_add(frec);
        }
    }
  // set metadata
  if (g().file && modify_meta(g().file, &metadata))
    vprint("set-meta: modified \"%s\"", 
           (const char*)ToNative(g().fileid));
}

static void
print_xmp(IFFByteStream &iff, ByteStream &out)
{
  GUTF8String chkid;  
  while (iff.get_chunk(chkid))
    {
      bool ok = false;
      GP<DjVuANT> ant=DjVuANT::create();
      if (chkid == "ANTz") {
          GP<ByteStream> bsiff=BSByteStream::create(iff.get_bytestream());
          ant->decode(*bsiff);
          ok = true;
      } else if (chkid == "ANTa") {
        ant->decode(*iff.get_bytestream());
        ok = true;
      }
      if (ok && ant->xmpmetadata.length()>0)
        {
          out.writestring(ant->xmpmetadata);
          out.write8('\n');
        }
      iff.close_chunk();
    }
}

void 
command_print_xmp(ParsingByteStream &)
{
  if (! g().file )
    {
      GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
      if (frec)
        {
          vprint("print-xmp: implicitly selecting shared annotations");
          select_clear();
          select_add(frec);
        }
    }
  if ( g().file )
    {
      GP<ByteStream> out=ByteStream::create("w");
      GP<ByteStream> anno = g().file->get_anno();
      if (! (anno && anno->size())) return;
      GP<IFFByteStream> iff=IFFByteStream::create(anno); 
      print_xmp(*iff,*out);
    }
}

static bool
modify_xmp(const GP<DjVuFile> &f, GUTF8String *newxmp)
{
  bool changed = false;
  GP<ByteStream> newant = ByteStream::create();
  if (newxmp && newxmp->length() > 0)
    {
      newant->writestring(GUTF8String("(xmp "));
      print_c_string((const char*)(*newxmp), newxmp->length(), *newant, true);
      newant->write(" )\n",3);
      changed = true;
    }
  GP<ByteStream> anno = f->get_anno();
  if (anno && anno->size()) 
    {
      GP<IFFByteStream> iff=IFFByteStream::create(anno);
      if (print_ant(iff, newant, DELXMP|CHKCOMPAT|EIGHTBIT))
        changed = true;
    }
  const GP<ByteStream> newantz=ByteStream::create();
  if (changed)
    {
      newant->seek(0);
      { 
        GP<ByteStream> bzz = BSByteStream::create(newantz,100); 
        bzz->copy(*newant); 
        bzz = 0;
      }
      newantz->seek(0);
      modify_ant(f, "ANTz", newantz);
    }
  return changed;
}

void
file_remove_xmp(const GP<DjVuFile> &f, const char *id)
{
  if (modify_xmp(f, 0))
    vprint("remove_xmp: modified \"%s\"", id);
}

void 
command_remove_xmp(ParsingByteStream &)
{
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
      {
        GUTF8String id = lst[p]->get_load_name();
        const GP<DjVuFile> f(g().doc->get_djvu_file(id));
        file_remove_xmp(f, id);
      }
  }
}

void 
command_set_xmp(ParsingByteStream &pbs)
{
  // get xmpmetadata
  GP<ByteStream> metastream = ByteStream::create();
  get_data_from_file("set-meta", pbs, *metastream);
  metastream->seek(0);
  // read xmpmetadata
  int size = metastream->size();
  char *buffer = new char[size+1];
  metastream->readall(buffer,size);
  buffer[size] = 0;
  GUTF8String xmpmetadata(buffer);
  delete [] buffer;
  // possibly select shared annotations.
  if (! g().file)
    {
      GP<DjVmDir::File> frec = g().doc->get_djvm_dir()->get_shared_anno_file();
      if (frec)
        {
          vprint("set-xmp: implicitly selecting shared annotations.");
        }
      else if (xmpmetadata.length() > 0)
        {
          vprint("set-xmp: implicitly creating and selecting shared annotations.");
          g().doc->create_shared_anno_file();
          frec = g().doc->get_djvm_dir()->get_shared_anno_file();
        }
      if (frec)
        {
          select_clear();
          select_add(frec);
        }
    }
  // set metadata
  if (g().file && modify_xmp(g().file, &xmpmetadata))
    vprint("set-xmp: modified \"%s\"", 
           (const char*)ToNative(g().fileid));
}





struct  zone_names_struct
{ 
  const char *name;
  DjVuTXT::ZoneType ztype;
  char separator;
};

static zone_names_struct* zone_names() {
  static zone_names_struct xzone_names[] = 
  {
    { "page",   DjVuTXT::PAGE,      0 },
    { "column", DjVuTXT::COLUMN,    DjVuTXT::end_of_column },
    { "region", DjVuTXT::REGION,    DjVuTXT::end_of_region },
    { "para",   DjVuTXT::PARAGRAPH, DjVuTXT::end_of_paragraph },
    { "line",   DjVuTXT::LINE,      DjVuTXT::end_of_line },
    { "word",   DjVuTXT::WORD,      ' ' },
    { "char",   DjVuTXT::CHARACTER, 0 },
    { 0, (DjVuTXT::ZoneType)0 ,0 }
  };
  return xzone_names;
};

GP<DjVuTXT>
get_text(const GP<DjVuFile> &file)
{ 
  GUTF8String chkid;
  const GP<ByteStream> bs(file->get_text());
  if (bs) 
    {
      long int i=0;
      const GP<IFFByteStream> iff(IFFByteStream::create(bs));
      while (iff->get_chunk(chkid))
        {
          i++;
          if (chkid == GUTF8String("TXTa")) 
            {
              GP<DjVuTXT> txt = DjVuTXT::create();
              txt->decode(iff->get_bytestream());
              return txt;
            }
          else if (chkid == GUTF8String("TXTz")) 
            {
              GP<DjVuTXT> txt = DjVuTXT::create();
              GP<ByteStream> bsiff=BSByteStream::create(iff->get_bytestream());
              txt->decode(bsiff);
              return txt;
            }
          iff->close_chunk();
        }
    }
  return 0;
}

void
print_txt_sub(const GP<DjVuTXT> &txt, DjVuTXT::Zone &zone, 
	      const GP<ByteStream> &out, int indent)
{
  // Indentation
  if (indent)
    {
      out->write("\n",1);
      static const char spaces[] = "                        ";
      if (indent > (int)sizeof(spaces))
        indent = sizeof(spaces);
      out->write(spaces, indent);
    }
  // Zone header
  int zinfo;
  for (zinfo=0; zone_names()[zinfo].name; zinfo++)
    if (zone.ztype == zone_names()[zinfo].ztype)
      break;
  GUTF8String message = "(bogus";
  if (zone_names()[zinfo].name)
    message.format("(%s %d %d %d %d", zone_names()[zinfo].name,
                   zone.rect.xmin, zone.rect.ymin, 
                   zone.rect.xmax, zone.rect.ymax);
  out->write((const char*)message, message.length());
  // Zone children
  if (zone.children.isempty()) 
    {
      const char *data = txt->textUTF8.getbuf() + zone.text_start;
      int length = zone.text_length;
      if (data[length-1] == zone_names()[zinfo].separator)
        length -= 1;
      out->write(" ",1);
      print_c_string(data, length, *out, utf8);
    }
  else
    {
      for (GPosition pos=zone.children; pos; ++pos)
        print_txt_sub(txt, zone.children[pos], out, indent + 1);
    }
  // Finish
  out->write(")",1);
  if (!indent)
    out->write("\n", 1);
}

void
print_txt(const GP<DjVuTXT> &txt, const GP<ByteStream> &out)
{
  if (txt)
    print_txt_sub(txt, txt->page_zone, out, 0);
}

void
command_print_txt(ParsingByteStream &)
{
  const GP<ByteStream> out = ByteStream::create("w");
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
      if (lst[p]->is_page())
      {
        GUTF8String id = lst[p]->get_load_name();
        const GP<DjVuFile> f(g().doc->get_djvu_file(id));
        const GP<DjVuTXT> txt(get_text(f));
        if (txt)
          print_txt(txt, out);
        else
          out->write("(page 0 0 0 0 \"\")\n",18);
      }
  }
}

void
command_print_pure_txt(ParsingByteStream &)
{
  const GP<ByteStream> out = ByteStream::create("w");
  GP<DjVuTXT> txt;
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
    {
      GUTF8String id = lst[p]->get_load_name();
      const GP<DjVuFile> f(g().doc->get_djvu_file(id));
      if ((txt = get_text(f)))
        {
          GUTF8String ntxt = txt->textUTF8;
          out->write((const char*)ntxt, ntxt.length());
        }
      out->write("\f",1);
    }
  }
}

static void
modify_txt(const GP<DjVuFile> &f, 
           const char *newchunkid,
           const GP<ByteStream> newchunk )
{
  const GP<ByteStream> text(ByteStream::create());
  if (newchunkid && newchunk && newchunk->size())
    {
      const GP<IFFByteStream> out(IFFByteStream::create(text));
      newchunk->seek(0);
      out->put_chunk(newchunkid);
      out->copy(*newchunk);
      out->close_chunk();
    }
  f->text = text;
  if (! text->size())
    f->remove_text();
  f->set_modified(true);
  modified = true;
}

void
file_remove_txt(const GP<DjVuFile> &f, const char *id)
{
  if (! f) return;
  modify_txt(f, 0, 0);
  vprint("remove-txt: modified \"%s\"", id);
}

void
command_remove_txt(ParsingByteStream &)
{
  GPList<DjVmDir::File> &lst = g().selected;
  { // extra nesting for windows
    for (GPosition p=lst; p; ++p)
    {
      GUTF8String id = lst[p]->get_load_name();
      const GP<DjVuFile> f(g().doc->get_djvu_file(id));
      file_remove_txt(f, id);
    }
  }
}

void
construct_djvutxt_sub(ParsingByteStream &pbs, 
                      const GP<DjVuTXT> &txt, DjVuTXT::Zone &zone,
                      int mintype, bool exact)
{
  int c;
  GUTF8String token;
  // Get zone type
  c = pbs.get_spaces(true);
  if (c != '(')
    verror("syntax error in txt data: expecting '(',\n\tnear '%s'", 
           pbs.get_error_context(c) );
  token = pbs.get_token(true);
  int zinfo;
  for (zinfo=0; zone_names()[zinfo].name; zinfo++)
    if (token == zone_names()[zinfo].name)
      break;
  if (! zone_names()[zinfo].name)
    verror("Syntax error in txt data: undefined token '%s',\n\tnear '%s'",
           (const char*)ToNative(token), pbs.get_error_context());
  zone.ztype = zone_names()[zinfo].ztype;
  if (zone.ztype<mintype || (exact && zone.ztype>mintype))
    verror("Syntax error in txt data: illegal zone token '%s',\n\tnear '%s'",
           (const char*)ToNative(token), pbs.get_error_context());           
  // Get zone rect
  GUTF8String str;
  str = pbs.get_token(true);
  if (!str || !str.is_int()) 
    nerror: verror("Syntax error in txt data: number expected,\n\tnear '%s%s'",
                   (const char*)ToNative(str), pbs.get_error_context());  
  zone.rect.xmin = atoi(str);
  str = pbs.get_token(true);
  if (!str || !str.is_int()) 
    goto nerror;
  zone.rect.ymin = atoi(str);
  str = pbs.get_token(true);
  if (!str || !str.is_int()) 
    goto nerror;
  zone.rect.xmax = atoi(str);
  str = pbs.get_token(true);
  if (!str || !str.is_int()) 
    goto nerror;
  zone.rect.ymax = atoi(str);
  if (zone.rect.xmin > zone.rect.xmax) 
    {
      int tmp = zone.rect.xmin; 
      zone.rect.xmin=zone.rect.xmax; 
      zone.rect.xmax=tmp; 
    }
  if (zone.rect.ymin > zone.rect.ymax)
    {
      int tmp = zone.rect.ymin; 
      zone.rect.ymin=zone.rect.ymax; 
      zone.rect.ymax=tmp; 
    }
  // Continue processing
  c = pbs.get_spaces(true);
  pbs.unget(c);
  if (c == '"') 
    {
      // This is a terminal
      str = pbs.get_token(true);
      zone.text_start = txt->textUTF8.length();
      zone.text_length = str.length();
      txt->textUTF8 += str;
      
    }
  else 
    {
      // This is a non terminal
      while (c != ')')
        {
          if (c != '(')
            verror("Syntax error in text data: expecting subzone,\n\tnear '%s'",
                   pbs.get_error_context() );
          DjVuTXT::Zone *nzone = zone.append_child();
          construct_djvutxt_sub(pbs, txt, *nzone, zone.ztype+1, false);
          c = pbs.get_spaces(true);
          pbs.unget(c);
        }
    }
  // Skip last parenthesis
  c = pbs.get_spaces(true);
  if (c != ')')
    verror("Syntax error in text data: missing parenthesis,\n\tnear '%s'",
           pbs.get_error_context(c) );
}

GP<DjVuTXT>
construct_djvutxt(ParsingByteStream &pbs)
{
  GP<DjVuTXT> txt(DjVuTXT::create());
  int c = pbs.get_spaces(true);
  if (c == EOF)
    return 0;
  pbs.unget(c);
  construct_djvutxt_sub(pbs, txt, txt->page_zone, DjVuTXT::PAGE, true);
  if (pbs.get_spaces(true) != EOF)
    verror("Syntax error in txt data: garbage after data");
  txt->normalize_text();
  if (! txt->textUTF8)
    return 0;
  return txt;
}

void
command_set_txt(ParsingByteStream &pbs)
{
  if (! g().file)
    verror("must select a single page first");
  const GP<ByteStream> txtbs(ByteStream::create());
  get_data_from_file("set-txt", pbs, *txtbs);
  txtbs->seek(0);
  GP<ParsingByteStream> txtpbs(ParsingByteStream::create(txtbs));
  const GP<DjVuTXT> txt(construct_djvutxt(*txtpbs));
  GP<ByteStream> txtobs=ByteStream::create();
  if (txt)
    {
      const GP<ByteStream> bsout(BSByteStream::create(txtobs,1000));
      txt->encode(bsout);
    }
  txtobs->seek(0);
  modify_txt(g().file, "TXTz", txtobs);
  vprint("set-txt: modified \"%s\"", (const char*)ToNative(g().fileid));
}

void
output(const GP<DjVuFile> &f, const GP<ByteStream> &out, 
       int flag, const char *id=0, int pageno=0)
{
  if (f)
    {
      const GP<ByteStream> ant(ByteStream::create());
      const GP<ByteStream> txt(ByteStream::create());
      char pagenumber[16];
      sprintf(pagenumber," # page %d", pageno);
      if (flag & 1) 
        { 
          const GP<ByteStream> anno(f->get_anno());
          if (anno && anno->size()) 
            {
              const GP<IFFByteStream> iff(IFFByteStream::create(anno)); 
              print_ant(iff, ant); 
              ant->seek(0); 
            }
        }
      if (flag & 2)
        { 
          print_txt(get_text(f),txt); 
          txt->seek(0); 
        }
      if (id && ant->size() + txt->size())
        {
          static const char msg1[] = "# ------------------------- \nselect \0";
          static const char msg2[] = "\n\0";
          out->write(msg1, strlen(msg1));
          print_c_string(id, strlen(id), *out, utf8);
          if (pageno > 0) out->write(pagenumber, strlen(pagenumber));
          out->write(msg2, strlen(msg2));
        }
      if (ant->size()) 
        {
          out->write("set-ant\n", 8);
          out->copy(*ant);
          out->write("\n.\n", 3);
        }
      if (txt->size()) 
        {
          out->write("set-txt\n", 8);
          out->copy(*txt);
          out->write("\n.\n", 3);
        }
    }
}

void
command_output_ant(ParsingByteStream &)
{
  const GP<ByteStream> out = ByteStream::create("w");
  if (g().file) 
    {
      output(g().file, out, 1);
    }
  else 
    {
      const char *pre = "select; remove-ant\n";
      out->write(pre, strlen(pre));
      GPList<DjVmDir::File> &lst = g().selected;
      { // extra nesting for windows
        for (GPosition p=lst; p; ++p)
        {
          int pageno = lst[p]->get_page_num();
          GUTF8String id = lst[p]->get_load_name();
          const GP<DjVuFile> f(g().doc->get_djvu_file(id));
          output(f, out, 1, id, pageno+1);
        }
      }
    }
}

void
command_output_txt(ParsingByteStream &)
{
  const GP<ByteStream> out = ByteStream::create("w");
  if (g().file) 
    {
      output(g().file, out, 2);
    }
  else 
    {
      const char *pre = "select; remove-txt\n";
      out->write(pre, strlen(pre));
      GPList<DjVmDir::File> &lst = g().selected;
      { // extra nesting for windows
        for (GPosition p=lst; p; ++p)
        {
          int pageno = lst[p]->get_page_num();
          GUTF8String id = lst[p]->get_load_name();
          const GP<DjVuFile> f(g().doc->get_djvu_file(id));
          output(f, out, 2, id, pageno+1);
        }
      }
    }
}

void
command_output_all(ParsingByteStream &)
{
  const GP<ByteStream> out = ByteStream::create("w");
  if (g().file) 
    {
      output(g().file, out, 3);
    }
  else 
    {
      const char *pre = "select; remove-ant; remove-txt\n";
      out->write(pre, strlen(pre));
      GPList<DjVmDir::File> &lst = g().selected;
      { // extra nesting for windows
        for (GPosition p=lst; p; ++p)
        {
          int pageno = lst[p]->get_page_num();
          GUTF8String id = lst[p]->get_load_name();
          const GP<DjVuFile> f(g().doc->get_djvu_file(id));
          output(f, out, 3, id, pageno+1);
        }
      }
    }
}

void
print_outline_sub(const GP<DjVmNav> &nav, int &pos, int count, 
                  const GP<ByteStream> &out, int indent)
{
  GUTF8String str;
  GP<DjVmNav::DjVuBookMark> entry;
  while (count > 0 && pos < nav->getBookMarkCount())
    {
      out->write("\n",1);
      { // extra nesting for windows
        for (int i=0; i<indent; i++)
          out->write(" ",1);
      }
      nav->getBookMark(entry, pos++);
      out->write("(",1);
      str = entry->displayname;
      print_c_string(str, str.length(), *out, utf8);
      out->write("\n ",2);
      { // extra nesting for windows
        for (int i=0; i<indent; i++)
          out->write(" ",1);
      }
      str = entry->url;
      print_c_string(str, str.length(), *out, utf8);
      print_outline_sub(nav, pos, entry->count, out, indent+1);
      out->write(" )",2);
      count--;
    }
}

void
command_print_outline(ParsingByteStream &pbs)
{
  GP<DjVmNav> nav = g().doc->get_djvm_nav();
  if (nav)
    {
      int pos = 0;
      int count = nav->getBookMarkCount();
      if (count > 0)
        {
          const GP<ByteStream> out = ByteStream::create("w");
          out->write("(bookmarks",10);
          print_outline_sub(nav, pos, count, out, 1);
          out->write(" )\n", 3);
        }
    }
}

void
construct_outline_sub(ParsingByteStream &pbs, GP<DjVmNav> nav, int &count)
{
  int c;
  GUTF8String name, url;
  GP<DjVmNav::DjVuBookMark> mark;
  if ((c = pbs.get_spaces(true)) != '\"')
    verror("Syntax error in outline: expecting name string,\n\tnear '%s'.",
           pbs.get_error_context(c) );    
  pbs.unget(c);
  name = pbs.get_token();
  if ((c = pbs.get_spaces(true)) != '\"')
    verror("Syntax error in outline: expecting url string,\n\tnear '%s'.",
           pbs.get_error_context(c) );    
  pbs.unget(c);
  url = pbs.get_token();
  mark = DjVmNav::DjVuBookMark::create(0, name, url);
  nav->append(mark);
  count += 1;
  while ((c = pbs.get_spaces(true)) == '(')
    construct_outline_sub(pbs, nav, mark->count);
  if (c != ')')
    verror("Syntax error in outline: expecting ')',\n\tnear '%s'.",
           pbs.get_error_context(c) );    
}

GP<DjVmNav>
construct_outline(ParsingByteStream &pbs)
{
  GP<DjVmNav> nav(DjVmNav::create());
  int c = pbs.get_spaces(true);
  int count = 0;
  if (c == EOF)
    return 0;
  if (c!='(')
    verror("Syntax error in outline data: expecting '(bookmarks'");
  if (pbs.get_token()!="bookmarks")
    verror("Syntax error in outline data: expecting '(bookmarks'");    
  while ((c = pbs.get_spaces(true)) == '(')
    construct_outline_sub(pbs, nav, count);
  if (c != ')')
    verror("Syntax error in outline: expecting parenthesis,\n\tnear '%s'.",
           pbs.get_error_context(c) );
  if (pbs.get_spaces(true) != EOF)
    verror("Syntax error in outline: garbage after last ')',\n\tnear '%s'",
           pbs.get_error_context(c) );
  if (nav->getBookMarkCount() < 1)
    return 0;
  if (!nav->isValidBookmark())
    verror("Invalid outline data!");
  return nav;
}

void
command_set_outline(ParsingByteStream &pbs)
{
  const GP<ByteStream> outbs(ByteStream::create());
  get_data_from_file("set-outline", pbs, *outbs);
  outbs->seek(0);
  GP<ParsingByteStream> outpbs(ParsingByteStream::create(outbs));
  GP<DjVmNav> nav(construct_outline(*outpbs));
  if (g().doc->get_djvm_nav() != nav)
    {
      g().doc->set_djvm_nav(nav);
      modified = true;
    }
}

void
command_remove_outline(ParsingByteStream &pbs)
{
  if (g().doc->get_djvm_nav())
    {
      g().doc->set_djvm_nav(0);
      modified = true;
    }
}

static bool
callback_thumbnails(int page_num, void *)
{
  vprint("set-thumbnails: processing page %d", page_num+1);
  return false;
}

void
command_set_thumbnails(ParsingByteStream &pbs)
{
  GUTF8String sizestr = pbs.get_token();
  if (! sizestr)
    sizestr = "192";
  if (! sizestr.is_int() )
    verror("expecting integer argument");
  int size = atoi(sizestr);
  if (size < 32 || size > 512) 
    verror("size should be between 32 and 256 (e.g. 128)");
  g().doc->generate_thumbnails(size, callback_thumbnails, NULL);
  modified = true;
}

void
command_remove_thumbnails(ParsingByteStream &)
{
  g().doc->remove_thumbnails();
  modified = true;
}

void
command_save_page(ParsingByteStream &pbs)
{
  GUTF8String fname = pbs.get_token();
  if (! fname) 
    verror("empty filename");
  if (! g().file)
    verror("must select a single page first");
  if (nosave)
    vprint("save_page: not saving anything (-n was specified)");
  if (nosave)
    return;
  const GP<ByteStream> bs(g().file->get_djvu_bytestream(false, false));
  const GP<ByteStream> out(ByteStream::create(GURL::Filename::UTF8(fname), "wb"));
  out->writall("AT&T",4);
  out->copy(*bs);
  vprint("saved \"%s\" as \"%s\"  (without inserting included files)",
         (const char*)ToNative(g().fileid), (const char*)fname);
}

void
command_save_page_with(ParsingByteStream &pbs)
{
  GUTF8String fname = pbs.get_token();
  if (! fname) 
    verror("empty filename");
  if (! g().file)
    verror("must select a single page first");
  if (nosave)
    vprint("save-page-with: not saving anything (-n was specified)");
  if (nosave)
    return;
  const GP<ByteStream> bs(g().file->get_djvu_bytestream(true, false));
  const GP<ByteStream> out(ByteStream::create(GURL::Filename::UTF8(fname), "wb"));
  out->writall("AT&T",4);
  out->copy(*bs);
  vprint("saved \"%s\" as \"%s\"  (inserting included files)",
         (const char*)ToNative(g().fileid), (const char*)fname);
}

void
command_save_bundled(ParsingByteStream &pbs)
{
  GUTF8String fname = pbs.get_token();
  if (! fname) 
    verror("empty filename");
  if (nosave) 
    vprint("save-bundled: not saving anything (-n was specified)");
  else
    g().doc->save_as(GURL::Filename::UTF8(fname), true);
  modified = false;
}

void
command_save_indirect(ParsingByteStream &pbs)
{
  GUTF8String fname = pbs.get_token();
  if (! fname) 
    verror("empty filename");
  if (nosave) 
    vprint("save-indirect: not saving anything (-n was specified)");
  else
    g().doc->save_as(GURL::Filename::UTF8(fname), false);
  modified = false;
}

void
command_save(void)
{
  if (!g().doc->can_be_saved())
    verror("cannot save old format (use save-bundled or save-indirect)");
  if (nosave)
    vprint("save: not saving anything (-n was specified)");
  else if (!modified)
    vprint("save: document was not modified");
  else 
    g().doc->save();
  modified = false;
}

void
command_save(ParsingByteStream &)
{
  command_save();
}

void
command_help(void)
{
  fprintf(stderr,
          "\n"
          "Commands\n"
          "--------\n"
          "The following commands can be separated by newlines or semicolons.\n"
          "Comment lines start with '#'.  Commands usually operate on pages and files\n"
          "specified by the \"select\" command.  All pages and files are initially selected.\n"
          "A single page must be selected before executing commands marked with a period.\n"
          "Commands marked with an underline do not use the selection\n"
          "\n"
          "   ls                     -- list all pages/files\n"
          "   n                      -- list pages count\n"
          "   dump                   -- shows IFF structure\n"
          "   size                   -- prints page width and height in html friendly way\n"
          "   select                 -- selects the entire document\n"
          "   select <id>            -- selects a single page/file by name or page number\n"
          "   select-shared-ant      -- selects the shared annotations file\n"
          "   create-shared-ant      -- creates and select the shared annotations file\n"
          "   showsel                -- displays currently selected pages/files\n"
          " . print-ant              -- prints annotations\n"
          " . print-merged-ant       -- prints annotations including the shared annotations\n"
          " . print-meta             -- prints file metadatas (a subset of the annotations\n"
          "   print-txt              -- prints hidden text using a lisp syntax\n"
          "   print-pure-txt         -- print hidden text without coordinates\n"
          " _ print-outline          -- print outline (bookmarks)\n"
          " . print-xmp              -- print xmp annotations\n"
          "   output-ant             -- dumps ant as a valid cmdfile\n"
          "   output-txt             -- dumps text as a valid cmdfile\n"
          "   output-all             -- dumps ant and text as a valid cmdfile\n"
          " . set-ant [<antfile>]    -- copies <antfile> into the annotation chunk\n"
          " . set-meta [<metafile>]  -- copies <metafile> into the metadata annotation tag\n"
          " . set-txt [<txtfile>]    -- copies <txtfile> into the hidden text chunk\n"
          " . set-xmp [<xmpfile>]    -- copies <xmpfile> into the xmp metadata annotation tag\n" 
          " _ set-outline [<bmfile>] -- sets outline (bookmarks)\n"
          " _ set-thumbnails [<sz>]  -- generates all thumbnails with given size\n"
          "   set-rotation [+-]<rot> -- sets page rotation\n"
          "   set-dpi <dpi>          -- sets page resolution\n"
          "   remove-ant             -- removes annotations\n"
          "   remove-meta            -- removes metadatas without changing other annotations\n"
          "   remove-txt             -- removes hidden text\n"
          " _ remove-outline         -- removes outline (bookmarks)\n"
          " . remove-xmp             -- removes xmp metadata from annotation chunk\n"
          " _ remove-thumbnails      -- removes all thumbnails\n"
          " . set-page-title <title> -- sets an alternate page title\n"
          " . save-page <name>       -- saves selected page/file as is\n"
          " . save-page-with <name>  -- saves selected page/file, inserting all included files\n"
          " _ save-bundled <name>    -- saves as bundled document under fname\n"
          " _ save-indirect <name>   -- saves as indirect document under fname\n"
          " _ save                   -- saves in-place\n"
          " _ help                   -- prints this message\n"
          "\n"
          "Interactive example:\n"
          "--------------------\n"
          "  Type\n"
          "    %% djvused -v file.djvu\n"
          "  and play with the commands above\n"
          "\n"
          "Command line example:\n"
          "---------------------\n"
          "  Save all text and annotation chunks as a djvused script with\n"
          "    %% djvused file.djvu -e output-all > file.dsed\n"
          "  Then edit the script with any text editor.\n"
          "  Finally restore the modified text and annotation chunks with\n"
          "    %% djvused file.djvu -f file.dsed -s\n"
          "  You may use option -v to see more messages\n"
          "\n" );
}

void
command_help(ParsingByteStream &)
{
  command_help();
}

typedef void (*CommandFunc)(ParsingByteStream &pbs);
static GMap<GUTF8String,CommandFunc> &command_map() {
  static GMap<GUTF8String,CommandFunc> xcommand_map;
  static bool first=true;
  if(first) {
    first=false;
    xcommand_map["ls"] = command_ls;
    xcommand_map["n"] = command_n;
    xcommand_map["dump"] = command_dump;
    xcommand_map["size"] = command_size;
    xcommand_map["showsel"] = command_showsel;
    xcommand_map["select"] = command_select;
    xcommand_map["select-shared-ant"] = command_select_shared_ant;
    xcommand_map["create-shared-ant"] = command_create_shared_ant;
    xcommand_map["print-ant"] = command_print_ant;  
    xcommand_map["print-merged-ant"] = command_print_merged_ant;
    xcommand_map["print-meta"] = command_print_meta;
    xcommand_map["print-txt"] = command_print_txt;
    xcommand_map["print-pure-txt"] = command_print_pure_txt;
    xcommand_map["print-outline"] = command_print_outline;
    xcommand_map["print-xmp"] = command_print_xmp;
    xcommand_map["output-ant"] = command_output_ant;
    xcommand_map["output-txt"] = command_output_txt;
    xcommand_map["output-all"] = command_output_all;
    xcommand_map["set-ant"] = command_set_ant;
    xcommand_map["set-meta"] = command_set_meta;
    xcommand_map["set-txt"] = command_set_txt;
    xcommand_map["set-outline"] = command_set_outline;
    xcommand_map["set-xmp"] = command_set_xmp;
    xcommand_map["set-thumbnails"] = command_set_thumbnails;
    xcommand_map["set-rotation"] = command_set_rotation;
    xcommand_map["set-dpi"] = command_set_dpi;
    xcommand_map["remove-ant"] = command_remove_ant;
    xcommand_map["remove-meta"] = command_remove_meta;
    xcommand_map["remove-txt"] = command_remove_txt;
    xcommand_map["remove-outline"] = command_remove_outline;
    xcommand_map["remove-thumbnails"] = command_remove_thumbnails;
    xcommand_map["remove-xmp"] = command_remove_xmp;
    xcommand_map["set-page-title"] = command_set_page_title;
    xcommand_map["save-page"] = command_save_page;
    xcommand_map["save-page-with"] = command_save_page_with;
    xcommand_map["save-bundled"] = command_save_bundled;
    xcommand_map["save-indirect"] = command_save_indirect;
    xcommand_map["save"] = command_save;
    xcommand_map["help"] = command_help;
  }
  return xcommand_map;
}

void
usage()
{
  DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
          "DJVUSED --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
          "Simple DjVu file manipulation program\n"
          "\n"
          "Usage: djvused [options] djvufile\n"
          "Executes scripting commands on djvufile.\n"
          "Script command come either from a script file (option -f),\n"
          "from the command line (option -e), or from stdin (default).\n"
          "\n"
          "Options are\n"
          "  -v               -- verbose\n"
          "  -f <scriptfile>  -- take commands from a file\n"
          "  -e <script>      -- take commands from the command line\n"
          "  -s               -- save after execution\n"
          "  -u               -- produces utf8 instead of escaping non ascii chars\n"
          "  -n               -- do not save anything\n"
          "\n"
          );
  command_help();
  exit(10);
}



// --------------------------------------------------
// MAIN
// --------------------------------------------------

void 
execute()
{
  if (!g().cmdbs)
    g().cmdbs = ByteStream::create("r");
  const GP<ParsingByteStream> gcmd(ParsingByteStream::create(g().cmdbs));
  ParsingByteStream &cmd=*gcmd;
  GUTF8String token;
  vprint("type \"help\" to see available commands.");
  vprint("ok.");
  while (!! (token = cmd.get_token(true)))
    {
      CommandFunc func = command_map()[token];
      G_TRY
        {
          if (!func) 
            verror("unrecognized command");
          // Cautious execution
          (*func)(cmd);
          // Skip extra arguments
          int c = cmd.get_spaces();
          if (c!=';' && c!='\n' && c!='\r' && c!=EOF)
            {
              while (c!=';' && c!='\n' && c!='\r' && c!=EOF)
                c = cmd.get();
              verror("too many arguments");
            }
          cmd.unget(c);
        }
      G_CATCH(ex)
        {
          vprint("Error (%s): %s",
                 (const char*)ToNative(token), 
                 (const char *)DjVuMessageLite::LookUpUTF8(ex.get_cause()));
          if (! verbose)
            G_RETHROW;
        }
      G_ENDCATCH;
      vprint("ok.");
    }
}


int 
main(int argc, char **argv)
{
  DJVU_LOCALE;
  G_TRY
     {
      { // extra nesting for windows
        for (int i=1; i<argc; i++)
          if (!strcmp(argv[i],"-v"))
            verbose = true;
          else if (!strcmp(argv[i],"-s"))
            save = true; 
          else if (!strcmp(argv[i],"-n"))
            nosave = true;
          else if (!strcmp(argv[i],"-u"))
            utf8 = true;
          else if (!strcmp(argv[i],"-f") && i+1<argc && !g().cmdbs) 
            g().cmdbs = ByteStream::create(GURL::Filename::UTF8(GNativeString(argv[++i])), "r");
          else if (!strcmp(argv[i],"-e") && !g().cmdbs && i+1<argc && ++i) 
            g().cmdbs = ByteStream::create_static(argv[i],strlen(argv[i]));
          else if (argv[i][0] != '-' && !g().djvufile)
            g().djvufile = GNativeString(argv[i]);
          else
            usage();
      }
      if (!g().djvufile)
        usage();
      // BOM
#ifdef _WIN32
      if (utf8)
        fwrite(utf8bom, sizeof(utf8bom), 1, stdout);
#endif
      // Open file
      g().doc = DjVuDocEditor::create_wait(GURL::Filename::UTF8(g().djvufile));
      select_all();
      // Execute
      execute();
      if (modified)
	{
	  if (save)
	    command_save();
	  else
	    fprintf(stderr,"djvused: (warning) file was modified but not saved\n");
	}
     }
  G_CATCH(ex)
    {
      ex.perror();
      return 10;
    }
  G_ENDCATCH;
  return 0;
}
