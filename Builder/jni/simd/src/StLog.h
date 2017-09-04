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

#ifndef __LOG_H__
#define __LOG_H__

#include <android/log.h>

#define DEBUG_L(DEBUG_ENABLED, LCTX, args...) \
    { if (DEBUG_ENABLED) {__android_log_print(ANDROID_LOG_DEBUG, LCTX, args); } }

#define ERROR_L(LCTX, args...) \
    __android_log_print(ANDROID_LOG_ERROR, LCTX, args)

#define WARN_L(LCTX, args...) \
    __android_log_print(ANDROID_LOG_WARNING, LCTX, args)

#define INFO_L(LCTX, args...) \
    __android_log_print(ANDROID_LOG_INFO, LCTX, args)

#endif
