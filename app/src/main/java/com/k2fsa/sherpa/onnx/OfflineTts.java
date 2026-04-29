package com.k2fsa.sherpa.onnx;

import android.content.res.AssetManager;

public class OfflineTts {
    private long ptr;
    private final OfflineTtsConfig config;

    static {
        System.loadLibrary("sherpa-onnx-jni");
    }

    public OfflineTts(AssetManager assetManager, OfflineTtsConfig config) {
        this.config = config;
        ptr = assetManager == null ? newFromFile(config) : newFromAsset(assetManager, config);
    }

    public int sampleRate() {
        return getSampleRate(ptr);
    }

    public int numSpeakers() {
        return getNumSpeakers(ptr);
    }

    public GeneratedAudio generate(String text, int sid, float speed) {
        return generateImpl(ptr, text, sid, speed);
    }

    public GeneratedAudio generateWithConfig(String text, GenerationConfig config) {
        return generateWithConfigImpl(ptr, text, config, null);
    }

    public void allocate(AssetManager assetManager) {
        if (ptr == 0) {
            ptr = assetManager == null ? newFromFile(config) : newFromAsset(assetManager, config);
        }
    }

    public void release() {
        if (ptr != 0) {
            delete(ptr);
            ptr = 0;
        }
    }

    @Override protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private native long newFromAsset(AssetManager assetManager, OfflineTtsConfig config);
    private native long newFromFile(OfflineTtsConfig config);
    private native void delete(long ptr);
    private native int getSampleRate(long ptr);
    private native int getNumSpeakers(long ptr);
    private native GeneratedAudio generateImpl(long ptr, String text, int sid, float speed);
    private native GeneratedAudio generateWithConfigImpl(long ptr, String text, GenerationConfig config,
                                                        Object callback);
}
