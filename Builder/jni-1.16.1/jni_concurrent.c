/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * This program implements the fz_lock()/fz_unlock()
 * call-backs necessary to make concurrent page processing
 * within a document possible. This is accomplished by
 * setting up the fz_locks_context that has been added to
 * jni_document_s. Each document has its own lock structure.
 *
 * A unique lock object is created for each opened document
 * this way each document handles locks within itself. This
 * lets us process multiple documents concurrently as well.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ */

#include "mupdf/fitz.h"
#include <pthread.h>

typedef pthread_mutex_t jni_mutex;

#define jni_init_mutex(l) pthread_mutex_init(l, NULL)
#define jni_destroy_mutex(l) pthread_mutex_destroy(l)
#define jni_enter_critical(l) pthread_mutex_lock(l)
#define jni_leave_critical(l) pthread_mutex_unlock(l)

typedef struct jni_locks_s jni_locks;
struct jni_locks_s
{
    jni_mutex * lock;
};

enum
{
    JNI_LOCK_INTERNAL = FZ_LOCK_MAX, JNI_MAX_LOCKS
};

/**
 * Enter critical section
 */
static void jni_lock_internal(void *user, int lock)
{
    if (user)
    {
        jni_locks *locks = (jni_locks*) user;
        if (locks[lock].lock)
        {
            jni_enter_critical(locks[lock].lock);
        }
    }
}

/**
 * Exit critical section
 */
static void jni_unlock_internal(void *user, int lock)
{
    if (user)
    {
        jni_locks *locks = (jni_locks*) user;
        if (locks[lock].lock)
        {
            jni_leave_critical(locks[lock].lock);
        }
    }
}

/**
 * Create new lock object
 */
static void * jni_new_lock_obj()
{
    jni_locks *obj = malloc(sizeof(jni_locks) * JNI_MAX_LOCKS);
    if (obj)
    {
        int i = 0;
        for (i = 0; i < JNI_MAX_LOCKS; i++)
        {
            obj[i].lock = malloc(sizeof(jni_mutex));
            jni_init_mutex(obj[i].lock);
        }
        return obj;
    }
    return NULL;
}

/**
 * Configure fz_locks_context
 */
fz_locks_context * jni_new_locks()
{
    fz_locks_context *locks = malloc(sizeof(fz_locks_context));

    if (!locks)
    {
        return NULL;
    }

    locks->user = jni_new_lock_obj();
    locks->lock = jni_lock_internal;
    locks->unlock = jni_unlock_internal;

    if (!locks->user)
    {
        free(locks);
        return NULL;
    }

    return locks;
}

/**
 * Free lock object
 */
void jni_free_locks(fz_locks_context *locks)
{
    if (locks && locks->user)
    {
        jni_locks *obj = (jni_locks*) locks->user;
        int i = 0;
        for (i = 0; i < JNI_MAX_LOCKS; i++)
        {
            if (obj[i].lock)
            {
                jni_destroy_mutex(obj[i].lock);
                free(obj[i].lock);
            }
        }
        free(obj);
        free(locks);
    }
}

/**
 * Enter critical section
 */
void jni_lock(fz_context *ctx)
{
    jni_lock_internal(ctx->user, JNI_LOCK_INTERNAL);
}

/**
 * Exit critical section
 */
void jni_unlock(fz_context *ctx)
{
    jni_unlock_internal(ctx->user, JNI_LOCK_INTERNAL);
}
