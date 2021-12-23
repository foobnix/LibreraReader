/** @file structure.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef mobi_structure_h
#define mobi_structure_h

#include "config.h"
#include "mobi.h"

/**
 @brief Dynamic array of uint32_t values structure
 */
typedef struct {
    uint32_t *data; /**< Array */
    size_t maxsize; /**< Allocated size */
    size_t step; /**< Step by which array will be enlarged if out of memory */
    size_t size; /**< Current size */
} MOBIArray;

MOBIArray * array_init(const size_t len);
MOBI_RET array_insert(MOBIArray *arr, const uint32_t value);
void array_sort(MOBIArray *arr, const bool unique);
size_t array_size(MOBIArray *arr);
void array_free(MOBIArray *arr);

/**
 @brief Trie storing arrays of values for character keys
 */
typedef struct MOBITrie {
    char c; /**< Key character */
    void **values; /**< Array of values */
    size_t values_count; /**< Array size */
    struct MOBITrie *next; /**< Next node at the same level */
    struct MOBITrie *children; /**< Link to children nodes, lower level */
} MOBITrie;

MOBI_RET mobi_trie_insert_reversed(MOBITrie **root, char *string, char *value);
MOBITrie * mobi_trie_get_next(char ***values, size_t *values_count, const MOBITrie *node, const char c);
void mobi_trie_free(MOBITrie *node);

/**
 @brief Structure for links reconstruction.
 
 Linked list of Fragment structures forms whole document part
 */
typedef struct MOBIFragment {
    size_t raw_offset; /**< fragment offset in raw markup, SIZE_MAX if not present in original markup */
    unsigned char *fragment; /**< Fragment data */
    size_t size; /**< Fragment size */
    bool is_malloc; /**< Is it needed to free this fragment or is it just an alias to part data */
    struct MOBIFragment *next; /**< Link to next fragment */
} MOBIFragment;

MOBIFragment * mobi_list_add(MOBIFragment *curr, size_t raw_offset, unsigned char *fragment, const size_t size, const bool is_malloc);
MOBIFragment * mobi_list_insert(MOBIFragment *curr, size_t raw_offset, unsigned char *fragment, const size_t size, const bool is_malloc, const size_t offset);
MOBIFragment * mobi_list_del(MOBIFragment *curr);
void mobi_list_del_all(MOBIFragment *first);

#endif
