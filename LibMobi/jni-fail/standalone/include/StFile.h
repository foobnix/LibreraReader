/*
 * Copyright (C) 2016 The Common CLI viewer interface Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __ST_FILE_H__
#define __ST_FILE_H__

#include <ext/stdio_filebuf.h>
#include <unistd.h>

#include <iostream>
#include <fstream>

#define nullfd (-1)

using namespace std;

class StFileDescriptor
{
private:
    int fd;

public:
    StFileDescriptor(const int fd)
        : fd(fd)
    {
    }
    StFileDescriptor(const char* filename, const char* openmode)
    {
        FILE* f = filename && filename[0] ? fopen(filename, openmode) : NULL;
        fd = f ? fileno(f) : -1;
    }
    StFileDescriptor(StFileDescriptor const& other)
    {
        fd = other.isValid() ? (::dup(other.fd)) : -1;
    }
    StFileDescriptor(StFileDescriptor&& other)
        : fd(other.fd)
    {
        other.cleanup();
    }

    StFileDescriptor& operator=(StFileDescriptor const&) = delete;

    ~StFileDescriptor()
    {
        close();
    }

public:
    bool isValid() const
    {
        return fd >= 0;
    }

    const int asInt() const
    {
        return fd;
    }

protected:
    void cleanup()
    {
        fd = nullfd;
    }
    void close()
    {
        ::close(fd);
    }
};

class StFile
{
private:
    StFileDescriptor fd;
    __gnu_cxx ::stdio_filebuf<char> filebuf;
    istream file;

public:
    StFile(const char* filename)
        : fd(filename, "r"), filebuf(fd.asInt(), std::ios::in), file(&filebuf)
    {
    }

    StFile(int fileDescriptor)
        : fd(fileDescriptor), filebuf(fd.asInt(), std::ios::in), file(&filebuf)
    {
    }

    StFile(StFile& file)
        : fd(file.fd), filebuf(fd.asInt(), std::ios::in), file(&filebuf) {}

    ~StFile() = default;

public:
    StFile& seekg(size_t __off, ios_base::seekdir __dir)
    {
        file.seekg(__off, __dir);
        return *this;
    }

    size_t tellg()
    {
        return file.tellg();
    }

    StFile& read(char* __s, size_t __n) {
        file.read(__s, __n);
        return *this;
    }
};

#endif
