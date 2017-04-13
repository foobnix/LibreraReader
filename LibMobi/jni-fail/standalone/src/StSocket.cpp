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
#include <errno.h>
#include <utility>
#include <atomic>
#include <unistd.h>

#include <sys/socket.h>
#include <sys/un.h>

#include "StLog.h"
#include "StSocket.h"

#define LCTX "EBookDroid.sockets"
#define L_DEBUG false

class StSocketAddress
{
private:
    struct sockaddr_un server_address;
    socklen_t address_length;

public:
    StSocketAddress(const char* name)
    {
        memset(&server_address, 0, sizeof(server_address));
        server_address.sun_family = AF_UNIX;
        size_t namelen = strlen(name);
        memcpy(server_address.sun_path + 1, name, namelen);
        address_length = sizeof(server_address.sun_family) + namelen + 1;
    }

public:
    bool bind(const int sfd)
    {
        return 0 <= ::bind(sfd, (const struct sockaddr *) &server_address, address_length);
    }

    bool connect(const int sfd)
    {
        return 0 <= ::connect(sfd, (const struct sockaddr *) &server_address, address_length);
    }

    bool unlink()
    {
        return 0 <= ::unlink(server_address.sun_path);
    }
};

#define SERVER_SOCKET_PATTERN "ebd.%08xd.%08xd"
static std::atomic_uint socketSeqId(0);

StUnixSocketUniqueName::StUnixSocketUniqueName()
{
    char socketName[64];
    sprintf(socketName, SERVER_SOCKET_PATTERN, getpid(), socketSeqId++);
    this->socketName = socketName;
}

static std::atomic_uint cid(0);

StSocketBase::StSocketBase(const int sfd)
    : socket_fd(sfd), id(cid++)
{
    DEBUG_L(L_DEBUG, LCTX, "base: %d %p %d", id, this, socket_fd);
}

StSocketBase::StSocketBase(StSocketBase&& other)
    : socket_fd(other.socket_fd), id(cid++)
{
    DEBUG_L(L_DEBUG, LCTX, "moved: %d %p <- %d %p: %d", id, this, other.id, &other, socket_fd);
    other.cleanup();
}

void StSocketBase::cleanup()
{
    DEBUG_L(L_DEBUG, LCTX, "cleanup(): %d %p %d", id, this, socket_fd);
    socket_fd = nullsocket;
}

void StSocketBase::close()
{
    DEBUG_L(L_DEBUG, LCTX, "close(): %d %p %d", id, this, socket_fd);
    if (isValid())
    {
        ::close(socket_fd);
    }
    cleanup();
}

StUnixServerSocket::StUnixServerSocket(StUnixSocketId& name, int numberOfClients)
    : StSocketBase(socket(AF_UNIX, SOCK_STREAM, 0))
{
    if (!isValid())
    {
        ERROR_L(LCTX, "socket() failed: %d", errno);
        return;
    }

    StSocketAddress addr(name.name());

    addr.unlink();

    if (!addr.bind(socket_fd))
    {
        ERROR_L(LCTX, "bind() failed: %d", errno);
        close();
        return;
    }

    listen(socket_fd, numberOfClients);
}

StSocketConnection StUnixServerSocket::waitForConnection()
{
    StSocketConnection connection(accept(socket_fd, 0, 0));
    if (!connection.isValid())
    {
        ERROR_L(LCTX, "accept() failed: %d", errno);
    }
    return connection;
}

StSocketConnection::StSocketConnection(const char* name)
    : StSocketBase(socket(AF_UNIX, SOCK_STREAM, 0))
{
    if (!isValid())
    {
        ERROR_L(LCTX, "socket() failed: %d", errno);
        return;
    }

    StSocketAddress addr(name);

    if (!addr.connect(socket_fd))
    {
        ERROR_L(LCTX, "connect() failed: %d", errno);
        close();
    }
}

bool StSocketConnection::sendFileDescriptor(int fd)
{
    struct msghdr msg;

    // allocate memory to 'msg_control' field in msghdr struct
    char buf[CMSG_SPACE(sizeof(int))];

    // the memory to be allocated should include data + header..
    // this is calculated by the above macro...
    // (it merely adds some no. of bytes and returs that number..
    struct cmsghdr *cmsg;

    struct iovec ve;
    // must send/receive atleast one byte...
    // main purpose is to have some error checking..
    // but this is completely irrelevant in the current context..
    char *st = "I";

    // jst let us allocate 1 byte for formality and leave it that way...
    ve.iov_base = st;
    ve.iov_len = 1;

    // attach this memory to our main msghdr struct...
    msg.msg_iov = &ve;
    msg.msg_iovlen = 1;

    // these are optional fields ..
    // leave these fields with zeros..
    // to prevent unnecessary SIGSEGVs..
    msg.msg_name = NULL;
    msg.msg_namelen = 0;

    // here starts the main part..
    // attach the 'buf' to msg_control..
    // and fill in the size field correspondingly..
    msg.msg_control = buf;

    // actually msg_control field must  point to a struct of type 'cmsghdr' we just allocated the memory,
    // yet we need to set all the corresponding fields..
    // It is done as follows:
    msg.msg_controllen = sizeof(buf);

    // this macro returns the address in the buffer..
    // from where the first header starts..
    // set all the fields appropriately..
    cmsg = CMSG_FIRSTHDR(&msg);

    cmsg->cmsg_level = SOL_SOCKET;
    cmsg->cmsg_type = SCM_RIGHTS;

    // in the above field we need to store
    // the size of header + data(in this case 4 bytes(int) for our fd..
    // this is returned by the 'CMSG_LEN' macro..
    cmsg->cmsg_len = CMSG_LEN(sizeof(fd));

    // after the above three fields we keep the actual data..
    // the macro 'CMSG_DATA' returns pointer to this location
    // and we set it to the file descriptor to be sent..
    *(int*) CMSG_DATA(cmsg) = fd;

    // now that we have filled the 'cmsg' struct we store the size of this struct..
    // this one isn't required when you pass a single fd..
    // but useful when u pass multiple fds.
    msg.msg_controllen = cmsg->cmsg_len;

    // leave the flags field zeroed..
    msg.msg_flags = 0;

    DEBUG_L(L_DEBUG, LCTX, "before sendmsg() : %d %d %d", id, socket_fd, fd);
    if (sendmsg(socket_fd, &msg, 0) == -1)
    {
        ERROR_L(LCTX, "sendmsg() failed: %d %d", id, errno);
        return false;
    }

    DEBUG_L(L_DEBUG, LCTX, "sendmsg() sent: %d", fd);
    return true;
}

bool StSocketConnection::receiveFileDescriptor(int& fd)
{
    fd = -1;

    struct msghdr msg;

    // do all the unwanted things first...
    // same as the send_fd function..
    struct iovec io;
    char ptr[1];

    io.iov_base = ptr;
    io.iov_len = 1;
    msg.msg_name = 0;
    msg.msg_namelen = 0;
    msg.msg_iov = &io;
    msg.msg_iovlen = 1;

    char buf[CMSG_SPACE(sizeof(int))];
    msg.msg_control = buf;
    msg.msg_controllen = sizeof(buf);

    /*now here comes the main part..*/
    if (recvmsg(socket_fd, &msg, 0) == -1)
    {
        // some shit has happened
        ERROR_L(LCTX, "recvmsg() failed: %d", errno);
        return false;
    }

    struct cmsghdr *cm;

    // get the first message header..
    cm = CMSG_FIRSTHDR(&msg);

    if (cm->cmsg_type != SCM_RIGHTS)
    {
        // again some shit has happened.
        ERROR_L(LCTX, "recvmsg() - unknown type: %d", cm->cmsg_type);
        return false;
    }

    // if control has reached here..
    // this means we have got the correct message..
    // and when you extract the fd out of this message this need not be same as the one which was sent..
    // allocating a new fd is all done by the kernel and our job is jst to use it..
    fd = *(int*) CMSG_DATA(cm);
    DEBUG_L(L_DEBUG, LCTX, "received fd: %d", fd);
    return true;
}

