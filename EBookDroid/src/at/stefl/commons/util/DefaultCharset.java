package at.stefl.commons.util;

import java.nio.charset.Charset;

public enum DefaultCharset {
    
    US_ASCII("US-ASCII"), ISO_8859_1("ISO-8859-1"), UTF_8("UTF-8"), UTF_16BE(
            "UTF-16BE"), UTF_16LE("UTF-16LE"), UTF_16("UTF-16");
    
    private final Charset charset;
    
    private DefaultCharset(String name) {
        this.charset = Charset.forName(name);
    }
    
    public Charset getCharset() {
        return charset;
    }
    
}