/*
 * Copyright (C) 2013 The DjVU CLI viewer interface Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdlib.h>

#include "StLog.h"
#include "StProtocol.h"

#include "DjvuOutline.h"

#define LCTX "EBookDroid.DJVU.Decoder.Outline"
#define L_DEBUG_OUTLINE false

const char EMPTY_TITLE[1] = { 0x0 };

#define PAGE_LINK 1
#define URI_LINK 2

DjvuOutlineItem::DjvuOutlineItem(ddjvu_document_t* doc, int level, int index, miniexp_t& expr)
{
    this->level = level;
    this->index = index;
    this->pageNo = -1;
    this->title = NULL;

    this->firstChild = NULL;
    this->nextItem = NULL;

    DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] Outline item found", this->level, this->index);

    miniexp_t item = miniexp_car(expr);

    miniexp_t head = miniexp_car(item);
    if (miniexp_stringp(head))
    {
        const char* buf = miniexp_to_str(head);
        this->title = buf == NULL ? NULL : strdup(buf);
        DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] Title: %s", this->level, this->index, this->title);
    }
    else
    {
        DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] No title", this->level, this->index);
    }

    miniexp_t tail = miniexp_cdr(item);
    if (miniexp_consp(tail))
    {
        miniexp_t tailHead = miniexp_car(tail);
        if (miniexp_stringp(tailHead))
        {
            const char *link = miniexp_to_str(tailHead);
            if (link && link[0] == '#')
            {
                int number = ddjvu_document_search_pageno(doc, link + 1);
                this->pageNo = number >= 0 ? number : -1;
                DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] PageNo: %d", this->level, this->index, this->pageNo);
            }
            else
            {
                DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] Unknown link: %s", this->level, this->index, link);
            }
        }
        else
        {
            DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] No pageNo", this->level, this->index);
        }

        miniexp_t tailTail = miniexp_cdr(tail);
        if (miniexp_consp(tailTail))
        {
            this->firstChild = new DjvuOutlineItem(doc, level + 1, 0, tailTail);
        }
    }
    else
    {
        DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] No pageNo and children", this->level, this->index);
    }

    miniexp_t next = miniexp_cdr(expr);
    if (miniexp_consp(next))
    {
        this->nextItem = new DjvuOutlineItem(doc, level, index + 1, next);
    }
}

DjvuOutlineItem::~DjvuOutlineItem()
{
    if (title)
    {
        free(title);
        title = NULL;
    }

    if (firstChild != NULL)
    {
        delete firstChild;
        firstChild = NULL;
    }

    if (nextItem == NULL)
    {
        delete nextItem;
        nextItem = NULL;
    }
}

void DjvuOutlineItem::toResponse(CmdResponse& response)
{
    response.addValue((uint32_t) this->level);
    response.addString(this->title != NULL ? this->title : EMPTY_TITLE, false);
    response.addWords((uint16_t) PAGE_LINK, (uint16_t) this->pageNo);
    response.addFloat(.0f).addFloat(.0f);

    DEBUG_L(L_DEBUG_OUTLINE, LCTX, "[%d:%d] %d", this->level, this->index, response.dataCount);

    if (firstChild != NULL)
    {
        firstChild->toResponse(response);
    }
    if (nextItem != NULL)
    {
        nextItem->toResponse(response);
    }
}

DjvuOutline::DjvuOutline(ddjvu_document_t* doc)
{
    this->firstPageIndex = 0;
    this->firstItem = NULL;

    miniexp_t outline = ddjvu_document_get_outline(doc);
    if (outline && outline != miniexp_dummy )
    {
        if (!miniexp_consp(outline) || miniexp_car(outline) != miniexp_symbol("bookmarks"))
        {
            ERROR_L(LCTX, "Outline data is corrupted");
            return;
        }
        else
        {
            DEBUG_L(L_DEBUG_OUTLINE, LCTX, "Outline found");
            this->firstItem = new DjvuOutlineItem(doc, 0, 0, outline);
        }
    }
    else
    {
        DEBUG_L(L_DEBUG_OUTLINE, LCTX, "Outline not found");
    }
}

DjvuOutline::~DjvuOutline()
{
    if (firstItem != NULL)
    {
        delete firstItem;
        firstItem = NULL;
    }
}

void DjvuOutline::toResponse(CmdResponse& response)
{
    response.addValue((uint32_t) firstPageIndex);
    if (firstItem != NULL)
    {
        firstItem->toResponse(response);
    }
}

