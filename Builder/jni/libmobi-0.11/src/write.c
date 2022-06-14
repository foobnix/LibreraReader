/** @file write.c
 *  @brief Writing functions
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "write.h"
#include "util.h"
#include "debug.h"
#ifdef USE_ENCRYPTION
#include "encryption.h"
#endif

#define MOBI_HEADER_MAXLEN 280
#define MOBI_RECORD0_PADDING 0x2002

/**
 @brief Write buffer contents to file
 
 @param[in,out] file File descriptor
 @param[in] buf Buffer
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_write_buffer(FILE *file, const MOBIBuffer *buf) {
    const size_t written = fwrite(buf->data, 1, buf->maxlen, file);
    if (written != buf->maxlen) {
        debug_print("Writing failed (%s)\n", strerror(errno));
        return MOBI_WRITE_FAILED;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write palm database header to file
 
 @param[in,out] file File descriptor
 @param[in] m MOBIData structure
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_write_pdbheader(FILE *file, const MOBIData *m) {
    if (m == NULL || m->ph == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (file == NULL) {
        debug_print("%s", "File not initialized\n");
        return MOBI_PARAM_ERR;
    }
    MOBIBuffer *buf = mobi_buffer_init(PALMDB_HEADER_LEN);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    mobi_buffer_addstring(buf, m->ph->name);
    size_t len = strlen(m->ph->name);
    mobi_buffer_addzeros(buf, PALMDB_NAME_SIZE_MAX - len);
    mobi_buffer_add16(buf, m->ph->attributes);
    mobi_buffer_add16(buf, m->ph->version);
    mobi_buffer_add32(buf, m->ph->ctime);
    mobi_buffer_add32(buf, m->ph->mtime);
    mobi_buffer_add32(buf, m->ph->btime);
    mobi_buffer_add32(buf, m->ph->mod_num);
    mobi_buffer_add32(buf, m->ph->appinfo_offset);
    mobi_buffer_add32(buf, m->ph->sortinfo_offset);
    mobi_buffer_addstring(buf, m->ph->type);
    mobi_buffer_addstring(buf, m->ph->creator);
    mobi_buffer_add32(buf, m->ph->uid);
    mobi_buffer_add32(buf, m->ph->next_rec);
    m->ph->rec_count = mobi_get_records_count(m);
    if (m->ph->rec_count == 0) {
        mobi_buffer_free(buf);
        debug_print("%s", "Zero records count\n");
        return MOBI_DATA_CORRUPT;
    }
    mobi_buffer_add16(buf, m->ph->rec_count);
    if (buf->error != MOBI_SUCCESS) {
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    MOBI_RET ret = mobi_write_buffer(file, buf);
    mobi_buffer_free(buf);
    return ret;
}

/**
 @brief Serialize mobi header to buffer
 
 @param[in,out] buf output buffer
 @param[in] m MOBIData structure
 @param[in] exthsize Size of exth record
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_serialize_mobiheader(MOBIBuffer *buf, const MOBIData *m, const uint32_t exthsize) {
    if (m == NULL || m->mh == NULL || buf == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    size_t buffer_init = buf->offset;
    mobi_buffer_addstring(buf, m->mh->mobi_magic);
    if (buf->offset > UINT32_MAX) {
        debug_print("Offset too large: %zu\n", buf->offset);
        return MOBI_DATA_CORRUPT;
    }
    uint32_t length_offset = (uint32_t) buf->offset;
    uint32_t name_offset = 0;
    uint32_t drm_offset = 0;
    mobi_buffer_add32(buf, 0); /* dummy length */
    if (m->mh->mobi_type) { mobi_buffer_add32(buf, *m->mh->mobi_type); } else { goto finalize; }
    if (m->mh->text_encoding) { mobi_buffer_add32(buf, *m->mh->text_encoding); } else { goto finalize; }
    if (m->mh->uid) { mobi_buffer_add32(buf, *m->mh->uid); } else { goto finalize; }
    bool isKF8 = false;
    if (m->mh->version) {
        mobi_buffer_add32(buf, *m->mh->version);
        if (*m->mh->version == 8) {
            isKF8 = true;
        }
    } else { goto finalize; }
    if (m->mh->orth_index) { mobi_buffer_add32(buf, *m->mh->orth_index); } else { goto finalize; }
    if (m->mh->infl_index) { mobi_buffer_add32(buf, *m->mh->infl_index); } else { goto finalize; }
    if (m->mh->names_index) { mobi_buffer_add32(buf, *m->mh->names_index); } else { goto finalize; }
    if (m->mh->keys_index) { mobi_buffer_add32(buf, *m->mh->keys_index); } else { goto finalize; }
    if (m->mh->extra0_index) { mobi_buffer_add32(buf, *m->mh->extra0_index); } else { goto finalize; }
    if (m->mh->extra1_index) { mobi_buffer_add32(buf, *m->mh->extra1_index); } else { goto finalize; }
    if (m->mh->extra2_index) { mobi_buffer_add32(buf, *m->mh->extra2_index); } else { goto finalize; }
    if (m->mh->extra3_index) { mobi_buffer_add32(buf, *m->mh->extra3_index); } else { goto finalize; }
    if (m->mh->extra4_index) { mobi_buffer_add32(buf, *m->mh->extra4_index); } else { goto finalize; }
    if (m->mh->extra5_index) { mobi_buffer_add32(buf, *m->mh->extra5_index); } else { goto finalize; }
    if (m->mh->non_text_index) { mobi_buffer_add32(buf, *m->mh->non_text_index); } else { goto finalize; }
    if (m->mh->full_name) {
        if (buf->offset > UINT32_MAX) {
            debug_print("Offset too large: %zu\n", buf->offset);
            return MOBI_DATA_CORRUPT;
        }
        name_offset = (uint32_t) buf->offset;
        mobi_buffer_add32(buf, MOBI_NOTSET);
        mobi_buffer_add32(buf, 0);
    } else { goto finalize; }
    if (m->mh->locale) { mobi_buffer_add32(buf, *m->mh->locale); } else { goto finalize; }
    if (m->mh->dict_input_lang) { mobi_buffer_add32(buf, *m->mh->dict_input_lang); } else { goto finalize; }
    if (m->mh->dict_output_lang) { mobi_buffer_add32(buf, *m->mh->dict_output_lang); } else { goto finalize; }
    if (m->mh->min_version) { mobi_buffer_add32(buf, *m->mh->min_version); } else { goto finalize; }
    if (m->mh->image_index) { mobi_buffer_add32(buf, *m->mh->image_index); } else { goto finalize; }
    if (m->mh->huff_rec_index) { mobi_buffer_add32(buf, *m->mh->huff_rec_index); } else { goto finalize; }
    if (m->mh->huff_rec_count) { mobi_buffer_add32(buf, *m->mh->huff_rec_count); } else { goto finalize; }
    if (m->mh->datp_rec_index) { mobi_buffer_add32(buf, *m->mh->datp_rec_index); } else { goto finalize; }
    if (m->mh->datp_rec_count) { mobi_buffer_add32(buf, *m->mh->datp_rec_count); } else { goto finalize; }
    if (m->mh->exth_flags) { mobi_buffer_add32(buf, *m->mh->exth_flags); } else { goto finalize; }
    mobi_buffer_addzeros(buf, 32); /* 32 unknown bytes */
    if (m->mh->unknown6) { mobi_buffer_add32(buf, *m->mh->unknown6); } else { goto finalize; }
    if (m->mh->drm_offset) {
        drm_offset = (uint32_t) buf->offset;
        mobi_buffer_add32(buf, *m->mh->drm_offset);
    } else { goto finalize; }
    if (m->mh->drm_count) { mobi_buffer_add32(buf, *m->mh->drm_count); } else { goto finalize; }
    if (m->mh->drm_size) { mobi_buffer_add32(buf, *m->mh->drm_size); } else { goto finalize; }
    if (m->mh->drm_flags) { mobi_buffer_add32(buf, *m->mh->drm_flags); } else { goto finalize; }
    mobi_buffer_addzeros(buf, 8); /* 8 unknown bytes */
    if (isKF8) {
        if (m->mh->fdst_index) { mobi_buffer_add32(buf, *m->mh->fdst_index); } else { goto finalize; }
    } else {
        if (m->mh->first_text_index) { mobi_buffer_add16(buf, *m->mh->first_text_index); } else { goto finalize; }
        if (m->mh->last_text_index) { mobi_buffer_add16(buf, *m->mh->last_text_index); } else { goto finalize; }
    }
    if (m->mh->fdst_section_count) { mobi_buffer_add32(buf, *m->mh->fdst_section_count); } else { goto finalize; }
    if (m->mh->fcis_index) { mobi_buffer_add32(buf, *m->mh->fcis_index); } else { goto finalize; }
    if (m->mh->fcis_count) { mobi_buffer_add32(buf, *m->mh->fcis_count); } else { goto finalize; }
    if (m->mh->flis_index) { mobi_buffer_add32(buf, *m->mh->flis_index); } else { goto finalize; }
    if (m->mh->flis_count) { mobi_buffer_add32(buf, *m->mh->flis_count); } else { goto finalize; }
    if (m->mh->unknown10) { mobi_buffer_add32(buf, *m->mh->unknown10); } else { goto finalize; }
    if (m->mh->unknown11) { mobi_buffer_add32(buf, *m->mh->unknown11); } else { goto finalize; }
    if (m->mh->srcs_index) { mobi_buffer_add32(buf, *m->mh->srcs_index); } else { goto finalize; }
    if (m->mh->srcs_count) { mobi_buffer_add32(buf, *m->mh->srcs_count); } else { goto finalize; }
    if (m->mh->unknown12) { mobi_buffer_add32(buf, *m->mh->unknown12); } else { goto finalize; }
    if (m->mh->unknown13) { mobi_buffer_add32(buf, *m->mh->unknown13); } else { goto finalize; }
    mobi_buffer_addzeros(buf, 2); /* 2 unknown bytes */
    if (m->mh->extra_flags) { mobi_buffer_add16(buf, *m->mh->extra_flags); } else { goto finalize; }
    if (m->mh->ncx_index) { mobi_buffer_add32(buf, *m->mh->ncx_index); } else { goto finalize; }
    if (isKF8) {
        if (m->mh->fragment_index) { mobi_buffer_add32(buf, *m->mh->fragment_index); } else { goto finalize; }
        if (m->mh->skeleton_index) { mobi_buffer_add32(buf, *m->mh->skeleton_index); } else { goto finalize; }
    } else {
        if (m->mh->unknown14) { mobi_buffer_add32(buf, *m->mh->unknown14); } else { goto finalize; }
        if (m->mh->unknown15) { mobi_buffer_add32(buf, *m->mh->unknown15); } else { goto finalize; }
    }
    if (m->mh->datp_index) { mobi_buffer_add32(buf, *m->mh->datp_index); } else { goto finalize; }
    if (isKF8) {
        if (m->mh->guide_index) { mobi_buffer_add32(buf, *m->mh->guide_index); } else { goto finalize; }
    } else {
        if (m->mh->unknown16) { mobi_buffer_add32(buf, *m->mh->unknown16); } else { goto finalize; }
    }
    if (m->mh->unknown17) { mobi_buffer_add32(buf, *m->mh->unknown17); } else { goto finalize; }
    if (m->mh->unknown18) { mobi_buffer_add32(buf, *m->mh->unknown18); } else { goto finalize; }
    if (m->mh->unknown19) { mobi_buffer_add32(buf, *m->mh->unknown19); } else { goto finalize; }
    if (m->mh->unknown20) { mobi_buffer_add32(buf, *m->mh->unknown20); } else { goto finalize; }

finalize:
    if (buf->error != MOBI_SUCCESS) {
        return MOBI_DATA_CORRUPT;
    }
    size_t headersize = buf->offset - buffer_init;
    if (headersize > UINT32_MAX) {
        debug_print("Header too large: %zu\n", headersize);
        return MOBI_DATA_CORRUPT;
    }
    size_t saved_offset = buf->offset;
    /* write header length at offset 20 */
    mobi_buffer_setpos(buf, length_offset);
    mobi_buffer_add32(buf, (uint32_t) headersize);
    *m->mh->header_length = (uint32_t) headersize;
    uint32_t drmsize = 0;
#ifdef USE_ENCRYPTION
    if (m->rh->encryption_type == MOBI_ENCRYPTION_V2 &&
        m->mh->drm_size && m->mh->drm_offset && *m->mh->drm_size > 0) {
        drmsize = mobi_get_drmsize(m);
        *m->mh->drm_offset = RECORD0_HEADER_LEN + (uint32_t) headersize + exthsize;
        mobi_buffer_setpos(buf, drm_offset);
        mobi_buffer_add32(buf, *m->mh->drm_offset);
    } else if (m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        drmsize = mobi_get_drmsize(m);
    }
#endif
    
    if (m->mh->full_name) {
        /* full name's offset is after exth records */
        uint32_t fullname_offset = RECORD0_HEADER_LEN + (uint32_t) headersize + exthsize + drmsize;
        size_t fullname_length = strlen(m->mh->full_name);
        if (fullname_length > MOBI_TITLE_SIZEMAX) {
            fullname_length = MOBI_TITLE_SIZEMAX;
            m->mh->full_name[MOBI_TITLE_SIZEMAX] = '\0';
        }
        /* write fullname offset and length */
        mobi_buffer_setpos(buf, name_offset);
        mobi_buffer_add32(buf, fullname_offset);
        mobi_buffer_add32(buf, (uint32_t) fullname_length);
        if (m->mh->full_name_offset == NULL) {
            m->mh->full_name_offset = malloc(sizeof(uint32_t));
            if (m->mh->full_name_offset == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        *m->mh->full_name_offset = fullname_offset;
        if (m->mh->full_name_length == NULL) {
            m->mh->full_name_length = malloc(sizeof(uint32_t));
            if (m->mh->full_name_length == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        *m->mh->full_name_length = (uint32_t) fullname_length;
    }
    mobi_buffer_setpos(buf, saved_offset);
    if (buf->error != MOBI_SUCCESS) {
        return MOBI_DATA_CORRUPT;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Serialize exth header to buffer
 
 @param[in,out] buf output buffer
 @param[in] m MOBIData structure
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_serialize_extheader(MOBIBuffer *buf, const MOBIData *m) {
    if (m == NULL || m->eh == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    MOBIExthHeader *curr = m->eh;
    mobi_buffer_addstring(buf, EXTH_MAGIC);
    const size_t length_offset = buf->offset;
    size_t length = 12; /* start with header length */
    mobi_buffer_add32(buf, 0);
    const size_t count_offset = buf->offset;
    size_t count = 0;
    mobi_buffer_add32(buf, 0);
    while (curr) {
        /* total size = data size plus 8 bytes for uid and size */
        const uint32_t size = curr->size + 8;
        length += size;
        count++;
        mobi_buffer_add32(buf, curr->tag);
        mobi_buffer_add32(buf, size);
        mobi_buffer_addraw(buf, curr->data, curr->size);
        if (buf->error != MOBI_SUCCESS) {
            return MOBI_DATA_CORRUPT;
        }
        curr = curr->next;
    }
    if (length > UINT32_MAX || count > UINT32_MAX) {
        debug_print("Length (%zu) or count (%zu) too large\n", length, count);
        return MOBI_DATA_CORRUPT;
    }
    /* add padding */
    const size_t padding_size = length % 4;
    length += padding_size;
    mobi_buffer_addzeros(buf, padding_size);
    const size_t saved_offset = buf->offset;
    mobi_buffer_setpos(buf, length_offset);
    mobi_buffer_add32(buf, (uint32_t) length);
    mobi_buffer_setpos(buf, count_offset);
    mobi_buffer_add32(buf, (uint32_t) count);
    mobi_buffer_setpos(buf, saved_offset);
    return MOBI_SUCCESS;
}

/**
 @brief Serialize record0 and update record in MOBIData structure.
 
 Record0 sequential number may be greater than zero in case
 of hybrid file with two info records
 
 @param[in,out] m MOBIData structure
 @param[in] seqnumber Record0 sequential number
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_update_record0(MOBIData *m, const size_t seqnumber) {
    if (m == NULL || m->rh == NULL || m->rec == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    size_t padding = MOBI_RECORD0_PADDING;
    if (!mobi_exists_mobiheader(m)) {
        padding = 0;
    } else if (mobi_get_fileversion(m) < 8) {
        padding -= 12;
    }
    size_t record0_maxlen = RECORD0_HEADER_LEN + MOBI_HEADER_MAXLEN;
    uint32_t exthsize = mobi_get_exthsize(m);
    uint32_t drmsize = mobi_get_drmsize(m);
    record0_maxlen += exthsize;
    record0_maxlen += drmsize;
    record0_maxlen += MOBI_TITLE_SIZEMAX;
    record0_maxlen += padding;
    MOBIBuffer *buf = mobi_buffer_init(record0_maxlen);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    mobi_buffer_add16(buf, m->rh->compression_type);
    mobi_buffer_addzeros(buf, 2);
    mobi_buffer_add32(buf, m->rh->text_length);
    mobi_buffer_add16(buf, m->rh->text_record_count);
    mobi_buffer_add16(buf, m->rh->text_record_size);
    mobi_buffer_add16(buf, m->rh->encryption_type);
    mobi_buffer_add16(buf, m->rh->unknown1);

    if (m->mh) {
        MOBI_RET ret = mobi_serialize_mobiheader(buf, m, exthsize);
        if (ret != MOBI_SUCCESS) {
            mobi_buffer_free(buf);
            return ret;
        }
        if (m->eh) {
            ret = mobi_serialize_extheader(buf, m);
            if (ret != MOBI_SUCCESS) {
                mobi_buffer_free(buf);
                return ret;
            }
        }
        
#ifdef USE_ENCRYPTION
        if (m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
            ret = mobi_drm_serialize_v1(buf, m);
        } else if (m->rh->encryption_type == MOBI_ENCRYPTION_V2) {
            ret = mobi_drm_serialize_v2(buf, m);
        }
        if (ret != MOBI_SUCCESS) {
            mobi_buffer_free(buf);
            return ret;
        }
#endif
        if (m->mh->full_name && m->mh->full_name_offset) {
            mobi_buffer_setpos(buf, *m->mh->full_name_offset);
            mobi_buffer_addstring(buf, m->mh->full_name);
            if (buf->error != MOBI_SUCCESS) {
                mobi_buffer_free(buf);
                return MOBI_DATA_CORRUPT;
            }
        }
    }
#ifdef USE_ENCRYPTION
    else if (m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        MOBI_RET ret = mobi_drm_serialize_v1(buf, m);
        if (ret != MOBI_SUCCESS) {
            mobi_buffer_free(buf);
            return ret;
        }
        mobi_buffer_setpos(buf, 14 + drmsize);
    }
#endif

    mobi_buffer_addzeros(buf, padding);
    if (buf->error) {
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    
    MOBIPdbRecord *record0 = mobi_get_record_by_seqnumber(m, seqnumber);
    if (record0 == NULL) {
        debug_print("%s", "Record 0 not initialized\n");
        mobi_buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    unsigned char *data = malloc(buf->offset);
    if (data == NULL) {
        mobi_buffer_free(buf);
        return MOBI_MALLOC_FAILED;
    }
    memcpy(data, buf->data, buf->offset);
    record0->size = buf->offset;
    mobi_buffer_free(buf);
    if (record0->data) {
        free(record0->data);
    }
    record0->data = data;
    return MOBI_SUCCESS;
}

/**
 @brief Write palm database records to file
 
 @param[in,out] file File descriptor
 @param[in] m MOBIData structure
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_write_records(FILE *file, const MOBIData *m) {
    if (m == NULL || m->rec == NULL) {
        debug_print("%s", "Mobi structure not initialized\n");
        return MOBI_INIT_FAILED;
    }
    if (file == NULL) {
        return MOBI_PARAM_ERR;
    }
    long pos = ftell(file);
    if (pos < 0) {
        return MOBI_WRITE_FAILED;
    }
    uint32_t offset = (uint32_t) pos;
    /* 8 bytes per record meta plus 2 bytes padding */
    offset += 8 * m->ph->rec_count + 2;
    MOBIPdbRecord *curr = m->rec;
    uint32_t i = 0;
    while (curr) {
        if (offset > UINT32_MAX) {
            return MOBI_DATA_CORRUPT;
        }
        MOBIBuffer *buf = mobi_buffer_init(PALMDB_RECORD_INFO_SIZE);
        if (buf == NULL) {
            return MOBI_MALLOC_FAILED;
        }
        mobi_buffer_add32(buf, (uint32_t) offset);
        offset += curr->size;
        mobi_buffer_add8(buf, curr->attributes);
        curr->uid = 2 * i++;
        const uint8_t h = (uint8_t) ((curr->uid & 0xff0000U) >> 16);
        const uint16_t l = (uint16_t) (curr->uid & 0xffffU);
        mobi_buffer_add8(buf, h);
        mobi_buffer_add16(buf, l);
        if (buf->error != MOBI_SUCCESS) {
            mobi_buffer_free(buf);
            return MOBI_DATA_CORRUPT;
        }
        MOBI_RET ret = mobi_write_buffer(file, buf);
        mobi_buffer_free(buf);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
        curr = curr->next;
    }
    char padding[2] = { 0 };
    size_t written = fwrite(padding, 1, sizeof(padding), file);
    if (written != sizeof(padding)) {
        debug_print("Writing failed (%s)\n", strerror(errno));
        return MOBI_WRITE_FAILED;
    }
    
    curr = m->rec;
    while (curr) {
        written = fwrite(curr->data, 1, curr->size, file);
        if (written != curr->size) {
            debug_print("Writing failed (%s)\n", strerror(errno));
            return MOBI_WRITE_FAILED;
        }
        curr = curr->next;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Write mobi document to file.
 
 Serializes metadata from MOBIData into raw records also stored in MOBIData (m->rec).
 Later writes palm database to file.
 
 @param[in,out] file File descriptor
 @param[in,out] m MOBIData structure
 @return MOBI_RET status code (MOBI_SUCCESS on success)
 */
MOBI_RET mobi_write_file(FILE *file, MOBIData *m) {
    MOBI_RET ret = mobi_write_pdbheader(file, m);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    MOBIData *m_kf7 = m;
    if (mobi_is_hybrid(m) && m->next) {
        MOBIData *m_kf8 = m;
        if (m->use_kf8 == false) {
            m_kf8 = m->next;
        } else {
            m_kf7 = m->next;
        }
        const size_t record0_kf8_offset = m_kf8->kf8_boundary_offset + 1;
        ret = mobi_update_record0(m_kf8, record0_kf8_offset);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    }
    ret = mobi_update_record0(m_kf7, 0);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    ret = mobi_write_records(file, m);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    return MOBI_SUCCESS;
    
}
