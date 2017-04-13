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

#include "scandir.h"

#if (defined WIN32)

#include <io.h>
namespace Fb2ToEpub
{
    /*
    //-----------------------------------------------------------------------
    // OS-dependent GetFileFromPath implementation
    //-----------------------------------------------------------------------
    String GetFileFromPath(const char *path)
    {
        char fname[_MAX_PATH], ext[_MAX_PATH], filename[_MAX_PATH];
        _splitpath(path, NULL, NULL, fname, ext);
        _makepath(filename, NULL, NULL, fname, ext);
        return filename;
    }
    */

    //-----------------------------------------------------------------------
    // OS-dependent ScanDir implementation
    //-----------------------------------------------------------------------
    class WinScanDir : public ScanDir, Noncopyable
    {
        intptr_t    h_;
        String      dir_, spec_;
    public:
        WinScanDir(const char *dir, const char *ext) : h_(-1L), dir_(dir)
        {
            int len = dir_.length();
            if(!len || (dir_[len-1] != '/' && dir_[len-1] != '\\'))
                dir_ += "/";
            spec_ = dir_ + "*." + ext;
        }
        ~WinScanDir()
        {
            if(h_ != -1L)
                _findclose(h_);
        }

        //virtual
        String GetNextFile(String *fname)
        {
            if(fname)
                *fname = "";

            _finddata_t data;
            if(h_ == -1)
            {
                if((h_ = _findfirst(spec_.c_str(), &data)) == -1L)
                    return "";
            }
            else if(_findnext(h_, &data))
                return "";

            if(fname)
                *fname = data.name;
            return dir_ + data.name;
        }
    };

    Ptr<ScanDir> CreateScanDir(const char *dir, const char *ext)
    {
        return new WinScanDir(dir, ext);
    }
};

#elif (defined unix)

#include <dirent.h>
#include <string.h>
namespace Fb2ToEpub
{
    //-----------------------------------------------------------------------
    // OS-dependent ScanDir implementation
    //-----------------------------------------------------------------------
    class UnixScanDir : public ScanDir, Noncopyable
    {
        DIR         *d_;
        String      dir_, ext_;
    public:
        UnixScanDir(const char *dir, const char *ext) : d_(opendir(dir)), dir_(dir), ext_(ext)
        {
            int len = dir_.length();
            if(!len || dir_[len-1] != '/')
                dir_ += "/";
        }
        ~UnixScanDir()
        {
            if(d_ != NULL)
                closedir(d_);
        }

        //virtual
        String GetNextFile(String *fname)
        {
            if(fname)
                *fname = "";

            for(;;)
            {
                if(d_ == NULL)
                    return "";
                struct dirent *entry = readdir(d_);
                if(!entry)
                    return "";

                const char *p = strrchr(entry->d_name, '.');
                if(!p || strcmp(p+1, ext_.c_str()))
                    continue;

                if(fname)
                    *fname = entry->d_name;
                return dir_ + entry->d_name;
            }
        }
    };

    Ptr<ScanDir> CreateScanDir(const char *dir, const char *ext)
    {
        return new UnixScanDir(dir, ext);
    }
};

#else

#error Implement ScanDir for your OS!!!

#endif
