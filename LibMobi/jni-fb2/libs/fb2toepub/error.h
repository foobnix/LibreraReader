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


#ifndef FB2TOEPUB__ERROR_H
#define FB2TOEPUB__ERROR_H

#include <string>
#include <stdarg.h>
#include "types.h"

namespace Fb2ToEpub
{
	
    //-----------------------------------------------------------------------
    // Generic external error exception
	struct ExternalException : public std::exception
	{
	public:
		ExternalException(const String &what)
			: message(what) {}
		virtual ~ExternalException() throw() {}
		virtual const char *what() const throw() { return message.c_str(); }
	protected:
		const std::string message;
	};


    //-----------------------------------------------------------------------
    // IO error exception
	struct IOException : public ExternalException
    {
    public:
	    explicit IOException(const String &file, const String &what)
		    : myFile(file)
		    , ExternalException(what) {}
	    virtual ~IOException() throw() {}
	    virtual const char *File() const throw() { return myFile.c_str(); };
    protected:
	    const std::string myFile;
    };


	class InternalException : public IOException
	{
	public:
		explicit InternalException(const String &file, int line, const String &what)
			: myLine(line)
			, IOException(file, what) {}
		virtual ~InternalException() throw() {}
		virtual int Line() const throw() { return myLine; }
	protected:
		const int myLine;
	};


    //-----------------------------------------------------------------------
    // Parser error exception
	struct ParserException : public IOException
    {
        struct Loc
        {
            int fstLn_, lstLn_, fstCol_, lstCol_;
            Loc()                                               : fstLn_(1), lstLn_(1), fstCol_(1), lstCol_(1) {}
            Loc(int fstLn, int lstLn, int fstCol, int lstCol)   : fstLn_(fstLn), lstLn_(lstLn), fstCol_(fstCol), lstCol_(lstCol) {}
        };
	    
    public:
	    explicit ParserException(const String &file, Loc loc, const String &what)
		    : myLocation(loc)
		    , IOException(file, what) {}
	    virtual ~ParserException() throw() {}
	    virtual const Loc&      Location() const throw() { return myLocation; }
    protected:
	    const Loc myLocation;
    };


    //-----------------------------------------------------------------------
    // font error exception
	struct FontException : public IOException
    {
    public:
	    explicit FontException(const String &file, const String &what)
		    : IOException(file, what) {}
	    virtual ~FontException() throw() {}
	};

    //-----------------------------------------------------------------------
    inline void ExternalError(const String &what)
        { throw ExternalException(what); }
    inline void InternalError(const String &file, int line, const String &what)
        { throw InternalException(file, line, what); }
    inline void IOError(const String &file, const String &what)
        { throw IOException(file, what); }
    inline void ParserError(const String &file, const ParserException::Loc &loc, const String &what)
    { 
			throw ParserException(file, loc, what); 
	}
    inline void FontError(const String &file, const String &what)
        { throw FontException(file, what);}

};  //namespace Fb2ToEpub

#endif
