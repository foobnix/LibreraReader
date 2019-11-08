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
    } else {
        return MOBI_SUCCESS;
    }
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
    MOBIBuffer *buf = buffer_init(PALMDB_HEADER_LEN);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    buffer_addstring(buf, m->ph->name);
    size_t len = strlen(m->ph->name);
    buffer_addzeros(buf, PALMDB_NAME_SIZE_MAX - len);
    buffer_add16(buf, m->ph->attributes);
    buffer_add16(buf, m->ph->version);
    buffer_add32(buf, m->ph->ctime);
    buffer_add32(buf, m->ph->mtime);
    buffer_add32(buf, m->ph->btime);
    buffer_add32(buf, m->ph->mod_num);
    buffer_add32(buf, m->ph->appinfo_offset);
    buffer_add32(buf, m->ph->sortinfo_offset);
    buffer_addstring(buf, m->ph->type);
    buffer_addstring(buf, m->ph->creator);
    buffer_add32(buf, m->ph->uid);
    buffer_add32(buf, m->ph->next_rec);
    uint16_t rec_count = mobi_get_records_count(m);
    if (rec_count == 0) {
        buffer_free(buf);
        debug_print("%s", "Zero records count\n");
        return MOBI_DATA_CORRUPT;
    }
    buffer_add16(buf, rec_count);
    if (buf->error != MOBI_SUCCESS) {
        buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    MOBI_RET ret = mobi_write_buffer(file, buf);
    buffer_free(buf);
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
    buffer_addstring(buf, m->mh->mobi_magic);
    if (buf->offset > UINT32_MAX) {
        debug_print("Offset too large: %zu\n", buf->offset);
        return MOBI_DATA_CORRUPT;
    }
    uint32_t length_offset = (uint32_t) buf->offset;
    uint32_t name_offset = 0;
    buffer_add32(buf, 0); /* dummy length */
    if (m->mh->mobi_type) { buffer_add32(buf, *m->mh->mobi_type); } else { goto finalize; }
    if (m->mh->text_encoding) { buffer_add32(buf, *m->mh->text_encoding); } else { goto finalize; }
    if (m->mh->uid) { buffer_add32(buf, *m->mh->uid); } else { goto finalize; }
    bool isKF8 = false;
    if (m->mh->version) {
        buffer_add32(buf, *m->mh->version);
        if (*m->mh->version == 8) {
            isKF8 = true;
        }
    } else { goto finalize; }
    if (m->mh->orth_index) { buffer_add32(buf, *m->mh->orth_index); } else { goto finalize; }
    if (m->mh->infl_index) { buffer_add32(buf, *m->mh->infl_index); } else { goto finalize; }
    if (m->mh->names_index) { buffer_add32(buf, *m->mh->names_index); } else { goto finalize; }
    if (m->mh->keys_index) { buffer_add32(buf, *m->mh->keys_index); } else { goto finalize; }
    if (m->mh->extra0_index) { buffer_add32(buf, *m->mh->extra0_index); } else { goto finalize; }
    if (m->mh->extra1_index) { buffer_add32(buf, *m->mh->extra1_index); } else { goto finalize; }
    if (m->mh->extra2_index) { buffer_add32(buf, *m->mh->extra2_index); } else { goto finalize; }
    if (m->mh->extra3_index) { buffer_add32(buf, *m->mh->extra3_index); } else { goto finalize; }
    if (m->mh->extra4_index) { buffer_add32(buf, *m->mh->extra4_index); } else { goto finalize; }
    if (m->mh->extra5_index) { buffer_add32(buf, *m->mh->extra5_index); } else { goto finalize; }
    if (m->mh->non_text_index) { buffer_add32(buf, *m->mh->non_text_index); } else { goto finalize; }
    if (m->mh->full_name) {
        if (buf->offset > UINT32_MAX) {
             debug_print("Offset too large: %zu\n", buf->offset);
            return MOBI_DATA_CORRUPT;
        }
        name_offset = (uint32_t) buf->offset;
        buffer_add32(buf, MOBI_NOTSET);
        buffer_add32(buf, 0);
    } else { goto finalize; }
    if (m->mh->locale) { buffer_add32(buf, *m->mh->locale); } else { goto finalize; }
    if (m->mh->dict_input_lang) { buffer_add32(buf, *m->mh->dict_input_lang); } else { goto finalize; }
    if (m->mh->dict_output_lang) { buffer_add32(buf, *m->mh->dict_output_lang); } else { goto finalize; }
    if (m->mh->min_version) { buffer_add32(buf, *m->mh->min_version); } else { goto finalize; }
    if (m->mh->image_index) { buffer_add32(buf, *m->mh->image_index); } else { goto finalize; }
    if (m->mh->huff_rec_index) { buffer_add32(buf, *m->mh->huff_rec_index); } else { goto finalize; }
    if (m->mh->huff_rec_count) { buffer_add32(buf, *m->mh->huff_rec_count); } else { goto finalize; }
    if (m->mh->datp_rec_index) { buffer_add32(buf, *m->mh->datp_rec_index); } else { goto finalize; }
    if (m->mh->datp_rec_count) { buffer_add32(buf, *m->mh->datp_rec_count); } else { goto finalize; }
    if (m->mh->exth_flags) { buffer_add32(buf, *m->mh->exth_flags); } else { goto finalize; }
    buffer_addzeros(buf, 32); /* 32 unknown bytes */
    if (m->mh->unknown6) { buffer_add32(buf, *m->mh->unknown6); } else { goto finalize; }
    if (m->mh->drm_offset) { buffer_add32(buf, *m->mh->drm_offset); } else { goto finalize; }
    if (m->mh->drm_count) { buffer_add32(buf, *m->mh->drm_count); } else { goto finalize; }
    if (m->mh->drm_size) { buffer_add32(buf, *m->mh->drm_size); } else { goto finalize; }
    if (m->mh->drm_flags) { buffer_add32(buf, *m->mh->drm_flags); } else { goto finalize; }
    buffer_addzeros(buf, 8); /* 8 unknown bytes */
    if (isKF8) {
        if (m->mh->fdst_index) { buffer_add32(buf, *m->mh->fdst_index); } else { goto finalize; }
    } else {
        if (m->mh->first_text_index) { buffer_add16(buf, *m->mh->first_text_index); } else { goto finalize; }
        if (m->mh->last_text_index) { buffer_add16(buf, *m->mh->last_text_index); } else { goto finalize; }
    }
    if (m->mh->fdst_section_count) { buffer_add32(buf, *m->mh->fdst_section_count); } else { goto finalize; }
    if (m->mh->fcis_index) { buffer_add32(buf, *m->mh->fcis_index); } else { goto finalize; }
    if (m->mh->fcis_count) { buffer_add32(buf, *m->mh->fcis_count); } else { goto finalize; }
    if (m->mh->flis_index) { buffer_add32(buf, *m->mh->flis_index); } else { goto finalize; }
    if (m->mh->flis_count) { buffer_add32(buf, *m->mh->flis_count); } else { goto finalize; }
    if (m->mh->unknown10) { buffer_add32(buf, *m->mh->unknown10); } else { goto finalize; }
    if (m->mh->unknown11) { buffer_add32(buf, *m->mh->unknown11); } else { goto finalize; }
    if (m->mh->srcs_index) { buffer_add32(buf, *m->mh->srcs_index); } else { goto finalize; }
    if (m->mh->srcs_count) { buffer_add32(buf, *m->mh->srcs_count); } else { goto finalize; }
    if (m->mh->unknown12) { buffer_add32(buf, *m->mh->unknown12); } else { goto finalize; }
    if (m->mh->unknown13) { buffer_add32(buf, *m->mh->unknown13); } else { goto finalize; }
    buffer_addzeros(buf, 2); /* 2 unknown bytes */
    if (m->mh->extra_flags) { buffer_add16(buf, *m->mh->extra_flags); } else { goto finalize; }
    if (m->mh->ncx_index) { buffer_add32(buf, *m->mh->ncx_index); } else { goto finalize; }
    if (isKF8) {
        if (m->mh->fragment_index) { buffer_add32(buf, *m->mh->fragment_index); } else { goto finalize; }
        if (m->mh->skeleton_index) { buffer_add32(buf, *m->mh->skeleton_index); } else { goto finalize; }
    } else {
        if (m->mh->unknown14) { buffer_add32(buf, *m->mh->unknown14); } else { goto finalize; }
        if (m->mh->unknown15) { buffer_add32(buf, *m->mh->unknown15); } else { goto finalize; }
    }
    if (m->mh->datp_index) { buffer_add32(buf, *m->mh->datp_index); } else { goto finalize; }
    if (isKF8) {
        if (m->mh->guide_index) { buffer_add32(buf, *m->mh->guide_index); } else { goto finalize; }
    } else {
        if (m->mh->unknown16) { buffer_add32(buf, *m->mh->unknown16); } else { goto finalize; }
    }
    if (m->mh->unknown17) { buffer_add32(buf, *m->mh->unknown17); } else { goto finalize; }
    if (m->mh->unknown18) { buffer_add32(buf, *m->mh->unknown18); } else { goto finalize; }
    if (m->mh->unknown19) { buffer_add32(buf, *m->mh->unknown19); } else { goto finalize; }
    if (m->mh->unknown20) { buffer_add32(buf, *m->mh->unknown20); } else { goto finalize; }

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
    buffer_setpos(buf, length_offset);
    buffer_add32(buf, (uint32_t) headersize);
    if (m->mh->full_name) {
        /* full name's offset is after exth records */
        uint32_t fullname_offset = RECORD0_HEADER_LEN + (uint32_t) headersize + exthsize;
        size_t fullname_length = strlen(m->mh->full_name);
        if (fullname_length > MOBI_TITLE_SIZEMAX) {
            fullname_length = MOBI_TITLE_SIZEMAX;
            m->mh->full_name[MOBI_TITLE_SIZEMAX] = '\0';
        }
        /* write fullname offset and length */
        buffer_setpos(buf, name_offset);
        buffer_add32(buf, fullname_offset);
        buffer_add32(buf, (uint32_t) fullname_length);
    }
    buffer_setpos(buf, saved_offset);
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
    buffer_addstring(buf, EXTH_MAGIC);
    const size_t length_offset = buf->offset;
    size_t length = 12; /* start with header length */
    buffer_add32(buf, 0);
    const size_t count_offset = buf->offset;
    size_t count = 0;
    buffer_add32(buf, 0);
    while (curr) {
        /* total size = data size plus 8 bytes for uid and size */
        const uint32_t size = curr->size + 8;
        length += size;
        count++;
        buffer_add32(buf, curr->tag);
        buffer_add32(buf, size);
        buffer_addraw(buf, curr->data, curr->size);
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
    buffer_addzeros(buf, padding_size);
    const size_t saved_offset = buf->offset;
    buffer_setpos(buf, length_offset);
    buffer_add32(buf, (uint32_t) length);
    buffer_setpos(buf, count_offset);
    buffer_add32(buf, (uint32_t) count);
    buffer_setpos(buf, saved_offset);
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
    record0_maxlen += exthsize;
    record0_maxlen += MOBI_TITLE_SIZEMAX;
    record0_maxlen += padding;
    MOBIBuffer *buf = buffer_init(record0_maxlen);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    buffer_add16(buf, m->rh->compression_type);
    buffer_addzeros(buf, 2);
    buffer_add32(buf, m->rh->text_length);
    buffer_add16(buf, m->rh->text_record_count);
    buffer_add16(buf, m->rh->text_record_size);
    buffer_add16(buf, m->rh->encryption_type);
    buffer_add16(buf, m->rh->unknown1);

    if (m->mh) {
        MOBI_RET ret = mobi_serialize_mobiheader(buf, m, exthsize);
        if (ret != MOBI_SUCCESS) {
            buffer_free(buf);
            return ret;
        }
        if (m->eh) {
            ret = mobi_serialize_extheader(buf, m);
            if (ret != MOBI_SUCCESS) {
                buffer_free(buf);
                return ret;
            }
        }
        if (m->mh->full_name) {
            buffer_addstring(buf, m->mh->full_name);
            if (buf->error != MOBI_SUCCESS) {
                buffer_free(buf);
                return MOBI_DATA_CORRUPT;
            }
        }
    }
    
    buffer_addzeros(buf, padding);
    if (buf->error) {
        buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    
    MOBIPdbRecord *record0 = mobi_get_record_by_seqnumber(m, seqnumber);
    if (record0 == NULL) {
        debug_print("%s", "Record 0 not initialized\n");
        buffer_free(buf);
        return MOBI_DATA_CORRUPT;
    }
    unsigned char *data = malloc(buf->offset);
    if (data == NULL) {
        buffer_free(buf);
        return MOBI_MALLOC_FAILED;
    }
    memcpy(data, buf->data, buf->offset);
    record0->size = buf->offset;
    buffer_free(buf);
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
    while (curr) {
        if (offset > UINT32_MAX) {
            return MOBI_DATA_CORRUPT;
        }
        MOBIBuffer *buf = buffer_init(PALMDB_RECORD_INFO_SIZE);
        if (buf == NULL) {
            return MOBI_MALLOC_FAILED;
        }
        buffer_add32(buf, (uint32_t) offset);
        offset += curr->size;
        buffer_add8(buf, curr->attributes);
        const uint8_t h = (uint8_t) ((curr->uid & 0xff0000U) >> 16);
        const uint16_t l = (uint16_t) (curr->uid & 0xffffU);
        buffer_add8(buf, h);
        buffer_add16(buf, l);
        if (buf->error != MOBI_SUCCESS) {
            buffer_free(buf);
            return MOBI_DATA_CORRUPT;
        }
        MOBI_RET ret = mobi_write_buffer(file, buf);
        buffer_free(buf);
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
