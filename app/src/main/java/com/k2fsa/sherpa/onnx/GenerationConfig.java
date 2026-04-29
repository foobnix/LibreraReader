package com.k2fsa.sherpa.onnx;

import java.util.Map;

public class GenerationConfig {
    public float silenceScale = 0.2f;
    public float speed = 1.0f;
    public int sid = 0;
    public float[] referenceAudio = null;
    public int referenceSampleRate = 0;
    public String referenceText = null;
    public int numSteps = 5;
    public Map<String, String> extra = null;
}
