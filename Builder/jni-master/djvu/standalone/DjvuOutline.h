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

#ifndef __DJVU_OUTLINE_H__
#define __DJVU_OUTLINE_H__

#include <ddjvuapi.h>
#include <miniexp.h>

#include "StProtocol.h"

class DjvuOutlineItem
{
public:
    int level;
    int index;
    int pageNo;
    char* title;

    DjvuOutlineItem* firstChild;
    DjvuOutlineItem* nextItem;

public:
    DjvuOutlineItem(ddjvu_document_t* doc, int level, int index, miniexp_t& expr);
    ~DjvuOutlineItem();

    void toResponse(CmdResponse& response);
};

class DjvuOutline
{
public:
    int firstPageIndex;

    DjvuOutlineItem* firstItem;

public:
    DjvuOutline(ddjvu_document_t* doc);
    ~DjvuOutline();

    void toResponse(CmdResponse& response);
};

#endif
