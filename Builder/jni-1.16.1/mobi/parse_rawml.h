/** @file parse_rawml.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_parse_rawml_h
#define mobi_parse_rawml_h

#include "config.h"
#include "mobi.h"

#define MOBI_ATTRNAME_MAXSIZE 150 /**< Maximum length of tag attribute name, like "href" */
#define MOBI_ATTRVALUE_MAXSIZE 150 /**< Maximum length of tag attribute value */

/**
 @brief Result data returned by mobi_search_links_kf7() and mobi_search_links_kf8()
 */
typedef struct {
    unsigned char *start; /**< Beginning data to be replaced */
    unsigned char *end; /**< End of data to be replaced */
    char value[MOBI_ATTRVALUE_MAXSIZE + 1]; /**< Attribute value */
    bool is_url; /**< True if value is part of css url attribute */
} MOBIResult;

MOBI_RET mobi_get_id_by_posoff(uint32_t *file_number, char *id, const MOBIRawml *rawml, const size_t pos_fid, const size_t pos_off);
MOBI_RET mobi_find_attrvalue(MOBIResult *result, const unsigned char *data_start, const unsigned char *data_end, const MOBIFiletype type, const char *needle);

#endif
