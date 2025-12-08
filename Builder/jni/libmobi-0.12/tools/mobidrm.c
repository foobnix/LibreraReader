/** @file mobidrm.c
 *
 * @brief mobidrm
 *
 * Copyright (c) 2021 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <string.h>
#include <stdbool.h>
#include <stdlib.h>
#include <ctype.h>
#include <errno.h>
#include <mobi.h>

#include "common.h"

#define VOUCHERS_COUNT_MAX 20

/* command line options */
bool decrypt_opt = false;
bool encrypt_opt = false;
bool expiry_opt = false;

/* options values */
char *pid[VOUCHERS_COUNT_MAX];
char *serial[VOUCHERS_COUNT_MAX];
size_t serial_count = 0;
size_t pid_count = 0;
time_t valid_from = -1;
time_t valid_to = -1;


/**
 @brief Print usage info
 @param[in] progname Executed program name
 */
static void print_usage(const char *progname) {
    printf("usage: %s [-d | -e] [-hv] [-p pid] [-f date] [-t date] [-s serial] [-o dir] filename\n", progname);
    printf("       without arguments prints document metadata and exits\n\n");

    printf("       Decrypt options:\n");
    printf("       -d        decrypt (required)\n");
    printf("       -p pid    set decryption pid (may be specified multiple times)\n");
    printf("       -s serial set device serial (may be specified multiple times)\n\n");
    
    printf("       Encrypt options:\n");
    printf("       -e        encrypt (required)\n");
    printf("       -s serial set device serial (may be specified multiple times)\n");
    printf("       -f date   set validity period from date (yyyy-mm-dd) when encrypting (inclusive)\n");
    printf("       -t date   set validity period to date (yyyy-mm-dd) when encrypting (inclusive)\n\n");
    
    printf("       Common options:\n");
    printf("       -o dir    save output to dir folder\n");
    printf("       -h        show this usage summary and exit\n");
    printf("       -v        show version and exit\n");
}

/**
 @brief Applies DRM
 
 @param[in,out] m MOBIData structure
 @param[in] use_kf8 In case of hybrid file process KF8 part if true, the other part otherwise
 @return SUCCESS or ERROR
 */
static int do_encrypt(MOBIData *m, bool use_kf8) {
    
    MOBI_RET mobi_ret;
    
    if (mobi_is_hybrid(m)) {
        mobi_ret = mobi_remove_hybrid_part(m, !use_kf8);
        if (mobi_ret != MOBI_SUCCESS) {
            printf("Error removing hybrid part (%s)\n", libmobi_msg(mobi_ret));
            return ERROR;
        }
        printf("\nProcessing file version %zu from hybrid file\n", mobi_get_fileversion(m));
    }
    
    MOBIExthTag *tags = NULL;
    MOBIExthTag amazonkeys[] = { EXTH_WATERMARK, EXTH_TTSDISABLE, EXTH_CLIPPINGLIMIT, EXTH_READFORFREE, EXTH_RENTAL, EXTH_UNK407 };
    MOBIExthTag tamperkeys[ARRAYSIZE(amazonkeys)] = { 0 };
    size_t tags_count = 0;
    
    if (mobi_get_fileversion(m) >= 4 && m->eh) {
        // EXTH tamperproof keys are required with encryption scheme 2 in mobi version 8
        // and are supported since version 4.
        // Amazon software by default includes tags: 208, 404, 401, 405, 406, 407 in DRM scheme.
        // Any change in tags that are included invalidates DRM.
        // You can secure any tag you want, we just add dummy watermark (208) if it is not already present in file
        // to satisfy requirements.
        if (mobi_get_exthrecord_by_tag(m, EXTH_WATERMARK) == NULL) {
            char watermark[] = "aHR0cHM6Ly9naXRodWIuY29tL2JmYWJpc3pld3NraS9saWJtb2Jp";
            mobi_add_exthrecord(m, EXTH_WATERMARK, (uint32_t) strlen(watermark), watermark);
        }
        tamperkeys[0] = amazonkeys[0];
        tags_count++;
        // If any of the Amazon default tags is present in file, secure it too.
        // You can also add any of them with mobimeta before running mobidrm.
        for (size_t i = 1; i < ARRAYSIZE(amazonkeys); i++) {
            // if exth record exists in mobi file
            if (mobi_get_exthrecord_by_tag(m, amazonkeys[i]) != NULL) {
                // add it tamperproof keys
                tamperkeys[tags_count++] = amazonkeys[i];
            }
        }
        
        tags = tamperkeys;
    }

    for (size_t i = 0; i < serial_count; i++) {
        mobi_ret = mobi_drm_addvoucher(m, serial[i], valid_from, valid_to, tags, tags_count);
        if (mobi_ret != MOBI_SUCCESS) {
            printf("Error adding encryption voucher (%s)\n", libmobi_msg(mobi_ret));
            return ERROR;
        }
    }
    if (serial_count == 0) {
        mobi_ret = mobi_drm_addvoucher(m, NULL, valid_from, valid_to, tags, tags_count);
        if (mobi_ret != MOBI_SUCCESS) {
            printf("Error adding encryption voucher (%s)\n", libmobi_msg(mobi_ret));
            return ERROR;
        }
    }
    
    mobi_ret = mobi_drm_encrypt(m);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Error encrypting document (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    
    printf("Encrypting with encryption type %u\n", m->rh->encryption_type);
    if (m->rh->encryption_type == MOBI_ENCRYPTION_V1 && serial_count) {
        printf("Warning! Encryption with device serial number is not supported with this encryption scheme. Skipping serial...\n");
    }
    if (valid_from != -1) {
        printf("Drm validity period from %s", ctime(&valid_from));
    }
    if (valid_to != -1) {
        printf("Drm validity period to %s", ctime(&valid_to));
    }
    return SUCCESS;
}

/**
 @brief Removes DRM
 
 @param[in,out] m MOBIData structure
 @return SUCCESS or ERROR
 */
static int do_decrypt(MOBIData *m) {
    MOBIExthHeader *exth = mobi_get_exthrecord_by_tag(m, EXTH_RENTAL);
    if (exth) {
        uint32_t is_rental = mobi_decode_exthvalue(exth->data, exth->size);
        if (is_rental) {
            printf("Can't remove DRM from rented documents\n");
            return ERROR;
        }
    }
    uint16_t encryption_type = m->rh->encryption_type;
    printf("Removing encryption type %u\n", encryption_type);
    bool has_key = false;
    if (pid_count) {
        for (size_t i = 0; i < pid_count; i++) {
            if (set_decryption_pid(m, pid[i]) == SUCCESS) {
                has_key = true;
                break;
            }
        }
    }
    if (!has_key && serial_count) {
        for (size_t i = 0; i < serial_count; i++) {
            if (set_decryption_serial(m, serial[i]) == SUCCESS) {
                break;
            }
        }
    }
    MOBI_RET mobi_ret = mobi_drm_decrypt(m);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Error encrypting document (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    
    if (encryption_type == MOBI_ENCRYPTION_V2) {
        // remove EXTH records that impose restrictions or are DRM related
        MOBIExthTag drmkeys[] = { EXTH_TAMPERKEYS, EXTH_WATERMARK, EXTH_TTSDISABLE, EXTH_CLIPPINGLIMIT, EXTH_READFORFREE, EXTH_RENTAL, EXTH_UNK407 };
        for (size_t i = 0; i < ARRAYSIZE(drmkeys); i++) {
            mobi_ret = mobi_delete_exthrecord_by_tag(m, drmkeys[i]);
            if (mobi_ret != MOBI_SUCCESS) {
                printf("Error removing EXTH record %u\n", drmkeys[i]);
                return ERROR;
            }
        }
    }
    return SUCCESS;
}

/**
 @brief Main routine that calls optional subroutines
 
 @param[in] fullpath Full file path
 @return SUCCESS or ERROR
 */
static int loadfilename(const char *fullpath) {

    static int run_count = 0;
    run_count++;
    
    bool use_kf8 = run_count == 1 ? false : true;
    
    /* Initialize main MOBIData structure */
    MOBIData *m = mobi_init();
    if (m == NULL) {
        printf("Memory allocation failed\n");
        return ERROR;
    }
    
    errno = 0;
    FILE *file = fopen(fullpath, "rb");
    if (file == NULL) {
        int errsv = errno;
        printf("Error opening file: %s (%s)\n", fullpath, strerror(errsv));
        mobi_free(m);
        return ERROR;
    }
    
    /* MOBIData structure will be filled with loaded document data and metadata */
    MOBI_RET mobi_ret = mobi_load_file(m, file);
    fclose(file);
    
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Error while loading document (%s)\n", libmobi_msg(mobi_ret));
        mobi_free(m);
        return ERROR;
    }
    
    if (run_count == 1) {
        print_summary(m);
    }

    // save is hybrid result here, as file may be modified later
    bool is_hybrid = mobi_is_hybrid(m);
    
    int ret = SUCCESS;
    const char *suffix;
    if (encrypt_opt) {
        // encrypt
        if (mobi_is_encrypted(m)) {
            mobi_free(m);
            printf("Document is already encrypted\n");
            return ERROR;
        }
        ret = do_encrypt(m, use_kf8);
        if (ret != SUCCESS) {
            mobi_free(m);
            return ERROR;
        }
        suffix = "encrypted";
    } else {
        // decrypt
        if (!mobi_is_encrypted(m)) {
            mobi_free(m);
            printf("Document is not encrypted\n");
            return ERROR;
        }
        ret = do_decrypt(m);
        if (ret != SUCCESS) {
            mobi_free(m);
            return ERROR;
        }
        suffix = "decrypted";
    }
    
    ret = save_mobi(m, fullpath, suffix);
    if (ret != SUCCESS) {
        mobi_free(m);
        return ERROR;
    }
    
    /* Free MOBIData structure */
    mobi_free(m);
    
    /* In case of encrypting hybrid file proceed with KF8 part */
    if (encrypt_opt && is_hybrid && use_kf8 == false) {
        loadfilename(fullpath);
    }
    return SUCCESS;
}

/**
 @brief Parse ISO8601 date into tm structure
 
 @param[in,out] tm Structure to be filled
 @param[in] date_str Input date string
 @return SUCCESS or ERROR
 */
static int parse_date(struct tm *tm, const char *date_str) {
    memset(tm, '\0', sizeof(*tm));
    int year;
    int month;
    int day;
    if (sscanf(date_str, "%5d-%2d-%2d", &year, &month, &day) != 3) {
        return ERROR;
    }
    if (year < 1000 || month < 1 || month > 12 || day < 1 || day > 31) {
        return ERROR;
    }
    
    tm->tm_year = year - 1900;
    tm->tm_mon = month - 1;
    tm->tm_mday = day;
    tm->tm_isdst = -1;
    
    return SUCCESS;
}

/**
 @brief Main
 
 @param[in] argc Arguments count
 @param[in] argv Arguments array
 @return SUCCESS (0) or ERROR (1)
 */
int main(int argc, char *argv[]) {
    if (argc < 2) {
        print_usage(argv[0]);
        return ERROR;
    }
    opterr = 0;
    int c;
    while ((c = getopt(argc, argv, "def:ho:p:s:t:vx:")) != -1) {
        switch(c) {
            case 'd':
                if (encrypt_opt) {
                    printf("Options -d and -e can't be used together.\n");
                    return ERROR;
                }
                decrypt_opt = true;
                break;
            case 'e':
                if (decrypt_opt) {
                    printf("Options -d and -e can't be used together.\n");
                    return ERROR;
                }
                encrypt_opt = true;
                break;
            case 'f':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", c);
                    return ERROR;
                }
                struct tm from;
                if (parse_date(&from, optarg) != SUCCESS) {
                    printf("Wrong valid from date format, use ISO 8601 yyyy-mm-dd\n");
                    return ERROR;
                }
                valid_from = mktime(&from);
                if (valid_from == -1) {
                    printf("Unparsable valid from date format, use ISO 8601 yyyy-mm-dd\n");
                    return ERROR;  
                }
                expiry_opt = true;
                break;
            case 'o':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", c);
                    return ERROR;
                }
                size_t outdir_length = strlen(optarg);
                if (outdir_length >= FILENAME_MAX - 1) {
                    printf("Output directory name too long\n");
                    return ERROR;
                }
                strncpy(outdir, optarg, FILENAME_MAX - 1);
                normalize_path(outdir);
                if (!dir_exists(outdir)) {
                    printf("Output directory is not valid\n");
                    return ERROR;
                }
                if (optarg[outdir_length - 1] != separator) {
                    // append separator
                    if (outdir_length >= FILENAME_MAX - 2) {
                        printf("Output directory name too long\n");
                        return ERROR;
                    }
                    outdir[outdir_length++] = separator;
                    outdir[outdir_length] = '\0';
                }
                outdir_opt = true;
                break;
            case 'p':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", c);
                    return ERROR;
                }
                if (pid_count < VOUCHERS_COUNT_MAX) {
                    pid[pid_count++] = optarg;
                } else {
                    printf("Maximum PIDs count reached, skipping PID...\n");
                }
                break;
            case 's':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", c);
                    return ERROR;
                }
                if (serial_count < VOUCHERS_COUNT_MAX) {
                    serial[serial_count++] = optarg;
                } else {
                    printf("Maximum serial numbers count reached, skipping serial...\n");
                }
                break;
            case 't':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", c);
                    return ERROR;
                }
                struct tm to;
                if (parse_date(&to, optarg) != SUCCESS) {
                    printf("Wrong valid to date format, use ISO 8601 yyyy-mm-dd\n");
                    return ERROR;
                }
                to.tm_hour = 23;
                to.tm_min = 59;
                to.tm_sec = 59;
                valid_to = mktime(&to);
                if (valid_to == -1) {
                    printf("Unparsable valid to date format, use ISO 8601 yyyy-mm-dd\n");
                    return ERROR;  
                }
                expiry_opt = true;
                break;
            case 'v':
                printf("mobidrm build: " __DATE__ " " __TIME__ " (" COMPILER ")\n");
                printf("libmobi: %s\n", mobi_version());
                return SUCCESS;
            case '?':
                if (isprint(optopt)) {
                    fprintf(stderr, "Unknown option `-%c'\n", optopt);
                }
                else {
                    fprintf(stderr, "Unknown option character `\\x%x'\n", optopt);
                }
                print_usage(argv[0]);
                return ERROR;
            case 'h':
            default:
                print_usage(argv[0]);
                return SUCCESS;
        }
    }
    
    if (argc <= optind) {
        printf("Missing filename\n");
        print_usage(argv[0]);
        return ERROR;
    }
    
    if (!decrypt_opt && !encrypt_opt) {
        printf("One of -d (decrypt) and -e (encrypt) options is required\n");
        print_usage(argv[0]);
        return ERROR;
    }

    if (!encrypt_opt && expiry_opt) {
        printf("Expiration flags can only be used with -e (encrypt)\n");
        print_usage(argv[0]);
        return ERROR;
    }
    
    if (!decrypt_opt && pid_count) {
        printf("PID can only be used with -d (decrypt)\n");
        print_usage(argv[0]);
        return ERROR;
    }
    
    int ret = 0;
    char filename[FILENAME_MAX];
    strncpy(filename, argv[optind], FILENAME_MAX - 1);
    filename[FILENAME_MAX - 1] = '\0';
    normalize_path(filename);

    ret = loadfilename(filename);
    
    return ret;
}
