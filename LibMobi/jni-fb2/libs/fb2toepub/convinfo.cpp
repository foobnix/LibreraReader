//
//  Copyright (C) 2010 Alexey Bobkov
//
//  This file is part of Fb2toepub converter.
//
//  Fb2toepub converter is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  Fb2toepub converter is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with Fb2toepub converter.  If not, see <http://www.gnu.org/licenses/>.
//


#include "hdr.h"

#include "scanner.h"
#include "converter.h"
#include "streamconv.h"
#include "streamzip.h"
#include <sstream>
#include <vector>
#include <sys/types.h>
#include <sys/stat.h>

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
static void PrintInfo(const String &name, const String &value)
{
    if(!value.empty())
    {
        std::vector<char> buf;
        LexScanner::Decode(value.c_str(), &buf, true, true);
        printf("%s=%s\n", name.c_str(), &buf[0]);
    }
}

//-----------------------------------------------------------------------
class ConverterInfo : public Object, Noncopyable
{
public:
    ConverterInfo (const String &in) : in_(in) {}

    void Scan()
    {
        std::size_t size = 0;
        {
            struct stat st;
            ::stat(in_.c_str(), &st);
            size = st.st_size;
        }

        Ptr<InStm> pin = CreateInUnicodeStm(CreateUnpackStm(in_.c_str()));
        s_ = CreateScanner(pin);
        s_->SkipXMLDeclaration();
        FictionBook();

        // author(s)
        String authors;
        {
            strvector::const_iterator cit = authors_.begin(), cit_end = authors_.end();
            for(; cit < cit_end; ++cit)
                authors = Concat(authors, ", ", *cit);
        }

        PrintInfo("author", authors);
        PrintInfo("title", title_);
        PrintInfo("date", title_info_date_);

        {
            std::ostringstream sizeStr;
            sizeStr << size;
            PrintInfo("size", sizeStr.str());
        }

        // sequence, number
        if(!sequences_.empty())
        {
            PrintInfo("sequence", sequences_[0].first);
            PrintInfo("number", sequences_[0].second);

            // if more than one sequence?
            /*
            for(int i = 1; i < sequences_.size(); ++i)
            {
                std::ostringstream index;
                index.width(4);
                index.fill('0');
                index << i;
                PrintInfo(String("sequence") + index.str(), sequences_[i].first);
                PrintInfo(String("number") + index.str(), sequences_[i].second);
            }
            */
        }
    }

private:
    String                  in_;
    Ptr<LexScanner>         s_;
    String                  title_, lang_, title_info_date_, isbn_;
    strvector               authors_;

    typedef std::vector<std::pair<String, String> > seqvector;
    seqvector               sequences_;

    // FictionBook elements
    void FictionBook            ();
    //void a                      ();
    //void annotation             ();
    void author                 ();
    //void binary                 ();
    //void body                   ();
    //void book_name              ();
    void book_title             ();
    //void cite                   ();
    //void city                   ();
    //void code                   ();
    //void coverpage              ();
    //void custom_info            ();
    //void date                   ();
    String date__textonly       ();
    void description            ();
    //void document_info          ();
    //void email                  ();
    //void emphasis               ();
    //void empty_line             ();
    //void epigraph               ();
    //void first_name             ();
    //void genre                  ();
    //void history                ();
    //void home_page              ();
    //void id                     ();
    //void image                  ();
    String isbn                 ();
    //void keywords               ();
    void lang                   ();
    //void last_name              ();
    //void middle_name            ();
    //void nickname               ();
    //void output_document_class  ();
    //void output                 ();
    //void p                      ();
    //void part                   ();
    //void poem                   ();
    //void program_used           ();
    void publish_info           ();
    //void publisher              ();
    //void section                ();
    void sequence               ();
    //void src_lang               ();
    //void src_ocr                ();
    //void src_title_info         ();
    //void src_url                ();
    //void stanza                 ();
    //void strikethrough          ();
    //void strong                 ();
    //void style                  ();
    //void stylesheet             ();
    //void sub                    ();
    //void subtitle               ();
    //void sup                    ();
    //void table                  ();
    //void td                     ();
    //void text_author            ();
    //void th                     ();
    //void title                  ();
    void title_info             ();
    //void tr                     ();
    //void translator             ();
    //void v                      ();
    //void version                ();
    //void year                   ();
};

//-----------------------------------------------------------------------
void ConverterInfo::FictionBook()
{
    s_->BeginNotEmptyElement("FictionBook");

    //<stylesheet>
    s_->SkipAll("stylesheet");
    //</stylesheet>

    //<description>
    description();
    //</description>
}

//-----------------------------------------------------------------------
void ConverterInfo::author()
{
    s_->BeginNotEmptyElement("author");

    String author;
    if(s_->IsNextElement("first-name"))
    {
        author = s_->SimpleTextElement("first-name");

        if(s_->IsNextElement("middle-name"))
            author = Concat(author, " ", s_->SimpleTextElement("middle-name"));

        author = Concat(author, " ", s_->SimpleTextElement("last-name"));
    }
    else if(s_->IsNextElement("nickname"))
        author = s_->SimpleTextElement("nickname");
    else
        s_->Error("<first-name> or <nickname> expected");

    authors_.push_back(author);
    s_->SkipRestOfElementContent();
}

//-----------------------------------------------------------------------
void ConverterInfo::book_title()
{
    title_ = s_->SimpleTextElement("book-title");
}

//-----------------------------------------------------------------------
String ConverterInfo::date__textonly()
{
    if(!s_->BeginElement("date"))
        return "";

    String text;
    SetScannerDataMode setDataMode(s_);
    if(s_->LookAhead().type_ == LexScanner::DATA)
        text = s_->GetToken().s_;
    s_->EndElement();
    return text;
}

//-----------------------------------------------------------------------
void ConverterInfo::description()
{
    s_->BeginNotEmptyElement("description");

    //<title-info>
    title_info();
    //</title-info>

    //<src-title-info>
    s_->SkipIfElement("src-title-info");
    //</src-title-info>

    //<document-info>
    s_->CheckAndSkipElement("document-info");
    //</document-info>

    //<publish-info>
    if(s_->IsNextElement("publish-info"))
        publish_info();
    //</publish-info>

    s_->SkipRestOfElementContent(); // skip rest of <description>
}

//-----------------------------------------------------------------------
String ConverterInfo::isbn()
{
    if(!s_->BeginElement("isbn"))
        return "";

    String text;
    SetScannerDataMode setDataMode(s_);
    if(s_->LookAhead().type_ == LexScanner::DATA)
        text = s_->GetToken().s_;
    s_->EndElement();
    return text;
}

//-----------------------------------------------------------------------
void ConverterInfo::lang()
{
    lang_ = s_->SimpleTextElement("lang");
}

//-----------------------------------------------------------------------
void ConverterInfo::publish_info()
{
    if(!s_->BeginElement("publish-info"))
        return;

    //<book-name>
    s_->SkipIfElement("book-name");
    //</book-name>

    //<publisher>
    s_->SkipIfElement("publisher");
    //</publisher>

    //<city>
    s_->SkipIfElement("city");
    //</city>

    //<year>
    s_->SkipIfElement("year");
    //</year>

    //<isbn>
    if(s_->IsNextElement("isbn"))
        isbn_ = isbn();
    //</isbn>

    s_->SkipRestOfElementContent(); // skip rest of <publish-info>
}

//-----------------------------------------------------------------------
void ConverterInfo::sequence()
{
    AttrMap attrmap;
    bool notempty = s_->BeginElement("sequence", &attrmap);

    String name = attrmap["name"];
    if(!name.empty())
        sequences_.push_back(seqvector::value_type(name, attrmap["number"]));

    if(notempty)
        s_->EndElement();
}

//-----------------------------------------------------------------------
void ConverterInfo::title_info()
{
	s_->BeginNotEmptyElement("title-info");

	for (LexScanner::Token t = s_->LookAhead(); t.type_ == LexScanner::START; t = s_->LookAhead())
	{
		if (!t.s_.compare("genre")) {
			s_->CheckAndSkipElement("genre");
			s_->SkipAll("genre");
		}
		else if (!t.s_.compare("author")) {
			author();
		}
		else if (!t.s_.compare("book-title")) {
			book_title();
		}
		else if (!t.s_.compare("date")) {
			title_info_date_ = date__textonly();
		}
		else if (!t.s_.compare("lang")) {
			lang();
		}
		else if (!t.s_.compare("sequence")) {
			sequence();
		}
		else {
			s_->SkipElement();
		}
	}
	s_->EndElement();

    ////<genre>
    //s_->CheckAndSkipElement("genre");
    //s_->SkipAll("genre");
    ////</genre>

    ////<author>
    //do
    //    author();
    //while(s_->IsNextElement("author"));
    ////<author>

    ////<book-title>
    //book_title();
    ////</book-title>

    ////<annotation>
    //s_->SkipIfElement("annotation");
    ////</annotation>

    ////<keywords>
    //s_->SkipIfElement("keywords");
    ////</keywords>

    ////<date>
    //if(s_->IsNextElement("date"))
    //    title_info_date_ = date__textonly();
    ////<date>

    ////<coverpage>
    //s_->SkipIfElement("coverpage");
    ////</coverpage>

    ////<lang>
    //lang();
    ////</lang>

    ////<src-lang>
    //s_->SkipIfElement("src-lang");
    ////</src-lang>

    ////<translator>
    //s_->SkipIfElement("translator");
    ////</translator>

    ////<sequence>
    //while(s_->IsNextElement("sequence"))
    //    sequence();
    ////</sequence>

    //s_->EndElement();
}

void FB2TOEPUB_DECL DoPrintInfo (const String &in)
{
    Ptr<ConverterInfo> conv = new ConverterInfo(in);
    conv->Scan();
}


};  //namespace Fb2ToEpub
