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


#if 0
#include "hdr.h"

#include <string>
#include "streamconv.h"
#if defined(WIN32)
#include "iconv.h"
#elif defined(unix)
#include <iconv.h>
#endif

namespace Fb2ToEpub
{


//-----------------------------------------------------------------------
// CONVERTER WRAPPER
//-----------------------------------------------------------------------
class FB2TOEPUB_DECL ConvLibiconv : Noncopyable
{
    ::iconv_t cd_;

    // Patch to fix iconv inbuf parameter incompatibility issue
    class iconv_patch
    {
        const char **inbuf_;
    public:
        iconv_patch(const char **inbuf)     : inbuf_(inbuf) {}
        operator const char**   () const    {return inbuf_;}
        operator char**         () const    {return const_cast<char**>(inbuf_);}
    };

public:
    ConvLibiconv(const char* tocode, const char* fromcode, bool translit = true, bool ignore = true);
    ~ConvLibiconv();

    size_t Convert(const char* * inbuf, size_t *inbytesleft, char* * outbuf, size_t *outbytesleft)
    {
        return ::iconv(cd_, iconv_patch(inbuf), inbytesleft, outbuf, outbytesleft);
    }
};

//-----------------------------------------------------------------------
// ConvLibiconv implementation
//-----------------------------------------------------------------------
ConvLibiconv::ConvLibiconv(const char* tocode, const char* fromcode, bool translit, bool ignore)
                                : cd_(::iconv_open(tocode, fromcode))
{
    if((::iconv_t)-1 == cd_)
        ExternalException("iconv_open error");
#ifdef ICONV_SET_TRANSLITERATE
    if(translit)
    {
        int one = 1;
        if(0 != ::iconvctl(cd_, ICONV_SET_TRANSLITERATE, &one))
            ExternalException("iconvctl error");
    }
#endif
#ifdef ICONV_SET_DISCARD_ILSEQ
    if(ignore)
    {
        int one = 1;
        if(0 != ::iconvctl(cd_, ICONV_SET_DISCARD_ILSEQ, &one))
            ExternalException("iconvctl error");
    }
#endif
}

//-----------------------------------------------------------------------
ConvLibiconv::~ConvLibiconv()
{
    ::iconv_close(cd_);
}


//-----------------------------------------------------------------------
// stream converter buffer sizes
const size_t IN_CONVBUF_SIZE = 256;
const size_t OUT_CONVBUF_SIZE = 512;

//-----------------------------------------------------------------------
// InLibiconvStm implementation
//-----------------------------------------------------------------------
class InLibiconvStm : public InStm, Noncopyable
{
    Ptr<InStm>          stm_;                       // input stream
    mutable ConvLibiconv conv_;                     // converter
    mutable char        ibuf_[IN_CONVBUF_SIZE];     // input buffer
    mutable char        *iend_;                     // input buffer unconverted data end
    mutable char        obuf_[OUT_CONVBUF_SIZE];    // output buffer
    mutable char        *ocur_;                     // output buffer current position
    mutable char        *oend_;                     // output buffer concerted data end

    size_t Fill() const;

public:
    InLibiconvStm(InStm *stm, const char* tocode, const char* fromcode);

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
InLibiconvStm::InLibiconvStm(InStm *stm, const char* tocode, const char* fromcode)
                            :   stm_(stm),
                                conv_(tocode, fromcode),
                                iend_(ibuf_),
                                ocur_(obuf_),
                                oend_(obuf_)
{
}

//-----------------------------------------------------------------------
bool InLibiconvStm::IsEOF() const
{
    return (ocur_ == oend_) && (stm_->IsEOF() || !Fill());
}

//-----------------------------------------------------------------------
size_t InLibiconvStm::Fill() const
{
    // fix output pointers
    ocur_ = oend_ = obuf_;

    // read data to input buffer
    iend_ += stm_->Read(iend_, ibuf_ + sizeof(ibuf_) - iend_);
    size_t inleft  = iend_ - ibuf_;
    if(!inleft)
        return 0;

    // convert data
    const char  *pi = ibuf_;
    size_t outleft = sizeof(obuf_);
    if((size_t)-1 == conv_.Convert(&pi, &inleft, &oend_, &outleft) && errno == EILSEQ)
        IOError(UIFileName(), "iconv: invalid codesymbol");

    // fix input data and pointers
    iend_ = ibuf_ + inleft;
    if(inleft)
        ::memmove(ibuf_, pi, inleft);   // move unconverted rest to beginning

    return oend_ - ocur_;
}

//-----------------------------------------------------------------------
char InLibiconvStm::GetChar()
{
    if(ocur_ == oend_ && !Fill())
        IOError(UIFileName(), "conv: EOF");
    return *ocur_++;
}

//-----------------------------------------------------------------------
size_t InLibiconvStm::Read(void *buffer, size_t max_cnt)
{
    char *pc = reinterpret_cast<char*> (buffer);
    for (size_t cnt = 0; cnt < max_cnt;)
    {
        size_t num_to_copy = oend_ - ocur_;
        if (num_to_copy <= 0 && (num_to_copy = Fill ()) == 0)
            return cnt;
        if (num_to_copy > max_cnt - cnt)
            num_to_copy = max_cnt - cnt;

        ::memcpy (pc, ocur_, num_to_copy);

        pc      += num_to_copy;
        ocur_   += num_to_copy;
        cnt     += num_to_copy;
    }
    return max_cnt;
}

//-----------------------------------------------------------------------
void InLibiconvStm::UngetChar(char c)
{
    if(ocur_ == obuf_)
        IOError(UIFileName(), "conv: can't unget");
    --ocur_;
}

//-----------------------------------------------------------------------
void InLibiconvStm::Rewind()
{
    stm_->Rewind();
    conv_.Convert(NULL, NULL, NULL, NULL);
    iend_ = ibuf_;
    ocur_ = oend_ = obuf_;
}


//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
Ptr<InStm> CreateInConvStm(InStm *stm, const char* tocode, const char* fromcode)
{
    return new InLibiconvStm(stm, tocode, fromcode);
}

};  //namespace Fb2ToEpub
#endif
