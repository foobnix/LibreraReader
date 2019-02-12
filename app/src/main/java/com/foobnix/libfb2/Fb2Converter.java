package com.foobnix.libfb2;

public class Fb2Converter {


    public static native int fb2ToEpubNative(String fb2FileName, String epubFileName, String cssDir, String fontsDir);
}
