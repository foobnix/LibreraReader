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


/* Program djvutxt has been rewritten to use the ddjvuapi only.
 */

#ifdef HAVE_CONFIG_H
# include "config.h"
#endif

#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <locale.h>
#include <fcntl.h>
#include <errno.h>

#if defined(_WIN32) && !defined(__CYGWIN32__)
# include <mbctype.h>
#endif

#include "libdjvu/miniexp.h"
#include "libdjvu/ddjvuapi.h"


/* Some day we'll redo i18n right. */
#ifndef i18n
# define i18n(x) (x)
# define I18N(x) (x)
#endif


/* Options */
const char *inputfilename = 0;
const char *outputfilename = 0;
const char *detail = 0;
const char *pagespec = 0;
int escape = 0;

ddjvu_context_t *ctx;
ddjvu_document_t *doc;


void
handle(int wait)
{
  const ddjvu_message_t *msg;
  if (!ctx)
    return;
  if (wait)
    msg = ddjvu_message_wait(ctx);
  while ((msg = ddjvu_message_peek(ctx)))
    {
      switch(msg->m_any.tag)
        {
        case DDJVU_ERROR:
          fprintf(stderr,"djvutxt: %s\n", msg->m_error.message);
          if (msg->m_error.filename)
            fprintf(stderr,"djvutxt: '%s:%d'\n", 
                    msg->m_error.filename, msg->m_error.lineno);
          exit(10);
        default:
          break;
        }
      ddjvu_message_pop(ctx);
    }
}


void 
die(const char *fmt, ...)
{
  /* Handling messages might give a better error message */
  handle(FALSE);
  /* Print */
  va_list args;
  fprintf(stderr,"djvutxt: ");
  va_start(args, fmt);
  vfprintf(stderr, fmt, args);
  va_end(args);
  fprintf(stderr,"\n");
  /* Terminates */
  exit(10);
}


void
dopage(int pageno)
{
  miniexp_t r = miniexp_nil;
  const char *lvl = (detail) ? detail : "page";
  while ((r = ddjvu_document_get_pagetext(doc,pageno-1,lvl))==miniexp_dummy)
    handle(TRUE);
  if (detail)
    {
      miniexp_io_t io;
      miniexp_io_init(&io);
#ifdef miniexp_io_print7bits
      int flags = (escape) ? miniexp_io_print7bits : 0;
      io.p_flags = &flags;
#else
      io.p_print7bits = &escape;
#endif
      miniexp_pprint_r(&io, r, 72);
    }
  else if ((r = miniexp_nth(5, r)) && miniexp_stringp(r))
    {
      const char *s = miniexp_to_str(r); 
      if (! escape)
        fputs(s, stdout);
      else
        {
          unsigned char c;
          while ((c = *(unsigned char*)s++))
            {
              bool esc = false;
              if (c == '\\' || c >= 0x7f)
                esc = true; /* non-ascii */
              if (c < 0x20 && !strchr("\013\035\037\012", c))
                esc = true; /* non-printable other than separators */
              if (esc)
                printf("\\%03o", c);
              else
                putc(c, stdout);
            }
        }
      fputs("\n\f", stdout);
    }
}


void
parse_pagespec(const char *s, int max_page, void (*dopage)(int))
{
  static const char *err = I18N("invalid page specification: %s");
  int spec = 0;
  int both = 1;
  int start_page = 1;
  int end_page = max_page;
  int pageno;
  char *p = (char*)s;
  while (*p)
    {
      spec = 0;
      while (*p==' ')
        p += 1;
      if (! *p)
        break;
      if (*p>='0' && *p<='9') {
        end_page = strtol(p, &p, 10);
        spec = 1;
      } else if (*p=='$') {
        spec = 1;
        end_page = max_page;
        p += 1;
      } else if (both) {
        end_page = 1;
      } else {
        end_page = max_page;
      }
      while (*p==' ')
        p += 1;
      if (both) {
        start_page = end_page;
        if (*p == '-') {
          p += 1;
          both = 0;
          continue;
        }
      }
      both = 1;
      while (*p==' ')
        p += 1;
      if (*p && *p != ',')
        die(i18n(err), s);
      if (*p == ',')
        p += 1;
      if (! spec)
        die(i18n(err), s);
      if (end_page < 0)
        end_page = 0;
      if (start_page < 0)
        start_page = 0;
      if (end_page > max_page)
        end_page = max_page;
      if (start_page > max_page)
        start_page = max_page;
      if (start_page <= end_page)
        for(pageno=start_page; pageno<=end_page; pageno++)
          (*dopage)(pageno);
      else
        for(pageno=start_page; pageno>=end_page; pageno--)
          (*dopage)(pageno);
    }
  if (! spec)
    die(i18n(err), s);
}


void
usage()
{
#ifdef DJVULIBRE_VERSION
  fprintf(stderr, "DDJVU --- DjVuLibre-" DJVULIBRE_VERSION "\n");
#endif
  fprintf(stderr, "%s",
    i18n("DjVu text extraction utility\n\n"
         "Usage: djvutxt [options] <djvufile> [<outputfile>]\n\n"
         "Options:\n"
         " -page=PAGESPEC    Selects page(s) to be decoded.\n"
         " -detail=KEYWORD   Outputs S-expression with the text location.\n"
         "                   The optional keyword <page>, <region>, <para>,\n"
         "                   <line>,<word>, or <char> specify the finest\n"
         "                   level of detail. Default is <char>.\n"
         " -escape           Output octal escape sequences for all\n"
         "                   non ASCII UTF-8 characters.\n\n") );
  /* Terminate */
  exit(10);
}


int
main(int argc, char **argv)
{
  int i;
#if defined(_WIN32) && !defined(__CYGWIN32__)
  _setmbcp(_MB_CP_OEM);
#endif
  /* Parse options */
  for (i=1; i<argc; i++)
    {
      char *s = argv[i];
      if (s[0] == '-' && s[1] != 0)
        {
          char buf[32];
          const char *opt = s;
          const char *arg = strchr(opt, '=');
          if (*opt == '-')
            opt += 1;
          if (*opt == '-')
            opt += 1;
          if (arg)
            {
              int l = arg - opt;
              if (l > (int)sizeof(buf) - 1)
                l = sizeof(buf) - 1;
              strncpy(buf, opt, l);
              buf[l] = 0;
              opt = buf;
              arg += 1;
            }
          
          if (!strcmp(opt,"page") || 
              !strcmp(opt,"pages") )
            {
              if (!arg && i<argc)
                arg = argv[i++];
              if (!arg)
                die(i18n("option %s needs an argument."), s);
              if (pagespec)
                fprintf(stderr,i18n("warning: duplicate option --page=...\n"));
              pagespec = arg;
            }
          else if (!strcmp(opt, "detail"))
            {
              if (!arg)
                arg = "char";
              if (detail)
                fprintf(stderr,i18n("warning: duplicate option --detail.\n"));
              detail = arg;
            }
          else if (!strcmp(opt, "escape") && !arg)
            escape = 1;
          else
            die(i18n("unrecognized option %s."), s);
        }
      else if (!inputfilename)
        inputfilename = s;
      else if (! outputfilename)
        outputfilename = s;
      else
        usage();
    }
  
  /* Defaults */
  if (! inputfilename)
    usage();
  if (outputfilename)
    if (! freopen(outputfilename, "w", stdout))
      die(i18n("cannot open output file %s."), outputfilename);
  if (! pagespec)
    pagespec = "1-$";
  
  /* Create context and document */
  if (! (ctx = ddjvu_context_create(argv[0])))
    die(i18n("Cannot create djvu context."));
  if (! (doc = ddjvu_document_create_by_filename(ctx, inputfilename, TRUE)))
    die(i18n("Cannot open djvu document '%s'."), inputfilename);
  while (! ddjvu_document_decoding_done(doc))
    handle(TRUE);
  
  /* Process all pages */
  i = ddjvu_document_get_pagenum(doc);
  parse_pagespec(pagespec, i, dopage);
  
  /* Close */
  if (doc)
    ddjvu_document_release(doc);
  if (ctx)
    ddjvu_context_release(ctx);

  /* Return */
  minilisp_finish();  
  return 0;
}

