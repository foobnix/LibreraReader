/** @file randombytes.h
 *
 * Copyright (c) 2021 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_randombytes_h
#define libmobi_randombytes_h

#include "mobi.h"

/**
 @brief Write n random bytes of high quality to buf
 
 @param[in,out] buf Buffer to be filled with random bytes
 @param[in] len Buffer length
 @return On success returns MOBI_SUCCESS
 */
MOBI_RET mobi_randombytes(void *buf, const size_t len);

#endif
