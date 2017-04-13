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


#ifndef FB2TOEPUB__STREAMZIP_H
#define FB2TOEPUB__STREAMZIP_H

#include "stream.h"

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// INPUT STREAM UNPACKING FILE IF NECESSARY
// Supported formats:
//  1) No packing
//  2) Zip (only first file is unpacked)
//-----------------------------------------------------------------------
Ptr<InStm> FB2TOEPUB_DECL   CreateUnpackStm(const char *name);

//-----------------------------------------------------------------------
// OUTPUT ZIP STREAM
//-----------------------------------------------------------------------
class FB2TOEPUB_DECL OutPackStm : public OutStm
{
public:
    virtual void BeginFile(const char *name, bool compress) = 0;

    // helper
    void AddFile(InStm *pin, const char *name, bool compress);
};

//-----------------------------------------------------------------------
// CREATE ZIP STREAM
//-----------------------------------------------------------------------
Ptr<OutPackStm> FB2TOEPUB_DECL CreatePackStm(const char *name);

};  //namespace Fb2ToEpub

#endif
