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

// The code in this file was partialy written using source code
// of "epub-tools" utility. You can find sources of epub-tools unility
// on the utility home page:
// http://code.google.com/p/epub-tools/
// Below is the original copyright notice of epub-tools.

/*******************************************************************************
 * Copyright (c) 2009, Adobe Systems Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * ·        Redistributions of source code must retain the above copyright 
 *          notice, this list of conditions and the following disclaimer. 
 *
 * ·        Redistributions in binary form must reproduce the above copyright 
 *		   notice, this list of conditions and the following disclaimer in the
 *		   documentation and/or other materials provided with the distribution. 
 *
 * ·        Neither the name of Adobe Systems Incorporated nor the names of its 
 *		   contributors may be used to endorse or promote products derived from
 *		   this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/


#include "hdr.h"

#include "opentypefont.h"
#include "error.h"

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// A trivial file wrapper
class FileWrp : public Object, Noncopyable
{
    FILE *f_;       // file
    String name_;   // file name
public:
    explicit FileWrp(const String &name) : f_(::fopen(name.c_str(), "rb")), name_(name)
    {
        if(!f_)
            IOError(name_, "can't open src file");
    }
    ~FileWrp()
    {
        fclose(f_);
    }
    
    unsigned char GetUChar()
    {
        int c = ::fgetc(f_);
        if(c == EOF)
            IOError(name_, "fgetc EOF or read error");
        return static_cast<unsigned char>(c);
    }
    void Read(void *buffer, size_t max_cnt)
    {
        if(::fread(buffer, 1, max_cnt, f_) != max_cnt)
            IOError(name_, "fread error");
    }
    void Seek(size_t pos)
    {
        if(::fseek(f_, pos, SEEK_SET))
            IOError(name_, "fseek error");
    }
    void Skip(size_t num)
    {
        if(::fseek(f_, num, SEEK_CUR))
            IOError(name_, "fseek error");
    }
};


//-----------------------------------------------------------------------
static unsigned int GetUInt2(FileWrp *f)
{
    unsigned int high = f->GetUChar();
    return (high << 8) | f->GetUChar();
}

//-----------------------------------------------------------------------
static unsigned int GetUInt4(FileWrp *f)
{
    unsigned int high = GetUInt2(f);
    return (high << 16) | GetUInt2(f);
}

//-----------------------------------------------------------------------
bool FB2TOEPUB_DECL IsFontEmbedAllowed(const String &fontpath)
{
    Ptr<FileWrp> f = new FileWrp(fontpath);

    // read header
    unsigned int version = GetUInt4(f);
    switch(version)
    {
    case 0x74746366: // TrueType collection file
        // we can only read the first font in the collection
        f->Seek(12);
        f->Seek(GetUInt4(f));
        version = GetUInt4(f);
        break;

    case 0x00010000: // regular TrueType
    case 0x4f54544f: // CFF based
        break;

    default:
        FontError(fontpath, "invalid OpenType file");
    }
    unsigned int numTables = GetUInt2(f);
    f->Skip(6);

    // find OS/2 table and check fsType field
    for(unsigned int i = numTables; i-- > 0;)
    {
        char id[5];
        f->Read(id, 4);
        id[4] = 0;
        if(!strcmp(id, "OS/2"))
        {
            f->Skip(4);     // checksum
            unsigned int offset = GetUInt4(f);
            f->Seek(offset+8);

            unsigned int fsType = GetUInt2(f);
            if((fsType & 0xF) == 2)
                return false; // explicitly disallowed embedding and subsetting
            if((fsType & 0x0200) != 0)
                return false; // explicitly disallowed outline embedding
            return true;
        }
        f->Skip(12);
    }

    FontError(fontpath, "OS/2 table not found");
    return false;
}

};  //namespace Fb2ToEpub
