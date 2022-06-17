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

/** @name djvmcvt

    {\bf Synopsis}\\
    \begin{verbatim}
        djvmcvt -b[undled] <doc_in.djvu> <doc_out.djvu>

	or
	
	djvmcvt -i[ndirect] <doc_in.djvu> <dir_out> <idx_fname.djvu>
    \end{verbatim}

    {\bf Description} ---
    File #"djvmcvt.cpp"# and the program #djvmcvt# serve the purpose of
    convertion of obsolete DjVu documents into the new formats. The program
    can also read documents in the new formats, so you can use it to
    perform conversion between #BUNDLED# and #INDIRECT# formats. This is a
    simple illustration of the capabilities of \Ref{DjVuDocument} class.

    As a matter of fact, there are two ways to make conversion between
    different formats:
    \begin{enumerate}
       \item If the input format is one of obsolete formats (#OLD_BUNDLED#
             or #OLD_INDEXED#) then the conversion can be done by
	     \Ref{DjVuDocument} only.
       \item If the input format is one of new formats (#BUNDLED# or
             #INDIRECT#) then the best candidate to perform conversion
	     is \Ref{DjVmDoc} class. It will do it at the lowest possible
	     with the least expenses.
    \end{enumerate}
    
    {\bf Arguments} ---
    Depending on the output format, the number and types of arguments
    differ. The second argument though (#<doc_in.djvu>#) is the same in both
    cases, and depending on the format of input document, it means:
    \begin{itemize}
       \item {\bf OLD_BUNDLED} format: just name of the document
       \item {\bf OLD_INDEXED} format: name of any page of the document
       \item {\bf BUNDLED} format: name of the document
       \item {\bf INDIRECT} format: name of the top-level file with the
             list of all pages of the document.
    \end{itemize}.

    So, in order to do conversion choose one of syntaxes below:
    \begin{itemize}
       \item To create a new {\em BUNDLED} document

             #djvmcvt -b[undled] <doc_in.djvu> <doc_out.djvu>#

	     This will read the document referenced by #<doc_in.djvu># as
	     descrived above, will convert it into the #BUNDLED#
	     format and will save the results into the #<doc_out.djvu># file.
	     
       \item To create a new {\em INDIRECT} document

             #djvmcvt -i[ndirect] <doc_in.djvu> <dir_out> <idx_fname.djvu>#

	     This will read the input document referenced by #<doc_in.djvu>#
	     as described above, will convert it into the #INDIRECT#
	     format and will save it into the #<dir_out># directory. Since
	     DjVu multipage documents in the #INDIRECT# formats are
	     represented by a bunch of files, you have to specify a directory
	     name where all of the files will be saved. In addition to these
	     files the program will also create a top-level file named
	     #<idx_fname.djvu># with the list of all pages and components
	     composing the given DjVu document. Whenever you need to open
	     this document later, open this top-level file.
    \end{itemize}
	     
    @memo
    DjVu multipage document converter.
    @author
    Andrei Erofeev <eaf@geocities.com>
*/


#include "DjVuDocument.h"
#include "DjVmDoc.h"
#include "ByteStream.h"
#include "GOS.h"
#include "DjVuMessage.h"
#include "debug.h"
#include "common.h"

static const char * progname;

static void Usage(void)
{
   DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
     "DJVMCVT --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
     "DjVu multipage document conversion utility\n"
     "\n"
     "Usage:\n"
     "\n"
     "  To convert any DjVu multipage document into the new BUNDLED format:\n"
     "	  %s -b[undled] <doc_in.djvu> <doc_out.djvu>\n"
     "	  where <doc_out.djvu> is the name of the output file.\n"
     "\n"
     "  To convert any DjVu multipage document into the new INDIRECT format:\n"
     "	  %s -i[ndirect] <doc_in.djvu> <dir_out> <idx_fname.djvu>\n"
     "	  where <dir_out> is the name of the output directory, and\n"
     "	  <idx_fname.djvu> is the name of the top-level document index file.\n"
     "\n"
     "The <doc_in.djvu> specifies the document to be converted.\n"
     "For OLD_BUNDLED and BUNDLED formats, this is the name of the document file.\n"
     "For INDIRECT format, this is the name of the top-level index file.\n"
     "For OLD_INDEXED format, this is the name of any page file.\n"
     "\n", progname, progname);
}

static void
do_bundled(GArray<GUTF8String> &argv)
      // <progname> -b[undled] <file_in> <file_out>
{
   const int argc=argv.hbound()+1;
   if (argc!=4) { Usage(); exit(1); }
   const GURL::Filename::UTF8 url2(argv[2]);
   const GURL::Filename::UTF8 url3(argv[3]);
   GP<DjVuDocument> doc = DjVuDocument::create_wait(url2);
   GP<ByteStream> str=ByteStream::create(url3, "wb");
   doc->write(str);
}

static void
do_indirect(GArray<GUTF8String> &argv)
      // <progname> -i[ndirect] <file_in> <dir_out> <idx_fname>
{
   const int argc=argv.hbound()+1;
   if (argc!=5) { Usage(); exit(1); }
   const GURL::Filename::UTF8 url2(argv[2]);
   GP<DjVuDocument> doc = DjVuDocument::create_wait(url2);
   const GURL::Filename::UTF8 url3(argv[3]);
   doc->expand(url3, argv[4]);
}

int 
main(int argc, char ** argv)
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);
  progname=dargv[0]=GOS::basename(dargv[0]);

  if (argc<2) { Usage(); exit(1); }

   bool bundled=true;
   G_TRY {
      if (!dargv[1].cmp("-b", 2)) bundled=true;
      else if (!dargv[1].cmp("-i", 2)) bundled=false;
      else { Usage(); exit(1); }

      if (bundled) do_bundled(dargv);
      else do_indirect(dargv);
   } G_CATCH(exc) {
      DjVuPrintErrorUTF8("%s\n", exc.get_cause());
      exit(1);
   } G_ENDCATCH;

   exit(0);
#ifdef _WIN32
   return 0;
#endif
}
