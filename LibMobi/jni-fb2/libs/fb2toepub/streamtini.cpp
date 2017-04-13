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


#if 1
#include "hdr.h"

#include <string>
#include <algorithm>
#include <ctype.h>
#include "streamconv.h"
#include "error.h"
#include "tiniconv/tiniconv.h"


namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// CONVERTER WRAPPER
//-----------------------------------------------------------------------
class FB2TOEPUB_DECL ConvTini : Noncopyable
{
    ::tiniconv_ctx_s ctx_;

    typedef std::pair<String, int> TblEntry;
    static int EncToCharset(String code);

public:
    ConvTini(const char* tocode, const char* fromcode, bool translit = true, bool ignore = true);

    bool Convert (const char* * inbuf, size_t *inbytesleft, char* * outbuf, size_t *outbytesleft);
    void Reset();
};

//-----------------------------------------------------------------------
// ConvTini implementation
//-----------------------------------------------------------------------

ConvTini::ConvTini(const char* tocode, const char* fromcode, bool translit, bool ignore)
{
    int ret = ::tiniconv_init(EncToCharset(fromcode), EncToCharset(tocode), TINICONV_OPTION_IGNORE_OUT_ILSEQ, &ctx_);
    if(ret != TINICONV_INIT_OK)
        ExternalError("tiniconv_init error");
}

//-----------------------------------------------------------------------
bool ConvTini::Convert (const char* * inbuf, size_t *inbytesleft, char* * outbuf, size_t *outbytesleft)
{
    int in_size_consumed = 0, out_size_consumed = 0;
    int ret = ::tiniconv_convert(&ctx_, reinterpret_cast<const unsigned char*>(*inbuf), *inbytesleft, &in_size_consumed,
                                        reinterpret_cast<unsigned char*>(*outbuf), *outbytesleft, &out_size_consumed);
    switch(ret)
    {
    case TINICONV_CONVERT_IN_ILSEQ:
    case TINICONV_CONVERT_OUT_ILSEQ:
    default:
        return false;

    case TINICONV_CONVERT_OK:
    case TINICONV_CONVERT_IN_TOO_SMALL:
    case TINICONV_CONVERT_OUT_TOO_SMALL:
        *inbuf          += in_size_consumed;
        *inbytesleft    -= in_size_consumed;
        *outbuf         += out_size_consumed;
        *outbytesleft   -= out_size_consumed;
        return true;
    }
}

//-----------------------------------------------------------------------
void ConvTini::Reset()
{
    if(ctx_.flushwc)
    {
        ucs4_t buf;
        (*(ctx_.flushwc))(&ctx_, &buf);
    }
}

//-----------------------------------------------------------------------
// It is not optimized, for we call this function just a few times.
inline bool nonalnum(int c) {return !isalnum(c);}
int ConvTini::EncToCharset(String code)
{
    static const TblEntry table[] =
    {
        TblEntry("ASCII",           TINICONV_CHARSET_ASCII),
        TblEntry("CP1250",          TINICONV_CHARSET_CP1250),
        TblEntry("CP1251",          TINICONV_CHARSET_CP1251),
        TblEntry("CP1252",          TINICONV_CHARSET_CP1252),
        TblEntry("CP1253",          TINICONV_CHARSET_CP1253),
        TblEntry("CP1254",          TINICONV_CHARSET_CP1254),
        TblEntry("CP1255",          TINICONV_CHARSET_CP1255),
        TblEntry("CP1256",          TINICONV_CHARSET_CP1256),
        TblEntry("CP1257",          TINICONV_CHARSET_CP1257),

#if !defined(TINICONV_NO_ASIAN_ENCODINGS)
        TblEntry("CP1258",          TINICONV_CHARSET_CP1258),
#endif

        TblEntry("WINDOWS1250",     TINICONV_CHARSET_CP1250),
        TblEntry("WINDOWS1251",     TINICONV_CHARSET_CP1251),
        TblEntry("WINDOWS1252",     TINICONV_CHARSET_CP1252),
        TblEntry("WINDOWS1253",     TINICONV_CHARSET_CP1253),
        TblEntry("WINDOWS1254",     TINICONV_CHARSET_CP1254),
        TblEntry("WINDOWS1255",     TINICONV_CHARSET_CP1255),
        TblEntry("WINDOWS1256",     TINICONV_CHARSET_CP1256),
        TblEntry("WINDOWS1257",     TINICONV_CHARSET_CP1257),

#if !defined(TINICONV_NO_ASIAN_ENCODINGS)
        TblEntry("WINDOWS1258",     TINICONV_CHARSET_CP1258),
        TblEntry("CP936",           TINICONV_CHARSET_CP936),
        TblEntry("MS936",           TINICONV_CHARSET_CP936),
        TblEntry("WINDOWS936",      TINICONV_CHARSET_CP936),
#endif

        TblEntry("MSCYRL",          TINICONV_CHARSET_CP1251),

#if !defined(TINICONV_NO_ASIAN_ENCODINGS)
        TblEntry("GB2312",          TINICONV_CHARSET_GB2312),
        TblEntry("GBK",             TINICONV_CHARSET_GBK),
        TblEntry("ISO2022JP",       TINICONV_CHARSET_ISO_2022_JP),
#endif

        TblEntry("ISO88591",        TINICONV_CHARSET_ISO_8859_1),
        TblEntry("ISO88592",        TINICONV_CHARSET_ISO_8859_2),
        TblEntry("ISO88593",        TINICONV_CHARSET_ISO_8859_3),
        TblEntry("ISO88594",        TINICONV_CHARSET_ISO_8859_4),
        TblEntry("ISO88595",        TINICONV_CHARSET_ISO_8859_5),
        TblEntry("ISO88596",        TINICONV_CHARSET_ISO_8859_6),
        TblEntry("ISO88597",        TINICONV_CHARSET_ISO_8859_7),
        TblEntry("ISO88598",        TINICONV_CHARSET_ISO_8859_8),
        TblEntry("ISO88599",        TINICONV_CHARSET_ISO_8859_9),
        TblEntry("ISO885910",       TINICONV_CHARSET_ISO_8859_10),

#if !defined(TINICONV_NO_ASIAN_ENCODINGS)
        TblEntry("ISO885911",       TINICONV_CHARSET_ISO_8859_11),
#endif

        TblEntry("ISO885913",       TINICONV_CHARSET_ISO_8859_13),
        TblEntry("ISO885914",       TINICONV_CHARSET_ISO_8859_14),
        TblEntry("ISO885915",       TINICONV_CHARSET_ISO_8859_15),
        TblEntry("ISO885916",       TINICONV_CHARSET_ISO_8859_16),
        TblEntry("CP866",           TINICONV_CHARSET_CP866),
        TblEntry("KOI8R",           TINICONV_CHARSET_KOI8_R),
        TblEntry("CSKOI8R",         TINICONV_CHARSET_KOI8_R),
        TblEntry("KOI8RU",          TINICONV_CHARSET_KOI8_RU),
        TblEntry("KOI8U",           TINICONV_CHARSET_KOI8_U),
        TblEntry("MACCYRILLIC",     TINICONV_CHARSET_MACCYRILLIC),
        TblEntry("UCS2",            TINICONV_CHARSET_UCS_2),
        TblEntry("UCS2BE",          TINICONV_CHARSET_UCS_2),
        TblEntry("UCS2LE",          TINICONV_CHARSET_UCS_2),
        TblEntry("UTF16",           TINICONV_CHARSET_UCS_2),
        TblEntry("UTF16BE",         TINICONV_CHARSET_UCS_2),
        TblEntry("UTF16LE",         TINICONV_CHARSET_UCS_2),
        TblEntry("UTF7",            TINICONV_CHARSET_UTF_7),
        TblEntry("UTF8",            TINICONV_CHARSET_UFT_8),

#if !defined(TINICONV_NO_ASIAN_ENCODINGS)
        TblEntry("CHINESE",         TINICONV_CHARSET_CHINESE),
        TblEntry("BIG5",            TINICONV_CHARSET_BIG5)
#endif
    };

    // remove all non digits and letters, convert to uppercase
    String::iterator it = code.begin(), it_end = code.end();
    it_end = std::remove_if(it, it_end, nonalnum);
    std::transform(it, it_end, it, toupper);
    code = code.substr(0, it_end - it);

    // find
    const TblEntry *p = table, *p_end = table + sizeof(table)/sizeof(TblEntry);
    for(; p < p_end; ++p)
        if(p->first == code)
            return p->second;

    // not found
    return -1;
}


//-----------------------------------------------------------------------
// stream converter buffer sizes
const size_t IN_CONVBUF_SIZE = 256;
const size_t OUT_CONVBUF_SIZE = 512;

//-----------------------------------------------------------------------
// InConvStmTini implementation
//-----------------------------------------------------------------------
class InConvStmTini : public InStm, Noncopyable
{
    Ptr<InStm>          stm_;                       // input stream
    mutable ConvTini    conv_;                      // converter
    mutable char        ibuf_[IN_CONVBUF_SIZE];     // input buffer
    mutable char        *iend_;                     // input buffer unconverted data end
    mutable char        obuf_[OUT_CONVBUF_SIZE];    // output buffer
    mutable char        *ocur_;                     // output buffer current position
    mutable char        *oend_;                     // output buffer concerted data end

    size_t Fill() const;

public:
    InConvStmTini(InStm *stm, const char* tocode, const char* fromcode);

    //virtuals
    bool        IsEOF() const;
    char        GetChar();
    size_t      Read(void *buffer, size_t max_cnt);
    void        UngetChar(char c);
    void        Rewind();
    String      UIFileName() const {return stm_->UIFileName();}
};

//-----------------------------------------------------------------------
InConvStmTini::InConvStmTini(InStm *stm, const char* tocode, const char* fromcode)
                            :   stm_(stm),
                                conv_(tocode, fromcode),
                                iend_(ibuf_),
                                ocur_(obuf_),
                                oend_(obuf_)
{
}

//-----------------------------------------------------------------------
bool InConvStmTini::IsEOF() const
{
    return (ocur_ == oend_) && (stm_->IsEOF() || !Fill());
}

//-----------------------------------------------------------------------
size_t InConvStmTini::Fill() const
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
    if(!conv_.Convert(&pi, &inleft, &oend_, &outleft))
        IOError(UIFileName(), "tiniconv: invalid codesymbol");

    // fix input data and pointers
    iend_ = ibuf_ + inleft;
    if(inleft)
        ::memmove(ibuf_, pi, inleft);   // move unconverted rest to beginning

    return oend_ - ocur_;
}

//-----------------------------------------------------------------------
char InConvStmTini::GetChar()
{
    if(ocur_ == oend_ && !Fill())
        IOError(UIFileName(), "tiniconv: EOF");
    return *ocur_++;
}

//-----------------------------------------------------------------------
size_t InConvStmTini::Read(void *buffer, size_t max_cnt)
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
void InConvStmTini::UngetChar(char c)
{
    if(ocur_ == obuf_)
        IOError(UIFileName(), "tiniconv: can't unget");
    --ocur_;
}

//-----------------------------------------------------------------------
void InConvStmTini::Rewind()
{
    stm_->Rewind();
    conv_.Reset();
    iend_ = ibuf_;
    ocur_ = oend_ = obuf_;
}


//-----------------------------------------------------------------------
//-----------------------------------------------------------------------
Ptr<InStm> CreateInConvStm(InStm *stm, const char* tocode, const char* fromcode)
{
    return new InConvStmTini(stm, tocode, fromcode);
}

};  //namespace Fb2ToEpub
#endif
