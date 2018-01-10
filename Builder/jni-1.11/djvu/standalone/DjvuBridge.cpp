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
#include "StSocket.h"

#include "DjvuBridge.h"

#define LCTX "EBookDroid.DJVU.Decoder"
#define L_DEBUG false

#define RES_DJVU_FAIL       255


DjvuBridge::DjvuBridge() : StBridge("EBookDroid.DJVU")
{
    context = NULL;
    doc = NULL;
    pageCount = 0;
    pages = NULL;
    info = NULL;
    outline = NULL;
}

DjvuBridge::~DjvuBridge()
{
    if (outline != NULL)
    {
        delete outline;
        outline = NULL;
    }

    if (pages != NULL)
    {
        int i;
        for (i = 0; i < pageCount; i++)
        {
            if (pages[i] != NULL)
            {
                ddjvu_page_release(pages[i]);
            }
        }
        free(pages);
        pages = NULL;
    }
    if (info != NULL)
    {
        int i;
        for (i = 0; i < pageCount; i++)
        {
            if (info[i] != NULL)
            {
                delete info[i];
            }
        }
        free(info);
        info = NULL;
    }

    if (doc)
    {
        ddjvu_document_release(doc);
        doc = NULL;
    }
    if (context)
    {
        ddjvu_context_release(context);
        context = NULL;
    }
}

void DjvuBridge::process(CmdRequest& request, CmdResponse& response)
{
    response.reset();

    request.print(LCTX);

    switch (request.cmd)
    {
    case CMD_REQ_OPEN:
        processOpen(request, response);
        break;
    case CMD_REQ_PAGE_INFO:
        processPageInfo(request, response);
        break;
    case CMD_REQ_PAGE:
        processPage(request, response);
        break;
    case CMD_REQ_PAGE_RENDER:
        processPageRender(request, response);
        break;
    case CMD_REQ_PAGE_FREE:
        processPageFree(request, response);
        break;
    case CMD_REQ_PAGE_TEXT:
        processPageText(request, response);
        break;
    case CMD_REQ_OUTLINE:
        processOutline(request, response);
        break;
    case CMD_REQ_QUIT:
        processQuit(request, response);
        break;
    default:
        ERROR_L(LCTX, "Unknown request: %d", request.cmd);
        response.result = RES_UNKNOWN_CMD;
        break;
    }

    response.print(LCTX);
}

void DjvuBridge::processQuit(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_QUIT;
}

void DjvuBridge::processOpen(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_OPEN;
    if (request.dataCount == 0)
    {
        ERROR_L(LCTX, "No request data found");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    uint8_t* socketName = NULL;
    uint32_t pageNo = 0;

    CmdDataIterator iter(request.first);
    iter.required(&socketName).integer(&pageNo);

    if ((!iter.isValid()) || (!socketName))
    {
        ERROR_L(LCTX, "Bad request data: %u %u %u %08X %p %u", request.cmd, request.dataCount, iter.getCount(),
            iter.getErrors(), socketName, pageNo);
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    DEBUG_L(L_DEBUG, LCTX, "Socket name: %s", socketName);
    StSocketConnection connection((const char*)socketName);
    if (!connection.isValid())
    {
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    DEBUG_L(L_DEBUG, LCTX, "client: connected!");
    int fd;
    bool received = connection.receiveFileDescriptor(fd);

    DEBUG_L(L_DEBUG, LCTX, "File descriptor:  %d", fd);
    if (!received)
    {
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    if (doc == NULL)
    {
        context = ddjvu_context_create(LCTX);
        char url[1024];
        sprintf(url, "fd:%d", fd);
        DEBUG_L(L_DEBUG, LCTX, "Opening url: %s", url);
        doc = ddjvu_document_create_by_filename(context, url, FALSE);
        if (!doc)
        {
            ERROR_L(LCTX, "DJVU file not found or corrupted.");
            response.result = RES_NO_FILE;
            return;
        }

        ddjvu_fileinfo_t finfo;
        ddjvu_status_t status;
        while ((status = ddjvu_document_get_fileinfo(doc, 0, &finfo)) < DDJVU_JOB_OK)
        {
            waitAndHandleMessages();
        }

        if (status != DDJVU_JOB_OK)
        {
            ERROR_L(LCTX, "DJVU file corrupted: %d", status);
            response.result = RES_DJVU_FAIL;
            return;
        }

        pageCount = ddjvu_document_get_pagenum(doc);

        info = (ddjvu_pageinfo_t**) calloc(pageCount, sizeof(ddjvu_pageinfo_t*));
        pages = (ddjvu_page_t**) calloc(pageCount, sizeof(ddjvu_page_t*));

        outline = new DjvuOutline(doc);
    }

    response.addValue(pageCount);

    if (pageCount > 0)
    {
        if (pageNo < pageCount)
        {
            getPageInfo(pageNo);
            getPage(pageNo, true);
        }
    }
}

void DjvuBridge::processPageInfo(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_PAGE_INFO;
    if (request.dataCount == 0)
    {
        ERROR_L(LCTX, "No request data found");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    uint32_t pageNo = 0;
    if (!CmdDataIterator(request.first).integer(&pageNo).isValid())
    {
        ERROR_L(LCTX, "Bad request data");
        response.result = RES_BAD_REQ_DATA;
        return;
    }
    if (doc == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    ddjvu_pageinfo_t* i = getPageInfo(pageNo);
    if (i == NULL)
    {
        response.result = RES_DJVU_FAIL;
        return;
    }

    float data[2];
    data[0] = i->width;
    data[1] = i->height;

    response.addFloatBuffer(2, data, true);
}

void DjvuBridge::processPage(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_PAGE;
    if (request.dataCount == 0)
    {
        ERROR_L(LCTX, "No request data found");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    uint32_t pageNumber = 0;
    CmdDataIterator iter(request.first);
    if (!iter.integer(&pageNumber).isValid())
    {
        ERROR_L(LCTX, "Bad request data");
        response.result = RES_BAD_REQ_DATA;
        return;
    }
    if (doc == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    if (pageNumber >= pageCount)
    {
        ERROR_L(LCTX, "Bad page index: %d", pageNumber);
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    ddjvu_pageinfo_t* i = getPageInfo(pageNumber);
    ddjvu_page_t* p = getPage(pageNumber, false);

    if (i == NULL || p == NULL)
    {
        response.result = RES_DJVU_FAIL;
        return;
    }

    processLinks(pageNumber, response);
}

void DjvuBridge::processPageFree(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_PAGE_FREE;
    if (request.dataCount == 0)
    {
        ERROR_L(LCTX, "No request data found");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    uint32_t pageNumber = 0;
    CmdDataIterator iter(request.first);
    if (!iter.integer(&pageNumber).isValid())
    {
        ERROR_L(LCTX, "Bad request data");
        response.result = RES_BAD_REQ_DATA;
        return;
    }
    if (doc == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    if (pageNumber >= pageCount)
    {
        ERROR_L(LCTX, "Bad page index: %d", pageNumber);
        response.result = RES_BAD_REQ_DATA;
        return;
    }
    if (pages[pageNumber])
    {
        ddjvu_page_release(pages[pageNumber]);
        pages[pageNumber] = NULL;
    }
}

void DjvuBridge::processPageRender(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_PAGE_RENDER;
    if (request.dataCount == 0)
    {
        ERROR_L(LCTX, "No request data found");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    uint32_t pageNumber, targetWidth, targetHeight;
    float pageSliceX, pageSliceY, pageSliceWidth, pageSliceHeight;
    uint32_t rendermode;

    CmdDataIterator iter(request.first);
    iter.integer(&pageNumber).integer(&targetWidth).integer(&targetHeight);
    iter.floater(&pageSliceX).floater(&pageSliceY).floater(&pageSliceWidth).floater(&pageSliceHeight);
    iter.integer(&rendermode);

    if (!iter.isValid())
    {
        ERROR_L(LCTX, "Bad request data");
        response.result = RES_BAD_REQ_DATA;
        return;
    }
    if (doc == NULL || pages == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    ddjvu_page_t* p = getPage(pageNumber, true);

    ddjvu_rect_t pageRect;
    pageRect.x = 0;
    pageRect.y = 0;
    pageRect.w = targetWidth / pageSliceWidth;
    pageRect.h = targetHeight / pageSliceHeight;
    ddjvu_rect_t targetRect;
    targetRect.x = pageSliceX * targetWidth / pageSliceWidth;
    targetRect.y = pageSliceY * targetHeight / pageSliceHeight;
    targetRect.w = targetWidth;
    targetRect.h = targetHeight;

    unsigned int masks[] = { 0x000000FF, 0x0000FF00, 0x00FF0000, 0xFF000000 };
    ddjvu_format_t* pixelFormat = ddjvu_format_create(DDJVU_FORMAT_RGBMASK32, 4, masks);

    ddjvu_format_set_row_order(pixelFormat, TRUE);
    ddjvu_format_set_y_direction(pixelFormat, TRUE);

    int size = targetRect.w * targetRect.h * 4;
    CmdData* resp = new CmdData();
    char* pixels = (char*)resp->createBuffer(size);

    int result = ddjvu_page_render(pages[pageNumber], (ddjvu_render_mode_t) rendermode, &pageRect, &targetRect,
        pixelFormat, targetWidth * 4, pixels);

    ddjvu_format_release(pixelFormat);

    if (!result)
    {
        response.result = RES_DJVU_FAIL;
        delete resp;
    }
    else
    {
        response.addData(resp);
    }
}

void DjvuBridge::processOutline(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_OUTLINE;

    if (doc == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    if (outline == NULL)
    {
        outline = new DjvuOutline(doc);
    }

    outline->toResponse(response);
}

void DjvuBridge::processPageText(CmdRequest& request, CmdResponse& response)
{
    response.cmd = CMD_RES_PAGE_TEXT;

    if (doc == NULL)
    {
        ERROR_L(LCTX, "Document not yet opened");
        response.result = RES_DUP_OPEN;
        return;
    }

    uint32_t pageNo;
    uint8_t* pattern = NULL;

    CmdDataIterator iter(request.first);
    iter.integer(&pageNo)/*.required(&pattern) */;

    if (!iter.isValid())
    {
        ERROR_L(LCTX, "Bad request data");
        response.result = RES_BAD_REQ_DATA;
        return;
    }

    processText((int) pageNo, (const char*) pattern, response);
}

ddjvu_pageinfo_t* DjvuBridge::getPageInfo(uint32_t pageNo)
{
    if (info[pageNo] == NULL)
    {
        ddjvu_pageinfo_t* i = new ddjvu_pageinfo_t();
        ddjvu_status_t r;
        while ((r = ddjvu_document_get_pageinfo(doc, pageNo, i)) < DDJVU_JOB_OK)
        {
            waitAndHandleMessages();
        }
        if (r == DDJVU_JOB_OK)
        {
            info[pageNo] = i;
        }
        else
        {
            ERROR_L(LCTX, "Cannot retrieve page info: %d %d", pageNo, r);
            delete i;
        }
    }
    return info[pageNo];
}

ddjvu_page_t* DjvuBridge::getPage(uint32_t pageNo, bool decode)
{
    if (pages[pageNo] == NULL)
    {
        pages[pageNo] = ddjvu_page_create_by_pageno(doc, pageNo);
    }

    if (decode)
    {
        ddjvu_status_t r;
        while ((r = ddjvu_page_decoding_status(pages[pageNo])) < DDJVU_JOB_OK)
        {
            waitAndHandleMessages();
        }
        if (r != DDJVU_JOB_OK)
        {
            ERROR_L(LCTX, "Cannot decode page: %d %d", pageNo, r);
        }
    }
    return pages[pageNo];
}

void DjvuBridge::waitAndHandleMessages()
{
// Wait for first message
    ddjvu_message_wait(context);
// Process available messages
    handleMessages();
}

void DjvuBridge::handleMessages()
{
    const ddjvu_message_t *msg;
    while ((msg = ddjvu_message_peek(context)))
    {
        switch (msg->m_any.tag)
        {
        case DDJVU_ERROR:
            ERROR_L(LCTX, "Djvu decoding error: %s %s %d", msg->m_error.filename, msg->m_error.function, msg->m_error.lineno);
            break;
        case DDJVU_INFO:
            break;
        case DDJVU_DOCINFO:
            break;
        default:
            break;
        }
        ddjvu_message_pop(context);
    }
}

