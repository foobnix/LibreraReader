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

#ifndef __ST_VECTOR_HELPERS_H__
#define __ST_VECTOR_HELPERS_H__

#include <vector>
#include <algorithm>

template<class T> T* null()
{
    return NULL;
}

template<class T> void release(std::vector<T*>& v)
{
    for (typename std::vector<T*>::iterator i = v.begin(); i != v.end(); ++i)
    {
        if (*i != NULL)
        {
            delete *i;
        }
        *i = NULL;
    }
}

template<class T> void empty(std::vector<T*>& v)
{
    release(v);
    v.empty();
}

template<class T> void init(std::vector<T*>& v, int size)
{
    release(v);
    v.resize(size);
    std::generate(v.begin(), v.end(), null<T>);
}

#endif
