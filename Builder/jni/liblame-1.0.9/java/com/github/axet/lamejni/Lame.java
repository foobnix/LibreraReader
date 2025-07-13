package com.github.axet.lamejni;

public class Lame {
    static {
        if (Config.natives) {
            System.loadLibrary("lamejni");
        }
    }

    private long handle;

    public native void open(int channels, int sampleRate, int brate, int q);

    public native byte[] encode(short[] buf, int pos, int len);

    public native byte[] encode_float(float[] buf, int pos, int len);

    public native byte[] close();

}
