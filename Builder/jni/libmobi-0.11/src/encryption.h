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

/**
 @brief Drm cookie data
 */
typedef struct {
    unsigned char *pid; /**< PIDs for decryption, NULL if not set */
    uint32_t valid_from; /**< validity period start time, unix time in minutes, 0 if not set */
    uint32_t valid_to; /**< validity period end time, unix time in minutes, MOBI_NOTSET if not set */
} MOBICookie;

/**
 @brief Drm data
 */

typedef struct {
    unsigned char *key; /**< key for decryption, NULL if not set */
    uint32_t cookies_count; /**< Cookies count */
    MOBICookie **cookies; /**< DRM cookie */
} MOBIDrm;

void mobi_free_drm(MOBIData *m);
MOBI_RET mobi_buffer_decrypt(unsigned char *out, const unsigned char *in, const size_t length, const MOBIData *m);
MOBI_RET mobi_drmkey_set(MOBIData *m, const char *pid);
MOBI_RET mobi_drmkey_set_serial(MOBIData *m, const char *serial);
MOBI_RET mobi_drmkey_delete(MOBIData *m);
MOBI_RET mobi_voucher_add(MOBIData *m, const char *serial, const time_t valid_from, const time_t valid_to,
                          const MOBIExthTag *tamperkeys, const size_t tamperkeys_count);
MOBI_RET mobi_drm_serialize_v1(MOBIBuffer *buf, const MOBIData *m);
MOBI_RET mobi_drm_serialize_v2(MOBIBuffer *buf, const MOBIData *m);

#endif /* defined(mobi_encryption_h) */
