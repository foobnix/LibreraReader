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
#include "unicode/ucnv.h"


namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// CONVERTER WRAPPER
//-----------------------------------------------------------------------
class ConvBaseICU : public Object
{
public:
    virtual UErrorCode  Convert(char **target, const char *targetLimit, const char **source, const char *sourceLimit, UBool flush) = 0;
    virtual void        Reset() = 0;
};


//-----------------------------------------------------------------------
// CONVERTER TO UNICODE
//-----------------------------------------------------------------------
class ConverterToUnicodeUCI : public ConvBaseICU, Noncopyable
{
    UConverter *uc_;
public:
    ConverterToUnicodeUCI(const char* name)
    {
        UErrorCode err = U_ZERO_ERROR;
        uc_ = ucnv_open(name, &err);
        if(U_FAILURE(err))
            ExternalError("ucnv_open error");
    }
    ~ConverterToUnicodeUCI()
    {
        ucnv_close(uc_);
    }

    //virtuals
    UErrorCode Convert(char **target, const char *targetLimit, const char **source, const char *sourceLimit, UBool flush)
    {
        UChar *t = reinterpret_cast<UChar*>(*target);
        const UChar *tl = reinterpret_cast<const UChar*>(targetLimit);

        UErrorCode err = U_ZERO_ERROR;
        ::ucnv_toUnicode(uc_, &t, tl, source, sourceLimit, NULL, flush, &err);
        *target = reinterpret_cast<char*>(t);
        return err;
    }
    void Reset()
    {
        ::ucnv_reset(uc_);
    }
};


//-----------------------------------------------------------------------
// CONVERTER FROM UNICODE
//-----------------------------------------------------------------------
class ConverterFromUnicodeICU : public ConvBaseICU, Noncopyable
{
    UConverter *uc_;
public:
    ConverterFromUnicodeICU(const char* name)
    {
        UErrorCode err = U_ZERO_ERROR;
        uc_ = ucnv_open(name, &err);
        if(U_FAILURE(err))
            ExternalError("ucnv_open error");
    }
    ~ConverterFromUnicodeICU()
    {
        ucnv_close(uc_);
    }

    //virtuals
    UErrorCode Convert(char **target, const char *targetLimit, const char **source, const char *sourceLimit, UBool flush)
    {
        const UChar *s = reinterpret_cast<const UChar*>(*source);
        const UChar *sl = reinterpret_cast<const UChar*>(sourceLimit);

        UErrorCode err = U_ZERO_ERROR;
        ::ucnv_fromUnicode(uc_, target, targetLimit, &s, sl, NULL, flush, &err);
        *source = reinterpret_cast<const char*>(s);
        return err;
    }
    void Reset()
    {
        ::ucnv_reset(uc_);
    }
};



//-----------------------------------------------------------------------
// stream converter buffer sizes
const size_t IN_CONVBUF_SIZE = 256;
const size_t OUT_CONVBUF_SIZE = 512;

//-----------------------------------------------------------------------
// InConvStmICU implementation
//-----------------------------------------------------------------------
class InConvStmICU : public InStm, Noncopyable
{
    Ptr<InStm>          stm_;                       // input stream
    Ptr<ConvBaseICU>    conv_;                      // converter
    mutable char        ibuf_[IN_CONVBUF_SIZE];     // input buffer
    mutable char        *iend_;                     // input buffer unconverted data end
    mutable char        obuf_[OUT_CONVBUF_SIZE];    // output buffer
    mutable char        *ocur_;                     // output buffer current position
    mutable char        *oend_;                     // output buffer concerted data end

    size_t Fill() const;

public:
    InConvStmICU(InStm *stm, ConvBaseICU *conv);

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
InConvStmICU::InConvStmICU(InStm *stm, ConvBaseICU *conv)
                            :   stm_(stm),
                                conv_(conv),
                                iend_(ibuf_),
                                ocur_(obuf_),
                                oend_(obuf_)
{
}

//-----------------------------------------------------------------------
bool InConvStmICU::IsEOF() const
{
    return (ocur_ == oend_) && !Fill();
}

//-----------------------------------------------------------------------
size_t InConvStmICU::Fill() const
{
    // fix output pointers
    ocur_ = oend_ = obuf_;

    // read data to input buffer
    iend_ += stm_->Read(iend_, ibuf_ + sizeof(ibuf_) - iend_);
    bool eof = stm_->IsEOF();
    if(iend_ == ibuf_ && !eof)
        InternalError(__FILE__, __LINE__, "InConvStmICU error");

    // convert data
    const char  *pi = ibuf_;
    UErrorCode err = conv_->Convert(&oend_, oend_ + sizeof(obuf_), &pi, iend_, eof);
    if(U_FAILURE(err) && err != U_BUFFER_OVERFLOW_ERROR)
        IOError(UIFileName(), "ucnv: conversion error");

    // fix input data and pointers
    size_t inleft  = iend_ - pi;
    iend_ = ibuf_ + inleft;
    if(inleft)
        ::memmove(ibuf_, pi, inleft);   // move unconverted rest to beginning

    return oend_ - ocur_;
}

//-----------------------------------------------------------------------
char InConvStmICU::GetChar()
{
    if(ocur_ == oend_ && !Fill())
        IOError(UIFileName(), "conv: EOF");
    return *ocur_++;
}

//-----------------------------------------------------------------------
size_t InConvStmICU::Read(void *buffer, size_t max_cnt)
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
void InConvStmICU::UngetChar(char c)
{
    if(ocur_ == obuf_)
        IOError(UIFileName(), "conv: can't unget");
    --ocur_;
}

//-----------------------------------------------------------------------
void InConvStmICU::Rewind()
{
    stm_->Rewind();
    conv_->Reset();
    iend_ = ibuf_;
    ocur_ = oend_ = obuf_;
}


//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
Ptr<InStm> CreateInConvStm(InStm *stm, const char* tocode, const char* fromcode)
{
    Ptr<ConvBaseICU> conv1 = new ConverterToUnicodeUCI(fromcode);
    Ptr<ConvBaseICU> conv2 = new ConverterFromUnicodeICU(tocode);
    Ptr<InStm> stm1 = new InConvStmICU(stm, conv1);
    return new InConvStmICU(stm1, conv2);
}

};  //namespace Fb2ToEpub

#endif
