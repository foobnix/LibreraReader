/** @file opf.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_opf_h
#define libmobi_opf_h

#include "config.h"
#include "mobi.h"

/** @brief Maximum number of opf meta tags */
#define OPF_META_MAX_TAGS 256

/**
 @defgroup mobi_opf OPF handling structures
 @{
 */

/** @brief OPF <dc:identifier/> element structure
 
 At least one identifier must have an id specified,
 so it can be referenced from the package unique-identifier attribute.
 */
typedef struct {
    char *value; /**< element value */
    char *id; /**< id attribute */
    char *scheme; /**< opf:scheme (optional) */
} OPFidentifier;

/** @brief OPF <dc:creator/> element structure
 
 Also applies to <dc:contributor/> element
 */
typedef struct {
    char *value; /**< element value */
    char *file_as; /**< opf:file-as attribute (optional) */
    char *role; /**< opf:role attribute (optional) */
} OPFcreator;

/** @brief OPF <dc:subject/> element structure */
typedef struct {
    char *value; /**< element value */
    char *basic_code; /**< BASICCode attribute (optional, non-standard) */
} OPFsubject;

/** @brief OPF <dc:date/> element structure
 
 Format: YYYY[-MM[-DD]]
 */
typedef struct {
    char *value; /**< element value */
    char *event; /**< opf:event attribute (optional) */
} OPFdate;

/** @brief OPF <dc-metadata/> element structure */
typedef struct {
    OPFcreator **contributor; /**< <dc:contributor/> element (optional) */
    OPFcreator **creator; /**< <dc:creator/> element (optional) */
    OPFidentifier **identifier; /**< <dc:identifier/> element (required) */
    OPFsubject **subject; /**< <dc:subject/> element (optional) */
    OPFdate **date; /**< <dc:date/> element (optional) */
    char **description; /**< <dc:description/> element (optional) */
    char **language; /**< <dc:language/> element (required) */
    char **publisher; /**< <dc:publisher/> element (optional) */
    char **rights; /**< <dc:rights/> element (optional) */
    char **source; /**< <dc:source/> element (optional) */
    char **title; /**< <dc:title/> element (required) */
    char **type; /**< <dc:type/> element (optional) */
} OPFdcmeta;

/** @brief OPF <srp/> element structure */
typedef struct {
    char *value; /**< element value */
    char *currency; /**< currency attribute */
} OPFsrp;

/** @brief OPF <x-metadata/> element structure */
typedef struct {
    OPFsrp **srp; /**< <srp/> element */
    char **adult; /**< <adult/> element */
    char **default_lookup_index; /**< <DefaultLookupIndex/> element */
    char **dict_short_name; /**< <DictionaryVeryShortName/> element */
    char **dictionary_in_lang; /**< <DictionaryInLanguage/> element */
    char **dictionary_out_lang; /**< <DictionaryOutLanguage/> element */
    char **embedded_cover; /**< <EmbeddedCover/> element */
    char **imprint; /**< <imprint/> element */
    char **review; /**< <review/> element */
} OPFxmeta;

/** @brief OPF <meta/> element structure */
typedef struct {
    char *name; /**< name attribute (required) */
    char *content; /**< content attribute (required) */
} OPFmeta;

/** @brief OPF <metadata/> element structure */
typedef struct {
    OPFmeta **meta; /**< <meta/> element (optional) */
    OPFdcmeta *dc_meta; /**< <dc-metadata/> element */
    OPFxmeta *x_meta; /**< <x-metadata/> element */
} OPFmetadata;

/** @brief OPF <item/> element structure */
typedef struct {
    char *id; /**< id attribute (required) */
    char *href; /**< href attribute (required) */
    char *media_type; /**< media-type attribute (required) */
} OPFitem;

/** @brief OPF <manifest/> element structure */
typedef struct {
    OPFitem **item; /**< <item/> element */
} OPFmanifest;

/** @brief OPF <spine/> element structure */
typedef struct {
    char *toc; /**< toc attribute (required) */
    char **itemref; /**< <itemref idref="xxx"/> element */
} OPFspine;

/** @brief OPF <reference/> tag structure */
typedef struct {
    char *type; /**< type attribute (required) */
    char *title; /**< title attribute */
    char *href; /**< href attribute (required) */
} OPFreference;

/** @brief OPF <guide/> element structure */
typedef struct {
    OPFreference **reference; /**< <reference/> element tag */
} OPFguide;

/** @brief OPF <package/> element structure */
typedef struct {
    //char *uid; /**< <package unique-identifier="uid"/> */
    OPFmetadata *metadata; /**< <metadata/> (required) */
    OPFmanifest *manifest; /**< <manifest/> (required) */
    OPFspine *spine; /**< <spine/> (required) */
    OPFguide *guide; /**< <guide/> (optional) */
} OPF;

/** @brief NCX index entry structure */
typedef struct {
    size_t id; /**< Sequential id */
    char *text; /**< Entry text content */
    char *target; /**< Entry target reference */
    size_t level; /**< Entry level */
    size_t parent; /**< Entry parent */
    size_t first_child; /**< First child id */
    size_t last_child; /**< Last child id */
} NCX;
/** @} */


MOBI_RET mobi_build_opf(MOBIRawml *rawml, const MOBIData *m);
MOBI_RET mobi_build_ncx(MOBIRawml *rawml, const OPF *opf);

#endif
