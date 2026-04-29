package com.k2fsa.sherpa.onnx;

public class OfflineTtsConfig {
    public OfflineTtsModelConfig model = new OfflineTtsModelConfig();
    public String ruleFsts = "";
    public String ruleFars = "";
    public int maxNumSentences = 1;
    public float silenceScale = 0.2f;
}
