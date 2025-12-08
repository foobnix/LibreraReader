/** @file common.c
 *
 * @brief common tools functions
 *
 * @example common.c
 * Common functions for tools
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <errno.h>
#include <mobi.h>
#include "common.h"

#ifdef _WIN32
# include <direct.h>
# define mkdir(path,flags) _mkdir(path)
const char separator = '\\';
#else
const char separator = '/';
#endif
bool outdir_opt = false;
char outdir[FILENAME_MAX];

#define UNUSED(x) (void)(x)

#ifndef min
# define min(a, b) ((a) < (b) ? (a) : (b))
#endif

/**
 @brief Messages for libmobi return codes
 For reference see enum MOBI_RET in mobi.h
 */
const char *libmobi_messages[] = {
    "Success",
    "Generic error",
    "Wrong function parameter",
    "Corrupted data",
    "File not found",
    "Document is encrypted",
    "Unsupported document format",
    "Memory allocation error",
    "Initialization error",
    "Buffer error",
    "XML error",
    "Invalid DRM pid",
    "DRM key not found",
    "DRM support not included",
    "Write failed",
    "DRM expired"
};

#define LIBMOBI_MSG_COUNT ARRAYSIZE(libmobi_messages)

/**
 @brief Return message for given libmobi return code
 @param[in] ret Libmobi return code
 @return Message string
 */
const char * libmobi_msg(const MOBI_RET ret) {
    size_t index = ret;
    if (index < LIBMOBI_MSG_COUNT) {
        return libmobi_messages[index];
    }
    return "Unknown error";
}

/**
 @brief Parse file name into file path and base name.
        Dirname or basename can be skipped by setting to null.
 @param[in] fullpath Full file path
 @param[in,out] dirname Will be set to full dirname
 @param[in,out] basename Will be set to file basename
 @param[in] buf_len Size of each ouput buffer: dirname and basename
 */
void split_fullpath(const char *fullpath, char *dirname, char *basename, const size_t buf_len) {
    if (buf_len == 0) {
        return;
    }
    char *p = strrchr(fullpath, separator);
    if (p) {
        p += 1;
        if (dirname) {
            size_t dirlen = min(buf_len - 1, (size_t) (p - fullpath));
            strncpy(dirname, fullpath, dirlen);
            dirname[dirlen] = '\0';
        }
        if (basename) {
            strncpy(basename, p, buf_len - 1);
            basename[buf_len - 1] = '\0';
        }
    }
    else {
        if (dirname) {
            dirname[0] = '\0';
        }
        if (basename) {
            strncpy(basename, fullpath, buf_len - 1);
            basename[buf_len - 1] = '\0';
        }
    }
    if (basename) {
        p = strrchr(basename, '.');
        if (p) {
            *p = '\0';
        }
    }
}

/**
 @brief Make directory
 @param[in] path Path
 @return SUCCESS or ERROR
 */
int make_directory(const char *path) {
    errno = 0;
    if (mkdir(path, S_IRWXU) != 0 && errno != EEXIST) {
        int errsv = errno;
        printf("Creating directory \"%s\" failed (%s)\n", path, strerror(errsv));
        return ERROR;
    }
    return SUCCESS;
}

/**
 @brief Create subfolder in directory
 @param[in,out] newdir Path to created subfolder
 @param[in] buf_len Buffer size fo created subfolder
 @param[in] parent_dir Directory path
 @param[in] subdir_name Subfolder name
 @return SUCCESS or ERROR
 */
int create_subdir(char *newdir, const size_t buf_len, const char *parent_dir, const char *subdir_name) {
    int n = snprintf(newdir, buf_len, "%s%c%s", parent_dir, separator, subdir_name);
    if (n < 0) {
        printf("Creating file name failed\n");
        return ERROR;
    }
    if ((size_t) n >= buf_len) {
        printf("File name too long: %s\n", newdir);
        return ERROR;
    }
    return make_directory(newdir);
}

/**
 @brief Open file descriptor and write buffer to it
 @param[in] buffer Buffer
 @param[in] len Buffer length
 @param[in] path File path
 @return SUCCESS or ERROR
 */
int write_file(const unsigned char *buffer, const size_t len, const char *path) {
    errno = 0;
    FILE *file = fopen(path, "wb");
    if (file == NULL) {
        int errsv = errno;
        printf("Could not open file for writing: %s (%s)\n", path, strerror(errsv));
        return ERROR;
    }
    size_t n = fwrite(buffer, 1, len, file);
    if (n != len) {
        int errsv = errno;
        printf("Error writing to file: %s (%s)\n", path, strerror(errsv));
        fclose(file);
        return ERROR;
    }
    fclose(file);
    return SUCCESS;
}

/**
 @brief Write content to file in directory
 @param[in] dir Directory path
 @param[in] name File name
 @param[in] buffer Buffer
 @param[in] len Buffer length
 @return SUCCESS or ERROR
 */
int write_to_dir(const char *dir, const char *name, const unsigned char *buffer, const size_t len) {
    char path[FILENAME_MAX];
    int n = snprintf(path, sizeof(path), "%s%c%s", dir, separator, name);
    if (n < 0) {
        printf("Creating file name failed\n");
        return ERROR;
    }
    if ((size_t) n >= sizeof(path)) {
        printf("File name too long\n");
        return ERROR;
    }
    return write_file(buffer, len, path);
}

/**
 @brief Check whether given path exists and is a directory
 @param[in] path Path to be tested
 @return True if directory exists, false otherwise
 */
bool dir_exists(const char *path) {
    struct stat sb;
    if (stat(path, &sb) != 0) {
        int errsv = errno;
        printf("Path \"%s\" is not accessible (%s)\n", path, strerror(errsv));
        return false;
    }
    if (!S_ISDIR(sb.st_mode)) {
        printf("Path \"%s\" is not a directory\n", path);
        return false;
    }
    return true;
}

/**
 @brief Make sure we use consistent separators on Windows builds
 @param[in,out] path Path to be fixed
 */
void normalize_path(char *path) {
#ifdef _WIN32
    if (path != NULL) {
        for (size_t i = 0; i <= strlen(path); i++) {
            if (path[i] == '/') {
                path[i] = separator;
            }
        }
    }
#else
    UNUSED(path);
#endif
}


/**
 @brief Print summary meta information
 @param[in] m MOBIData structure
 */
void print_summary(const MOBIData *m) {
    char *title = mobi_meta_get_title(m);
    if (title) {
        printf("Title: %s\n", title);
        free(title);
    }
    char *author = mobi_meta_get_author(m);
    if (author) {
        printf("Author: %s\n", author);
        free(author);
    }
    char *contributor = mobi_meta_get_contributor(m);
    uint32_t major = 0, minor = 0, build = 0;
    bool is_calibre = false;
    if (contributor) {
        const char *calibre_contributor = "calibre (";
        if (strncmp(contributor, calibre_contributor, strlen(calibre_contributor)) == 0) {
            is_calibre = true;
            sscanf(contributor, "calibre (%u.%u.%u)", &major, &minor, &build);
        } else {
            printf("Contributor: %s\n", contributor);
        }
        free(contributor);
    }
    char *subject = mobi_meta_get_subject(m);
    if (subject) {
        printf("Subject: %s\n", subject);
        free(subject);
    }
    char *publisher = mobi_meta_get_publisher(m);
    if (publisher) {
        printf("Publisher: %s\n", publisher);
        free(publisher);
    }
    char *date = mobi_meta_get_publishdate(m);
    if (date) {
        printf("Publishing date: %s\n", date);
        free(date);
    }
    char *description = mobi_meta_get_description(m);
    if (description) {
        printf("Description: %s\n", description);
        free(description);
    }
    char *review = mobi_meta_get_review(m);
    if (review) {
        printf("Review: %s\n", review);
        free(review);
    }
    char *imprint = mobi_meta_get_imprint(m);
    if (imprint) {
        printf("Imprint: %s\n", imprint);
        free(imprint);
    }
    char *copyright = mobi_meta_get_copyright(m);
    if (copyright) {
        printf("Copyright: %s\n", copyright);
        free(copyright);
    }
    char *isbn = mobi_meta_get_isbn(m);
    if (isbn) {
        printf("ISBN: %s\n", isbn);
        free(isbn);
    }
    char *asin = mobi_meta_get_asin(m);
    if (asin) {
        printf("ASIN: %s\n", asin);
        free(asin);
    }
    char *language = mobi_meta_get_language(m);
    if (language) {
        printf("Language: %s", language);
        free(language);
        if (m->mh && m->mh->text_encoding) {
            uint32_t encoding = *m->mh->text_encoding;
            if (encoding == MOBI_CP1252) {
                printf(" (cp1252)");
            } else if (encoding == MOBI_UTF8) {
                printf(" (utf8)");
            }
        }
        printf("\n");
    }
    if (mobi_is_dictionary(m)) {
        printf("Dictionary");
        if (m->mh && m->mh->dict_input_lang && m->mh->dict_output_lang &&
            *m->mh->dict_input_lang && *m->mh->dict_output_lang) {
            const char *locale_in = mobi_get_locale_string(*m->mh->dict_input_lang);
            const char *locale_out = mobi_get_locale_string(*m->mh->dict_output_lang);
            printf(": %s => %s", locale_in ? locale_in : "unknown", locale_out ? locale_out : "unknown");
        }
        printf("\n");
    }
    printf("__\n");
    if (strcmp(m->ph->type, "TEXt") == 0) {
        if (strcmp(m->ph->creator, "TlDc") == 0) {
            printf("TealDoc\n");
        } else {
            printf("PalmDoc\n");
        }
    } else {
        printf("Mobi version: %zu", mobi_get_fileversion(m));
        if (mobi_is_hybrid(m)) {
            size_t version = mobi_get_fileversion(m->next);
            if (version != MOBI_NOTSET) {
                printf(" (hybrid with version %zu)", version);
            }
        }
        printf("\n");
    }
    if (mobi_is_replica(m)) {
        printf("Print Replica\n");
    }
    if (mobi_is_encrypted(m)) {
        printf("Document is encrypted\n");
    }
    if (is_calibre) {
        printf("Creator software: calibre %u.%u.%u\n", major, minor, build);
    } else {
        MOBIExthHeader *exth = mobi_get_exthrecord_by_tag(m, EXTH_CREATORSOFT);
        if (exth) {
            printf("Creator software: ");
            uint32_t creator = mobi_decode_exthvalue(exth->data, exth->size);
            exth = mobi_get_exthrecord_by_tag(m, EXTH_CREATORMAJOR);
            if (exth) {
                major = mobi_decode_exthvalue(exth->data, exth->size);
            }
            exth = mobi_get_exthrecord_by_tag(m, EXTH_CREATORMINOR);
            if (exth) {
                minor = mobi_decode_exthvalue(exth->data, exth->size);
            }
            exth = mobi_get_exthrecord_by_tag(m, EXTH_CREATORBUILD);
            if (exth) {
                build = mobi_decode_exthvalue(exth->data, exth->size);
            }
            exth = mobi_get_exthrecord_by_tag(m, EXTH_CREATORBUILDREV);
            if (major == 2 && minor == 9 && build == 0 && exth) {
                char *rev = mobi_decode_exthstring(m, exth->data, exth->size);
                if (rev) {
                    if (strcmp(rev, "0730-890adc2") == 0) {
                        is_calibre = true;
                    }
                    free(rev);
                }
            }
            switch (creator) {
                case 0:
                    printf("mobipocket reader %u.%u.%u", major, minor, build);
                    break;
                case 1:
                case 101:
                    printf("mobigen %u.%u.%u", major, minor, build);
                    break;
                case 2:
                    printf("mobipocket creator %u.%u.%u", major, minor, build);
                    break;
                case 200:
                    printf("kindlegen %u.%u.%u (windows)", major, minor, build);
                    if (is_calibre) {
                        printf(" or calibre");
                    }
                    break;
                case 201:
                    printf("kindlegen %u.%u.%u (linux)", major, minor, build);
                    if ((major == 1 && minor == 2 && build == 33307) ||
                        (major == 2 && minor == 0 && build == 101) ||
                        is_calibre) {
                        printf(" or calibre");
                    }
                    break;
                case 202:
                    printf("kindlegen %u.%u.%u (mac)", major, minor, build);
                    if (is_calibre) {
                        printf(" or calibre");
                    }
                    break;
                default:
                    printf("unknown");
                    break;
            }
            printf("\n");
        }
    }
}

/**
 @brief Print all loaded EXTH record tags
 @param[in] m MOBIData structure
 */
void print_exth(const MOBIData *m) {
    if (m->eh == NULL) {
        return;
    }
    /* Linked list of MOBIExthHeader structures holds EXTH records */
    const MOBIExthHeader *curr = m->eh;
    if (curr != NULL) {
        printf("\nEXTH records:\n");
    }
    uint32_t val32;
    while (curr != NULL) {
        /* check if it is a known tag and get some more info if it is */
        MOBIExthMeta tag = mobi_get_exthtagmeta_by_tag(curr->tag);
        if (tag.tag == 0) {
            /* unknown tag */
            /* try to print the record both as string and numeric value */
            char *str = malloc(curr->size + 1);
            if (!str) {
                printf("Memory allocation failed\n");
                exit(1);
            }
            unsigned i = 0;
            unsigned char *p = curr->data;
            while (i < curr->size && isprint(*p)) {
                str[i] = (char)*p++;
                i++;
            }
            str[i] = '\0';
            val32 = mobi_decode_exthvalue(curr->data, curr->size);
            printf("Unknown (%i): %s (%u)\n", curr->tag, str, val32);
            free(str);
        } else {
            /* known tag */
            size_t size = curr->size;
            unsigned char *data = curr->data;
            switch (tag.type) {
                    /* numeric */
                case EXTH_NUMERIC:
                    val32 = mobi_decode_exthvalue(data, size);
                    printf("%s (%i): %u\n", tag.name, tag.tag, val32);
                    break;
                    /* string */
                case EXTH_STRING:
                {
                    char *exth_string = mobi_decode_exthstring(m, data, size);
                    if (exth_string) {
                        printf("%s (%i): %s\n", tag.name, tag.tag, exth_string);
                        free(exth_string);
                    }
                    break;
                }
                    /* binary */
                case EXTH_BINARY:
                {
                    unsigned i = 0;
                    const size_t str_len = 2 * size + 1;
                    char *str = malloc(str_len);
                    if (!str) {
                        printf("Memory allocation failed\n");
                        exit(1);
                    }
                    str[0] = '\0';
                    while (size) {
                        uint8_t val8 = *data++;
                        snprintf(&str[i], str_len - i, "%02x", val8);
                        i += 2;
                        size--;
                    }
                    printf("%s (%i): 0x%s\n", tag.name, tag.tag, str);
                    free(str);
                    break;
                }
                default:
                    break;
            }
        }
        curr = curr->next;
    }
}

/**
 @brief Set PID for decryption
 @param[in,out] m MOBIData structure
 @param[in] pid Serial number
 @return SUCCESS or error code
 */
int set_decryption_pid(MOBIData *m, const char *pid) {
    printf("\nVerifying PID %s...", pid);
    MOBI_RET mobi_ret = mobi_drm_setkey(m, pid);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("failed (%s)\n", libmobi_msg(mobi_ret));
        return (int) mobi_ret;
    }
    printf("ok\n");
    return SUCCESS;
}

/**
 @brief Set device serial number for decryption
 @param[in,out] m MOBIData structure
 @param[in] serial Serial number
 @return SUCCESS or error code
 */
int set_decryption_serial(MOBIData *m, const char *serial) {
    printf("\nVerifying serial %s... ", serial);
    MOBI_RET mobi_ret = mobi_drm_setkey_serial(m, serial);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("failed (%s)\n", libmobi_msg(mobi_ret));
        return (int) mobi_ret;
    }
    printf("ok\n");
    return SUCCESS;
}

/**
 @brief Set key for decryption. Use user supplied pid or device serial number
 @param[in,out] m MOBIData structure
 @param[in] serial Serial number
 @param[in] pid Pid
 @return SUCCESS or error code
 */
int set_decryption_key(MOBIData *m, const char *serial, const char *pid) {

    if (!pid && !serial) {
        return SUCCESS;
    }
    if (!mobi_is_encrypted(m)) {
        printf("\nDocument is not encrypted, ignoring PID/serial\n");
        return SUCCESS;
    }
    if (m->rh && m->rh->encryption_type == MOBI_ENCRYPTION_V1) {
        printf("\nEncryption type 1, ignoring PID/serial\n");
        return SUCCESS;
    }
    int ret = SUCCESS;
    if (pid && (ret = set_decryption_pid(m, pid)) == SUCCESS) {
        return SUCCESS;
    }
    if (serial) {
        ret = set_decryption_serial(m, serial);
    }
    return ret;
}

/**
 @brief Save mobi file
 
 @param[in,out] m MOBIData struicture
 @param[in] fullpath Full file path
 @param[in] suffix Suffix appended to file name
 @return SUCCESS or ERROR
 */
int save_mobi(MOBIData *m, const char *fullpath, const char *suffix) {
    char outfile[FILENAME_MAX];
    char basename[FILENAME_MAX];
    char dirname[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename, FILENAME_MAX);
    const char *ext = (mobi_get_fileversion(m) >= 8) ? "azw3" : "mobi";
    int n;
    if (outdir_opt) {
        n = snprintf(outfile, sizeof(outfile), "%s%s-%s.%s", outdir, basename, suffix, ext);
    } else {
        n = snprintf(outfile, sizeof(outfile), "%s%s-%s.%s", dirname, basename, suffix, ext);
    }
    if (n < 0) {
        printf("Creating file name failed\n");
        return ERROR;
    }
    if ((size_t) n >= sizeof(outfile)) {
        printf("File name too long\n");
        return ERROR;
    }
    
    /* write */
    printf("Saving %s...\n", outfile);
    FILE *file_out = fopen(outfile, "wb");
    if (file_out == NULL) {
        int errsv = errno;
        printf("Error opening file: %s (%s)\n", outfile, strerror(errsv));
        return ERROR;
    }
    MOBI_RET mobi_ret = mobi_write_file(file_out, m);
    fclose(file_out);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Error writing file (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    return SUCCESS;
}
