/** @file meta.c
 *  @brief Functions for metadata manipulation
 *
 * Copyright (c) 2016 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#define _GNU_SOURCE 1
#ifndef __USE_BSD
#define __USE_BSD /* for strdup on linux/glibc */
#endif

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "meta.h"
#include "util.h"

/**
 @brief Get document metadata from exth string
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @param[in] exth_tag MOBIExthTag
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_exthstring(const MOBIData *m, const MOBIExthTag exth_tag) {
    char *string = NULL;
    
    MOBIExthHeader *exth;
    MOBIExthHeader *start = NULL;
    while ((exth = mobi_next_exthrecord_by_tag(m, exth_tag, &start))) {
        char *exth_string = mobi_decode_exthstring(m, exth->data, exth->size);
        if (string == NULL) {
            string = exth_string;
        } else if (exth_string) {
            const char *separator = "; ";
            size_t new_length = strlen(string) + strlen(exth_string) + strlen(separator) + 1;
            char *new = malloc(new_length);
            if (new == NULL) {
                free(string);
                free(exth_string);
                return NULL;
            }
            strcpy(new, string);
            strcat(new, separator);
            strcat(new, exth_string);
            free(string);
            free(exth_string);
            string = new;
        }
        if (start == NULL) {
            break;
        }
    }
    return string;
}

/**
 @brief Get document title metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_title(const MOBIData *m) {
    if (m == NULL) {
        return NULL;
    }
    char *title = mobi_meta_get_exthstring(m, EXTH_UPDATEDTITLE);
    if (title) {
        return title;
    }
    char fullname[MOBI_TITLE_SIZEMAX + 1];
    MOBI_RET ret = mobi_get_fullname(m, fullname, MOBI_TITLE_SIZEMAX);
    if (ret == MOBI_SUCCESS) {
        title = strdup(fullname);
    } else if (m->ph) {
        title = strdup(m->ph->name);
    }
    return title;
}

/**
 @brief Add document title metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] title String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_title(MOBIData *m, const char *title) {
    if (title == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(title), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_UPDATEDTITLE, (uint32_t) size, title);
}

/**
 @brief Delete all title metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_title(MOBIData *m) {
    if (mobi_exists_mobiheader(m) && m->mh->full_name) {
        m->mh->full_name[0] = '\0';
    }
    if (mobi_is_hybrid(m) && mobi_exists_mobiheader(m->next) && m->next->mh->full_name) {
        m->next->mh->full_name[0] = '\0';
    }
    return mobi_delete_exthrecord_by_tag(m, EXTH_UPDATEDTITLE);
}

/**
 @brief Set document title metadata
 
 Replaces all title metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] title String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_title(MOBIData *m, const char *title) {
    if (title == NULL) {
        return MOBI_PARAM_ERR;
    }
    /* set title in mobi header */
    MOBI_RET ret = mobi_set_fullname(m, title);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    /* set title in palm header */
    ret = mobi_set_pdbname(m, title);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
    /* set title in exth header */
    ret = mobi_delete_exthrecord_by_tag(m, EXTH_UPDATEDTITLE);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_title(m, title);
    }
    return ret;
}

/**
 @brief Get document author metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_author(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_AUTHOR);
}

/**
 @brief Add document author metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] author String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_author(MOBIData *m, const char *author) {
    if (author == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(author), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_AUTHOR, (uint32_t) size, author);
}

/**
 @brief Delete all author metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_author(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_AUTHOR);
}

/**
 @brief Set document author metadata
 
 Replaces all author metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] author String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_author(MOBIData *m, const char *author) {
    if (author == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_author(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_author(m, author);
    }
    return ret;
}

/**
 @brief Get document subject metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_subject(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_SUBJECT);
}

/**
 @brief Add document subject metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] subject String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_subject(MOBIData *m, const char *subject) {
    if (subject == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(subject), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_SUBJECT, (uint32_t) size, subject);
}

/**
 @brief Delete all subject metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_subject(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_SUBJECT);
}

/**
 @brief Set document subject metadata
 
 Replaces all subject metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] subject String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_subject(MOBIData *m, const char *subject) {
    if (subject == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_subject(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_subject(m, subject);
    }
    return ret;
}

/**
 @brief Get document publisher metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_publisher(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_PUBLISHER);
}

/**
 @brief Add document publisher metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] publisher String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_publisher(MOBIData *m, const char *publisher) {
    if (publisher == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(publisher), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_PUBLISHER, (uint32_t) size, publisher);
}

/**
 @brief Delete all publisher metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_publisher(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_PUBLISHER);
}

/**
 @brief Set document publisher metadata
 
 Replaces all publisher metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] publisher String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_publisher(MOBIData *m, const char *publisher) {
    if (publisher == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_publisher(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_publisher(m, publisher);
    }
    return ret;
}

/**
 @brief Get document publishing date metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_publishdate(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_PUBLISHINGDATE);
}

/**
 @brief Add document publishdate metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] publishdate String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_publishdate(MOBIData *m, const char *publishdate) {
    if (publishdate == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(publishdate), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_PUBLISHINGDATE, (uint32_t) size, publishdate);
}

/**
 @brief Delete all publishdate metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_publishdate(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_PUBLISHINGDATE);
}

/**
 @brief Set document publishdate metadata
 
 Replaces all publishdate metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] publishdate String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_publishdate(MOBIData *m, const char *publishdate) {
    if (publishdate == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_publishdate(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_publishdate(m, publishdate);
    }
    return ret;
}

/**
 @brief Get document description metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_description(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_DESCRIPTION);
}

/**
 @brief Add document description metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] description String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_description(MOBIData *m, const char *description) {
    if (description == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(description), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_DESCRIPTION, (uint32_t) size, description);
}

/**
 @brief Delete all description metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_description(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_DESCRIPTION);
}

/**
 @brief Set document description metadata
 
 Replaces all description metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] description String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_description(MOBIData *m, const char *description) {
    if (description == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_description(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_description(m, description);
    }
    return ret;
}

/**
 @brief Get document imprint metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_imprint(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_IMPRINT);
}

/**
 @brief Add document imprint metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] imprint String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_imprint(MOBIData *m, const char *imprint) {
    if (imprint == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(imprint), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_IMPRINT, (uint32_t) size, imprint);
}

/**
 @brief Delete all imprint metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_imprint(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_IMPRINT);
}

/**
 @brief Set document imprint metadata
 
 Replaces all imprint metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] imprint String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_imprint(MOBIData *m, const char *imprint) {
    if (imprint == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_imprint(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_imprint(m, imprint);
    }
    return ret;
}

/**
 @brief Get document contributor metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_contributor(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_CONTRIBUTOR);
}

/**
 @brief Add document contributor metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] contributor String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_contributor(MOBIData *m, const char *contributor) {
    if (contributor == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(contributor), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_CONTRIBUTOR, (uint32_t) size, contributor);
}

/**
 @brief Delete all contributor metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_contributor(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_CONTRIBUTOR);
}

/**
 @brief Set document contributor metadata
 
 Replaces all contributor metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] contributor String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_contributor(MOBIData *m, const char *contributor) {
    if (contributor == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_contributor(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_contributor(m, contributor);
    }
    return ret;
}

/**
 @brief Get document review metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_review(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_REVIEW);
}

/**
 @brief Add document review metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] review String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_review(MOBIData *m, const char *review) {
    if (review == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(review), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_REVIEW, (uint32_t) size, review);
}

/**
 @brief Delete all review metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_review(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_REVIEW);
}

/**
 @brief Set document review metadata
 
 Replaces all review metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] review String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_review(MOBIData *m, const char *review) {
    if (review == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_review(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_review(m, review);
    }
    return ret;
}

/**
 @brief Get document copyright metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_copyright(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_RIGHTS);
}

/**
 @brief Add document copyright metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] copyright String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_copyright(MOBIData *m, const char *copyright) {
    if (copyright == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(copyright), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_RIGHTS, (uint32_t) size, copyright);
}

/**
 @brief Delete all copyright metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_copyright(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_RIGHTS);
}

/**
 @brief Set document copyright metadata
 
 Replaces all copyright metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] copyright String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_copyright(MOBIData *m, const char *copyright) {
    if (copyright == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_copyright(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_copyright(m, copyright);
    }
    return ret;
}

/**
 @brief Get document ISBN metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_isbn(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_ISBN);
}

/**
 @brief Add document isbn metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] isbn String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_isbn(MOBIData *m, const char *isbn) {
    if (isbn == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(isbn), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_ISBN, (uint32_t) size, isbn);
}

/**
 @brief Delete all isbn metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_isbn(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_ISBN);
}

/**
 @brief Set document isbn metadata
 
 Replaces all isbn metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] isbn String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_isbn(MOBIData *m, const char *isbn) {
    if (isbn == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_isbn(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_isbn(m, isbn);
    }
    return ret;
}

/**
 @brief Get document ASIN metadata
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_asin(const MOBIData *m) {
    return mobi_meta_get_exthstring(m, EXTH_ASIN);
}

/**
 @brief Add document asin metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] asin String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_asin(MOBIData *m, const char *asin) {
    if (asin == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(asin), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_ASIN, (uint32_t) size, asin);
}

/**
 @brief Delete all asin metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_asin(MOBIData *m) {
    return mobi_delete_exthrecord_by_tag(m, EXTH_ASIN);
}

/**
 @brief Set document asin metadata
 
 Replaces all asin metadata with new string
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] asin String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_asin(MOBIData *m, const char *asin) {
    if (asin == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_asin(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_asin(m, asin);
    }
    return ret;
}

/**
 @brief Get document language code metadata
 
 Locale strings are based on IANA language-subtag registry with some custom Mobipocket modifications.
 See mobi_locale array.
 
 Returned string must be deallocated by caller
 
 @param[in] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
char * mobi_meta_get_language(const MOBIData *m) {
    if (m == NULL) {
        return NULL;
    }
    char *lang = mobi_meta_get_exthstring(m, EXTH_LANGUAGE);
    if(lang == NULL && m->mh && m->mh->locale && *m->mh->locale) {
        const char *locale_string = mobi_get_locale_string(*m->mh->locale);
        if (locale_string) {
            lang = strdup(locale_string);
        }
    }
    return lang;
}

/**
 @brief Add document language code metadata
 
 Locale strings are based on IANA language-subtag registry with some custom Mobipocket modifications.
 See mobi_locale array.
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] language String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_add_language(MOBIData *m, const char *language) {
    if (language == NULL) {
        return MOBI_PARAM_ERR;
    }
    size_t size = min(strlen(language), UINT32_MAX);
    return mobi_add_exthrecord(m, EXTH_LANGUAGE, (uint32_t) size, language);
}

/**
 @brief Delete all language code metadata
 
 @param[in,out] m MOBIData structure with loaded data
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_delete_language(MOBIData *m) {
    if(mobi_exists_mobiheader(m) && m->mh->locale) {
        *m->mh->locale = 0;
    }
    if(mobi_is_hybrid(m) && mobi_exists_mobiheader(m->next) && m->next->mh->locale) {
        *m->next->mh->locale = 0;
    }
    return mobi_delete_exthrecord_by_tag(m, EXTH_LANGUAGE);
}

/**
 @brief Set document language code metadata
 
 Replaces all language metadata with new string
 Locale strings are based on IANA language-subtag registry with some custom Mobipocket modifications.
 See mobi_locale array.
 
 @param[in,out] m MOBIData structure with loaded data
 @param[in] language String value
 @return Pointer to null terminated string, NULL on failure
 */
MOBI_RET mobi_meta_set_language(MOBIData *m, const char *language) {
    if (language == NULL) {
        return MOBI_PARAM_ERR;
    }
    MOBI_RET ret = mobi_meta_delete_language(m);
    if (ret == MOBI_SUCCESS) {
        ret = mobi_meta_add_language(m, language);
    }
    if(mobi_exists_mobiheader(m) && m->mh->locale) {
        *m->mh->locale = (uint32_t) mobi_get_locale_number(language);
    }
    if(mobi_is_hybrid(m) && mobi_exists_mobiheader(m->next) && m->next->mh->locale) {
        *m->next->mh->locale = (uint32_t) mobi_get_locale_number(language);
    }
    return ret;
}
