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

/** @name djvm

    {\bf Synopsis}\\
    \begin{verbatim}
        djvm [options] <djvu_doc_name> [djvufiles] [pagenum]
    \end{verbatim}

    {\bf Description} ---
    File #"djvm.cpp"# and program #djvm# illustrate how class \Ref{DjVuDocEditor}
    can be used to create and modify DjVu multipage documents. The program
    demonstrates how to pack several DjVu single-page files together,
    how to remove pages from a multipage document or how to insert new.
    
    {\bf Arguments} ---
    Depending on the task to be performed, the number and types of arguments
    differ:
    \begin{itemize}
       \item To create a new document

             #djvm -c[reate] <doc.djvu> <page_1.djvu> ... <page_n.djvu>#

	     This will package pages represented by files #<page_1.djvu>#
	     ... #<page_n.djvu># into #BUNDLED# multipage DjVu document
	     #<doc.djvu># (see \Ref{DjVuDocument} for the explanation of
	     the #BUNDLED# format). The specified page files may include other
	     files (by means of #INCL# chunks). They will also be packed
	     into the document.

       \item To insert a page

       	     #djvm -i[nsert] <doc.djvu> <page.djvu> [<page_num>]#

	     This will insert page represented by file #<page.djvu># into
	     document #<doc.djvu># as page number #<page_num># (page
	     numbering starts from #1#). Negative or missing #<page_num>#
	     means to append the page.

	     The #<page.djvu># file can actually be another
	     multipage DjVu document. In this cases, all pages from that
	     document will be inserted into #<doc.djvu># starting from
	     page #<page_num>#.

       \item To delete a page

             #djvm -d[elete] <doc.djvu> <page_num>#

	     This will remove page number #<page_num># from document
	     #<doc.djvu>#. If there is only one page left in #<doc.djvu>#
	     after deletion, it will automatically be converted to a
	     single page DjVu file format. Page numbering starts from #1#.

       \item To view document contents

             #djvm -l[ist] <doc.djvu>#

	     This will list all files composing the given document #<doc.djvu>#.
	     The files list includes the names of page files plus names
	     of any files included into the page files by means of
	     #INCL# chunk.
    \end{itemize}

    @memo
    DjVu multipage documents creator.
    @author
    Andrei Erofeev <eaf@geocities.com>
*/
//@{
//@}

#include "GException.h"
#include "DjVuDocEditor.h"
#include "GOS.h"
#include "DjVuMessage.h"
#include "common.h"

static const char * progname;

static void
usage(void)
{
   DjVuPrintErrorUTF8(
#ifdef DJVULIBRE_VERSION
           "DJVM --- DjVuLibre-" DJVULIBRE_VERSION "\n"
#endif
           "DjVu multipage document manipulation utility\n"
           "\n"
           "Usage:\n"
           "   To compose a multipage document:\n"
           "      %s -c[reate] <doc.djvu> <page_1.djvu> ... <page_n.djvu>\n"
           "      where <doc.djvu> is the name of the BUNDLED document to be\n"
           "      created, <page_n.djvu> are the names of the page files to\n"
           "      be packed together.\n"
           "\n"
           "   To insert a new page into an existing document:\n"
           "      %s -i[nsert] <doc.djvu> <page.djvu> [<page_num>]\n"
           "      where <doc.djvu> is the name of the BUNDLED DjVu document to be\n"
           "      modified, <page.djvu> is the name of the single-page DjVu document\n"
           "      file to be inserted as page <page_num> (page numbers start from 1).\n"
           "      Negative or omitted <page_num> means to append the page.\n"
           "      <page.djvu> can be another multipage DjVu document, in which case\n"
           "      all pages of that document will be inserted into <doc.djvu>\n"
           "      starting starting at page <page_num>\n"
           "\n"
           "   To delete a page from an existing document:\n"
           "      %s -d[elete] <doc.djvu> <page_num>\n"
           "      where <doc.djvu> is the name of the docyment to be modified\n"
           "      and <page_num> is the number of the page to be deleted\n"
           "\n"
           "   To list document contents:\n"
           "      %s -l[ist] <doc.djvu>\n"
           "\n"
           "Pages being inserted may reference other files by means of INCL chunks.\n"
           "Moreover, files shared between pages will be stored into the document\n"
           "only once.\n"
           "\n", progname, progname, progname, progname );
}

static void
create(GArray<GUTF8String> &argv)
      // djvm -c[reate] <doc.djvu> <page_1.djvu> ... <page_n.djvu>
      // doc.djvu will be overwritten
{
   const int argc=argv.hbound()+1;
   if (argc<4) { usage(); exit(1); }

      // Initialize the DjVuDocEditor class
   GP<DjVuDocEditor> doc=DjVuDocEditor::create_wait();

      // Insert pages
   GList<GURL> list;
   for(int i=3;i<argc;i++)
      list.append(GURL::Filename::UTF8(argv[i]));
   doc->insert_group(list);

   const GURL::Filename::UTF8 url(argv[2]);
      // Save in BUNDLED format
   doc->save_as(url, true);
}

static void
insert(GArray<GUTF8String> &argv)
      // djvm -i[nsert] <doc.djvu> <page.djvu> <page_num>
{
   const int argc=argv.hbound()+1;
   if (argc!=4 && argc!=5) { usage(); exit(1); }

      // Initialize DjVuDocEditor class
   const GURL::Filename::UTF8 url(argv[2]);
   GP<DjVuDocEditor> doc=DjVuDocEditor::create_wait(url);

      // Insert page
   int page_num=-1;
   if (argc==5) page_num=atoi(argv[4])-1;
   doc->insert_page(GURL::Filename::UTF8(argv[3]), page_num);

      // Save the document
   doc->save();
}

static void
del(GArray<GUTF8String> &argv)
      // djvm -d[elete] <doc.djvu> <page_num>
{
   const int argc=argv.hbound()+1;
   if (argc!=4) { usage(); exit(1); }

      // Initialize DjVuDocEditor class
   const GURL::Filename::UTF8 url(argv[2]);
   GP<DjVuDocEditor> doc=DjVuDocEditor::create_wait(url);

      // Delete the page
   int page_num=atoi(argv[3])-1;
   if (page_num<0)
   {
     DjVuPrintErrorUTF8("%s","Page number must be positive.\n");
     exit(1);
   }
   doc->remove_page(page_num);

      // Save the document
   doc->save();
}

static void
list(GArray<GUTF8String> &argv)
      // djvm -l[ist] <doc.djvu>
{
   const int argc=argv.hbound()+1;
   if (argc!=3) { usage(); exit(1); }

   const GURL::Filename::UTF8 url(argv[2]);
   GP<DjVmDoc> doc=DjVmDoc::create();
   doc->read(url);
   
   GP<DjVmDir> dir=doc->get_djvm_dir();
   if (dir)
   {
      GPList<DjVmDir::File> files_list=dir->get_files_list();
      printf("Size     Type        Name\n");
      printf("--------------------------\n");
      for(GPosition pos=files_list;pos;++pos)
      {
	 GP<DjVmDir::File> file=files_list[pos];
	 printf("%6d   ", file->size);
	 if (file->is_page())
	 {
	    char buffer[128];
	    sprintf(buffer, "PAGE #%d", file->get_page_num()+1);
	    printf("%s", buffer);
	    for(int i=strlen(buffer);i<9;i++)
	       putchar(' ');
	 } else if (file->is_include())
         {
	    printf("%s", "INCLUDE  ");
	 } else if (file->is_shared_anno())
         {
	    printf("%s", "SHARED_ANNO  ");
	 }else if (file->is_thumbnails())
         {
	    printf("%s", "THUMBNAIL");
	 } else
         {
           printf("%s", "UNKNOWN  ");
         }
	 printf("   %s\n", (const char *) file->get_save_name());
      }
   }
}

int
main(int argc, char ** argv)
{
  DJVU_LOCALE;
  GArray<GUTF8String> dargv(0,argc-1);
  for(int i=0;i<argc;++i)
    dargv[i]=GNativeString(argv[i]);
  progname=dargv[0]=GOS::basename(dargv[0]);
   
   G_TRY {
      if (argc<2) { usage(); exit(1); }
      if (!dargv[1].cmp("-c", 2)) create(dargv);
      else if (!dargv[1].cmp("-i", 2)) insert(dargv);
      else if (!dargv[1].cmp("-d", 2)) del(dargv);
      else if (!dargv[1].cmp("-l", 2)) list(dargv);
      else { usage(); exit(1); }
   } G_CATCH(exc) {
      exc.perror();
      exit(1);
   } G_ENDCATCH;
   return 0; 
}

