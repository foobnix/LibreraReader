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


#ifndef FB2TOEPUB__CONFIG_H
#define FB2TOEPUB__CONFIG_H

#define FB2TOEPUB_DECL

//-----------------------------------------------------------------------
// MAX TEXT FILE SIZE (APPROXIMATE, DOESN'T TAKE XML STUFF INTO ACCOUNT)
// DEFAULT: 0x30000 (192K)
//-----------------------------------------------------------------------
//#define FB2TOEPUB_MAX_TEXT_FILE_SIZE 0x30000


//-----------------------------------------------------------------------
// DO NOT OVERWRITE OUTPUT FILE UNLESS --overwrite IS SET
// DEFAULT: OFF
//-----------------------------------------------------------------------
//#define FB2TOEPUB_DONT_OVERWRITE 0


//-----------------------------------------------------------------------
// SUPPRESS EMPTY TITLES IN TABLE OF CONTENTS
// If the value is nonzero, fb2 section without title don't appear in TOC,
// and their nested section are moved one level up.
// Otherwise, every section without title is assigned a title "- - - - -"
// DEFAULT: ON
//-----------------------------------------------------------------------
//#define FB2TOEPUB_SUPPRESS_EMPTY_TITLES 1


//-----------------------------------------------------------------------
// ENABLE IDS IN TABLE OF CONTENTS
// If the value is nonzero, TOC contains references to file only
// Otherwise, TOC contains "file#id" references.
// DEFAULT: ON
//-----------------------------------------------------------------------
//#define FB2TOEPUB_TOC_REFERS_FILES_ONLY 1


//-----------------------------------------------------------------------
// REMOVE REFERENCES TO std::string::compare
// (Custom option for ARM Linux)
// DEFAULT: OFF
//-----------------------------------------------------------------------
//#define FB2TOEPUB_NO_STD_STRING_COMPARE 0




//-----------------------------------------------------------------------
// DEFAULTS - DON'T EDIT
//-----------------------------------------------------------------------
#ifndef FB2TOEPUB_MAX_TEXT_FILE_SIZE
#define FB2TOEPUB_MAX_TEXT_FILE_SIZE 0x30000
#endif
#ifndef FB2TOEPUB_DONT_OVERWRITE
#define FB2TOEPUB_DONT_OVERWRITE 0
#endif
#ifndef FB2TOEPUB_SUPPRESS_EMPTY_TITLES
#define FB2TOEPUB_SUPPRESS_EMPTY_TITLES 1
#endif
#ifndef FB2TOEPUB_TOC_REFERS_FILES_ONLY
#define FB2TOEPUB_TOC_REFERS_FILES_ONLY 1
#endif
#ifndef FB2TOEPUB_NO_STD_STRING_COMPARE
#define FB2TOEPUB_NO_STD_STRING_COMPARE 0
#endif
#ifndef FB2TOEPUB_VERSION
#define FB2TOEPUB_VERSION Test Build
#endif
#define FB2TOEPUB_MAKESTR2__(a) #a
#define FB2TOEPUB_MAKESTR(a) FB2TOEPUB_MAKESTR2__(a)
#define FB2TOEPUB_VERSION_STRING FB2TOEPUB_MAKESTR(FB2TOEPUB_VERSION)


#endif
