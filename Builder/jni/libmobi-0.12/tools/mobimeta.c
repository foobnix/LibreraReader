/** @file mobimeta.c
 *
 * @brief mobimeta
 *
 * @example mobimeta.c
 * Program for testing libmobi library
 *
 * Copyright (c) 2020 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <stdio.h>
#include <stdbool.h>
#include <stdlib.h>
#include <ctype.h>
#include <errno.h>
#include <string.h>
#include <mobi.h>

#include "common.h"

/* encryption */
#ifdef USE_ENCRYPTION
# define PRINT_ENC_USG " [-p pid] [-P serial]"
# define PRINT_ENC_ARG "p:P:"
#else
# define PRINT_ENC_USG ""
# define PRINT_ENC_ARG ""
#endif

/* command line options */
#ifdef USE_ENCRYPTION
bool setpid_opt = false;
bool setserial_opt = false;
#endif

/* options values */
#ifdef USE_ENCRYPTION
char *pid = NULL;
char *serial = NULL;
#endif

#define META_SIZE ARRAYSIZE(meta_functions)
#define ACTIONS_SIZE ARRAYSIZE(actions)

#if HAVE_ATTRIBUTE_NORETURN 
static void exit_with_usage(const char *progname) __attribute__((noreturn));
#else
static void exit_with_usage(const char *progname);
#endif

/**
 @brief Meta handling functions
 */
typedef MOBI_RET (*MetaFunAdd)(MOBIData *m, const char *string);
typedef MOBI_RET (*MetaFunDel)(MOBIData *m);

/**
 @brief Meta functions structure
 */
typedef struct {
    const char *name;
    MetaFunAdd function_add;
    MetaFunAdd function_set;
    MetaFunDel function_del;
} CB;

const CB meta_functions[] = {
    { "author", mobi_meta_add_author, mobi_meta_set_author, mobi_meta_delete_author },
    { "title", mobi_meta_add_title, mobi_meta_set_title, mobi_meta_delete_title },
    { "publisher", mobi_meta_add_publisher, mobi_meta_set_publisher, mobi_meta_delete_publisher },
    { "imprint", mobi_meta_add_imprint, mobi_meta_set_imprint, mobi_meta_delete_imprint },
    { "description", mobi_meta_add_description, mobi_meta_set_description, mobi_meta_delete_description },
    { "isbn", mobi_meta_add_isbn, mobi_meta_set_isbn, mobi_meta_delete_isbn },
    { "subject", mobi_meta_add_subject, mobi_meta_set_subject, mobi_meta_delete_subject },
    { "publishdate", mobi_meta_add_publishdate, mobi_meta_set_publishdate, mobi_meta_delete_publishdate },
    { "review", mobi_meta_add_review, mobi_meta_set_review, mobi_meta_delete_review },
    { "contributor", mobi_meta_add_contributor, mobi_meta_set_contributor, mobi_meta_delete_contributor },
    { "copyright", mobi_meta_add_copyright, mobi_meta_set_copyright, mobi_meta_delete_copyright },
    { "asin", mobi_meta_add_asin, mobi_meta_set_asin, mobi_meta_delete_asin },
    { "language", mobi_meta_add_language, mobi_meta_set_language, mobi_meta_delete_language },
};

/**
 @brief Print usage info
 @param[in] progname Executed program name
 */
static void exit_with_usage(const char *progname) {
    char *p = strrchr(progname, separator);
    if (p) { progname = ++p; }
    printf("usage: %s [-a | -s meta=value[,meta=value,...]] [-d meta[,meta,...]]" PRINT_ENC_USG " [-hv] filein [fileout]\n", progname);
    printf("       without arguments prints document metadata and exits\n");
    printf("       -a ?           list valid meta named keys\n");
    printf("       -a meta=value  add metadata\n");
    printf("       -d meta        delete metadata\n");
    printf("       -s meta=value  set metadata\n");
#ifdef USE_ENCRYPTION
    printf("       -p pid         set pid for decryption\n");
    printf("       -P serial      set device serial for decryption\n");
#endif
    printf("       -h             show this usage summary and exit\n");
    printf("       -v             show version and exit\n");
    exit(ERROR);
}

/**
 @brief Check whether string is integer
 @param[in] string String
 @return True if string represents integer
 */
static bool isinteger(const char *string) {
    if (*string == '\0') { return false; }
    while (*string) {
        if (!isdigit(*string++)) { return false; }
    }
    return true;
}

/**
 @brief Parse suboption's list key=value[,key=value,...]
 and get first key-value pair.
 @param[in,out] subopts List of suboptions
 @param[in,out] token Will be filled with first found key name or NULL if missing
 @param[in,out] value Will be filled with first found key value or NULL if missing
 @return True if there are more pairs to parse
 */
static bool parsesubopt(char **subopts, char **token, char **value) {
    if (!**subopts) { return false; }
    *token = NULL;
    *value = NULL;
    char *p = NULL;
    while ((p = (*subopts)++) && *p) {
        if (*p == ',') {
            *p = '\0';
            return true;
        }
        if (!*token) { *token = p; }
        if (*p == '=') {
            *p = '\0';
            *value = ++p;
        }
    }
    return false;
}

/**
 @brief Get matching token from meta functions array
 @param[in] token Meta token name
 @return Index in array,-1 if not found
 */
static int get_meta(const char *token) {
    for (int i = 0; i < (int) META_SIZE; i++) {
        if (strcmp(token, meta_functions[i].name) == 0) {
            return i;
        }
    }
    return -1;
}

/**
 @brief Main
 
 @param[in] argc Arguments count
 @param[in] argv Arguments array
 @return SUCCESS (0) or ERROR (1)
 */
int main(int argc, char *argv[]) {
    if (argc < 2) {
        exit_with_usage(argv[0]);
    }
    
    typedef struct {
        int command;
        int meta;
        char *value;
    } Action;
    Action actions[100];
    
    size_t cmd_count = 0;
    
    char *token = NULL;
    char *value = NULL;
    char *subopts;
    int opt;
    int subopt;
    bool parse;
    while ((opt = getopt(argc, argv, "a:d:hs:" PRINT_ENC_ARG "v")) != -1) {
        switch (opt) {
            case 'a':
            case 'd':
            case 's':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", opt);
                    return ERROR;
                }
                subopts = optarg;
                parse = true;
                while (parse) {
                    parse = parsesubopt(&subopts, &token, &value);
                    if (token && (subopt = get_meta(token)) >= 0) {
                        if ((value == NULL || strlen(value) == 0) && opt != 'd') {
                            printf("Missing value for suboption '%s'\n\n", meta_functions[subopt].name);
                            exit_with_usage(argv[0]);
                        }
                        if (cmd_count < (ACTIONS_SIZE)) {
                            actions[cmd_count++] = (Action) { opt, subopt, value };
                        }
                    } else if (token && isinteger(token)) {
                        if ((value == NULL || strlen(value) == 0) && opt != 'd') {
                            printf("Missing value for suboption '%s'\n\n", token);
                            exit_with_usage(argv[0]);
                        }
                        if (cmd_count < (ACTIONS_SIZE)) {
                            /* mark numeric meta with upper opt */
                            actions[cmd_count++] = (Action) { toupper(opt), atoi(token), value };
                        }
                    } else {
                        if (token == NULL || token[0] != '?') {
                            printf("Unknown meta: %s\n", token ? token : optarg);
                        }
                        printf("Valid named meta keys:\n");
                        for (size_t i = 0; i < META_SIZE; i++) {
                            printf("   %s\n", meta_functions[i].name);
                        }
                        printf("\n");
                        if (token == NULL || token[0] != '?') {
                            exit_with_usage(argv[0]);
                        }
                        return SUCCESS;
                    }
                }
                break;
#ifdef USE_ENCRYPTION
            case 'p':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", opt);
                    return ERROR;
                }
                setpid_opt = true;
                pid = optarg;
                break;
            case 'P':
                if (strlen(optarg) == 2 && optarg[0] == '-') {
                    printf("Option -%c requires an argument.\n", opt);
                    return ERROR;
                }
                setserial_opt = true;
                serial = optarg;
                break;
#endif
            case 'v':
                printf("mobimeta build: " __DATE__ " " __TIME__ " (" COMPILER ")\n");
                printf("libmobi: %s\n", mobi_version());
                return 0;
            case '?':
                if (isprint(optopt)) {
                    printf("Unknown option `-%c'\n", optopt);
                }
                else {
                    printf("Unknown option character `\\x%x'\n", optopt);
                }
                exit_with_usage(argv[0]);
            case 'h':
            default:
                exit_with_usage(argv[0]);
        }
    }
    
    int file_args = argc - optind;
    if (file_args <= 0) {
        printf("Missing filename\n");
        exit_with_usage(argv[0]);
    }
    char infile[FILENAME_MAX];
    strncpy(infile, argv[optind], FILENAME_MAX - 1);
    infile[FILENAME_MAX - 1] = '\0';
    normalize_path(infile);

    if (file_args >= 2) { optind++; }
    
    char outfile[FILENAME_MAX];
    strncpy(outfile, argv[optind], FILENAME_MAX - 1);
    outfile[FILENAME_MAX - 1] = '\0';
    normalize_path(outfile);

    /* Initialize MOBIData structure */
    MOBIData *m = mobi_init();
    if (m == NULL) {
        printf("Libmobi initialization failed\n");
        return ERROR;
    }
    
    /* read */
    FILE *file_in = fopen(infile, "rb");
    if (file_in == NULL) {
        mobi_free(m);
        int errsv = errno;
        printf("Error opening file: %s (%s)\n", infile, strerror(errsv));
        return ERROR;
    }
    MOBI_RET mobi_ret = mobi_load_file(m, file_in);
    fclose(file_in);
    if (mobi_ret != MOBI_SUCCESS) {
        mobi_free(m);
        printf("Error loading file (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    
#ifdef USE_ENCRYPTION
    if (setpid_opt || setserial_opt) {
        int ret = set_decryption_key(m, serial, pid);
        if (ret != SUCCESS) {
            mobi_free(m);
            return ret;
        }
    }
#endif
    
    if (cmd_count == 0) {
        print_summary(m);
        print_exth(m);
        mobi_free(m);
        return SUCCESS;
    }
    
    for (size_t i = 0; i < cmd_count; i++) {
        Action action = actions[i];
        MOBIExthMeta tag;
        mobi_ret = MOBI_SUCCESS;
        switch (action.command) {
                /* named meta */
            case 'a':
                mobi_ret = meta_functions[action.meta].function_add(m, action.value);
                break;
            case 'd':
                mobi_ret = meta_functions[action.meta].function_del(m);
                break;
            case 's':
                mobi_ret = meta_functions[action.meta].function_set(m, action.value);
                break;
                /* numeric exth */
            case 'A':
                tag = mobi_get_exthtagmeta_by_tag((MOBIExthTag) action.meta);
                if (tag.name && tag.type == EXTH_NUMERIC && isinteger(action.value)) {
                    uint32_t numeric = (uint32_t) atoi(action.value);
                    mobi_ret = mobi_add_exthrecord(m, (MOBIExthTag) action.meta, sizeof(uint32_t), &numeric);
                } else {
                    mobi_ret = mobi_add_exthrecord(m, (MOBIExthTag) action.meta, (uint32_t) strlen(action.value), action.value);
                }
                break;
            case 'D':
                mobi_ret = mobi_delete_exthrecord_by_tag(m, (MOBIExthTag) action.meta);
                break;
            case 'S':
                mobi_ret = mobi_delete_exthrecord_by_tag(m, (MOBIExthTag) action.meta);
                if (mobi_ret != MOBI_SUCCESS) { break; }
                tag = mobi_get_exthtagmeta_by_tag((MOBIExthTag) action.meta);
                if (tag.name && tag.type == EXTH_NUMERIC && isinteger(action.value)) {
                    uint32_t numeric = (uint32_t) atoi(action.value);
                    mobi_ret = mobi_add_exthrecord(m, (MOBIExthTag) action.meta, sizeof(uint32_t), &numeric);
                } else {
                    mobi_ret = mobi_add_exthrecord(m, (MOBIExthTag) action.meta, (uint32_t) strlen(action.value), action.value);
                }
                break;
        }
        if (mobi_ret != MOBI_SUCCESS) {
            printf("Metadata modification failed (%s)\n", libmobi_msg(mobi_ret));
            mobi_free(m);
            return ERROR;
        }
        
    }
    
    /* write */
    printf("Saving %s...\n", outfile);
    FILE *file_out = fopen(outfile, "wb");
    if (file_out == NULL) {
        mobi_free(m);
        int errsv = errno;
        printf("Error opening file: %s (%s)\n", outfile, strerror(errsv));
        return ERROR;
    }
    mobi_ret = mobi_write_file(file_out, m);
    fclose(file_out);
    if (mobi_ret != MOBI_SUCCESS) {
        mobi_free(m);
        printf("Error writing file (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    
    /* Free MOBIData structure */
    mobi_free(m);
    
    return SUCCESS;
}
