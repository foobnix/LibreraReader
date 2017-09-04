/** @file memory.h
 *
 * Copyright (c) 2014 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

#ifndef libmobi_memory_h
#define libmobi_memory_h

#include "config.h"
#include "index.h"
#include "compression.h"
#include "mobi.h"

MOBIData * mobi_init(void);
void mobi_free_mh(MOBIMobiHeader *mh);
void mobi_free_rec(MOBIData *m);
void mobi_free_eh(MOBIData *m);
void mobi_free(MOBIData *m);

MOBIHuffCdic * mobi_init_huffcdic(void);
void mobi_free_huffcdic(MOBIHuffCdic *huffcdic);

MOBIIndx * mobi_init_indx(void);
void mobi_free_indx(MOBIIndx *indx);
void mobi_free_tagx(MOBITagx *tagx);
void mobi_free_ordt(MOBIOrdt *ordt);
void mobi_free_index_entries(MOBIIndx *indx);

#endif
