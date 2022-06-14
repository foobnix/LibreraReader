/** @file read.c
 *  @brief Functions for reading and parsing of MOBI document
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "read.h"
#include "util.h"
#include "index.h"
#include "debug.h"

/**
 @brief Read palm database header from file into MOBIData structure (MOBIPdbHeader)
 
 @param[in,out] m MOBIData structure to be filled with read data
 @param[in] file Filedescriptor to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_pdbheader(MOBIData *m, FILE *file) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (!file) {
        return MOBI_FILE_NOT_FOUND;
    }
    MOBIBuffer *buf = mobi_buffer_init(PALMDB_HEADER_LEN);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    const size_t len = fread(buf->data, 1, PALMDB_HEADER_LEN, file);
    if (len != PALMDB_HEADER_LEN) {
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    m->ph = calloc(1, sizeof(MOBIPdbHeader));
    if (m->ph == NULL) {
        debug_print("%s", "Memory allocation for pdb header failed\n");
        mobi_buffer_free(buf);
        return MOBI_MALLOC_FAILED;
    }
    /* parse header */
    mobi_buffer_getstring(m->ph->name, buf, PALMDB_NAME_SIZE_MAX);
    m->ph->attributes = mobi_buffer_get16(buf);
    m->ph->version = mobi_buffer_get16(buf);
    m->ph->ctime = mobi_buffer_get32(buf);
    m->ph->mtime = mobi_buffer_get32(buf);
    m->ph->btime = mobi_buffer_get32(buf);
    m->ph->mod_num = mobi_buffer_get32(buf);
    m->ph->appinfo_offset = mobi_buffer_get32(buf);
    m->ph->sortinfo_offset = mobi_buffer_get32(buf);
    mobi_buffer_getstring(m->ph->type, buf, 4);
    mobi_buffer_getstring(m->ph->creator, buf, 4);
    m->ph->uid = mobi_buffer_get32(buf);
    m->ph->next_rec = mobi_buffer_get32(buf);
    m->ph->rec_count = mobi_buffer_get16(buf);
    mobi_buffer_free(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Read list of database records from file into MOBIData structure (MOBIPdbRecord)
 
 @param[in,out] m MOBIData structure to be filled with read data
 @param[in] file Filedescriptor to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_reclist(MOBIData *m, FILE *file) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (!file) {
        debug_print("%s", "File not ready\n");
        return MOBI_FILE_NOT_FOUND;
    }
    m->rec = calloc(1, sizeof(MOBIPdbRecord));
    if (m->rec == NULL) {
        debug_print("%s", "Memory allocation for pdb record failed\n");
        return MOBI_MALLOC_FAILED;
    }
    MOBIPdbRecord *curr = m->rec;
    for (int i = 0; i < m->ph->rec_count; i++) {
        MOBIBuffer *buf = mobi_buffer_init(PALMDB_RECORD_INFO_SIZE);
        if (buf == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            return MOBI_MALLOC_FAILED;
        }
        const size_t len = fread(buf->data, 1, PALMDB_RECORD_INFO_SIZE, file);
        if (len != PALMDB_RECORD_INFO_SIZE) {
            mobi_buffer_free(buf);
            return MOBI_DATA_CORRUPT;
        }
        if (i > 0) {
            curr->next = calloc(1, sizeof(MOBIPdbRecord));
            if (curr->next == NULL) {
                debug_print("%s", "Memory allocation for pdb record failed\n");
                mobi_buffer_free(buf);
                return MOBI_MALLOC_FAILED;
            }
            curr = curr->next;
        }
        curr->offset = mobi_buffer_get32(buf);
        curr->attributes = mobi_buffer_get8(buf);
        const uint8_t h = mobi_buffer_get8(buf);
        const uint16_t l = mobi_buffer_get16(buf);
        curr->uid =  (uint32_t) h << 16 | l;
        curr->next = NULL;
        mobi_buffer_free(buf);
    }
    return MOBI_SUCCESS;
}

/**
 @brief Read record data and size from file into MOBIData structure (MOBIPdbRecord)
 
 @param[in,out] m MOBIData structure to be filled with read data
 @param[in] file Filedescriptor to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_rec(MOBIData *m, FILE *file) {
    MOBI_RET ret;
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    MOBIPdbRecord *curr = m->rec;
    while (curr != NULL) {
        MOBIPdbRecord *next;
        size_t size;
        if (curr->next != NULL) {
            next = curr->next;
            size = next->offset - curr->offset;
        } else {
            fseek(file, 0, SEEK_END);
            long diff = ftell(file) - curr->offset;
            if (diff <= 0) {
                debug_print("Wrong record size: %li\n", diff);
                return MOBI_DATA_CORRUPT;
            }
            size = (size_t) diff;
            next = NULL;
        }

        curr->size = size;
        ret = mobi_load_recdata(curr, file);
        if (ret  != MOBI_SUCCESS) {
            debug_print("Error loading record uid %i data\n", curr->uid);
            mobi_free_rec(m);
            return ret;
        }
        curr = next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Read record data from file into MOBIPdbRecord structure
 
 @param[in,out] rec MOBIPdbRecord structure to be filled with read data
 @param[in] file Filedescriptor to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_recdata(MOBIPdbRecord *rec, FILE *file) {
    const int ret = fseek(file, rec->offset, SEEK_SET);
    if (ret != 0) {
        debug_print("Record %i not found\n", rec->uid);
        return MOBI_DATA_CORRUPT;
    }
    rec->data = malloc(rec->size);
    if (rec->data == NULL) {
        debug_print("%s", "Memory allocation for pdb record data failed\n");
        return MOBI_MALLOC_FAILED;
    }
    const size_t len = fread(rec->data, 1, rec->size, file);
    if (len < rec->size) {
        debug_print("Truncated data in record %i\n", rec->uid);
        return MOBI_DATA_CORRUPT;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Parse EXTH header from Record 0 into MOBIData structure (MOBIExthHeader)
 
 @param[in,out] m MOBIData structure to be filled with parsed data
 @param[in] buf MOBIBuffer buffer to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_extheader(MOBIData *m, MOBIBuffer *buf) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    char exth_magic[5];
    const size_t header_length = 12;
    mobi_buffer_getstring(exth_magic, buf, 4);
    const size_t exth_length = mobi_buffer_get32(buf) - header_length;
    const size_t rec_count = mobi_buffer_get32(buf);
    if (strncmp(exth_magic, EXTH_MAGIC, 4) != 0 ||
        exth_length + buf->offset > buf->maxlen ||
        rec_count == 0 || rec_count > MOBI_EXTH_MAXCNT) {
        debug_print("%s", "Sanity checks for EXTH header failed\n");
        return MOBI_DATA_CORRUPT;
    }
    const size_t saved_maxlen = buf->maxlen;
    buf->maxlen = exth_length + buf->offset;
    m->eh = calloc(1, sizeof(MOBIExthHeader));
    if (m->eh == NULL) {
        debug_print("%s", "Memory allocation for EXTH header failed\n");
        return MOBI_MALLOC_FAILED;
    }
    MOBIExthHeader *curr = m->eh;
    for (size_t i = 0; i < rec_count; i++) {
        if (curr->data) {
            curr->next = calloc(1, sizeof(MOBIExthHeader));
            if (curr->next == NULL) {
                debug_print("%s", "Memory allocation for EXTH header failed\n");
                mobi_free_eh(m);
                return MOBI_MALLOC_FAILED;
            }
            curr = curr->next;
        }
        curr->tag = mobi_buffer_get32(buf);
        /* data size = record size minus 8 bytes for uid and size */
        curr->size = mobi_buffer_get32(buf) - 8;
        if (curr->size == 0) {
            debug_print("Skip record %i, data too short\n", curr->tag);
            continue;
        }
        if (buf->offset + curr->size > buf->maxlen) {
            debug_print("Record %i too long\n", curr->tag);
            mobi_free_eh(m);
            return MOBI_DATA_CORRUPT;
        }
        curr->data = malloc(curr->size);
        if (curr->data == NULL) {
            debug_print("Memory allocation for EXTH record %i failed\n", curr->tag);
            mobi_free_eh(m);
            return MOBI_MALLOC_FAILED;
        }
        mobi_buffer_getraw(curr->data, buf, curr->size);
        curr->next = NULL;
    }    
    buf->maxlen = saved_maxlen;
    return MOBI_SUCCESS;
}

/**
 @brief Parse MOBI header from Record 0 into MOBIData structure (MOBIMobiHeader)
 
 @param[in,out] m MOBIData structure to be filled with parsed data
 @param[in] buf MOBIBuffer buffer to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_mobiheader(MOBIData *m, MOBIBuffer *buf) {
    int isKF8 = 0;
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    m->mh = calloc(1, sizeof(MOBIMobiHeader));
    if (m->mh == NULL) {
        debug_print("%s", "Memory allocation for MOBI header failed\n");
        return MOBI_MALLOC_FAILED;
    }
    mobi_buffer_getstring(m->mh->mobi_magic, buf, 4);
    mobi_buffer_dup32(&m->mh->header_length, buf);
    if (strcmp(m->mh->mobi_magic, MOBI_MAGIC) != 0 || m->mh->header_length == NULL) {
        debug_print("%s", "MOBI header not found\n");
        mobi_free_mh(m->mh);
        m->mh = NULL;
        return MOBI_DATA_CORRUPT;
    }
    const size_t saved_maxlen = buf->maxlen;
    /* some old files declare zero length mobi header, try to read first 24 bytes anyway */
    uint32_t header_length = (*m->mh->header_length > 0) ? *m->mh->header_length : 24;
    /* read only declared MOBI header length (curr offset minus 8 already read bytes) */
    const size_t left_length = header_length + buf->offset - 8;
    buf->maxlen = saved_maxlen < left_length ? saved_maxlen : left_length;
    mobi_buffer_dup32(&m->mh->mobi_type, buf);
    uint32_t encoding = mobi_buffer_get32(buf);
    if (encoding == 1252) {
        m->mh->text_encoding = malloc(sizeof(MOBIEncoding));
        if (m->mh->text_encoding == NULL) {
            debug_print("%s", "Memory allocation for MOBI header failed\n");
            return MOBI_MALLOC_FAILED;
        }
        *m->mh->text_encoding = MOBI_CP1252;
    }
    else if (encoding == 65001) {
        m->mh->text_encoding = malloc(sizeof(MOBIEncoding));
        if (m->mh->text_encoding == NULL) {
            debug_print("%s", "Memory allocation for MOBI header failed\n");
            return MOBI_MALLOC_FAILED;
        }
        *m->mh->text_encoding = MOBI_UTF8;
    } else {
        debug_print("Unknown encoding in mobi header: %i\n", encoding);
    }
    mobi_buffer_dup32(&m->mh->uid, buf);
    mobi_buffer_dup32(&m->mh->version, buf);
    if (header_length >= MOBI_HEADER_V7_SIZE
        && m->mh->version && *m->mh->version == 8) {
        isKF8 = 1;
    }
    mobi_buffer_dup32(&m->mh->orth_index, buf);
    mobi_buffer_dup32(&m->mh->infl_index, buf);
    mobi_buffer_dup32(&m->mh->names_index, buf);
    mobi_buffer_dup32(&m->mh->keys_index, buf);
    mobi_buffer_dup32(&m->mh->extra0_index, buf);
    mobi_buffer_dup32(&m->mh->extra1_index, buf);
    mobi_buffer_dup32(&m->mh->extra2_index, buf);
    mobi_buffer_dup32(&m->mh->extra3_index, buf);
    mobi_buffer_dup32(&m->mh->extra4_index, buf);
    mobi_buffer_dup32(&m->mh->extra5_index, buf);
    mobi_buffer_dup32(&m->mh->non_text_index, buf);
    mobi_buffer_dup32(&m->mh->full_name_offset, buf);
    mobi_buffer_dup32(&m->mh->full_name_length, buf);
    mobi_buffer_dup32(&m->mh->locale, buf);
    mobi_buffer_dup32(&m->mh->dict_input_lang, buf);
    mobi_buffer_dup32(&m->mh->dict_output_lang, buf);
    mobi_buffer_dup32(&m->mh->min_version, buf);
    mobi_buffer_dup32(&m->mh->image_index, buf);
    mobi_buffer_dup32(&m->mh->huff_rec_index, buf);
    mobi_buffer_dup32(&m->mh->huff_rec_count, buf);
    mobi_buffer_dup32(&m->mh->datp_rec_index, buf);
    mobi_buffer_dup32(&m->mh->datp_rec_count, buf);
    mobi_buffer_dup32(&m->mh->exth_flags, buf);
    mobi_buffer_seek(buf, 32); /* 32 unknown bytes */
    mobi_buffer_dup32(&m->mh->unknown6, buf);
    mobi_buffer_dup32(&m->mh->drm_offset, buf);
    mobi_buffer_dup32(&m->mh->drm_count, buf);
    mobi_buffer_dup32(&m->mh->drm_size, buf);
    mobi_buffer_dup32(&m->mh->drm_flags, buf);
    mobi_buffer_seek(buf, 8); /* 8 unknown bytes */
    if (isKF8) {
        mobi_buffer_dup32(&m->mh->fdst_index, buf);
    } else {
        mobi_buffer_dup16(&m->mh->first_text_index, buf);
        mobi_buffer_dup16(&m->mh->last_text_index, buf);
    }
    mobi_buffer_dup32(&m->mh->fdst_section_count, buf);
    mobi_buffer_dup32(&m->mh->fcis_index, buf);
    mobi_buffer_dup32(&m->mh->fcis_count, buf);
    mobi_buffer_dup32(&m->mh->flis_index, buf);
    mobi_buffer_dup32(&m->mh->flis_count, buf);
    mobi_buffer_dup32(&m->mh->unknown10, buf);
    mobi_buffer_dup32(&m->mh->unknown11, buf);
    mobi_buffer_dup32(&m->mh->srcs_index, buf);
    mobi_buffer_dup32(&m->mh->srcs_count, buf);
    mobi_buffer_dup32(&m->mh->unknown12, buf);
    mobi_buffer_dup32(&m->mh->unknown13, buf);
    mobi_buffer_seek(buf, 2); /* 2 byte fill */
    mobi_buffer_dup16(&m->mh->extra_flags, buf);
    mobi_buffer_dup32(&m->mh->ncx_index, buf);
    if (isKF8) {
        mobi_buffer_dup32(&m->mh->fragment_index, buf);
        mobi_buffer_dup32(&m->mh->skeleton_index, buf);
    } else {
        mobi_buffer_dup32(&m->mh->unknown14, buf);
        mobi_buffer_dup32(&m->mh->unknown15, buf);
    }
    mobi_buffer_dup32(&m->mh->datp_index, buf);
    if (isKF8) {
        mobi_buffer_dup32(&m->mh->guide_index, buf);
    } else {
        mobi_buffer_dup32(&m->mh->unknown16, buf);
    }
    mobi_buffer_dup32(&m->mh->unknown17, buf);
    mobi_buffer_dup32(&m->mh->unknown18, buf);
    mobi_buffer_dup32(&m->mh->unknown19, buf);
    mobi_buffer_dup32(&m->mh->unknown20, buf);
    if (buf->maxlen > buf->offset) {
        debug_print("Skipping %zu unknown bytes in MOBI header\n", (buf->maxlen - buf->offset));
        mobi_buffer_setpos(buf, buf->maxlen);
    }
    buf->maxlen = saved_maxlen;
    /* get full name stored at m->mh->full_name_offset */
    if (m->mh->full_name_offset && m->mh->full_name_length) {
        const size_t saved_offset = buf->offset;
        const uint32_t full_name_length = min(*m->mh->full_name_length, MOBI_TITLE_SIZEMAX);
        mobi_buffer_setpos(buf, *m->mh->full_name_offset);
        m->mh->full_name = malloc(full_name_length + 1);
        if (m->mh->full_name == NULL) {
            debug_print("%s", "Memory allocation for full name failed\n");
            return MOBI_MALLOC_FAILED;
        }
        if (full_name_length) {
            mobi_buffer_getstring(m->mh->full_name, buf, full_name_length);
        } else {
            m->mh->full_name[0] = '\0';
        }
        mobi_buffer_setpos(buf, saved_offset);
    }
    return MOBI_SUCCESS;
}

/**
 @brief Parse Record 0 into MOBIData structure
 
 This function will parse MOBIRecord0Header, MOBIMobiHeader and MOBIExthHeader
 
 @param[in,out] m MOBIData structure to be filled with parsed data
 @param[in] seqnumber Sequential number of the palm database record
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_record0(MOBIData *m, const size_t seqnumber) {
    MOBI_RET ret;
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    const MOBIPdbRecord *record0 = mobi_get_record_by_seqnumber(m, seqnumber);
    if (record0 == NULL) {
        debug_print("%s", "Record 0 not loaded\n");
        return MOBI_DATA_CORRUPT;
    }
    if (record0->size < RECORD0_HEADER_LEN) {
        debug_print("%s", "Record 0 too short\n");
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init_null(record0->data, record0->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    m->rh = calloc(1, sizeof(MOBIRecord0Header));
    if (m->rh == NULL) {
        debug_print("%s", "Memory allocation for record 0 header failed\n");
        mobi_buffer_free_null(buf);
        return MOBI_MALLOC_FAILED;
    }
    /* parse palmdoc header */
    const uint16_t compression = mobi_buffer_get16(buf);
    mobi_buffer_seek(buf, 2); // unused 2 bytes, zeroes
    if ((compression != MOBI_COMPRESSION_NONE &&
         compression != MOBI_COMPRESSION_PALMDOC &&
         compression != MOBI_COMPRESSION_HUFFCDIC)) {
        debug_print("Wrong record0 header: %c%c%c%c\n", record0->data[0], record0->data[1], record0->data[2], record0->data[3]);
        mobi_buffer_free_null(buf);
        free(m->rh);
        m->rh = NULL;
        return MOBI_DATA_CORRUPT;
    }
    m->rh->compression_type = compression;
    m->rh->text_length = mobi_buffer_get32(buf);
    m->rh->text_record_count = mobi_buffer_get16(buf);
    m->rh->text_record_size = mobi_buffer_get16(buf);
    m->rh->encryption_type = mobi_buffer_get16(buf);
    m->rh->unknown1 = mobi_buffer_get16(buf);
    if (mobi_is_mobipocket(m)) {
        /* parse mobi header if present  */
        ret = mobi_parse_mobiheader(m, buf);
        if (ret == MOBI_SUCCESS) {
            /* parse exth header if present */
            mobi_parse_extheader(m, buf);
        }
    } 
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Calculate the size of extra bytes at the end of text record
 
 @param[in] record MOBIPdbRecord structure containing the record
 @param[in] flags Flags from MOBI header (extra_flags)
 @return The size of trailing bytes, MOBI_NOTSET on failure
 */
size_t mobi_get_record_extrasize(const MOBIPdbRecord *record, const uint16_t flags) {
    size_t extra_size = 0;
    MOBIBuffer *buf = mobi_buffer_init_null(record->data, record->size);
    if (buf == NULL) {
        debug_print("%s", "Buffer init in extrasize failed\n");
        return MOBI_NOTSET;
    }
    /* set pointer at the end of the record data */
    mobi_buffer_setpos(buf, buf->maxlen - 1);
    for (int bit = 15; bit > 0; bit--) {
        if (flags & (1 << bit)) {
            /* bit is set */
            size_t len = 0;
            /* size contains varlen itself and optional data */
            const uint32_t size = mobi_buffer_get_varlen_dec(buf, &len);
            /* skip data */
            /* TODO: read and store in record struct */
            mobi_buffer_seek(buf, - (int)(size - len));
            extra_size += size;
        }
    }
    /* check bit 0 */
    if (flags & 1) {
            const uint8_t b = mobi_buffer_get8(buf);
            /* two first bits hold size */
            extra_size += (b & 0x3) + 1;
    }
    mobi_buffer_free_null(buf);
    return extra_size;
}

/**
 @brief Calculate the size of extra multibyte section at the end of text record
 
 @param[in] record MOBIPdbRecord structure containing the record
 @param[in] flags Flags from MOBI header (extra_flags)
 @return The size of trailing bytes, MOBI_NOTSET on failure
 */
size_t mobi_get_record_mb_extrasize(const MOBIPdbRecord *record, const uint16_t flags) {
    size_t extra_size = 0;
    if (flags & 1) {
        MOBIBuffer *buf = mobi_buffer_init_null(record->data, record->size);
        if (buf == NULL) {
            debug_print("%s", "Buffer init in extrasize failed\n");
            return MOBI_NOTSET;
        }
        /* set pointer at the end of the record data */
        mobi_buffer_setpos(buf, buf->maxlen - 1);
        for (int bit = 15; bit > 0; bit--) {
            if (flags & (1 << bit)) {
                /* bit is set */
                size_t len = 0;
                /* size contains varlen itself and optional data */
                const uint32_t size = mobi_buffer_get_varlen_dec(buf, &len);
                /* skip data */
                /* TODO: read and store in record struct */
                mobi_buffer_seek(buf, - (int)(size - len));
            }
        }
        /* read multibyte section */
        const uint8_t b = mobi_buffer_get8(buf);
        /* two first bits hold size */
        extra_size += (b & 0x3) + 1;
        mobi_buffer_free_null(buf);
    }
    return extra_size;
}

/**
 @brief Parse HUFF record into MOBIHuffCdic structure
 
 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
 @param[in] record MOBIPdbRecord structure containing the record
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_huff(MOBIHuffCdic *huffcdic, const MOBIPdbRecord *record) {
    MOBIBuffer *buf = mobi_buffer_init_null(record->data, record->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    char huff_magic[5];
    mobi_buffer_getstring(huff_magic, buf, 4);
    const size_t header_length = mobi_buffer_get32(buf);
    if (strncmp(huff_magic, HUFF_MAGIC, 4) != 0 || header_length < HUFF_HEADER_LEN) {
        debug_print("HUFF wrong magic: %s\n", huff_magic);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    const size_t data1_offset = mobi_buffer_get32(buf);
    const size_t data2_offset = mobi_buffer_get32(buf);
    /* skip little-endian table offsets */
    mobi_buffer_setpos(buf, data1_offset);
    if (buf->offset + (256 * 4) > buf->maxlen) {
        debug_print("%s", "HUFF data1 too short\n");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* read 256 indices from data1 big-endian */
    for (int i = 0; i < 256; i++) {
        huffcdic->table1[i] = mobi_buffer_get32(buf);
    }
    mobi_buffer_setpos(buf, data2_offset);
    if (buf->offset + (64 * 4) > buf->maxlen) {
        debug_print("%s", "HUFF data2 too short\n");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* read 32 mincode-maxcode pairs from data2 big-endian */
    huffcdic->mincode_table[0] = 0;
    huffcdic->maxcode_table[0] = 0xFFFFFFFF;
    for (int i = 1; i < HUFF_CODETABLE_SIZE; i++) {
        const uint32_t mincode = mobi_buffer_get32(buf);
        const uint32_t maxcode = mobi_buffer_get32(buf);
        huffcdic->mincode_table[i] =  mincode << (32 - i);
        huffcdic->maxcode_table[i] =  ((maxcode + 1) << (32 - i)) - 1;
    }
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Parse CDIC record into MOBIHuffCdic structure
 
 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
 @param[in] record MOBIPdbRecord structure containing the record
 @param[in] num Number of CDIC record in a set, starting from zero
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_cdic(MOBIHuffCdic *huffcdic, const MOBIPdbRecord *record, const size_t num) {
    MOBIBuffer *buf = mobi_buffer_init_null(record->data, record->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    char cdic_magic[5];
    mobi_buffer_getstring(cdic_magic, buf, 4);
    const size_t header_length = mobi_buffer_get32(buf);
    if (strncmp(cdic_magic, CDIC_MAGIC, 4) != 0 || header_length < CDIC_HEADER_LEN) {
        debug_print("CDIC wrong magic: %s or declared header length: %zu\n", cdic_magic, header_length);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* variables in huffcdic initialized to zero with calloc */
    /* save initial count and length */
    size_t index_count = mobi_buffer_get32(buf);
    const size_t code_length = mobi_buffer_get32(buf);
    if (huffcdic->code_length && huffcdic->code_length != code_length) {
        debug_print("CDIC different code length %zu in record %i, previous was %zu\n", huffcdic->code_length, record->uid, code_length);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    if (huffcdic->index_count && huffcdic->index_count != index_count) {
        debug_print("CDIC different index count %zu in record %i, previous was %zu\n", huffcdic->index_count, record->uid, index_count);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    if (code_length == 0 || code_length > HUFF_CODELEN_MAX) {
        debug_print("Code length exceeds sanity checks (%zu)\n", code_length);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    huffcdic->code_length = code_length;
    huffcdic->index_count = index_count;
    if (index_count == 0) {
        debug_print("%s", "CDIC index count is null");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* allocate memory for symbol offsets if not already allocated */
    if (num == 0) {
        if (index_count > (1 << HUFF_CODELEN_MAX) * CDIC_RECORD_MAXCNT) {
            debug_print("CDIC index count too large %zu\n", index_count);
            mobi_buffer_free_null(buf);
            return MOBI_DATA_CORRUPT;
        }
        huffcdic->symbol_offsets = malloc(index_count * sizeof(*huffcdic->symbol_offsets));
        if (huffcdic->symbol_offsets == NULL) {
            debug_print("%s", "CDIC cannot allocate memory");
            mobi_buffer_free_null(buf);
            return MOBI_MALLOC_FAILED;
        }
    }
    index_count -= huffcdic->index_read;
    /* limit number of records read to code_length bits */
    if (index_count >> code_length) {
        index_count = (1 << code_length);
    }
    if (buf->offset + (index_count * 2) > buf->maxlen) {
        debug_print("%s", "CDIC indices data too short\n");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* read i * 2 byte big-endian indices */
    while (index_count--) {
        const uint16_t offset = mobi_buffer_get16(buf);
        const size_t saved_pos = buf->offset;
        mobi_buffer_setpos(buf, offset + CDIC_HEADER_LEN);
        const size_t len = mobi_buffer_get16(buf) & 0x7fff;
        if (buf->error != MOBI_SUCCESS || buf->offset + len > buf->maxlen) {
            debug_print("%s", "CDIC offset beyond buffer\n");
            mobi_buffer_free_null(buf);
            return MOBI_DATA_CORRUPT;
        }
        mobi_buffer_setpos(buf, saved_pos);
        huffcdic->symbol_offsets[huffcdic->index_read++] = offset;
    }
    if (buf->offset + code_length > buf->maxlen) {
        debug_print("%s", "CDIC dictionary data too short\n");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    /* copy pointer to data */
    huffcdic->symbols[num] = record->data + CDIC_HEADER_LEN;
    /* free buffer */
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Parse a set of HUFF and CDIC records into MOBIHuffCdic structure
 
 @param[in] m MOBIData structure with loaded MOBI document
 @param[in,out] huffcdic MOBIHuffCdic structure to be filled with parsed data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_huffdic(const MOBIData *m, MOBIHuffCdic *huffcdic) {
    MOBI_RET ret;
    const size_t offset = mobi_get_kf8offset(m);
    if (m->mh == NULL || m->mh->huff_rec_index == NULL || m->mh->huff_rec_count == NULL) {
        debug_print("%s", "HUFF/CDIC records metadata not found in MOBI header\n");
        return MOBI_DATA_CORRUPT;
    }
    const size_t huff_rec_index = *m->mh->huff_rec_index + offset;
    const size_t huff_rec_count = *m->mh->huff_rec_count;
    if (huff_rec_count > HUFF_RECORD_MAXCNT) {
        debug_print("Too many HUFF record (%zu)\n", huff_rec_count);
        return MOBI_DATA_CORRUPT;
    }
    const MOBIPdbRecord *curr = mobi_get_record_by_seqnumber(m, huff_rec_index);
    if (curr == NULL || huff_rec_count < 2) {
        debug_print("%s", "HUFF/CDIC record not found\n");
        return MOBI_DATA_CORRUPT;
    }
    if (curr->size < HUFF_RECORD_MINSIZE) {
        debug_print("HUFF record too short (%zu b)\n", curr->size);
        return MOBI_DATA_CORRUPT;
    }
    ret = mobi_parse_huff(huffcdic, curr);
    if (ret != MOBI_SUCCESS) {
        debug_print("%s", "HUFF parsing failed\n");
        return ret;
    }
    curr = curr->next;
    /* allocate memory for symbols data in each CDIC record */
    huffcdic->symbols = malloc((huff_rec_count - 1) * sizeof(*huffcdic->symbols));
    if (huffcdic->symbols == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    /* get following CDIC records */
    size_t i = 0;
    while (i < huff_rec_count - 1) {
        if (curr == NULL) {
            debug_print("%s\n", "CDIC record not found");
            return MOBI_DATA_CORRUPT;
        }
        ret = mobi_parse_cdic(huffcdic, curr, i++);
        if (ret != MOBI_SUCCESS) {
            debug_print("%s", "CDIC parsing failed\n");
            return ret;
        }
        curr = curr->next;
    }
    if (huffcdic->index_count != huffcdic->index_read) {
        debug_print("CDIC: wrong read index count: %zu, total: %zu\n", huffcdic->index_read, huffcdic->index_count);
        return MOBI_DATA_CORRUPT;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Parse FDST record into MOBIRawml structure (MOBIFdst member)
 
 @param[in] m MOBIData structure with loaded MOBI document
 @param[in,out] rawml MOBIRawml structure to be filled with parsed data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_parse_fdst(const MOBIData *m, MOBIRawml *rawml) {
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    const size_t fdst_record_number = mobi_get_fdst_record_number(m);
    if (fdst_record_number == MOBI_NOTSET) {
        return MOBI_DATA_CORRUPT;
    }
    const MOBIPdbRecord *fdst_record = mobi_get_record_by_seqnumber(m, fdst_record_number);
    if (fdst_record == NULL) {
        return MOBI_DATA_CORRUPT;
    }
    MOBIBuffer *buf = mobi_buffer_init_null(fdst_record->data, fdst_record->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    char fdst_magic[5];
    mobi_buffer_getstring(fdst_magic, buf, 4);
    const size_t data_offset = mobi_buffer_get32(buf);
    const size_t section_count = mobi_buffer_get32(buf);
    if (strncmp(fdst_magic, FDST_MAGIC, 4) != 0 ||
        section_count <= 1 ||
        section_count != *m->mh->fdst_section_count ||
        data_offset != 12) {
        debug_print("FDST wrong magic: %s, sections count: %zu or data offset: %zu\n", fdst_magic, section_count, data_offset);
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    if ((buf->maxlen - buf->offset) < section_count * 8) {
        debug_print("%s", "Record FDST too short\n");
        mobi_buffer_free_null(buf);
        return MOBI_DATA_CORRUPT;
    }
    rawml->fdst = malloc(sizeof(MOBIFdst));
    if (rawml->fdst == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        mobi_buffer_free_null(buf);
        return MOBI_MALLOC_FAILED;
    }
    rawml->fdst->fdst_section_count = section_count;
    rawml->fdst->fdst_section_starts = malloc(sizeof(*rawml->fdst->fdst_section_starts) * section_count);
    if (rawml->fdst->fdst_section_starts == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        mobi_buffer_free_null(buf);
        free(rawml->fdst);
        rawml->fdst = NULL;
        return MOBI_MALLOC_FAILED;
    }
    rawml->fdst->fdst_section_ends = malloc(sizeof(*rawml->fdst->fdst_section_ends) * section_count);
    if (rawml->fdst->fdst_section_ends == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        mobi_buffer_free_null(buf);
        free(rawml->fdst->fdst_section_starts);
        free(rawml->fdst);
        rawml->fdst = NULL;
        return MOBI_MALLOC_FAILED;
    }
    size_t i = 0;
    while (i < section_count) {
        rawml->fdst->fdst_section_starts[i] = mobi_buffer_get32(buf);
        rawml->fdst->fdst_section_ends[i] = mobi_buffer_get32(buf);
        debug_print("FDST[%zu]:\t%i\t%i\n", i, rawml->fdst->fdst_section_starts[i], rawml->fdst->fdst_section_ends[i]);
        i++;
    }
    mobi_buffer_free_null(buf);
    return MOBI_SUCCESS;
}

/**
 @brief Read MOBI document from file into MOBIData structure
 
 @param[in,out] m MOBIData structure to be filled with read data
 @param[in] file File descriptor to read from
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_file(MOBIData *m, FILE *file) {
    MOBI_RET ret;
    if (m == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    ret = mobi_load_pdbheader(m, file);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    if (strcmp(m->ph->type, "BOOK") != 0 && strcmp(m->ph->type, "TEXt") != 0) {
        debug_print("Unsupported file type: %s\n", m->ph->type);
        return MOBI_FILE_UNSUPPORTED;
    }
    if (m->ph->rec_count == 0) {
        debug_print("%s", "No records found\n");
        return MOBI_DATA_CORRUPT;
    }
    ret = mobi_load_reclist(m, file);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    ret = mobi_load_rec(m, file);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    ret = mobi_parse_record0(m, 0);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    if (m->rh && m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        /* try to set key for encryption type 1 */
        debug_print("Trying to set key for encryption type 1%s", "\n");
        mobi_drm_setkey(m, NULL);
    }
    /* if EXTH is loaded parse KF8 record0 for hybrid KF7/KF8 file */
    if (m->eh) {
        const size_t boundary_rec_number = mobi_get_kf8boundary_seqnumber(m);
        if (boundary_rec_number != MOBI_NOTSET && boundary_rec_number < UINT32_MAX) {
            /* it is a hybrid KF7/KF8 file */
            m->kf8_boundary_offset = (uint32_t) boundary_rec_number;
            m->next = mobi_init();
            /* link pdb header and records data to KF8data structure */
            m->next->ph = m->ph;
            m->next->rec = m->rec;
            m->next->drm_key = m->drm_key;
            m->next->internals = m->internals;
            /* close next loop */
            m->next->next = m;
            ret = mobi_parse_record0(m->next, boundary_rec_number + 1);
            if (ret != MOBI_SUCCESS) {
                return ret;
            }
            /* swap to kf8 part if use_kf8 flag is set */
            if (m->use_kf8) {
                mobi_swap_mobidata(m);
            }
        }
    }
    return MOBI_SUCCESS;
}

/**
 @brief Read MOBI document from a path into MOBIData structure
 
 @param[in,out] m MOBIData structure to be filled with read data
 @param[in] path Path to a MOBI document on disk (eg. /home/me/test.mobi)
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_load_filename(MOBIData *m, const char *path) {
    FILE *file = fopen(path, "rb");
    if (file == NULL) {
        debug_print("%s", "File not found\n");
        return MOBI_FILE_NOT_FOUND;
    }
    const MOBI_RET ret = mobi_load_file(m, file);
    fclose(file);
    return ret;
}
