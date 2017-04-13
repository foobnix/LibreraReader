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

#ifndef __ST_SOCKET_H__
#define __ST_SOCKET_H__

#include <string>

#define nullsocket (-1)

class StUnixSocketId
{
public:
    StUnixSocketId() = default;
    virtual ~StUnixSocketId() = default;

public:
    virtual const char* name() = 0;
};

class StUnixSocketUniqueName : public StUnixSocketId
{
private:
    std::string socketName;

public:
    StUnixSocketUniqueName();
    StUnixSocketUniqueName(StUnixSocketUniqueName&) = delete;
    StUnixSocketUniqueName(StUnixSocketUniqueName&&) = default;
    virtual ~StUnixSocketUniqueName() = default;

public:
    virtual const char* name() override { return socketName.c_str(); }
};

class StSocketBase
{
protected:
    int id;
    int socket_fd;

protected:
    StSocketBase(const int sfd);
    StSocketBase(StSocketBase&& other);

public:
    StSocketBase(StSocketBase const&)            = delete;
    StSocketBase& operator=(StSocketBase const&) = delete;

    virtual ~StSocketBase() { close(); }

public:
    bool isValid() { return socket_fd >= 0; }

protected:
    void cleanup();
    void close();
};

class StSocketConnection: public StSocketBase
{
public:
    StSocketConnection(const char* name);
    StSocketConnection(const int sfd)               : StSocketBase(sfd) {}
    StSocketConnection(StSocketConnection&& other)  : StSocketBase(static_cast<StSocketBase&&>(other)) {}

    StSocketConnection(StSocketConnection const&)            = delete;
    StSocketConnection& operator=(StSocketConnection const&) = delete;

    virtual ~StSocketConnection() {}

public:
    bool sendFileDescriptor(int fd);
    bool receiveFileDescriptor(int& fd);
};

class StUnixServerSocket: public StSocketBase
{
public:
    StUnixServerSocket(StUnixSocketId& name, int numberOfClients);
    StUnixServerSocket(StUnixServerSocket&& other) = default;

    StUnixServerSocket(StUnixServerSocket const&)            = delete;
    StUnixServerSocket& operator=(StUnixServerSocket const&) = delete;

    virtual ~StUnixServerSocket() {}

public:
    StSocketConnection waitForConnection();
};

#endif
