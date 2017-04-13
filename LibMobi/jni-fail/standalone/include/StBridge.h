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

#ifndef __ST_BRIDGE_H__
#define __ST_BRIDGE_H__

#include "StProtocol.h"

class StBridge
{
protected:
    const char* lctx;

public:
    StBridge(const char* lctx) { this->lctx = lctx; };
    virtual ~StBridge() {};

public:
    int main(int argc, char *argv[]);

    virtual void process(CmdRequest& request, CmdResponse& response)=0;

protected:

    void renice();
};

#endif
