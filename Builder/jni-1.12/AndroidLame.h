//
// Created by Naman on 26/01/16.
//

#include <jni.h>
#include "libmp3lame/lame.h"

#ifndef TANDROIDLAME_ANDROIDLAME_H
#define TANDROIDLAME_ANDROIDLAME_H

#ifdef __cplusplus
extern "C" {
#endif

lame_global_flags *initializeDefault(
        JNIEnv *env);

lame_global_flags *initialize(
        JNIEnv *env,
        jint inSamplerate, jint outChannel,
        jint outSamplerate, jint outBitrate, jfloat scaleInput, jint mode, jint vbrMode,
        jint quality, jint vbrQuality, jint abrMeanBitrate, jint lowpassFreq, jint highpassFreq,
        jstring id3tagTitle, jstring id3tagArtist, jstring id3tagAlbum,
        jstring id3tagYear, jstring id3tagComment);

jint encode(
        JNIEnv *env, lame_global_flags *glf,
        jshortArray buffer_l, jshortArray buffer_r,
        jint samples, jbyteArray mp3buf);

jint encodeBufferInterleaved(
        JNIEnv *env, lame_global_flags *glf,
        jshortArray pcm, jint samples, jbyteArray mp3buf);

jint flush(
        JNIEnv *env, lame_global_flags *glf,
        jbyteArray mp3buf);

void close(
        lame_global_flags *glf);

#ifdef __cplusplus
}
#endif

#endif //TANDROIDLAME_ANDROIDLAME_H
