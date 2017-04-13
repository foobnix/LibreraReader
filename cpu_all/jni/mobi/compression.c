/** @file compression.c
 *  @brief Functions handling compression
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <string.h>
#include "compression.h"
#include "buffer.h"
#include "mobi.h"
#include "debug.h"


/** 
 @brief Decompressor fo PalmDOC version of LZ77 compression

 Decompressor based on this algorithm:
 http://en.wikibooks.org/wiki/Data_Compression/Dictionary_compression#PalmDoc

 @param[out] out Decompressed destination data
 @param[in] in Compressed source data
 @param[in,out] len_out Size of the memory reserved for decompressed data.
 On return it is set to actual size of decompressed data
 @param[in] len_in Size of compressed data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_decompress_lz77(unsigned char *out, const unsigned char *in, size_t *len_out, const size_t len_in) {
    MOBI_RET ret = MOBI_SUCCESS;
    MOBIBuffer *buf_in = buffer_init_null((unsigned char *) in, len_in);
    if (buf_in == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    MOBIBuffer *buf_out = buffer_init_null(out, *len_out);
    if (buf_out == NULL) {
        buffer_free_null(buf_in);
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    while (ret == MOBI_SUCCESS && buf_in->offset < buf_in->maxlen) {
        uint8_t byte = buffer_get8(buf_in);
        /* byte pair: space + char */
        if (byte >= 0xc0) {
            buffer_add8(buf_out, ' ');
            buffer_add8(buf_out, byte ^ 0x80);
        }
        /* length, distance pair */
        /* 0x8000 + (distance << 3) + ((length-3) & 0x07) */
        else if (byte >= 0x80) {
            uint8_t next = buffer_get8(buf_in);
            uint16_t distance = ((((byte << 8) | ((uint8_t)next)) >> 3) & 0x7ff);
            uint8_t length = (next & 0x7) + 3;
            while (length--) {
                buffer_move(buf_out, -distance, 1);
            }
        }
        /* single char, not modified */
        else if (byte >= 0x09) {
            buffer_add8(buf_out, byte);
        }
        /* val chars not modified */
        else if (byte >= 0x01) {
            buffer_copy(buf_out, buf_in, byte);
        }
        /* char '\0', not modified */
        else {
            buffer_add8(buf_out, byte);
        }
        if (buf_in->error || buf_out->error) {
            ret = MOBI_BUFFER_END;
        }
    }
    *len_out = buf_out->offset;
    buffer_free_null(buf_out);
    buffer_free_null(buf_in);
    return ret;
}

/**
 @brief Read at most 8 bytes from buffer, big-endian
 
 If buffer data is shorter returned value is padded with zeroes
 
 @param[in] buf MOBIBuffer structure to read from
 @return 64-bit value
 */
static MOBI_INLINE uint64_t buffer_fill64(MOBIBuffer *buf) {
    uint64_t val = 0;
    uint8_t i = 8;
    size_t bytesleft = buf->maxlen - buf->offset;
    unsigned char *ptr = buf->data + buf->offset;
    while (i-- && bytesleft--) {
        val |= (uint64_t) *ptr++ << (i * 8);
    }
    /* increase counter by 4 bytes only, 4 bytes overlap on each call */
    buf->offset += 4;
    return val;
}

/**
 @brief Internal function for huff/cdic decompression
 
 Decompressor and HUFF/CDIC records parsing based on:
 perl EBook::Tools::Mobipocket
 python mobiunpack.py, calibre
 
 @param[out] buf_out MOBIBuffer structure with decompressed data
 @param[in] buf_in MOBIBuffer structure with compressed data
 @param[in] huffcdic MOBIHuffCdic structure with parsed data from huff/cdic records
 @param[in] depth Depth of current recursion level
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_decompress_huffman_internal(MOBIBuffer *buf_out, MOBIBuffer *buf_in, const MOBIHuffCdic *huffcdic, size_t depth) {
    if (depth > MOBI_HUFFMAN_MAXDEPTH) {
        debug_print("Too many levels of recursion: %zu\n", depth);
        return MOBI_DATA_CORRUPT;
    }
    MOBI_RET ret = MOBI_SUCCESS;
    int8_t bitcount = 32;
    /* this cast should be safe: max record size is 4096 */
    int bitsleft = (int) (buf_in->maxlen * 8);
    uint8_t code_length = 0;
    uint64_t buffer = buffer_fill64(buf_in);
    while (ret == MOBI_SUCCESS) {
        if (bitcount <= 0) {
            bitcount += 32;
            buffer = buffer_fill64(buf_in);
        }
        uint32_t code = (buffer >> bitcount) & 0xffffffffU;
        /* lookup code in table1 */
        uint32_t t1 = huffcdic->table1[code >> 24];
        /* get maxcode and codelen from t1 */
        code_length = t1 & 0x1f;
        uint32_t maxcode = (((t1 >> 8) + 1) << (32 - code_length)) - 1;
        /* check termination bit */
        if (!(t1 & 0x80)) {
            /* get offset from mincode, maxcode tables */
            while (code < huffcdic->mincode_table[code_length]) {
                code_length++;
            }
            maxcode = huffcdic->maxcode_table[code_length];
        }
        bitcount -= code_length;
        bitsleft -= code_length;
        if (bitsleft < 0) {
            break;
        }
        /* get index for symbol offset */
        uint32_t index = (uint32_t) (maxcode - code) >> (32 - code_length);
        /* check which part of cdic to use */
        uint16_t cdic_index = (uint16_t) ((uint32_t)index >> huffcdic->code_length);
        if (index >= huffcdic->index_count) {
            debug_print("Wrong symbol offsets index: %u\n", index);
            return MOBI_DATA_CORRUPT;
        }
        /* get offset */
        uint32_t offset = huffcdic->symbol_offsets[index];
        uint32_t symbol_length = (uint32_t) huffcdic->symbols[cdic_index][offset] << 8 | (uint32_t) huffcdic->symbols[cdic_index][offset + 1];
        /* 1st bit is is_decompressed flag */
        int is_decompressed = symbol_length >> 15;
        /* get rid of flag */
        symbol_length &= 0x7fff;
        if (is_decompressed) {
            /* symbol is at (offset + 2), 2 bytes used earlier for symbol length */
            buffer_addraw(buf_out, (huffcdic->symbols[cdic_index] + offset + 2), symbol_length);
            ret = buf_out->error;
        } else {
            /* symbol is compressed */
            /* TODO cache uncompressed symbols? */
            MOBIBuffer buf_sym;
            buf_sym.data = huffcdic->symbols[cdic_index] + offset + 2;
            buf_sym.offset = 0;
            buf_sym.maxlen = symbol_length;
            buf_sym.error = MOBI_SUCCESS;
            ret = mobi_decompress_huffman_internal(buf_out, &buf_sym, huffcdic, depth + 1);
        }
    }
    return ret;
}

/**
 @brief Decompressor for huff/cdic compressed text records
 
 Decompressor and HUFF/CDIC records parsing based on:
 perl EBook::Tools::Mobipocket
 python mobiunpack.py, calibre
 
 @param[out] out Decompressed destination data
 @param[in] in Compressed source data
 @param[in,out] len_out Size of the memory reserved for decompressed data.
 On return it is set to actual size of decompressed data
 @param[in] len_in Size of compressed data
 @param[in] huffcdic MOBIHuffCdic structure with parsed data from huff/cdic records
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_decompress_huffman(unsigned char *out, const unsigned char *in, size_t *len_out, size_t len_in, const MOBIHuffCdic *huffcdic) {
    MOBIBuffer *buf_in = buffer_init_null((unsigned char *) in, len_in);
    if (buf_in == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    MOBIBuffer *buf_out = buffer_init_null(out, *len_out);
    if (buf_out == NULL) {
        buffer_free_null(buf_in);
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    MOBI_RET ret = mobi_decompress_huffman_internal(buf_out, buf_in, huffcdic, 0);
    *len_out = buf_out->offset;
    buffer_free_null(buf_out);
    buffer_free_null(buf_in);
    return ret;
}
