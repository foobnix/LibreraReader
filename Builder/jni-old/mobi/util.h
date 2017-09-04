/** @file util.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_util_h
#define libmobi_util_h

#include "config.h"
#include "mobi.h"
#include "memory.h"
#include "buffer.h"
#include "read.h"
#include "compression.h"

#ifndef HAVE_STRDUP
/** @brief strdup replacement */
#define strdup mobi_strdup
#endif

#ifdef USE_MINIZ
#include "miniz.h"
#define m_uncompress mz_uncompress
#define m_crc32 mz_crc32
#define M_OK MZ_OK
#else
#include <zlib.h>
#define m_uncompress uncompress
#define m_crc32 crc32
#define M_OK Z_OK
#endif

#define UNUSED(x) (void)(x)

/** @brief Magic numbers of records */
#define MOBI_MAGIC "MOBI"
#define EXTH_MAGIC "EXTH"
#define HUFF_MAGIC "HUFF"
#define CDIC_MAGIC "CDIC"
#define FDST_MAGIC "FDST"
#define IDXT_MAGIC "IDXT"
#define INDX_MAGIC "INDX"
#define LIGT_MAGIC "LIGT"
#define ORDT_MAGIC "ORDT"
#define TAGX_MAGIC "TAGX"
#define FONT_MAGIC "FONT"
#define AUDI_MAGIC "AUDI"
#define VIDE_MAGIC "VIDE"
#define SRCS_MAGIC "SRCS"
#define CMET_MAGIC "CMET"
#define BOUNDARY_MAGIC "BOUNDARY"
#define EOF_MAGIC "\xe9\x8e\r\n"
#define REPLICA_MAGIC "%MOP"

/** @brief Difference in seconds between epoch time and mac time */
#define EPOCH_MAC_DIFF 2082844800UL

/** 
 @defgroup mobi_pdb Params for pdb record header structure
 @{
 */
#define PALMDB_HEADER_LEN 78 /**< Length of header without record info headers */
#define PALMDB_NAME_SIZE_MAX 32 /**< Max length of db name stored at offset 0 */
#define PALMDB_RECORD_INFO_SIZE 8 /**< Record info header size of each pdb record */
/** @} */

/** 
 @defgroup mobi_pdb_defs Default values for pdb record header structure
 @{
 */
#define PALMDB_ATTRIBUTE_DEFAULT 0
#define PALMDB_VERSION_DEFAULT 0
#define PALMDB_MODNUM_DEFAULT 0
#define PALMDB_APPINFO_DEFAULT 0
#define PALMDB_SORTINFO_DEFAULT 0
#define PALMDB_TYPE_DEFAULT "BOOK"
#define PALMDB_CREATOR_DEFAULT "MOBI"
#define PALMDB_NEXTREC_DEFAULT 0
/** @} */

/** 
 @defgroup mobi_rec0 Params for record0 header structure
 @{ 
 */
#define RECORD0_HEADER_LEN 16 /**< Length of Record 0 header */
#define RECORD0_NO_COMPRESSION 1 /**< Text record compression type: none */
#define RECORD0_PALMDOC_COMPRESSION 2 /**< Text record compression type: palmdoc */
#define RECORD0_HUFF_COMPRESSION 17480 /**< Text record compression type: huff/cdic */
#define RECORD0_TEXT_SIZE_MAX 4096 /**< Max size of uncompressed text record */
#define RECORD0_FULLNAME_SIZE_MAX 1024 /**< Max size to full name string */
#define RECORD0_NO_ENCRYPTION 0 /**< Text record encryption type: none */
#define RECORD0_OLD_ENCRYPTION 1 /**< Text record encryption type: old mobipocket */
#define RECORD0_MOBI_ENCRYPTION 2 /**< Text record encryption type: mobipocket */
/** @} */

/** 
 @defgroup mobi_len Header length / size of records 
 @{ 
 */
#define CDIC_HEADER_LEN 16
#define CDIC_RECORD_MAXCNT 1024
#define HUFF_CODELEN_MAX 16
#define HUFF_HEADER_LEN 24
#define HUFF_RECORD_MAXCNT 1024
#define HUFF_RECORD_MINSIZE 2584
#define FONT_HEADER_LEN 24
#define MEDIA_HEADER_LEN 12
#define FONT_SIZEMAX (50 * 1024 * 1024)
#define RAWTEXT_SIZEMAX 0xfffffff
#define MOBI_HEADER_V2_SIZE 0x18
#define MOBI_HEADER_V3_SIZE 0x74
#define MOBI_HEADER_V4_SIZE 0xd0
#define MOBI_HEADER_V5_SIZE 0xe4
#define MOBI_HEADER_V6_SIZE 0xe4
#define MOBI_HEADER_V6_EXT_SIZE 0xe8
#define MOBI_HEADER_V7_SIZE 0xe4
/** @} */

/** @} */

#ifndef max
#define max(a, b) ((a) > (b) ? (a) : (b))
#endif
#ifndef min
#define min(a, b) ((a) < (b) ? (a) : (b))
#endif

#define ARRAYSIZE(arr) (sizeof(arr) / sizeof(arr[0]))

#define MOBI_TITLE_SIZEMAX 1024

int mobi_bitcount(const uint8_t byte);
MOBI_RET mobi_delete_record_by_seqnumber(MOBIData *m, const size_t num);
MOBI_RET mobi_swap_mobidata(MOBIData *m);
char * mobi_strdup(const char *s);
bool mobi_is_cp1252(const MOBIData *m);
MOBI_RET mobi_cp1252_to_utf8(char *output, const char *input, size_t *outsize, const size_t insize);
MOBI_RET mobi_utf8_to_cp1252(char *output, const char *input, size_t *outsize, const size_t insize);
uint8_t mobi_ligature_to_cp1252(const uint8_t c1, const uint8_t c2);
uint16_t mobi_ligature_to_utf16(const uint32_t control, const uint32_t c);
MOBIFiletype mobi_determine_resource_type(const MOBIPdbRecord *record);
MOBIFiletype mobi_determine_flowpart_type(const MOBIRawml *rawml, const size_t part_number);
MOBI_RET mobi_base32_decode(uint32_t *decoded, const char *encoded);
MOBIFiletype mobi_get_resourcetype_by_uid(const MOBIRawml *rawml, const size_t uid);
uint32_t mobi_get_exthsize(const MOBIData *m);
uint16_t mobi_get_records_count(const MOBIData *m);
void mobi_remove_zeros(unsigned char *buffer, size_t *len);
MOBI_RET mobi_add_audio_resource(MOBIPart *part);
MOBI_RET mobi_add_video_resource(MOBIPart *part);
MOBI_RET mobi_add_font_resource(MOBIPart *part);
MOBI_RET mobi_set_fullname(MOBIData *m, const char *fullname);
MOBI_RET mobi_set_pdbname(MOBIData *m, const char *name);
uint32_t mobi_get32be(const unsigned char buf[4]);
uint32_t mobi_get32le(const unsigned char buf[4]);
#endif
