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

#include "StProtocol.h"
#include "StLog.h"

#define L_PRINT_CMD false

CmdData::CmdData()
{
    type = TYPE_NONE;
    value.value32 = 0;
    owned_external = false;
    external = NULL;
    nextData = NULL;
}

CmdData::CmdData(CmdData& data)
{
    type = data.type;
    value.value32 = data.value.value32;
    owned_external = data.owned_external;
    external = NULL;
    nextData = NULL;

    if (data.external != NULL)
    {
        if (owned_external)
        {
            external = (uint8_t*)calloc(1, value.value32);
            memcpy(external, data.external, value.value32);
        }
        else
        {
            external = data.external;
        }
    }
}

CmdData::~CmdData()
{
    freeBuffer();
    type = TYPE_NONE;
    value.value32 = 0;
    external = NULL;
    if (nextData != NULL)
    {
        delete nextData;
        nextData = NULL;
    }
}

uint8_t* CmdData::createBuffer(int n)
{
    freeBuffer();
    type = TYPE_VAR;
    value.value32 = n;
    owned_external = true;
    external = (uint8_t*) calloc(1, value.value32);
    return external;
}

void CmdData::freeBuffer()
{
    if (owned_external && external != NULL)
    {
        free(external);
    }
    type = TYPE_NONE;
    value.value32 = 0;
    external = NULL;
    owned_external = false;
}

CmdData* CmdData::setValue(uint32_t val)
{
    freeBuffer();
    type = TYPE_FIX_INT;
    value.value32 = val;
    return this;
}

CmdData* CmdData::setWords(uint16_t val0, uint16_t val1)
{
    freeBuffer();
    type = TYPE_FIX_WORDS;
    value.value16[0] = val0;
    value.value16[1] = val1;
    return this;
}

CmdData* CmdData::setFloat(float val)
{
    freeBuffer();
    type = TYPE_FIX_FLOAT;
    value.valuef = val;
    return this;
}

CmdData* CmdData::setBuffer(int n, uint8_t* ptr, bool owned)
{
    freeBuffer();
    type = TYPE_VAR;
    value.value32 = n;
    owned_external = owned;
    if (owned)
    {
        external = (uint8_t*) calloc(1, value.value32);
        memcpy(external, ptr, value.value32);
    }
    else
    {
        external = ptr;
    }
    return this;
}

CmdData* CmdData::setWordBuffer(int n, uint16_t* ptr, bool owned)
{
    freeBuffer();
    type = TYPE_VAR;
    value.value32 = n * sizeof(uint16_t);
    owned_external = owned;
    if (owned)
    {
        external = (uint8_t*) calloc(1, value.value32);
        memcpy(external, ptr, value.value32);
    }
    else
    {
        external = (uint8_t*) ptr;
    }
    return this;
}

CmdData* CmdData::setIntBuffer(int n, int* ptr, bool owned)
{
    freeBuffer();
    type = TYPE_VAR;
    value.value32 = n * sizeof(int);
    owned_external = owned;
    if (owned)
    {
        external = (uint8_t*) calloc(1, value.value32);
        memcpy(external, ptr, value.value32);
    }
    else
    {
        external = (uint8_t*) ptr;
    }
    return this;
}

CmdData* CmdData::setFloatBuffer(int n, float* ptr, bool owned)
{
    freeBuffer();
    type = TYPE_VAR;
    value.value32 = n * sizeof(float);
    owned_external = owned;
    if (owned)
    {
        external = (uint8_t*) calloc(1, value.value32);
        memcpy(external, ptr, value.value32);
    }
    else
    {
        external = (uint8_t*) ptr;
    }
    return this;
}

CmdData* CmdData::setString(const char* data, bool owned)
{
    freeBuffer();
    type = TYPE_VAR;
    owned_external = owned;
    if (data != NULL)
    {
        value.value32 = strlen(data) + 1;
        external = owned ? (uint8_t*) strdup(data) : (uint8_t*) data;
    }
    else
    {
        value.value32 = 0;
        external = NULL;
    }
    return this;
}

void CmdData::print(const char* lctx)
{
    DEBUG_L(L_PRINT_CMD, lctx, "Data: %p %u %08x %p", this, this->type, this->value.value32, this->external);
}

CmdDataList::CmdDataList()
{
    dataCount = 0;
    first = last = NULL;
}

CmdDataList& CmdDataList::addData(CmdData* data)
{
    if (data != NULL)
    {
        if (last == NULL)
        {
            first = last = data;
        }
        else
        {
            last->nextData = data;
            last = data;
        }
        dataCount++;
    }
    return *this;
}

CmdDataList& CmdDataList::addValue(uint32_t val)
{
    return addData((new CmdData())->setValue(val));
}

CmdDataList& CmdDataList::addWords(uint16_t val0, uint16_t val1)
{
    return addData((new CmdData())->setWords(val0, val1));
}

CmdDataList& CmdDataList::addFloat(float val)
{
    return addData((new CmdData())->setFloat(val));
}

CmdDataList& CmdDataList::addBuffer(int n, uint8_t* ptr, bool owned)
{
    return addData((new CmdData())->setBuffer(n, ptr, owned));
}

CmdDataList& CmdDataList::addWordBuffer(int n, uint16_t* ptr, bool owned)
{
    return addData((new CmdData())->setWordBuffer(n, ptr, owned));
}

CmdDataList& CmdDataList::addIntBuffer(int n, int* ptr, bool owned)
{
    return addData((new CmdData())->setIntBuffer(n, ptr, owned));
}
CmdDataList& CmdDataList::addFloatBuffer(int n, float* ptr, bool owned)
{
    return addData((new CmdData())->setFloatBuffer(n, ptr, owned));
}

CmdDataList& CmdDataList::addString(const char* data, bool owned)
{
    return addData((new CmdData())->setString(data, owned));
}

CmdRequest::CmdRequest()
{
    cmd = CMD_UNKNOWN;
}
CmdRequest::CmdRequest(uint8_t c)
{
    cmd = c;
}
CmdRequest::~CmdRequest()
{
    reset();
}

void CmdRequest::reset()
{
    if (first != NULL)
    {
        delete first;
    }
    cmd = CMD_UNKNOWN;
    dataCount = 0;
    first = last = NULL;
}

void CmdRequest::print(const char* lctx)
{
    DEBUG_L(L_PRINT_CMD, lctx, "Request: %u", this->cmd);
    CmdData* data;
    for (data = this->first; data != NULL; data = data->nextData)
    {
        data->print(lctx);
    }
}

CmdResponse::CmdResponse()
{
    cmd = CMD_UNKNOWN;
    result = RES_OK;
}

CmdResponse::CmdResponse(uint8_t c)
{
    cmd = c;
    result = RES_OK;
}

CmdResponse::~CmdResponse()
{
    reset();
}

void CmdResponse::reset()
{
    if (first != NULL)
    {
        delete first;
    }
    cmd = CMD_UNKNOWN;
    result = RES_OK;
    first = last = NULL;

}

void CmdResponse::print(const char* lctx)
{
    DEBUG_L(L_PRINT_CMD, lctx, "Response: %u %u", this->cmd, this->result);
    CmdData* data;
    for (data = this->first; data != NULL; data = data->nextData)
    {
        data->print(lctx);
    }
}

CmdDataIterator::CmdDataIterator(CmdData* d)
{
    this->count = 0;
    this->data = d;
    this->errors = 0;
}
CmdDataIterator::~CmdDataIterator()
{
    this->count = 0;
    this->data = NULL;
    this->errors = 0;
}

bool CmdDataIterator::hasNext()
{
    return this->data != NULL;
}

bool CmdDataIterator::isValid()
{
    return this->errors == 0;
}

bool CmdDataIterator::isValid(int index)
{
    return (this->errors & (1 << index)) != 0;
}

int CmdDataIterator::getCount()
{
    return count;
}

uint32_t CmdDataIterator::getErrors()
{
    return errors;
}

CmdDataIterator& CmdDataIterator::bytes(uint8_t* v0, uint8_t* v1, uint8_t* v2, uint8_t* v3)
{
    *v0 = *v1 = *v2 = *v3 = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_FIX_BYTES)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *v0 = this->data->value.value8[0];
        *v1 = this->data->value.value8[1];
        *v2 = this->data->value.value8[2];
        *v3 = this->data->value.value8[3];
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::words(uint16_t* v0, uint16_t* v1)
{
    *v0 = *v1 = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_FIX_WORDS)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *v0 = this->data->value.value16[0];
        *v1 = this->data->value.value16[1];
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;

}

CmdDataIterator& CmdDataIterator::integer(uint32_t* v0)
{
    *v0 = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_FIX_INT)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *v0 = this->data->value.value32;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::floater(float* v0)
{
    *v0 = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_FIX_FLOAT)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *v0 = this->data->value.valuef;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::optional(uint8_t** buffer, uint32_t* len)
{
    *buffer = NULL;
    *len = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_VAR)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *len = this->data->value.value32;
        *buffer = this->data->external;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::optional(uint16_t** buffer, uint32_t* len)
{
    *buffer = NULL;
    *len = 0;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_VAR)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *len = this->data->value.value32 / sizeof(uint16_t);
        *buffer = (uint16_t*) this->data->external;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::required(uint8_t** buffer)
{
    *buffer = NULL;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_VAR)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->value.value32 == 0 || this->data->external == NULL)
    {
        this->errors |= 1 << count;
    }
    else
    {
        *buffer = this->data->external;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::required(uint32_t** buffer, int elements)
{
    *buffer = NULL;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_VAR)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->value.value32 != elements * sizeof(uint32_t))
    {
        this->errors |= 1 << count;
    }
    else
    {
        *buffer = (uint32_t*) this->data->external;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

CmdDataIterator& CmdDataIterator::required(float** buffer, int elements)
{
    *buffer = NULL;
    if (this->data == NULL)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->type != TYPE_VAR)
    {
        this->errors |= 1 << count;
    }
    else if (this->data->value.value32 != elements * sizeof(float))
    {
        this->errors |= 1 << count;
    }
    else
    {
        *buffer = (float*) this->data->external;
    }

    count++;
    this->data = this->data != NULL ? this->data->nextData : NULL;
    return *this;
}

void CmdDataIterator::print(const char* lctx)
{
    DEBUG_L(L_PRINT_CMD, lctx, "Iterator: %p %u %08x", this->data, this->count, this->errors);
}

