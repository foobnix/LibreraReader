/** @file compression.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_compression_h
#define libmobi_compression_h

#include "config.h"
#include "mobi.h"

#ifndef MOBI_INLINE
#define MOBI_INLINE /**< Syntax for compiler inline keyword from config.h */
#endif

/* FIXME: what is the reasonable value? */
#define MOBI_HUFFMAN_MAXDEPTH 20 /**< Maximal recursion level for huffman decompression routine */


/**
 @brief Parsed data from HUFF and CDIC records needed to unpack huffman compressed text
 */
typedef struct {
    size_t index_count; /**< Total number of indices in all CDIC records, stored in each CDIC record header */
    size_t index_read; /**< Number of indices parsed, used by parser */
    size_t code_length; /**< Code length value stored in CDIC record header */
    uint32_t table1[256]; /**< Table of big-endian indices from HUFF record data1 */
    uint32_t mincode_table[33]; /**< Table of big-endian mincodes from HUFF record data2 */
    uint32_t maxcode_table[33]; /**< Table of big-endian maxcodes from HUFF record data2 */
    uint16_t *symbol_offsets; /**< Index of symbol offsets parsed from CDIC records (index_count entries) */
    unsigned char **symbols; /**< Array of pointers to start of symbols data in each CDIC record (index = number of CDIC record) */
} MOBIHuffCdic;

MOBI_RET mobi_decompress_lz77(unsigned char *out, const unsigned char *in, size_t *len_out, const size_t len_in);
MOBI_RET mobi_decompress_huffman(unsigned char *out, const unsigned char *in, size_t *len_out, size_t len_in, const MOBIHuffCdic *huffcdic);

#endif
