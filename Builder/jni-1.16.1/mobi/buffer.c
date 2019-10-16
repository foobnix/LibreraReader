/** @file buffer.c
 *  @brief Functions to read/write raw big endian data
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <stdlib.h>
#include <string.h>
#include "buffer.h"
#include "debug.h"

/**
 @brief Initializer for MOBIBuffer structure
 
 It allocates memory for structure and for data.
 Memory should be freed with buffer_free().
 
 @param[in] len Size of data to be allocated for the buffer
 @return MOBIBuffer on success, NULL otherwise
 */
MOBIBuffer * buffer_init(const size_t len) {
    unsigned char *data = malloc(len);
    if (data == NULL) {
        debug_print("%s", "Buffer data allocation failed\n");
        return NULL;
    }
    MOBIBuffer *buf = buffer_init_null(data, len);
    if (buf == NULL) {
        free(data);
    }
    return buf;
}

/**
 @brief Initializer for MOBIBuffer structure
 
 It allocates memory for structure but, unlike buffer_init(), it does not allocate memory for data.
 Instead it works on external data.
 Memory should be freed with buffer_free_null() (buf->data will not be deallocated).
 
 @param[in,out] data Set data as buffer data
 @param[in] len Size of data held by the buffer
 @return MOBIBuffer on success, NULL otherwise
 */
MOBIBuffer * buffer_init_null(unsigned char *data, const size_t len) {
    MOBIBuffer *buf = malloc(sizeof(MOBIBuffer));
	if (buf == NULL) {
        debug_print("%s", "Buffer allocation failed\n");
        return NULL;
    }
    buf->data = data;
	buf->offset = 0;
	buf->maxlen = len;
    buf->error = MOBI_SUCCESS;
	return buf;
}

/**
 @brief Resize buffer
 
 Smaller size than offset will cause data truncation.
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] newlen New buffer size
 */
void buffer_resize(MOBIBuffer *buf, const size_t newlen) {
    unsigned char *tmp = realloc(buf->data, newlen);
    if (tmp == NULL) {
        debug_print("%s", "Buffer allocation failed\n");
        buf->error = MOBI_MALLOC_FAILED;
        return;
    }
    buf->data = tmp;
    buf->maxlen = newlen;
    if (buf->offset >= newlen) {
        buf->offset = newlen - 1;
    }
    debug_print("Buffer successfully resized to %zu\n", newlen);
    buf->error = MOBI_SUCCESS;
}

/**
 @brief Adds 8-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
void buffer_add8(MOBIBuffer *buf, const uint8_t data) {
    if (buf->offset + 1 > buf->maxlen) {
        debug_print("%s", "Buffer full\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    buf->data[buf->offset++] = data;
}

/**
 @brief Adds 16-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
void buffer_add16(MOBIBuffer *buf, const uint16_t data) {
    if (buf->offset + 2 > buf->maxlen) {
        debug_print("%s", "Buffer full\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    unsigned char *buftr = buf->data + buf->offset;
    *buftr++ = (uint8_t)((uint32_t)(data & 0xff00U) >> 8);
    *buftr = (uint8_t)((uint32_t)(data & 0xffU));
    buf->offset += 2;
}

/**
 @brief Adds 32-bit value to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Integer to be put into the buffer
 */
void buffer_add32(MOBIBuffer *buf, const uint32_t data) {
    if (buf->offset + 4 > buf->maxlen) {
        debug_print("%s", "Buffer full\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    unsigned char *buftr = buf->data + buf->offset;
    *buftr++ = (uint8_t)((uint32_t)(data & 0xff000000U) >> 24);
    *buftr++ = (uint8_t)((uint32_t)(data & 0xff0000U) >> 16);
    *buftr++ = (uint8_t)((uint32_t)(data & 0xff00U) >> 8);
    *buftr = (uint8_t)((uint32_t)(data & 0xffU));
    buf->offset += 4;
}

/**
 @brief Adds raw data to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] data Pointer to read data
 @param[in] len Size of the read data
 */
void buffer_addraw(MOBIBuffer *buf, const unsigned char* data, const size_t len) {
    if (buf->offset + len > buf->maxlen) {
        debug_print("%s", "Buffer full\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    memcpy(buf->data + buf->offset, data, len);
    buf->offset += len;
}

/**
 @brief Adds string to MOBIBuffer without null terminator
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] str Pointer to string
 */
void buffer_addstring(MOBIBuffer *buf, const char *str) {
    const size_t len = strlen(str);
    buffer_addraw(buf, (const unsigned char *) str, len);
}

/**
 @brief Adds count of zeroes to MOBIBuffer
 
 @param[in,out] buf MOBIBuffer structure to be filled with data
 @param[in] count Number of zeroes to be put into the buffer
 */
void buffer_addzeros(MOBIBuffer *buf, const size_t count) {
    if (buf->offset + count > buf->maxlen) {
        debug_print("%s", "Buffer full\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    memset(buf->data + buf->offset, 0, count);
    buf->offset += count;
}

/**
 @brief Reads 8-bit value from MOBIBuffer
 
 @param[in] buf MOBIBuffer structure containing data
 @return Read value, 0 if end of buffer is encountered
 */
uint8_t buffer_get8(MOBIBuffer *buf) {
    if (buf->offset + 1 > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return 0;
    }
    return buf->data[buf->offset++];
}

/**
 @brief Reads 16-bit value from MOBIBuffer
 
 @param[in] buf MOBIBuffer structure containing data
 @return Read value, 0 if end of buffer is encountered
 */
uint16_t buffer_get16(MOBIBuffer *buf) {
    if (buf->offset + 2 > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return 0;
    }
    uint16_t val;
    val = (uint16_t)((uint16_t) buf->data[buf->offset] << 8 | (uint16_t) buf->data[buf->offset + 1]);
    buf->offset += 2;
    return val;
}

/**
 @brief Reads 32-bit value from MOBIBuffer
 
 @param[in] buf MOBIBuffer structure containing data
 @return Read value, 0 if end of buffer is encountered
 */
uint32_t buffer_get32(MOBIBuffer *buf) {
    if (buf->offset + 4 > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return 0;
    }
    uint32_t val;
    val = (uint32_t) buf->data[buf->offset] << 24 | (uint32_t) buf->data[buf->offset + 1] << 16 | (uint32_t) buf->data[buf->offset + 2] << 8 | (uint32_t) buf->data[buf->offset + 3];
    buf->offset += 4;
    return val;
}

/**
 @brief Reads variable length value from MOBIBuffer
 
 Internal function for wrappers: 
 buffer_get_varlen();
 buffer_get_varlen_dec();
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @param[in] direction 1 - read buffer forward, -1 - read buffer backwards
 @return Read value, 0 if end of buffer is encountered
 */
static uint32_t _buffer_get_varlen(MOBIBuffer *buf, size_t *len, const int direction) {
    uint32_t val = 0;
    uint8_t byte_count = 0;
    uint8_t byte;
    const uint8_t stop_flag = 0x80;
    const uint8_t mask = 0x7f;
    uint32_t shift = 0;
    do {
        if (direction == 1) {
            if (buf->offset + 1 > buf->maxlen) {
                debug_print("%s", "End of buffer\n");
                buf->error = MOBI_BUFFER_END;
                return val;
            }
            byte = buf->data[buf->offset++];
            val <<= 7;
            val |= (byte & mask);
        } else {
            if (buf->offset < 1) {
                debug_print("%s", "End of buffer\n");
                buf->error = MOBI_BUFFER_END;
                return val;
            }
            byte = buf->data[buf->offset--];
            val = val | (uint32_t)(byte & mask) << shift;
            shift += 7;
        }        
        (*len)++;
        byte_count++;
    } while (!(byte & stop_flag) && (byte_count < 4));
    return val;
}

/**
 @brief Reads variable length value from MOBIBuffer
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @return Read value, 0 if end of buffer is encountered
 */
uint32_t buffer_get_varlen(MOBIBuffer *buf, size_t *len) {
    return _buffer_get_varlen(buf, len, 1);
}

/**
 @brief Reads variable length value from MOBIBuffer going backwards
 
 Reads maximum 4 bytes from the buffer. Stops when byte has bit 7 set.
 
 @param[in] buf MOBIBuffer structure containing data
 @param[out] len Value will be increased by number of bytes read
 @return Read value, 0 if end of buffer is encountered
 */
uint32_t buffer_get_varlen_dec(MOBIBuffer *buf, size_t *len) {
    return _buffer_get_varlen(buf, len, -1);
}

/**
 @brief Reads raw data from MOBIBuffer and pads it with zero character
 
 @param[out] str Destination for string read from buffer. Length must be (len + 1)
 @param[in] buf MOBIBuffer structure containing data
 @param[in] len Length of the data to be read from buffer
 */
void buffer_getstring(char *str, MOBIBuffer *buf, const size_t len) {
    if (!str) {
        buf->error = MOBI_PARAM_ERR;
        return;
    }
    if (buf->offset + len > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        str[0] = '\0';
        return;
    }
    memcpy(str, buf->data + buf->offset, len);
    str[len] = '\0';
    buf->offset += len;
}

/**
 @brief Reads raw data from MOBIBuffer, appends it to a string and pads it with zero character
 
 @param[in,out] str A string to which data will be appended
 @param[in] buf MOBIBuffer structure containing data
 @param[in] len Length of the data to be read from buffer
 */
void buffer_appendstring(char *str, MOBIBuffer *buf, const size_t len) {
    if (!str) {
        buf->error = MOBI_PARAM_ERR;
        return;
    }
    if (buf->offset + len > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    size_t str_len = strlen(str);
    memcpy(str + str_len, buf->data + buf->offset, len);
    str[str_len + len] = '\0';
    buf->offset += len;
}

/**
 @brief Reads raw data from MOBIBuffer
 
 @param[out] data Destination to which data will be appended
 @param[in] buf MOBIBuffer structure containing data
 @param[in] len Length of the data to be read from buffer
 */
void buffer_getraw(void *data, MOBIBuffer *buf, const size_t len) {
    if (!data) {
        buf->error = MOBI_PARAM_ERR;
        return;
    }
    if (buf->offset + len > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return;
    }
    memcpy(data, buf->data + buf->offset, len);
    buf->offset += len;
}

/**
 @brief Get pointer to MOBIBuffer data at offset
 
 @param[in] buf MOBIBuffer structure containing data
 @param[in] len Check if requested length is available in buffer
 @return Pointer to offset, or NULL on failure
 */
unsigned char * buffer_getpointer(MOBIBuffer *buf, const size_t len) {
    if (buf->offset + len > buf->maxlen) {
        debug_print("%s", "End of buffer\n");
        buf->error = MOBI_BUFFER_END;
        return NULL;
    }
    buf->offset += len;
    return buf->data + buf->offset - len;
}

/**
 @brief Read 8-bit value from MOBIBuffer into allocated memory
 
 Read 8-bit value from buffer into memory allocated by the function.
 Returns pointer to the value, which must be freed later.
 If the data is not accessible function will return null pointer.
 
 @param[out] val Pointer to value or null pointer on failure
 @param[in] buf MOBIBuffer structure containing data
 */
void buffer_dup8(uint8_t **val, MOBIBuffer *buf) {
    *val = NULL;
    if (buf->offset + 1 > buf->maxlen) {
        return;
    }
    *val = malloc(sizeof(uint8_t));
    if (*val == NULL) {
        return;
    }
    **val = buffer_get8(buf);
}

/**
 @brief Read 16-bit value from MOBIBuffer into allocated memory
 
 Read 16-bit value from buffer into allocated memory.
 Returns pointer to the value, which must be freed later.
 If the data is not accessible function will return null pointer.
 
 @param[out] val Pointer to value or null pointer on failure
 @param[in] buf MOBIBuffer structure containing data
 */
void buffer_dup16(uint16_t **val, MOBIBuffer *buf) {
    *val = NULL;
    if (buf->offset + 2 > buf->maxlen) {
        return;
    }
    *val = malloc(sizeof(uint16_t));
    if (*val == NULL) {
        return;
    }
    **val = buffer_get16(buf);
}

/**
 @brief Read 32-bit value from MOBIBuffer into allocated memory
 
 Read 32-bit value from buffer into allocated memory.
 Returns pointer to the value, which must be freed later.
 If the data is not accessible function will return null pointer.
 
 @param[out] val Pointer to value
 @param[in] buf MOBIBuffer structure containing data
 */
void buffer_dup32(uint32_t **val, MOBIBuffer *buf) {
    *val = NULL;
    if (buf->offset + 4 > buf->maxlen) {
        return;
    }
    *val = malloc(sizeof(uint32_t));
    if (*val == NULL) {
        return;
    }
    **val = buffer_get32(buf);
}

/**
 @brief Copy 8-bit value from one MOBIBuffer into another
 
 @param[out] dest Destination buffer
 @param[in] source Source buffer
 */
void buffer_copy8(MOBIBuffer *dest, MOBIBuffer *source) {
    buffer_add8(dest, buffer_get8(source));
}

/**
 @brief Copy raw value from one MOBIBuffer into another
 
 @param[out] dest Destination buffer
 @param[in] source Source buffer
 @param[in] len Number of bytes to copy
 */
void buffer_copy(MOBIBuffer *dest, MOBIBuffer *source, const size_t len) {
    if (source->offset + len > source->maxlen) {
        debug_print("%s", "End of buffer\n");
        source->error = MOBI_BUFFER_END;
        return;
    }
    if (dest->offset + len > dest->maxlen) {
        debug_print("%s", "End of buffer\n");
        dest->error = MOBI_BUFFER_END;
        return;
    }
    memcpy(dest->data + dest->offset, source->data + source->offset, len);
    dest->offset += len;
    source->offset += len;
}

/**
 @brief Copy raw value within one MOBIBuffer
 
 Memmove len bytes from offset (relative to current position)
 to current position in buffer and advance buffer position.
 Data may overlap.
 
 @param[out] buf Buffer
 @param[in] offset Offset to read from
 @param[in] len Number of bytes to copy
 */
void buffer_move(MOBIBuffer *buf, const int offset, const size_t len) {
    size_t aoffset = (size_t) abs(offset);
    unsigned char *source = buf->data + buf->offset;
    if (offset >= 0) {
        if (buf->offset + aoffset + len > buf->maxlen) {
            debug_print("%s", "End of buffer\n");
            buf->error = MOBI_BUFFER_END;
            return;
        }
        source += aoffset;
    } else {
        if (buf->offset < aoffset) {
            debug_print("%s", "End of buffer\n");
            buf->error = MOBI_BUFFER_END;
            return;
        }
        source -= aoffset;
    }
    memmove(buf->data + buf->offset, source, len);
    buf->offset += len;
}

/**
 @brief Check if buffer data header contains magic signature
 
 @param[in] buf MOBIBuffer buffer containing data
 @param[in] magic Magic signature
 @return boolean true on match, false otherwise
 */
bool buffer_match_magic(MOBIBuffer *buf, const char *magic) {
    const size_t magic_length = strlen(magic);
    if (buf->offset + magic_length > buf->maxlen) {
        return false;
    }
    if (memcmp(buf->data + buf->offset, magic, magic_length) == 0) {
        return true;
    }
    return false;
}

/**
 @brief Check if buffer contains magic signature at given offset
 
 @param[in] buf MOBIBuffer buffer containing data
 @param[in] magic Magic signature
 @param[in] offset Offset
 @return boolean true on match, false otherwise
 */
bool buffer_match_magic_offset(MOBIBuffer *buf, const char *magic, const size_t offset) {
    bool match = false;
    if (offset <= buf->maxlen) {
        const size_t save_offset = buf->offset;
        buf->offset = offset;
        match = buffer_match_magic(buf, magic);
        buf->offset = save_offset;
    }
    return match;
}

/**
 @brief Move current buffer offset by diff bytes
 
 @param[in,out] buf MOBIBuffer buffer containing data
 @param[in] diff Number of bytes by which the offset is adjusted
 */
void buffer_seek(MOBIBuffer *buf, const int diff) {
    size_t adiff = (size_t) abs(diff);
    if (diff >= 0) {
        if (buf->offset + adiff <= buf->maxlen) {
            buf->offset += adiff;
            return;
        }
    } else {
        if (buf->offset >= adiff) {
            buf->offset -= adiff;
            return;
        }
    }
    buf->error = MOBI_BUFFER_END;
    debug_print("%s", "End of buffer\n");
}

/**
 @brief Set buffer offset to pos position
 
 @param[in,out] buf MOBIBuffer buffer containing data
 @param[in] pos New position
 */
void buffer_setpos(MOBIBuffer *buf, const size_t pos) {
    if (pos <= buf->maxlen) {
        buf->offset = pos;
        return;
    }
    buf->error = MOBI_BUFFER_END;
    debug_print("%s", "End of buffer\n");
}

/**
 @brief Free pointer to MOBIBuffer structure and pointer to data
 
 Free data initialized with buffer_init();
 
 @param[in] buf MOBIBuffer structure
 */
void buffer_free(MOBIBuffer *buf) {
	if (buf == NULL) { return; }
	if (buf->data != NULL) {
		free(buf->data);
	}
	free(buf);
}

/**
 @brief Free pointer to MOBIBuffer structure
 
 Free data initialized with buffer_init_null();
 Unlike buffer_free() it will not free pointer to buf->data
 
 @param[in] buf MOBIBuffer structure
 */
void buffer_free_null(MOBIBuffer *buf) {
	if (buf == NULL) { return; }
	free(buf);
}
