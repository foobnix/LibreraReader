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

#include "mangling.h"
#include "error.h"
#include "zlib.h"

namespace Fb2ToEpub
{


//-----------------------------------------------------------------------
// stream converter buffer sizes
const size_t IN_CONVBUF_SIZE = 1024;
const size_t OUT_CONVBUF_SIZE = 256;

//-----------------------------------------------------------------------
// InDeflateStm
//-----------------------------------------------------------------------
class InDeflateStm : public InStm, Noncopyable
{
    Ptr<InStm>          stm_;                       // input stream
    mutable ::z_stream  df_;                        // converter
    mutable char        ibuf_[IN_CONVBUF_SIZE];     // input buffer
    mutable char        *iend_;                     // input buffer unconverted data end
    mutable char        obuf_[OUT_CONVBUF_SIZE];    // output buffer
    mutable char        *ocur_;                     // output buffer current position
    mutable char        *oend_;                     // output buffer concerted data end

    void    DeflateInit();
    size_t  Fill() const;

public:
    InDeflateStm(InStm *stm);
    ~InDeflateStm();

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
InDeflateStm::InDeflateStm(InStm *stm)
                            :   stm_(stm),
                                iend_(ibuf_),
                                ocur_(obuf_),
                                oend_(obuf_)
{
    DeflateInit();
}

//-----------------------------------------------------------------------
InDeflateStm::~InDeflateStm()
{
    ::deflateEnd(&df_);
}

//-----------------------------------------------------------------------
void InDeflateStm::DeflateInit()
{
    df_.zalloc  = Z_NULL;
    df_.zfree   = Z_NULL;
    df_.opaque  = Z_NULL;
    int ret = ::deflateInit2(&df_, Z_BEST_COMPRESSION, Z_DEFLATED, -15, 8, Z_DEFAULT_STRATEGY);     // some magic numbers
    if (ret != Z_OK)
        IOError(UIFileName(), "InDeflateStm: deflateInit2 error");
}

//-----------------------------------------------------------------------
bool InDeflateStm::IsEOF() const
{
    return (ocur_ == oend_) && !Fill();
}

//-----------------------------------------------------------------------
size_t InDeflateStm::Fill() const
{
    df_.next_out = reinterpret_cast<Bytef*>(obuf_);
    df_.avail_out = sizeof(obuf_);

    int flush;
    do
    {
        // read data to input buffer
        iend_ += stm_->Read(iend_, ibuf_ + sizeof(ibuf_) - iend_);

        flush = stm_->IsEOF() ? Z_FINISH : Z_NO_FLUSH;
        df_.next_in = reinterpret_cast<Bytef*>(ibuf_);
        df_.avail_in = iend_ - ibuf_;

        int ret = ::deflate(&df_, flush);
        if(ret == Z_STREAM_ERROR)
            IOError(UIFileName(), "InDeflateStm: stream error");

        // fix input data and pointers
        iend_ = ibuf_ + df_.avail_in;
        if(df_.avail_in)
            ::memmove(ibuf_, df_.next_in, df_.avail_in); // move unconverted rest to beginning
    }
    while(df_.avail_out == sizeof(obuf_) && flush != Z_FINISH);

    // fix output pointers
    ocur_ = obuf_;
    oend_ = obuf_ + (sizeof(obuf_) - df_.avail_out);
    return oend_ - ocur_;
}

//-----------------------------------------------------------------------
char InDeflateStm::GetChar()
{
    if(ocur_ == oend_ && !Fill())
        IOError(UIFileName(), "InDeflateStm: EOF");
    return *ocur_++;
}

//-----------------------------------------------------------------------
size_t InDeflateStm::Read(void *buffer, size_t max_cnt)
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
void InDeflateStm::UngetChar(char c)
{
    if(ocur_ == obuf_)
        IOError(UIFileName(), "InDeflateStm: can't unget");
    --ocur_;
}

//-----------------------------------------------------------------------
void InDeflateStm::Rewind()
{
    stm_->Rewind();
    iend_ = ibuf_;
    ocur_ = oend_ = obuf_;

    ::deflateEnd(&df_);
    DeflateInit();
}


//-----------------------------------------------------------------------
// InManglingStm
//-----------------------------------------------------------------------
class InManglingStm : public InStm, Noncopyable
{
    Ptr<InStm>          stm_;
    const unsigned char *key_;
    size_t              keySize_, maxSize_, keyPos_, pos_;

public:
    InManglingStm(InStm *stm, const unsigned char *key, size_t keySize, size_t maxSize)
        : stm_(stm), key_(key), keySize_(keySize), maxSize_(maxSize), keyPos_(0), pos_(0) {}

    //virtuals
    bool        IsEOF() const                       {return stm_->IsEOF();}
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c)                   {IOError(UIFileName(), "InManglingStm: unget not implemented");}
    void        Rewind()                            {stm_->Rewind(); keyPos_ = pos_ = 0;}
    String      UIFileName() const                  {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
char InManglingStm::GetChar()
{
    if(pos_ >= maxSize_)
        return stm_->GetChar();
    else
    {
        char c = stm_->GetChar() ^ key_[keyPos_++];
        if(keyPos_ >= keySize_)
            keyPos_= 0;
        ++pos_;
        return c;
    }
}

//-----------------------------------------------------------------------
size_t InManglingStm::Read(void *buffer, size_t max_cnt)
{
    size_t cnt = stm_->Read(buffer, max_cnt);
    if(pos_ >= maxSize_ || !cnt)
        return cnt;

    char *cb = reinterpret_cast<char*>(buffer);
    max_cnt = cnt;
    for(;;)
    {
        size_t to_mangle = keySize_ - keyPos_;
        if (to_mangle > maxSize_ - pos_)
            to_mangle = maxSize_ - pos_;
        if (to_mangle > max_cnt)
            to_mangle = max_cnt;

        for(size_t u = to_mangle; u-- > 0;)
            *cb++ ^= key_[keyPos_++];

        if(keyPos_ >= keySize_)
            keyPos_ = 0;

        if((pos_ += to_mangle) >= maxSize_ || !(max_cnt -= to_mangle))
            return cnt;
    }
}


//-----------------------------------------------------------------------
Ptr<InStm> CreateManglingStm(InStm *stm, const unsigned char *key, size_t keySize, size_t maxSize)
{
    Ptr<InStm> tmp = new InDeflateStm(stm);
    return new InManglingStm(tmp, key, keySize, maxSize);
}


};  //namespace Fb2ToEpub
