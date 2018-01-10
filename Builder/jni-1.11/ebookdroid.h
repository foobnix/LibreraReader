#ifndef EBOOKDROID_H
#define EBOOKDROID_H

#include <jni.h>

#ifdef __cplusplus
extern "C"
{
#endif

int getDescriptor(JNIEnv *env, jobject fd);

const char* GetStringUTFChars(JNIEnv *env, jstring jstr, jboolean* iscopy);
void ReleaseStringUTFChars(JNIEnv *env, jstring jstr, const char* str);

#ifdef __cplusplus
}
#endif

#endif
