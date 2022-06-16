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

#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <fcntl.h>
#include <errno.h>

#include "libdjvu/ddjvuapi.h"

#if defined(_WIN32) || defined(__CYGWIN32__)
# include <io.h>
#endif
#if defined(_WIN32) && !defined(__CYGWIN32__)
# include <mbctype.h>
#endif

/* Some day we'll redo i18n right. */
#ifndef i18n
# define i18n(x) (x)
# define I18N(x) (x)
#endif

bool verbose = false;
bool tryhelp = false;
ddjvu_context_t *ctx;
ddjvu_document_t *doc;
ddjvu_job_t *job;

void
progress(int p)
{
  if (verbose)
    {
      int i=0;
      char buffer[52];
      for (; p>0; p-=2)
        buffer[i++]='#';
      for (; i<50;)
        buffer[i++]=' ';
      buffer[i] = 0;
      fprintf(stderr,"\r[%s]",buffer);
    }        
}

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
          if (verbose)
            fprintf(stderr,"\n");
          fprintf(stderr,"djvups: %s\n", msg->m_error.message);
          if (msg->m_error.filename)
            fprintf(stderr,"djvups: '%s:%d'\n", 
                    msg->m_error.filename, msg->m_error.lineno);
          if (tryhelp)
            fprintf(stderr,"djvups: %s\n", i18n("Try option --help."));
          exit(10);
        case DDJVU_PROGRESS:
          if (verbose)
            progress(msg->m_progress.percent);
          break;
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
  tryhelp = true;
  handle(FALSE);
  /* Print */
  va_list args;
  fprintf(stderr,"ddjvu: ");
  va_start(args, fmt);
  vfprintf(stderr, fmt, args);
  va_end(args);
  fprintf(stderr,"\n");
  /* Terminates */
  exit(10);
}

const char *options[] = {
  I18N("-verbose"),
  I18N("-page=<pagelists>                   (default: print all)"),
  I18N("-format=<ps|eps>                    (default: ps)"),
  I18N("-level=<1|2|3>                      (default: 2)"),
  I18N("-orient=<auto|portrait|landscape>   (default: auto)"),
  I18N("-mode=<color|bw|fore|back>          (default: color)"),
  I18N("-zoom=<auto|25...2400)              (default: auto)"),
  I18N("-color=<yes|no>                     (default: yes)"),
  I18N("-gray                               (same as -color=no)"),
  I18N("-colormatch=<yes|no>                (default: yes)"),
  I18N("-gamma=<0.3...5.0>                  (default: 2.2)"),
  I18N("-copies=<1...999999>                (default: 1)"),
  I18N("-frame=<yes|no>                     (default: no)"),
  I18N("-cropmarks=<yes|no>                 (default: no)"),
#ifdef THIS_THING_DOES_NOT_WORK_WITH_UTF8_STRINGS
  I18N("-text=<yes|no>                      (default: no)"),
#endif
  I18N("-booklet=<no|recto|verso|yes>       (default: no)"),
  I18N("-bookletmax=<n>                     (default: 0)"),
  I18N("-bookletalign=<n>                   (default: 0)"),
  I18N("-bookletfold=<n>[+<m>]              (default: 18+200)"),
  NULL };


void
usage(void)
{
  int i;
#ifdef DJVULIBRE_VERSION
  fprintf(stderr, "DJVUPS --- DjVuLibre-" DJVULIBRE_VERSION "\n");
#endif
  fprintf(stderr, "%s\n",
          i18n("DjVu to PostScript conversion utility\n\n"
               "Usage: djvups [<options>] [<infile.djvu> [<outfile.ps>]]\n"
               "Options:\n  -help"));
  for(i=0; options[i]; i++)
    fprintf(stderr, "  %s\n", i18n(options[i]));
  fprintf(stderr,"\n");
  exit(1);
}

int 
check_option(char *s)
{
  int i;
  for (i=0; options[i]; i++)
    {
      int n = 0;
      const char *p = options[i];
      while (p[n] && p[n]!='=' && p[n]!=' ')
        n += 1;
      if (p[n]=='=' && !strncmp(s, p, n+1))
        return 1;
      if (p[n]!='=' && !strncmp(s, p, n) && !s[n])
        return 1;        
    }
  // compatibility aliases 
  if (!strcmp(s,"-grayscale") ||
      !strncmp(s,"-pages=",7) ||
      !strncmp(s,"-orientation=",13) ||
      !strncmp(s,"-srgb=",6) )
    return 1;
  return 0;
}

int
main(int argc, char **argv)
{

  int i;
  int optc = 0;
  char **optv;
  const char *infile = 0;
  const char *outfile = 0;
  FILE *fout;
#if defined(_WIN32) && !defined(__CYGWIN32__)
  _setmbcp(_MB_CP_OEM);
#endif
  /* Sort options */
  if (! (optv = (char**)malloc(argc*sizeof(char*))))
    die(i18n("Out of memory"));
  for (i=1; i<argc; i++)
    {
      char *s = argv[i];
      if (s[0]=='-' && s[1]=='-')
        s = s+1;
      if (!strcmp(s,"-verbose"))
        verbose = true;
      else if (check_option(s))
        optv[optc++] = s;
      else if (s[0]=='-' && s[1])
        usage();
      else if (s[0] && !infile)
        infile = s;
      else if (s[0] && !outfile)
        outfile = s;
      else
        die(i18n("Incorrect arguments. Try option --help."));
    }
  if (! infile)
    infile = "-";
  if (! outfile)
    outfile = "-";
  /* Open document */
  if (! (ctx = ddjvu_context_create(argv[0])))
    die(i18n("Cannot create djvu context."));
  if (! (doc = ddjvu_document_create_by_filename(ctx, infile, TRUE)))
    die(i18n("Cannot open djvu document '%s'."), infile);
  while (! ddjvu_document_decoding_done(doc))
    handle(TRUE);
  /* Open output file */
  if (! strcmp(outfile,"-")) 
    {
      fout = stdout;
#if defined(__CYGWIN32__)
      setmode(fileno(fout), O_BINARY);
#elif defined(_WIN32)
      _setmode(_fileno(fout), _O_BINARY);
#endif
    } 
  else if (! (fout = fopen(outfile, "wb")))
    die(i18n("Cannot open output file '%s'."), outfile);
  /* Create printing job */
  if (! (job = ddjvu_document_print(doc, fout, optc, optv)))
    die(i18n("Cannot create PostScript conversion job."));
  /* Wait until completion and cleanup */
  while (! ddjvu_job_done(job))
    handle(TRUE);
  if (verbose)
    fprintf(stderr,"\n");
  /* Make sure we get error messages */
  tryhelp = false;
  if (ddjvu_job_error(job))
    handle(FALSE);
  if (ddjvu_job_error(job))
    die(i18n("PostScript conversion job failed."));
  /* Close */
  fclose(fout);
  if (job)
    ddjvu_job_release(job);
  if (doc)
    ddjvu_document_release(doc);
  if (ctx)
    ddjvu_context_release(ctx);
  return 0;
}
