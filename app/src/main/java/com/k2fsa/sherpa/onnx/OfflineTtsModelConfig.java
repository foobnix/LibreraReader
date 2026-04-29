package com.k2fsa.sherpa.onnx;

public class OfflineTtsModelConfig {
    public OfflineTtsVitsModelConfig vits = new OfflineTtsVitsModelConfig();
    public OfflineTtsMatchaModelConfig matcha = new OfflineTtsMatchaModelConfig();
    public OfflineTtsKokoroModelConfig kokoro = new OfflineTtsKokoroModelConfig();
    public OfflineTtsZipVoiceModelConfig zipvoice = new OfflineTtsZipVoiceModelConfig();
    public OfflineTtsKittenModelConfig kitten = new OfflineTtsKittenModelConfig();
    public OfflineTtsPocketModelConfig pocket = new OfflineTtsPocketModelConfig();
    public OfflineTtsSupertonicModelConfig supertonic = new OfflineTtsSupertonicModelConfig();
    public int numThreads = 2;
    public boolean debug = false;
    public String provider = "cpu";
}
