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

#include <map>
#include <vector>
#include "translit.h"
#include "scanner.h"

namespace Fb2ToEpub
{

typedef std::map<String, String> XlitMap;

//-----------------------------------------------------------------------
// XlitConv implementation
// (It is not very efficient, however it hopefully is efficient enough,
// as we translate only author name, title and table of contents.)
//-----------------------------------------------------------------------
class XlitConvImpl : public XlitConv
{
public:
    XlitConvImpl(InStm *stm);

    //virtual
    String Convert(const String &s) const;

private:
    XlitMap xlit_;

    // elements
    void map        (LexScanner *s);
    void transtable (LexScanner *s);
};

//-----------------------------------------------------------------------
XlitConvImpl::XlitConvImpl(InStm *stm)
{
    Ptr<LexScanner> s = CreateScanner(stm);
    s->SkipXMLDeclaration();

    //<transtable>
    transtable(s);
    //</transtable>
}


//-----------------------------------------------------------------------
String XlitConvImpl::Convert(const String &s) const
{
    if(xlit_.empty())
        return s;

    std::vector<char> buf;
    const char *p = s.c_str(), *p_end = p + s.length();
    while(*p)
    {
        XlitMap::const_iterator cit = xlit_.lower_bound(p);
        if(cit != xlit_.end() && !strcmp(p, cit->first.c_str()))
        {
            buf.insert(buf.end(), cit->second.begin(), cit->second.end());
            p += cit->first.length();
            continue;
        }
        if(cit != xlit_.begin())
        {
            --cit;
            if(!strncmp(p, cit->first.c_str(), cit->first.length()))
            {
                buf.insert(buf.end(), cit->second.begin(), cit->second.end());
                p += cit->first.length();
                continue;
            }
        }

        // copy next utf-8 symbol
        if(!(*p & 0x80))
        {
            buf.push_back(*p++);
            continue;
        }

        size_t maxCopy = p_end - p;
        if((*p & 0xe0) == 0xc0)
        {
            if(maxCopy > 2)
                maxCopy = 2;
        }
        else if((*p & 0xf0) == 0xe0)
        {
            if(maxCopy > 3)
                maxCopy = 3;
        }
        else
        {
            if(maxCopy > 4)
                maxCopy = 4;
        }
        buf.insert(buf.end(), p, p + maxCopy);
        p += maxCopy;
    }

    buf.push_back('\0');
    return &buf[0];
}


//-----------------------------------------------------------------------
void XlitConvImpl::map(LexScanner *s)
{
    AttrMap attrmap;
    bool notempty = s->BeginElement("map", &attrmap);

    String in = attrmap["in"], out = attrmap["out"];
    if(!in.empty() && !out.empty())
        xlit_[in] = out;

    if(notempty)
        s->EndElement();
}

//-----------------------------------------------------------------------
void XlitConvImpl::transtable(LexScanner *s)
{
    s->BeginNotEmptyElement("transtable");

    //<map>
    while(s->IsNextElement("map"))
        map(s);
    //</map>
}

//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
Ptr<XlitConv> FB2TOEPUB_DECL CreateXlitConverter(InStm *stm)
{
    return new XlitConvImpl(stm);
}

};  //namespace Fb2ToEpub
