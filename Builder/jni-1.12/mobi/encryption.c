/** @file encryption.c
 *  @brief Functions to handle encryption
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

/* PC1 routines adapted from:

 * File PC1DEC.c
 * written in Borland Turbo C 2.0 on PC
 * PC1 Cipher Algorithm ( Pukall Cipher 1 )
 * By Alexander PUKALL 1991
 * free code no restriction to use
 * please include the name of the Author in the final software

 * Mobi encryption algorithm learned from:
 
 * mobidedrm.py
 * Copyright © 2008 The Dark Reverser
 */

#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "util.h"
#include "debug.h"
#include "sha1.h"
#include "encryption.h"

#define KEYVEC1 ((unsigned char*) "\x72\x38\x33\xb0\xb4\xf2\xe3\xca\xdf\x09\x01\xd6\xe2\xe0\x3f\x96")
#define KEYVEC1_V1 ((unsigned char*) "QDCVEPMU675RUBSZ")
#define PIDSIZE 10
#define SERIALSIZE 16
#define SERIALLONGSIZE 40
#define KEYSIZE 16
#define COOKIESIZE 32
#define pk1_swap(a, b) { uint16_t tmp = a; a = b; b = tmp; }

/**
 @brief Structure for PK1 routines
 */
typedef struct {
    uint16_t si, x1a2, x1a0[8];
} PK1;

/**
 @brief Helper function for PK1 encryption/decryption
 
 @param[in,out] pk1 PK1 structure
 @param[in] i Iteration number
 @return PK1 inter
 */
static uint16_t pk1_code(PK1 *pk1, const uint8_t i) {
    uint16_t dx = pk1->x1a2 + i;
    uint16_t ax = pk1->x1a0[i];
    uint16_t cx = 0x015a;
    uint16_t bx = 0x4e35;
    pk1_swap(ax, pk1->si);
    pk1_swap(ax, dx);
    if (ax) { ax *= bx; }
    pk1_swap(ax, cx);
    if (ax) {
        ax *= pk1->si;
        cx += ax;
    }
    pk1_swap(ax, pk1->si);
    ax *= bx;
    dx += cx;
    ax += 1;
    pk1->x1a2 = dx;
    pk1->x1a0[i] = ax;
    return ax ^ dx;
}

/**
 @brief Helper function for PK1 encryption/decryption
 
 @param[in,out] pk1 PK1 structure
 @param[in] key 128-bit key
 @return PK1 inter
 */
static uint16_t pk1_assemble(PK1 *pk1, const unsigned char key[KEYSIZE]) {
    pk1->x1a0[0] = (key[0] * 256) + key[1];
    uint16_t inter = pk1_code(pk1, 0);
    for (uint8_t i = 1; i < (KEYSIZE / 2); i++) {
        pk1->x1a0[i] = pk1->x1a0[i - 1] ^ ((key[i * 2] * 256) + key[i * 2 + 1]);
        inter ^= pk1_code(pk1, i);
    }
    return inter;
}

/**
 @brief Decrypt buffer with PK1 algorithm
 
 @param[in,out] out Decrypted buffer
 @param[in] in Encrypted buffer
 @param[in] length Buffer length
 @param[in] key Key
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_pk1_decrypt(unsigned char *out, const unsigned char *in, size_t length, const unsigned char key[KEYSIZE]) {
    if (!out || !in) {
        return MOBI_INIT_FAILED;
    }
    unsigned char key_copy[KEYSIZE];
    memcpy(key_copy, key, KEYSIZE);
    PK1 *pk1 = calloc(1, sizeof(PK1));
    while (length--) {
        uint16_t inter = pk1_assemble(pk1, key_copy);
        uint8_t cfc = inter >> 8;
        uint8_t cfd = inter & 0xff;
        uint8_t c = *in++;
        c ^= (cfc ^ cfd);
        for (size_t i = 0; i < KEYSIZE; i++) {
            key_copy[i] ^= c;
        }
        *out++ = c;
    }
    free(pk1);
    return MOBI_SUCCESS;
}

/**
 @brief Encrypt buffer with PK1 algorithm
 
 @param[in,out] out Decrypted buffer
 @param[in] in Encrypted buffer
 @param[in] length Buffer length
 @param[in] key Key
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_pk1_encrypt(unsigned char *out, const unsigned char *in, size_t length, const unsigned char key[KEYSIZE]) {
    if (!out || !in) {
        return MOBI_INIT_FAILED;
    }
    PK1 *pk1 = calloc(1, sizeof(PK1));
    unsigned char k[KEYSIZE];
    memcpy(k, key, KEYSIZE);
    while (length--) {
        uint16_t inter = pk1_assemble(pk1, k);
        uint8_t cfc = inter >> 8;
        uint8_t cfd = inter & 0xff;
        uint8_t c = *in++;
        for (size_t i = 0; i < KEYSIZE; i++) {
            k[i] ^= c;
        }
        c ^= (cfc ^ cfd);
        *out++ = c;
    }
    free(pk1);
    return MOBI_SUCCESS;
}

/**
 @brief Structure for parsed drm record in Record0 header
 */
typedef struct {
    uint32_t verification, size, type;
    uint8_t checksum;
    unsigned char *cookie;
} MOBIDrm;

/**
 @brief Read drm records from Record0 header
 
 @param[in,out] drm MOBIDrm structure will hold parsed data
 @param[in] m MOBIData structure with raw data and metadata
 @return Number of parsed records
 */
static size_t mobi_drm_parse(MOBIDrm **drm, const MOBIData *m) {
    if (!m || !m->mh) {
        return 0;
    }
    uint32_t offset = *m->mh->drm_offset;
    uint32_t count = *m->mh->drm_count;
    uint32_t size = *m->mh->drm_size;
    if (offset == MOBI_NOTSET || count == 0) {
        return 0;
    }
    /* First record */
    MOBIPdbRecord *rec = m->rec;
    MOBIBuffer *buf = buffer_init_null(rec->data, rec->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return 0;
    }
    if (offset + size > rec->size) {
        buffer_free_null(buf);
        return 0;
    }
    buffer_setpos(buf, offset);
    for (size_t i = 0; i < count; i++) {
        drm[i] = calloc(1, sizeof(MOBIDrm));
		if (drm[i] == NULL) {
			debug_print("%s\n", "Memory allocation failed");
			buffer_free_null(buf);
			return 0;
		}
        drm[i]->verification = buffer_get32(buf);
        drm[i]->size = buffer_get32(buf);
        drm[i]->type = buffer_get32(buf);
        drm[i]->checksum = buffer_get8(buf);
        buffer_seek(buf, 3);
        drm[i]->cookie = buffer_getpointer(buf, COOKIESIZE);
    }
    buffer_free_null(buf);
    return count;
}

/**
 @brief Calculate checksum for key
 
 @param[in] key Key
 @return Checksum
 */
static uint8_t mobi_drm_keychecksum(const unsigned char key[KEYSIZE]) {
    size_t sum = 0;
    for (size_t i = 0; i < KEYSIZE; i++) {
        sum += key[i];
    }
    return (uint8_t) sum;
}

/**
 @brief Free MOBIDrm structure
 
 @param[in] drm MOBIDrm structure
 */
static void mobi_drm_free(MOBIDrm **drm, const size_t count) {
    for (size_t i = 0; i < count; i++) {
        free(drm[i]);
    }
    free(drm);
}

/**
 @brief Check whether drm expired
 
 @param[in] from Cookie valid after (unix timestamp in minutes)
 @param[in] to Cookie valid before (unix timestamp in minutes)
 @return True if drm validation dates are present and expired, false otherwise
 */
static bool mobi_drm_expired(const uint32_t from, const uint32_t to) {
    if (from == 0 || to == (uint32_t) -1) {
        /* expiration dates not set */
        return false;
    }
    /* FIXME: needs sample for testing */
    struct tm epoch;
    memset(&epoch, 0, sizeof(epoch));
    epoch.tm_year = 70; epoch.tm_mday = 1; epoch.tm_isdst = -1;
    time_t now = time(NULL);
    double curr = difftime(now, mktime(&epoch)) / 60;
    if (curr < 0 || curr > UINT32_MAX || (uint32_t) curr < from || (uint32_t) curr > to) {
        debug_print("Drm expired (%u), valid period %u-%u\n", (uint32_t) curr, from, to);
        return true;
    } else {
        debug_print("Drm valid (%u), valid period %u-%u\n", (uint32_t) curr, from, to);
        return false;
    }
}

/**
 @brief Verify decrypted cookie
 
 @param[in] drm_verification Checksum from drm header
 @param[in] cookie Decrypted cookie
 @param[in] key_type Key type
 @return True if verification succeeds, false otherwise
 */
static bool mobi_drm_verify(const uint32_t drm_verification, const unsigned char cookie[COOKIESIZE], const uint8_t key_type) {
    uint32_t verification = mobi_get32be(&cookie[0]);
    uint32_t flags = mobi_get32be(&cookie[4]);
    if (verification == drm_verification && (flags & 0x1f) == key_type) {
        uint32_t to = mobi_get32be(&cookie[24]);
        uint32_t from = mobi_get32be(&cookie[28]);
        return !mobi_drm_expired(from, to);
    }
    return false;
}

/**
 @brief Get key corresponding to given pid (encryption type 2)
 
 @param[in,out] key Key
 @param[in] pid PID
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_getkey_v2(unsigned char key[KEYSIZE], const unsigned char *pid, const MOBIData *m) {
    unsigned char pid_nocrc[KEYSIZE] = "\0";
    memcpy(pid_nocrc, pid, PIDSIZE - 2);
    unsigned char tempkey[KEYSIZE];
    MOBI_RET ret = mobi_pk1_encrypt(tempkey, pid_nocrc, KEYSIZE, KEYVEC1);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    uint8_t tempkey_checksum = mobi_drm_keychecksum(tempkey);
    /* default key, no pid required */
    uint8_t keyvec1_checksum = mobi_drm_keychecksum(KEYVEC1);
    MOBIDrm **drm = malloc(*m->mh->drm_count * sizeof(MOBIDrm*));
    if (drm == NULL) {
        debug_print("Memory allocation failed%s", "\n")
        return MOBI_MALLOC_FAILED;
    }
    size_t drm_count = mobi_drm_parse(drm, m);
    for (size_t i = 0; i < drm_count; i++) {
        if (drm[i]->checksum == tempkey_checksum) {
            unsigned char cookie[COOKIESIZE];
            ret = mobi_pk1_decrypt(cookie, drm[i]->cookie, COOKIESIZE, tempkey);
            if (ret != MOBI_SUCCESS) {
                mobi_drm_free(drm, drm_count);
                return ret;
            }
            if (mobi_drm_verify(drm[i]->verification, cookie, 1)) {
                memcpy(key, &cookie[8], KEYSIZE);
                mobi_drm_free(drm, drm_count);
                return MOBI_SUCCESS;
            }
        } else if (drm[i]->checksum == keyvec1_checksum) {
            /* try to decrypt with KEYVEC1 */
            unsigned char cookie[COOKIESIZE];
            ret = mobi_pk1_decrypt(cookie, drm[i]->cookie, COOKIESIZE, KEYVEC1);
            if (ret != MOBI_SUCCESS) {
                mobi_drm_free(drm, drm_count);
                return ret;
            }
            /* FIXME: needs sample to verify if key_type 3 is ok */
            if (mobi_drm_verify(drm[i]->verification, cookie, 3)) {
                memcpy(key, &cookie[8], KEYSIZE);
                mobi_drm_free(drm, drm_count);
                return MOBI_SUCCESS;
            }
        }
    }
    mobi_drm_free(drm, drm_count);
    return MOBI_DRM_KEYNOTFOUND;
}

/**
 @brief Get key corresponding for encryption type 1
 
 @param[in,out] key Key
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_getkey_v1(unsigned char key[KEYSIZE], const MOBIData *m) {
    if (m == NULL || m->ph == NULL) {
        return MOBI_DATA_CORRUPT;
    }
    /* First record */
    MOBIPdbRecord *rec = m->rec;
    MOBIBuffer *buf = buffer_init_null(rec->data, rec->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    if (strcmp(m->ph->type, "TEXt") == 0 && strcmp(m->ph->creator, "REAd") == 0) {
        /* offset 14 */
        buffer_setpos(buf, 14);
    } else if (m->mh == NULL || m->mh->version == NULL || *m->mh->version == MOBI_NOTSET) {
        /* offset 144 */
        buffer_setpos(buf, 144);
    } else {
        /* offset header + 16 */
        if (m->mh == NULL) {
            return MOBI_DATA_CORRUPT;
        }
        buffer_setpos(buf, *m->mh->header_length + 16);
    }
    unsigned char key_enc[KEYSIZE];
    buffer_getraw(key_enc, buf, KEYSIZE);
    buffer_free_null(buf);
    MOBI_RET ret = mobi_pk1_decrypt(key, key_enc, KEYSIZE, KEYVEC1_V1);
    return ret;
}

/**
 @brief Get key corresponding to given pid
 
 @param[in,out] key Key
 @param[in] pid PID
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_getkey(unsigned char key[KEYSIZE], const unsigned char *pid, const MOBIData *m) {
    MOBI_RET ret;
    if (m->rh && m->rh->encryption_type == RECORD0_OLD_ENCRYPTION) {
        ret = mobi_drm_getkey_v1(key, m);
    } else {
        if (pid[0] == '\0') {
            return MOBI_INIT_FAILED;
        }
        ret = mobi_drm_getkey_v2(key, pid, m);
    }
    return ret;
}

/**
 @brief Decrypt buffer with PK1 algorithm
 
 @param[in,out] out Decrypted buffer
 @param[in] in Encrypted buffer
 @param[in] length Buffer length
 @param[in] m MOBIData structure with loaded key
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_decrypt_buffer(unsigned char *out, const unsigned char *in, const size_t length, const MOBIData *m) {
    if (m == NULL || m->drm_key == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBI_RET ret = mobi_pk1_decrypt(out, in, length, m->drm_key);
    return ret;
}

/**
 @brief Calculate pid checksum
 
 @param[in] pid Pid
 @param[out] checksum Calculated checksum
 */
static void mobi_drm_pidchecksum(char checksum[2], const unsigned char *pid) {
    const char map[] = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
    const uint8_t map_length = sizeof(map) - 1;
    uint32_t crc = (uint32_t) ~m_crc32(0xffffffff, pid, PIDSIZE - 2);
    crc ^= (crc >> 16);
    for (size_t i = 0; i < 2; i++){
        uint8_t b = crc & 0xff;
        uint8_t pos = (b / map_length) ^ (b % map_length);
        checksum[i] = map[pos % map_length];
        crc >>= 8;
    }
}

/**
 @brief Verify PID
 
 @param[in] pid PID
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_pidverify(const unsigned char *pid) {
    char checksum[2] = "\0";
    mobi_drm_pidchecksum(checksum, pid);
    if (memcmp(checksum, &pid[PIDSIZE - 2], 2) == 0) {
        return MOBI_SUCCESS;
    }
    return MOBI_DRM_PIDINV;
}

/**
 @brief Decrypt buffer with PK1 algorithm
 
 @param[in] serial Device serial number
 @param[out] pid Pid
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_devicepid_from_serial(char pid[PIDSIZE + 1], const char *serial) {
    memset(pid, 0, PIDSIZE + 1);
    const size_t serial_length = strlen(serial);
    if (serial_length != SERIALSIZE && serial_length != SERIALLONGSIZE) {
        debug_print("Wrong serial length: %zu\n", serial_length);
        return MOBI_DRM_PIDINV;
    }
    const char map[] = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
    unsigned char out[PIDSIZE - 2] = "\0";
    size_t out_length = sizeof(out);
    if (serial_length == SERIALSIZE) {
        out_length--;
        pid[out_length] = '*';
    }
    uint32_t crc = (uint32_t) ~m_crc32(0xffffffff, (const unsigned char *) serial, (unsigned int) serial_length);
    for (size_t i = 0; i < serial_length; i++) {
        out[i % out_length] ^= serial[i];
    }
    for (size_t i = 0; i < out_length; i++) {
        uint8_t b = crc >> (24 - 8 * (i & 3)) & 0xff;
        out[i] ^= b;
        uint8_t pos = (out[i] >> 7) + ((out[i] >> 5 & 3) ^ (out[i] & 0x1f));
        /* pos max is 32 < map size */
        pid[i] = map[pos];
    }
    char checksum[2] = "\0";
    mobi_drm_pidchecksum(checksum, (unsigned char *) pid);
    memcpy(&pid[PIDSIZE - 2], checksum, 2);
    return MOBI_SUCCESS;
}

/**
 @brief Drm components extracted from EXTH records
 */
typedef struct {
    unsigned char *data; /**< EXTH_TAMPERKEYS record data */
    unsigned char *token; /**< Drm token */
    size_t data_size; /**< Data size */
    size_t token_size;  /**< Token size */
} EXTHDrm;

/**
 @brief Get drm components from EXTH records
 
 Must be deallocated after use with mobi_exthdrm_free()
 
 @param[in] m MOBIData structure
 @return EXTHDrm structure (NULL or error)
 */
EXTHDrm * mobi_exthdrm_get(const MOBIData *m) {
    if (m == NULL || m->eh == NULL) {
        return NULL;
    }
    const MOBIExthHeader *meta = mobi_get_exthrecord_by_tag(m, EXTH_TAMPERKEYS);
    if (meta == NULL) {
        return NULL;
    }
    MOBIBuffer *buf = buffer_init_null(meta->data, meta->size);
    if (buf == NULL) {
        return NULL;
    }
    const MOBIExthHeader *submeta[10];
    size_t submeta_count = 0;
    size_t submeta_total = 0;
    while (buf->offset < buf->maxlen && submeta_count < ARRAYSIZE(submeta)) {
        buffer_seek(buf, 1);
        uint32_t exth_key = buffer_get32(buf);
        const MOBIExthHeader *sub = mobi_get_exthrecord_by_tag(m, exth_key);
        if (sub) {
            submeta[submeta_count++] = sub;
            submeta_total += sub->size;
        }
    }
    if (submeta_total == 0) {
        buffer_free_null(buf);
        return NULL;
    }
    unsigned char *token = malloc(submeta_total);
    if (token == NULL) {
        buffer_free_null(buf);
        return NULL;
    }
    unsigned char *p = token;
    for (size_t i = 0; i < submeta_count; i++) {
        memcpy(p, submeta[i]->data, submeta[i]->size);
        p += submeta[i]->size;
    }
    EXTHDrm *exth_drm = malloc(sizeof(EXTHDrm));
    if (exth_drm) {
        exth_drm->data = meta->data;
        exth_drm->data_size = meta->size;
        exth_drm->token = token;
        exth_drm->token_size = submeta_total;
    } else {
        free(token);
    }
    buffer_free_null(buf);
    return exth_drm;
}

/**
 @brief Free EXTHDrm structure
 
 @param[in,out] exth_drm EXTHDrm to be freed
 */
void mobi_exthdrm_free(EXTHDrm **exth_drm) {
    if (*exth_drm) {
        free((*exth_drm)->token);
        (*exth_drm)->token = NULL;
    }
    free(*exth_drm);
    *exth_drm = NULL;
}

/**
 @brief Calculate SHA-1 hash for given message
 
 @param[in] message Message
 @param[in] message_length Message length
 @param[out] hash Hash
 */
static void mobi_SHA1(unsigned char hash[SHA1_DIGEST_SIZE], size_t message_length, const unsigned char *message) {
    SHA1_CTX ctx;
    SHA1_Init(&ctx);
    SHA1_Update(&ctx, message, message_length);
    SHA1_Final(&ctx, hash);
}

/**
 @brief Calculate book pid from device serial
 
 @param[in] serial Device serial number
 @param[in] m MOBIData structure
 @param[out] pid Pid
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_bookpid_from_serial(char pid[PIDSIZE + 1], const MOBIData *m, const char *serial) {
    memset(pid, 0, PIDSIZE + 1);
    const char map[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    EXTHDrm *exth_drm = mobi_exthdrm_get(m);
    if (exth_drm == NULL) {
        return MOBI_DRM_PIDINV;
    }
    const size_t serial_length = strlen(serial);
    size_t message_length = serial_length + exth_drm->token_size + exth_drm->data_size;
    unsigned char *message = malloc(message_length);
    if (message == NULL) {
        mobi_exthdrm_free(&exth_drm);
        return MOBI_MALLOC_FAILED;
    }
    memcpy(message, serial, serial_length);
    memcpy(&message[serial_length], exth_drm->data, exth_drm->data_size);
    memcpy(&message[serial_length + exth_drm->data_size], exth_drm->token, exth_drm->token_size);
    mobi_exthdrm_free(&exth_drm);

    unsigned char hash[SHA1_DIGEST_SIZE];
    mobi_SHA1(hash, message_length, message);
    free(message);

    int bytes = 8;
    uint64_t val = 0;
    unsigned char *ptr = hash;
    while (bytes--) {
        val |= (uint64_t) *ptr++ << (bytes * 8);
    }
    for (size_t i = 0; i < 8; i++) {
        uint8_t pos = (uint64_t) val >> 58;
        val <<= 6;
        pid[i] = map[pos];
    }
    char checksum[2] = "\0";
    mobi_drm_pidchecksum(checksum, (unsigned char *) pid);
    memcpy(&pid[PIDSIZE - 2], checksum, 2);
    return MOBI_SUCCESS;
}

/**
 @brief Store key for encryption in MOBIData stucture. 
 Pid will be calculated from device serial number.
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] serial Serial
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_setkey_serial_internal(MOBIData *m, const char *serial) {
    char pid[PIDSIZE + 1];
    MOBI_RET ret = mobi_drm_devicepid_from_serial(pid, serial);
    debug_print("Device pid: %s\n", pid);
    if (ret == MOBI_SUCCESS && mobi_drm_setkey_internal(m, pid) == MOBI_SUCCESS) {
        return MOBI_SUCCESS;
    }
    ret = mobi_drm_bookpid_from_serial(pid, m, serial);
    debug_print("Book pid: %s\n", pid);
    if (ret == MOBI_SUCCESS) {
        return mobi_drm_setkey_internal(m, pid);
    }
    return MOBI_DRM_PIDINV;
}

/**
 @brief Store key for encryption in MOBIData stucture
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] pid PID
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_setkey_internal(MOBIData *m, const char *pid) {
    if (m == NULL || m->rh == NULL) {
        return MOBI_INIT_FAILED;
    }
    MOBI_RET ret;
    if (!mobi_is_encrypted(m)) {
        debug_print("Document not encrypted%s", "\n");
        return MOBI_SUCCESS;
    }
    unsigned char drm_pid[PIDSIZE] = "\0";
    if (m->rh->encryption_type > 1) {
        if (pid == NULL) {
            return MOBI_INIT_FAILED;
        }
        const size_t pid_length = strlen(pid);
        if (pid_length != PIDSIZE) {
            debug_print("PID size is wrong (%zu)\n", pid_length);
            return MOBI_DRM_PIDINV;
        }
        memcpy(drm_pid, pid, PIDSIZE);
        ret = mobi_drm_pidverify(drm_pid);
        if (ret != MOBI_SUCCESS) {
            debug_print("PID is invalid%s", "\n")
            return ret;
        }
    } else {
        /* PID not needed */
        debug_print("Encryption doesn't require PID%s", "\n");
    }
    unsigned char key[KEYSIZE];
    ret = mobi_drm_getkey(key, drm_pid, m);
    if (ret != MOBI_SUCCESS) {
        debug_print("Key not found%s", "\n")
        return ret;
    }
    if (m->drm_key == NULL) {
        m->drm_key = malloc(KEYSIZE);
        if (m->drm_key == NULL) {
            debug_print("Memory allocation failed%s", "\n")
            return MOBI_MALLOC_FAILED;
        }
    }
    memcpy(m->drm_key, key, KEYSIZE);
    return MOBI_SUCCESS;
}

/**
 @brief Remove key from MOBIData structure
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_delkey_internal(MOBIData *m) {
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    if (m->drm_key) {
        free(m->drm_key);
    }
    m->drm_key = NULL;
    return MOBI_SUCCESS;
}

/**
 @brief Mark document as decrypted
 
 @param[in,out] m MOBIData structure with raw data and metadata
 */
void mobi_drm_unset(MOBIData *m) {
    if (m->rh->encryption_type == RECORD0_MOBI_ENCRYPTION && m->mh) {
        if (m->mh->drm_offset) {
            *m->mh->drm_offset = MOBI_NOTSET;
        }
        if (m->mh->drm_size) {
            *m->mh->drm_size = 0;
        }
        if (m->mh->drm_count) {
            *m->mh->drm_count = 0;
        }
        if (m->mh->drm_flags) {
            *m->mh->drm_flags = 0;
        }
    }
    m->rh->encryption_type = RECORD0_NO_ENCRYPTION;
    free(m->drm_key);
    m->drm_key = NULL;
}

/**
 @brief Decrypt document.
 
 It is not necessary to call this function in order to parse encrypted document.
 If pid is set document will be decrypted automatically during uncompression.
 The reason for this function is to load and resave decrypted document without parsing.
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_decrypt(MOBIData *m) {
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    if (!mobi_is_encrypted(m)) {
        debug_print("%s\n", "Document not encrypted");
        return MOBI_SUCCESS;
    }
    if (m->rh == NULL || m->rh->text_record_count == 0) {
        debug_print("%s\n", "No records to decrypt");
        return MOBI_SUCCESS;
    }
    if (m->drm_key == NULL && m->rh->encryption_type == RECORD0_OLD_ENCRYPTION) {
        /* try to set key for encryption type 1 */
        printf("Trying to set key for encryption type 1\n");
        mobi_drm_setkey(m, NULL);
    }
    if (m->drm_key == NULL) {
        debug_print("%s\n", "Missing decryption key\n");
        return MOBI_DRM_KEYNOTFOUND;
    }
    /* FIXME: hybrid files are not encrypted, are they? */
    const size_t text_rec_index = 1;
    size_t text_rec_count = m->rh->text_record_count;
    const uint16_t compression_type = m->rh->compression_type;
    uint16_t extra_flags = 0;
    if (m->mh && m->mh->extra_flags) {
        extra_flags = *m->mh->extra_flags;
    }
    /* get first text record */
    const MOBIPdbRecord *curr = mobi_get_record_by_seqnumber(m, text_rec_index);
    
    while (text_rec_count-- && curr) {
        size_t extra_size = 0;
        if (extra_flags) {
            extra_size = mobi_get_record_extrasize(curr, extra_flags);
            if (extra_size == MOBI_NOTSET || extra_size >= curr->size) {
                return MOBI_DATA_CORRUPT;
            }
        }
        const size_t record_size = curr->size - extra_size;
        unsigned char *decrypted = malloc(record_size);
        if (decrypted == NULL) {
            debug_print("Memory allocation failed%s", "\n");
            return MOBI_MALLOC_FAILED;
        }
        MOBI_RET ret = MOBI_SUCCESS;
        size_t decrypt_size = record_size;
        if (compression_type != RECORD0_HUFF_COMPRESSION) {
            /* decrypt also multibyte extra data */
            size_t mb_size = mobi_get_record_mb_extrasize(curr, extra_flags);
            decrypt_size += mb_size;
        }
        ret = mobi_drm_decrypt_buffer(decrypted, curr->data, decrypt_size, m);
        if (ret != MOBI_SUCCESS) {
            free(decrypted);
            return ret;
        }
        memcpy(curr->data, decrypted, record_size);
        free(decrypted);
        curr = curr->next;
    }
    
    mobi_drm_unset(m);
    
    return MOBI_SUCCESS;
}
