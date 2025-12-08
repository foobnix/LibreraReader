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
 * Copyright (c) 2008 The Dark Reverser
 */

#include <string.h>
#include <stdlib.h>
#include <time.h>
#include "util.h"
#include "debug.h"
#include "randombytes.h"
#include "sha1.h"
#include "encryption.h"

#define INTERNAL_READER_KEY ((unsigned char*) "\x72\x38\x33\xb0\xb4\xf2\xe3\xca\xdf\x09\x01\xd6\xe2\xe0\x3f\x96")
#define INTERNAL_PUBLISHER_KEY ((unsigned char*) "\x95\xda\x7b\xed\x90\x5e\x10\x2e\x44\x4c\xb5\xe5\xc0\x25\xdf\x2c")
#define INTERNAL_READER_KEY_V1 ((unsigned char*) "QDCVEPMU675RUBSZ")
#define PIDSIZE 10
#define SERIALSIZE 16
#define SERIALLONGSIZE 40
#define KEYSIZE 16
#define COOKIESIZE 32
#define VOUCHERSIZE 48
#define VOUCHERS_COUNT_MAX 1024
#define VOUCHERS_SIZE_MIN 288
#define pk1_swap(a, b) { uint16_t tmp = a; (a) = b; (b) = tmp; }

/**
 @brief Structure for PK1 routines
 */
typedef struct {
    uint16_t si, x1a2, x1a0[8];
} MOBIPk1;


/**
 @brief Structure for parsed drm record in record 0 header
 */
typedef struct {
    uint32_t verification; /**< Verification checksum */
    uint32_t size; /**< Voucher size */
    uint32_t type; /**< Voucher type */
    uint8_t checksum; /**< Temporary key checksum */
    unsigned char *cookie; /**< Encrypted part contains key, verificationś, expiration dates, flags */
} MOBIVoucher;

/**
 @brief Drm components extracted from EXTH records
 */
typedef struct {
    unsigned char *data; /**< EXTH_TAMPERKEYS record data */
    unsigned char *token; /**< Drm token */
    size_t data_size; /**< Data size */
    size_t token_size;  /**< Token size */
} MOBIExthDrm;

/**
 @brief Helper function for PK1 encryption/decryption
 
 @param[in,out] pk1 PK1 structure
 @param[in] i Iteration number
 @return PK1 inter
 */
static uint16_t mobi_pk1_code(MOBIPk1 *pk1, const uint8_t i) {
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
static uint16_t mobi_pk1_assemble(MOBIPk1 *pk1, const unsigned char key[KEYSIZE]) {
    pk1->x1a0[0] = (key[0] * 256) + key[1];
    uint16_t inter = mobi_pk1_code(pk1, 0);
    for (uint8_t i = 1; i < (KEYSIZE / 2); i++) {
        pk1->x1a0[i] = pk1->x1a0[i - 1] ^ ((key[i * 2] * 256) + key[i * 2 + 1]);
        inter ^= mobi_pk1_code(pk1, i);
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
    MOBIPk1 *pk1 = calloc(1, sizeof(MOBIPk1));
    while (length--) {
        uint16_t inter = mobi_pk1_assemble(pk1, key_copy);
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
    MOBIPk1 *pk1 = calloc(1, sizeof(MOBIPk1));
    unsigned char key_copy[KEYSIZE];
    memcpy(key_copy, key, KEYSIZE);
    while (length--) {
        uint16_t inter = mobi_pk1_assemble(pk1, key_copy);
        uint8_t cfc = inter >> 8;
        uint8_t cfd = inter & 0xff;
        uint8_t c = *in++;
        for (size_t i = 0; i < KEYSIZE; i++) {
            key_copy[i] ^= c;
        }
        c ^= (cfc ^ cfd);
        *out++ = c;
    }
    free(pk1);
    return MOBI_SUCCESS;
}


/**
 @brief Initialize DRM structure
 
 @return Number of parsed records
 */
static MOBIDrm * mobi_drm_init(void) {
    
    MOBIDrm *drm = calloc(1, sizeof(MOBIDrm));
    if (drm == NULL) {
        debug_print("%s", "Memory allocation for drm structure failed\n");
    }
    return drm;
}

/**
 @brief Free DRM cookie structure
 
 @param[in,out] cookie Cookie
 */
static void mobi_free_cookie(MOBICookie *cookie) {
    if (cookie) {
        if (cookie->pid) {
            free(cookie->pid);
            cookie->pid = NULL;
        }
        free(cookie);
    }
}

/**
 @brief Free DRM structure
 
 @param[in,out] m MOBIData structure with raw data and metadata
 */
void mobi_free_drm(MOBIData *m) {
    if (m->internals) {
        MOBIDrm *drm = m->internals;
        if (drm->key) {
            free(drm->key);
        }
        drm->key = NULL;
        if (drm->cookies) {
            while (drm->cookies_count--) {
                mobi_free_cookie(drm->cookies[drm->cookies_count]);
            }
            free(drm->cookies);
        }
        drm->cookies = NULL;
        free(m->internals);
        m->internals = NULL;
    }
    
}

/**
 @brief Initialize DRM data with given key
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] key Key
 @return Number of parsed records
 */
static MOBI_RET mobi_drmkey_init(MOBIData *m, const unsigned char key[KEYSIZE]) {
    if (m->internals == NULL && (m->internals = mobi_drm_init()) == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    MOBIDrm *drm = m->internals;
    if (drm->key == NULL) {
        drm->key = malloc(KEYSIZE);
        if (drm->key == NULL) {
            debug_print("Memory allocation failed%s", "\n");
            return MOBI_MALLOC_FAILED;
        }
    }
    memcpy(drm->key, key, KEYSIZE);
    // remove in future versions, kept for backwards compatibility
    m->drm_key = drm->key;
    
    debug_print("m->drm->key: %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x\n",
                drm->key[0], drm->key[1], drm->key[2], drm->key[3],
                drm->key[4], drm->key[5], drm->key[6], drm->key[7],
                drm->key[8], drm->key[9], drm->key[10], drm->key[11],
                drm->key[12], drm->key[13], drm->key[14], drm->key[15]);
    
    return MOBI_SUCCESS;
}

/**
 @brief Read drm records from Record0 header
 
 @param[in,out] drm MOBIDrm structure will hold parsed data
 @param[in] m MOBIData structure with raw data and metadata
 @return Number of parsed records
 */
static size_t mobi_vouchers_get(MOBIVoucher **drm, const MOBIData *m) {
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
    MOBIBuffer *buf = mobi_buffer_init_null(rec->data, rec->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return 0;
    }
    if (offset + size > rec->size) {
        mobi_buffer_free_null(buf);
        return 0;
    }
    mobi_buffer_setpos(buf, offset);
    debug_print("%s\n", "Parsing DRM data from record 0");
    for (size_t i = 0; i < count; i++) {
        drm[i] = calloc(1, sizeof(MOBIVoucher));
        if (drm[i] == NULL) {
            debug_print("%s\n", "Memory allocation failed");
            mobi_buffer_free_null(buf);
            for (size_t j = 0; j < i; j++) {
                free(drm[j]);
            }
            return 0;
        }
        drm[i]->verification = mobi_buffer_get32(buf);
        drm[i]->size = mobi_buffer_get32(buf);
        drm[i]->type = mobi_buffer_get32(buf);
        drm[i]->checksum = mobi_buffer_get8(buf);
        mobi_buffer_seek(buf, 3);
        drm[i]->cookie = mobi_buffer_getpointer(buf, COOKIESIZE);
        debug_print("drm[%zu] verification = %#x, size = %#x, type = %#x, checksum = %#x\n",
                    i, drm[i]->verification, drm[i]->size, drm[i]->type, drm[i]->checksum);
        if (buf->error != MOBI_SUCCESS) {
            debug_print("%s\n", "DRM data too short");
            mobi_buffer_free_null(buf);
            for (size_t j = 0; j < i + 1; j++) {
                free(drm[j]);
            }
            return 0;
        }
    }
    mobi_buffer_free_null(buf);
    return count;
}

/**
 @brief Calculate checksum for key
 
 @param[in] key Key
 @return Checksum
 */
static uint8_t mobi_drmkey_checksum(const unsigned char key[KEYSIZE]) {
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
static void mobi_free_vouchers(MOBIVoucher **drm, const size_t count) {
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
static bool mobi_drm_is_expired(const uint32_t from, const uint32_t to) {
    if (from == 0 || to == MOBI_NOTSET) {
        /* expiration dates not set */
        return false;
    }
#if (MOBI_DEBUG)
    time_t debug_from = (time_t) from * 60;
    time_t debug_to = (time_t) to * 60;
    debug_print("Drm validity period from %s", ctime(&debug_from));
    debug_print("                      to %s", ctime(&debug_to));
#endif
    size_t current_minutes = (size_t) time(NULL) / 60;
    if (current_minutes >= UINT32_MAX) {
        debug_print("Drm cannot be checked, current time outside valid range: %zu\n", current_minutes);
        return false;
    }
    if (current_minutes < from || current_minutes > to) {
        debug_print("%s\n", "Drm expired");
        return true;
    }
    return false;
}

/**
 @brief Verify decrypted cookie
 
 @param[in] drm_verification Checksum from drm header
 @param[in] cookie Decrypted cookie
 @param[in] key_type Key type
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_cookie_verify(const uint32_t drm_verification, const unsigned char cookie[COOKIESIZE], const uint8_t key_type) {
    uint32_t verification = mobi_get32be(&cookie[0]);
    uint32_t flags = mobi_get32be(&cookie[4]);
    debug_print("%s\n", "Parsing decrypted data from record 0");
    debug_print("verification = %#x, flags = %#x\n", verification, flags);
    if (verification == drm_verification && (flags & 0x1f) == key_type) {
        uint32_t to = mobi_get32be(&cookie[24]);
        uint32_t from = mobi_get32be(&cookie[28]);
        debug_print("from = %#x, to = %#x\n", from, to);
        if (mobi_drm_is_expired(from, to)) {
            return MOBI_DRM_EXPIRED;
        }
        return MOBI_SUCCESS;
    }
    return MOBI_DRM_KEYNOTFOUND;
}

/**
 @brief Get key corresponding to encryption type 2
 
 If PID is supplied main key is decrypted using PID key, otherwise internal reader key is used
 
 @param[in,out] key Key
 @param[in] pid PID, NULL if not set
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmkey_get_v2(unsigned char key[KEYSIZE], const unsigned char *pid, const MOBIData *m) {
    
    MOBI_RET ret;
    unsigned char *device_key = NULL;
    unsigned char pidkey[KEYSIZE] = "\0";
    if (pid) {
        memcpy(pidkey, pid, PIDSIZE - 2);

        ret = mobi_pk1_encrypt(pidkey, pidkey, KEYSIZE, INTERNAL_READER_KEY);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
        device_key = pidkey;
    }

    MOBIVoucher **drm = malloc(*m->mh->drm_count * sizeof(MOBIVoucher*));
    if (drm == NULL) {
        debug_print("Memory allocation failed%s", "\n");
        return MOBI_MALLOC_FAILED;
    }
    size_t drm_count = mobi_vouchers_get(drm, m);
    bool key_expired = false;
    for (size_t i = 0; i < drm_count; i++) {
        /* Try 1 uses pid key, try 2 - default key */
        size_t tries = 2;
        while (tries) {
            const unsigned char *try_key = (tries == 2) ? device_key : INTERNAL_READER_KEY;
            const uint8_t try_type = (tries == 2) ? 1 : 3;
            
            if (try_key && drm[i]->checksum == mobi_drmkey_checksum(try_key)) {

                unsigned char cookie[COOKIESIZE];
                ret = mobi_pk1_decrypt(cookie, drm[i]->cookie, COOKIESIZE, try_key);
                if (ret != MOBI_SUCCESS) {
                    // fatal error
                    mobi_free_vouchers(drm, drm_count);
                    return ret;
                }

                ret = mobi_cookie_verify(drm[i]->verification, cookie, try_type);
                if (ret == MOBI_SUCCESS) {
                    memcpy(key, &cookie[8], KEYSIZE);
                    mobi_free_vouchers(drm, drm_count);
                    if (tries == 2) {
                        debug_print("Cookie encrypted with pid key%s", "\n");
                    } else {
                        debug_print("Cookie encrypted with default key%s", "\n");
                    }
                    return MOBI_SUCCESS;
                }
                if (ret == MOBI_DRM_EXPIRED) {
                    key_expired = true;
                }
            }
            tries--;
        }
    }
    mobi_free_vouchers(drm, drm_count);
    return key_expired ? MOBI_DRM_EXPIRED : MOBI_DRM_KEYNOTFOUND;
}

/**
 @brief Get key corresponding for encryption type 1
 
 Main key is encrypted with internal reader key (old version)
 
 @param[in,out] key Key
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmkey_get_v1(unsigned char key[KEYSIZE], const MOBIData *m) {
    if (m == NULL || m->ph == NULL) {
        return MOBI_DATA_CORRUPT;
    }
    /* First record */
    MOBIPdbRecord *rec = m->rec;
    MOBIBuffer *buf = mobi_buffer_init_null(rec->data, rec->size);
    if (buf == NULL) {
        debug_print("%s\n", "Memory allocation failed");
        return MOBI_MALLOC_FAILED;
    }
    size_t mobi_version = mobi_get_fileversion(m);
    
    if (mobi_version > 1) {
        /* offset mobi header + record 0 header (+ 12 in newer files) */
        if (m->mh == NULL || m->mh->header_length == NULL) {
            mobi_buffer_free_null(buf);
            return MOBI_DATA_CORRUPT;
        }
        size_t offset = 0;
        if (mobi_version > 2) {
            offset = 12;
        }
        mobi_buffer_setpos(buf, *m->mh->header_length + RECORD0_HEADER_LEN + offset);
    } else {
        /* offset 14 */
        mobi_buffer_setpos(buf, 14);
    }
    
    unsigned char key_enc[KEYSIZE];
    mobi_buffer_getraw(key_enc, buf, KEYSIZE);
    mobi_buffer_free_null(buf);
    MOBI_RET ret = mobi_pk1_decrypt(key, key_enc, KEYSIZE, INTERNAL_READER_KEY_V1);
    return ret;
}

/**
 @brief Get key corresponding to given pid
 
 @param[in,out] key Key
 @param[in] pid PID
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmkey_get(unsigned char key[KEYSIZE], const unsigned char *pid, const MOBIData *m) {
    MOBI_RET ret;
    if (m->rh && m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        ret = mobi_drmkey_get_v1(key, m);
    } else {
        ret = mobi_drmkey_get_v2(key, pid, m);
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
MOBI_RET mobi_buffer_decrypt(unsigned char *out, const unsigned char *in, const size_t length, const MOBIData *m) {
    if (m == NULL || !mobi_has_drmkey(m)) {
        return MOBI_INIT_FAILED;
    }
    MOBIDrm *drm = m->internals;
    return mobi_pk1_decrypt(out, in, length, drm->key);
}

/**
 @brief Encrypt buffer with PK1 algorithm
 
 @param[in,out] out Encrypted buffer
 @param[in] in Decrypted buffer
 @param[in] length Buffer length
 @param[in] m MOBIData structure with loaded key
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_buffer_encrypt(unsigned char *out, const unsigned char *in, const size_t length, const MOBIData *m) {
    if (m == NULL || !mobi_has_drmkey(m)) {
        return MOBI_INIT_FAILED;
    }
    
    MOBIDrm *drm = m->internals;
    return mobi_pk1_encrypt(out, in, length, drm->key);
}

/**
 @brief Calculate pid checksum
 
 @param[in] pid Pid
 @param[out] checksum Calculated checksum
 */
static void mobi_drmpid_checksum(char checksum[2], const unsigned char *pid) {
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
static MOBI_RET mobi_drmpid_verify(const unsigned char *pid) {
    char checksum[2] = "\0";
    mobi_drmpid_checksum(checksum, pid);
    if (memcmp(checksum, &pid[PIDSIZE - 2], 2) == 0) {
        return MOBI_SUCCESS;
    }
    debug_print("checksum: %x%x, pid: %x%x\n", checksum[0], checksum[1], pid[PIDSIZE - 2], pid[PIDSIZE - 1]);
    return MOBI_DRM_PIDINV;
}

/**
 @brief Verify serial number
 
 @param[in] serial Serial number
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_serial_verify(const char *serial) {
    const size_t serial_length = strlen(serial);
    if (serial_length != SERIALSIZE && serial_length != SERIALLONGSIZE) {
        debug_print("Wrong serial length: %zu\n", serial_length);
        return MOBI_DRM_PIDINV;
    }
    return MOBI_SUCCESS;
}

/**
 @brief Calculate PID from device serial number
 
 @param[in] serial Device serial number
 @param[out] pid Pid
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmpid_from_serial(char pid[PIDSIZE + 1], const char *serial) {
    memset(pid, 0, PIDSIZE + 1);
    MOBI_RET ret = mobi_serial_verify(serial);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    const char map[] = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
    unsigned char out[PIDSIZE - 2] = "\0";
    size_t out_length = sizeof(out);
    const size_t serial_length = strlen(serial);
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
    mobi_drmpid_checksum(checksum, (unsigned char *) pid);
    memcpy(&pid[PIDSIZE - 2], checksum, 2);
    return MOBI_SUCCESS;
}

/**
 @brief Get drm components from EXTH records
 
 Must be deallocated after use with mobi_exthdrm_free()
 
 @param[in] m MOBIData structure
 @return EXTHDrm structure (NULL or error)
 */
static MOBI_RET mobi_exthdrm_get(MOBIExthDrm **exth_drm, const MOBIData *m) {
    if (m == NULL || m->eh == NULL || exth_drm == NULL) {
        return MOBI_INIT_FAILED;
    }
    const MOBIExthHeader *meta = mobi_get_exthrecord_by_tag(m, EXTH_TAMPERKEYS);
    
    size_t token_size = 0;
    unsigned char *token = NULL;
    if (meta) {
        MOBIBuffer *buf = mobi_buffer_init_null(meta->data, meta->size);
        if (buf == NULL) {
            return MOBI_MALLOC_FAILED;
        }

        while (buf->offset < buf->maxlen) {
            uint8_t exth_type = mobi_buffer_get8(buf); // 1 - string, 0 - binary
            uint32_t exth_key = mobi_buffer_get32(buf);
            const MOBIExthHeader *sub = mobi_get_exthrecord_by_tag(m, exth_key);
            if (sub && sub->size) {
                size_t size = sub->size;
                unsigned char *data = sub->data;
                char *data_converted = NULL;
                if (exth_type == 1 && mobi_is_cp1252(m)) {
                    // FIXME: sample needed
                    size_t out_len = size * 3 + 1;
                    data_converted = malloc(out_len);
                    if (data_converted == NULL) {
                        free(token);
                        mobi_buffer_free_null(buf);
                        return MOBI_MALLOC_FAILED;
                    }
                    MOBI_RET ret = mobi_cp1252_to_utf8(data_converted, sub->data, &out_len, size);
                    if (ret == MOBI_SUCCESS) {
                        data = (unsigned char *) data_converted;
                        size = out_len;
                    }
                }
                size_t offset = token_size;
                token_size += size;
                unsigned char *tmp = realloc(token, token_size);
                if (tmp == NULL) {
                    free(token);
                    free(data_converted);
                    mobi_buffer_free_null(buf);
                    return MOBI_MALLOC_FAILED;
                }
                token = tmp;
                memcpy(token + offset, data, size);
                free(data_converted);
            }
        }
        mobi_buffer_free_null(buf);
    }

    *exth_drm = malloc(sizeof(MOBIExthDrm));
    if (*exth_drm == NULL) {
        free(token);
        return MOBI_MALLOC_FAILED;
    }
    (*exth_drm)->data = meta ? meta->data : NULL;
    (*exth_drm)->data_size = meta ? meta->size : 0;
    (*exth_drm)->token = token;
    (*exth_drm)->token_size = token_size;

    return MOBI_SUCCESS;
}

/**
 @brief Free EXTHDrm structure
 
 @param[in,out] exth_drm EXTHDrm to be freed
 */
static void mobi_free_exthdrm(MOBIExthDrm **exth_drm) {
    if (exth_drm) {
        if (*exth_drm) {
            free((*exth_drm)->token);
            (*exth_drm)->token = NULL;
        }
        free(*exth_drm);
        *exth_drm = NULL;
    }
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
 @brief Calculate PID using device serial and values stored in EXTH records
  
 @param[in] serial Device serial number
 @param[in] m MOBIData structure
 @param[out] pid Pid
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmpid_from_serial_exth(char pid[PIDSIZE + 1], const MOBIData *m, const char *serial) {
    memset(pid, 0, PIDSIZE + 1);
    
    MOBI_RET ret = mobi_serial_verify(serial);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    MOBIExthDrm *exth_drm = NULL;
    ret = mobi_exthdrm_get(&exth_drm, m);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    const size_t serial_length = strlen(serial);
    size_t message_length = serial_length + exth_drm->token_size + exth_drm->data_size;
    
    unsigned char *message = malloc(message_length);
    if (message == NULL) {
        mobi_free_exthdrm(&exth_drm);
        return MOBI_MALLOC_FAILED;
    }
    memcpy(message, serial, serial_length);
    if (exth_drm->data_size) {
        memcpy(&message[serial_length], exth_drm->data, exth_drm->data_size);
    }
    if (exth_drm->token_size) {
        memcpy(&message[serial_length + exth_drm->data_size], exth_drm->token, exth_drm->token_size);
    }
    mobi_free_exthdrm(&exth_drm);

    unsigned char hash[SHA1_DIGEST_SIZE];
    mobi_SHA1(hash, message_length, message);
    free(message);

    int bytes = 8;
    uint64_t val = 0;
    unsigned char *ptr = hash;
    while (bytes--) {
        val |= (uint64_t) *ptr++ << (bytes * 8);
    }
    const char map[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    for (size_t i = 0; i < 8; i++) {
        uint8_t pos = (uint64_t) val >> 58;
        val <<= 6;
        pid[i] = map[pos];
    }
    char checksum[2] = "\0";
    mobi_drmpid_checksum(checksum, (unsigned char *) pid);
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
MOBI_RET mobi_drmkey_set_serial(MOBIData *m, const char *serial) {
    char pid[PIDSIZE + 1];
    MOBI_RET ret = mobi_serial_verify(serial);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    ret = mobi_drmpid_from_serial(pid, serial);
    debug_print("Device pid: %s\n", pid);
    if (ret == MOBI_SUCCESS && (ret = mobi_drmkey_set(m, pid)) == MOBI_SUCCESS) {
        return MOBI_SUCCESS;
    }
    if (ret != MOBI_DRM_PIDINV && ret != MOBI_DRM_EXPIRED && ret != MOBI_DRM_KEYNOTFOUND) {
        /* return in case of non-drm error */
        return ret;
    }
    bool isExpired = (ret == MOBI_DRM_EXPIRED);
    ret = mobi_drmpid_from_serial_exth(pid, m, serial);
    debug_print("Book pid: %s\n", pid);
    if (ret == MOBI_SUCCESS && (ret = mobi_drmkey_set(m, pid) == MOBI_SUCCESS)) {
        return MOBI_SUCCESS;
    }
    return isExpired ? MOBI_DRM_EXPIRED : ret;
}

/**
 @brief Store DRM cookie data in MOBIData stucture
 
 This data is needed to encrypt main key during DRM applying process
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] pid  PID, NULL if not set
 @param[in] valid_from  Voucher validity start time, 0 if not set
 @param[in] valid_to Voucher expire time, MOBI_NOTSET if not set
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_cookie_add(MOBIData *m, const unsigned char *pid, const uint32_t valid_from, const uint32_t valid_to) {
    if (valid_from > valid_to) {
        return MOBI_PARAM_ERR;
    }
    if (m->internals == NULL && (m->internals = mobi_drm_init()) == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    MOBIDrm *drm = m->internals;
    if (drm->cookies_count == VOUCHERS_COUNT_MAX) {
        debug_print("Maximum PID count reached (%d) %s", VOUCHERS_COUNT_MAX, "\n");
        return MOBI_PARAM_ERR;
    }
    if (drm->cookies_count == 0) {
        drm->cookies = malloc(sizeof(*drm->cookies));
    } else {
        MOBICookie **tmp = realloc(drm->cookies, (drm->cookies_count + 1) * sizeof(*drm->cookies));
        if (tmp == NULL) {
            debug_print("Memory allocation failed%s", "\n");
            return MOBI_MALLOC_FAILED;
        }
        drm->cookies = tmp;
    }
    
    MOBICookie *cookie = calloc(1, sizeof(MOBICookie));
    if (cookie == NULL) {
        debug_print("Memory allocation failed%s", "\n");
        return MOBI_MALLOC_FAILED;
    }
    
    cookie->valid_from = valid_from;
    cookie->valid_to = valid_to;
    if (pid) {
        cookie->pid = malloc(PIDSIZE);
        if (cookie->pid == NULL) {
            debug_print("Memory allocation failed%s", "\n");
            free(cookie);
            return MOBI_MALLOC_FAILED;
        }
        memcpy(cookie->pid, pid, PIDSIZE);
    }

    drm->cookies[drm->cookies_count++] = cookie;
    
    return MOBI_SUCCESS;
}

/**
 @brief Store pid for encryption in MOBIData stucture
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] pid PID
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drmpid_add(MOBIData *m, const unsigned char *pid) {
    return mobi_cookie_add(m, pid, 0, MOBI_NOTSET);
}

/**
 @brief Adds tamperproof keys record to EXTH header
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] tamperkeys Array of tags to put in tamperproof record
 @param[in] tamperkeys_count Tags count
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_tamperkeys_add(MOBIData *m, const MOBIExthTag *tamperkeys, const size_t tamperkeys_count) {
    
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    if (tamperkeys_count == 0 || tamperkeys == NULL ) {
        return MOBI_PARAM_ERR;
    }
    
    // size of buffer is 5 bytes for each array member
    const size_t record_size = tamperkeys_count * 5;
    if (record_size >= UINT32_MAX) {
        debug_print("Too many tamperkeys: %zu\n", record_size);
        return MOBI_PARAM_ERR;
    }
    MOBIBuffer *buf = mobi_buffer_init(record_size);
    if (buf == NULL) {
        return MOBI_MALLOC_FAILED;
    }
    
    for (size_t i = 0; i < tamperkeys_count; i++) {
        MOBIExthHeader *tag = mobi_get_exthrecord_by_tag(m, tamperkeys[i]);
        if (tag == NULL) {
            debug_print("Missing EXTH record with tag %u\n", tamperkeys[i]);
            mobi_buffer_free(buf);
            return MOBI_PARAM_ERR;
        }
        MOBIExthMeta meta = mobi_get_exthtagmeta_by_tag(tamperkeys[i]);
        uint8_t tag_type = meta.type == EXTH_STRING ? 1 : 0;
        mobi_buffer_add8(buf, tag_type);
        mobi_buffer_add32(buf, tamperkeys[i]);
    }
    
    mobi_delete_exthrecord_by_tag(m, EXTH_TAMPERKEYS);
    MOBI_RET ret = mobi_add_exthrecord(m, EXTH_TAMPERKEYS, (uint32_t) record_size, buf->data);
    
    mobi_buffer_free(buf);
    
    return ret;
}

/**
 @brief Add DRM voucher
 
 @see mobi_drm_addvoucher
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] serial Device serial number
 @param[in] valid_from  Voucher validity start time, -1 if not set
 @param[in] valid_to Voucher expire time, -1 if not set
 @param[in] tamperkeys Array of EXTH tags to include in PID generation, NULL if none
 @param[in] tamperkeys_count Count of EXTH tags
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_voucher_add(MOBIData *m, const char *serial, const time_t valid_from, const time_t valid_to,
                          const MOBIExthTag *tamperkeys, const size_t tamperkeys_count) {

    if (tamperkeys_count) {
        mobi_tamperkeys_add(m, tamperkeys, tamperkeys_count);
    }
    
    // calculate PID if serial is provided
    char serial_pid[PIDSIZE + 1];
    unsigned char *pid = NULL;
    if (serial) {
        MOBI_RET ret = mobi_drmpid_from_serial(serial_pid, serial);
        debug_print("Pid for device serial %s: %s\n", serial, serial_pid);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
        pid = (unsigned char *) serial_pid;
    }
    
    // convert time values to mobi format
    uint32_t from = 0;
    if (valid_from > 0 && (size_t) (valid_from / 60) <= UINT32_MAX) {
        from = (uint32_t) (valid_from / 60);
    }
    uint32_t to = MOBI_NOTSET;
    if (valid_to >= 0 && (size_t) (valid_to / 60) <= UINT32_MAX) {
        to = (uint32_t) (valid_to / 60);
    }
    
    // Verification on Kindles requires both dates to be set but we allow users to set just one.
    // Set missing range limit if necessary.
    if (from != 0 && to == MOBI_NOTSET) {
        to = UINT32_MAX - 1;
    }
    if (to != MOBI_NOTSET && from == 0) {
        from = 1;
    }
    
    return mobi_cookie_add(m, pid, from, to);
}

/**
 @brief Store key for encryption in MOBIData stucture
 
 In case of encrypted document key is extracted from document. PID may be needed.
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] pid PID, may be NULL in case of encryption type 1, which does not use PID
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drmkey_set(MOBIData *m, const char *pid) {
    if (m == NULL || m->rh == NULL) {
        return MOBI_INIT_FAILED;
    }

    MOBI_RET ret;
    unsigned char *drm_pid = (unsigned char *) pid;
    if (pid) {
        const size_t pid_length = strlen(pid);
        if (pid_length != PIDSIZE) {
            debug_print("PID size is wrong (%zu)\n", pid_length);
            return MOBI_DRM_PIDINV;
        }
        ret = mobi_drmpid_verify(drm_pid);
        if (ret != MOBI_SUCCESS) {
            debug_print("PID is invalid%s", "\n");
            return ret;
        }
        if (!mobi_is_encrypted(m)) {
            debug_print("Document not encrypted, adding voucher%s", "\n");
            mobi_drmpid_add(m, drm_pid);
            return MOBI_SUCCESS;
        }
    }
    
    if (drm_pid && m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        /* PID not needed */
        debug_print("Encryption doesn't require PID%s", "\n");
        drm_pid = NULL;
    }
    unsigned char key[KEYSIZE];
    ret = mobi_drmkey_get(key, drm_pid, m);
    if (ret != MOBI_SUCCESS) {
        debug_print("Key not found%s", "\n");
        return ret;
    }
    ret = mobi_drmkey_init(m, key);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    if (m->rh->encryption_type > 1) {
        ret = mobi_drmpid_add(m, drm_pid);
    }
    
    return ret;
}

/**
 @brief Remove key from MOBIData structure
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drmkey_delete(MOBIData *m) {
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    mobi_free_drm(m);
    return MOBI_SUCCESS;
}

/**
 @brief Mark document as encrypted
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] encryption_type  Encryption type
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_set(MOBIData *m, const uint16_t encryption_type) {
    
    m->rh->encryption_type = encryption_type;

    if (encryption_type == MOBI_ENCRYPTION_V2) {
        if (m->mh == NULL) {
            return MOBI_DATA_CORRUPT;
        }
        if (m->mh->drm_size == NULL) {
            m->mh->drm_size = malloc(sizeof(uint32_t));
            if (m->mh->drm_size == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        if (m->mh->drm_offset == NULL) {
            m->mh->drm_offset = malloc(sizeof(uint32_t));
            if (m->mh->drm_offset == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        if (m->mh->drm_count == NULL) {
            m->mh->drm_count = malloc(sizeof(uint32_t));
            if (m->mh->drm_count == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        if (m->mh->drm_flags == NULL) {
            m->mh->drm_flags = malloc(sizeof(uint32_t));
            if (m->mh->drm_flags == NULL) {
                debug_print("Memory allocation failed%s", "\n");
                return MOBI_MALLOC_FAILED;
            }
        }
        
        size_t drm_size = VOUCHERS_SIZE_MIN;
        MOBIDrm *drm = m->internals;
        if (drm->cookies_count * VOUCHERSIZE > VOUCHERS_SIZE_MIN) {
            drm_size = drm->cookies_count * VOUCHERSIZE;
        }
        *m->mh->drm_size = (uint32_t) drm_size;
        /* initial offset set to unknown */
        *m->mh->drm_offset = MOBI_NOTSET;
        /* flags: b0 = 1 internal key? */
        *m->mh->drm_flags = 2048;
        *m->mh->drm_count = drm->cookies_count;
    }
        
    return MOBI_SUCCESS;
}

/**
 @brief Mark document as decrypted
 
 @param[in,out] m MOBIData structure with raw data and metadata
 */
static void mobi_drm_unset(MOBIData *m) {
    if (m->rh->encryption_type == MOBI_ENCRYPTION_V2 && m->mh) {
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
    m->rh->encryption_type = MOBI_ENCRYPTION_NONE;
    
    mobi_drmkey_delete(m);
}

/**
 @brief Decrypt or encrypt records
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @param[in] is_decryption Should we decrypt or encrypt
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_records(MOBIData *m, const bool is_decryption) {
 
    /* FIXME: hybrid files are not encrypted, are they? */
    size_t text_rec_index = 1;
    
    if (!is_decryption) {
        const size_t offset = mobi_get_kf8offset(m);
        if (m->rh == NULL || m->rh->text_record_count == 0) {
            debug_print("%s", "Text records not found in MOBI header\n");
            return MOBI_DATA_CORRUPT;
        }
        text_rec_index = 1 + offset;
    }
    
    size_t text_rec_count = m->rh->text_record_count;
    const uint16_t compression_type = m->rh->compression_type;
    uint16_t extra_flags = 0;
    if (m->mh && m->mh->extra_flags) {
        extra_flags = *m->mh->extra_flags;
    }
    if (compression_type != MOBI_COMPRESSION_HUFFCDIC) {
        /* encrypt also multibyte extra data */
        extra_flags &= 0xfffe;
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
        
        size_t decrypt_size = curr->size - extra_size;;
        unsigned char *decrypted = malloc(decrypt_size);
        if (decrypted == NULL) {
            debug_print("Memory allocation failed%s", "\n");
            return MOBI_MALLOC_FAILED;
        }
        
        MOBI_RET ret;
        if (is_decryption) {
            ret = mobi_buffer_decrypt(decrypted, curr->data, decrypt_size, m);
        } else {
            ret = mobi_buffer_encrypt(decrypted, curr->data, decrypt_size, m);
        }
        if (ret != MOBI_SUCCESS) {
            free(decrypted);
            return ret;
        }
        memcpy(curr->data, decrypted, decrypt_size);
        free(decrypted);
        curr = curr->next;
    }
    
    return MOBI_SUCCESS;
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
    
    MOBI_RET ret;
    if (!mobi_has_drmkey(m)) {
        /* try to set key */
        debug_print("%s\n", "Trying to set key for encryption without device PID");
        ret = mobi_drm_setkey(m, NULL);
        if (ret != MOBI_SUCCESS) {
            debug_print("%s\n", "Key setting failed");
            return ret;
        }
    }

    ret = mobi_drm_records(m, true);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    mobi_drm_unset(m);
    
    return MOBI_SUCCESS;
}

/**
 @brief Serialize encryption voucher
 
 This supports key types 1 and 3, used on eInk devices
 
 @param[in,out] buf Output buffer
 @param[in] key Main encryption key
 @param[in] cookie Voucher cookie data
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_voucher_serialize(MOBIBuffer *buf, const unsigned char *key, const MOBICookie *cookie) {
    
    MOBI_RET ret;
    // Notes on reversing flags
    // 5 first bits (flags & 0x1f) mean key type:
    // type 1 - device serial + exth key,
    // type 2 - device generic type id (no samples, not used any more?),
    // type 3 – internal key.
    // If drm flags in mobi header have bit 0 set then additional user password is used (probably not used any more).
    // In case of password protection temporary key is additionally encrypted with password (bit 4 must be set?)
    // In case of key types 1 and 2 bit 11 (0x800) must be set, unless there is additional password
    // Bit 6 (0x40) must be set, otherwise found key is not used.
    uint32_t flags;
    unsigned char pidkey[KEYSIZE] = "\0";
    if (cookie->pid) {
        flags = 0x841; // key type 1
        memcpy(pidkey, cookie->pid, PIDSIZE - 2);
        ret = mobi_pk1_encrypt(pidkey, pidkey, KEYSIZE, INTERNAL_READER_KEY);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    } else {
        memcpy(pidkey, INTERNAL_READER_KEY, KEYSIZE);
        flags = 0x43; // key type 3
    }

    uint8_t pidkey_checksum = mobi_drmkey_checksum(pidkey);
    uint32_t verification = 0x43; // always same in real files, why???
    
    mobi_buffer_add32(buf, verification);        //  0: verification
    mobi_buffer_add32(buf, VOUCHERSIZE);         //  4: size
    mobi_buffer_add32(buf, 1);                   //  8: always 1 ?
    mobi_buffer_add8(buf, pidkey_checksum);      // 12: pid checksum
    mobi_buffer_addzeros(buf, 3);
    // start of encrypted part - cookie
    unsigned char *cookie_buf = buf->data + buf->offset;
    mobi_buffer_add32(buf, verification);        // 16: verification
    mobi_buffer_add32(buf, flags);               // 20: flags
    mobi_buffer_addraw(buf, key, KEYSIZE);       // 24: key
    mobi_buffer_add32(buf, cookie->valid_to);    // 40: valid to
    mobi_buffer_add32(buf, cookie->valid_from);  // 44: valid from
    
    ret = mobi_pk1_encrypt(cookie_buf, cookie_buf, COOKIESIZE, pidkey);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    return MOBI_SUCCESS;
}

/**
 @brief Serialize encryption scheme version 2
 
 @param[in,out] buf Output buffer
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_serialize_v2(MOBIBuffer *buf, const MOBIData *m) {
    if (!m || !m->mh || m->internals == NULL) {
        return MOBI_INIT_FAILED;
    }
    
    size_t saved_offset = buf->offset;
    
    mobi_buffer_setpos(buf, *m->mh->drm_offset);
    
    MOBIDrm *drm = m->internals;
    for (size_t i = 0; i < drm->cookies_count; i++) {
        MOBI_RET ret = mobi_voucher_serialize(buf, drm->key, drm->cookies[i]);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    }
    size_t cookies_count_min = VOUCHERS_SIZE_MIN / VOUCHERSIZE;
    if (drm->cookies_count < cookies_count_min) {
        // pad with zeros
        mobi_buffer_addzeros(buf, (cookies_count_min - drm->cookies_count) * VOUCHERSIZE);
    }
    
    mobi_buffer_setpos(buf, saved_offset);
    
    return MOBI_SUCCESS;
}

/**
 @brief Serialize encryption scheme version 1
 
 @param[in,out] buf Output buffer
 @param[in] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_serialize_v1(MOBIBuffer *buf, const MOBIData *m) {
    
    size_t saved_offset = buf->offset;
    
    size_t mobi_version = mobi_get_fileversion(m);
    
    if (mobi_version > 1) {
        /* offset mobi header + record 0 header (+ 12 in newer files) */
        if (m->mh == NULL || m->mh->header_length == NULL) {
            return MOBI_DATA_CORRUPT;
        }
        size_t offset = 0;
        if (mobi_version > 2) {
            offset = 12;
        }
        mobi_buffer_setpos(buf, *m->mh->header_length + RECORD0_HEADER_LEN + offset);
    } else {
        /* offset 14 */
        mobi_buffer_setpos(buf, 14);
    }
    
    MOBIDrm *drm = m->internals;
    
    uint8_t key_type = 1; // 1 - simple, 2 - verification password, 4 - verification key
    unsigned char *key_offset = buf->data + buf->offset;
    mobi_buffer_addraw(buf, drm->key, KEYSIZE); //  0
    mobi_buffer_addzeros(buf, 1);               // 16
    // meta
    unsigned char *meta_offset = buf->data + buf->offset;
    mobi_buffer_add8(buf, key_type);            // 17: key type
    mobi_buffer_addzeros(buf, 1);               // 18
    // simple: no verification
    mobi_buffer_addzeros(buf, 32);              // 19: password
                                                // 25: verfication key
    
    MOBI_RET ret = mobi_pk1_encrypt(key_offset, key_offset, KEYSIZE + 1, INTERNAL_READER_KEY_V1);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    ret = mobi_pk1_encrypt(meta_offset, meta_offset, 34, drm->key);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    mobi_buffer_setpos(buf, saved_offset);
    
    return MOBI_SUCCESS;
}

/**
 @brief Initializes random key
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_drm_generate_key(MOBIData *m) {
    
    unsigned char buf[KEYSIZE];
    
    MOBI_RET ret = mobi_randombytes(buf, KEYSIZE);
    if (ret != MOBI_SUCCESS) {
        debug_print("Getting random buffer failed%s", "\n");
        return ret;
    }
    
    return mobi_drmkey_init(m, buf);
}

/**
 @brief Encrypt document
 
 DRM vouchers must be added in order to use device serial number in encryption
 
 @param[in,out] m MOBIData structure with raw data and metadata
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_drm_encrypt(MOBIData *m) {
    
    if (m == NULL) {
        return MOBI_INIT_FAILED;
    }
    
    uint16_t encryption_type = MOBI_ENCRYPTION_V2;
    if (mobi_get_fileversion(m) < 4 || m->eh == NULL) {
        encryption_type = MOBI_ENCRYPTION_V1;
    }
    debug_print("Encryption type: %u\n", encryption_type);

    MOBI_RET ret;
    if (!mobi_has_drmkey(m)) {
        ret = mobi_drm_generate_key(m);
        if (ret != MOBI_SUCCESS) {
            return ret;
        }
    }
    
    if (encryption_type == MOBI_ENCRYPTION_V2 && !mobi_has_drmcookies(m)) {
        mobi_cookie_add(m, NULL, 0, MOBI_NOTSET);
    }
    
    ret = mobi_drm_records(m, false);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    
    return mobi_drm_set(m, encryption_type);
}
