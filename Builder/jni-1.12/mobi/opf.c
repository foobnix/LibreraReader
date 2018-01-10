/** @file opf.c
 *  @brief Functions for handling OPF structures
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#define _GNU_SOURCE 1
#ifndef __USE_BSD
#define __USE_BSD /* for strdup on linux/glibc */
#endif

#include <stdlib.h>
#include <string.h>
#include "opf.h"
#ifdef USE_LIBXML2
#ifdef __clang__
#pragma clang diagnostic push
/* suppress clang documentation warning for libxml headers */
#pragma clang diagnostic ignored "-Wdocumentation"
#endif
#include <libxml/encoding.h>
#include <libxml/xmlwriter.h>
#ifdef __clang__
#pragma clang diagnostic pop
#endif
#else
#include "xmlwriter.h"
#endif
#include "index.h"
#include "util.h"
#include "parse_rawml.h"
#include "debug.h"

/**
 @brief Array of valid OPF guide types
 
 http://www.idpf.org/epub/20/spec/OPF_2.0.1_draft.htm#Section2.6
 */
const char *mobi_guide_types[] = {
    "cover", /**< the book cover(s), jacket information, etc. */
    "title-page", /**< page with possibly title, author, publisher, and other metadata */
    "toc", /**< table of contents */
    "index", /**< back-of-book style index */
    "glossary", /**< glossary */
    "acknowledgements", /**< acknowledgements */
    "bibliography", /**< bibliography */
    "colophon", /**< colophon */
    "copyright-page", /**< copyright page */
    "dedication", /**< dedication */
    "epigraph", /**< epigraph */
    "foreword", /**< foreword */
    "loi", /**< list of illustrations */
    "lot", /**< list of tables */
    "notes", /**< notes */
    "preface", /**< preface */
    "text", /**< First "real" page of content (e.g. "Chapter 1") */
    NULL /**< eof */
};

/**
 @brief Check if type is valid OPF guide element
 
 Compares types with elements of mobi_guide_types[] array
 
 @param[in] type OPF guide type
 @return True if type is valid guide type, false otherwise
 */
bool mobi_is_guide_type(const char *type) {
    size_t i = 0;
    size_t type_length = strlen(type);
    while (mobi_guide_types[i]) {
        if (strncmp(mobi_guide_types[i++], type, type_length) == 0) {
            return true;
        }
    }
    /* check if "other" type */
    if (strncmp(type, "other.", 6) == 0) { return true; }
    return false;
}

/**
 @brief Reconstruct guide part of the OPF file
 
 @param[in,out] opf Structure OPF->OPFguide will be filled with parsed data
 @param[in] rawml Structure MOBIRawml will be parsed
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_build_opf_guide(OPF *opf, const MOBIRawml *rawml) {
    /* parse guide data */
    if (rawml == NULL || rawml->guide == NULL) {
        debug_print("%s\n", "Initialization failed");
        return MOBI_INIT_FAILED;
    }
    size_t i = 0, j = 0;
    MOBI_RET ret;
    size_t count = rawml->guide->entries_count;
    if (count == 0) {
        return MOBI_SUCCESS;
    }
    opf->guide = malloc(sizeof(OPFguide));
    if (opf->guide == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    OPFreference **reference = malloc((count + 1) * sizeof(OPFreference*));
    if (reference == NULL) {
        free(opf->guide);
        opf->guide = NULL;
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    if (rawml->guide->cncx_record == NULL) {
        free(reference);
        free(opf->guide);
        opf->guide = NULL;
        debug_print("%s\n", "Missing cncx record");
        return MOBI_DATA_CORRUPT;
    }
    while (i < count) {
        const MOBIIndexEntry *guide_entry = &rawml->guide->entries[i];
        const char *type = guide_entry->label;
        uint32_t cncx_offset;
        ret = mobi_get_indxentry_tagvalue(&cncx_offset, guide_entry, INDX_TAG_GUIDE_TITLE_CNCX);
        if (ret != MOBI_SUCCESS) {
            free(reference);
            free(opf->guide);
            opf->guide = NULL;
            return ret;
        }
        const MOBIPdbRecord *cncx_record = rawml->guide->cncx_record;
        char *ref_title = mobi_get_cncx_string_utf8(cncx_record, cncx_offset, rawml->guide->encoding);
        if (ref_title == NULL) {
            free(reference);
            free(opf->guide);
            opf->guide = NULL;
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        uint32_t frag_number = MOBI_NOTSET;
        ret = mobi_get_indxentry_tagvalue(&frag_number, guide_entry, INDX_TAG_FRAG_POSITION);
        if (ret != MOBI_SUCCESS) {
            debug_print("INDX_TAG_FRAG_POSITION not found (%i)\n", ret);
            free(ref_title);
            i++;
            continue;
            /* FIXME: I need some examples which use other tags */
            //mobi_get_indxentry_tagvalue(&frag_number, guide_entry, INDX_TAG_FRAG_FILE_NR);
        }
        if (frag_number > rawml->frag->entries_count) {
            debug_print("Wrong frag entry index (%i)\n", frag_number);
            free(ref_title);
            i++;
            continue;
        }
        const MOBIIndexEntry *frag_entry = &rawml->frag->entries[frag_number];
        uint32_t file_number;
        ret = mobi_get_indxentry_tagvalue(&file_number, frag_entry, INDX_TAG_FRAG_FILE_NR);
        if (ret != MOBI_SUCCESS) {
            free(reference);
            free(opf->guide);
            free(ref_title);
            opf->guide = NULL;
            return ret;
        }
        /* check if valid guide type */
        char *ref_type;
        size_t type_size = strlen(type);
        if (!mobi_is_guide_type(type)) {
            /* prepend "other." prefix */
            type_size += 6;
            ref_type = malloc(type_size + 1);
            if (ref_type == NULL) {
                free(reference);
                free(opf->guide);
                opf->guide = NULL;
                free(ref_title);
                debug_print("%s\n", "Memory allocation failed");
                return MOBI_MALLOC_FAILED;
            }
            snprintf(ref_type, type_size + 1, "other.%s", type);
        } else {
            ref_type = malloc(type_size + 1);
            if (ref_type == NULL) {
                free(reference);
                free(opf->guide);
                opf->guide = NULL;
                free(ref_title);
                debug_print("%s\n", "Memory allocation failed");
                return MOBI_MALLOC_FAILED;
            }
            strncpy(ref_type, type, type_size);
            ref_type[type_size] = '\0';
        }
        debug_print("<reference type=\"%s\" title=\"%s\" href=\"part%05u.html\" />", ref_type, ref_title, file_number);
        char href[FILENAME_MAX + 1];
        snprintf(href, FILENAME_MAX, "part%05u.html", file_number);
        char *ref_href = strdup(href);
        reference[j] = calloc(1, sizeof(OPFreference));
        *reference[j] = (OPFreference) { ref_type, ref_title, ref_href };
        i++;
        j++;
    }
    /* terminate array with NULL */
    reference[j] = NULL;
    opf->guide->reference = reference;
    return MOBI_SUCCESS;
}

/**
 @brief Write <navPoint/> entries for given ncx level
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] ncx Array of NCX structures with ncx content
 @param[in] level TOC level
 @param[in] from First entry in NCX array to copy from
 @param[in] to Last entry in NCX array to copy from
 @param[in] seq Sequential number for playOrder attribute
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_write_ncx_level(xmlTextWriterPtr writer, const NCX *ncx, const size_t level, const size_t from, const size_t to, size_t *seq) {
    for (size_t i = from; i < to; i++) {
        if (level != ncx[i].level) {
            continue;
        }
        /* start <navPoint> */
        char playorder[10 + 1];
        snprintf(playorder, 11, "%u", (uint32_t) (*seq)++);

        /* id string (max 10 digits and dash) for each level + "toc" + terminator */
        size_t id_size = 11 * (level + 1) + 3 + 1;
        char *id = malloc(id_size);
        if (id == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        strcpy(id, "toc");
        size_t curr_id = i;
        while (curr_id != MOBI_NOTSET) {
            size_t parent_id = ncx[curr_id].parent;
            if (parent_id == curr_id) {
                debug_print("%s\n", "Skip id of corrupt ncx entry");
                break;
            }
            size_t curr_from = 0;
            if (parent_id != MOBI_NOTSET && ncx[parent_id].first_child != MOBI_NOTSET) {
                curr_from = ncx[parent_id].first_child;
            }
            char level_id[10 + 1];
            snprintf(level_id, 11, "%u", (uint32_t) (curr_id - curr_from + 1));
            char *id_copy = strdup(id + 3);
            if (id_copy == NULL) {
                debug_print("%s\n", "Memory allocation failed");
                free(id);
                return MOBI_MALLOC_FAILED;
            }
            snprintf(id, id_size, "toc-%s%s", level_id, id_copy);
            free(id_copy);
            curr_id = parent_id;
        }
        
        int xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "navPoint");
        if (xml_ret < 0) {
            free(id);
            return MOBI_XML_ERR;
        }
        xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "id", BAD_CAST id);
        free(id);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "playOrder", BAD_CAST playorder);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        /* write <navLabel> */
        xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "navLabel");
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "text");
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterWriteString(writer, BAD_CAST ncx[i].text);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        /* write <content> */
        xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "content");
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "src", BAD_CAST ncx[i].target);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
        debug_print("%s - %s\n", ncx[i].text, ncx[i].target);
        if (ncx[i].first_child != MOBI_NOTSET && ncx[i].last_child != MOBI_NOTSET) {
            MOBI_RET ret = mobi_write_ncx_level(writer, ncx, level + 1, ncx[i].first_child, ncx[i].last_child, seq);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
        }
        /* end <navPoint> */
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) { return MOBI_XML_ERR; }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write element <meta name="name" content="content"/> to XML buffer
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] name Attribute name
 @param[in] content Attribute content
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_meta(xmlTextWriterPtr writer, const char *name, const char *content) {
    int xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "meta");
    if (xml_ret < 0) {
        debug_print("XML error: %i (name: %s, content: %s)\n", xml_ret, name, content);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "name", BAD_CAST name);
    if (xml_ret < 0) {
        debug_print("XML error: %i (name: %s, content: %s)\n", xml_ret, name, content);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "content", BAD_CAST content);
    if (xml_ret < 0) {
        debug_print("XML error: %i (name: %s, content: %s)\n", xml_ret, name, content);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) {
        debug_print("XML error: %i (name: %s, content: %s)\n", xml_ret, name, content);
        return MOBI_XML_ERR;
    }
    return MOBI_SUCCESS;
}


/**
 @brief Add reconstruced opf part to rawml
 
 @param[in] opf_xml OPF xml string
 @param[in,out] rawml New data will be added to MOBIRawml rawml->resources structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_opf_add_to_rawml(const char *opf_xml, MOBIRawml *rawml) {
    MOBIPart *opf_part;
    size_t uid = 0;
    if (rawml->resources) {
        MOBIPart *part = rawml->resources;
        while (part->next) {
            part = part->next;
        }
        uid = part->uid + 1;
        part->next = calloc(1, sizeof(MOBIPart));
        opf_part = part->next;
    }
    else {
        rawml->resources = calloc(1, sizeof(MOBIPart));
        opf_part = rawml->resources;
    }
    if (opf_part == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    opf_part->uid = uid;
    opf_part->next = NULL;
    opf_part->data = (unsigned char *) strdup(opf_xml);
    if (opf_part->data == NULL) {
        free(opf_part);
        opf_part = NULL;
        return MOBI_MALLOC_FAILED;
    }
    opf_part->size = strlen(opf_xml);
    opf_part->type = T_OPF;
    return MOBI_SUCCESS;
}

/**
 @brief Add reconstruced ncx part to rawml
 
 @param[in] ncx_xml OPF xml string
 @param[in,out] rawml New data will be added to MOBIRawml rawml->resources structure
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_ncx_add_to_rawml(const char *ncx_xml, MOBIRawml *rawml) {
    MOBIPart *ncx_part;
    size_t uid = 0;
    if (rawml->resources) {
        MOBIPart *part = rawml->resources;
        while (part->next) {
            part = part->next;
        }
        uid = part->uid + 1;
        part->next = calloc(1, sizeof(MOBIPart));
        ncx_part = part->next;
    }
    else {
        rawml->resources = calloc(1, sizeof(MOBIPart));
        ncx_part = rawml->resources;
    }
    if (ncx_part == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    ncx_part->uid = uid;
    ncx_part->next = NULL;
    ncx_part->data = (unsigned char *) strdup(ncx_xml);
    if (ncx_part->data == NULL) {
        free(ncx_part);
        ncx_part = NULL;
        return MOBI_MALLOC_FAILED;
    }
    ncx_part->size = strlen(ncx_xml);
    ncx_part->type = T_NCX;
    return MOBI_SUCCESS;
}

/**
 @brief Write ncx header
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] opf OPF structure to fetch some data
 @param[in] maxlevel Value of dtb:depth attribute
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_write_ncx_header(xmlTextWriterPtr writer, const OPF *opf, uint32_t maxlevel) {
    /* write header */
    char depth[10 + 1];
    snprintf(depth, 11, "%d", maxlevel);

    /* <head> */
    int xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "head");
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    /* meta uid */
    MOBI_RET ret = mobi_xml_write_meta(writer, "dtb:uid", opf->metadata->dc_meta->identifier[0]->value);
    if (ret != MOBI_SUCCESS) { return ret; }
    /* meta depth */
    ret = mobi_xml_write_meta(writer, "dtb:depth", depth);
    if (ret != MOBI_SUCCESS) { return ret; }
    /* meta pagecount */
    ret = mobi_xml_write_meta(writer, "dtb:totalPageCount", "0");
    if (ret != MOBI_SUCCESS) { return ret; }
    /* meta pagenumber */
    ret = mobi_xml_write_meta(writer, "dtb:maxPageNumber", "0");
    if (ret != MOBI_SUCCESS) { return ret; }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    // <docTitle>
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "docTitle");
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "text");
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    xml_ret = xmlTextWriterWriteString(writer, BAD_CAST opf->metadata->dc_meta->title[0]);
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) { return MOBI_XML_ERR; }
    return MOBI_SUCCESS;
}

/**
 @brief Build ncx document using libxml2 and append it to rawml
 
 @param[in,out] rawml MOBIRawml structure
 @param[in] ncx Array of NCX structures with ncx content
 @param[in] opf OPF structure to fetch some data
 @param[in] maxlevel Value of dtb:depth attribute
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_write_ncx(MOBIRawml *rawml, const NCX *ncx, const OPF *opf, uint32_t maxlevel) {
    const xmlChar * NCXNamespace = BAD_CAST "http://www.daisy.org/z3986/2005/ncx/";
    xmlBufferPtr buf = xmlBufferCreate();
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    xmlTextWriterPtr writer = xmlNewTextWriterMemory(buf, 0);
    if (writer == NULL) {
        xmlBufferFree(buf);
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    xmlTextWriterSetIndent(writer, 1);
    int xml_ret = xmlTextWriterStartDocument(writer, NULL, NULL, NULL);
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterStartElementNS(writer, NULL, BAD_CAST "ncx", NCXNamespace);
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "version", BAD_CAST "2005-1");
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "xml:lang", BAD_CAST opf->metadata->dc_meta->language[0]);
    if (xml_ret < 0) { goto cleanup; }
    
    MOBI_RET ret = mobi_write_ncx_header(writer, opf, maxlevel);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    
    /* start <navMap> */
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "navMap");
    if (xml_ret < 0) { goto cleanup; }
    if (ncx) {
        const size_t count = rawml->ncx->entries_count;
        size_t seq = 1;
        ret = mobi_write_ncx_level(writer, ncx, 0, 0, count, &seq);
        if (ret != MOBI_SUCCESS) { goto cleanup; }
    }

    /* end <navMap> */
    xml_ret = xmlTextWriterEndDocument(writer);
    if (xml_ret < 0) { goto cleanup; }
    xmlFreeTextWriter(writer);
    const char *ncx_xml = (const char *) buf->content;
    mobi_ncx_add_to_rawml(ncx_xml, rawml);
    xmlBufferFree(buf);
    return MOBI_SUCCESS;
    
cleanup:
    xmlFreeTextWriter(writer);
    xmlBufferFree(buf);
    debug_print("%s\n", "XML writing failed");
    return MOBI_XML_ERR;
}


/**
 @brief Free array of ncx entries
 
 @param[in] ncx Array of NCX structures with ncx content
 @param[in] count Size of the array
 */
void mobi_free_ncx(NCX *ncx, size_t count) {
    if (ncx) {
        while (count--) {
            free(ncx[count].target);
            free(ncx[count].text);
        }
        free(ncx);
    }
}

/**
 @brief Parse ncx index, recreate ncx document and append it to rawml
 
 @param[in,out] rawml MOBIRawml structure
 @param[in] opf OPF structure to fetch some data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_build_ncx(MOBIRawml *rawml, const OPF *opf) {
    /* parse ncx data */
    if (rawml == NULL) {
        debug_print("%s\n", "Initialization failed");
        return MOBI_INIT_FAILED;
    }
    if (rawml->ncx && rawml->ncx->cncx_record) {
        size_t i = 0;
        uint32_t maxlevel = 0;
        MOBI_RET ret;
        const size_t count = rawml->ncx->entries_count;
        if (count == 0) {
            return MOBI_SUCCESS;
        }
        NCX *ncx = malloc(count * sizeof(NCX));
        if (ncx == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        while (i < count) {
            const MOBIIndexEntry *ncx_entry = &rawml->ncx->entries[i];
            const char *label = ncx_entry->label;
            const size_t id = strtoul(label, NULL, 16);
            uint32_t cncx_offset;
            ret = mobi_get_indxentry_tagvalue(&cncx_offset, ncx_entry, INDX_TAG_NCX_TEXT_CNCX);
            if (ret != MOBI_SUCCESS) {
                mobi_free_ncx(ncx, i);
                return ret;
            }
            const MOBIPdbRecord *cncx_record = rawml->ncx->cncx_record;
            char *text = mobi_get_cncx_string_utf8(cncx_record, cncx_offset, rawml->ncx->encoding);
            if (text == NULL) {
                mobi_free_ncx(ncx, i);
                debug_print("%s\n", "Memory allocation failed");
                return MOBI_MALLOC_FAILED;
            }
            char *target = malloc(MOBI_ATTRNAME_MAXSIZE + 1);
            if (target == NULL) {
                free(text);
                mobi_free_ncx(ncx, i);
                debug_print("%s\n", "Memory allocation failed");
                return MOBI_MALLOC_FAILED;
            }
            if (mobi_is_rawml_kf8(rawml)) {
                uint32_t posfid;
                ret = mobi_get_indxentry_tagvalue(&posfid, ncx_entry, INDX_TAG_NCX_POSFID);
                if (ret != MOBI_SUCCESS) {
                    free(text);
                    free(target);
                    mobi_free_ncx(ncx, i);
                    return ret;
                }
                uint32_t posoff;
                ret = mobi_get_indxentry_tagvalue(&posoff, ncx_entry, INDX_TAG_NCX_POSOFF);
                if (ret != MOBI_SUCCESS) {
                    free(text);
                    free(target);
                    mobi_free_ncx(ncx, i);
                    return ret;
                }
                uint32_t filenumber;
                char targetid[MOBI_ATTRNAME_MAXSIZE + 1];
                ret = mobi_get_id_by_posoff(&filenumber, targetid, rawml, posfid, posoff);
                if (ret != MOBI_SUCCESS) {
                    free(text);
                    free(target);
                    mobi_free_ncx(ncx, i);
                    return ret;
                }
                /* FIXME: posoff == 0 means top of file? */
                if (posoff) {
                    snprintf(target, MOBI_ATTRNAME_MAXSIZE + 1, "part%05u.html#%s", filenumber, targetid);
                } else {
                    snprintf(target, MOBI_ATTRNAME_MAXSIZE + 1, "part%05u.html", filenumber);
                }
                
            } else {
                uint32_t filepos;
                ret = mobi_get_indxentry_tagvalue(&filepos, ncx_entry, INDX_TAG_NCX_FILEPOS);
                if (ret != MOBI_SUCCESS) {
                    free(text);
                    free(target);
                    mobi_free_ncx(ncx, i);
                    return ret;
                }
                snprintf(target, MOBI_ATTRNAME_MAXSIZE + 1, "part00000.html#%010u", filepos);
            }
            uint32_t level;
            ret = mobi_get_indxentry_tagvalue(&level, ncx_entry, INDX_TAG_NCX_LEVEL);
            if (ret != MOBI_SUCCESS) {
                free(text);
                free(target);
                mobi_free_ncx(ncx, i);
                return ret;
            }
            if (level > maxlevel) {
                maxlevel = level;
            }
            uint32_t parent = MOBI_NOTSET;
            ret = mobi_get_indxentry_tagvalue(&parent, ncx_entry, INDX_TAG_NCX_PARENT);
            if (ret == MOBI_INIT_FAILED) {
                free(text);
                free(target);
                mobi_free_ncx(ncx, i);
                return ret;
            }
            uint32_t first_child = MOBI_NOTSET;
            ret = mobi_get_indxentry_tagvalue(&first_child, ncx_entry, INDX_TAG_NCX_CHILD_START);
            if (ret == MOBI_INIT_FAILED) {
                free(text);
                free(target);
                mobi_free_ncx(ncx, i);
                return ret;
            }
            uint32_t last_child = MOBI_NOTSET;
            ret = mobi_get_indxentry_tagvalue(&last_child, ncx_entry, INDX_TAG_NCX_CHILD_END);
            if (ret == MOBI_INIT_FAILED) {
                free(text);
                free(target);
                mobi_free_ncx(ncx, i);
                return ret;
            }
            if ((first_child != MOBI_NOTSET && first_child > rawml->ncx->entries_count) ||
                (last_child != MOBI_NOTSET && last_child > rawml->ncx->entries_count) ||
                (parent != MOBI_NOTSET && parent > rawml->ncx->entries_count)) {
                free(text);
                free(target);
                mobi_free_ncx(ncx, i);
                return MOBI_DATA_CORRUPT;
            }
            debug_print("seq=%zu, id=%zu, text='%s', target='%s', level=%u, parent=%u, fchild=%u, lchild=%u\n", i, id, text, target, level, parent, first_child, last_child);
            ncx[i++] = (NCX) {id, text, target, level, parent, first_child, last_child};
        }
        mobi_write_ncx(rawml, ncx, opf, maxlevel);
        mobi_free_ncx(ncx, count);
    } else {
        mobi_write_ncx(rawml, NULL, opf, 1);
    }
    return MOBI_SUCCESS;
}

/**
 @brief Copy text data from EXTH record to array of strings
 
 It will allocate memory for the array if not already allocated.
 It will find first array index that is not already used
 
 @param[in] m MOBIData structure
 @param[in] exth MOBIExthHeader record
 @param[in,out] array Array into which text string will be inserted
 */
static void mobi_opf_fill_tag(const MOBIData *m, const MOBIExthHeader *exth, char ***array) {
    if (*array == NULL) {
        *array = calloc(OPF_META_MAX_TAGS, sizeof(**array));
        if (*array == NULL) {
            return;
        }
    }
    size_t i = 0;
    while (i < OPF_META_MAX_TAGS) {
        /* find first free slot */
        if((*array)[i] != NULL) { i++; continue; }
        MOBIExthMeta exth_tag = mobi_get_exthtagmeta_by_tag(exth->tag);
        char *value = NULL;
        if (exth_tag.type == EXTH_NUMERIC) {
            value = malloc(10 + 1);
            if (value) {
                const uint32_t val32 = mobi_decode_exthvalue(exth->data, exth->size);
                snprintf(value, 10 + 1, "%d", val32);
            }
        } else if (exth_tag.type == EXTH_STRING) {
            value = mobi_decode_exthstring(m, exth->data, exth->size);
        }
        if (value) {
            (*array)[i] = value;
        }
        return;
    }
    /* not enough tags */
    debug_print("OPF_META_MAX_TAGS = %i reached\n", OPF_META_MAX_TAGS);
}

/**
 @brief Set values for attributes of OPF <meta/> tag
 
 It will allocate memory for the OPFmeta members: name and content.
 It will find first array index that is not already used
 
 @param[in,out] meta Array of OPFmeta structures to be filled with data
 @param[in] name Value of the name attribute
 @param[in] content Value of the content attribute
 */
static void mobi_opf_set_meta(OPFmeta **meta, const char *name, const char *content) {
    size_t i = 0;
    while (i < OPF_META_MAX_TAGS) {
        /* find first free slot */
        if(meta[i] != NULL) { i++; continue; }
        meta[i] = malloc(sizeof(OPFmeta));
        if (meta[i] == NULL) {
            return;
        }
        meta[i]->name = strdup(name);
        meta[i]->content = strdup(content);
        if (meta[i]->name == NULL || meta[i]->content == NULL) {
            free(meta[i]);
            meta[i] = NULL;
        }
        return;
    }
    /* not enough tags */
    debug_print("OPF_META_MAX_TAGS = %i reached\n", OPF_META_MAX_TAGS);
}

/**
 @brief Set values for attributes of OPF <meta/> tag
 
 It will allocate memory for the OPFmeta members: name and content.
 Content attribute will be copied from EXTH record.
 It will find first array index that is not already used
 
 @param[in] m MOBIData structure
 @param[in] exth MOBIExthHeader structure containing EXTH records
 @param[in,out] meta Array of OPFmeta structures to be filled with data
 @param[in] name Value of the name attribute
 */
static void mobi_opf_copy_meta(const MOBIData *m, const MOBIExthHeader *exth, OPFmeta **meta, const char *name) {
    MOBIExthMeta exth_tag = mobi_get_exthtagmeta_by_tag(exth->tag);
    char *content = NULL;
    if (exth_tag.tag == EXTH_COVEROFFSET) {
        content = malloc(13 + 1);
        if (content) {
            const uint32_t val32 = mobi_decode_exthvalue(exth->data, exth->size);
            snprintf(content, 14, "resource%05d", val32);
        }
    } else if (exth_tag.type == EXTH_NUMERIC) {
        content = malloc(10 + 1);
        if (content) {
            const uint32_t val32 = mobi_decode_exthvalue(exth->data, exth->size);
            snprintf(content, 11, "%d", val32);
        }
    } else if (exth_tag.type == EXTH_STRING) {
        char *string = mobi_decode_exthstring(m, exth->data, exth->size);
        content = string;
    }
    if (content) {
        mobi_opf_set_meta(meta, name, content);
        free(content);
    }
}

/**
 @brief Set values for attributes of OPF manifest <item/> tag
 
 It will allocate memory for the OPFitem members: id, href and media-type.
 It will find first array index that is not already used
 
 @param[in,out] meta Array of OPFmeta structures to be filled with data
 @param[in] name Value of the name attribute
 @param[in] content Value of the content attribute
 */
void mobi_opf_set_item(OPFmeta **meta, const char *name, const char *content) {
    size_t i = 0;
    while (i < OPF_META_MAX_TAGS) {
        /* find first free slot */
        if(meta[i] != NULL) { i++; continue; }
        meta[i] = malloc(sizeof(OPFmeta));
        if (meta[i] == NULL) {
            return;
        }
        meta[i]->name = strdup(name);
        meta[i]->content = strdup(content);
        if (meta[i]->name == NULL || meta[i]->content == NULL) {
            free(meta[i]);
            meta[i] = NULL;
        }
        return;
    }
    /* not enough tags */
    debug_print("OPF_META_MAX_TAGS = %i reached\n", OPF_META_MAX_TAGS);
}

/**
 @brief Copy text data from EXTH record to "member_name" member of a structure with given type
 
 Data will copied from curr->data.
 It will allocate memory for the array of structures if not already allocated.
 It will find first array index that is not already used
 
 @param[in] mobidata Mobidata structure
 @param[in] struct_type Structure type defined with typedef
 @param[in] struct_element Member member_name of this structure will be set to EXTH data
 @param[in] member_name Structure member name that will be modified
 */
#define mobi_opf_copy_tagtype(mobidata, struct_type, struct_element, member_name) { \
    if (struct_element == NULL) { \
        struct_element = calloc(OPF_META_MAX_TAGS, sizeof(*struct_element)); \
        if(struct_element == NULL) { \
            debug_print("%s\n", "Memory allocation failed"); \
            return MOBI_MALLOC_FAILED; \
        } \
    } \
    struct_type **element = struct_element; \
    size_t i = 0; \
    while (i < OPF_META_MAX_TAGS) { \
        /* find first free slot */ \
        if(element[i] != NULL) { \
            if(element[i]->member_name != NULL) { i++; continue; } \
        } else { \
            element[i] = calloc(1, sizeof(*element[i])); \
            if(element[i] == NULL) { \
                debug_print("%s\n", "Memory allocation failed"); \
                return MOBI_MALLOC_FAILED; \
            } \
        } \
        MOBIExthMeta exth_tag = mobi_get_exthtagmeta_by_tag(curr->tag); \
        char *value = NULL; \
        MOBI_RET error_ret = MOBI_DATA_CORRUPT; \
        if (exth_tag.type == EXTH_NUMERIC) { \
            value = malloc(10 + 1); \
            if (value) { \
                const uint32_t val32 = mobi_decode_exthvalue(curr->data, curr->size); \
                snprintf(value, 10 + 1, "%d", val32); \
            } else { \
                error_ret = MOBI_MALLOC_FAILED; \
            } \
        } else if (exth_tag.type == EXTH_STRING) { \
            value = mobi_decode_exthstring(mobidata, curr->data, curr->size); \
        } \
        if(value == NULL) { \
            free(element[i]); \
            element[i] = NULL; \
            debug_print("%s\n", "Decoding failed"); \
            return error_ret; \
        } \
        element[i]->member_name = value; \
        break; \
    } \
    if (i == OPF_META_MAX_TAGS) { \
    /* not enough tags */ \
    debug_print("OPF_META_MAX_TAGS = %i reached\n", OPF_META_MAX_TAGS); \
    } \
}

/**
 @brief Set "member_name" member of a structure with given type to string value
 
 It will allocate memory for the array of structures if not already allocated.
 It will find first array index that is not already used
 
 @param[in] struct_type Structure type defined with typedef
 @param[in] struct_element Member member_name of this structure will be set to EXTH data
 @param[in] member_name Structure member name that will be modified
 @param[in] string String value that will be assigned to the structure memeber
 */
#define mobi_opf_set_tagtype(struct_type, struct_element, member_name, string) { \
    if (struct_element == NULL) { \
        struct_element = calloc(OPF_META_MAX_TAGS, sizeof(*struct_element)); \
        if(struct_element == NULL) { \
            debug_print("%s\n", "Memory allocation failed"); \
            return MOBI_MALLOC_FAILED; \
        } \
    } \
    struct_type **element = struct_element; \
    size_t i = 0; \
    while (i < OPF_META_MAX_TAGS) { \
        /* find first free slot */ \
        if(element[i] != NULL) { \
            if(element[i]->member_name != NULL) { i++; continue; } \
        } else { \
            element[i] = calloc(1, sizeof(*element[i])); \
            if(element[i] == NULL) { \
                debug_print("%s\n", "Memory allocation failed"); \
                return MOBI_MALLOC_FAILED; \
            } \
        } \
        element[i]->member_name = strdup(string); \
        if(element[i]->member_name == NULL) { \
            free(element[i]); \
            element[i] = NULL; \
            debug_print("%s\n", "Memory allocation failed"); \
            return MOBI_MALLOC_FAILED; \
        } \
        break; \
    } \
    if (i == OPF_META_MAX_TAGS) { \
        /* not enough tags */ \
        debug_print("OPF_META_MAX_TAGS = %i reached\n", OPF_META_MAX_TAGS); \
    } \
}

/**
 @brief Copy text data from EXTH record to OPFmetadata tags structure
 
 @param[in,out] metadata Structure OPFmetadata will be filled with parsed data
 @param[in] m MOBIData structure with loaded data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_get_opf_from_exth(OPFmetadata *metadata, const MOBIData *m) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (m->eh == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBIExthHeader *curr = m->eh;
    /* iterate through EXTH records */
    while (curr != NULL) {
        switch (curr->tag) {
                /* <dc-metadata/> */
            case EXTH_DESCRIPTION:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->description);
                break;
            case EXTH_LANGUAGE:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->language);
                break;
            case EXTH_PUBLISHER:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->publisher);
                break;
            case EXTH_RIGHTS:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->rights);
                break;
            case EXTH_SOURCE:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->source);
                break;
            case EXTH_TITLE:
            case EXTH_UPDATEDTITLE:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->title);
                break;
            case EXTH_TYPE:
                mobi_opf_fill_tag(m, curr, &metadata->dc_meta->type);
                break;
            case EXTH_AUTHOR:
                mobi_opf_copy_tagtype(m, OPFcreator, metadata->dc_meta->creator, value);
                break;
            case EXTH_CONTRIBUTOR:
                mobi_opf_copy_tagtype(m, OPFcreator, metadata->dc_meta->contributor, value);
                break;
            case EXTH_SUBJECT:
                mobi_opf_copy_tagtype(m, OPFsubject, metadata->dc_meta->subject, value);
                break;
            case EXTH_SUBJECTCODE:
                mobi_opf_copy_tagtype(m, OPFsubject, metadata->dc_meta->subject, basic_code);
                break;
            case EXTH_ISBN:
                mobi_opf_copy_tagtype(m, OPFidentifier, metadata->dc_meta->identifier, value);
                mobi_opf_set_tagtype(OPFidentifier, metadata->dc_meta->identifier, scheme, "ISBN");
                break;
            case EXTH_PUBLISHINGDATE:
                mobi_opf_copy_tagtype(m, OPFdate, metadata->dc_meta->date, value);
                mobi_opf_set_tagtype(OPFdate, metadata->dc_meta->date, event, "publication");
                break;
                /* <x-metadata/> */
            case EXTH_ADULT:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->adult);
                break;
            case EXTH_DICTNAME:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->dict_short_name);
                break;
            case EXTH_DICTLANGIN:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->dictionary_in_lang);
                break;
            case EXTH_DICTLANGOUT:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->dictionary_out_lang);
                break;
            case EXTH_IMPRINT:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->imprint);
                break;
            case EXTH_REVIEW:
                mobi_opf_fill_tag(m, curr, &metadata->x_meta->review);
                break;
            case EXTH_PRICE:
                mobi_opf_copy_tagtype(m, OPFsrp, metadata->x_meta->srp, value);
                break;
            case EXTH_CURRENCY:
                mobi_opf_copy_tagtype(m, OPFsrp, metadata->x_meta->srp, currency);
                break;
                /* <meta/> */
            case EXTH_FIXEDLAYOUT:
                mobi_opf_copy_meta(m, curr, metadata->meta, "fixed-layout");
                break;
            case EXTH_BOOKTYPE:
                mobi_opf_copy_meta(m, curr, metadata->meta, "book-type");
                break;
            case EXTH_ORIENTATIONLOCK:
                mobi_opf_copy_meta(m, curr, metadata->meta, "orientation-lock");
                break;
            case EXTH_ORIGRESOLUTION:
                mobi_opf_copy_meta(m, curr, metadata->meta, "original-resolution");
                break;
            case EXTH_ZEROGUTTER:
                mobi_opf_copy_meta(m, curr, metadata->meta, "zero-gutter");
                break;
            case EXTH_ZEROMARGIN:
                mobi_opf_copy_meta(m, curr, metadata->meta, "zero-margin");
                break;
            case EXTH_REGIONMAGNI:
                mobi_opf_copy_meta(m, curr, metadata->meta, "region-mag");
                break;
            case EXTH_ALIGNMENT:
                mobi_opf_copy_meta(m, curr, metadata->meta, "primary-writing-mode");
                break;
            case EXTH_OVERRIDEFONTS:
                mobi_opf_copy_meta(m, curr, metadata->meta, "override-kindle-fonts");
                break;
            case EXTH_COVEROFFSET:
                mobi_opf_copy_meta(m, curr, metadata->meta, "cover");
                break;
            default:
                break;
        }
        curr = curr->next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Recreate OPF structure
 
 @param[in,out] opf Structure OPF->OPFmetadata will be filled with parsed data
 @param[in] m MOBIData structure containing document metadata
 @param[in] rawml MOBIRawml structure containing parsed records
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_build_opf_metadata(OPF *opf,  const MOBIData *m, const MOBIRawml *rawml) {
    if (m == NULL) {
        debug_print("%s\n", "Initialization failed");
        return MOBI_INIT_FAILED;
    }
    opf->metadata = calloc(1, sizeof(OPFmetadata));
    if (opf->metadata == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    /* initialize metadata sub-elements */
    opf->metadata->meta = calloc(OPF_META_MAX_TAGS, sizeof(OPFmeta*));
    if (opf->metadata->meta == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    opf->metadata->dc_meta = calloc(1, sizeof(OPFdcmeta));
    if (opf->metadata->dc_meta == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    opf->metadata->x_meta = calloc(1, sizeof(OPFxmeta));
    if (opf->metadata->x_meta == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    if (m->eh) {
        MOBI_RET ret = mobi_get_opf_from_exth(opf->metadata, m);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    }
    /* check for required elements */
    if (opf->metadata->dc_meta->identifier == NULL) {
        /* default id will be "0" */
        char uid_string[11] = "0";
        if (m->mh && m->mh->uid) {
            snprintf(uid_string, 11, "%u", *m->mh->uid);
        }
        mobi_opf_set_tagtype(OPFidentifier, opf->metadata->dc_meta->identifier, value, uid_string);
        mobi_opf_set_tagtype(OPFidentifier, opf->metadata->dc_meta->identifier, id, "uid");
    } else {
        opf->metadata->dc_meta->identifier[0]->id = strdup("uid");
    }
    if (opf->metadata->dc_meta->title == NULL) {
        opf->metadata->dc_meta->title = calloc(OPF_META_MAX_TAGS, sizeof(char*));
        if (opf->metadata->dc_meta->title == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        char *title = mobi_meta_get_title(m);
        if (title == NULL) {
            title = strdup("Unknown");
        }
        opf->metadata->dc_meta->title[0] = title;
    }
    if (opf->metadata->dc_meta->language == NULL) {
        opf->metadata->dc_meta->language = calloc(OPF_META_MAX_TAGS, sizeof(char*));
        if (opf->metadata->dc_meta->language == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        const char *lang_string = NULL;
        if (m->mh && m->mh->locale) {
            uint32_t lang_code = *m->mh->locale;
            lang_string = mobi_get_locale_string(lang_code);
        }
        if (lang_string) {
            opf->metadata->dc_meta->language[0] = strdup(lang_string);
        } else {
            opf->metadata->dc_meta->language[0] = strdup("en");
        }
    }
    /* write optional elements */
    if (mobi_is_dictionary(m)) {
        if (opf->metadata->x_meta->dictionary_in_lang == NULL) {
            if (m->mh && m->mh->dict_input_lang) {
                opf->metadata->x_meta->dictionary_in_lang = calloc(OPF_META_MAX_TAGS, sizeof(char*));
                if (opf->metadata->x_meta->dictionary_in_lang == NULL) {
                    debug_print("%s\n", "Memory allocation failed");
                    return MOBI_MALLOC_FAILED;
                }
                uint32_t dict_lang_in = *m->mh->dict_input_lang;
                opf->metadata->x_meta->dictionary_in_lang[0] = strdup(mobi_get_locale_string(dict_lang_in));
            }
        }
        if (opf->metadata->x_meta->dictionary_out_lang == NULL) {
            if (m->mh && m->mh->dict_output_lang) {
                opf->metadata->x_meta->dictionary_out_lang = calloc(OPF_META_MAX_TAGS, sizeof(char*));
                if (opf->metadata->x_meta->dictionary_out_lang == NULL) {
                    debug_print("%s\n", "Memory allocation failed");
                    return MOBI_MALLOC_FAILED;
                }
                uint32_t dict_lang_in = *m->mh->dict_output_lang;
                opf->metadata->x_meta->dictionary_out_lang[0] = strdup(mobi_get_locale_string(dict_lang_in));
            }
        }
        if (rawml->orth->orth_index_name) {
            opf->metadata->x_meta->default_lookup_index = calloc(OPF_META_MAX_TAGS, sizeof(char*));
			if (opf->metadata->x_meta->default_lookup_index == NULL) {
				debug_print("%s\n", "Memory allocation failed");
				return MOBI_MALLOC_FAILED;
			}
            opf->metadata->x_meta->default_lookup_index[0] = strdup(rawml->orth->orth_index_name);
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of xml elements of given name to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] name XML element name
 @param[in] content Array of XML element contents
 @param[in] ns XML namespace string or NULL if empty
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_element_ns(xmlTextWriterPtr writer, const char *name, const char **content, const char *ns) {
    if (content) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (content[i] == NULL) {
                break;
            }
            xmlChar *namespace = NULL;
            if (ns) {
                namespace = BAD_CAST ns;
            }
            int xml_ret = xmlTextWriterWriteElementNS(writer, namespace, BAD_CAST name, NULL, BAD_CAST content[i]);
            if (xml_ret < 0) {
                debug_print("XML error: %i (name: %s, content: %s)\n", xml_ret, name, content[i]);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of Dublin Core elements of given name to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] name XML element name
 @param[in] content Array of XML element contents
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_dcmeta(xmlTextWriterPtr writer, const char *name, const char **content) {
    return mobi_xml_write_element_ns(writer, name, content, "dc");
}

/**
 @brief Write array of custom MOBI elements of given name to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] name XML element name
 @param[in] content Array of XML element contents
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_xmeta(xmlTextWriterPtr writer, const char *name, const char **content) {
    return mobi_xml_write_element_ns(writer, name, content, NULL);
}

/**
 @brief Write array of <meta/> elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElement() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] meta Array of OPFmeta structures
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_opfmeta(xmlTextWriterPtr writer, const OPFmeta **meta) {
    if (meta) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (meta[i] == NULL) {
                break;
            }
            MOBI_RET ret = mobi_xml_write_meta(writer, meta[i]->name, meta[i]->content);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of <referenece/> elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElement() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] reference Array of OPFreference structures
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_reference(xmlTextWriterPtr writer, const OPFreference **reference) {
    if (reference) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (reference[i] == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "reference");
            if (xml_ret < 0) {
                debug_print("XML error: %i (reference type: %s)\n", xml_ret, reference[i]->type);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "type", BAD_CAST reference[i]->type);
            if (xml_ret < 0) {
                debug_print("XML error: %i (reference type: %s)\n", xml_ret, reference[i]->type);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "title", BAD_CAST reference[i]->title);
            if (xml_ret < 0) {
                debug_print("XML error: %i (reference type: %s)\n", xml_ret, reference[i]->type);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "href", BAD_CAST reference[i]->href);
            if (xml_ret < 0) {
                debug_print("XML error: %i (reference type: %s)\n", xml_ret, reference[i]->type);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (reference type: %s)\n", xml_ret, reference[i]->type);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write single <item/> element to XML buffer
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] id Attribute "id"
 @param[in] href Attribute "href"
 @param[in] media_type Attribute "media-type"
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_item(xmlTextWriterPtr writer, const char *id, const char *href, const char *media_type) {
    int xml_ret;
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "item");
    if (xml_ret < 0) {
        debug_print("XML error: %i (item id: %s)\n", xml_ret, id);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "id", BAD_CAST id);
    if (xml_ret < 0) {
        debug_print("XML error: %i (item id: %s)\n", xml_ret, id);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "href", BAD_CAST href);
    if (xml_ret < 0) {
        debug_print("XML error: %i (item id: %s)\n", xml_ret, id);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "media-type", BAD_CAST media_type);
    if (xml_ret < 0) {
        debug_print("XML error: %i (item id: %s)\n", xml_ret, id);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) {
        debug_print("XML error: %i (item id: %s)\n", xml_ret, id);
        return MOBI_XML_ERR;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write opf <spine/> part to XML buffer
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] rawml MOBIRawml structure containing parts metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_spine(xmlTextWriterPtr writer, const MOBIRawml *rawml) {
    if (!rawml || !rawml->resources || !rawml->markup || !writer) {
        return MOBI_INIT_FAILED;
    }
    /* get toc id */
    char ncxid[13 + 1];
    MOBIPart *curr = rawml->resources;
    while (curr != NULL && curr->type != T_NCX) {
        curr = curr->next;
    }
    if (curr) {
        snprintf(ncxid, sizeof(ncxid), "resource%05zu", curr->uid);
    } else {
        return MOBI_DATA_CORRUPT;
    }
    int xml_ret;
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "spine");
    if (xml_ret < 0) {
        debug_print("XML error: %i (spine)\n", xml_ret);
        return MOBI_XML_ERR;
    }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "toc", BAD_CAST ncxid);
    if (xml_ret < 0) {
        debug_print("XML error: %i (spine toc: %s)\n", xml_ret, ncxid);
        return MOBI_XML_ERR;
    }
    char id[9 + 1];
    curr = rawml->markup;
    while (curr != NULL) {
        snprintf(id, sizeof(id), "part%05zu", curr->uid);
        xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "itemref");
        if (xml_ret < 0) {
            debug_print("XML error: %i (itemref)\n", xml_ret);
            return MOBI_XML_ERR;
        }
        xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "idref", BAD_CAST id);
        if (xml_ret < 0) {
            debug_print("XML error: %i (idref: %s)\n", xml_ret, id);
            return MOBI_XML_ERR;
        }
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) {
            debug_print("XML error: %i (idref: %s)\n", xml_ret, id);
            return MOBI_XML_ERR;
        }
        curr = curr->next;
    }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) {
        debug_print("XML error: %i (spine)\n", xml_ret);
        return MOBI_XML_ERR;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write all manifest <item/> elements to XML buffer
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] rawml MOBIRawml structure containing parts metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_manifest(xmlTextWriterPtr writer, const MOBIRawml *rawml) {
    char href[256];
    char id[256];
    if (rawml->flow != NULL) {
        MOBIPart *curr = rawml->flow;
        /* skip first raw html part */
        curr = curr->next;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(href, sizeof(href), "flow%05zu.%s", curr->uid, file_meta.extension);
            snprintf(id, sizeof(id), "flow%05zu", curr->uid);
            MOBI_RET ret = mobi_xml_write_item(writer, id, href, file_meta.mime_type);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
            curr = curr->next;
        }
    }
    if (rawml->markup != NULL) {
        MOBIPart *curr = rawml->markup;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(href, sizeof(href), "part%05zu.%s", curr->uid, file_meta.extension);
            snprintf(id, sizeof(id), "part%05zu", curr->uid);
            MOBI_RET ret = mobi_xml_write_item(writer, id, href, file_meta.mime_type);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
            curr = curr->next;
        }
    }
    if (rawml->resources != NULL) {
        MOBIPart *curr = rawml->resources;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(href, sizeof(href), "resource%05zu.%s", curr->uid, file_meta.extension);
            snprintf(id, sizeof(id), "resource%05zu", curr->uid);
            MOBI_RET ret = mobi_xml_write_item(writer, id, href, file_meta.mime_type);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
            curr = curr->next;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of Dublin Core identifier elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] identifier OPFidentifier structure representing identifier element
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_dcmeta_identifier(xmlTextWriterPtr writer, const OPFidentifier **identifier) {
    if (identifier) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (identifier[i] == NULL || identifier[i]->value == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElementNS(writer, BAD_CAST "dc", BAD_CAST "identifier", NULL);
            if (xml_ret < 0) {
                debug_print("XML error: %i (identifier value: %s)\n", xml_ret, identifier[i]->value);
                return MOBI_XML_ERR;
            }
            if (identifier[i]->id) {
                xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "id", BAD_CAST identifier[i]->id);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (identifier id: %s)\n", xml_ret, identifier[i]->id);
                    return MOBI_XML_ERR;
                }
            }
            if (identifier[i]->scheme) {
                xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "opf", BAD_CAST "scheme", NULL, BAD_CAST identifier[i]->scheme);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (identifier value: %s)\n", xml_ret, identifier[i]->value);
                    return MOBI_XML_ERR;
                }
            }
            xml_ret = xmlTextWriterWriteString(writer, BAD_CAST identifier[i]->value);
            if (xml_ret < 0) {
                debug_print("XML error: %i (identifier value: %s)\n", xml_ret, identifier[i]->value);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (identifier value: %s)\n", xml_ret, identifier[i]->value);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of Dublin Core creator/contributor elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] creator OPFcreator structure representing creator/contributor element
 @param[in] name OPF creator value
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_dcmeta_creator(xmlTextWriterPtr writer, const OPFcreator **creator, const char *name) {
    if (creator) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (creator[i] == NULL || creator[i]->value == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElementNS(writer, BAD_CAST "dc", BAD_CAST name, NULL);
            if (xml_ret < 0) {
                debug_print("XML error: %i (creator value: %s)\n", xml_ret, creator[i]->value);
                return MOBI_XML_ERR;
            }
            if (creator[i]->role) {
                xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "opf", BAD_CAST "role", NULL, BAD_CAST creator[i]->role);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (creator role: %s)\n", xml_ret, creator[i]->role);
                    return MOBI_XML_ERR;
                }
            }
            if (creator[i]->file_as) {
                xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "opf", BAD_CAST "file-as", NULL, BAD_CAST creator[i]->file_as);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (creator file-as: %s)\n", xml_ret, creator[i]->file_as);
                    return MOBI_XML_ERR;
                }
            }
            xml_ret = xmlTextWriterWriteString(writer, BAD_CAST creator[i]->value);
            if (xml_ret < 0) {
                debug_print("XML error: %i (creator value: %s)\n", xml_ret, creator[i]->value);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (creator value: %s)\n", xml_ret, creator[i]->value);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of Dublin Core subject elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] subject OPFsubject structure representing subject element
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_dcmeta_subject(xmlTextWriterPtr writer, const OPFsubject **subject) {
    if (subject) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (subject[i] == NULL || subject[i]->value == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElementNS(writer, BAD_CAST "dc", BAD_CAST "subject", NULL);
            if (xml_ret < 0) {
                debug_print("XML error: %i (subject value: %s)\n", xml_ret, subject[i]->value);
                return MOBI_XML_ERR;
            }
            if (subject[i]->basic_code) {
                xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "BASICCode", BAD_CAST subject[i]->basic_code);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (subject BASICCode: %s)\n", xml_ret, subject[i]->basic_code);
                    return MOBI_XML_ERR;
                }
            }
            xml_ret = xmlTextWriterWriteString(writer, BAD_CAST subject[i]->value);
            if (xml_ret < 0) {
                debug_print("XML error: %i (subject value: %s)\n", xml_ret, subject[i]->value);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (subject value: %s)\n", xml_ret, subject[i]->value);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of Dublin Core date elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] date OPFdate structure representing date element
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_dcmeta_date(xmlTextWriterPtr writer, const OPFdate **date) {
    if (date) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (date[i] == NULL || date[i]->value == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElementNS(writer, BAD_CAST "dc", BAD_CAST "date", NULL);
            if (xml_ret < 0) {
                debug_print("XML error: %i (date value: %s)\n", xml_ret, date[i]->value);
                return MOBI_XML_ERR;
            }
            if (date[i]->event) {
                xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "opf", BAD_CAST "event", NULL, BAD_CAST date[i]->event);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (date event: %s)\n", xml_ret, date[i]->event);
                    return MOBI_XML_ERR;
                }
            }
            xml_ret = xmlTextWriterWriteString(writer, BAD_CAST date[i]->value);
            if (xml_ret < 0) {
                debug_print("XML error: %i (date value: %s)\n", xml_ret, date[i]->value);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (date value: %s)\n", xml_ret, date[i]->value);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write array of custom srp elements to XML buffer
 
 Wrapper for libxml2 xmlTextWriterWriteElementNS() function.
 Writes xml element for each not-null entry in the input array.
 
 @param[in,out] writer xmlTextWriterPtr to write to
 @param[in] srp OPFsrp structure representing srp element
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_xml_write_xmeta_srp(xmlTextWriterPtr writer, const OPFsrp **srp) {
    if (srp) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (srp[i] == NULL || srp[i]->value == NULL) {
                break;
            }
            int xml_ret;
            xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "srp");
            if (xml_ret < 0) {
                debug_print("XML error: %i (srp value: %s)\n", xml_ret, srp[i]->value);
                return MOBI_XML_ERR;
            }
            if (srp[i]->currency) {
                xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "currency", BAD_CAST srp[i]->currency);
                if (xml_ret < 0) {
                    debug_print("XML error: %i (srp currency: %s)\n", xml_ret, srp[i]->currency);
                    return MOBI_XML_ERR;
                }
            }
            xml_ret = xmlTextWriterWriteString(writer, BAD_CAST srp[i]->value);
            if (xml_ret < 0) {
                debug_print("XML error: %i (srp value: %s)\n", xml_ret, srp[i]->value);
                return MOBI_XML_ERR;
            }
            xml_ret = xmlTextWriterEndElement(writer);
            if (xml_ret < 0) {
                debug_print("XML error: %i (srp value: %s)\n", xml_ret, srp[i]->value);
                return MOBI_XML_ERR;
            }
            i++;
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Free array of OPF sturcture members
 
 @param[in] array Array
 */
void mobi_free_opf_array(char **array) {
    if (array) {
        size_t i = 0;
        while (i < OPF_META_MAX_TAGS) {
            if (array[i] == NULL) {
                break;
            }
            free(array[i]);
            i++;
        }
        free(array);
    }
}

/**
 @brief Macro to free generic OPF structure with two members
 
 @param[in] struct_array Structure name
 @param[in] struct_member1 Structure member 1
 @param[in] struct_member2 Structure member 2
 */
#define mobi_free_opf_struct_2el(struct_array, struct_member1, struct_member2) { \
    if (struct_array) { \
        size_t i = 0; \
        while (i < OPF_META_MAX_TAGS) { \
            if (struct_array[i] == NULL) { \
                break; \
            } \
            free(struct_array[i]->struct_member1); \
            free(struct_array[i]->struct_member2); \
            free(struct_array[i]); \
            i++; \
        } \
        free(struct_array); \
    } \
}

/**
 @brief Macro to free generic OPF structure with three members
 
 @param[in] struct_array Structure name
 @param[in] struct_member1 Structure member 1
 @param[in] struct_member2 Structure member 2
 @param[in] struct_member3 Structure member 3
 */
#define mobi_free_opf_struct_3el(struct_array, struct_member1, struct_member2, struct_member3) { \
    if (struct_array) { \
        size_t i = 0; \
        while (i < OPF_META_MAX_TAGS) { \
            if (struct_array[i] == NULL) { \
                break; \
            } \
            free(struct_array[i]->struct_member1); \
            free(struct_array[i]->struct_member2); \
            free(struct_array[i]->struct_member3); \
            free(struct_array[i]); \
            i++; \
        } \
        free(struct_array); \
    } \
}

/**
 @brief Free OPF metadata structure and data
 
 @param[in] metadata OPF opf->metadata structure
 */
void mobi_free_opf_metadata(OPFmetadata *metadata) {
    if (metadata) {
        /* <meta/> */
        mobi_free_opf_struct_2el(metadata->meta, name, content);
        /* <dc-metadata/> */
        mobi_free_opf_struct_3el(metadata->dc_meta->contributor, value, file_as, role);
        mobi_free_opf_struct_3el(metadata->dc_meta->creator, value, file_as, role);
        mobi_free_opf_struct_3el(metadata->dc_meta->identifier, value, id, scheme);
        mobi_free_opf_struct_2el(metadata->dc_meta->subject, value, basic_code);
        mobi_free_opf_struct_2el(metadata->dc_meta->date, value, event);
        mobi_free_opf_array(metadata->dc_meta->description);
        mobi_free_opf_array(metadata->dc_meta->language);
        mobi_free_opf_array(metadata->dc_meta->publisher);
        mobi_free_opf_array(metadata->dc_meta->rights);
        mobi_free_opf_array(metadata->dc_meta->source);
        mobi_free_opf_array(metadata->dc_meta->title);
        mobi_free_opf_array(metadata->dc_meta->type);
        free(metadata->dc_meta);
        /* <x-metadata/> */
        mobi_free_opf_struct_2el(metadata->x_meta->srp, value, currency);
        mobi_free_opf_array(metadata->x_meta->adult);
        mobi_free_opf_array(metadata->x_meta->default_lookup_index);
        mobi_free_opf_array(metadata->x_meta->dict_short_name);
        mobi_free_opf_array(metadata->x_meta->dictionary_in_lang);
        mobi_free_opf_array(metadata->x_meta->dictionary_out_lang);
        mobi_free_opf_array(metadata->x_meta->embedded_cover);
        mobi_free_opf_array(metadata->x_meta->imprint);
        mobi_free_opf_array(metadata->x_meta->review);
        free(metadata->x_meta);
        free(metadata);
    }
}

/**
 @brief Free OPFmanifest structure and data
 
 @param[in] manifest OPF opf->manifest structure
 */
void mobi_free_opf_manifest(OPFmanifest *manifest) {
    if (manifest) {
        mobi_free_opf_struct_3el(manifest->item, id, href, media_type);
        free(manifest);
    }
}

/**
 @brief Free OPFspine structure and data
 
 @param[in] spine OPF opf->spine structure
 */
void mobi_free_opf_spine(OPFspine *spine) {
    if (spine) {
        mobi_free_opf_array(spine->itemref);
        free(spine->toc);
        free(spine);
    }
}

/**
 @brief Free OPFguide structure and data
 
 @param[in] guide OPF opf->guide structure
 */
void mobi_free_opf_guide(OPFguide *guide) {
    if (guide) {
        mobi_free_opf_struct_3el(guide->reference, type, title, href);
        free(guide);
    }
}

/**
 @brief Free OPF structure and data
 
 @param[in] opf OPF structure
 */
void mobi_free_opf(OPF *opf) {
    mobi_free_opf_metadata(opf->metadata);
    mobi_free_opf_manifest(opf->manifest);
    mobi_free_opf_spine(opf->spine);
    mobi_free_opf_guide(opf->guide);
}

/**
 @brief Recreate OPF structure
 
 This function will fill OPF structure with parsed index data and convert it to xml file. The file will be stored in MOBIRawml structure.
 
 @param[in,out] rawml OPF xml file will be appended to rawml->markup linked list
 @param[in] m MOBIData structure containing document metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_build_opf(MOBIRawml *rawml, const MOBIData *m) {
    debug_print("Reconstructing opf%s", "\n");
    /* initialize libXML2 */
    LIBXML_TEST_VERSION
    /* initialize OPF structure */
    OPF opf = {
        .metadata = NULL,
        .manifest = NULL,
        .guide = NULL,
        .spine = NULL
    };
    MOBI_RET ret = mobi_build_opf_metadata(&opf, m, rawml);
    if (ret != MOBI_SUCCESS) {
        mobi_free_opf(&opf);
        return ret;
    }
    mobi_build_ncx(rawml, &opf);
    if (rawml->guide) {
        ret = mobi_build_opf_guide(&opf, rawml);
        if (ret != MOBI_SUCCESS) {
            mobi_free_opf(&opf);
            return ret;
        }
    }

    /* build OPF xml document */
    int xml_ret;
    const xmlChar * OPFNamespace = BAD_CAST "http://www.idpf.org/2007/opf";
    const xmlChar * DCNamespace = BAD_CAST "http://purl.org/dc/elements/1.1/";
    xmlBufferPtr buf = xmlBufferCreate();
    if (buf == NULL) {
        mobi_free_opf(&opf);
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    xmlTextWriterPtr writer = xmlNewTextWriterMemory(buf, 0);
    if (writer == NULL) {
        xmlBufferFree(buf);
        mobi_free_opf(&opf);
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    xmlTextWriterSetIndent(writer, 1);
    xml_ret = xmlTextWriterStartDocument(writer, NULL, NULL, NULL);
    if (xml_ret < 0) { goto cleanup; }
    /* <package/> */
    xml_ret = xmlTextWriterStartElementNS(writer, NULL, BAD_CAST "package", OPFNamespace);
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "version", BAD_CAST "2.0");
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttribute(writer, BAD_CAST "unique-identifier", BAD_CAST "uid");
    if (xml_ret < 0) { goto cleanup; }
    /* <metadata /> */
    xml_ret = xmlTextWriterStartElementNS(writer, NULL, BAD_CAST "metadata", NULL);
    if (xml_ret < 0) { goto cleanup; }
    /* <dc-metadata/> */
    //xml_ret = xmlTextWriterStartElementNS(writer, NULL, BAD_CAST "dc-metadata", NULL);
    //if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "xmlns", BAD_CAST "opf", NULL, OPFNamespace);
    if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterWriteAttributeNS(writer, BAD_CAST "xmlns", BAD_CAST "dc", NULL, DCNamespace);
    if (xml_ret < 0) { goto cleanup; }
    /* Dublin Core elements */
    OPFdcmeta *dc_meta = opf.metadata->dc_meta;
    ret = mobi_xml_write_dcmeta(writer, "title", (const char **) dc_meta->title);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "description", (const char **) dc_meta->description);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "language", (const char **) dc_meta->language);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "publisher", (const char **) dc_meta->publisher);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "rights", (const char **) dc_meta->rights);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "source", (const char **) dc_meta->source);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta(writer, "type", (const char **) dc_meta->type);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta_identifier(writer, (const OPFidentifier **) dc_meta->identifier);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta_creator(writer, (const OPFcreator **) dc_meta->creator, "creator");
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta_creator(writer, (const OPFcreator **) dc_meta->contributor, "contributor");
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta_subject(writer, (const OPFsubject **) dc_meta->subject);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_dcmeta_date(writer, (const OPFdate **) dc_meta->date);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    //xml_ret = xmlTextWriterEndElement(writer);
    //if (xml_ret < 0) { goto cleanup; }
    /* <x-metadata/> */
    //xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "x-metadata");
    //if (xml_ret < 0) { goto cleanup; }
    OPFxmeta *x_meta = opf.metadata->x_meta;
    /* custom elements */
    ret = mobi_xml_write_xmeta_srp(writer, (const OPFsrp **) x_meta->srp);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "adult", (const char **) x_meta->adult);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "DefaultLookupIndex", (const char **) x_meta->default_lookup_index);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "DictionaryVeryShortName", (const char **) x_meta->dict_short_name);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "DictionaryInLanguage", (const char **) x_meta->dictionary_in_lang);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "DictionaryOutLanguage", (const char **) x_meta->dictionary_out_lang);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "EmbeddedCover", (const char **) x_meta->embedded_cover);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "imprint", (const char **) x_meta->imprint);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    ret = mobi_xml_write_xmeta(writer, "review", (const char **) x_meta->review);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    /* <meta/> */
    ret = mobi_xml_write_opfmeta(writer, (const OPFmeta **) opf.metadata->meta);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    //xml_ret = xmlTextWriterEndElement(writer);
    //if (xml_ret < 0) { goto cleanup; }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) { goto cleanup; }
    /* <manifest/> */
    xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "manifest");
    if (xml_ret < 0) { goto cleanup; }
    ret = mobi_xml_write_manifest(writer, rawml);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    xml_ret = xmlTextWriterEndElement(writer);
    if (xml_ret < 0) { goto cleanup; }
    /* <spine/> */
    ret = mobi_xml_write_spine(writer, rawml);
    if (ret != MOBI_SUCCESS) { goto cleanup; }
    /* <guide/> */
    if (opf.guide) {
        xml_ret = xmlTextWriterStartElement(writer, BAD_CAST "guide");
        if (xml_ret < 0) { goto cleanup; }
        ret = mobi_xml_write_reference(writer, (const OPFreference **) opf.guide->reference);
        if (ret != MOBI_SUCCESS) { goto cleanup; }
        xml_ret = xmlTextWriterEndElement(writer);
        if (xml_ret < 0) { goto cleanup; }
    }
    xml_ret = xmlTextWriterEndDocument(writer);
    if (xml_ret < 0) { goto cleanup; }
    
    xmlFreeTextWriter(writer);
    const char *opf_xml = (const char *) buf->content;
    mobi_opf_add_to_rawml(opf_xml, rawml);
    xmlBufferFree(buf);
    mobi_free_opf(&opf);
    /* cleanup function for the XML library */
    xmlCleanupParser();
    return MOBI_SUCCESS;
    
cleanup:
    xmlFreeTextWriter(writer);
    xmlBufferFree(buf);
    mobi_free_opf(&opf);
    xmlCleanupParser();
    debug_print("%s\n", "XML writing failed");
    return MOBI_XML_ERR;
}
