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

#include <sstream>
#include "scanner.h"

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
class SetScannerSkipMode
{
    Ptr<LexScanner> s_;
    bool old_;
public:
    SetScannerSkipMode(LexScanner *scanner)     : s_(scanner), old_(scanner->SetSkipMode(true)) {}
    ~SetScannerSkipMode()                       {s_->SetSkipMode(old_);}
};


//-----------------------------------------------------------------------
void LexScanner::SkipAttributes()
{
    SetScannerSkipMode skipMode(this);
    while(LookAhead().type_ == NAME)
    {
        GetToken();
        if (GetToken().type_ != EQ || GetToken().type_ != VALUE)
            Error("'=value' expected in attribute definition");
    }
}

//-----------------------------------------------------------------------
void LexScanner::SkipRestOfElementContent()
{
    SetScannerSkipMode skipMode(this);
    for(;;)
    {
        Token t = GetToken();
        switch(t.type_)
        {
        case DATA:
            continue;
        case START:
            UngetToken(t);
            SkipElement();
            continue;
        case END:
            if (GetToken().type_ != CLOSE)
                Error("'close' of etag expected");
            return;
        default:
            Error("unexpected token");
        }
    }
}

//-----------------------------------------------------------------------
void LexScanner::SkipElement()
{
    SetScannerSkipMode skipMode(this);

    // start
    if(GetToken().type_ != START)
        Error("element stag expected");

    SkipAttributes();

    // close of start tag
    switch(GetToken().type_)
    {
    case SLASHCLOSE:    return;
    case CLOSE:         break;
    default:            Error("'close' of stag expected");
    }

    // skip element content
    SkipRestOfElementContent();
}

//-----------------------------------------------------------------------
void LexScanner::CheckAndSkipElement(const String &element)
{
	if (!IsNextElement(element))
		Error("expected element not found");
    SkipElement();
}

//-----------------------------------------------------------------------
void LexScanner::SkipIfElement(const String &element)
{
    if(LookAhead() == Token(START, element))
        SkipElement();
}

//-----------------------------------------------------------------------
void LexScanner::SkipAll(const String &element)
{
    Token t(START, element);
    while(LookAhead() == t)
        SkipElement();
}

//-----------------------------------------------------------------------
void LexScanner::SkipXMLDeclaration ()
{
    // skip version
    if (GetToken() != Token(XMLDECL) || GetToken().type_ != VERSION)
        Error("xmldecl parsing error");
    
    // skip initial encoding (the stream is already converted to UTF-8)
    Token t = GetToken();
    if  (t == Token(ENCODING))
    {
        if (GetToken().type_ != EQ || GetToken().type_ != VALUE)
            Error("xmldecl 'encoding' parsing error");
        t = GetToken();
    }

    // skip standalone
    if  (t == Token(STANDALONE))
    {
        if (GetToken().type_ != EQ || GetToken().type_ != VALUE)
            Error("xmldecl 'standalone' parsing error");
        t = GetToken();
    }

    if(t != Token(CLOSE))
        Error("closing xmldecl expected");
}

//-----------------------------------------------------------------------
bool LexScanner::IsNextElement(const String &element)
{
    return (LookAhead() == Token(START, element));
}

//-----------------------------------------------------------------------
void LexScanner::ParseAttributes(AttrMap *attrmap)
{
    for(;;)
    {
        Token tname = GetToken();
        if(tname.type_ != NAME)
        {
            UngetToken(tname);
            return;
        }

        AttrMap::iterator it = attrmap->lower_bound(tname.s_);
        if(it != attrmap->end() && it->first == tname.s_)
            Error("attribute redefinition");

        if (GetToken() != Token(EQ))
            Error("'=' expected in attribute definition");
        
        Token tval = GetToken();
        if(tval.type_ != VALUE)
            Error("'value' expected in attribute definition");

        attrmap->insert(it, AttrMap::value_type(tname.s_, tval.s_));
    }
}

//-----------------------------------------------------------------------
bool LexScanner::BeginElement(const String &element, AttrMap *attrmap)
{
	Token t1 = GetToken();
	Token t2 = Token(START, element);
    if(t1 != Token(START, element))
    {
        std::ostringstream ss;
        ss << "element <" << element << "> expected";
        Error(ss.str());
		return false;
    }

    if(attrmap)
        ParseAttributes(attrmap);
    else
        SkipAttributes();

    Token t = GetToken();
    switch(t.type_)
    {
    case CLOSE:         return true;
    case SLASHCLOSE:    return false;

    default:
        {
            std::ostringstream ss;
            ss << "element <" << element << "> expected";
            Error(ss.str());
        }
        return false;
    }
}

//-----------------------------------------------------------------------
void LexScanner::BeginNotEmptyElement(const String &element, AttrMap *attrmap)
{
    if(!BeginElement(element, attrmap))
    {
        std::ostringstream ss;
        ss << "element <" << element << "> can't be empty";
        Error(ss.str());
    }
}

//-----------------------------------------------------------------------
String LexScanner::SimpleTextElement(const String &element, AttrMap *attrmap)
{
    if(!BeginElement(element, attrmap))
        return "";

    SetScannerDataMode setDataMode(this);
    String text;
    LexScanner::Token t = GetToken();
    if(t.type_ == DATA)
    {
        text = t.s_;
        t = GetToken();
    }
    if(t.type_ != END || GetToken().type_ != CLOSE)
        Error("etag expected");
    return text;
}

//-----------------------------------------------------------------------
void LexScanner::EndElement()
{
	LexScanner::Token t1 = GetToken();
	LexScanner::Token t2 = GetToken();
    if(t1.type_ != END || t2.type_ != CLOSE)
        Error("etag expected");
}

//-----------------------------------------------------------------------
static void ConvertToUtf8(unsigned long x, std::vector<char> *buf)
{
    if(x <= 0x7fUL)
        buf->push_back(static_cast<char>(x));
    else if(x <= 0x7ffUL)
    {
        buf->push_back(0xc0|static_cast<char>(x >> 6));
        buf->push_back(0x80|static_cast<char>(x & 0x3f));
    }
    else if(x <= 0xffffUL)
    {
        buf->push_back(0xe0|static_cast<char>(x >> 12));
        buf->push_back(0x80|static_cast<char>((x >> 6) & 0x3f));
        buf->push_back(0x80|static_cast<char>(x & 0x3f));
    }
    else
    {
        buf->push_back(0xf0|static_cast<char>(x >> 18));
        buf->push_back(0x80|static_cast<char>((x >>12) & 0x3f));
        buf->push_back(0x80|static_cast<char>((x >> 6) & 0x3f));
        buf->push_back(0x80|static_cast<char>(x & 0x3f));
    }
}

//-----------------------------------------------------------------------
static const bool DecodeDecimal(const char **s, std::vector<char> *buf)
{
    const char *pc = *s;
    unsigned long x = 0;
    char c = *pc++;
    do
    {
        if(c < '0' || c > '9')
            return false;

        x = x*10 + (c - '0');
        c = *pc++;
    }
    while(c != ';');
    ConvertToUtf8(x, buf);
    *s = pc;
    return true;
}

//-----------------------------------------------------------------------
static const bool DecodeHex(const char **s, std::vector<char> *buf)
{
    const char *pc = *s;
    unsigned long x = 0;
    char c = *pc++;
    do
    {
        if(c >= '0' && c <= '9')
            x = x*16 + (c - '0');
        else if(c >= 'a' && c <= 'f')
            x = x*16 + (c + 10 - 'a');
        else if(c >= 'A' && c <= 'F')
            x = x*16 + (c + 10 - 'A');
        else
            return false;

        c = *pc++;
    }
    while(c != ';');
    ConvertToUtf8(x, buf);
    *s = pc;
    return true;
}

//-----------------------------------------------------------------------
static bool DecodeEntity(const char **s, const char *etext, char val, std::vector<char> *buf)
{
    const char *pc = *s;
    for(;;)
    {
        char c = *etext++;
        if(!c)
        {
            buf->push_back(val);
            *s = pc;
            return true;
        }
        if(*pc++ != c)
            return false;
    }
}

//-----------------------------------------------------------------------
static void UnknownEntity(const char **s, std::vector<char> *buf)
{
    static const char amp[] = "&amp;";
    buf->insert(buf->end(), amp, amp + strlen(amp));
}

//-----------------------------------------------------------------------
void LexScanner::Decode(const char *s, std::vector<char> *buf, bool decodeEntities, bool removeLF)
{
    for(;;)
    {
        char c = *s++;
        switch(c)
        {
        default:    buf->push_back(c); continue;
        case '\0':  buf->push_back('\0'); return;

        case '\n':
            if(!removeLF)
                buf->push_back(c);
            continue;

        case '\r':
            if(!removeLF)
            {
                buf->push_back('\n');
                if(*s == '\n')
                    ++s;
            }
            continue;

        case '&':
            if(!decodeEntities)
                buf->push_back('&');
            else
            {
                const char *sOld = s;
                if(*s == '#')
                {
                    if(*++s != 'x')
                    {
                        if(DecodeDecimal(&s, buf))
                            continue;
                    }
                    else
                    {
                        ++s;
                        if(DecodeHex(&s, buf))
                            continue;
                    }
                }
                else
                {
                    if (DecodeEntity(&s, "lt;", '<', buf) ||
                        DecodeEntity(&s, "gt;", '>', buf) ||
                        DecodeEntity(&s, "amp;", '&', buf) ||
                        DecodeEntity(&s, "apos;", '\'', buf) ||
                        DecodeEntity(&s, "quot;", '"', buf))
                    {
                        continue;
                    }
                }

                s = sOld;
                UnknownEntity(&s, buf);
            }
            continue;
        }
    }
}

//-----------------------------------------------------------------------
static void EncodeEntity(const char *s, std::vector<char> *buf)
{
    buf->insert(buf->end(), s, s + strlen(s));
}

//-----------------------------------------------------------------------
void LexScanner::Encode(const char *s, std::vector<char> *buf)
{
    for(;;)
    {
        char c = *s++;
        switch(c)
        {
        case '\0':  buf->push_back('\0');           return;
        case '<':   EncodeEntity("&lt;", buf);      continue;
        case '>':   EncodeEntity("&gt;", buf);      continue;
        case '&':   EncodeEntity("&amp;", buf);     continue;
        case '\'':  EncodeEntity("&apos;", buf);    continue;
        case '"':   EncodeEntity("&quot;", buf);    continue;
        default:    buf->push_back(c);              continue;
        }
    }
}


};  //namespace Fb2ToEpub
