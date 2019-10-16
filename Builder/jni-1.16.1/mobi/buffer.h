/** @file buffer.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_buffer_h
#define libmobi_buffer_h

#include "config.h"
#include "mobi.h"

/**
 @brief Buffer to read to/write from
 */
typedef struct {
    size_t offset; /**< Current offset in respect to buffer start */
    size_t maxlen; /**< Length of the buffer data */
    unsigned char *data; /**< Pointer to buffer data */
    MOBI_RET error; /**< MOBI_SUCCESS = 0 if operation on buffer is successful, non-zero value on failure */
} MOBIBuffer;

MOBIBuffer * buffer_init(const size_t len);
MOBIBuffer * buffer_init_null(unsigned char *data, const size_t len);
void buffer_resize(MOBIBuffer *buf, const size_t newlen);
void buffer_add8(MOBIBuffer *buf, const uint8_t data);
void buffer_add16(MOBIBuffer *buf, const uint16_t data);
void buffer_add32(MOBIBuffer *buf, const uint32_t data);
void buffer_addraw(MOBIBuffer *buf, const unsigned char* data, const size_t len);
void buffer_addstring(MOBIBuffer *buf, const char *str);
void buffer_addzeros(MOBIBuffer *buf, const size_t count);
uint8_t buffer_get8(MOBIBuffer *buf);
uint16_t buffer_get16(MOBIBuffer *buf);
uint32_t buffer_get32(MOBIBuffer *buf);
uint32_t buffer_get_varlen(MOBIBuffer *buf, size_t *len);
uint32_t buffer_get_varlen_dec(MOBIBuffer *buf, size_t *len);
void buffer_dup8(uint8_t **val, MOBIBuffer *buf);
void buffer_dup16(uint16_t **val, MOBIBuffer *buf);
void buffer_dup32(uint32_t **val, MOBIBuffer *buf);
void buffer_getstring(char *str, MOBIBuffer *buf, const size_t len);
void buffer_appendstring(char *str, MOBIBuffer *buf, const size_t len);
void buffer_getraw(void *data, MOBIBuffer *buf, const size_t len);
unsigned char * buffer_getpointer(MOBIBuffer *buf, const size_t len);
void buffer_copy8(MOBIBuffer *in, MOBIBuffer *source);
void buffer_move(MOBIBuffer *buf, const int offset, const size_t len);
void buffer_copy(MOBIBuffer *dest, MOBIBuffer *source, const size_t len);
bool buffer_match_magic(MOBIBuffer *buf, const char *magic);
bool buffer_match_magic_offset(MOBIBuffer *buf, const char *magic, const size_t offset);
void buffer_seek(MOBIBuffer *buf, const int diff);
void buffer_setpos(MOBIBuffer *buf, const size_t pos);
void buffer_free(MOBIBuffer *buf);
void buffer_free_null(MOBIBuffer *buf);

#endif
