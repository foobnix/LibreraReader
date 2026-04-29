package com.k2fsa.sherpa.onnx;

public class OfflineTtsPocketModelConfig {
    public String lmFlow = "";
    public String lmMain = "";
    public String encoder = "";
    public String decoder = "";
    public String textConditioner = "";
    public String vocabJson = "";
    public String tokenScoresJson = "";
    public int voiceEmbeddingCacheCapacity = 50;
}
