#include <string.h>
#include <jni.h>
#include <stdint.h>

#include <android/log.h>

#include "hqxcommon.h"
#include "hqx.h"


#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define MIN(a,b) (((a) < (b)) ? (a) : (b))

JNIEXPORT void JNICALL
Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeHq4x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq4x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeHq3x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq3x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeHq2x(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jintArray dstArray, jint width, jint height)
{

    jint* src;
    jint* dst;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);
    dst = (*env)->GetIntArrayElements(env, dstArray, 0);

    hq2x_32(src, dst, width, height);

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
    (*env)->ReleaseIntArrayElements(env, dstArray, dst, 0);
}


JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeInvert(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jint width, jint height)
{
    jint* src;
	int i;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);

    for (i = 0; i < width * height; i++) {
    	src[i] ^= 0x00FFFFFF;
    }

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeContrast(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jint width, jint height, jint contrast)
{
    jint* src;
    int i, a;
    unsigned char buf[256];
    int midBright = 0;
    unsigned char* src1;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);

	src1 = (unsigned char*)src;

    for (i = 0; i < width * height * 4; i += 4) {
        midBright += src1[i+2] * 77 + src1[i + 1] * 150 + src1[i] * 29;
    }
    midBright /= (256 * width * height);

    for (i = 0; i < 256; i++) {
        a = (((i - midBright) * contrast) / 256) + midBright;
        if (a < 0) {
            buf[i] = 0;
        } else if (a > 255) {
            buf[i] = 255;
        } else {
            buf[i] = a;
        }
    }

    for (i = 0; i < width * height * 4; i+=4) {
        src1[i] = buf[src1[i]];
        src1[i+1] = buf[src1[i+1]];
        src1[i+2] = buf[src1[i+2]];
    }

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeExposure(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jint width, jint height, jint exp)
{
    jint* src;
    int i, a;
    unsigned char* src1;

    src = (*env)->GetIntArrayElements(env, srcArray, 0);

    src1 = (unsigned char*)src;

    for (i = 0; i < width * height * 4; i += 4) {
        src1[i] = MIN(MAX(src1[i] + exp * 11 / 100, 0), 255);
        src1[i+1] = MIN(MAX(src1[i+1] + exp * 59 / 100, 0), 255);
        src1[i+2] = MIN(MAX(src1[i+2] + exp * 30 / 100, 0), 255);
    }

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeAutoLevels(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jint width, jint height)
{
    jint* src;
    int i;
    unsigned char* src1;
    int histoR[256];
    int cumulativeFreqR[256];
    int histoG[256];
    int cumulativeFreqG[256];
    int histoB[256];
    int cumulativeFreqB[256];

    int numpixels = width * height;
    src = (*env)->GetIntArrayElements(env, srcArray, 0);

    src1 = (unsigned char*)src;
    for (i = 0; i < 256; i++) {
        histoR[i] = 0;
        histoG[i] = 0;
        histoB[i] = 0;
    }
    for (i = 0; i < numpixels * 4; i += 4) {
        histoR[src1[i]]++;
        histoG[src1[i+1]]++;
        histoB[src1[i+2]]++;
    }

    for (i = 0; i < 256; i++) {
        cumulativeFreqR[i] = histoR[i] + (i > 0 ? cumulativeFreqR[i-1] : 0);
        cumulativeFreqG[i] = histoG[i] + (i > 0 ? cumulativeFreqG[i-1] : 0);
        cumulativeFreqB[i] = histoB[i] + (i > 0 ? cumulativeFreqB[i-1] : 0);
    }

    for (i = 0; i < numpixels * 4; i += 4) {
        src1[i] = MIN(MAX(cumulativeFreqR[src1[i]] * 255 / numpixels, 0), 255);
        src1[i+1] = MIN(MAX(cumulativeFreqG[src1[i+1]] * 255 / numpixels, 0), 255);
        src1[i+2] = MIN(MAX(cumulativeFreqB[src1[i+2]] * 255 / numpixels, 0), 255);
    }



    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
}

JNIEXPORT void JNICALL
 Java_org_ebookdroid_common_bitmaps_RawBitmap_nativeAutoLevels2(JNIEnv* env, jclass classObject, jintArray srcArray,
                                                          jint width, jint height)
{
    jint* src;
    int i;
    unsigned char* src1;
    int histoR[256];
    int cumulativeFreqR[256];
    int histoG[256];
    int cumulativeFreqG[256];
    int histoB[256];
    int cumulativeFreqB[256];

    int numpixels = width * height;
    int minR = 0, minG = 0, minB = 0;
    int maxR = 0, maxG = 0, maxB = 0;

    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "nativeAutoLevels");

    src = (*env)->GetIntArrayElements(env, srcArray, 0);

    src1 = (unsigned char*)src;
    for (i = 0; i < 256; i++) {
        histoR[i] = 0;
        histoG[i] = 0;
        histoB[i] = 0;
    }
    for (i = 0; i < numpixels * 4; i += 4) {
        histoR[src1[i]]++;
        histoG[src1[i+1]]++;
        histoB[src1[i+2]]++;
    }

    for (i = 0; i < 256; i++) {
        cumulativeFreqR[i] = histoR[i] + (i > 0 ? cumulativeFreqR[i-1] : 0);
        cumulativeFreqG[i] = histoG[i] + (i > 0 ? cumulativeFreqG[i-1] : 0);
        cumulativeFreqB[i] = histoB[i] + (i > 0 ? cumulativeFreqB[i-1] : 0);
    }

    for (i = 0; i < 256; i++) {
        if (cumulativeFreqR[i] > 5 * numpixels / 100 && minR == 0) {
            minR = MAX(0, i - 1);
        }
        if (cumulativeFreqG[i] > 5 * numpixels / 100 && minG == 0) {
            minG = MAX(0, i - 1);
        }
        if (cumulativeFreqB[i] > 5 * numpixels / 100 && minB == 0) {
            minB = MAX(0, i - 1);
        }
    }

    for (i = 255; i >= 0; i--) {
        if (cumulativeFreqR[i] < 95 * numpixels / 100 && maxR == 0) {
            maxR = MIN(255, i + 1);
        }
        if (cumulativeFreqG[i] < 95 * numpixels / 100 && maxG == 0) {
            maxG = MIN(255, i + 1);
        }
        if (cumulativeFreqB[i] < 95 * numpixels / 100 && maxB == 0) {
            maxB = MIN(255, i + 1);
        }
    }

    if (maxR - minR < 10) {
      minR = MAX(0, minR - 5);
      maxR = MIN(255, maxR + 5);
    }

    if (maxG - minG < 10) {
      minG = MAX(0, minG - 5);
      maxG = MIN(255, maxG + 5);
    }

    if (maxB - minB < 10) {
      minB = MAX(0, minB - 5);
      maxB = MIN(255, maxB + 5);
    }

    int min = MIN(MIN(minR, minG), minB);
    int max = MAX(MAX(maxR, maxG), maxB);

//    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "minR = %d, minG = %d, minB = %d", minR, minG, minB);
//    __android_log_print(ANDROID_LOG_DEBUG, "EBookDroid", "maxR = %d, maxG = %d, maxB = %d", maxR, maxG, maxB);

    for (i = 0; i < numpixels * 4; i += 4) {
        src1[i] = MIN(MAX((src1[i] - minR) * 255 / (maxR - minR), 0), 255);
        src1[i+1] = MIN(MAX((src1[i+1] - minG) * 255 / (maxG - minG), 0), 255);
        src1[i+2] = MIN(MAX((src1[i+2] - minB) * 255 / (maxB - minB), 0), 255);
    }

    (*env)->ReleaseIntArrayElements(env, srcArray, src, 0);
}

