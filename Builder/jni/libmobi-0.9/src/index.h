/** @file index.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_index_h
#define mobi_index_h

#include "config.h"
#include "structure.h"
#include "mobi.h"

/**
 @defgroup index_tag Predefined tag arrays: {tagid, tagindex} for mobi_get_indxentry_tagvalue()
 @{
 */
#define INDX_TAG_GUIDE_TITLE_CNCX (unsigned[]) {1, 0} /**< Guide title CNCX offset */

#define INDX_TAG_NCX_FILEPOS (unsigned[]) {1, 0} /**< NCX filepos offset */
#define INDX_TAG_NCX_TEXT_CNCX (unsigned[]) {3, 0} /**< NCX text CNCX offset */
#define INDX_TAG_NCX_LEVEL (unsigned[]) {4, 0} /**< NCX level */
#define INDX_TAG_NCX_KIND_CNCX (unsigned[]) {5, 0} /**< NCX kind CNCX offset */
#define INDX_TAG_NCX_POSFID (unsigned[]) {6, 0} /**< NCX pos:fid */
#define INDX_TAG_NCX_POSOFF (unsigned[]) {6, 1} /**< NCX pos:off */
#define INDX_TAG_NCX_PARENT (unsigned[]) {21, 0} /**< NCX parent */
#define INDX_TAG_NCX_CHILD_START (unsigned[]) {22, 0} /**< NCX start child */
#define INDX_TAG_NCX_CHILD_END (unsigned[]) {23, 0} /**< NCX last child */

#define INDX_TAG_SKEL_COUNT (unsigned[]) {1, 0} /**< Skel fragments count */
#define INDX_TAG_SKEL_POSITION (unsigned[]) {6, 0} /**< Skel position */
#define INDX_TAG_SKEL_LENGTH (unsigned[]) {6, 1} /**< Skel length */

#define INDX_TAG_FRAG_AID_CNCX (unsigned[]) {2, 0} /**< Frag aid CNCX offset */
#define INDX_TAG_FRAG_FILE_NR (unsigned[]) {3, 0} /**< Frag file number */
#define INDX_TAG_FRAG_SEQUENCE_NR (unsigned[]) {4, 0} /**< Frag sequence number */
#define INDX_TAG_FRAG_POSITION (unsigned[]) {6, 0} /**< Frag position */
#define INDX_TAG_FRAG_LENGTH (unsigned[]) {6, 1} /**< Frag length */

#define INDX_TAG_ORTH_STARTPOS (unsigned[]) {1, 0} /**< Orth entry start position */
#define INDX_TAG_ORTH_ENDPOS (unsigned[]) {2, 0} /**< Orth entry end position */

#define INDX_TAGARR_ORTH_INFL 42 /**< Inflection groups for orth entry */
#define INDX_TAGARR_INFL_GROUPS 5 /**< Inflection groups in infl index */
#define INDX_TAGARR_INFL_PARTS_V2 26 /**< Inflection particles in infl index */

#define INDX_TAGARR_INFL_PARTS_V1 7 /**< Inflection particles in old type infl index */
/** @} */

#define INDX_LABEL_SIZEMAX 1000 /**< Max size of index label */
#define INDX_INFLTAG_SIZEMAX 25000 /**< Max size of inflections tags per entry */
#define INDX_INFLBUF_SIZEMAX 500 /**< Max size of index label */
#define INDX_INFLSTRINGS_MAX 500 /**< Max number of inflected strings */
#define ORDT_RECORD_MAXCNT 256 /* max entries count in old ordt */
#define CNCX_RECORD_MAXCNT 0xf /* max entries count */
#define INDX_RECORD_MAXCNT 6000 /* max index entries per record */
#define INDX_TOTAL_MAXCNT ((size_t) INDX_RECORD_MAXCNT * 0xffff) /* max total index entries */
#define INDX_NAME_SIZEMAX 0xff

/**
 @brief Maximum value of tag values in index entry (MOBIIndexTag)
 */
#define INDX_TAGVALUES_MAX 100

/**
 @brief Tag entries in TAGX section (for internal INDX parsing)
 */
typedef struct {
    uint8_t tag; /**< Tag */
    uint8_t values_count; /**< Number of values */
    uint8_t bitmask; /**< Bitmask */
    uint8_t control_byte; /**< EOF control byte */
} TAGXTags;

/**
 @brief Parsed TAGX section (for internal INDX parsing)
 
 TAGX tags hold metadata of index entries.
 It is present in the first index record.
 */
typedef struct {
    TAGXTags *tags; /**< Array of tag entries */
    size_t tags_count; /**< Number of tag entries */
    size_t control_byte_count; /**< Number of control bytes */
} MOBITagx;

/**
 @brief Parsed IDXT section (for internal INDX parsing)
 
 IDXT section holds offsets to index entries
 */
typedef struct {
    uint32_t *offsets; /**< Offsets to index entries */
    size_t offsets_count; /**< Offsets count */
} MOBIIdxt;

/**
 @brief Parsed ORDT sections (for internal INDX parsing)
 
 ORDT sections hold data for decoding index labels.
 It is mapping of encoded chars to unicode.
 */
typedef struct {
    uint8_t *ordt1; /**< ORDT1 offsets */
    uint16_t *ordt2; /**< ORDT2 offsets */
    size_t type; /**< Type (0: 16, 1: 8 bit offsets) */
    size_t ordt1_pos; /**< Offset of ORDT1 data */
    size_t ordt2_pos; /**< Offset of ORDT2 data */
    size_t offsets_count; /**< Offsets count */
} MOBIOrdt;

MOBI_RET mobi_parse_index(const MOBIData *m, MOBIIndx *indx, const size_t indx_record_number);
MOBI_RET mobi_parse_indx(const MOBIPdbRecord *indx_record, MOBIIndx *indx, MOBITagx *tagx, MOBIOrdt *ordt);
MOBI_RET mobi_get_indxentry_tagvalue(uint32_t *tagvalue, const MOBIIndexEntry *entry, const unsigned tag_arr[]);
size_t mobi_get_indxentry_tagarray(uint32_t **tagarr, const MOBIIndexEntry *entry, const size_t tagid);
bool mobi_indx_has_tag(const MOBIIndx *indx, const size_t tagid);
char * mobi_get_cncx_string(const MOBIPdbRecord *cncx_record, const uint32_t cncx_offset);
char * mobi_get_cncx_string_utf8(const MOBIPdbRecord *cncx_record, const uint32_t cncx_offset, MOBIEncoding cncx_encoding);
char * mobi_get_cncx_string_flat(const MOBIPdbRecord *cncx_record, const uint32_t cncx_offset, const size_t length);
MOBI_RET mobi_decode_infl(unsigned char *decoded, int *decoded_size, const unsigned char *rule);
MOBI_RET mobi_decode_infl_old(const MOBIIndx *indx);
MOBI_RET mobi_trie_insert_infl(MOBITrie **root, const MOBIIndx *indx, size_t i);
size_t mobi_trie_get_inflgroups(char **infl_strings, MOBITrie * const root, const char *string);

#endif
