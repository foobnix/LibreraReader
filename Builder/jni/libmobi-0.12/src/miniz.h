/** @file miniz.h
 *  @brief header file for third party miniz.c, zlib replacement
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_miniz_h
#define libmobi_miniz_h

#define MINIZ_HEADER_FILE_ONLY
#define MINIZ_NO_STDIO
#define MINIZ_NO_ARCHIVE_APIS
#define MINIZ_NO_ZLIB_COMPATIBLE_NAMES
#define MINIZ_NO_TIME
#define MINIZ_NO_ARCHIVE_WRITING_APIS

#include "miniz.c"

#endif
