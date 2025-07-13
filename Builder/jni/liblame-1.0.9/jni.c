#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <lame.h>

JNIEXPORT void JNICALL
Java_com_github_axet_lamejni_Lame_open(JNIEnv *env, jobject thiz, jint channels,
                                       jint sampleFreq, jint brate, jint q) {
    jclass cls = (*env)->GetObjectClass(env, thiz);
    jfieldID fid = (*env)->GetFieldID(env, cls, "handle", "J");

    lame_t lame = lame_init();
    (*env)->SetLongField(env, thiz, fid, (jlong) lame);

    lame_set_num_channels(lame, channels);
    if (channels == 1)
        lame_set_mode(lame, MONO);
    if (channels == 2)
        lame_set_mode(lame, STEREO);

    lame_set_in_samplerate(lame, sampleFreq);
    lame_set_VBR(lame, vbr_default);
    lame_set_VBR_q(lame, q);
    lame_set_VBR_mean_bitrate_kbps(lame, brate); // lame_set_brate(lame, brate);

    if (lame_init_params(lame) != 0) {
        jclass class_rex = (*env)->FindClass(env, "java/lang/RuntimeException");
        (*env)->ThrowNew(env, class_rex, "Bad lame_init_params");
        return;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_github_axet_lamejni_Lame_encode(JNIEnv *env, jobject thiz,
                                         jshortArray array, jint pos, jint len) {
    jclass cls = (*env)->GetObjectClass(env, thiz);
    jfieldID fid = (*env)->GetFieldID(env, cls, "handle", "J");
    lame_t lame = (lame_t) (*env)->GetLongField(env, thiz, fid);

    char *out = 0;
    int outlen = 0;

    outlen = sizeof(short int) * len;
    if (outlen < 4096)
        outlen = 4096;
    out = malloc(outlen);

    if (len == 0) {
        outlen = lame_encode_flush(lame, out, outlen);
    } else {
        jshort *bufferPtr = (*env)->GetShortArrayElements(env, array, NULL);

        int channels = lame_get_num_channels(lame);
        int ns = len / channels;
        if (channels == 1)
            outlen = lame_encode_buffer(lame, pos + bufferPtr, NULL, ns, out, outlen);
        else
            outlen = lame_encode_buffer_interleaved(lame, pos + bufferPtr, ns, out, outlen);

        (*env)->ReleaseShortArrayElements(env, array, bufferPtr, 0);
    }

    if (outlen < 0)
        outlen = 0;

    jbyteArray ret = (*env)->NewByteArray(env, outlen);
    (*env)->SetByteArrayRegion(env, ret, 0, outlen, out);
    free(out);
    return ret;
}

JNIEXPORT jbyteArray JNICALL
Java_com_github_axet_lamejni_Lame_encode_1float(JNIEnv *env, jobject thiz, jfloatArray buf,
                                                jint pos, jint len) {
    jclass cls = (*env)->GetObjectClass(env, thiz);
    jfieldID fid = (*env)->GetFieldID(env, cls, "handle", "J");
    lame_t lame = (lame_t) (*env)->GetLongField(env, thiz, fid);

    char *out = 0;
    int outlen = 0;

    outlen = sizeof(short int) * len;
    if (outlen < 4096)
        outlen = 4096;
    out = malloc(outlen);

    if (len == 0) {
        outlen = lame_encode_flush(lame, out, outlen);
    } else {
        jfloat *bufferPtr = (*env)->GetFloatArrayElements(env, buf, NULL);

        int channels = lame_get_num_channels(lame);
        int ns = len / channels;
        if (channels == 1)
            outlen = lame_encode_buffer_ieee_float(lame, pos + bufferPtr, NULL, ns, out, outlen);
        else
            outlen = lame_encode_buffer_interleaved_ieee_float(lame, pos + bufferPtr, ns, out, outlen);

        (*env)->ReleaseFloatArrayElements(env, buf, bufferPtr, 0);
    }

    if (outlen < 0)
        outlen = 0;

    jbyteArray ret = (*env)->NewByteArray(env, outlen);
    (*env)->SetByteArrayRegion(env, ret, 0, outlen, out);
    free(out);
    return ret;
}

JNIEXPORT jbyteArray JNICALL
Java_com_github_axet_lamejni_Lame_close(JNIEnv *env, jobject thiz) {
    jclass cls = (*env)->GetObjectClass(env, thiz);
    jfieldID fid = (*env)->GetFieldID(env, cls, "handle", "J");
    lame_t lame = (lame_t) (*env)->GetLongField(env, thiz, fid);

    char *out = 0;
    int outlen = 4096;
    out = malloc(outlen);
    outlen = lame_get_lametag_frame(lame, out, outlen);
    if (outlen < 0)
        outlen = 0;
    jbyteArray ret = (*env)->NewByteArray(env, outlen);
    (*env)->SetByteArrayRegion(env, ret, 0, outlen, out);
    free(out);

    lame_close(lame);

    (*env)->SetLongField(env, thiz, fid, 0);
    return ret;
}
