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
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>

#include "StLog.h"
#include "StProtocol.h"
#include "StQueue.h"
#include "StBridge.h"

#define L_DEBUG false

void StBridge::renice()
{
    const char *env = getenv("ST_NICE_LEVEL");
    if (env != NULL)
    {
        int level = 0;

        if (strcmp(env, "Lower") == 0)
        {
            level = -10;
        }
        else if (strcmp(env, "Lowest") == 0)
        {
            level = -20;
        }

        if (level != 0)
        {
            errno = 0;
            nice(level);
            if (!errno)
            {
                INFO_L(lctx, "Process nice level has been successfully changed to %s", env);
            }
            else
            {
                ERROR_L(lctx, "Process nice level cannot be changed: %d", errno);
            }

            return;
        }
    }

    INFO_L(lctx, "Process nice level should not be changed");
}

int StBridge::main(int argc, char *argv[])
{
    if (argc < 3)
    {
        ERROR_L(lctx, "No command line arguments");
        return 1;
    }

    renice();

    DEBUG_L(L_DEBUG, lctx, "Output file: %s", argv[2]);

    ResponseQueue out(argv[2], O_WRONLY, lctx);

    INFO_L(lctx, "Sending ready notification...");
    out.sendReadyNotification();

    DEBUG_L(L_DEBUG, lctx, "Input  file: %s", argv[1]);
    RequestQueue in(argv[1], O_RDONLY, lctx);

    CmdRequest request;
    CmdResponse response;

    bool run = true;
    while (run)
    {
        DEBUG_L(L_DEBUG, lctx, "Waiting for request...");
        int res = in.readRequest(request);
        if (res == 0)
        {
            ERROR_L(lctx, "No data received");
            return -1;
        }

        DEBUG_L(L_DEBUG, lctx, "Processing request...");
        process(request, response);

        DEBUG_L(L_DEBUG, lctx, "Sending response...");
        out.writeResponse(response);

        run = response.cmd != CMD_RES_QUIT;

        request.reset();
        response.reset();
    }

    INFO_L(lctx, "Exit");

    return 0;
}
