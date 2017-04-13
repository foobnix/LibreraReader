/*
    Copyright (C) 2010 Alexey Bobkov

    This file is part of Fb2toepub converter.

    Fb2toepub converter is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Fb2toepub converter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Fb2toepub converter.  If not, see <http://www.gnu.org/licenses/>.
*/

%option c++ 8bit nodefault noyywrap never-interactive


%{
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include <stdlib.h>

#include "scanner.h"
#include <vector>

namespace Fb2ToEpub
{
    static const LexScanner::Token  lt      (LexScanner::DATA, "&lt;", 4),
                                    gt      (LexScanner::DATA, "&gt;", 4),
                                    amp     (LexScanner::DATA, "&amp;", 5),
                                    apos    (LexScanner::DATA, "&apos;", 6),
                                    quot    (LexScanner::DATA, "&quot;", 6),
                                    unknown (LexScanner::DATA, "?", 1);

    //-----------------------------------------------------------------------
    class ScannerImpl : public Fb2ToEpub::LexScanner, public yyFlexLexer, Noncopyable
    {
        Ptr<InStm>	                stm_;
        strvector                   tagStack_;
        std::vector<Token>          tokenStack_;
        bool                        skipMode_;
        bool                        dataMode_;
        int                         doctypeCnt_;
        Loc                         loc_;
        int                         stateCaller_;
        bool                        attrHasValue_;
        Token                       last_;

        Token ScanToken();

        void ScanAndConcatenateTo(Token *t)
        {
            for(;;)     // concatenate all DATA together
            {
                Token t1 = ScanToken();
                t1.loc_ = loc_;

                if(t1.type_ != t->type_)
                {
                    UngetToken(t1);
                    return;
                }

                t->s_           += t1.s_;
                t->size_        += t1.size_;
                t->loc_.lstLn_  = t1.loc_.lstLn_;
                t->loc_.lstCol_ = t1.loc_.lstCol_;
            }
        }

        void NewLn()
        {
            ++loc_.lstLn_;
            loc_.lstCol_ = 1;
        }

        void OnError(const Loc &loc, const String &what)
        {
            ParserError(stm_->UIFileName(), loc, what);
        }

    protected:
        //virtual
        void LexerError(const char* msg)
        {
            ExternalError(msg);
        }

    public:
        explicit ScannerImpl(InStm *stm)
                            :   stm_            (stm),
                                skipMode_       (false),
                                dataMode_       (false),
                                doctypeCnt_     (0),
                                loc_            (1,1,1,1),
                                stateCaller_    (0),
                                attrHasValue_   (false),
                                last_           (STOP)
        {
        }

        //-----------------------------------------------------------------------
        //virtual
        const Token& GetToken()
        {
            while(tokenStack_.size())
            {
                Token t = tokenStack_.back();
                tokenStack_.pop_back();
                if(t.type_ != DATA || dataMode_)
                    return last_ = t;
            }

            Token t = ScanToken();
            t.loc_ = loc_;
            if(t.type_ == DATA || t.type_ == VALUE)
                ScanAndConcatenateTo(&t);

            return last_ = t;
        }
        
        //-----------------------------------------------------------------------
        //virtual
        void UngetToken(const Token &t)
        {
            tokenStack_.push_back(t);
        }

        //-----------------------------------------------------------------------
        //virtual
        bool SetSkipMode(bool newMode)
        {
            bool old = skipMode_;
            skipMode_ = newMode;
            return old;
        }

        //-----------------------------------------------------------------------
        //virtual
        bool SetDataMode(bool newMode)
        {
            bool old = dataMode_;
            dataMode_ = newMode;
            return old;
        }

        //-----------------------------------------------------------------------
        //virtual
        void Error(const String &what)
        {
            OnError(last_.loc_, what);
        }

        //-----------------------------------------------------------------------
        //virtual
        int LexerInput(char* buf, int max_size);
    };
};

static Fb2ToEpub::LexScanner::Token yyterminate()
{
    return Fb2ToEpub::LexScanner::STOP;
}
#define YY_USER_ACTION  {\
                            loc_.fstLn_ = loc_.lstLn_; \
                            loc_.fstCol_ = loc_.lstCol_; \
                            loc_.lstCol_ += yyleng; \
                            /*printf("line: %d state: %d act: %d len: %d \"%s\"\n", loc_.fstLn_, (YY_START), yy_act, yyleng, yytext);*/ \
                        }
#define YY_DECL	 Fb2ToEpub::LexScanner::Token Fb2ToEpub::ScannerImpl::ScanToken()

%}


nl              (\r\n|\r|\n)
ws              [ \t]+

vervalue        \""1."[0-9]+\"|\'"1."[0-9]+\'
encname         [A-Za-z]([A-Za-z0-9._]|"-")*
encvalue        \"{encname}\"|\'{encname}\'
sdname          "yes"|"no"
sdvalue         \"{sdname}\"|\'{sdname}\'

chardata        [^<&>"'\]\r\n]*
letter          [A-Za-z]
digit           [0-9]
namestart       ({letter}|"_"|":")
namechar        ({letter}|{digit}|"."|"-"|"_"|":")
name            {namestart}{namechar}*
entityref       &{name};
charref         ("&#"[0-9]+;)|("&#x"[0-9a-fA-F]+;)
reference       {entityref}|{charref}
data            {chardata}?({reference}{chardata}?)*

stagstart       "<"{name}
etagstart       "</"{name}
attrvalue1      ([^<&"\r\n]|{reference})*
attrvalue2      ([^<&'\r\n]|{reference})*

comment         ([^-\r\n]|"-"[^-\r\n])*
cdatablock      ([^\]\r\n]|"]"[^\]\r\n]|"]]""]"*[^>\]\r\n])*
xmlreserved     ([^\?\r\n]|"?"*[^>\?\r\n])*


/*
 * The X<n>, X<n>_WS modes are used for XML header parsing.
 * The OUTSIDE mode is used outside XML top-level element.
 * The MARKUP, MARKUP1 and MARKUP2 modes are used inside markup.
 * The D<n> modes are used for the content of elements, i.e.,
 * between the ">" and "<" of element tags.
 * The DOCTYPE mode is used to skip DOCTYPE definition.
 * The COMMENT, CDB, RESERVED modes are used to ignore comment, CDATA block, XML reserved stuff
 */

%s X0 X1 X2 X3 X4 X0_WS X3_WS X4_WS
%s OUTSIDE MARKUP MARKUP1 MARKUP2 D1 D2 DOCTYPE
%s COMMENT CDB RESERVED


%%

    /* XML declaration */

<INITIAL>"<?xml"                {BEGIN(X0_WS); return Token(XMLDECL);}
<X0_WS>{ws}                     {BEGIN(X0);}
<X0_WS>{nl}                     {NewLn(); BEGIN(X0);}
<X0>"version"                   {BEGIN(X1);}
<X1>"="                         {BEGIN(X2);}
<X2>{vervalue}                  {
                                    BEGIN(X3_WS);
                                    yytext[yyleng-1] = '\0';
                                    return Token(VERSION, yytext+1);
                                }
<X3_WS>{ws}                     {BEGIN(X3);}
<X3_WS>{nl}                     {NewLn(); BEGIN(X3);}
<X3>"encoding"                  {return ENCODING;}
<X3>"="                         {return EQ;}
<X3>{encvalue}                  {
                                    BEGIN(X4_WS);
                                    yytext[yyleng-1] = '\0';
                                    return Token(VALUE, yytext+1);
                                }
<X4_WS>{ws}                     {BEGIN(X4);}
<X4_WS>{nl}                     {NewLn(); BEGIN(X4);}
<X3,X4>"standalone"             {BEGIN(X4); return STANDALONE;}
<X4>"="                         {return EQ;}
<X4>{sdvalue}                   {
                                    yytext[yyleng-1] = '\0';
                                    return Token(VALUE, yytext+1);
                                }
<X3_WS,X3,X4_WS,X4>"?>"         {BEGIN(OUTSIDE); return CLOSE;}
<X0,X1,X2,X3,X4>{ws}            {}
<X0,X1,X2,X3,X4>{nl}            {NewLn();}
<X0,X1,X2,X3,X4>.               {OnError(loc_, "xml declaration: unexpected character"); yyterminate();}


    /* Skip comment */

<D1,D2>"<!--"                   {stateCaller_ = D1; BEGIN(COMMENT);}
<OUTSIDE>"<!--"                 {stateCaller_ = OUTSIDE; BEGIN(COMMENT);}
<COMMENT>{comment}              {/* eat */}
<COMMENT>"-"?{nl}               {NewLn();}
<COMMENT>"-->"                  {BEGIN(stateCaller_);}


    /* Skip CDATA block */

<D1,D2>"<![CDATA["              {stateCaller_ = D1; BEGIN(CDB);}
<OUTSIDE>"<![CDATA["            {stateCaller_ = OUTSIDE; BEGIN(CDB);}
<CDB>{cdatablock}               {/* eat */}
<CDB>("]")*{nl}                 {NewLn();}
<CDB>("]")*"]]>"                {BEGIN(stateCaller_);}


    /* Skip DOCTYPE */

<OUTSIDE>"<!DOCTYPE"            {doctypeCnt_ = 1; BEGIN(DOCTYPE);}
<DOCTYPE>"<"                    {++doctypeCnt_;}
<DOCTYPE>[^<>\r\n]*             {/* eat */}
<DOCTYPE>{nl}                   {NewLn();}
<DOCTYPE>">"                    {
                                    if(--doctypeCnt_ <= 0)
                                        BEGIN(OUTSIDE);
                                }


    /* Skip reserved xml element */

<D1,D2>"<?xml"                  {stateCaller_ = D1; BEGIN(RESERVED);}
<OUTSIDE>"<?xml"                {stateCaller_ = OUTSIDE; BEGIN(RESERVED);}
<RESERVED>{xmlreserved}         {/* eat */}
<RESERVED>("?")*{nl}            {NewLn();}
<RESERVED>("?")*"?>"            {BEGIN(stateCaller_);}


    /* Content */

<OUTSIDE>{ws}                   {}
<OUTSIDE>{nl}                   {NewLn();}
<D1,D2>{data}                   {
                                    BEGIN(D1);
                                    if(dataMode_)
                                        return  skipMode_ ?
                                                Token(DATA, yyleng) :
                                                Token(DATA, yytext, yyleng);
                                }
<D1,D2>{nl}	                    {
                                    NewLn();
                                    BEGIN(D1);
                                    if(dataMode_)
                                        return  skipMode_ ?
                                                Token(DATA, 1) :
                                                Token(DATA, "\n", 1);
                                }
<D1,D2>"]"*                     {
                                    BEGIN(yyleng >= 2 ? D2 : D1);   // if number of "]" >= 2, disable ">"
                                    if(dataMode_)
                                        return  skipMode_ ?
                                                Token(DATA, yyleng) :
                                                Token(DATA, yytext, yyleng);
                                }
<D1>">"                         {
                                    if(dataMode_)
                                        return gt;
                                }
<D1,D2>"'"                      {
                                    BEGIN(D1);
                                    if(dataMode_)
                                        return apos;
                                }
<D1,D2>"\""                     {
                                    BEGIN(D1);
                                    if(dataMode_)
                                        return quot;
                                }
<D1,D2,OUTSIDE>{stagstart}      {
                                    char *tagName = &yytext[1];
                                    tagStack_.push_back(tagName);
                                    BEGIN(MARKUP);
                                    return Token(START, tagName);
                                }
<D1,D2>{etagstart}              {
                                    char *tagName = &yytext[2];
                                    if(!tagStack_.size())
                                        OnError(loc_, "tag stack is empty #0");
                                    if(tagStack_.back().compare(tagName))
                                        OnError(loc_, "tag mismatch");
                                    tagStack_.pop_back();
                                    BEGIN(MARKUP);
                                    return Token(END, tagName);
                                }
<D1,D2,OUTSIDE>"<!"             {OnError(loc_, "not implemented"); yyterminate();}


    /* Garbage */

<OUTSIDE>.                      {/* ignore outside garbage */}
<D1,D2>.                        {
                                    // error character - try to process
                                    BEGIN(D1);
                                    if(dataMode_)
                                        switch(yytext[0])
                                        {
                                        case '<':   return lt;
                                        case '>':   return gt;
                                        case '&':   return amp;
                                        case '\'':  return apos;
                                        case '"':   return quot;
                                        default:    return unknown;
                                        }
                                }


    /* Markup */

<MARKUP>{ws}                    {}
<MARKUP>{nl}                    {NewLn();}
<MARKUP>"="                     {return EQ;}
<MARKUP>{name}	                {attrHasValue_ = false; return Token(NAME, yytext);}
<MARKUP>"\""                    {BEGIN(MARKUP1);}
<MARKUP>"'"                     {BEGIN(MARKUP2);}
<MARKUP1>{attrvalue1}           {
                                    attrHasValue_ = true;
                                    if(skipMode_)
                                        return Token(VALUE);
                                    std::vector<char> buf;
                                    Decode(yytext, &buf, true, true);
                                    return Token(VALUE, &buf[0]);
                                }
<MARKUP2>{attrvalue2}           {
                                    attrHasValue_ = true;
                                    if(skipMode_)
                                        return Token(VALUE);
                                    std::vector<char> buf;
                                    Decode(yytext, &buf, true, true);
                                    return Token(VALUE, &buf[0]);
                                }
<MARKUP1,MARKUP2>{nl}           {
                                    attrHasValue_ = true;
                                    NewLn();
                                    return skipMode_ ? Token(VALUE) : Token(VALUE, "\n");
                                }
<MARKUP1>"\""                   {
                                    BEGIN(MARKUP);
                                    if(!attrHasValue_)
                                        return Token(VALUE);
                                    attrHasValue_ = false;
                                }
<MARKUP2>"'"                    {
                                    BEGIN(MARKUP);
                                    if(!attrHasValue_)
                                        return Token(VALUE);
                                    attrHasValue_ = false;
                                }
<MARKUP>"/>"                    {
                                    if(!tagStack_.size())
                                        OnError(loc_, "tag stack is empty #1");
                                    tagStack_.pop_back();
                                    BEGIN(tagStack_.size() ? D1 : OUTSIDE);
                                    return SLASHCLOSE;
                                }
<MARKUP>">"                     {
                                    BEGIN(tagStack_.size() ? D1 : OUTSIDE);
                                    return CLOSE;
                                }


    /* Default */

.|{nl}                          {OnError(loc_, "default: unrecognized char"); yyterminate();}

%%

namespace Fb2ToEpub
{
    int ScannerImpl::LexerInput(char* buf, int max_size)
    {
        return stm_->Read(buf, max_size);
    }


    Ptr<LexScanner> CreateScanner(InStm *stm)
    {
        return new ScannerImpl(stm);
    }
};

int yyFlexLexer::yylex()
{
    return -1;
}
