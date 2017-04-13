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

#include "error.h"
#include <sstream>

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
static String GetFName(const String file)
{
    String::size_type pos = file.find_last_of("\\/:");
    if(pos != String::npos)
        return file.substr(pos+1);
    else
        return file;
}


//-----------------------------------------------------------------------
// Internal error exception implementation
class InternalExceptionImpl : public ExceptionImpl<InternalException>
{
public:
    InternalExceptionImpl(const String &file, int line, const String &what)
                            : file_(file), line_(line)
    {
        std::ostringstream txt;
        txt << "internal converter error, " << file << "(" << line << ") : " << what;
        Init(txt.str());
    }

    //virtuals
    const String&   File() const    {return file_;}
    int             Line() const    {return line_;}

private:
    String  file_;
    int     line_;
};
void InternalException::Raise(const String &file, int line, const String &what)
{
    throw InternalExceptionImpl(file, line, what);
}


//-----------------------------------------------------------------------
// External error exception implementation
void ExternalException::Raise(const String &what)
{
    throw ExceptionImpl<ExternalException>(what);
}


//-----------------------------------------------------------------------
// IO error exception implementation
class IOExceptionImpl : public ExceptionImpl<IOException>
{
public:
    IOExceptionImpl(const String &file, const String &what) : file_(file)
    {
        std::ostringstream txt;
        txt << GetFName(file) << ": IO error: " << what;
        Init(txt.str());
    }

    //virtuals
    const String& File() const {return file_;}

private:
    String  file_;
};
void IOException::Raise(const String &file, const String &what)
{
    throw IOExceptionImpl(file, what);
}


//-----------------------------------------------------------------------
// Parser error exception
class ParserExceptionImpl : public ExceptionImpl<ParserException>
{
public:
    ParserExceptionImpl(const String &file, const Loc &loc, const String &what)
                        : file_(file), loc_(loc)
    {
        std::ostringstream txt;
        txt << GetFName(file_);
        txt << "(" << loc.fstLn_ << "," << loc.fstCol_;
        txt << "-" << loc.lstLn_ << "," << loc.lstCol_;
        txt << ") : parser error: " << what;
        Init(txt.str());
    }

    //virtuals
    const String&   File() const        {return file_;}
    const Loc&      Location() const    {return loc_;}

private:
    String  file_;
    Loc     loc_;
};
void ParserException::Raise(const String &file, const Loc &loc, const String &what)
{
    throw ParserExceptionImpl(file, loc, what);
}


//-----------------------------------------------------------------------
// Font error exception implementation
class FontExceptionImpl : public ExceptionImpl<FontException>
{
public:
    FontExceptionImpl(const String &file, const String &what) : file_(file)
    {
        std::ostringstream txt;
        txt << "font error, " << GetFName(file) << " : " << what;
        Init(txt.str());
    }

    //virtuals
    const String& File() const {return file_;}

private:
    String  file_;
};
void FontException::Raise(const String &file, const String &what)
{
    throw FontExceptionImpl(file, what);
}


};  //namespace Fb2ToEpub
