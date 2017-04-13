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


#ifndef FB2TOEPUB__TYPES_H
#define FB2TOEPUB__TYPES_H

#include "config.h"
#include <string>
#include <string.h>
#include <vector>

namespace Fb2ToEpub
{

#if FB2TOEPUB_NO_STD_STRING_COMPARE

class String : public std::string
{
public:
    String ()                           {}
    String (const char *p)              : std::string (p) {}
    String (const std::string &s)       : std::string (s) {}

    int compare(const String &x) const
    {
        return strcmp(c_str(), x.c_str());
    }
    int compare(size_type offset, size_type no, const String &x) const
    {
        return strncmp(c_str()+offset, x.c_str(), no);
    }
};
inline bool operator==(const String &x1, const String &x2)  {return x1.compare(x2) == 0;}
inline bool operator!=(const String &x1, const String &x2)  {return x1.compare(x2) != 0;}
inline bool operator< (const String &x1, const String &x2)  {return x1.compare(x2) < 0;}
inline bool operator> (const String &x1, const String &x2)  {return x1.compare(x2) > 0;}
inline bool operator<=(const String &x1, const String &x2)  {return x1.compare(x2) <= 0;}
inline bool operator>=(const String &x1, const String &x2)  {return x1.compare(x2) >= 0;}

#else

typedef std::string String;

#endif


/*
// unconst
template<typename T> struct Unconst
{
    typedef T Type;
    static T* cast(T *p) {return p;}
};
template<typename T> struct Unconst<const T>
{
    typedef T Type;
    static T* cast(const T *p) {return const_cast<T*>(p);}
};
*/

//-----------------------------------------------------------------------
// OBJECT
//-----------------------------------------------------------------------
class FB2TOEPUB_DECL Object
{
public:
    Object()                                    : cnt_(0) {}
    Object(const Object&)                       : cnt_(0) {}
    Object& operator=(const Object&)            {return *this;}
    virtual ~Object()                           {}

    void Lock() const       {++cnt_;}
    void Unlock() const     {if (!--cnt_) const_cast<Object*>(this)->DeleteUnreferenced();}

protected:
    virtual void DeleteUnreferenced()           {delete this;}

private:
    mutable long cnt_;
};


//-----------------------------------------------------------------------
// POINTER TO OBJECT
//-----------------------------------------------------------------------
template<typename T> class Ptr
{
    T *p_;
public:
    Ptr(T *p = 0)                                   : p_(p) {if (p_) p_->Lock();}
    Ptr(const Ptr<T> &that)                         : p_(that.p_) {if (p_) p_->Lock();}
    ~Ptr()                                          {if (p_) p_->Unlock();}
    const Ptr<T>& operator=(const Ptr<T> &that)     {return operator=(that.p_);}
    const Ptr<T>& operator=(T *p)
    {
        if (p_ != p)
        {
            if (p) p->Lock();
            if (p_) p_->Unlock();
            p_ = p;
        }
        return *this;
    }
    T* ptr() const              {return p_;}
    operator T*() const         {return p_;}
    T* operator->() const       {return p_;}
    T& operator*() const        {return *p_;}
    typedef T Type;
};


//-----------------------------------------------------------------------
// NONCOPYABLE
//-----------------------------------------------------------------------
class Noncopyable
{
    Noncopyable(const Noncopyable &x);
    Noncopyable& operator=(const Noncopyable &x);
public:
    Noncopyable() {}
};


//-----------------------------------------------------------------------
inline String Concat(const String &s1, const String &divider, const String &s2)
{
    return  s2.empty() ? s1 :
            s1.empty() ? s2 :
            String(s1 + divider + s2);
}


//-----------------------------------------------------------------------
typedef std::vector<String> strvector;

//-----------------------------------------------------------------------
// test mode stuff (to make output file predictable for automatic testing)
const unsigned int TEST_MODE_ON = 1;
void FB2TOEPUB_DECL SetTestMode(unsigned int flags);
unsigned int FB2TOEPUB_DECL IsTestMode();

//-----------------------------------------------------------------------

#if 0
//-----------------------------------------------------------------------
// Location - pair of (path itself, path type).
//-----------------------------------------------------------------------
struct PathLoc
{
    String      path_;
    bool        isFile_;
    PathLoc() {}
    PathLoc(const String &path, bool isFile) : path_(path), isFile_(isFile) {}
};
typedef std::vector<PathLoc> PathLocVector;
#endif


};  //namespace Fb2ToEpub

#endif
