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

#include "DjvuBridge.h"

#define LCTX "EBookDroid.DJVU.Decoder.Search"
#define L_DEBUG_TEXT false

void djvu_get_djvu_words(miniexp_t expr, const char* pattern, ddjvu_pageinfo_t *pi, CmdResponse& response)
{
    if (!miniexp_consp(expr))
    {
        return;
    }

    miniexp_t head = miniexp_car(expr);
    expr = miniexp_cdr(expr);
    if (!miniexp_symbolp(head))
    {
        return;
    }

    int coords[4];
    float width = pi->width;
    float height = pi->height;

    int i;
    for (i = 0; i < 4 && miniexp_consp(expr); i++)
    {
        head = miniexp_car(expr);
        expr = miniexp_cdr(expr);

        if (!miniexp_numberp(head))
        {
            return;
        }
        coords[i] = miniexp_to_int(head);
    }

    while (miniexp_consp(expr))
    {
        head = miniexp_car(expr);

        if (miniexp_stringp(head))
        {
            const char* text = miniexp_to_str(head);

            DEBUG_L(L_DEBUG_TEXT, LCTX,
                "processText: %d, %d, %d, %d: %s", coords[0], coords[1], coords[2], coords[3], text);

            float t = 1.0 - coords[1] / height;
            float b = 1.0 - coords[3] / height;
            response.addFloat(coords[0] / width);
            response.addFloat(t < b ? t : b);
            response.addFloat(coords[2] / width);
            response.addFloat(t > b ? t : b);
            response.addString(text, true);
        }
        else if (miniexp_consp(head))
        {
            djvu_get_djvu_words(head, pattern, pi, response);
        }

        expr = miniexp_cdr(expr);
    }
}

void DjvuBridge::processText(int pageNo, const char* pattern, CmdResponse& response)
{
    ddjvu_pageinfo_t *pi = getPageInfo(pageNo);
    if (pi == NULL)
    {
        DEBUG_L(L_DEBUG_TEXT, LCTX, "processText: no page info %d", pageNo);
        return;
    }

    miniexp_t r = miniexp_nil;

    while ((r = ddjvu_document_get_pagetext(doc, pageNo, "word")) == miniexp_dummy )
    {
        waitAndHandleMessages();
    }

    if (r == miniexp_nil || !miniexp_consp(r))
    {
        DEBUG_L(L_DEBUG_TEXT, LCTX, "processText: no text on page %d", pageNo);
        return;
    }

    DEBUG_L(L_DEBUG_TEXT, LCTX, "processText: text found on page %d", pageNo);

    djvu_get_djvu_words(r, pattern, pi, response);

    ddjvu_miniexp_release(doc, r);
}

