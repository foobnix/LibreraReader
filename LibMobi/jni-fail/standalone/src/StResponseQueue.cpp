/*
 * Copyright (C) 2013 The Common CLI viewer interface Project
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
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "StLog.h"
#include "StProtocol.h"
#include "StQueue.h"

#define L_DEBUG_RES false

ResponseQueue::ResponseQueue(const char* fname, int mode, const char* lctx)
    : Queue(fname, mode, lctx)
{
}

void ResponseQueue::sendReadyNotification()
{
    CmdResponse response(CMD_NOTIF_READY);
    writeResponse(response);
}

void ResponseQueue::writeResponse(CmdResponse& response)
{
    DEBUG_L(L_DEBUG_RES, lctx, "Waiting for write lock");
    pthread_mutex_lock(&writelock);

    uint8_t cmd = response.cmd | (response.first != NULL ? CMD_MASK_HAS_DATA : 0);

    DEBUG_L(L_DEBUG_RES, lctx, "Writing response cmd: %02x", cmd);
    write(fp, &(cmd), sizeof(cmd));

    DEBUG_L(L_DEBUG_RES, lctx, "Writing response result: %d", response.result);
    write(fp, &(response.result), sizeof(response.result));

    CmdData* data = response.first;
    while (data != NULL)
    {
        writeData(data);
        data = data->nextData;
    }

    DEBUG_L(L_DEBUG_RES, lctx, "Flush response");
    fdatasync(fp);

    pthread_mutex_unlock(&writelock);
}

int ResponseQueue::readResponse(CmdResponse& response)
{
    DEBUG_L(L_DEBUG_RES, lctx, "Waiting for read lock");
    pthread_mutex_lock(&readlock);

    DEBUG_L(L_DEBUG_RES, lctx, "Reading response cmd...");
    uint8_t cmd = 0;
    int res = readByte(&cmd);
    if (res == 0)
    {
        pthread_mutex_unlock(&readlock);
        return 0;
    }
    response.cmd = cmd & CMD_MASK_CMD;
    uint8_t hasData = cmd & CMD_MASK_HAS_DATA;
    DEBUG_L(L_DEBUG_RES, lctx, "Response cmd: %d, has data: %d", response.cmd, hasData);

    DEBUG_L(L_DEBUG_RES, lctx, "Reading response result...");
    res = readByte(&(response.result));
    if (res == 0)
    {
        pthread_mutex_unlock(&readlock);
        return 0;
    }
    DEBUG_L(L_DEBUG_RES, lctx, "Response result: %d", response.result);

    CmdData* data = response.first;
    while (hasData)
    {
        if (data == NULL)
        {
            data = new CmdData();
            response.addData(data);
        }

        if (readData(data, hasData) == 0)
        {
            pthread_mutex_unlock(&readlock);
            return 0;
        }

        data = data->nextData;
    }

    pthread_mutex_unlock(&readlock);
    return res;
}
