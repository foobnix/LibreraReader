/** @file randombytes.c
 *  @brief Portable function for generating random data
 *  
 * Copyright (c) 2022 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of libmobi.
 * Licensed under LGPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 *
 * This code is based on libsodium's randombytes_buf function
 * We just extract the single function we need from libsodium and  adjust it for our use.
 * Most of the code originates from:
 * https://github.com/jedisct1/libsodium/blob/d250858c7445b7de94e912b529b81defe20d4aaa/src/libsodium/randombytes/sysrandom/randombytes_sysrandom.c
 * Original code uses following license:
 *
 * ISC License
 *
 * Copyright (c) 2013-2022
 * Frank Denis <j at pureftpd dot org>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdint.h>
#include <string.h>
#ifndef _WIN32
# include <unistd.h>
#endif
#include <stdlib.h>

#include "config.h"
#include "randombytes.h"
#include "debug.h"

#include <sys/types.h>
#ifndef _WIN32
# include <sys/stat.h>
# include <sys/time.h>
#endif
#ifdef __linux__
# define _LINUX_SOURCE
#endif
#ifdef HAVE_SYS_RANDOM_H
# include <sys/random.h>
#endif
#ifdef __linux__
# define BLOCK_ON_DEV_RANDOM
# include <poll.h>
# ifdef HAVE_GETRANDOM
#  define HAVE_LINUX_COMPATIBLE_GETRANDOM
# else
#  include <sys/syscall.h>
#  if defined(SYS_getrandom) && defined(__NR_getrandom)
#   define getrandom(B, S, F) syscall(SYS_getrandom, (B), (int) (S), (F))
#   define HAVE_LINUX_COMPATIBLE_GETRANDOM
#  endif
# endif
#elif defined(__FreeBSD__) || defined(__DragonFly__)
# include <sys/param.h>
# if (defined(__FreeBSD_version) && __FreeBSD_version >= 1200000) || \
     (defined(__DragonFly_version) && __DragonFly_version >= 500700)
#  define HAVE_LINUX_COMPATIBLE_GETRANDOM
# endif
#endif

#define UNUSED(x) (void)(x)

typedef struct {
    int random_data_source_fd;
    int getrandom_available;
} MOBIRandom;

#ifdef _WIN32

# include <windows.h>
# define RtlGenRandom SystemFunction036
# if defined(__cplusplus)
extern "C"
# endif
BOOLEAN NTAPI RtlGenRandom(PVOID RandomBuffer, ULONG RandomBufferLength);
# ifdef _MSC_VER
#  pragma comment(lib, "advapi32.lib")
# endif
#endif

#if defined(__OpenBSD__) || defined(__CloudABI__) || defined(__wasi__)
# define HAVE_SAFE_ARC4RANDOM
#endif

#ifdef HAVE_SAFE_ARC4RANDOM

/**
 @brief Read a buffer of random bytes using arc4random_buf call
 
 @param[in,out] handle Handle
 @param[in,out] buf Buffer
 @param[in] size Buffer size
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_sysrandom_buf(MOBIRandom *handle, void *buf, const size_t size) {
    UNUSED(handle);
    arc4random_buf(buf, size);
    return MOBI_SUCCESS;
}

#else /* HAVE_SAFE_ARC4RANDOM */

# ifndef _WIN32
/**
 @brief Read a buffer of random bytes from file descriptoir
 
 @param[in] fd File descriptior
 @param[in,out] buf_ Buffer
 @param[in] size Buffer size
 @return Number of bytes read
 */
static ssize_t mobi_safe_read(const int fd, void *buf_, size_t size) {
    unsigned char *buf = (unsigned char *) buf_;
    ssize_t readnb;

    do {
        while ((readnb = read(fd, buf, size)) < (ssize_t) 0 && (errno == EINTR || errno == EAGAIN));
        if (readnb < (ssize_t) 0) {
            return readnb;
        }
        if (readnb == (ssize_t) 0) {
            break;
        }
        size -= (size_t) readnb;
        buf += readnb;
    } while (size > (ssize_t) 0);

    return (ssize_t) (buf - (unsigned char *) buf_);
}

#  ifdef BLOCK_ON_DEV_RANDOM
/**
 @brief Block on /dev/random until enough entropy is available
 
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_block_on_dev_random() {
    int fd = open("/dev/random", O_RDONLY);
    if (fd == -1) {
        return MOBI_SUCCESS;
    }
    struct pollfd pfd;
    pfd.fd = fd;
    pfd.events = POLLIN;
    pfd.revents = 0;
    int pret;
    do {
        pret = poll(&pfd, 1, -1);
    } while (pret < 0 && (errno == EINTR || errno == EAGAIN));
    if (pret != 1) {
        (void) close(fd);
        errno = EIO;
        return MOBI_DRM_RANDOM_ERR;
    }
    if (close(fd) != 0) {
        return MOBI_DRM_RANDOM_ERR;
    }
    return MOBI_SUCCESS;
}
#  endif /* BLOCK_ON_DEV_RANDOM */

/**
 @brief Open random device, wait for enough entropy if supported
 
 @return Random device file descriptor
 */
static int mobi_randombytes_sysrandom_random_dev_open() {

#  ifdef BLOCK_ON_DEV_RANDOM
    if (mobi_randombytes_block_on_dev_random() != MOBI_SUCCESS) {
        return -1;
    }
#  endif

    static const char *devices[] = {
        "/dev/urandom",
        "/dev/random", NULL
    };
    const char **device = devices;

    do {
        int fd = open(*device, O_RDONLY);
        if (fd != -1) {
            struct stat st;
            if (fstat(fd, &st) == 0 &&
#  ifdef S_ISNAM
                (S_ISNAM(st.st_mode) || S_ISCHR(st.st_mode))
#  else
                S_ISCHR(st.st_mode)
#  endif
               ) {
#  if defined(F_SETFD) && defined(FD_CLOEXEC)
                (void) fcntl(fd, F_SETFD, fcntl(fd, F_GETFD) | FD_CLOEXEC);
#  endif
                return fd;
            }
            (void) close(fd);
        } else if (errno == EINTR) {
            continue;
        }
        device++;
    } while (*device != NULL);

    errno = EIO;
    return -1;
}

#  ifdef HAVE_LINUX_COMPATIBLE_GETRANDOM
/**
 @brief Read a buffer of random bytes using getrandom system call
 In libmobi we only need small KEYSIZE buffer, so we don't have to handle buffers over 256 bytes and read chunks
 
 @param[in,out] buf Buffer
 @param[in] size Buffer size (up to 256 bytes)
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_linux_getrandom(void *buf, const size_t size) {

    if (size > 256U) {
        debug_print("This function can only handle buffer size up to 256 bytes (%zu requested)\n", size);
        return MOBI_PARAM_ERR;
    }
    int readnb;
    do {
        readnb = getrandom(buf, size, 0);
    } while (readnb < 0 && (errno == EINTR || errno == EAGAIN));

    if (readnb != (int) size) {
        debug_print("Getrandom failed (%s)\n", strerror(errno));
        return MOBI_DRM_RANDOM_ERR;
    }
    return MOBI_SUCCESS;
}

#  endif /* HAVE_LINUX_COMPATIBLE_GETRANDOM */

/**
 @brief Initialize random data source
 
 @param[in,out] handle Handle
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_sysrandom_init(MOBIRandom *handle) {
#  define NEEDS_INIT
    const int errno_save = errno;

#  ifdef HAVE_LINUX_COMPATIBLE_GETRANDOM
    {
        unsigned char fodder[16];

        if (mobi_randombytes_linux_getrandom(fodder, sizeof fodder) == MOBI_SUCCESS) {
            handle->getrandom_available = 1;
            errno = errno_save;
            return MOBI_SUCCESS;
        }
        handle->getrandom_available = 0;
    }
#  endif

    if ((handle->random_data_source_fd = mobi_randombytes_sysrandom_random_dev_open()) == -1) {
        debug_print("Couldn't open random device (%s)\n", strerror(errno));
        return MOBI_DRM_RANDOM_ERR;
    }
    errno = errno_save;
    return MOBI_SUCCESS;
}

/**
 @brief Initialize random data source
 
 @param[in,out] handle Handle
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_sysrandom_close(MOBIRandom *handle) {
#  define NEEDS_CLOSE
    MOBI_RET ret = MOBI_DRM_RANDOM_ERR;
    if (handle->random_data_source_fd != -1 && close(handle->random_data_source_fd) == 0) {
        handle->random_data_source_fd = -1;
        ret = MOBI_SUCCESS;
    }
#  ifdef HAVE_LINUX_COMPATIBLE_GETRANDOM
    if (handle->getrandom_available != 0) {
        ret = MOBI_SUCCESS;
    }
#  endif
    return ret;
}

# endif /* _WIN32 */

/**
 @brief Read a buffer of random bytes
 
 @param[in,out] handle Handle
 @param[in,out] buf Buffer
 @param[in] size Buffer size (up to 256 bytes)
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
static MOBI_RET mobi_randombytes_sysrandom_buf(MOBIRandom *handle, void *buf, const size_t size) {
# ifndef _WIN32
#  ifdef HAVE_LINUX_COMPATIBLE_GETRANDOM
    if (handle->getrandom_available != 0) {
        return mobi_randombytes_linux_getrandom(buf, size);
    }
#  endif
    if (handle->random_data_source_fd == -1 ||
        mobi_safe_read(handle->random_data_source_fd, buf, size) != (ssize_t) size) {
        return MOBI_DRM_RANDOM_ERR;
    }
# else /* _WIN32 */
    UNUSED(handle);
    if (! RtlGenRandom((PVOID) buf, (ULONG) size)) {
        return MOBI_DRM_RANDOM_ERR;
    }
# endif /* _WIN32 */
    return MOBI_SUCCESS;
}

#endif /* HAVE_SAFE_ARC4RANDOM */

/**
 @brief Fill buffer with random bytes
 
 @param[in,out] buf Buffer
 @param[in] size Buffer size (up to 256 bytes)
 @return MOBI_RET status code (on success MOBI_SUCCESS)
 */
MOBI_RET mobi_randombytes(void *buf, const size_t size) {
    
    MOBIRandom handle = {
        .random_data_source_fd = -1,
        .getrandom_available = 0
    };
    
    MOBI_RET ret;
#ifdef NEEDS_INIT
    ret = mobi_randombytes_sysrandom_init(&handle);
    if (ret != MOBI_SUCCESS) {
        return ret;
    }
#endif
    
    if (size > (size_t) 0U) {
        ret = mobi_randombytes_sysrandom_buf(&handle, buf, size);
        if (ret != MOBI_SUCCESS) {
            debug_print("%s\n", "Generating random data failed");
            return ret;
        }
    }
#ifdef NEEDS_CLOSE
    if (mobi_randombytes_sysrandom_close(&handle) != MOBI_SUCCESS) {
        debug_print("%s\n", "Closing random data source failed");
    }
#endif
    return MOBI_SUCCESS;
}
