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

#include <string.h>
#include <string>
#include <algorithm>
#include <ctype.h>
#include "streamconv.h"
#include "scanner.h"


namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
//-----------------------------------------------------------------------

static const char* RoughEncoding(InStm *stm)
{
    // usually it's enough to test the first byte only...
    switch(stm->GetUChar())
    {
    case 0x00:  return "UTF-32BE";
    case 0x0E:  return "SCSU";
    case 0x2B:  return "UTF-7";
    case '<':   return "UTF-8";     // temporary assume UTF-8 XML
    case 0x84:  return "GB-18030";
    case 0xEF:  return "UTF-8";
    case 0xDD:  return "UTF-EBCDIC";
    case 0xF7:  return "UTF-1";
    case 0xFB:  return "BOCU-1";
    case 0xFE:  return "UTF-16BE";
    case 0xFF:
        if(stm->GetUChar() != 0xFE)
            break;
        return stm->GetUChar() ? "UTF-16LE" : "UTF-32LE";
    default:
        break;
    }

    IOError(stm->UIFileName(), "bad XML or unknown encoding");
    return NULL;
}

//-----------------------------------------------------------------------
// INPUT UTF-8 STREAM (DISCARD BYTE ORDER MARK)
//-----------------------------------------------------------------------
class InStmUtf8 : public InStm, Noncopyable
{
    Ptr<InStm>  stm_;
    bool        has_bom_;

public:
    InStmUtf8(InStm *stm, const char *fromcode);

    //virtuals
    bool        IsEOF() const                       {return stm_->IsEOF();}
    char        GetChar()                           {return stm_->GetChar();}
    size_t      Read(void *buffer, size_t max_cnt)  {return stm_->Read(buffer, max_cnt);}
    void        UngetChar(char c)                   {return stm_->UngetChar(c);}
    void        Rewind();
    String      UIFileName() const                  {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
InStmUtf8::InStmUtf8(InStm *stm, const char *fromcode)
{
    stm->Rewind();
    stm_ =  strcmp(fromcode, "UTF-8") ? CreateInConvStm(stm, "UTF-8", fromcode) : Ptr<InStm>(stm);
    unsigned char uc = stm_->GetUChar();
    // skip byte order mark
    has_bom_ = (uc == 0xEF);
    if(!has_bom_)
        stm_->UngetUChar(uc);
    else if(stm_->GetUChar() != 0xBB || stm_->GetUChar() != 0xBF)
        InternalError(__FILE__, __LINE__, "bad UTF-8 BOM");
}

//-----------------------------------------------------------------------
void InStmUtf8::Rewind()
{
    stm_->Rewind();
    if(has_bom_)
    {
        // skip byte order mark
        stm_->GetChar();
        stm_->GetChar();
        stm_->GetChar();
    }
}

static String ParseEncoding(InStm *stm)
{
    Ptr<LexScanner> scanner = CreateScanner(stm);

    // skip version
    if (scanner->GetToken() != LexScanner::Token(LexScanner::XMLDECL) ||
        scanner->GetToken().type_ != LexScanner::VERSION)
    {
        scanner->Error("XML version error");
    }

    // check if next is encoding
    LexScanner::Token t = scanner->GetToken();
    if(t != LexScanner::Token(LexScanner::ENCODING))
        return "UTF-8";

    // skip '='
    if (scanner->GetToken().type_ != LexScanner::EQ)
        scanner->Error("XML header: '=' expected");

    // next should be string value
    t = scanner->GetToken();
    if(t.type_ != LexScanner::VALUE)
        scanner->Error("XML header: 'value' expected");
    return t.s_;
}

//-----------------------------------------------------------------------
Ptr<InStm> FB2TOEPUB_DECL CreateInUnicodeStm(InStm *stm)
{
    // analize bit order mark to get rough encoding
    const char *fromcode = RoughEncoding(stm);

    // parse <?xml version="1.0" encoding="..."?>
    // to refine the encoding
    Ptr<InStm> tmp = new InStmUtf8(stm, fromcode);
    return new InStmUtf8(stm, ParseEncoding(tmp).c_str());
}


};  //namespace Fb2ToEpub
