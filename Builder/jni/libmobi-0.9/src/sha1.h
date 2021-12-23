/** @file sha1.h
 *  @brief Header for sha1.c
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_sha1_h
#define mobi_sha1_h

typedef struct {
    uint32_t state[5];
    uint32_t count[2];
    uint8_t buffer[64];
} SHA1_CTX;

#define SHA1_DIGEST_SIZE 20

void SHA1_Init(SHA1_CTX *context);
void SHA1_Update(SHA1_CTX *context, const uint8_t *data, const size_t len);
void SHA1_Final(SHA1_CTX *context, uint8_t digest[SHA1_DIGEST_SIZE]);

#endif /* sha1_h */
