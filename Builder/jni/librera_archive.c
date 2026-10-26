/*
 * The comic archives - .cbz (ZIP) and .cbr (RAR 1.5-4.x, RAR5) - read with libarchive, for
 * com.foobnix.ext.NativeArchive: every build opens a comic in the comic engine, which reads its
 * pages through it, and takes the library's covers and ComicInfo.xml from it. It is in
 * libMuPDF.so and libLibreraDocs.so alike (libarchive-3.8.10/Android.mk); MuPDF does not use it.
 *
 * The archive is read from the file in place, never copied. libarchive reads entries in the order
 * they are stored, so an entry is reached by reading the headers up to it - which skips the data
 * in between with a seek, or by decompressing it in a solid RAR - and an entry before the last
 * one read means starting again from the top. A comic is read front to back, so that is rare.
 *
 * One handle is not thread-safe; the Java side synchronizes on it.
 */

#include <jni.h>
#include <errno.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include <android/log.h>

#include <archive.h>
#include <archive_entry.h>

#define TAG "NativeArchive"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define BLOCK (64 * 1024)

typedef struct
{
    int fd;
    off64_t file_size;
    struct archive *archive;

    /* the entry the next archive_read_next_header() returns */
    int next;

    int count;
    char **names;       /* raw bytes, UTF-8 where the archive says so; NULL but for a file */
    int64_t *sizes;     /* -1 where the header does not say */

    unsigned char block[BLOCK];
} native_archive;

static la_ssize_t
na_read(struct archive *a, void *data, const void **buf)
{
    native_archive *na = data;
    ssize_t n;

    do
        n = read(na->fd, na->block, sizeof na->block);
    while (n < 0 && errno == EINTR);

    if (n < 0)
    {
        archive_set_error(a, errno, "read failed");
        return -1;
    }
    *buf = na->block;
    return n;
}

static la_int64_t
na_skip(struct archive *a, void *data, la_int64_t request)
{
    native_archive *na = data;
    off64_t pos = lseek64(na->fd, 0, SEEK_CUR);

    /* 0 is "could not skip", and libarchive reads through the data instead */
    if (pos < 0 || request <= 0)
        return 0;
    if (request > na->file_size - pos)
        request = na->file_size - pos;
    if (request <= 0 || lseek64(na->fd, pos + request, SEEK_SET) < 0)
        return 0;
    return request;
}

static la_int64_t
na_seek(struct archive *a, void *data, la_int64_t offset, int whence)
{
    native_archive *na = data;
    off64_t pos = lseek64(na->fd, offset, whence);

    return pos < 0 ? ARCHIVE_FATAL : pos;
}

static void
na_stop(native_archive *na)
{
    if (na->archive)
    {
        archive_read_free(na->archive);
        na->archive = NULL;
    }
}

/* Opens the archive at its first entry. */
static int
na_start(native_archive *na)
{
    na_stop(na);
    na->next = 0;

    if (lseek64(na->fd, 0, SEEK_SET) < 0)
        return -1;

    na->archive = archive_read_new();
    if (!na->archive)
        return -1;
    archive_read_support_format_zip(na->archive);
    archive_read_support_format_rar(na->archive);
    archive_read_support_format_rar5(na->archive);

    if (archive_read_set_seek_callback(na->archive, na_seek) != ARCHIVE_OK
            || archive_read_open2(na->archive, na, NULL, na_read, na_skip, NULL) != ARCHIVE_OK)
    {
        LOGE("cannot open: %s", archive_error_string(na->archive));
        na_stop(na);
        return -1;
    }
    return 0;
}

/* ARCHIVE_WARN is a header read with something odd in it, a name that would not convert for one */
static int
na_next_header(native_archive *na, struct archive_entry **entry)
{
    int r = archive_read_next_header(na->archive, entry);

    if (r == ARCHIVE_OK || r == ARCHIVE_WARN)
    {
        na->next++;
        return ARCHIVE_OK;
    }
    return r;
}

static void
na_free(native_archive *na)
{
    int i;

    na_stop(na);
    if (na->fd >= 0)
        close(na->fd);
    for (i = 0; i < na->count; i++)
        free(na->names[i]);
    free(na->names);
    free(na->sizes);
    free(na);
}

/* Reads every header once, for the names and the sizes. */
static int
na_list(native_archive *na)
{
    struct archive_entry *entry;
    int max = 0;
    int r;

    while ((r = na_next_header(na, &entry)) == ARCHIVE_OK)
    {
        const char *name;

        if (na->count == max)
        {
            int new_max = max ? max * 2 : 64;
            char **names = realloc(na->names, sizeof(*names) * new_max);
            int64_t *sizes;

            if (!names)
                return -1;
            na->names = names;
            sizes = realloc(na->sizes, sizeof(*sizes) * new_max);
            if (!sizes)
                return -1;
            na->sizes = sizes;
            max = new_max;
        }

        /* an old RAR or zip stores names in the packer's code page, which this build (no iconv)
         * cannot convert; the bytes as they are still tell the pages apart */
        name = archive_entry_pathname_utf8(entry);
        if (!name)
            name = archive_entry_pathname(entry);

        /* only a file can be a page: directories and links have no name here */
        na->names[na->count] = NULL;
        if (name && archive_entry_filetype(entry) == AE_IFREG)
        {
            na->names[na->count] = strdup(name);
            if (!na->names[na->count])
                return -1;
        }
        na->sizes[na->count] = archive_entry_size_is_set(entry) ? archive_entry_size(entry) : -1;
        na->count++;
    }

    if (r != ARCHIVE_EOF)
    {
        LOGE("cannot list: %s", archive_error_string(na->archive));
        return -1;
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_foobnix_ext_NativeArchive_nativeOpen(JNIEnv *env, jclass clazz, jbyteArray jpath)
{
    native_archive *na;
    struct stat64 st;
    jsize len = (*env)->GetArrayLength(env, jpath);
    char *path = malloc(len + 1);

    if (!path)
        return 0;
    (*env)->GetByteArrayRegion(env, jpath, 0, len, (jbyte *) path);
    path[len] = 0;

    na = calloc(1, sizeof *na);
    if (!na)
    {
        free(path);
        return 0;
    }
    na->fd = open(path, O_RDONLY | O_CLOEXEC);
    free(path);

    if (na->fd < 0 || fstat64(na->fd, &st) < 0)
    {
        na_free(na);
        return 0;
    }
    na->file_size = st.st_size;

    if (na_start(na) < 0 || na_list(na) < 0)
    {
        na_free(na);
        return 0;
    }

    /* the listing left the archive at its end: the first read starts it again */
    return (jlong) (intptr_t) na;
}

JNIEXPORT jobjectArray JNICALL
Java_com_foobnix_ext_NativeArchive_nativeNames(JNIEnv *env, jclass clazz, jlong handle)
{
    native_archive *na = (native_archive *) (intptr_t) handle;
    jclass bytes = (*env)->FindClass(env, "[B");
    jobjectArray names;
    int i;

    if (!bytes)
        return NULL;
    names = (*env)->NewObjectArray(env, na->count, bytes, NULL);
    if (!names)
        return NULL;
    for (i = 0; i < na->count; i++)
    {
        jbyteArray name;
        jsize n;

        if (!na->names[i])
            continue;
        n = (jsize) strlen(na->names[i]);
        name = (*env)->NewByteArray(env, n);
        if (!name)
            return NULL;
        (*env)->SetByteArrayRegion(env, name, 0, n, (const jbyte *) na->names[i]);
        (*env)->SetObjectArrayElement(env, names, i, name);
        (*env)->DeleteLocalRef(env, name);
    }
    return names;
}

JNIEXPORT jlongArray JNICALL
Java_com_foobnix_ext_NativeArchive_nativeSizes(JNIEnv *env, jclass clazz, jlong handle)
{
    native_archive *na = (native_archive *) (intptr_t) handle;
    jlongArray sizes = (*env)->NewLongArray(env, na->count);

    if (sizes && na->count > 0)
        (*env)->SetLongArrayRegion(env, sizes, 0, na->count, (const jlong *) na->sizes);
    return sizes;
}

JNIEXPORT jbyteArray JNICALL
Java_com_foobnix_ext_NativeArchive_nativeRead(JNIEnv *env, jclass clazz, jlong handle, jint index)
{
    native_archive *na = (native_archive *) (intptr_t) handle;
    struct archive_entry *entry;
    unsigned char *data = NULL;
    size_t len = 0;
    size_t cap;
    jbyteArray result;

    if (index < 0 || index >= na->count)
        return NULL;

    if (!na->archive || index < na->next)
    {
        if (na_start(na) < 0)
            return NULL;
    }

    /* the data of the entries in between is skipped by the next header read */
    while (na->next <= index)
    {
        if (na_next_header(na, &entry) != ARCHIVE_OK)
        {
            LOGE("cannot reach entry %d: %s", index, archive_error_string(na->archive));
            na_stop(na);
            return NULL;
        }
    }

    cap = na->sizes[index] >= 0 ? (size_t) na->sizes[index] : BLOCK;
    if (cap == 0)
        cap = 1;
    data = malloc(cap);
    if (!data)
        return NULL;

    for (;;)
    {
        la_ssize_t n;

        if (len == cap)
        {
            unsigned char *more;

            /* the header said less than there is, or nothing at all */
            cap *= 2;
            more = realloc(data, cap);
            if (!more)
            {
                free(data);
                return NULL;
            }
            data = more;
        }
        n = archive_read_data(na->archive, data + len, cap - len);
        if (n == 0)
            break;
        if (n < 0)
        {
            LOGE("cannot read entry %d: %s", index, archive_error_string(na->archive));
            free(data);
            na_stop(na);
            return NULL;
        }
        len += (size_t) n;
    }

    result = (*env)->NewByteArray(env, (jsize) len);
    if (result)
        (*env)->SetByteArrayRegion(env, result, 0, (jsize) len, (const jbyte *) data);
    free(data);
    return result;
}

JNIEXPORT void JNICALL
Java_com_foobnix_ext_NativeArchive_nativeClose(JNIEnv *env, jclass clazz, jlong handle)
{
    native_archive *na = (native_archive *) (intptr_t) handle;

    if (na)
        na_free(na);
}
