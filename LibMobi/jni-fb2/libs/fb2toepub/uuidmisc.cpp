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

#include "uuidmisc.h"
#include "error.h"
#include <ctype.h>
#include <vector>
#include <time.h>

namespace Fb2ToEpub
{

//-----------------------------------------------------------------------
// uuid variant
inline bool isxdigit_variant(char c)
{
    return c == '8' || c == '9' || c == 'a' || c == 'b' || c == 'A' || c == 'B';
}

//-----------------------------------------------------------------------
bool IsValidUUID(const String &id)
{
    return (id.length() == 36 &&
            isxdigit(id[0]) && isxdigit(id[1]) && isxdigit(id[2]) && isxdigit(id[3]) &&
            isxdigit(id[4]) && isxdigit(id[5]) && isxdigit(id[6]) && isxdigit(id[7]) &&
            id[8] == '-' &&
            isxdigit(id[9]) && isxdigit(id[10]) && isxdigit(id[11]) && isxdigit(id[12]) &&
            id[13] == '-' &&
            isxdigit(id[14]) && isxdigit(id[15]) && isxdigit(id[16]) && isxdigit(id[17]) &&
            id[18] == '-' &&
            isxdigit_variant(id[19]) &&
            isxdigit(id[20]) && isxdigit(id[21]) && isxdigit(id[22]) &&
            id[23] == '-' &&
            isxdigit(id[24]) && isxdigit(id[25]) && isxdigit(id[26]) && isxdigit(id[27]) &&
            isxdigit(id[28]) && isxdigit(id[29]) && isxdigit(id[30]) && isxdigit(id[31]) &&
            isxdigit(id[32]) && isxdigit(id[33]) && isxdigit(id[34]) && isxdigit(id[35]));
}

//-----------------------------------------------------------------------
inline void AddRandomHex(std::vector<char> *buf, int cnt)
{
    static const char tbl[16] = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    while(--cnt >= 0)
        buf->push_back(tbl[rand()%16]);
}

//-----------------------------------------------------------------------
String GenerateUUID()
{
    if(IsTestMode())
        srand(0);   // make predictable
    else
        srand(static_cast<unsigned int>(time(NULL)^2718281828UL));
    std::vector<char> buf;

    AddRandomHex(&buf, 8);
    buf.push_back('-');
    AddRandomHex(&buf, 4);
    buf.push_back('-');
    AddRandomHex(&buf, 4);
    buf.push_back('-');

    {
        static const char tbl[4] = {'8','9','A','B'};
        buf.push_back(tbl[rand()%4]);
    }

    AddRandomHex(&buf, 3);
    buf.push_back('-');
    AddRandomHex(&buf, 12);
    buf.push_back('\0');

    return &buf[0];

    //return "49fdf150-b8dd-11de-92bf-00a0d1e7a3b4";
    //return "00000000-0000-0000-0000-000000000000";
}
    
//-----------------------------------------------------------------------
void MakeAdobeKey(const String &uuid, unsigned char *adobeKey)
{
#if defined(_DEBUG)
    if(!IsValidUUID(uuid))
        InternalError(__FILE__, __LINE__, "uuid error");
#endif

    const char *p = uuid.c_str();
    bool high = true;
    for(;;)
    {
        unsigned char nibble;
        char c = *p++;
        switch(c)
        {
        case '\0':  return;
        case '-':   continue;

        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
            nibble = (c - '0');
            break;

        case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
            nibble = (10 + (c - 'a'));
            break;

        case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
            nibble = (10 + (c - 'A'));
            break;
        }

        if(high)
        {
            high = false;
            *adobeKey = (nibble << 4);
        }
        else
        {
            high = true;
            *adobeKey++ |= nibble;
        }
    }
}


};  //namespace Fb2ToEpub
