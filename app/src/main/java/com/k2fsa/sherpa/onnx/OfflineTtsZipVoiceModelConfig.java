package com.k2fsa.sherpa.onnx;

public class OfflineTtsZipVoiceModelConfig {
    public String tokens = "";
    public String encoder = "";
    public String decoder = "";
    public String vocoder = "";
    public String dataDir = "";
    public String lexicon = "";
    public float featScale = 0.1f;
    public float tShift = 0.5f;
    public float targetRms = 0.1f;
    public float guidanceScale = 1.0f;
}
