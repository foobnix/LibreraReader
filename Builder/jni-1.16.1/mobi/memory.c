/** @file memory.c
 *  @brief Functions for initializing and releasing structures and data containers
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <stdlib.h>
#include "memory.h"
#include "debug.h"
#include "util.h"

/**
 @brief Initializer for MOBIData structure
 
 It allocates memory for structure.
 Memory should be freed with mobi_free().
 
 @return MOBIData on success, NULL otherwise
 */
MOBIData * mobi_init(void) {
    MOBIData *m = NULL;
    m = calloc(1, sizeof(MOBIData));
	if (m == NULL) return NULL;
    m->use_kf8 = true;
    m->kf8_boundary_offset = MOBI_NOTSET;
    m->drm_key = NULL;
    m->ph = NULL;
    m->rh = NULL;
    m->mh = NULL;
    m->eh = NULL;
    m->rec = NULL;
    m->next = NULL;
    return m;
}

/**
 @brief Free MOBIMobiHeader structure
 
 @param[in] mh MOBIMobiHeader structure
 */
void mobi_free_mh(MOBIMobiHeader *mh) {
    if (mh == NULL) {
        return;
    }
    free(mh->header_length);
    free(mh->mobi_type);
    free(mh->text_encoding);
    free(mh->uid);
    free(mh->version);
    free(mh->orth_index);
    free(mh->infl_index);
    free(mh->names_index);
    free(mh->keys_index);
    free(mh->extra0_index);
    free(mh->extra1_index);
    free(mh->extra2_index);
    free(mh->extra3_index);
    free(mh->extra4_index);
    free(mh->extra5_index);
    free(mh->non_text_index);
    free(mh->full_name_offset);
    free(mh->full_name_length);
    free(mh->locale);
    free(mh->dict_input_lang);
    free(mh->dict_output_lang);
    free(mh->min_version);
    free(mh->image_index);
    free(mh->huff_rec_index);
    free(mh->huff_rec_count);
    free(mh->datp_rec_index);
    free(mh->datp_rec_count);
    free(mh->exth_flags);
    free(mh->unknown6);
    free(mh->drm_offset);
    free(mh->drm_count);
    free(mh->drm_size);
    free(mh->drm_flags);
    free(mh->fdst_index);
    free(mh->first_text_index);
    free(mh->last_text_index);
    free(mh->fdst_section_count);
    //free(mh->unknown9);
    free(mh->fcis_index);
    free(mh->fcis_count);
    free(mh->flis_index);
    free(mh->flis_count);
    free(mh->unknown10);
    free(mh->unknown11);
    free(mh->srcs_index);
    free(mh->srcs_count);
    free(mh->unknown12);
    free(mh->unknown13);
    free(mh->extra_flags);
    free(mh->ncx_index);
    free(mh->fragment_index);
    free(mh->skeleton_index);
    free(mh->unknown14);
    free(mh->unknown15);
    free(mh->datp_index);
    free(mh->guide_index);
    free(mh->unknown16);
    free(mh->unknown17);
    free(mh->unknown18);
    free(mh->unknown19);
    free(mh->unknown20);
    free(mh->full_name);
    free(mh);
    mh = NULL;
}

/**
 @brief Free all MOBIPdbRecord structures and its respective data attached to MOBIData structure
 
 Each MOBIPdbRecord structure holds metadata and data for each pdb record
 
 @param[in,out] m MOBIData structure
 */
void mobi_free_rec(MOBIData *m) {
    MOBIPdbRecord *curr, *tmp;
    curr = m->rec;
    while (curr != NULL) {
        tmp = curr;
        curr = curr->next;
        free(tmp->data);
        free(tmp);
        tmp = NULL;
    }
    m->rec = NULL;
}

/**
 @brief Free all MOBIExthHeader structures and its respective data attached to MOBIData structure
 
 Each MOBIExthHeader structure holds metadata and data for each EXTH record
 
 @param[in,out] m MOBIData structure
 */
void mobi_free_eh(MOBIData *m) {
    MOBIExthHeader *curr, *tmp;
    curr = m->eh;
    while (curr != NULL) {
        tmp = curr;
        curr = curr->next;
        free(tmp->data);
        free(tmp);
        tmp = NULL;
    }
    m->eh = NULL;
}

/**
 @brief Free MOBIData structure and all its children
 
 @param[in] m MOBIData structure
 */
void mobi_free(MOBIData *m) {
    if (m == NULL) {
        return;
    }
    mobi_free_mh(m->mh);
    mobi_free_eh(m);
    mobi_free_rec(m);
    free(m->ph);
    free(m->rh);
    if (m->next) {
        mobi_free_mh(m->next->mh);
        mobi_free_eh(m->next);
        free(m->next->rh);
        free(m->next);
        m->next = NULL;
    }
    if (m->drm_key) {
        free(m->drm_key);
    }
    free(m);
    m = NULL;
}

/**
 @brief Initialize and return MOBIHuffCdic structure.
 
 MOBIHuffCdic structure holds parsed data from HUFF, CDIC records.
 It is used for huffman decompression.
 Initialized structure is a child of MOBIData structure.
 It must be freed with mobi_free_huffcdic().
 
 @return MOBIHuffCdic on success, NULL otherwise
 */
MOBIHuffCdic * mobi_init_huffcdic(void) {
    MOBIHuffCdic *huffcdic = calloc(1, sizeof(MOBIHuffCdic));
    if (huffcdic == NULL) {
        debug_print("%s", "Memory allocation for huffcdic structure failed\n");
        return NULL;
    }
    return huffcdic;
}

/**
 @brief Free MOBIHuffCdic structure and all its children
 
 @param[in] huffcdic MOBIData structure
 */
void mobi_free_huffcdic(MOBIHuffCdic *huffcdic) {
    if (huffcdic == NULL) {
        return;
    }
    free(huffcdic->symbol_offsets);
    free(huffcdic->symbols);
    free(huffcdic);
    huffcdic = NULL;
}

/**
 @brief Initialize and return MOBIRawml structure.
 
 MOBIRawml structure holds parsed text record metadata.
 It is used in the process of parsing rawml text data.
 It must be freed with mobi_free_rawml().
 
 @param[in] m Initialized MOBIData structure
 @return MOBIRawml on success, NULL otherwise
 */
MOBIRawml * mobi_init_rawml(const MOBIData *m) {
    MOBIRawml *rawml = malloc(sizeof(MOBIRawml));
    if (rawml == NULL) {
        debug_print("%s", "Memory allocation failed for rawml structure\n");
        return NULL;
    }
    rawml->version = mobi_get_fileversion(m);
    rawml->fdst = NULL;
    rawml->skel = NULL;
    rawml->frag = NULL;
    rawml->guide = NULL;
    rawml->ncx = NULL;
    rawml->orth = NULL;
    rawml->infl = NULL;
    rawml->flow = NULL;
    rawml->markup = NULL;
    rawml->resources = NULL;
    return rawml;
}

/**
 @brief Free MOBIFdst structure and all its children
 
 @param[in] fdst MOBIFdst structure
 */
void mobi_free_fdst(MOBIFdst *fdst) {
    if (fdst == NULL) {
        return;
    }
    if (fdst->fdst_section_count > 0) {
        free(fdst->fdst_section_starts);
        free(fdst->fdst_section_ends);
    }
    free(fdst);
    fdst = NULL;
}

/**
 @brief Initialize and return MOBIIndx structure.
 
 MOBIIndx structure holds INDX index record entries.
 Must be freed with mobi_free_indx()
 
 @return MOBIIndx on success, NULL otherwise
 */
MOBIIndx * mobi_init_indx(void) {
    MOBIIndx *indx = calloc(1, sizeof(MOBIIndx));
    if (indx == NULL) {
        debug_print("%s", "Memory allocation failed for indx structure\n");
        return NULL;
    }
    indx->entries = NULL;
    indx->cncx_record = NULL;
    indx->orth_index_name = NULL;
    return indx;
}

/**
 @brief Free index entries data and all its children
 
 @param[in] indx MOBIIndx structure that holds indx->entries
 */
void mobi_free_index_entries(MOBIIndx *indx) {
    if (indx == NULL || indx->entries == NULL) {
        return;
    }
    size_t i = 0;
    while (i < indx->entries_count) {
        free(indx->entries[i].label);
        if (indx->entries[i].tags != NULL) {
            size_t j = 0;
            while (j < indx->entries[i].tags_count) {
                free(indx->entries[i].tags[j++].tagvalues);
            }
            free(indx->entries[i].tags);
        }
        i++;
    }
    free(indx->entries);
    indx->entries = NULL;
}

/**
 @brief Free MOBIIndx structure and all its children
 
 @param[in] indx MOBIIndx structure that holds indx->entries
 */
void mobi_free_indx(MOBIIndx *indx) {
    if (indx == NULL) {
        return;
    }
    mobi_free_index_entries(indx);
    if (indx->orth_index_name) {
        free(indx->orth_index_name);
    }
    free(indx);
    indx = NULL;
}

/**
 @brief Free MOBITagx structure and all its children
 
 @param[in] tagx MOBITagx structure
 */
void mobi_free_tagx(MOBITagx *tagx) {
    if (tagx == NULL) {
        return;
    }
    free(tagx->tags);
    free(tagx);
    tagx = NULL;
}

/**
 @brief Free MOBIOrdt structure and all its children
 
 @param[in] ordt MOBIOrdt structure
 */
void mobi_free_ordt(MOBIOrdt *ordt) {
    if (ordt == NULL) {
        return;
    }
    free(ordt->ordt1);
    free(ordt->ordt2);
    free(ordt);
    ordt = NULL;
}

/**
 @brief Free MOBIPart structure
 
 Pointer to data may point to memory area also used by record->data. 
 So we need a flag to leave the memory allocated, while freeing MOBIPart structure
 
 @param[in] part MOBIPart structure
 @param[in] free_data Flag, if set - a pointer to part->data is also released, otherwise not released
 */
void mobi_free_part(MOBIPart *part, int free_data) {
    MOBIPart *curr, *tmp;
    curr = part;
    while (curr != NULL) {
        tmp = curr;
        curr = curr->next;
        if (free_data) { free(tmp->data); }
        free(tmp);
        tmp = NULL;
    }
    part = NULL;
}

/**
 @brief Free MOBIPart structure for opf and ncx data
 
 @param[in] part MOBIPart structure
 */
void mobi_free_opf_data(MOBIPart *part) {
    while (part != NULL) {
        if (part->type == T_NCX || part->type == T_OPF) {
            free(part->data);
        }
        part = part->next;
    }
}

/**
 @brief Free MOBIPart structure for decoded font data
 
 @param[in] part MOBIPart structure
 */
void mobi_free_font_data(MOBIPart *part) {
    while (part != NULL) {
        if (part->type == T_OTF || part->type == T_TTF) {
            free(part->data);
        }
        part = part->next;
    }
}

/**
 @brief Free MOBIRawml structure allocated by mobi_init_rawml()
 
 Pointer to data may point to memory area also used by record->data.
 So we need a flag to leave the memory allocated, while freeing MOBIPart structure
 
 @param[in] rawml MOBIRawml structure
 */
void mobi_free_rawml(MOBIRawml *rawml) {
    if (rawml == NULL) {
        return;
    }
    mobi_free_fdst(rawml->fdst);
    mobi_free_indx(rawml->skel);
    mobi_free_indx(rawml->frag);
    mobi_free_indx(rawml->guide);
    mobi_free_indx(rawml->ncx);
    mobi_free_indx(rawml->orth);
    mobi_free_indx(rawml->infl);
    mobi_free_part(rawml->flow, true);
    mobi_free_part(rawml->markup,true);
    /* do not free resources data, these are links to records data */
    /* only free opf and ncx data */
    mobi_free_opf_data(rawml->resources);
    /* and free decoded fonts data */
    mobi_free_font_data(rawml->resources);
    mobi_free_part(rawml->resources, false);
    free(rawml);
    rawml = NULL;
}


