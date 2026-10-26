#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <mupdf/fitz.h>

static void* handler = NULL;
static int present = 0;

void* NativeBitmapInit();
void closeHandler();

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid",
        "initializing EBookDroid JNI library based on MuPDF and DjVuLibre");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *jvm, void *reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Unloading EBookDroid JNI library based on MuPDF and DjVuLibre");
    closeHandler();
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_free(JNIEnv *env, jobject this)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "Free EBookDroid JNI library");
    closeHandler();
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_EBookDroidLibraryLoader_isNativeGraphicsAvailable(JNIEnv *env, jobject this)
{
    return present;
}

void* NativeBitmapInit()
{
        return NULL;
}

void closeHandler()
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "closeHandler");
    present = 0;
    if (handler)
        dlclose(handler);
    handler = NULL;
}


/**
 * Get file descriptor from FileDescriptor class.
 */
int getDescriptor(JNIEnv *env, jobject fd)
{
    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "getDescriptor");
    jclass fd_class = (*env)->GetObjectClass(env, fd);
    jfieldID field_id = (*env)->GetFieldID(env, fd_class, "descriptor", "I");
    return (*env)->GetIntField(env, fd, field_id);
}

const char* GetStringUTFChars(JNIEnv *env, jstring jstr, jboolean* iscopy)
{
    return jstr != NULL ? (*env)->GetStringUTFChars(env, jstr, iscopy) : NULL ;
}

void ReleaseStringUTFChars(JNIEnv *env, jstring jstr, const char* str)
{
    if (jstr && str)
    {
        (*env)->ReleaseStringUTFChars(env, jstr, str);
    }
}


