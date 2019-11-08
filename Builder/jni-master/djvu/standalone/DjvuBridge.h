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

#ifndef __DJVU_BRIDGE_H__
#define __DJVU_BRIDGE_H__

#include <ddjvuapi.h>

#include "DjvuOutline.h"

#include "StBridge.h"

class DjvuBridge : public StBridge
{
private:
    ddjvu_context_t *context;
    ddjvu_document_t *doc;

    uint32_t pageCount;
    ddjvu_pageinfo_t **info;
    ddjvu_page_t **pages;

    DjvuOutline* outline;

public:
    DjvuBridge();
    ~DjvuBridge();

    void process(CmdRequest& request, CmdResponse& response);

protected:
    void processOpen(CmdRequest& request, CmdResponse& response);
    void processQuit(CmdRequest& request, CmdResponse& response);
    void processPageInfo(CmdRequest& request, CmdResponse& response);
    void processPage(CmdRequest& request, CmdResponse& response);
    void processPageRender(CmdRequest& request, CmdResponse& response);
    void processPageFree(CmdRequest& request, CmdResponse& response);
    void processOutline(CmdRequest& request, CmdResponse& response);
    void processPageText(CmdRequest& request, CmdResponse& response);

    ddjvu_pageinfo_t* getPageInfo(uint32_t pageNo);
    ddjvu_page_t* getPage(uint32_t pageNo, bool decode);

    void processLinks(int pageNo, CmdResponse& response);
    void processText(int pageNo, const char* pattern, CmdResponse& response);

    void waitAndHandleMessages();
    void handleMessages();
};

#endif
