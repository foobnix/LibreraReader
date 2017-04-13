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


#ifndef FB2TOEPUB__SCANNER_H
#define FB2TOEPUB__SCANNER_H

#include "stream.h"
#include "error.h"
#include <string>
#include <map>
#include <vector>

namespace Fb2ToEpub
{
    //-----------------------------------------------------------------------
    typedef std::map<String, String> AttrMap;

    //-----------------------------------------------------------------------
    class LexScanner : public Object
    {
    public:
        enum TokenType
        {
            // no value
            STOP,
            XMLDECL,
            ENCODING,
            STANDALONE,
            EQ,
            SLASHCLOSE,
            CLOSE,

            // value in c_
            CHAR,

            // value in s_
            VERSION,
            NAME,
            VALUE,
            DATA,
            COMMENT,
            START,
            END
        };

        typedef ParserException::Loc Loc;

        struct Token
        {
            TokenType   type_;
            char        c_;
            String      s_;
            std::size_t size_;  // approximate size of DATA section (valid in skip mode)
            Loc         loc_;

            Token(TokenType type, std::size_t size = 0)                         : type_(type), size_(size) {}
            Token(char c)                                                       : type_(CHAR), c_(c) {}
            Token(TokenType type, const char *s, std::size_t size = 0)          : type_(type), s_(s), size_(size) {}
            Token(TokenType type, const String &s, std::size_t size = 0)        : type_(type), s_(s), size_(size) {}

            static int compare(const Token &t1, const Token &t2)
            {
                int ret = t1.type_ - t2.type_;
                if(ret)
                    return ret;
                switch(t1.type_)
                {
                default:                    return 0;
                case CHAR:                  return static_cast<int>(t1.c_) - static_cast<int>(t2.c_);

                case VERSION:   case NAME:
                case VALUE:     case DATA:
                case COMMENT:   case START:
                case END:                   return t1.s_.compare(t2.s_);
                }
            }
        };

        virtual ~LexScanner() {}
        virtual const Token& GetToken() = 0;
        virtual void UngetToken(const Token &t) = 0;
        virtual bool SetSkipMode(bool newMode) = 0;
        virtual bool SetDataMode(bool newMode) = 0;
        virtual void Error(const String &what) = 0;

        // helpers
        Token LookAhead()
        {
            Token t = GetToken();
            UngetToken(t);
            return t;
        }

        // more helpers
        void SkipAttributes                         ();
        void SkipRestOfElementContent               ();
        void SkipElement                            ();
        void CheckAndSkipElement                    (const String &element);
        void SkipIfElement                          (const String &element);
        void SkipAll                                (const String &element);
        void SkipXMLDeclaration                     ();
        bool IsNextElement                          (const String &element);
        void ParseAttributes                        (AttrMap *attrmap);
        bool BeginElement                           (const String &element, AttrMap *attrmap = NULL);  // returns true if nonempty, false otherwise
        void BeginNotEmptyElement                   (const String &element, AttrMap *attrmap = NULL);
        String SimpleTextElement                    (const String &element, AttrMap *attrmap = NULL);
        void EndElement                             ();

        // text processing helpers
        static void Decode                          (const char *s, std::vector<char> *buf, bool decodeEntities, bool removeLF);    // always removes CR
        static void Encode                          (const char *s, std::vector<char> *buf);
    };

    inline bool operator==(const LexScanner::Token &t1, const LexScanner::Token &t2)   {return !LexScanner::Token::compare(t1, t2);}
    inline bool operator!=(const LexScanner::Token &t1, const LexScanner::Token &t2)   {return LexScanner::Token::compare(t1, t2) != 0;}

    //-----------------------------------------------------------------------
    class ChangeScannerDataMode
    {
        Ptr<LexScanner> s_;
        bool old_;
    public:
        ChangeScannerDataMode(LexScanner *s, bool newMode)  : s_(s), old_(s->SetDataMode(newMode)) {}
        ~ChangeScannerDataMode()                            {s_->SetDataMode(old_);}
    };
    struct SetScannerDataMode : ChangeScannerDataMode {SetScannerDataMode(LexScanner *s) : ChangeScannerDataMode(s, true) {}};
    struct ClrScannerDataMode : ChangeScannerDataMode {ClrScannerDataMode(LexScanner *s) : ChangeScannerDataMode(s, false) {}};

    //-----------------------------------------------------------------------
    Ptr<LexScanner> FB2TOEPUB_DECL CreateScanner(InStm *stm);

};  //namespace Fb2ToEpub

#endif
