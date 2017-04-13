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

#include "stream.h"
#include "error.h"
#include <vector>

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// InFileStm implementation
//-----------------------------------------------------------------------
class InFileStm : public InStm, Noncopyable
{
    FILE *f_;       // file
    String name_;   // file name
    mutable int c_; // last buffered character

public:
    explicit InFileStm(const char *name);
    ~InFileStm() {fclose(f_);}

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const;
};

//-----------------------------------------------------------------------
InFileStm::InFileStm(const char *name) : f_(::fopen(name, "rb")), name_(name), c_(EOF)
{
    if(!f_)
        IOError(name_, "can't open src file");
}

//-----------------------------------------------------------------------
bool InFileStm::IsEOF() const
{
    return (c_ == EOF) && ((c_ = ::fgetc(f_)) == EOF);
}

//-----------------------------------------------------------------------
char InFileStm::GetChar()
{
    if(IsEOF())
        IOError(name_, "fgetc EOF or read error");

    char cret = static_cast<char>(c_);
    c_ = EOF;
    return cret;
}

//-----------------------------------------------------------------------
size_t InFileStm::Read(void *buffer, size_t max_cnt)
{
    if(!max_cnt)
        return 0;

    char *cb = reinterpret_cast<char*>(buffer);
    size_t cnt = 0;

    if(c_ != EOF)
    {
        *cb++ = static_cast<char>(c_);
        if(max_cnt == 1)
            return 1;
        cnt = 1;
        c_ = EOF;
    }

    cnt += ::fread(cb, 1, max_cnt - cnt, f_);
#ifdef _WIN32
	if (cnt != max_cnt && !::feof(f_))
#else
	if (cnt != max_cnt && (f_->_flags  & __SEOF) == 0) // !::ferror(f_))
#endif		
        IOError(name_, "fread error");

    return cnt;
}

//-----------------------------------------------------------------------
void InFileStm::UngetChar(char c)
{
    if(c_ != EOF && ::ungetc(c_, f_) == EOF)
        IOError(name_, "ungetc error");
    c_ = static_cast<unsigned char>(c);
}

//-----------------------------------------------------------------------
void InFileStm::Rewind()
{
    rewind(f_);
    c_ = EOF;
}

String InFileStm::UIFileName() const
{
    return name_;
}

//-----------------------------------------------------------------------
Ptr<InStm> CreateInFileStm(const char *name)
{
    return new InFileStm(name);
}


//-----------------------------------------------------------------------
// OutFileStm implementation
//-----------------------------------------------------------------------
class OutFileStm : public OutStm, Noncopyable
{
    FILE *f_;
    String name_;
public:
    explicit OutFileStm(const char *name);
    ~OutFileStm() {fclose(f_);}

    //virtuals
    void    PutChar(char c);
    void    Write(const void *p, size_t cnt);
};

//-----------------------------------------------------------------------
OutFileStm::OutFileStm(const char *name) : f_(::fopen(name, "wb")), name_(name)
{
    if(!f_)
        IOError(name_, "can't open dst file");
}

//-----------------------------------------------------------------------
void OutFileStm::PutChar(char c)
{
    if(EOF == fputc(c, f_))
        IOError(name_, "dst: fputc error");
}

//-----------------------------------------------------------------------
void OutFileStm::Write(const void *p, size_t cnt)
{
    if(fwrite(p, 1, cnt, f_) != cnt)
        IOError(name_, "dst: fwrite error");
}

//-----------------------------------------------------------------------
Ptr<OutStm> CreateOutFileStm(const char *name)
{
    return new OutFileStm(name);
}


/*
//-----------------------------------------------------------------------
// MEMORY INPUT STREAM
//-----------------------------------------------------------------------
class MemInStm : public InStm
{
    const char *pbegin_, *pend_;
    mutable const char *p_;
    mutable int c_; // last buffered character

public:
    MemInStm(const char *p, std::size_t size) : pbegin_(p), pend_(p + size), p_(p), c_(EOF) {}

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return "memory stream";}
};

//-----------------------------------------------------------------------
bool MemInStm::IsEOF() const
{
    return c_ == EOF && p_ >= pend_;
}

//-----------------------------------------------------------------------
char MemInStm::GetChar()
{
    if(c_ == EOF)
    {
        if(p_ >= pend_)
            IOError("memory stream", "end reached");
        return *p_++;
    }
    else
    {
        char cret = static_cast<char>(c_);
        c_ = EOF;
        return cret;
    }
}

//-----------------------------------------------------------------------
size_t MemInStm::Read(void *buffer, size_t max_cnt)
{
    if(!max_cnt)
        return 0;

    char *cb = reinterpret_cast<char*>(buffer);
    size_t cnt = 0;

    if(c_ != EOF)
    {
        *cb++ = static_cast<char>(c_);
        if(max_cnt == 1)
            return 1;
        cnt = 1;
        c_ = EOF;
    }

    size_t to_copy = max_cnt - cnt;
    if(to_copy > static_cast<size_t>(pend_ - p_))
        to_copy = pend_ - p_;
    if(!(cnt += to_copy))
        IOError("memory stream", "end reached");

    memcpy(cb, p_, to_copy);
    p_ += to_copy;
    return cnt;
}

//-----------------------------------------------------------------------
void MemInStm::UngetChar(char c)
{
    if(c_ != EOF)
        IOError("memory stream", "ungetc error");
    c_ = static_cast<unsigned char>(c);
}

//-----------------------------------------------------------------------
void MemInStm::Rewind()
{
    p_ = pbegin_;
    c_ = EOF;
}

//-----------------------------------------------------------------------
Ptr<InStm> FB2TOEPUB_DECL CreateInMemStm(const void *p, std::size_t size)
{
    return new MemInStm(reinterpret_cast<const char*>(p), size);
}
*/


/*
//-----------------------------------------------------------------------
// INPUT STREAM WRAPPER WITH INFINITE UNGET
//-----------------------------------------------------------------------
class InInfUngetStm : public InStm, Noncopyable
{
    Ptr<InStm>                  stm_;   // stream
    mutable std::vector<char>   cbuf_;  // char buffer (for unget etc.)

public:
    InInfUngetStm(InStm *stm) : stm_(stm) {}

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
bool InInfUngetStm::IsEOF() const
{
    return cbuf_.empty() && stm_->IsEOF();
}

//-----------------------------------------------------------------------
char InInfUngetStm::GetChar()
{
    if(cbuf_.empty())
        return stm_->GetChar();
    else
    {
        char c = cbuf_.back();
        cbuf_.pop_back();
        return c;
    }
}

//-----------------------------------------------------------------------
size_t InInfUngetStm::Read(void *buffer, size_t max_cnt)
{
    if(!max_cnt)
        return 0;

    char *cb = reinterpret_cast<char*>(buffer);
    size_t cnt = cbuf_.size();
    if(cnt)
    {
        if (cnt > max_cnt)
            cnt = max_cnt;
        std::vector<char>::const_iterator cit = cbuf_.end();
        for(size_t i = cnt; i-- > 0;)
            *cb++ = *--cit;
        cbuf_.resize(cbuf_.size() - cnt);
    }

    return (max_cnt > cnt) ?
            cnt + stm_->Read(cb, max_cnt - cnt) :
            cnt;
}

//-----------------------------------------------------------------------
void InInfUngetStm::UngetChar(char c)
{
    cbuf_.push_back(c);
}

//-----------------------------------------------------------------------
void InInfUngetStm::Rewind()
{
    cbuf_.clear();
    stm_->Rewind();
}

//-----------------------------------------------------------------------
Ptr<InStm> CreateInfUngetStm(InStm *stm)
{
    return new InInfUngetStm(stm);
}
*/


//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
void OutStmI::VWriteFmt(const char *fmt, va_list ap)
{
#if !(defined WIN32) && !(defined unix)
#error Set initial buffer size to 2 and check if the code with vsnprintf below works in your system!!!
#endif
    // Guess we need no more than 100 bytes.
    std::vector<char> buf(100);
    int size = buf.size();
    for(;;)
    {
        // Try to print in the allocated space.
        int cnt = vsnprintf(&buf[0], size, fmt, ap);

        // If that worked, write string and return.
        if(cnt > -1 && cnt < size)
        {
            buf[size-1] = '\0';
            WriteStr(&buf[0]);
            return;
        }

        // Else try again with more space.
        if (cnt > -1)
           buf.resize(size = cnt + 1);  // precisely what is needed
        else
           buf.resize(size *= 2);       // twice the old size
    }
}


};  //namespace Fb2ToEpub
