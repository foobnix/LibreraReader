/** @file encryption.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_encryption_h
#define mobi_encryption_h

#include "config.h"
#include "mobi.h"

MOBI_RET mobi_drm_decrypt_buffer(unsigned char *out, const unsigned char *in, const size_t length, const MOBIData *m);
MOBI_RET mobi_drm_setkey_internal(MOBIData *m, const char *pid);
MOBI_RET mobi_drm_setkey_serial_internal(MOBIData *m, const char *pid);
MOBI_RET mobi_drm_delkey_internal(MOBIData *m);

#endif /* defined(mobi_encryption_h) */
