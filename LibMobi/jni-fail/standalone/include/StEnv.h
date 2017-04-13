/*
 * Copyright (C) 2015 The Common CLI viewer interface Project
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

#ifndef __ST_ENV_H__
#define __ST_ENV_H__

#include <jni.h>
#include <stddef.h>
#include <string.h>

class JByteArray
{
private:
    JNIEnv *jenv;
    jbyte* buf;
    jbyteArray buffer;

public:
    JByteArray(JNIEnv *jenv, jbyteArray buffer)
        : jenv(jenv), buffer(buffer)
    {
        buf = buffer ? jenv->GetByteArrayElements(buffer, NULL) : NULL;
    }

    JByteArray(JByteArray&) = delete;
    JByteArray(JByteArray&&) = default;
    JByteArray& operator=(JByteArray const&) = delete;

    ~JByteArray()
    {
        if (buffer && buf)
        {
            jenv->ReleaseByteArrayElements(buffer, buf, 0);
        }
    }

    bool isValid() const
    {
        return buf != NULL;
    }

    uint8_t* asBuffer() const
    {
        return (uint8_t*) buf;
    }

    const char* asString() const
    {
        return (const char*) buf;
    }

    void copyFrom(const char* string)
    {
        memcpy(buf, string, strlen(string));
    }

    void copyFrom(const char* string, int len)
    {
        memcpy(buf, string, len);
    }

    operator const char*() const
    {
        return asString();
    }

public:
    static bool copy(JNIEnv *jenv, jbyteArray targetBuffer, const char* sourceString, int len)
    {
        JByteArray buf(jenv, targetBuffer);
        if (buf.isValid())
        {
            memcpy(buf.asBuffer(), sourceString, len);
        }
        return false;
    }
};

class JIntArray
{
private:
    JNIEnv *jenv;
    jint* buf;
    jintArray buffer;

public:
    JIntArray(JNIEnv *jenv, jintArray buffer)
        : jenv(jenv), buffer(buffer)
    {
        buf = buffer ? jenv->GetIntArrayElements(buffer, 0) : NULL;
    }

    JIntArray(JIntArray&) = delete;
    JIntArray(JIntArray&&) = default;
    JIntArray& operator=(JIntArray const&) = delete;

    ~JIntArray()
    {
        if (buffer && buf)
        {
            jenv->ReleaseIntArrayElements(buffer, buf, 0);
        }
    }

    bool isValid() const
    {
        return buf != NULL;
    }

    jint* asBuffer() const
    {
        return buf;
    }

    jint& operator[](int index) {
        return buf[index];
    }
};


template <typename T>
class JBuffer
{
protected:
    JNIEnv *jenv;
    size_t bufSize;
    T* buf;
    jobject buffer;

public:
    JBuffer(JNIEnv *jenv, jobject buffer)
        : jenv(jenv), buffer(buffer)
    {
        bufSize = buffer ? jenv->GetDirectBufferCapacity(buffer) : 0;
        buf = buffer ? (T*)jenv->GetDirectBufferAddress(buffer) : NULL;
    }

    JBuffer(JBuffer&) = delete;
    JBuffer(JBuffer&&) = default;
    JBuffer& operator=(JBuffer const&) = delete;

    virtual ~JBuffer() = default;

    size_t size() const
    {
        return bufSize;
    }

    bool isValid() const
    {
        return buf != NULL;
    }

    T* asBuffer() const
    {
        return (T*) buf;
    }

    const char* asString() const
    {
        return (const char*) buf;
    }

    void clear(uint8_t value)
    {
        memset(buf, value, bufSize);
    }

    operator const char*() const
    {
        return asString();
    }
};

class JByteBuffer : public JBuffer<uint8_t>
{
public:
    JByteBuffer(JNIEnv *jenv, jobject buffer) : JBuffer(jenv, buffer) {}
    JByteBuffer(JByteBuffer&) = delete;
    JByteBuffer(JByteBuffer&&) = default;
    JByteBuffer& operator=(JByteBuffer const&) = delete;

    ~JByteBuffer() = default;
};

class JString
{
private:
    JNIEnv *jenv;
    const char* buf;
    jstring javaString;

public:
    JString(JNIEnv *jenv, jstring javaString)
        : jenv(jenv), javaString(javaString)
    {
        buf = javaString ? jenv->GetStringUTFChars(javaString, NULL) : NULL;
    }

    JString(JNIEnv *jenv, jstring javaString, const char* defValue)
        : jenv(jenv), javaString(javaString)
    {
        buf = javaString ? jenv->GetStringUTFChars(javaString, NULL) : defValue;
    }

    JString(JString&) = delete;
    JString(JString&&) = default;
    JString& operator=(JString const&) = delete;

    ~JString()
    {
        if (javaString && buf)
        {
            jenv->ReleaseStringUTFChars(javaString, buf);
        }
        javaString = NULL;
        buf = NULL;
    }
public:
    bool isValid() const
    {
        return buf;
    }

    const char* asString() const
    {
        return buf;
    }

    operator const char*() const
    {
        return asString();
    }
};

#endif

