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
#include "DjVmDir.h"
#include "ByteStream.h"
#include "IFFByteStream.h"
#include "DjVuText.h"
#include "DjVuImage.h"
#include "GString.h"
#include "GOS.h"
#include "GURL.h"
#include "DjVuMessage.h"

#include "common.h"

#include <sys/stat.h>
#include <time.h>


static bool cgi = false;
static bool head = false;


struct DJVUSERVEGlobal 
{
  // Globals that need static initialization
  // are grouped here to work around broken compilers.
  GUTF8String pathinfo;
  GUTF8String pathtranslated;
  GUTF8String requestmethod;
  GUTF8String querystring;
};

static DJVUSERVEGlobal& g(void)
{
  static DJVUSERVEGlobal g;
  return g;
}


static void
usage(void)
{
   DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
          "DJVUSERVE --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
          "Extracts hidden text from Djvu files\n"
          "\n"
          "Usage: djvuserve [<djvufile>[/<djvmid>]\n"
          "Outputs the specified <djvufile> with valid Content-Type,\n"
          "Content-Length, and Expire HTTP headers.  Bundled multipage DjVu\n"
          "documents are accessed as indirect document using the <djvmid>\n"
          "syntax. Specifying a <djvmid> of <index> generates an indirect page\n"
          "directory pointing to the other component files.\n"
          "This program is designed to be used as a CGI executable.\n"
          "It uses environment variable PATH_TRANSLATED when executed\n"
          "without arguments.\n\n" );
   exit(10);
}

static const char * 
day_name(int d)
{
   static const char *n[] = {
     "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" 
   };
   if (d>=0 && d<7)
     return n[d];
   return "???";
}

static const char* 
month_name(int d)
{
   static const char *n[] = {
     "Jan", "Feb", "Mar", "Apr", "May", "Jun",
     "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" 
   };
   if (d>=0 && d<12)
     return n[d];
   return "???";
}

void
fprintdate(FILE *f, const char *fmt, const time_t *tim)
{
  char ctim[128];
  struct tm *ttim = gmtime(tim);
  /* strftime(ctim, sizeof(ctim)-1, "%a, %d %b %Y %H:%M:%S GMT", ttim); */
  sprintf(ctim,"%3s, %02d %3s %04d %02d:%02d:%02d GMT",
	  day_name(ttim->tm_wday), ttim->tm_mday,
	  month_name(ttim->tm_mon), 1900+ttim->tm_year,
	  ttim->tm_hour, ttim->tm_min, ttim->tm_sec);
  fprintf(stdout, fmt, ctim);
}

void
headers(const struct stat *statbuf, const char *fname = 0)
{
  fprintf(stdout,"Content-Type: image/x.djvu\n");
  if (fname)
    fprintf(stdout,"Content-Disposition: attachment; filename=\"%s\"\n", fname);
  fprintf(stdout,"Content-Length: %ld\n", (long)statbuf->st_size);
  time_t tim = time(0) + 360 * 24 * 3600;
  fprintdate(stdout, "Last-Modified: %s\n", &statbuf->st_mtime);
  fprintdate(stdout, "Expires: %s\n", &tim);
}

bool
is_djvu_file_bundled(GURL &pathurl)
{
  GP<ByteStream> in = ByteStream::create(pathurl,"rb");
  GP<IFFByteStream> iff = IFFByteStream::create(in);
  GUTF8String chkid;
  iff->get_chunk(chkid);
  // Make sure that this is a DjVu file.
  if (chkid != "FORM:DJVU" &&
      chkid != "FORM:DJVM" &&
      chkid != "FORM:PM44" &&
      chkid != "FORM:BM44"   )
    G_THROW("Corrupted DjVu file");
  // Test if it is bundled
  if (chkid == "FORM:DJVM")
    {
      while (iff->get_chunk(chkid) && chkid!="DIRM")
        iff->close_chunk();
      if (chkid == "DIRM")
        {
          GP<ByteStream> dirm = iff->get_bytestream();
          if (dirm->read8() & 0x80)
            return true;
        }
    }
  return false;
}

void 
djvuserver_file(GURL pathurl, bool bundled, bool download)
{
  GNativeString fname = pathurl.NativeFilename();
  struct stat statbuf;
  if (stat((const char *)fname, &statbuf) < 0)
    G_THROW(strerror(errno));
  
  // Is this a bundled file?
  if (is_djvu_file_bundled(pathurl) && !bundled)
    {
      // It is bundled
      GUTF8String id = pathurl.name();
      fprintf(stdout,"Location: %s/index.djvu", (const char*)id);
      if (g().querystring.length())
        fprintf(stdout,"?%s", (const char*)g().querystring);
      fprintf(stdout,"\n\n");
      return;
    }
  // Push the file
  if (download)
    headers(&statbuf, pathurl.fname());
  else
    headers(&statbuf);
  if (head) 
    return;
  fprintf(stdout,"\n");
  fflush(stdout);
  GP<ByteStream> in = ByteStream::create(pathurl,"rb");
  GP<ByteStream> out = ByteStream::get_stdout("ab");
  out->copy(*in);
}

void 
djvuserver_directory(GURL pathurl)
{
  GNativeString fname = pathurl.NativeFilename();
  struct stat statbuf;
  if (stat((const char *)fname, &statbuf) < 0)
    G_THROW(strerror(errno));
  // Find the DIRM chunk directly (save time)
  GP<ByteStream> temp;
  GP<ByteStream> bsin = ByteStream::create(pathurl,"rb");
  GP<DjVmDir> dir = DjVmDir::create();
  {
    GP<IFFByteStream> iffin = IFFByteStream::create(bsin);
    GUTF8String chkid;
    iffin->get_chunk(chkid);
    if (chkid != "FORM:DJVM")
      G_THROW( "This is not a multipage DjVu document" );
    while (iffin->get_chunk(chkid) && chkid!="DIRM")
      iffin->close_chunk();
    if (chkid != "DIRM")
      G_THROW( "This is not a new style bundled DjVu document" );
    temp = iffin->get_bytestream();
    dir->decode(temp);
    if (! dir->is_bundled())
      G_THROW( "This is not a bundled DjVu document" );
  }
  // Assemble index of indirect multipage file
  GP<ByteStream> bsdir = ByteStream::create();
  {
    GP<IFFByteStream> iff = IFFByteStream::create(bsdir);
    iff->put_chunk("FORM:DJVM",1);
    iff->put_chunk("DIRM");
    temp = iff->get_bytestream();
    dir->encode(temp, false, false);
    iff->close_chunk();
    iff->close_chunk();
  }
  // HTTP output
  statbuf.st_size = bsdir->tell();
  headers(&statbuf);
  if (head) 
    return;
  bsdir->seek(0);
  fprintf(stdout,"\n");
  fflush(stdout);
  GP<ByteStream> out = ByteStream::get_stdout("ab");
  out->copy(*bsdir);
}

void 
djvuserver_component(GURL pathurl, GUTF8String id)
{
  GNativeString fname = pathurl.NativeFilename();
  struct stat statbuf;
  if (stat((const char *)fname, &statbuf) < 0)
    G_THROW(strerror(errno));
  // Find the DIRM chunk directly (save time)
  GP<ByteStream> temp;
  GP<ByteStream> bsin = ByteStream::create(pathurl,"rb");
  GP<DjVmDir> dir = DjVmDir::create();
  {
    GP<IFFByteStream> iffin = IFFByteStream::create(bsin);
    GUTF8String chkid;
    iffin->get_chunk(chkid);
    if (chkid != "FORM:DJVM")
      G_THROW( "This is not a multipage DjVu document" );
    while (iffin->get_chunk(chkid) && chkid!="DIRM")
      iffin->close_chunk();
    if (chkid != "DIRM")
      G_THROW( "This is not a new style bundled DjVu document" );
    temp = iffin->get_bytestream();
    dir->decode(temp);
    if (! dir->is_bundled())
      G_THROW( "This is not a bundled DjVu document" );
  }
  // Find the file record
  GP<DjVmDir::File> frec = dir->id_to_file(id);
  if (!frec)
    G_THROW( "Cannot locate requested component file" );
  if (!frec->size || !frec->offset)
    G_THROW( "Corrupted DjVu directory" );

  // HTTP output
  statbuf.st_size = frec->size + 4;
  headers(&statbuf);
  if (head) 
    return;
  fprintf(stdout,"\n");
  fflush(stdout);
  GP<ByteStream> out = ByteStream::get_stdout("ab");
  out->writall("AT&T", 4);
  bsin->seek(frec->offset);
  out->copy(*bsin, frec->size);
}


bool
search_cgi_arg(const char *name)
{
  const char *s = g().querystring;
  int l = strlen(name);
  if (*s == '?')
    s += 1;
  while (*s)
    {
      if (! strncmp(s, name, l))
        if (s[l]=='&' || s[l]=='=' || s[l]==0)
          return true;
      while (*s && *s != '&')
        s += 1;
      if (*s == '&')
        s += 1;
    }
  return false;
}


int
main(int argc, char ** argv)
{
  DJVU_LOCALE;
  G_TRY 
    {
      // Obtain path
      bool bundled = false;
      bool download = false;
      if (argc == 1)
        {
          cgi = true;
          g().pathinfo = GNativeString(getenv("PATH_INFO"));
          g().pathtranslated = GNativeString(getenv("PATH_TRANSLATED"));
          if (! g().pathinfo)
            usage();
          if (! g().pathtranslated)
            G_THROW("No path information");
          g().requestmethod = GNativeString(getenv("REQUEST_METHOD"));
          g().querystring = GUTF8String(getenv("QUERY_STRING"));
          if (search_cgi_arg("bundled"))
            bundled = true;
          if (search_cgi_arg("download") || search_cgi_arg("bundle"))
            bundled = download = true;
        }
      else if (argc == 2)
        {
          cgi = false;
          g().pathtranslated = GNativeString(argv[1]);
          g().requestmethod = "GET";
        }
      if (! g().pathtranslated)
        usage();
      head = false;
      if (g().requestmethod == "HEAD")
        head = true;
      else if (g().requestmethod != "GET")
        G_THROW("Only serve HEAD and GET requests");
      // Do it.
      GURL pathurl = GURL::Filename::UTF8(g().pathtranslated);
      if (pathurl.is_file())
        {
          djvuserver_file(pathurl, bundled, download);
        }
      else
        {
          GUTF8String id = pathurl.name();
          pathurl = pathurl.base();
          if (! pathurl.is_file())
            G_THROW("File not found");
          if (id != "index" && id != "index.djvu")
            djvuserver_component(pathurl, id);
          else if (bundled)
            djvuserver_file(pathurl, bundled, download);
          else
            djvuserver_directory(pathurl);
        }
    }
  G_CATCH(ex)
    {
      if (cgi)
        {
          GUTF8String cause = DjVuMessageLite::LookUpUTF8(ex.get_cause());
          fprintf(stdout,"Status: 400 %s\n", (const char*)cause);
          fprintf(stdout,"Content-Type: text/html\n\n");
          fprintf(stdout,
                  "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n"
                  "<HTML><HEAD><TITLE>400 Error</TITLE></HEAD><BODY>\n"
                  "<H1>%s</H1>The requested URL '%s' cannot be processed.<P>\n"
#ifdef DJVULIBRE_VERSION
                  "<HR><ADDRESS>djvuserve/DjVuLibre-" DJVULIBRE_VERSION "</ADDRESS>\n"
#endif
                  "</BODY></HTML>\n",
                  (const char *) cause,
                  (const char *) g().pathinfo );
        }
      else
        {
          ex.perror();
        }
      exit(10);
    }
  G_ENDCATCH;
  // return code zero
  return 0;
}
