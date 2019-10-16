/** @file mobitool.c
 *
 * @brief mobitool
 *
 * @example mobitool.c
 * Program for testing libmobi library
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#include <jni.h>
#include <string.h>
#include <stdlib.h>
#ifdef _WIN32
#include <direct.h>
#include "win32/getopt.h"
#else
#include <unistd.h>
#endif
#include <ctype.h>
#include <time.h>
#include <errno.h>
/* include libmobi header */
#include <mobi.h>
#include "common.h"
#ifdef HAVE_CONFIG_H
# include "../config.h"
#endif
/* miniz file is needed for EPUB creation */
#ifdef USE_XMLWRITER
# define MINIZ_HEADER_FILE_ONLY
# define MINIZ_NO_ZLIB_COMPATIBLE_NAMES
# include "miniz.c"
#endif

#ifdef HAVE_SYS_RESOURCE_H
/* rusage */
# include <sys/resource.h>
# define PRINT_RUSAGE_ARG "u"
#else
# define PRINT_RUSAGE_ARG ""
#endif
/* encryption */
#ifdef USE_ENCRYPTION
# define PRINT_ENC_USG " [-p pid] [-P serial]"
# define PRINT_ENC_ARG "p:P:"
#else
# define PRINT_ENC_USG ""
# define PRINT_ENC_ARG ""
#endif
/* xmlwriter */
#ifdef USE_XMLWRITER
# define PRINT_EPUB_ARG "e"
#else
# define PRINT_EPUB_ARG ""
#endif


#define EPUB_CONTAINER "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\
<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n\
  <rootfiles>\n\
    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n\
  </rootfiles>\n\
</container>"
#define EPUB_MIMETYPE "application/epub+zip"

/* command line options */
int dump_rawml_opt = 0;
int create_epub_opt = 0;
int print_extended_meta_opt = 0;
int print_rec_meta_opt = 0;
int dump_rec_opt = 0;
int parse_kf7_opt = 0;
int dump_parts_opt = 0;
int print_rusage_opt = 0;
int outdir_opt = 0;
int extract_source_opt = 0;
#ifdef USE_ENCRYPTION
int setpid_opt = 0;
int setserial_opt = 0;
#endif

/* options values */
char *outdir;
#ifdef USE_ENCRYPTION
char *pid = NULL;
char *serial = NULL;
#endif

/**
 @brief Print all loaded headers meta information
 @param[in] m MOBIData structure
 */
void print_meta(const MOBIData *m) {
    /* Full name stored at offset given in MOBI header */
    if (m->mh && m->mh->full_name) {
        char full_name[FULLNAME_MAX + 1];
        if (mobi_get_fullname(m, full_name, FULLNAME_MAX) == MOBI_SUCCESS) {
            printf("\nFull name: %s\n", full_name);
        }
    }
    /* Palm database header */
    if (m->ph) {
        printf("\nPalm doc header:\n");
        printf("name: %s\n", m->ph->name);
        printf("attributes: %hu\n", m->ph->attributes);
        printf("version: %hu\n", m->ph->version);
        struct tm * timeinfo = mobi_pdbtime_to_time(m->ph->ctime);
        printf("ctime: %s", asctime(timeinfo));
        timeinfo = mobi_pdbtime_to_time(m->ph->mtime);
        printf("mtime: %s", asctime(timeinfo));
        timeinfo = mobi_pdbtime_to_time(m->ph->btime);
        printf("btime: %s", asctime(timeinfo));
        printf("mod_num: %u\n", m->ph->mod_num);
        printf("appinfo_offset: %u\n", m->ph->appinfo_offset);
        printf("sortinfo_offset: %u\n", m->ph->sortinfo_offset);
        printf("type: %s\n", m->ph->type);
        printf("creator: %s\n", m->ph->creator);
        printf("uid: %u\n", m->ph->uid);
        printf("next_rec: %u\n", m->ph->next_rec);
        printf("rec_count: %u\n", m->ph->rec_count);
    }
    /* Record 0 header */
    if (m->rh) {
        printf("\nRecord 0 header:\n");
        printf("compresion type: %u\n", m->rh->compression_type);
        printf("text length: %u\n", m->rh->text_length);
        printf("text record count: %u\n", m->rh->text_record_count);
        printf("text record size: %u\n", m->rh->text_record_size);
        printf("encryption type: %u\n", m->rh->encryption_type);
        printf("unknown: %u\n", m->rh->unknown1);
    }
    /* Mobi header */
    if (m->mh) {
        printf("\nMOBI header:\n");
        printf("identifier: %s\n", m->mh->mobi_magic);
        if(m->mh->header_length) { printf("header length: %u\n", *m->mh->header_length); }
        if(m->mh->mobi_type) { printf("mobi type: %u\n", *m->mh->mobi_type); }
        if(m->mh->text_encoding) { printf("text encoding: %u\n", *m->mh->text_encoding); }
        if(m->mh->uid) { printf("unique id: %u\n", *m->mh->uid); }
        if(m->mh->version) { printf("file version: %u\n", *m->mh->version); }
        if(m->mh->orth_index) { printf("orth index: %u\n", *m->mh->orth_index); }
        if(m->mh->infl_index) { printf("infl index: %u\n", *m->mh->infl_index); }
        if(m->mh->names_index) { printf("names index: %u\n", *m->mh->names_index); }
        if(m->mh->keys_index) { printf("keys index: %u\n", *m->mh->keys_index); }
        if(m->mh->extra0_index) { printf("extra0 index: %u\n", *m->mh->extra0_index); }
        if(m->mh->extra1_index) { printf("extra1 index: %u\n", *m->mh->extra1_index); }
        if(m->mh->extra2_index) { printf("extra2 index: %u\n", *m->mh->extra2_index); }
        if(m->mh->extra3_index) { printf("extra3 index: %u\n", *m->mh->extra3_index); }
        if(m->mh->extra4_index) { printf("extra4 index: %u\n", *m->mh->extra4_index); }
        if(m->mh->extra5_index) { printf("extra5 index: %u\n", *m->mh->extra5_index); }
        if(m->mh->non_text_index) { printf("non text index: %u\n", *m->mh->non_text_index); }
        if(m->mh->full_name_offset) { printf("full name offset: %u\n", *m->mh->full_name_offset); }
        if(m->mh->full_name_length) { printf("full name length: %u\n", *m->mh->full_name_length); }
        if(m->mh->locale) {
            const char *locale_string = mobi_get_locale_string(*m->mh->locale);
            if (locale_string) {
                printf("locale: %s (%u)\n", locale_string, *m->mh->locale);
            } else {
                printf("locale: unknown (%u)\n", *m->mh->locale);
            }
        }
        if(m->mh->dict_input_lang) {
            const char *locale_string = mobi_get_locale_string(*m->mh->dict_input_lang);
            if (locale_string) {
                printf("dict input lang: %s (%u)\n", locale_string, *m->mh->dict_input_lang);
            } else {
                printf("dict input lang: unknown (%u)\n", *m->mh->dict_input_lang);
            }
        }
        if(m->mh->dict_output_lang) {
            const char *locale_string = mobi_get_locale_string(*m->mh->dict_output_lang);
            if (locale_string) {
                printf("dict output lang: %s (%u)\n", locale_string, *m->mh->dict_output_lang);
            } else {
                printf("dict output lang: unknown (%u)\n", *m->mh->dict_output_lang);
            }
        }
        if(m->mh->min_version) { printf("minimal version: %u\n", *m->mh->min_version); }
        if(m->mh->image_index) { printf("first image index: %u\n", *m->mh->image_index); }
        if(m->mh->huff_rec_index) { printf("huffman record offset: %u\n", *m->mh->huff_rec_index); }
        if(m->mh->huff_rec_count) { printf("huffman records count: %u\n", *m->mh->huff_rec_count); }
        if(m->mh->datp_rec_index) { printf("DATP record offset: %u\n", *m->mh->datp_rec_index); }
        if(m->mh->datp_rec_count) { printf("DATP records count: %u\n", *m->mh->datp_rec_count); }
        if(m->mh->exth_flags) { printf("EXTH flags: %u\n", *m->mh->exth_flags); }
        if(m->mh->unknown6) { printf("unknown: %u\n", *m->mh->unknown6); }
        if(m->mh->drm_offset) { printf("drm offset: %u\n", *m->mh->drm_offset); }
        if(m->mh->drm_count) { printf("drm count: %u\n", *m->mh->drm_count); }
        if(m->mh->drm_size) { printf("drm size: %u\n", *m->mh->drm_size); }
        if(m->mh->drm_flags) { printf("drm flags: %u\n", *m->mh->drm_flags); }
        if(m->mh->first_text_index) { printf("first text index: %u\n", *m->mh->first_text_index); }
        if(m->mh->last_text_index) { printf("last text index: %u\n", *m->mh->last_text_index); }
        if(m->mh->fdst_index) { printf("FDST offset: %u\n", *m->mh->fdst_index); }
        if(m->mh->fdst_section_count) { printf("FDST count: %u\n", *m->mh->fdst_section_count); }
        if(m->mh->fcis_index) { printf("FCIS index: %u\n", *m->mh->fcis_index); }
        if(m->mh->fcis_count) { printf("FCIS count: %u\n", *m->mh->fcis_count); }
        if(m->mh->flis_index) { printf("FLIS index: %u\n", *m->mh->flis_index); }
        if(m->mh->flis_count) { printf("FLIS count: %u\n", *m->mh->flis_count); }
        if(m->mh->unknown10) { printf("unknown: %u\n", *m->mh->unknown10); }
        if(m->mh->unknown11) { printf("unknown: %u\n", *m->mh->unknown11); }
        if(m->mh->srcs_index) { printf("SRCS index: %u\n", *m->mh->srcs_index); }
        if(m->mh->srcs_count) { printf("SRCS count: %u\n", *m->mh->srcs_count); }
        if(m->mh->unknown12) { printf("unknown: %u\n", *m->mh->unknown12); }
        if(m->mh->unknown13) { printf("unknown: %u\n", *m->mh->unknown13); }
        if(m->mh->extra_flags) { printf("extra record flags: %u\n", *m->mh->extra_flags); }
        if(m->mh->ncx_index) { printf("NCX offset: %u\n", *m->mh->ncx_index); }
        if(m->mh->unknown14) { printf("unknown: %u\n", *m->mh->unknown14); }
        if(m->mh->unknown15) { printf("unknown: %u\n", *m->mh->unknown15); }
        if(m->mh->fragment_index) { printf("fragment index: %u\n", *m->mh->fragment_index); }
        if(m->mh->skeleton_index) { printf("skeleton index: %u\n", *m->mh->skeleton_index); }
        if(m->mh->datp_index) { printf("DATP index: %u\n", *m->mh->datp_index); }
        if(m->mh->unknown16) { printf("unknown: %u\n", *m->mh->unknown16); }
        if(m->mh->guide_index) { printf("guide index: %u\n", *m->mh->guide_index); }
        if(m->mh->unknown17) { printf("unknown: %u\n", *m->mh->unknown17); }
        if(m->mh->unknown18) { printf("unknown: %u\n", *m->mh->unknown18); }
        if(m->mh->unknown19) { printf("unknown: %u\n", *m->mh->unknown19); }
        if(m->mh->unknown20) { printf("unknown: %u\n", *m->mh->unknown20); }
    }
}

/**
 @brief Print meta data of each document record
 @param[in] m MOBIData structure
 */
void print_records_meta(const MOBIData *m) {
    /* Linked list of MOBIPdbRecord structures holds records data and metadata */
    const MOBIPdbRecord *currec = m->rec;
    while (currec != NULL) {
        printf("offset: %u\n", currec->offset);
        printf("size: %zu\n", currec->size);
        printf("attributes: %hhu\n", currec->attributes);
        printf("uid: %u\n", currec->uid);
        printf("\n");
        currec = currec->next;
    }
}

/**
 @brief Dump each document record to a file into created folder
 @param[in] m MOBIData structure
 @param[in] fullpath File path will be parsed to build basenames of dumped records
 */
int dump_records(const MOBIData *m, const char *fullpath) {
    char dirname[FILENAME_MAX];
    char basename[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename);
    char newdir[FILENAME_MAX];
    if (outdir_opt) {
        snprintf(newdir, sizeof(newdir), "%s%s_records", outdir, basename);
    } else {
        snprintf(newdir, sizeof(newdir), "%s%s_records", dirname, basename);
    }
    printf("Saving records to %s\n", newdir);
    errno = 0;
    if (mt_mkdir(newdir) != 0 && errno != EEXIST) {
        int errsv = errno;
        printf("Creating directory failed (%s)\n", strerror(errsv));
        return ERROR;
    }
    /* Linked list of MOBIPdbRecord structures holds records data and metadata */
    const MOBIPdbRecord *currec = m->rec;
    int i = 0;
    while (currec != NULL) {
        char name[FILENAME_MAX];
        snprintf(name, sizeof(name), "%s%crecord_%i_uid_%i", newdir, separator, i++, currec->uid);
        errno = 0;
        FILE *file = fopen(name, "wb");
        if (file == NULL) {
            int errsv = errno;
            printf("Could not open file for writing: %s (%s)\n", name, strerror(errsv));
            return ERROR;
        }
        fwrite(currec->data, 1, currec->size, file);
        fclose(file);
        currec = currec->next;
    }
    return SUCCESS;
}

/**
 @brief Dump all text records, decompressed and concatenated, to a single rawml file
 @param[in] m MOBIData structure
 @param[in] fullpath File path will be parsed to create a new name for saved file
 */
int dump_rawml(const MOBIData *m, const char *fullpath) {
    char dirname[FILENAME_MAX];
    char basename[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename);
    char newdir[FILENAME_MAX];
    
    if (outdir_opt) {
        snprintf(newdir, sizeof(newdir), "%s%s.rawml", outdir, basename);
    } else {
        snprintf(newdir, sizeof(newdir), "%s%s.rawml", dirname, basename);
    }
    printf("Saving rawml to %s\n", newdir);
    errno = 0;
    FILE *file = fopen(newdir, "wb");
    if (file == NULL) {
        int errsv = errno;
        printf("Could not open file for writing: %s (%s)\n", newdir, strerror(errsv));
        return ERROR;
    }
    const MOBI_RET mobi_ret = mobi_dump_rawml(m, file);
    fclose(file);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Dumping rawml file failed (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    return SUCCESS;
}

/**
 @brief Dump parsed markup files and resources into created folder
 @param[in] rawml MOBIRawml structure holding parsed records
 @param[in] fullpath File path will be parsed to build basenames of dumped records
 */
int dump_rawml_parts(const MOBIRawml *rawml, const char *fullpath) {
    if (rawml == NULL) {
        printf("Rawml structure not initialized\n");
        return ERROR;
    }
    char dirname[FILENAME_MAX];
    char basename[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename);
    char newdir[FILENAME_MAX];
    if (outdir_opt) {
        snprintf(newdir, sizeof(newdir), "%s%s_markup", outdir, basename);
    } else {
        snprintf(newdir, sizeof(newdir), "%s%s_markup", dirname, basename);
    }
    printf("Saving markup to %s\n", newdir);
    errno = 0;
    if (mt_mkdir(newdir) != 0 && errno != EEXIST) {
        int errsv = errno;
        printf("Creating directory failed (%s)\n", strerror(errsv));
        return ERROR;
    }
    if (create_epub_opt) {
        /* create META_INF directory */
        char opfdir[FILENAME_MAX];
        snprintf(opfdir, sizeof(newdir), "%s%c%s", newdir, separator, "META-INF");
        errno = 0;
        if (mt_mkdir(opfdir) != 0 && errno != EEXIST) {
            int errsv = errno;
            printf("Creating META-INF directory failed (%s)\n", strerror(errsv));
            return ERROR;
        }
        /* create container.xml */
        char container[FILENAME_MAX];
        snprintf(container, sizeof(container), "%s%c%s", opfdir, separator, "container.xml");
        FILE *file = fopen(container, "wb");
        if (file == NULL) {
            int errsv = errno;
            printf("Could not open file for writing: %s (%s)\n", container, strerror(errsv));
            return ERROR;
        }
        errno = 0;
        fwrite(EPUB_CONTAINER, 1, sizeof(EPUB_CONTAINER) - 1, file);
        if (ferror(file)) {
            int errsv = errno;
            printf("Error writing: %s (%s)\n", container, strerror(errsv));
            fclose(file);
            return ERROR;
        }
        fclose(file);
        /* create mimetype file */
        snprintf(container, sizeof(container), "%s%c%s", newdir, separator, "mimetype");
        file = fopen(container, "wb");
        if (file == NULL) {
            int errsv = errno;
            printf("Could not open file for writing: %s (%s)\n", container, strerror(errsv));
            return ERROR;
        }
        errno = 0;
        fwrite(EPUB_MIMETYPE, 1, sizeof(EPUB_MIMETYPE) - 1, file);
        if (ferror(file)) {
            int errsv = errno;
            printf("Error writing: %s (%s)\n", container, strerror(errsv));
            fclose(file);
            return ERROR;
        }
        fclose(file);
        /* create OEBPS directory */
        snprintf(opfdir, sizeof(opfdir), "%s%c%s", newdir, separator, "OEBPS");
        errno = 0;
        if (mt_mkdir(opfdir) != 0 && errno != EEXIST) {
            int errsv = errno;
            printf("Creating OEBPS directory failed (%s)\n", strerror(errsv));
            return ERROR;
        }
        /* output everything else to OEBPS dir */
        strcpy(newdir, opfdir);
    }
    char partname[FILENAME_MAX];
    if (rawml->markup != NULL) {
        /* Linked list of MOBIPart structures in rawml->markup holds main text files */
        MOBIPart *curr = rawml->markup;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(partname, sizeof(partname), "%s%cpart%05zu.%s", newdir, separator, curr->uid, file_meta.extension);
            errno = 0;
            FILE *file = fopen(partname, "wb");
            if (file == NULL) {
                int errsv = errno;
                printf("Could not open file for writing: %s (%s)\n", partname, strerror(errsv));
                return ERROR;
            }
            printf("part%05zu.%s\n", curr->uid, file_meta.extension);
            errno = 0;
            fwrite(curr->data, 1, curr->size, file);
            if (ferror(file)) {
                int errsv = errno;
                printf("Error writing: %s (%s)\n", partname, strerror(errsv));
                fclose(file);
                return ERROR;
            }
            fclose(file);
            curr = curr->next;
        }
    }
    if (rawml->flow != NULL) {
        /* Linked list of MOBIPart structures in rawml->flow holds supplementary text files */
        MOBIPart *curr = rawml->flow;
        /* skip raw html file */
        curr = curr->next;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(partname, sizeof(partname), "%s%cflow%05zu.%s", newdir, separator, curr->uid, file_meta.extension);
            errno = 0;
            FILE *file = fopen(partname, "wb");
            if (file == NULL) {
                int errsv = errno;
                printf("Could not open file for writing: %s (%s)\n", partname, strerror(errsv));
                return ERROR;
            }
            printf("flow%05zu.%s\n", curr->uid, file_meta.extension);
            errno = 0;
            fwrite(curr->data, 1, curr->size, file);
            if (ferror(file)) {
                int errsv = errno;
                printf("Error writing: %s (%s)\n", partname, strerror(errsv));
                fclose(file);
                return ERROR;
            }
            fclose(file);
            curr = curr->next;
        }
    }
    if (rawml->resources != NULL) {
        /* Linked list of MOBIPart structures in rawml->resources holds binary files, also opf files */
        MOBIPart *curr = rawml->resources;
        /* jpg, gif, png, bmp, font, audio, video also opf, ncx */
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            if (curr->size > 0) {
                if (create_epub_opt && file_meta.type == T_OPF) {
                    snprintf(partname, sizeof(partname), "%s%ccontent.opf", newdir, separator);
                } else {
                    snprintf(partname, sizeof(partname), "%s%cresource%05zu.%s", newdir, separator, curr->uid, file_meta.extension);
                }
                errno = 0;
                FILE *file = fopen(partname, "wb");
                if (file == NULL) {
                    int errsv = errno;
                    printf("Could not open file for writing: %s (%s)\n", partname, strerror(errsv));
                    return ERROR;
                }
                if (create_epub_opt && file_meta.type == T_OPF) {
                    printf("content.opf\n");
                } else {
                    printf("resource%05zu.%s\n", curr->uid, file_meta.extension);
                }
                errno = 0;
                fwrite(curr->data, 1, curr->size, file);
                if (ferror(file)) {
                    int errsv = errno;
                    printf("Error writing: %s (%s)\n", partname, strerror(errsv));
                    fclose(file);
                    return ERROR;
                }
                fclose(file);
            }
            curr = curr->next;
        }
    }
    return SUCCESS;
}

#ifdef USE_XMLWRITER
/**
 @brief Bundle recreated source files into EPUB container
 
 This function is a simple example.
 In real world implementation one should validate and correct all input
 markup to check if it conforms to OPF and HTML specifications and
 correct all the issues.
 
 @param[in] rawml MOBIRawml structure holding parsed records
 @param[in] fullpath File path will be parsed to build basenames of dumped records
 */
int create_epub(const MOBIRawml *rawml, const char *fullpath) {
    if (rawml == NULL) {
        printf("Rawml structure not initialized\n");
        return ERROR;
    }
    char dirname[FILENAME_MAX];
    char basename[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename);
    char zipfile[FILENAME_MAX];
    if (outdir_opt) {
        snprintf(zipfile, sizeof(zipfile), "%s%s.epub", outdir, basename);
    } else {
        snprintf(zipfile, sizeof(zipfile), "%s%s.epub", dirname, basename);
    }
    printf("Saving EPUB to %s\n", zipfile);
    /* create zip (epub) archive */
    mz_zip_archive zip;
    memset(&zip, 0, sizeof(mz_zip_archive));
    mz_bool mz_ret = mz_zip_writer_init_file(&zip, zipfile, 0);
    if (!mz_ret) {
        printf("Could not initialize zip archive\n");
        return ERROR;
    }
    /* start adding files to archive */
    mz_ret = mz_zip_writer_add_mem(&zip, "mimetype", EPUB_MIMETYPE, sizeof(EPUB_MIMETYPE) - 1, MZ_NO_COMPRESSION);
    if (!mz_ret) {
        printf("Could not add mimetype\n");
        mz_zip_writer_end(&zip);
        return ERROR;
    }
    mz_ret = mz_zip_writer_add_mem(&zip, "META-INF/container.xml", EPUB_CONTAINER, sizeof(EPUB_CONTAINER) - 1, (mz_uint)MZ_DEFAULT_COMPRESSION);
    if (!mz_ret) {
        printf("Could not add container.xml\n");
        mz_zip_writer_end(&zip);
        return ERROR;
    }
    char partname[FILENAME_MAX];
    if (rawml->markup != NULL) {
        /* Linked list of MOBIPart structures in rawml->markup holds main text files */
        MOBIPart *curr = rawml->markup;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(partname, sizeof(partname), "OEBPS/part%05zu.%s", curr->uid, file_meta.extension);
            mz_ret = mz_zip_writer_add_mem(&zip, partname, curr->data, curr->size, (mz_uint) MZ_DEFAULT_COMPRESSION);
            if (!mz_ret) {
                printf("Could not add file to archive: %s\n", partname);
                mz_zip_writer_end(&zip);
                return ERROR;
            }
            curr = curr->next;
        }
    }
    if (rawml->flow != NULL) {
        /* Linked list of MOBIPart structures in rawml->flow holds supplementary text files */
        MOBIPart *curr = rawml->flow;
        /* skip raw html file */
        curr = curr->next;
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            snprintf(partname, sizeof(partname), "OEBPS/flow%05zu.%s", curr->uid, file_meta.extension);
            mz_ret = mz_zip_writer_add_mem(&zip, partname, curr->data, curr->size, (mz_uint) MZ_DEFAULT_COMPRESSION);
            if (!mz_ret) {
                printf("Could not add file to archive: %s\n", partname);
                mz_zip_writer_end(&zip);
                return ERROR;
            }
            curr = curr->next;
        }
    }
    if (rawml->resources != NULL) {
        /* Linked list of MOBIPart structures in rawml->resources holds binary files, also opf files */
        MOBIPart *curr = rawml->resources;
        /* jpg, gif, png, bmp, font, audio, video, also opf, ncx */
        while (curr != NULL) {
            MOBIFileMeta file_meta = mobi_get_filemeta_by_type(curr->type);
            if (curr->size > 0) {
                if (file_meta.type == T_OPF) {
                    snprintf(partname, sizeof(partname), "OEBPS/content.opf");
                } else {
                    snprintf(partname, sizeof(partname), "OEBPS/resource%05zu.%s", curr->uid, file_meta.extension);
                }
                mz_ret = mz_zip_writer_add_mem(&zip, partname, curr->data, curr->size, (mz_uint) MZ_DEFAULT_COMPRESSION);
                if (!mz_ret) {
                    printf("Could not add file to archive: %s\n", partname);
                    mz_zip_writer_end(&zip);
                    return ERROR;
                }
            }
            curr = curr->next;
        }
    }
    /* Finalize epub archive */
    mz_ret = mz_zip_writer_finalize_archive(&zip);
    if (!mz_ret) {
        printf("Could not finalize zip archive\n");
        mz_zip_writer_end(&zip);
        return ERROR;
    }
    mz_ret = mz_zip_writer_end(&zip);
    if (!mz_ret) {
        printf("Could not finalize zip writer\n");
        return ERROR;
    }
    return SUCCESS;
}
#endif

/**
 @brief Dump SRCS record
 @param[in] m MOBIData structure
 @param[in] fullpath Full file path
 */
int dump_embedded_source(const MOBIData *m, const char *fullpath) {
    /* Try to get embedded source */
    unsigned char *data = NULL;
    size_t size = 0;
    MOBI_RET mobi_ret = mobi_get_embedded_source(&data, &size, m);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Extracting source from mobi failed (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    if (data == NULL || size == 0 ) {
        printf("Source archive not found\n");
        return SUCCESS;
    }
    char dirname[FILENAME_MAX];
    char basename[FILENAME_MAX];
    split_fullpath(fullpath, dirname, basename);
    char newdir[FILENAME_MAX];
    if (outdir_opt) {
        snprintf(newdir, sizeof(newdir), "%s%s_source", outdir, basename);
    } else {
        snprintf(newdir, sizeof(newdir), "%s%s_source", dirname, basename);
    }
    errno = 0;
    if (mt_mkdir(newdir) != 0 && errno != EEXIST) {
        int errsv = errno;
        printf("Creating directory failed (%s)\n", strerror(errsv));
        return ERROR;
    }
    char srcsname[FILENAME_MAX];
    snprintf(srcsname, sizeof(srcsname), "%s%c%s_source.zip", newdir, separator, basename);

    printf("Saving source archive to %s\n", srcsname);
    errno = 0;
    FILE *file = fopen(srcsname, "wb");
    if (file == NULL) {
        int errsv = errno;
        printf("Could not open file for writing: %s (%s)\n", srcsname, strerror(errsv));
        return ERROR;
    }
    errno = 0;
    fwrite(data, 1, size, file);
    if (ferror(file)) {
        int errsv = errno;
        printf("Error writing: %s (%s)\n", srcsname, strerror(errsv));
        fclose(file);
        return ERROR;
    }
    fclose(file);
    /* Try to get embedded conversion log */
    data = NULL;
    size = 0;
    mobi_ret = mobi_get_embedded_log(&data, &size, m);
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Extracting conversion log from mobi failed (%s)\n", libmobi_msg(mobi_ret));
        return ERROR;
    }
    if (data == NULL || size == 0 ) {
        printf("Conversion log not found\n");
        return SUCCESS;
    }
    snprintf(srcsname, sizeof(srcsname), "%s%c%s_source.txt", newdir, separator, basename);
    
    printf("Saving conversion log to %s\n", srcsname);
    errno = 0;
    file = fopen(srcsname, "wb");
    if (file == NULL) {
        int errsv = errno;
        printf("Could not open file for writing: %s (%s)\n", srcsname, strerror(errsv));
        return ERROR;
    }
    errno = 0;
    fwrite(data, 1, size, file);
    if (ferror(file)) {
        int errsv = errno;
        printf("Error writing: %s (%s)\n", srcsname, strerror(errsv));
        fclose(file);
        return ERROR;
    }
    fclose(file);
    return SUCCESS;
}

/**
 @brief Main routine that calls optional subroutines
 @param[in] fullpath Full file path
 */
int loadfilename(const char *fullpath) {
    MOBI_RET mobi_ret;
    int ret = SUCCESS;
    /* Initialize main MOBIData structure */
    MOBIData *m = mobi_init();
    if (m == NULL) {
        printf("Memory allocation failed\n");
        return ERROR;
    }
    /* By default loader will parse KF8 part of hybrid KF7/KF8 file */
    if (parse_kf7_opt) {
        /* Force it to parse KF7 part */
        mobi_parse_kf7(m);
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
    mobi_ret = mobi_load_file(m, file);
    fclose(file);
    
    /* Try to print basic metadata, even if further loading failed */
    /* In case of some unsupported formats it may still print some useful info */
    if (print_extended_meta_opt) { print_meta(m); }
    
    if (mobi_ret != MOBI_SUCCESS) {
        printf("Error while loading document (%s)\n", libmobi_msg(mobi_ret));
        mobi_free(m);
        return ERROR;
    }
    
    if (!print_extended_meta_opt) {
        print_summary(m);
    }
    
    if (print_extended_meta_opt) {
        /* Try to print EXTH metadata */
        print_exth(m);
    }
    
#ifdef USE_ENCRYPTION
    if (setpid_opt || setserial_opt) {
        ret = set_decryption_key(m, serial, pid);
        if (ret != SUCCESS) {
            mobi_free(m);
            return ret;
        };
    }
#endif
    if (print_rec_meta_opt) {
        printf("\nPrinting records metadata...\n");
        print_records_meta(m);
    }
    if (dump_rec_opt) {
        printf("\nDumping raw records...\n");
        ret = dump_records(m, fullpath);
    }
    if (dump_rawml_opt) {
        printf("\nDumping rawml...\n");
        ret = dump_rawml(m, fullpath);
    } else if (dump_parts_opt || create_epub_opt) {
        printf("\nReconstructing source resources...\n");
        /* Initialize MOBIRawml structure */
        /* This structure will be filled with parsed records data */
        MOBIRawml *rawml = mobi_init_rawml(m);
        if (rawml == NULL) {
            printf("Memory allocation failed\n");
            mobi_free(m);
            return ERROR;
        }

        /* Parse rawml text and other data held in MOBIData structure into MOBIRawml structure */
        mobi_ret = mobi_parse_rawml(rawml, m);
        if (mobi_ret != MOBI_SUCCESS) {
            printf("Parsing rawml failed (%s)\n", libmobi_msg(mobi_ret));
            mobi_free(m);
            mobi_free_rawml(rawml);
            return ERROR;
        }
        if (create_epub_opt && !dump_parts_opt) {
#ifdef USE_XMLWRITER
            printf("\nCreating EPUB...\n");
            /* Create epub file */
            ret = create_epub(rawml, fullpath);
            if (ret != SUCCESS) {
                printf("Creating EPUB failed\n");
            }
#endif
        } else {
            printf("\nDumping resources...\n");
            /* Save parts to files */
            ret = dump_rawml_parts(rawml, fullpath);
            if (ret != SUCCESS) {
                printf("Dumping parts failed\n");
            }
        }
        /* Free MOBIRawml structure */
        mobi_free_rawml(rawml);
    }
    if (extract_source_opt) {
        ret = dump_embedded_source(m, fullpath);
    }
    /* Free MOBIData structure */
    mobi_free(m);
    return ret;
}

/**
 @brief Print usage info
 @param[in] progname Executed program name
 */
void exit_with_usage(const char *progname) {
    printf("usage: %s [-d" PRINT_EPUB_ARG "imrs" PRINT_RUSAGE_ARG "vx7] [-o dir]" PRINT_ENC_USG " filename\n", progname);
    printf("       without arguments prints document metadata and exits\n");
    printf("       -d        dump rawml text record\n");
#ifdef USE_XMLWRITER
    printf("       -e        create EPUB file (with -s will dump EPUB source)\n");
#endif
    printf("       -i        print detailed metadata\n");
    printf("       -m        print records metadata\n");
    printf("       -o dir    save output to dir folder\n");
#ifdef USE_ENCRYPTION
    printf("       -p pid    set pid for decryption\n");
    printf("       -P serial set device serial for decryption\n");
#endif
    printf("       -r        dump raw records\n");
    printf("       -s        dump recreated source files\n");
#ifdef HAVE_SYS_RESOURCE_H
    printf("       -u        show rusage\n");
#endif
    printf("       -v        show version and exit\n");
    printf("       -x        extract conversion source and log (if present)\n");
    printf("       -7        parse KF7 part of hybrid file (by default KF8 part is parsed)\n");
    exit(SUCCESS);
}

/**
 @brief Main
 */
int main(int argc, char *argv[]) {
    if (argc < 2) {
        exit_with_usage(argv[0]);
    }
    opterr = 0;
    int c;
    while((c = getopt(argc, argv, "d" PRINT_EPUB_ARG "imo:" PRINT_ENC_ARG "rs" PRINT_RUSAGE_ARG "vx7")) != -1)
        switch(c) {
            case 'd':
                dump_rawml_opt = 1;
                break;
#ifdef USE_XMLWRITER
            case 'e':
                create_epub_opt = 1;
                break;
#endif
            case 'i':
                print_extended_meta_opt = 1;
                break;
            case 'm':
                print_rec_meta_opt = 1;
                break;
            case 'o':
                outdir_opt = 1;
                size_t outdir_length = strlen(optarg);
                if (outdir_length >= FILENAME_MAX - 1) {
                    printf("Output directory name too long\n");
                    return ERROR;
                }
                strncpy(outdir, optarg, FILENAME_MAX - 1);
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
                break;
#ifdef USE_ENCRYPTION
            case 'p':
                setpid_opt = 1;
                pid = optarg;
                break;
            case 'P':
                setserial_opt = 1;
                serial = optarg;
                break;
#endif
            case 'r':
                dump_rec_opt = 1;
                break;
            case 's':
                dump_parts_opt = 1;
                break;
#ifdef HAVE_SYS_RESOURCE_H
            case 'u':
                print_rusage_opt = 1;
                break;
#endif
            case 'v':
                printf("mobitool build: " __DATE__ " " __TIME__ " (" COMPILER ")\n");
                printf("libmobi: %s\n", mobi_version());
                return 0;
            case 'x':
                extract_source_opt = 1;
                break;
            case '7':
                parse_kf7_opt = 1;
                break;
            case '?':
#ifdef USE_ENCRYPTION
                if (optopt == 'p') {
                    fprintf(stderr, "Option -%c requires an argument.\n", optopt);
                }
                else
#endif
                if (isprint(optopt)) {
                    fprintf(stderr, "Unknown option `-%c'\n", optopt);
                }
                else {
                    fprintf(stderr, "Unknown option character `\\x%x'\n", optopt);
                }
                exit_with_usage(argv[0]);
            default:
                exit_with_usage(argv[0]);
        }
    if (argc <= optind) {
        printf("Missing filename\n");
        exit_with_usage(argv[0]);
    }
    int ret = 0;
    char filename[FILENAME_MAX];
    strncpy(filename, argv[optind], FILENAME_MAX - 1);
    filename[FILENAME_MAX - 1] = '\0';
    ret = loadfilename(filename);
#ifdef HAVE_SYS_RESOURCE_H
    if (print_rusage_opt) {
        /* rusage */
        struct rusage ru;
        struct timeval utime;
        struct timeval stime;
        getrusage(RUSAGE_SELF, &ru);
        utime = ru.ru_utime;
        stime = ru.ru_stime;
        printf("RUSAGE: ru_utime => %lld.%lld sec.; ru_stime => %lld.%lld sec.\n",
               (long long) utime.tv_sec, (long long) utime.tv_usec,
               (long long) stime.tv_sec, (long long) stime.tv_usec);
    }
#endif
    return ret;
}

JNIEXPORT int JNICALL
Java_com_foobnix_libmobi_LibMobi_convertToEpub(JNIEnv *env, jclass clazz, jstring jinput, jstring joutpub) {
	jboolean iscopy;

	char *fullpath;
	char *outpub;

	fullpath = (char*) (*env)->GetStringUTFChars(env, jinput, &iscopy);
	outdir = (char*) (*env)->GetStringUTFChars(env, joutpub, &iscopy);

    outdir_opt = 1;


	MOBI_RET mobi_ret;
	int ret = SUCCESS;

	MOBIData *m = mobi_init();
	if (m == NULL) {
		printf("Memory allocation failed\n");
		return ERROR;
	}

	FILE *file = fopen(fullpath, "rb");
	if (file == NULL) {
		printf("Error opening file: %s\n", fullpath);
		mobi_free(m);
		return ERROR;
	}

	mobi_ret = mobi_load_file(m, file);
	fclose(file);

    print_meta(m);

	 if (mobi_ret != MOBI_SUCCESS) {
		printf("Error while loading document (%s)\n", libmobi_msg(mobi_ret));
		mobi_free(m);
		return ERROR;
	}

	 MOBIRawml *rawml = mobi_init_rawml(m);
	 if (rawml == NULL) {
		 printf("Memory allocation failed\n");
		 mobi_free(m);
		 return ERROR;
	 }

	 mobi_ret = mobi_parse_rawml(rawml, m);
	 if (mobi_ret != MOBI_SUCCESS) {
		 printf("Parsing rawml failed (%s)\n", libmobi_msg(mobi_ret));
		 mobi_free(m);
		 mobi_free_rawml(rawml);
		 return ERROR;
	 }

   

	 ret = create_epub(rawml, outdir);

	 if (ret != SUCCESS) {
		 printf("Creating EPUB failed\n");
	 }

    mobi_free_rawml(rawml);
    mobi_free(m);
    return ret;
}
