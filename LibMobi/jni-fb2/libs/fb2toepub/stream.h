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


#ifndef FB2TOEPUB__STREAM_H
#define FB2TOEPUB__STREAM_H

#include "types.h"
#include <string.h>
#include <stdarg.h>

namespace Fb2ToEpub
{


//-----------------------------------------------------------------------
// INPUT STREAM INTERFACE, OBJECT
//-----------------------------------------------------------------------
class InStmI
{
public:
    virtual bool        IsEOF() const                       = 0;
    virtual char        GetChar()                           = 0;
    virtual size_t      Read(void *buffer, size_t max_cnt)  = 0;
    virtual void        UngetChar(char c)                   = 0;
    virtual void        Rewind()                            = 0;
    virtual String      UIFileName() const                  = 0;

    // helper
    unsigned char GetUChar()
    {
        return static_cast<unsigned char>(GetChar());
    }
    void UngetUChar(unsigned char uc)
    {
        UngetChar(static_cast<unsigned char>(uc));
    }
    char LookChar()
    {
        char c = GetChar();
        UngetChar(c);
        return c;
    }
    unsigned char LookUChar()
    {
        unsigned char c = GetUChar();
        UngetUChar(c);
        return c;
    }
};
class InStm : public InStmI, public Object {};

//-----------------------------------------------------------------------
// OUTPUT STREAM INTERFACE, OBJECT
//-----------------------------------------------------------------------
class FB2TOEPUB_DECL OutStmI
{
public:
    virtual void    PutChar(char c)                     = 0;
    virtual void    Write (const void *p, size_t cnt)   = 0;

    // helper
    void WriteStr(const char *pc)
    {
        Write(pc, strlen(pc));
    }
    void WriteFmt(const char *fmt, ...)
    {
        va_list ap;
        va_start(ap, fmt);
        VWriteFmt(fmt, ap);
        va_end(ap);
    }
    void VWriteFmt(const char *fmt, va_list ap);
};
class OutStm : public OutStmI, public Object {};

//-----------------------------------------------------------------------
// INPUT AND OUTPUT STREAM IMPLEMENTATION FOR FILE
//-----------------------------------------------------------------------
Ptr<InStm> FB2TOEPUB_DECL   CreateInFileStm(const char *name);
Ptr<OutStm> FB2TOEPUB_DECL  CreateOutFileStm(const char *name);

//-----------------------------------------------------------------------
// INPUT STREAM FROM MEMORY
//-----------------------------------------------------------------------
//Ptr<InStm> FB2TOEPUB_DECL   CreateInMemStm(const void *p, std::size_t size);

//-----------------------------------------------------------------------
// INPUT STREAM WRAPPER WITH INFINITE UNGET
//-----------------------------------------------------------------------
//Ptr<InStm> FB2TOEPUB_DECL   CreateInfUngetStm(InStm *stm);


};  //namespace Fb2ToEpub

#endif
